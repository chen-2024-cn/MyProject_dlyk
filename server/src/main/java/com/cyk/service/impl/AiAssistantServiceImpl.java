package com.cyk.service.impl;

import com.cyk.mapper.TProductMapper;
import com.cyk.mapper.TRoleMapper;
import com.cyk.mapper.TTranMapper;
import com.cyk.mapper.TUserMapper;
import com.cyk.model.TUser;
import com.cyk.constants.Constants;
import com.cyk.result.ai.AiAbility;
import com.cyk.result.ai.AiAbilityVO;
import com.cyk.result.ai.AiRoleProfile;
import com.cyk.service.AiAssistantService;
import com.cyk.service.AiPremiumAbilityService;
import com.cyk.service.AiRoleResolverService;
import com.cyk.service.RedisService;
import com.cyk.service.StatisticService;
import com.cyk.service.UserService;
import com.cyk.service.ai.AiAgentContext;
import com.cyk.service.ai.toolkit.AdminAgentToolkit;
import com.cyk.service.ai.toolkit.UserAgentToolkit;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI 业务智能体服务实现 —— 角色感知的编排中枢。
 *
 * 核心设计（企业级）：
 * 1. 角色物理隔离：普通用户会话仅挂载 UserAgentToolkit；管理员挂载 AdminAgentToolkit。
 *    管理员工具在普通用户会话中根本不存在，从根源上杜绝越权调用（优于"运行时拦截"）。
 * 2. 身份权威来源：角色判定走 AiRoleResolverService（实时查库），不信任前端传参，也不依赖 JWT 旧快照。
 * 3. 会话记忆隔离：memoryId 与 userId 复合，防止会话 ID 碰撞导致跨用户记忆串流。
 * 4. 结构化事件：工具层通过 Sinks 向流中注入业务事件帧（如付费开通卡片），
 *    前端按事件标记解析渲染，不依赖大模型转述，保证确定性。
 */
@Slf4j
@Service
public class AiAssistantServiceImpl implements AiAssistantService {

    @Resource
    private StreamingChatModel l4jStreamingChatModel;

    @Resource
    private AiRoleResolverService aiRoleResolverService;

    @Resource
    private AiPremiumAbilityService aiPremiumAbilityService;

    @Resource
    private UserService userService;

    @Resource
    private TTranMapper tTranMapper;

    @Resource
    private TProductMapper tProductMapper;

    @Resource
    private TUserMapper tUserMapper;

    @Resource
    private TRoleMapper tRoleMapper;

    @Resource
    private RedisService redisService;

    @Resource
    private StatisticService statisticService;

    /** 会话记忆仓库：key = userId:memoryId（用户级隔离，防跨用户读写记忆） */
    private final ConcurrentHashMap<String, dev.langchain4j.memory.ChatMemory> memoryCache = new ConcurrentHashMap<>();

    /**
     * 会话级附件暂存：key = userId:memoryId。
     * 背景：前端上传的附件在发送第一条消息后即被「一次性消费」清空。若大模型对敏感操作
     * 采用「先确认再执行」策略（符合操作准则），用户的确认回复发生在后续轮次，不再携带
     * attachmentFileId，导入工具将拿不到文件。故服务端按会话暂存最近一份附件，
     * 后续轮次自动回捞；导入成功后才通过 consumeAttachment 真正移除。
     */
    private final ConcurrentHashMap<String, Attachment> sessionAttachmentCache = new ConcurrentHashMap<>();

    /** 声明式 Agent 接口 */
    interface BusinessAgent {
        Flux<String> chat(@MemoryId String memoryId, @UserMessage String userMessage);
    }

    @Override
    public Flux<String> chatStream(TUser user, String memoryId, String message, String attachmentFileId) {
        Integer userId = user.getId();
        boolean isAdmin = aiRoleResolverService.isAdmin(user);
        String role = isAdmin ? AiRoleProfile.ROLE_ADMIN : AiRoleProfile.ROLE_USER;
        log.info("AI 对话开始 | userId={}, role={}, memoryId={}, attachment={}", userId, role, memoryId, attachmentFileId);

        // 1. 加载本轮附件（管理员导入场景）：服务端按 fileId 读取暂存文件，绝不信任前端传文件内容；
        //    本轮未携带时回捞会话级暂存附件（覆盖「模型先确认、用户后确认」的多轮导入场景）
        String memKey = memKey(userId, memoryId);
        Attachment attachment = loadAttachment(attachmentFileId);
        boolean attachmentCarriedOver = false;
        if (attachment != null) {
            sessionAttachmentCache.put(memKey, attachment);
        } else {
            attachment = sessionAttachmentCache.get(memKey);
            attachmentCarriedOver = attachment != null;
            if (attachmentCarriedOver) {
                log.info("AI 会话回捞暂存附件 | userId={}, memoryId={}", userId, memoryId);
            }
        }

        // 2. 结构化事件通道：工具异步线程 -> SSE 流（unicast + 缓冲，线程安全）
        Sinks.Many<String> eventSink = Sinks.many().unicast().onBackpressureBuffer();

        // 3. 构造请求级上下文（身份快照绑定进不可变对象，跨线程安全）。
        //    附件消费钩子：导入真正成功后移除会话级暂存，防止下一轮重复导入
        AiAgentContext context = new AiAgentContext(userId, user.getName(), isAdmin,
                event -> emitEvent(eventSink, event),
                attachment != null ? attachment.fileName() : null,
                attachment != null ? attachment.base64() : null,
                () -> sessionAttachmentCache.remove(memKey));

        // 4. 按角色挂载对应工具包（物理隔离）
        BusinessAgent agent = buildAgent(context, isAdmin);

        // 5. 组装角色感知的增强消息（角色设定 + 能力边界 + 附件说明）
        //    回捞附件且用户只是简短确认时，不再重复"直接调用导入工具"的指令，
        //    避免与上一轮"等待确认"的语境冲突，附件说明降级为中性提示
        String augmentedMessage = buildAugmentedMessage(user.getName(), isAdmin, message, attachment, attachmentCarriedOver);

        // 6. 合并「模型 token 流」与「工具事件流」。
        //    终结顺序关键点：merge 需所有源都完成才会结束，而事件 Sink 不会自己完成，
        //    因此必须在模型流终结时用 doFinally 主动关闭 Sink，否则 SSE 永不结束。
        Flux<String> modelStream = agent.chat(memKey, augmentedMessage)
                .doFinally(signal -> eventSink.tryEmitComplete());
        return Flux.merge(modelStream, eventSink.asFlux())
                .onErrorResume(error -> {
                    log.error("AI 流式对话报错 | userId={}", userId, error);
                    eventSink.tryEmitComplete();
                    return Flux.just("\n\n抱歉，本次服务出现异常：" + error.getMessage() + "。请稍后重试或检查后端大模型配置。");
                });
    }

    @Override
    public void resetMemory(Integer userId, String memoryId) {
        String memKey = memKey(userId, memoryId);
        dev.langchain4j.memory.ChatMemory chatMemory = memoryCache.remove(memKey);
        if (chatMemory != null) {
            chatMemory.clear();
        }
        // 会话级附件暂存与记忆同生命周期，重置会话时一并清除
        sessionAttachmentCache.remove(memKey);
        log.info("AI 会话记忆已重置 | userId={}, memoryId={}", userId, memoryId);
    }

    @Override
    public AiRoleProfile buildProfile(TUser user) {
        boolean isAdmin = aiRoleResolverService.isAdmin(user);
        List<AiAbilityVO> abilities = new ArrayList<>();
        for (AiAbility ability : AiAbility.values()) {
            if (!isVisible(ability, isAdmin)) {
                continue;
            }
            abilities.add(AiAbilityVO.builder()
                    .key(ability.getKey())
                    .name(ability.getName())
                    .description(ability.getDescription())
                    .premium(ability.isPremium())
                    .price(ability.getPrice())
                    // 免费能力恒已开通；付费能力查运行时开通状态
                    .purchased(ability.isPremium()
                            ? aiPremiumAbilityService.isGranted(user.getId(), ability)
                            : true)
                    .build());
        }
        return AiRoleProfile.builder()
                .role(isAdmin ? AiRoleProfile.ROLE_ADMIN : AiRoleProfile.ROLE_USER)
                .userName(user.getName())
                .abilities(abilities)
                .build();
    }

    /**
     * 按角色构建 Agent 并装配工具包（每次请求新建实例，绑定请求级身份快照，避免并发串流）
     */
    private BusinessAgent buildAgent(AiAgentContext context, boolean isAdmin) {
        Object toolkit = isAdmin
                ? new AdminAgentToolkit(context, aiPremiumAbilityService, userService,
                tUserMapper, tRoleMapper, tTranMapper, redisService, statisticService)
                : new UserAgentToolkit(context, aiPremiumAbilityService,
                tTranMapper, tProductMapper, statisticService);

        return AiServices.builder(BusinessAgent.class)
                .streamingChatModel(l4jStreamingChatModel)
                .chatMemoryProvider(memId ->
                        memoryCache.computeIfAbsent(memId.toString(),
                                k -> MessageWindowChatMemory.withMaxMessages(20)))
                .tools(toolkit)
                .build();
    }

    /**
     * 能力可见性：管理员可见管理域能力，普通用户可见个人域能力，全角色能力双方均可见
     */
    private boolean isVisible(AiAbility ability, boolean isAdmin) {
        if (ability.getAudience() == AiAbility.Audience.ALL) {
            return true;
        }
        return isAdmin
                ? ability.getAudience() == AiAbility.Audience.ADMIN
                : ability.getAudience() == AiAbility.Audience.USER;
    }

    /**
     * 组装角色感知的增强提示：让模型明确角色定位、能力清单与行为约束。
     * （软约束层；硬性闸门在工具层的角色断言与付费墙）
     *
     * @param attachmentCarriedOver 本轮附件是否为「会话级回捞」：为 true 时说明附件是之前轮次上传的，
     *                              模型上一轮可能已在等待用户确认，此时附件说明改为中性提示，
     *                              不再重复「直接调用导入工具、无需再索要文件」的强指令。
     */
    private String buildAugmentedMessage(String userName, boolean isAdmin, String message,
                                         Attachment attachment, boolean attachmentCarriedOver) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== [角色设定] ===\n");
        sb.append("你是 DLYK 旅途管理系统的「AI 业务领航员」，面向业务职能提供智能服务，");
        sb.append("语言专业、简洁、友好，称呼用户为「").append(userName).append("」。\n");
        if (isAdmin) {
            sb.append("当前用户角色：【管理员】，具备运营管理职责。\n");
            sb.append("可用能力：用户数据 Excel 导入导出、全局订单查询、用户权限管理（查询用户/角色、分配角色、启停账号）、");
            sb.append("以及增值付费的「经营深度洞察报告」与「交易趋势预测」。\n");
            sb.append("操作准则：涉及用户权限修改、账号启停、批量导入等敏感操作，必须先向用户复述变更内容并征得确认后再执行。\n");
        } else {
            sb.append("当前用户角色：【普通用户】，只为该用户个人业务提供服务。\n");
            sb.append("可用能力：查询本人名下的订单、交易跟进提醒、业务流程疑难解答、产品行情咨询、");
            sb.append("以及增值付费的「经营深度洞察报告」与「交易趋势预测」。\n");
            sb.append("数据边界：只能访问该用户本人创建的业务数据，无法访问他人或全局数据。\n");
        }
        sb.append("付费墙规则：若工具返回【付费能力未开通】，请礼貌引导用户点击对话中自动弹出的开通卡片完成购买。\n");
        if (attachment != null) {
            if (attachmentCarriedOver) {
                // 本轮附件来自之前轮次上传（前端已消费清空），保持与上一轮"待确认"语境的连贯性
                sb.append("附件信息：当前会话中存在已上传的 Excel 附件「").append(attachment.fileName())
                        .append("」（此前轮次上传），导入工具可正常读取该文件。\n");
            } else {
                sb.append("附件信息：用户本轮已上传 Excel 附件「").append(attachment.fileName())
                        .append("」，若用户意图为导入用户，请先复述导入事项并在用户确认后调用导入工具（文件可随时使用，无需再索要文件）。\n");
            }
        }
        sb.append("=== [用户消息] ===\n").append(message);
        return sb.toString();
    }

    /**
     * 向前端事件流注入结构化事件帧；连接已关闭时静默丢弃，不影响模型主流程
     */
    private void emitEvent(Sinks.Many<String> eventSink, String event) {
        eventSink.tryEmitNext(event);
    }

    /**
     * 本轮对话附件（服务端暂存文件的内存快照）
     */
    private record Attachment(String fileName, String base64) {
    }

    /** 附件体积上限：1.5MB（防御超大 payload 打爆内存） */
    private static final long ATTACHMENT_MAX_BYTES = 1024 * 1024 * 3 / 2;

    /**
     * 按 fileId 从服务端暂存区加载附件（绝不信任前端传文件内容）。
     * 上传时已随机重命名为 fileId.xlsx，此处直接拼接，无路径注入面。
     */
    private Attachment loadAttachment(String attachmentFileId) {
        if (attachmentFileId == null || attachmentFileId.isBlank() || !attachmentFileId.matches("[a-zA-Z0-9]{8,32}")) {
            return null;
        }
        Path file = Paths.get(Constants.AI_EXPORT_DIR, "upload", attachmentFileId + ".xlsx");
        if (!Files.exists(file)) {
            log.warn("AI 附件不存在或已失效 | fileId={}", attachmentFileId);
            return null;
        }
        try {
            if (Files.size(file) > ATTACHMENT_MAX_BYTES) {
                log.warn("AI 附件超限 | fileId={}", attachmentFileId);
                return null;
            }
            byte[] bytes = Files.readAllBytes(file);
            return new Attachment(attachmentFileId + ".xlsx", Base64.getEncoder().encodeToString(bytes));
        } catch (java.io.IOException e) {
            log.error("AI 附件读取失败 | fileId={}", attachmentFileId, e);
            return null;
        }
    }

    private String memKey(Integer userId, String memoryId) {
        return userId + ":" + memoryId;
    }
}
