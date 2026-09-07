package com.cyk.web;

import com.cyk.model.TUser;
import com.cyk.query.AiPaymentQuery;
import com.cyk.result.CodeEnum;
import com.cyk.result.R;
import com.cyk.service.AiAssistantService;
import com.cyk.service.AiPaymentService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * DLYK AI 业务智能体控制器。
 *
 * 设计要点（企业级安全分层）：
 * 1. 角色与权限一律由服务端从 JWT 登录人实时判定（AiRoleResolverService），
 *    前端不传任何权限参数——旧版「前端传 permissions」方案存在伪造风险，已废弃；
 * 2. 付费能力的开通判定下沉到工具执行层（付费墙），控制器只做入口路由；
 * 3. 所有写操作（下单/支付/导入）均校验登录人归属，防横向越权。
 */
@Slf4j
@RestController
public class AiAgentController {

    @Resource
    private AiAssistantService aiAssistantService;

    @Resource
    private AiPaymentService aiPaymentService;

    // ==================== 会话能力 ====================

    /**
     * 获取当前登录人的 AI 角色画像：按角色过滤的能力清单（含付费开通状态）。
     * 前端凭此渲染差异化的工具面板与能力商店。
     */
    @GetMapping("/api/ai/profile")
    public R profile(Authentication authentication) {
        TUser user = currentUser(authentication);
        return R.OK(aiAssistantService.buildProfile(user));
    }

    /**
     * SSE 流式对话。角色感知：服务端根据登录人角色动态挂载不同工具包，
     * 普通用户会话中根本不存在管理员工具（物理隔离，而非运行时拦截）。
     */
    @GetMapping(value = "/api/ai/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestParam String message,
                                   @RequestParam String memoryId,
                                   @RequestParam(value = "attachmentFileId", required = false) String attachmentFileId,
                                   Authentication authentication) {
        TUser user = currentUser(authentication);
        return aiAssistantService.chatStream(user, memoryId, message, attachmentFileId);
    }

    /**
     * 清空会话记忆。记忆 ID 在服务端与登录人绑定，防止清空他人会话。
     */
    @GetMapping("/api/ai/reset")
    public R resetMemory(@RequestParam String memoryId, Authentication authentication) {
        TUser user = currentUser(authentication);
        aiAssistantService.resetMemory(user.getId(), memoryId);
        return R.OK();
    }

    /**
     * 读取当前登录人的聊天记录（登录态内持久化）。
     * 前端每次进入 AI 模块时调用，实现「切换模块不丢记录、退出登录才清除」。
     */
    @GetMapping("/api/ai/history")
    public R history(Authentication authentication) {
        TUser user = currentUser(authentication);
        return R.OK(aiAssistantService.history(user.getId()));
    }

    // ==================== 增值付费体系 ====================

    /** 创建增值能力支付订单（同一能力重复下单幂等复用） */
    @PostMapping("/api/ai/payment/order")
    public R createOrder(@RequestBody AiPaymentQuery query, Authentication authentication) {
        TUser user = currentUser(authentication);
        return aiPaymentService.createOrder(user.getId(), query.getAbilityKey());
    }

    /** 模拟支付完成：推进订单状态机并自动开通能力 */
    @PostMapping("/api/ai/payment/pay")
    public R payOrder(@RequestBody AiPaymentQuery query, Authentication authentication) {
        TUser user = currentUser(authentication);
        return aiPaymentService.payOrder(user.getId(), query.getOrderNo());
    }

    /** 取消待支付订单 */
    @PostMapping("/api/ai/payment/cancel")
    public R cancelOrder(@RequestBody AiPaymentQuery query, Authentication authentication) {
        TUser user = currentUser(authentication);
        return aiPaymentService.cancelOrder(user.getId(), query.getOrderNo());
    }

    /** 查询我的支付订单记录 */
    @GetMapping("/api/ai/payment/orders")
    public R myOrders(Authentication authentication) {
        TUser user = currentUser(authentication);
        return R.OK(aiPaymentService.listMyOrders(user.getId()));
    }

    // ==================== 文件通道（管理员专属） ====================

    /**
     * 上传待导入的 Excel 文件（管理员专属），服务端暂存并返回 fileId，
     * 由用户在对话中指示 AI 使用该文件执行批量导入。
     */
    @PreAuthorize("hasAuthority('admin')")
    @PostMapping("/api/ai/file/upload")
    public R uploadFile(@RequestParam("file") MultipartFile file, Authentication authentication) throws IOException {
        TUser user = currentUser(authentication);
        if (file == null || file.isEmpty()) {
            return R.FAIL("未接收到有效文件");
        }
        String originalName = file.getOriginalFilename();
        if (originalName == null || !(originalName.endsWith(".xlsx") || originalName.endsWith(".xls"))) {
            return R.FAIL("仅支持 .xlsx / .xls 格式的 Excel 文件");
        }
        // 服务端重命名为随机 fileId，杜绝客户端文件名带来的路径注入风险
        String fileId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        Path dir = Paths.get(com.cyk.constants.Constants.AI_EXPORT_DIR, "upload");
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        Path target = dir.resolve(fileId + ".xlsx");
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        log.info("AI 文件上传完成 | operator={}, fileId={}, originalName={}, size={}",
                user.getId(), fileId, originalName, file.getSize());
        return R.OK(new UploadResult(fileId, originalName));
    }

    /**
     * 下载 AI 工具生成的 Excel（管理员专属，需登录态）。
     * 文件名严格白名单校验：禁止路径分隔符，仅允许 xlsx/xls。
     */
    @PreAuthorize("hasAuthority('admin')")
    @GetMapping("/api/ai/file/download")
    public void downloadFile(@RequestParam String fileName, HttpServletResponse response) throws IOException {
        if (!StringUtils.hasText(fileName)
                || fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")
                || !(fileName.endsWith(".xlsx") || fileName.endsWith(".xls"))) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        Path file = Paths.get(com.cyk.constants.Constants.AI_EXPORT_DIR, fileName);
        if (!Files.exists(file)) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        response.setContentType("application/octet-stream");
        response.setCharacterEncoding("utf-8");
        response.setHeader("Content-disposition", "attachment;filename="
                + URLEncoder.encode(fileName, StandardCharsets.UTF_8));
        Files.copy(file, response.getOutputStream());
        response.getOutputStream().flush();
    }

    /** 文件上传结果 */
    public record UploadResult(String fileId, String fileName) {
    }

    private TUser currentUser(Authentication authentication) {
        return (TUser) authentication.getPrincipal();
    }
}
