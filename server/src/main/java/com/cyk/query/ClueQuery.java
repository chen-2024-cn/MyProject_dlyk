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
