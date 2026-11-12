package com.cyk.util;

import com.cyk.constants.Constants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Refresh Cookie 的安全属性。用 MockHttpServletResponse，不启动 Spring。
 */
class RefreshCookieHelperTest {

    private static String setCookie(MockHttpServletResponse response) {
        return response.getHeader("Set-Cookie");
    }

    @Test
    void persistentCookie_hasAllSecurityAttributesAndMaxAge() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshCookieHelper.write(response, "token-value", true, 604800, false);

        String header = setCookie(response);
        assertTrue(header.startsWith(Constants.REFRESH_COOKIE_NAME + "=token-value"));
        assertTrue(header.contains("HttpOnly"));
        assertTrue(header.contains("SameSite=Strict"));
        assertTrue(header.contains("Path=" + Constants.REFRESH_COOKIE_PATH));
        assertTrue(header.contains("Max-Age=604800"));
        assertFalse(header.contains("Secure"));
    }

    @Test
    void sessionCookie_hasNoMaxAge() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshCookieHelper.write(response, "token-value", false, 3600, false);

        assertFalse(setCookie(response).contains("Max-Age"));
    }

    @Test
    void secureFlag_followsParameter() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshCookieHelper.write(response, "token-value", false, 3600, true);

        assertTrue(setCookie(response).contains("Secure"));
    }

    @Test
    void clear_expiresImmediatelyWithSamePathAsWrite() {
        MockHttpServletResponse response = new MockHttpServletResponse();

        RefreshCookieHelper.clear(response, false);

        String header = setCookie(response);
        assertTrue(header.startsWith(Constants.REFRESH_COOKIE_NAME + "="));
        assertTrue(header.contains("Max-Age=0"));
        // Path 必须和写入时一致，否则浏览器按"名称 + Path"定位不到要删的 Cookie
        assertTrue(header.contains("Path=" + Constants.REFRESH_COOKIE_PATH));
        assertTrue(header.contains("HttpOnly"));
    }
}
