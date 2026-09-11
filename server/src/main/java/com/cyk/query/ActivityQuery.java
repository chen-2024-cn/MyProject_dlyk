package com.cyk.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class ActivityQuery extends BaseQuery{
    private Integer id;
    private Integer ownerId;
    private String name;
    //前端传来的是字符串类型的日期，需要将字符串转为java.util.date类型
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date startTime;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date endTime;
    private BigDecimal budget;
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /**
     * 【修复的既有缺陷】活动描述。
     * 原 ActivityQuery 缺少此字段，前端新增/编辑填写的 description 在参数绑定阶段即被丢弃，
     * 再叠加 Service 层 insert/edit 未 setDescription，导致活动描述无论如何都存不进库
     * （接口实测新建活动后 description 恒为空）。补齐该字段并贯通持久化链路。
     */
    private String description;

}
