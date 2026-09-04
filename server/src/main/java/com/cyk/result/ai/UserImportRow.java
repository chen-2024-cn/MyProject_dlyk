package com.cyk.result.ai;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

/**
 * AI 管理员工具 · 用户批量导入行模型
 * 仅登录账号为必填列；密码为空时系统使用统一初始密码
 */
@Data
public class UserImportRow {

    @ExcelProperty("登录账号(必填)")
    private String loginAct;

    @ExcelProperty("登录密码(选填)")
    private String loginPwd;

    @ExcelProperty("用户姓名")
    private String name;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;
}
