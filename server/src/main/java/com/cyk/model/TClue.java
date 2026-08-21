package com.cyk.model;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.cyk.config.converter.*;
import lombok.Data;

/**
 * 线索表
 * t_clue
 */
@ExcelIgnoreUnannotated  //没有加注解的字段就不参与Excel的读写
@Data
public class TClue implements Serializable {/**
 * 主键，自动增长，线索ID
 */
private Integer id;

    /**
     * 线索所属人ID
     */
    @ExcelProperty(value = "负责人")
    private Integer ownerId;

    /**
     * 活动ID
     */
    @ExcelProperty(value = "所属活动")
    private Integer activityId;

    /**
     * 姓名
     */
    @ExcelProperty(value = "姓名")
    private String fullName;

    /**
     * 称呼
     */
    @ExcelProperty(value = "称呼", converter = AppellationConverter.class)
    private Integer appellation;

    /**
     * 手机号
     */
    @ExcelProperty(value = "手机号")
    private String phone;

    /**
     * 微信号
     */
    @ExcelProperty(value = "微信号")
    private String weixin;

    /**
     * QQ号
     */
    @ExcelProperty(value = "QQ号")
    private String qq;

    /**
     * 邮箱
     */
    @ExcelProperty(value = "邮箱")
    private String email;

    /**
     * 年龄
     */
    @ExcelProperty(value = "年龄")
    private Integer age;

    /**
     * 职业
     */
    @ExcelProperty(value = "职业")
    private String job;

    /**
     * 年收入
     */
    @ExcelProperty(value = "年收入")
    private BigDecimal yearIncome;

    /**
     * 地址
     */
    @ExcelProperty(value = "地址")
    private String address;

    /**
     * 是否需要贷款（0不需要，1需要）
     */
    @ExcelProperty(value = "是否贷款", converter = NeedLoanConverter.class)
    private Integer needLoan;

    /**
     * 意向状态
     */
    @ExcelProperty(value = "意向状态", converter = IntentionStateConverter.class)
    private Integer intentionState;

    /**
     * 意向产品
     */
    @ExcelProperty(value = "意向产品", converter = IntentionProductConverter.class)
    private Integer intentionProduct;

    /**
     * 线索状态
     */
    @ExcelProperty(value = "线索状态", converter = StateConverter.class)
    private Integer state;

    /**
     * 线索来源
     */
    @ExcelProperty(value = "线索来源", converter = SourceConverter.class)
    private Integer source;

    /**
     * 线索描述
     */
    @ExcelProperty(value = "线索描述")
    private String description;

    /**
     * 下次联系时间
     */
    @ExcelProperty(value = "下次联系时间")
    private Date nextContactTime;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 创建人
     */
    private Integer createBy;

    /**
     * 编辑时间
     */
    private Date editTime;

    /**
     * 编辑人
     */
    private Integer editBy;

    /**
     * 一对一关联
     */
    private TUser ownerDO = new TUser();
    private TActivity activityDO = new TActivity();
    private TDicValue appellationDO = new TDicValue();
    private TDicValue needLoanDO = new TDicValue();
    private TDicValue intentionStateDO = new TDicValue();
    private TProduct intentionProductDO = new TProduct();
    private TDicValue stateDO = new TDicValue();
    private TDicValue sourceDO = new TDicValue();


    private static final long serialVersionUID = 1L;
}