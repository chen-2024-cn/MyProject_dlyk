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

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date expectedDate;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date nextContactTime;

    private String description;
}
