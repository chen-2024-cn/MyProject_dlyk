package com.cyk.result.ai;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

/**
 * AI 智能体「能力目录」枚举 —— 整个增值付费体系的唯一事实来源（Single Source of Truth）。
 *
 * 设计要点：
 * 1. 能力的可见范围由 audience 决定：管理员/普通用户各自只看到与自己职责匹配的能力；
 * 2. premium=true 的能力必须完成支付流程（t_ai_payment_order 状态机推进）后才可被工具层放行；
 * 3. 金额使用 BigDecimal 字符串构造，禁止 double（浮点精度是金融字段大忌）。
 */
@Getter
@RequiredArgsConstructor
public enum AiAbility {

    // ==================== 普通用户 · 免费业务能力 ====================

    /** 查询本人名下的交易订单（数据范围强制限定 create_by = 当前用户） */
    MY_ORDERS("my_orders", "我的订单查询", "查询我名下的交易订单、金额与阶段进展", Audience.USER, false, null),

    /** 查询本人近期需要跟进联系的交易提醒 */
    TRAN_REMINDER("tran_reminder", "交易跟进提醒", "列出我近期需要联系客户跟进的交易提醒", Audience.USER, false, null),

    /** CRM 业务流程疑难解答（内置知识库） */
    BUSINESS_FAQ("business_faq", "业务疑难解答", "解答交易阶段、线索转化、客户管理等业务流程问题", Audience.USER, false, null),

    /** 在售产品咨询 */
    PRODUCT_CONSULT("product_consult", "产品行情咨询", "查询在售产品的报价与行情信息", Audience.USER, false, null),

    // ==================== 管理员 · 免费管理能力 ====================

    /** 导出用户数据 Excel（AI 生成文件并返回下载链接） */
    USER_EXPORT("user_export", "用户数据导出", "将全部用户数据导出为 Excel 并生成下载链接", Audience.ADMIN, false, null),

    /** 通过上传 Excel 批量导入用户 */
    USER_IMPORT("user_import", "用户数据导入", "上传 Excel 批量导入用户（提供标准模板下载）", Audience.ADMIN, false, null),

    /** 全局订单查询（不受个人数据范围限制） */
    TRAN_OVERVIEW("tran_overview", "全局订单查询", "跨用户查询全局交易订单与流水明细", Audience.ADMIN, false, null),

    /** 查询/分配用户角色权限、启停账号 */
    USER_PERMISSION("user_permission", "用户权限管理", "查询用户角色、分配角色权限、启停账号", Audience.ADMIN, false, null),

    /** 系统运行状态巡检（复用既有系统运维工具） */
    SYSTEM_INSPECT("system_inspect", "系统运行巡检", "巡检 JVM/运行环境等项目系统状态", Audience.ADMIN, false, null),

    // ==================== 增值付费能力（全角色通用） ====================

    /** 付费能力：经营深度洞察报告（汇总漏斗/来源/订单的多维分析报告） */
    DEEP_INSIGHT("deep_insight", "经营深度洞察报告",
            "汇总转化漏斗、来源分布与订单规模，生成多维经营洞察报告", Audience.ALL, true, new BigDecimal("9.90")),

    /** 付费能力：交易趋势预测（基于月度交易额的走势推演） */
    TREND_FORECAST("trend_forecast", "交易趋势预测",
            "基于月度交易额走势进行环比推演与趋势预判", Audience.ALL, true, new BigDecimal("4.90"));

    /** 能力唯一标识（落库/缓存/前后端约定的 key） */
    private final String key;

    /** 能力名称（展示用，下单时冗余为快照） */
    private final String name;

    /** 能力说明 */
    private final String description;

    /** 能力可见范围 */
    private final Audience audience;

    /** 是否增值付费能力 */
    private final boolean premium;

    /** 付费价格（元），免费能力为 null */
    private final BigDecimal price;

    /**
     * 能力可见范围：普通用户 / 管理员 / 全角色
     */
    public enum Audience {
        USER, ADMIN, ALL
    }

    /**
     * 按 key 解析能力，未匹配返回 null（由调用方做业务码兜底）
     */
    public static AiAbility fromKey(String key) {
        if (key == null) {
            return null;
        }
        for (AiAbility ability : values()) {
            if (ability.key.equals(key)) {
                return ability;
            }
        }
        return null;
    }
}
