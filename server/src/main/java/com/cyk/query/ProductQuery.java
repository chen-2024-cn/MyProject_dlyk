package com.cyk.query;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductQuery extends BaseQuery {

    private Integer id;

    private String name;

    private BigDecimal guidePriceS;

    private BigDecimal guidePriceE;

    private BigDecimal quotation;

    private Integer state;
}
