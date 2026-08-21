package com.cyk.query;

import lombok.Data;

@Data
public class ClueRemarkQuery extends BaseQuery {

    private Integer id;

    private Integer clueId;

    private String noteContent;

    private Integer noteWay;
}
