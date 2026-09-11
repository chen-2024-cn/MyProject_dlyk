package com.cyk.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ClueQuery extends BaseQuery {

    /**
     * 主键，自动增长，线索ID
     */
    private Integer id;

    /**
     * 线索所属人ID
     */
    private Integer ownerId;

    /**
     * 活动ID
     */
    private Integer activityId;

    /**
     * 姓名
     */
    private String fullName;

    /**
     * 称呼
     */
    private Integer appellation;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 微信号
     */
    private String weixin;

    /**
     * QQ号
     */
    private String qq;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 年龄
     */
    private Integer age;

    /**
     * 职业
     */
    private String job;

    /**
     * 年收入
     */
    private BigDecimal yearIncome;

    /**
     * 地址
     */
    private String address;

    /**
     * 是否需要贷款（0不需要，1需要）
     */
    private Integer needLoan;

    /**
     * 意向状态
     */
    private Integer intentionState;

    /**
     * 意向产品
     */
    private Integer intentionProduct;

    /**
     * 线索状态
     */
    private Integer state;

    /**
     * 【新增筛选项】线索是否已转化为客户。
     *
     * <p>业务背景：{@code state = -1} 是「已转客户」的业务魔法值（非字典项），
     * 销售日常最关心的分群就是「还在跟进的线索」与「已成功转化的线索」。
     * 因此单独提供一个语义化布尔筛选，避免前端直接暴露 -1 这个魔法值：</p>
     * <ul>
     *   <li>{@code true}  → 仅查已转化（state = -1）</li>
     *   <li>{@code false} → 仅查未转化（state != -1）</li>
     *   <li>{@code null}  → 不限（全量）</li>
     * </ul>
     */
    private Boolean converted;

    /**
     * 线索来源
     */
    private Integer source;

    /**
     * 线索描述
     */
    private String description;

    /**
     * 下次联系时间
     *
     * 前端提交过来了一个string的日期，后端使用Date接收，那么需要加个注解转换一下
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextContactTime;
}
