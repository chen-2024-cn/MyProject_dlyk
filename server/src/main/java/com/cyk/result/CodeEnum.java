package com.cyk.result;

import lombok.*;

@Getter
@RequiredArgsConstructor
@NoArgsConstructor
@AllArgsConstructor
public enum CodeEnum {

    OK(200, "成功"),

    FAIL(500, "失败"),

    TOKEN_IS_EMPTY(901, "请求Token参数为空"),

    TOKEN_IS_ERROR(902, "请求Token有误"),

    TOKEN_IS_EXPIRED(903, "请求Token已过期"),

    TOKEN_IS_NONE_MATCH(904, "请求Token不匹配"),

    // 单设备登录互斥：同一账号在另一台设备重新登录，当前设备被顶下线
    TOKEN_IS_ELSEWHERE(905, "您的账号已在其他设备登录"),

    USER_LOGOUT(200, "退出成功"),

    DATA_ACCESS_EXCEPTION(500,"数据库操作失败"),

    ACCESS_DENIED(500, "权限不足"),

    DUPLICATE_EXCEPTION(500, "邮箱或者电话重复"),

    // ------------------------------------------------------------------
    // AI 领航员增值付费体系业务码（3000 段）
    // ------------------------------------------------------------------

    /** 能力不存在或未上架 */
    AI_ABILITY_NOT_FOUND(3001, "指定的增值能力不存在或未上架"),

    /** 订单不存在或不属于当前登录人（防横向越权） */
    AI_ORDER_NOT_FOUND(3002, "订单不存在或无权操作"),

    /** 订单状态不允许当前操作（重复支付/重复取消等） */
    AI_ORDER_STATUS_ILLEGAL(3003, "订单状态不允许该操作"),

    /** 支付网关处理失败（幂等冲突、状态推进失败等） */
    AI_PAYMENT_FAILED(3004, "支付网关处理失败，请稍后重试")
    ;

    //结果码
    private int code;

    //结果信息
    @NonNull
    private String msg;

}
