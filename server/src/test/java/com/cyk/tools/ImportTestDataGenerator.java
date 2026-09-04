package com.cyk.tools;

import com.alibaba.excel.EasyExcel;
import com.cyk.result.ai.UserImportRow;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * AI 批量导入测试数据生成器（独立 main 方法，不依赖 Spring 上下文）。
 *
 * 用途：生成与「用户批量导入模板」（UserImportRow）表头完全一致的测试 Excel，
 * 直接通过前端「上传 Excel」→ 发送「把我刚才上传的Excel附件批量导入用户」指令，
 * 即可验证 AdminAgentToolkit.importUsersFromAttachment 全链路。
 *
 * 运行方式（在 server 目录下）：
 *   mvnw test-compile -q
 *   mvnw exec:java -Dexec.classpathScope=test -Dexec.mainClass=com.cyk.tools.ImportTestDataGenerator
 * 或直接在 IDE 中运行本类的 main 方法。
 *
 * 测试数据说明：
 * - 前 8 行为正常可导入数据（密码留空 → 系统使用统一初始密码 Dlyk@2026）；
 * - 第 9-10 行为边界场景（账号已存在/空账号），用于验证跳过逻辑；
 * - 若账号 test01~test08 已存在，重复运行不会造成脏数据（导入端幂等跳过）。
 */
public class ImportTestDataGenerator {

    public static void main(String[] args) {
        String outPath = args.length > 0 ? args[0]
                : new File("db").getAbsolutePath() + File.separator + "ai-import-testdata.xlsx";

        List<UserImportRow> rows = new ArrayList<>();

        // ========== 正常数据 8 行（覆盖姓名/电话/邮箱各种组合） ==========
        rows.add(build("testuser01", null, "测试用户甲", "13800000001", "test01@dlyk.com"));
        rows.add(build("testuser02", null, "测试用户乙", "13800000002", "test02@dlyk.com"));
        rows.add(build("testuser03", "Dlyk@2026", "测试用户丙", "13800000003", "test03@dlyk.com"));
        rows.add(build("testuser04", null, "测试用户丁", "13800000004", null));
        rows.add(build("testuser05", null, "市场部专员", "13800000005", "mkt05@dlyk.com"));
        rows.add(build("testuser06", null, "销售部专员", "13800000006", "sale06@dlyk.com"));
        rows.add(build("testuser07", null, "客服部专员", "13800000007", "cs07@dlyk.com"));
        rows.add(build("testuser08", "Test@8888", "财务部专员", "13800000008", "fin08@dlyk.com"));

        // ========== 边界场景 ==========
        // 场景1：登录账号与系统已有账号重复（如 admin 存在则被跳过）——用于验证幂等跳过
        rows.add(build("admin", null, "重复账号验证", "13800000009", "dup@dlyk.com"));
        // 场景2：登录账号为空——用于验证必填项缺失行的跳过逻辑
        rows.add(build(null, null, "空账号验证", "13800000010", "empty@dlyk.com"));

        new File(outPath).getParentFile().mkdirs();
        EasyExcel.write(outPath, UserImportRow.class).sheet("用户导入").doWrite(rows);

        System.out.println("测试 Excel 已生成：" + outPath);
        System.out.println("共 " + rows.size() + " 行（8 行正常数据 + 2 行边界场景）。");
        System.out.println("密码说明：密码列为空的用户，导入后使用初始密码 " +
                com.cyk.constants.Constants.AI_IMPORT_DEFAULT_PASSWORD + " 登录。");
    }

    private static UserImportRow build(String loginAct, String pwd, String name, String phone, String email) {
        UserImportRow row = new UserImportRow();
        row.setLoginAct(loginAct);
        row.setLoginPwd(pwd);
        row.setName(name);
        row.setPhone(phone);
        row.setEmail(email);
        return row;
    }
}
