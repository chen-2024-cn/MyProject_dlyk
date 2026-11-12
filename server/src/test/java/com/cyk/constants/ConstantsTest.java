package com.cyk.constants;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 登录账号归一化：失败计数 key 与锁 key 共用，必须和数据库 login_act 的比较规则一致
 * （不区分大小写、忽略首尾空格），否则换写法就能绕过登录锁定。
 */
class ConstantsTest {

    @Test
    void differentSpellingsOfSameAccount_normalizeToSameKey() {
        assertEquals("admin", Constants.normalizeLoginAct("admin"));
        assertEquals("admin", Constants.normalizeLoginAct("Admin"));
        assertEquals("admin", Constants.normalizeLoginAct("ADMIN"));
        assertEquals("admin", Constants.normalizeLoginAct("admin "));
        assertEquals("admin", Constants.normalizeLoginAct("  Admin\t"));
    }

    @Test
    void nullBecomesEmptyString() {
        assertEquals("", Constants.normalizeLoginAct(null));
    }

    @Test
    void innerSpacesAreKept() {
        // 只去首尾空格：中间的空格是账号本身的一部分
        assertEquals("a b", Constants.normalizeLoginAct(" A B "));
    }
}
