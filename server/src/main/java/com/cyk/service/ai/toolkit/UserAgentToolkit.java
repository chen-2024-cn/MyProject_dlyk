package com.cyk.service.ai.toolkit;

import com.cyk.config.AiPromptProvider;
import com.cyk.mapper.TProductMapper;
import com.cyk.mapper.TTranMapper;
import com.cyk.model.TProduct;
import com.cyk.model.TTran;
import com.cyk.result.TrendPoint;
import com.cyk.result.ai.AiAbility;
import com.cyk.service.AiPremiumAbilityService;
import com.cyk.service.StatisticService;
import com.cyk.service.ai.AiAgentContext;
import com.cyk.result.SummaryData;

import java.util.Map;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.agent.tool.P;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * AI 智能体 · 普通用户工具包。
 *
 * 职责定位：面向普通业务用户，围绕「自己的订单、自己的提醒、业务答疑」提供服务。
 * 安全边界：所有订单类查询在 SQL 层强制限定 create_by = 当前登录人，无法越权查看他人数据。
 */
@Slf4j
public class UserAgentToolkit extends AbstractAiToolkit {

    private final TTranMapper tTranMapper;
    private final TProductMapper tProductMapper;
    private final StatisticService statisticService;
    private final AiPromptProvider promptProvider;

    public UserAgentToolkit(AiAgentContext context,
                            AiPremiumAbilityService premiumAbilityService,
                            TTranMapper tTranMapper,
                            TProductMapper tProductMapper,
                            StatisticService statisticService,
                            AiPromptProvider promptProvider) {
        super(context, premiumAbilityService);
        this.tTranMapper = tTranMapper;
        this.tProductMapper = tProductMapper;
        this.statisticService = statisticService;
        this.promptProvider = promptProvider;
    }

    // ==================== 免费业务能力 ====================

    @Tool("查询当前登录用户自己名下的交易订单列表，可返回订单流水号、客户姓名、金额、阶段、创建时间；"
            + "这是数据范围安全的查询，只会返回该用户本人创建的订单")
    public String queryMyOrders(
            @P(value = "阶段字典ID；不确定时省略该参数（表示查询全部阶段）", required = false) Integer stage,
            @P(value = "返回条数上限，最多20条；不指定时省略该参数（默认10条）", required = false) Integer limit) {
        log.info("AI 工具调用: queryMyOrders | userId={}, stage={}, limit={}", context.getUserId(), stage, limit);
        int safeLimit = limit == null || limit <= 0 || limit > 20 ? 10 : limit;
        List<TTran> orders = tTranMapper.selectRecentByCreateBy(context.getUserId(), stage, safeLimit);
        if (orders == null || orders.isEmpty()) {
            return "查询完成：您名下暂无符合条件的交易订单。";
        }
        StringBuilder sb = new StringBuilder("查询完成，您名下最近的 ").append(orders.size()).append(" 条订单：\n");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd");
        for (TTran t : orders) {
            sb.append("- 流水号: ").append(t.getTranNo())
                    .append(" | 客户: ").append(t.getCustomerName() != null ? t.getCustomerName() : "未知")
                    .append(" | 金额: ").append(t.getMoney())
                    .append(" | 阶段: ").append(t.getStageDO() != null ? t.getStageDO().getTypeValue() : "未知")
                    .append(" | 创建时间: ").append(t.getCreateTime() != null ? fmt.format(t.getCreateTime()) : "-")
                    .append("\n");
        }
        return sb.toString();
    }

    @Tool("查询当前登录用户自己在指定天数内需要跟进联系客户的交易提醒（下次联系时间临近且未成交的订单）")
    public String queryMyFollowUpReminder(
            @P(value = "未来多少天内需要跟进，如 3 表示未来3天；不指定时省略该参数（默认3天）", required = false) Integer withinDays) {
        int days = withinDays == null || withinDays <= 0 ? 3 : Math.min(withinDays, 30);
        log.info("AI 工具调用: queryMyFollowUpReminder | userId={}, withinDays={}", context.getUserId(), days);
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, days);
        Date deadline = cal.getTime();
        List<TTran> reminders = tTranMapper.selectUpcomingFollowUp(context.getUserId(), deadline, 20);
        if (reminders == null || reminders.isEmpty()) {
            return "查询完成：未来 " + days + " 天内您没有需要跟进联系的交易提醒。";
        }
        StringBuilder sb = new StringBuilder("查询完成，未来 ").append(days)
                .append(" 天内您有 ").append(reminders.size()).append(" 笔交易需要跟进联系：\n");
        SimpleDateFormat fmt = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        for (TTran t : reminders) {
            sb.append("- 流水号: ").append(t.getTranNo())
                    .append(" | 客户: ").append(t.getCustomerName() != null ? t.getCustomerName() : "未知")
                    .append(" | 金额: ").append(t.getMoney())
                    .append(" | 阶段: ").append(t.getStageDO() != null ? t.getStageDO().getTypeValue() : "未知")
                    .append(" | 联系时间: ").append(t.getNextContactTime() != null ? fmt.format(t.getNextContactTime()) : "-")
                    .append("\n");
        }
        sb.append("建议提前准备好沟通话术，按时联系客户以推进成交。");
        return sb.toString();
    }

    @Tool("解答 CRM 业务流程中的疑难问题，包括交易阶段含义、线索转化流程、客户管理规则等业务知识")
    public String answerBusinessFaq(@P("用户的业务问题原文") String question) {
        log.info("AI 工具调用: answerBusinessFaq | keyword={}", question);
        // 知识库全文外置于 prompts/business-faq.md，运营可直接维护 Markdown 文件扩充知识条目
        return promptProvider.render("business-faq", Map.of("question", question == null ? "" : question));
    }

    @Tool("查询当前在售的产品目录及经销商报价，用于解答产品与价格相关咨询")
    public String queryProductsOnSale(
            @P(value = "产品名称关键字；不确定时省略该参数（查询全部在售产品）", required = false) String keyword) {
        log.info("AI 工具调用: queryProductsOnSale");
        List<TProduct> products = tProductMapper.selectAllOnSaleProduct();
        if (products == null || products.isEmpty()) {
            return "查询完成：当前暂无在售产品。";
        }
        StringBuilder sb = new StringBuilder("查询完成，当前在售产品：\n");
        for (TProduct p : products) {
            if (keyword != null && !keyword.isBlank() && !p.getName().contains(keyword)) {
                continue;
            }
            sb.append("- ").append(p.getName())
                    .append(" | 经销商报价: ").append(p.getQuotation())
                    .append(" | 官方指导价区间: ").append(p.getGuidePriceS()).append("~").append(p.getGuidePriceE())
                    .append("\n");
        }
        if (sb.length() == "查询完成，当前在售产品：\n".length()) {
            return "查询完成：未找到匹配「" + keyword + "」的在售产品。";
        }
        return sb.toString();
    }

    // ==================== 增值付费能力 ====================

    @Tool("生成当前登录用户的经营深度洞察报告（增值付费能力）：汇总本人订单数量、总金额、各阶段分布等多维分析")
    public String generateDeepInsightReport() {
        String gate = premiumGate(AiAbility.DEEP_INSIGHT);
        if (gate != null) {
            return gate;
        }
        log.info("AI 工具调用: generateDeepInsightReport | userId={}", context.getUserId());
        List<TTran> orders = tTranMapper.selectRecentByCreateBy(context.getUserId(), null, 20);
        int total = orders == null ? 0 : orders.size();
        BigDecimal totalAmount = orders == null ? BigDecimal.ZERO
                : orders.stream().map(t -> t.getMoney() == null ? BigDecimal.ZERO : t.getMoney())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long won = orders == null ? 0 : orders.stream()
                .filter(t -> t.getStage() != null && t.getStage() == 42).count();

        SummaryData summary = statisticService.loadSummaryData();
        StringBuilder sb = new StringBuilder("【经营深度洞察报告】\n");
        sb.append("一、您的个人盘面：近期订单 ").append(total).append(" 笔，合计金额 ")
                .append(totalAmount.setScale(2, RoundingMode.HALF_UP)).append(" 元，其中已成交 ").append(won).append(" 笔。\n");
        if (summary != null) {
            sb.append("二、全局经营参照：平台总交易额 ")
                    .append(summary.getTotalTranAmount() != null ? summary.getTotalTranAmount().setScale(2, RoundingMode.HALF_UP) : 0)
                    .append(" 元，成功交易额 ")
                    .append(summary.getSuccessTranAmount() != null ? summary.getSuccessTranAmount().setScale(2, RoundingMode.HALF_UP) : 0)
                    .append(" 元，客户总数 ").append(summary.getTotalCustomerCount()).append("。\n");
        }
        sb.append("三、洞察建议：若成交率偏低，建议优先清理阶段长期停滞的订单，保持每笔交易的下次联系时间有效。");
        return sb.toString();
    }

    @Tool("预测当前登录用户的交易趋势（增值付费能力）：基于本人每月交易金额走势做环比推演")
    public String forecastTranTrend() {
        String gate = premiumGate(AiAbility.TREND_FORECAST);
        if (gate != null) {
            return gate;
        }
        log.info("AI 工具调用: forecastTranTrend | userId={}", context.getUserId());
        List<TrendPoint> points = tTranMapper.selectTranAmountByMonthByCreateBy(context.getUserId());
        if (points == null || points.size() < 2) {
            return "趋势数据不足（至少需要两个月的交易数据），暂无法生成预测，建议多沉淀一些经营数据后再使用本能力。";
        }
        StringBuilder sb = new StringBuilder("【交易趋势预测】您的近月交易金额走势：\n");
        for (TrendPoint p : points) {
            sb.append("- ").append(p.getName()).append(": ")
                    .append(p.getValue() != null ? p.getValue().setScale(2, RoundingMode.HALF_UP) : 0).append(" 元\n");
        }
        // 简单环比推演：最近两月增速外推下月（演示级预测，生产应接入时间序列模型）
        BigDecimal last = points.get(points.size() - 1).getValue() != null
                ? points.get(points.size() - 1).getValue() : BigDecimal.ZERO;
        BigDecimal prev = points.get(points.size() - 2).getValue() != null
                ? points.get(points.size() - 2).getValue() : BigDecimal.ZERO;
        BigDecimal diff = last.subtract(prev);
        BigDecimal forecast = last.add(diff);
        sb.append("环比推演：最近一月较上月").append(diff.signum() >= 0 ? "增长 " : "下降 ")
                .append(diff.abs().setScale(2, RoundingMode.HALF_UP))
                .append(" 元，按当前动能推算下月交易金额约 ").append(forecast.setScale(2, RoundingMode.HALF_UP))
                .append(" 元。\n建议：").append(diff.signum() >= 0
                ? "继续保持跟进节奏，重点推进高金额在途订单。"
                : "增速转弱，建议加大线索跟进频率并及时回访老客户。");
        return sb.toString();
    }
}
