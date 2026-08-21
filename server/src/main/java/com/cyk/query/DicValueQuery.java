package com.cyk.query;

import lombok.Data;

@Data
public class DicValueQuery extends BaseQuery {

    private Integer id;

    private String typeCode;

    private String typeValue;

    private Integer order;

    private String remark;
}
