package com.cyk.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 统一封装web层向前端页面返回的结果
 *
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
public class R {

    //表示返回的结果码，比如200成功，500失败
    private int code;

    //表示返回的结果信息，比如 用户登录状态失效了，请求参数格式有误.......
    private String msg;

    //表示返回的结果数据，数据可能是一个对象，也可以是一个List集合.....
    private Object data;

    public static R OK() {
        return R.builder()
                .code(CodeEnum.OK.getCode())
                .msg(CodeEnum.OK.getMsg())
                .build();
    }

    public static R OK(int code, String msg) {
        return R.builder()
                .code(code)
                .msg(msg)
                .build();
    }

    public static R OK(Object data) {
        return R.builder()
                .code(CodeEnum.OK.getCode())
                .msg(CodeEnum.OK.getMsg())
                .data(data)
                .build();
    }

    public static R OK(CodeEnum codeEnum) {
        return R.builder()
                .code(CodeEnum.OK.getCode())
                .msg(codeEnum.getMsg())
                .build();
    }

    public static R FAIL() {
        return R.builder()
                .code(CodeEnum.FAIL.getCode())
                .msg(CodeEnum.FAIL.getMsg())
                .build();
    }

    public static R FAIL(String msg) {
        return R.builder()
                .code(CodeEnum.FAIL.getCode())
                .msg(msg)
                .build();
    }

    public static R FAIL(CodeEnum codeEnum) {
        return R.builder()
                .code(codeEnum.getCode())
                .msg(codeEnum.getMsg())
                .build();
    }
}

