package com.cyk.query;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class BaseQuery {
    //jwt
    String token;

    //数据权限的sql过滤条件，等价于 tu.id = 2 或 ta.owner_id = 2
    String filterSQL;
}
