package com.cyk.result;

import lombok.*;
import org.aspectj.lang.annotation.Aspect;

@AllArgsConstructor
@NoArgsConstructor
public enum DicEnum {

    APPELLATION("appellation"),

    SOURCE("source"),

    STATE("clueState"),

    INTENTIONSTATE("intentionState"),

    NEEDLOAN("needLoan"),

    PRODUCT("product"),

    ACTIVITY("activity")

    ;

    @Setter
    @Getter
    private String code;
}
