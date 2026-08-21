package com.cyk.query;

import lombok.Data;

@Data
public class TranRemarkQuery extends BaseQuery {

    private Integer id;

    private Integer tranId;

    private String noteContent;

    private Integer noteWay;
}
