package com.cyk.service.ai.toolkit;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.context.AnalysisContext;
import com.alibaba.excel.read.listener.ReadListener;
import com.cyk.constants.Constants;
import com.cyk.mapper.TRoleMapper;
import com.cyk.mapper.TTranMapper;
import com.cyk.mapper.TUserMapper;
import com.cyk.model.TRole;
import com.cyk.model.TTran;
import com.cyk.model.TUser;
import com.cyk.model.TUserRole;
import com.cyk.result.R;
import com.cyk.result.ai.AiAbility;
import com.cyk.result.ai.UserExportRow;
import com.cyk.result.ai.UserImportRow;
import com.cyk.service.AiPremiumAbilityService;
import com.cyk.service.RedisService;
import com.cyk.service.StatisticService;
import com.cyk.service.UserService;
import com.cyk.service.ai.AiAgentContext;
import com.cyk.result.SummaryData;
import com.cyk.result.TrendData;
import com.cyk.result.TrendPoint;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 智能体 · 管理员工具包。
 *
 * 职责定位：面向管理员，提供用户数据导入导出、全局订单查询、用户权限管理等运营级操作。
 * 安全边界：
 * 1. 本工具包仅在登录人被服务端判定为 admin 时挂载，普通用户会话中根本不存在这些工具；
 * 2. 每个危险操作入口再做二次角色断言（纵深防御，防止挂载逻辑变更时越权）；
 * 3. 账号状态变更会同步清理 Redis 登录 token，被禁用账号立即失效。
 */
@Slf4j
public class AdminAgentToolkit extends AbstractAiToolkit {

    private final UserService userService;
    private final TUserMapper tUserMapper;
    private final TRoleMapper tRoleMapper;
    private final TTranMapper tTranMapper;
    private final RedisService redisService;
    private final StatisticService statisticService;

    public AdminAgentToolkit(AiAgentContext context,
                             AiPremiumAbilityService premiumAbilityService,
                             UserService userService,
                             TUserMapper tUserMapper,
                             TRoleMapper tRoleMapper,
                             TTranMapper tTranMapper,
                             RedisService redisService,
                             StatisticService statisticService) {
        super(context, premiumAbilityService);
        this.userService = userService;
        this.tUserMapper = tUserMapper;
        this.tRoleMapper = tRoleMapper;
        this.tTranMapper = tTranMapper;
        this.redisService = redisService;
        this.statisticService = statisticService;
    }

    /** 二次角色断言：纵深防御，所有管理工具入口统一执行 */
    private String adminGuard() {
        if (!context.isAdmin()) {
            log.warn("AI 管理工具越权拦截 | userId={}", context.getUserId());
            return "【权限不足】该操作仅限管理员使用，当前账号不具备管理员角色。";
        }
        return null;
    }

    // ==================== 用户数据导入导出 ====================

    @Tool("导出全部用户数据为 Excel 文件（管理员专属）。执行成功后前端会自动收到下载卡片")
    public String exportUsersExcel() {
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        log.info("AI 工具调用: exportUsersExcel | operator={}", context.getUserId());
        try {
            List<TUser> users = tUserMapper.selectAllUsers();
            List<UserExportRow> rows = new ArrayList<>();
            for (TUser u : users) {
                UserExportRow row = new UserExportRow();
                row.setId(u.getId());
                row.setLoginAct(u.getLoginAct());
                row.setName(u.getName());
                row.setPhone(u.getPhone());
                row.setEmail(u.getEmail());
                row.setAccountStatus(u.getAccountEnabled() != null && u.getAccountEnabled() == 1 ? "启用" : "禁用");
                List<TRole> roles = tRoleMapper.selectByUserId(u.getId());
                row.setRoleNames(roles == null ? "" :
                        roles.stream().map(TRole::getRole).collect(Collectors.joining(",")));
                row.setCreateTime(u.getCreateTime());
                rows.add(row);
            }

            // 生成文件到 AI 导出目录，文件名含时间戳避免并发覆盖
            String fileName = "用户数据导出_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
            File file = resolveExportFile(fileName);
            EasyExcel.write(file, UserExportRow.class).sheet("用户数据").doWrite(rows);

            // 结构化事件：前端据此渲染下载按钮（下载链接由前端附登录 token 组装）
            context.getEventEmitter().accept(EVENT_FILE_READY_PREFIX + fileName + "]");
            log.info("AI 用户导出完成 | operator={}, count={}, file={}",
                    context.getUserId(), rows.size(), file.getAbsolutePath());
            return String.format("导出完成：共 %d 条用户数据，文件已生成（%s）。已向前端推送下载卡片，请提示用户点击下载。",
                    rows.size(), fileName);
        } catch (Exception e) {
            log.error("AI 用户导出失败 | operator=" + context.getUserId(), e);
            return "【导出失败】" + e.getMessage();
        }
    }

    @Tool("生成用户批量导入的标准 Excel 模板（管理员专属），执行成功后前端会自动收到下载卡片")
    public String downloadImportTemplate() {
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        log.info("AI 工具调用: downloadImportTemplate | operator={}", context.getUserId());
        try {
            String fileName = "用户导入模板_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".xlsx";
            File file = resolveExportFile(fileName);
            EasyExcel.write(file, UserImportRow.class).sheet("用户导入").doWrite(new ArrayList<>());
            context.getEventEmitter().accept(EVENT_FILE_READY_PREFIX + fileName + "]");
            return "模板已生成并向前端推送下载卡片。填写要求：登录账号必填且唯一；密码不填时系统使用统一初始密码；"
                    + "导入后默认启用，角色需另外分配。";
        } catch (Exception e) {
            log.error("AI 导入模板生成失败 | operator=" + context.getUserId(), e);
            return "【模板生成失败】" + e.getMessage();
        }
    }

    @Tool("将当前会话中用户上传的 Excel 附件批量导入用户（管理员专属）。"
            + "文件内容由服务端直接读取上传暂存区（含此前轮次上传的附件），无需用户提供任何文件内容参数")
    public String importUsersFromAttachment() {
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        String base64Content = context.getAttachmentBase64();
        if (base64Content == null || base64Content.isBlank()) {
            return "【导入失败】当前会话未携带 Excel 附件。请引导用户先在页面上方点击附件上传按钮选择文件，发送消息后再执行导入。";
        }
        log.info("AI 工具调用: importUsersFromAttachment | operator={}, fileName={}",
                context.getUserId(), context.getAttachmentFileName());
        try {
            byte[] bytes = Base64.getDecoder().decode(base64Content.trim());
            List<UserImportRow> rows = new ArrayList<>();
            try (InputStream in = new java.io.ByteArrayInputStream(bytes)) {
                EasyExcel.read(in, UserImportRow.class, new ReadListener<UserImportRow>() {
                    @Override
                    public void invoke(UserImportRow row, AnalysisContext analysisContext) {
                        rows.add(row);
                    }

                    @Override
                    public void doAfterAllAnalysed(AnalysisContext analysisContext) {
                        // 数据量受上传体积上限约束，无需分批落库
                    }
                }).sheet().doRead();
            }
            if (rows.isEmpty()) {
                return "【导入失败】Excel 中没有可识别的数据行，请确认使用了标准模板。";
            }
            int imported = userService.aiImportUsers(rows, context.getUserId());
            // 导入成功才消费会话级附件暂存：失败时保留附件，用户纠正后可重试；
            // 成功后移除也防止多轮确认导致同一文件被重复导入
            context.consumeAttachment();
            return String.format("导入完成：共解析 %d 行，成功导入 %d 条（账号已存在的行被自动跳过）。"
                            + "提醒：新导入账号未分配角色，如需分配请继续指示。",
                    rows.size(), imported);
        } catch (IllegalArgumentException e) {
            return "【导入失败】文件不是有效的 Excel 格式：" + e.getMessage();
        } catch (Exception e) {
            log.error("AI 用户导入失败 | operator=" + context.getUserId(), e);
            return "【导入失败】" + e.getMessage();
        }
    }

    // ==================== 全局订单查询 ====================

    @Tool("全局查询所有用户的交易订单（管理员专属），可按客户姓名模糊搜索、按阶段筛选")
    public String queryAllOrders(@P(value = "客户姓名关键字；不需要时省略该参数（不按姓名过滤）", required = false) String customerName,
                                 @P(value = "阶段字典ID；不需要时省略该参数（不按阶段过滤）", required = false) Integer stage,
                                 @P(value = "返回条数上限，最多30；不指定时省略该参数（默认15条）", required = false) Integer limit) {
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        int safeLimit = limit == null || limit <= 0 || limit > 30 ? 15 : limit;
        log.info("AI 工具调用: queryAllOrders | operator={}, keyword={}, stage={}",
                context.getUserId(), customerName, stage);
        List<TTran> orders = tTranMapper.selectGlobalByCondition(customerName, stage, safeLimit);
        if (orders == null || orders.isEmpty()) {
            return "查询完成：没有符合条件的全局订单。";
        }
        StringBuilder sb = new StringBuilder("查询完成，全局订单（前 ").append(orders.size()).append(" 条）：\n");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        for (TTran t : orders) {
            sb.append("- 流水号: ").append(t.getTranNo())
                    .append(" | 客户: ").append(t.getCustomerName() != null ? t.getCustomerName() : "未知")
                    .append(" | 金额: ").append(t.getMoney())
                    .append(" | 阶段: ").append(t.getStageDO() != null ? t.getStageDO().getTypeValue() : "未知")
                    .append(" | 负责人: ").append(t.getCreateByDO() != null ? t.getCreateByDO().getName()
                            : "用户#" + t.getCreateBy())
                    .append(" | 创建时间: ").append(t.getCreateTime() != null ? fmt.format(t.getCreateTime()) : "-")
                    .append("\n");
        }
        return sb.toString();
    }

    // ==================== 用户权限管理 ====================

    @Tool("查询全部用户及其当前角色、账号状态（管理员专属），用于权限管理前的人员盘点")
    public String queryAllUsers(@P(value = "返回条数上限，最多50；不指定时省略该参数（默认20条）", required = false) Integer limit) {
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        int safeLimit = limit == null || limit <= 0 || limit > 50 ? 20 : limit;
        log.info("AI 工具调用: queryAllUsers | operator={}, limit={}", context.getUserId(), safeLimit);
        List<TUser> users = tUserMapper.selectAllUsers();
        if (users == null || users.isEmpty()) {
            return "查询完成：系统中暂无用户。";
        }
        StringBuilder sb = new StringBuilder("系统用户清单（前 ").append(Math.min(users.size(), safeLimit)).append(" 名）：\n");
        for (int i = 0; i < Math.min(users.size(), safeLimit); i++) {
            TUser u = users.get(i);
            List<TRole> roles = tRoleMapper.selectByUserId(u.getId());
            sb.append("- ID: ").append(u.getId())
                    .append(" | 账号: ").append(u.getLoginAct())
                    .append(" | 姓名: ").append(u.getName())
                    .append(" | 状态: ").append(u.getAccountEnabled() != null && u.getAccountEnabled() == 1 ? "启用" : "禁用")
                    .append(" | 角色: ").append(roles == null || roles.isEmpty() ? "无"
                            : roles.stream().map(TRole::getRole).collect(Collectors.joining(",")))
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool("查询系统中全部可用角色及其ID（管理员专属），分配角色前需要先获取本清单以确认角色ID")
    public String queryRoleList() {
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        log.info("AI 工具调用: queryRoleList | operator={}", context.getUserId());
        List<TRole> roles = tRoleMapper.selectRoleList();
        if (roles == null || roles.isEmpty()) {
            return "查询完成：暂无角色数据。";
        }
        StringBuilder sb = new StringBuilder("系统角色清单：\n");
        for (TRole r : roles) {
            sb.append("- 角色ID: ").append(r.getId())
                    .append(" | 角色标识: ").append(r.getRole())
                    .append(" | 角色名称: ").append(r.getRoleName()).append("\n");
        }
        return sb.toString();
    }

    @Tool("修改指定用户的账号状态（启用/禁用）与角色权限（管理员专属）。"
            + "修改前必须向用户复述变更内容并得到确认；被禁用的账号会立即强制下线")
    public String updateUserPermission(@P("目标用户ID") Integer userId,
                                       @P(value = "账号启用状态：1启用 0禁用；不需要修改状态时省略该参数", required = false) Integer accountEnabled,
                                       @P(value = "新的角色ID列表，逗号分隔的角色ID字符串；不需要修改角色时省略该参数", required = false) String roleIds) {
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        if (userId == null) {
            return "【操作失败】必须先明确目标用户ID。";
        }
        // 防自锁：不允许管理员禁用自己
        if (userId.equals(context.getUserId()) && accountEnabled != null && accountEnabled == 0) {
            return "【操作拒绝】你不能禁用当前登录的管理员账号本身，会导致自我锁定。";
        }
        List<Integer> roleIdList = parseRoleIds(roleIds);
        if (accountEnabled == null && roleIdList == null) {
            return "【操作失败】没有需要修改的内容：请明确要修改账号状态还是角色，或两者同时修改。";
        }
        log.info("AI 工具调用: updateUserPermission | operator={}, target={}, enabled={}, roleIds={}",
                context.getUserId(), userId, accountEnabled, roleIds);
        R result = userService.aiUpdateUserRolesAndStatus(userId, accountEnabled, roleIdList, context.getUserId());
        return result.getCode() == 200
                ? "修改成功：用户ID " + userId + " 的账号状态/角色已更新" + (accountEnabled != null && accountEnabled == 0 ? "（已禁用并强制下线）" : "") + "。"
                : "修改失败：" + result.getMsg();
    }

    // ==================== 全局经营洞察（付费能力同样对管理员开放） ====================

    @Tool("生成全局经营深度洞察报告（增值付费能力）：汇总平台线索客户转化漏斗、来源分布、订单与交易金额")
    public String generateGlobalDeepInsight() {
        String gate = premiumGate(AiAbility.DEEP_INSIGHT);
        if (gate != null) {
            return gate;
        }
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        log.info("AI 工具调用: generateGlobalDeepInsight | operator={}", context.getUserId());
        SummaryData summary = statisticService.loadSummaryData();
        List<com.cyk.result.NameValue> sources = statisticService.loadSourcePieData();
        StringBuilder sb = new StringBuilder("【全局经营深度洞察报告】\n");
        if (summary != null) {
            sb.append("一、总量盘面：线索 ").append(summary.getTotalClueCount())
                    .append(" 条 | 客户 ").append(summary.getTotalCustomerCount())
                    .append(" 个 | 有效活动 ").append(summary.getEffectiveActivityCount())
                    .append("/").append(summary.getTotalActivityCount()).append(" 场。\n");
            sb.append("二、交易盘面：总交易额 ")
                    .append(summary.getTotalTranAmount() != null ? summary.getTotalTranAmount().setScale(2, RoundingMode.HALF_UP) : 0)
                    .append(" 元，其中成功交易 ")
                    .append(summary.getSuccessTranAmount() != null ? summary.getSuccessTranAmount().setScale(2, RoundingMode.HALF_UP) : 0)
                    .append(" 元。\n");
        }
        if (sources != null && !sources.isEmpty()) {
            sb.append("三、来源分布：");
            sb.append(sources.stream().map(n -> n.getName() + " " + n.getValue()).collect(Collectors.joining("、")));
            sb.append("。\n");
        }
        sb.append("四、建议：关注线索到客户的转化率瓶颈与高价值来源渠道，优先投入转化效率最高的渠道资源。");
        return sb.toString();
    }

    @Tool("生成全局交易趋势预测报告（增值付费能力）：按月度汇总交易金额走势并做趋势推演")
    public String forecastGlobalTrend() {
        String gate = premiumGate(AiAbility.TREND_FORECAST);
        if (gate != null) {
            return gate;
        }
        String deny = adminGuard();
        if (deny != null) {
            return deny;
        }
        log.info("AI 工具调用: forecastGlobalTrend | operator={}", context.getUserId());
        TrendData trend = statisticService.loadTrendData();
        if (trend == null || trend.getMonthList() == null || trend.getMonthList().size() < 2) {
            return "趋势数据不足（至少需要两个月的经营数据），暂无法生成预测报告。";
        }
        StringBuilder sb = new StringBuilder("【全局交易趋势预测】近月交易金额走势：\n");
        List<String> months = trend.getMonthList();
        List<BigDecimal> amounts = trend.getTranAmountList();
        for (int i = 0; i < months.size(); i++) {
            sb.append("- ").append(months.get(i)).append(": ")
                    .append(amounts.get(i) != null ? amounts.get(i).setScale(2, RoundingMode.HALF_UP) : 0)
                    .append(" 元\n");
        }
        BigDecimal last = amounts.get(amounts.size() - 1);
        BigDecimal prev = amounts.get(amounts.size() - 2);
        BigDecimal diff = last.subtract(prev);
        sb.append("环比推演：最近一月较上月").append(diff.signum() >= 0 ? "增长 " : "下降 ")
                .append(diff.abs().setScale(2, RoundingMode.HALF_UP)).append(" 元，")
                .append("按当前动能推算下月交易金额约 ").append(last.add(diff).setScale(2, RoundingMode.HALF_UP)).append(" 元。");
        return sb.toString();
    }

    private List<Integer> parseRoleIds(String roleIds) {
        if (roleIds == null || roleIds.isBlank()) {
            return null;
        }
        List<Integer> list = new ArrayList<>();
        for (String part : roleIds.split(",")) {
            try {
                list.add(Integer.parseInt(part.trim()));
            } catch (NumberFormatException ignore) {
                // 忽略无法解析的片段，由调用结果提示兜底
            }
        }
        return list.isEmpty() ? null : list;
    }

    /**
     * 解析导出目录下的目标文件（目录不存在时自动创建）
     */
    private File resolveExportFile(String fileName) throws
            IOException {
        Path dir = Paths.get(Constants.AI_EXPORT_DIR);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }
        return dir.resolve(fileName).toFile();
    }
}
