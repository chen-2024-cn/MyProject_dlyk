package com.cyk.result.ai;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.util.Date;

/**
 * AI 管理员工具 · 用户数据导出行模型
 * 仅供 EasyExcel 写出使用，字段与表头一一对应
 */
@Data
public class UserExportRow {

    @ExcelProperty("用户ID")
    private Integer id;

    @ExcelProperty("登录账号")
    private String loginAct;

    @ExcelProperty("用户姓名")
    private String name;

    @ExcelProperty("手机号")
    private String phone;

    @ExcelProperty("邮箱")
    private String email;

    @ExcelProperty("账号状态")
    private String accountStatus;

    @ExcelProperty("角色")
    private String roleNames;

    @ExcelProperty("创建时间")
    private Date createTime;
}
