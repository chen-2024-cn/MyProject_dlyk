package com.cyk.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Redis 会话值 "sid:jti" 的序列化与解析。纯 JUnit，不依赖 Spring。
 */
class LoginSessionTest {

    @Test
    void roundTrip_keepsSidAndJti() {
        LoginSession original = new LoginSession("sid-1", "jti-1");

        LoginSession parsed = LoginSession.parse(original.toRedisValue());

        assertEquals(original, parsed);
    }

    @Test
    void uuidValues_roundTrip() {
        String sid = java.util.UUID.randomUUID().toString();
        String jti = java.util.UUID.randomUUID().toString();

        LoginSession parsed = LoginSession.parse(new LoginSession(sid, jti).toRedisValue());

        assertEquals(sid, parsed.sid());
        assertEquals(jti, parsed.jti());
    }

    @Test
    void malformedValues_returnNull() {
        assertNull(LoginSession.parse(null));
        assertNull(LoginSession.parse(""));
        assertNull(LoginSession.parse("no-colon"));
        assertNull(LoginSession.parse(":only-jti"));
        assertNull(LoginSession.parse("only-sid:"));
    }

    @Test
    void firstColonIsTheSeparator() {
        LoginSession parsed = LoginSession.parse("sid:jti:extra");

        assertEquals("sid", parsed.sid());
        assertEquals("jti:extra", parsed.jti());
    }
}
