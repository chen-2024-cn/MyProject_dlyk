package com.cyk.commons;

import java.lang.annotation.*;

/**
 * 数据范围注解
 */
@Target(ElementType.METHOD)//方法上
@Retention(RetentionPolicy.RUNTIME)//运行时生效
@Documented
public @interface DataScope {
    //在sql语句的末尾添加一个过滤条件
    //select * from t_user （管理员）
    //select * from t_user tu where tu.id = ... （普通用户）

    //select * from t_activity （管理员）
    //select * from t_activity ta where ta.owner_id = ... （普通用户）

    /**
     * 表的别名
     */
    public String tableAlias() default "";

    /**
     * 表的字段名
     */
    public String tableField() default "";
}
