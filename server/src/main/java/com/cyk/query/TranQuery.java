package com.cyk.query;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.util.Date;

@Data
public class TranQuery extends BaseQuery {

    private Integer id;

    private Integer customerId;

    private BigDecimal money;

    private Integer stage;

    /**
     * 【新增筛选项】交易流水号。
     *
     * <p>与 activity 模块的 name 模糊检索同理，流水号是销售日常最高频的精确定位手段
     * （客户报一个单号就要立刻查到该笔交易），故支持 like 模糊匹配。</p>
     *
     * <p><b>安全声明</b>：该字段主要服务于「列表检索」。由于 {@code TranQuery} 会被
     * {@code BeanUtils.copyProperties} 拷贝到 {@code TTran}，而 TTran 有同名字段，
     * 为避免外部通过编辑接口篡改业务主键，已在 {@code TranServiceImpl.updateTran}
     * 中显式将 tranNo 置空（流水号不可变更）。</p>
     */
    private String tranNo;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expectedDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextContactTime;

    private String description;
}
