package com.cyk.util;

import com.cyk.constants.Constants;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

/**
 * Refresh Token Cookie 的统一出口：登录、刷新、登出都经过这里，
 * 保证 HttpOnly / SameSite / Path 等安全属性在整个系统里只定义一份，不会写得不一致。
 */
public final class RefreshCookieHelper {

    private RefreshCookieHelper() {
    }

    /**
     * 写入 Refresh Cookie。
     *
     * @param persistent true=持久 Cookie（"记住我"，到期时间=ttlSeconds，关浏览器后仍然在）；
     *                   false=会话 Cookie（不设 Max-Age，关闭浏览器即消失）
     */
    public static void write(HttpServletResponse response, String refreshToken,
                             boolean persistent, long ttlSeconds, boolean secure) {
        ResponseCookie.ResponseCookieBuilder builder = ResponseCookie
                .from(Constants.REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)                       // JS 无法通过 document.cookie 读取，XSS 偷不走
                .secure(secure)                       // true 时仅 HTTPS 传输（生产必须开启）
                .sameSite("Strict")                   // 跨站发起的请求不携带，挡住 CSRF
                .path(Constants.REFRESH_COOKIE_PATH); // 只有 /api/auth/** 的请求才带上它
        if (persistent) {
            builder.maxAge(Duration.ofSeconds(ttlSeconds));
        }
        response.addHeader(HttpHeaders.SET_COOKIE, builder.build().toString());
    }

    /**
     * 清除 Refresh Cookie。浏览器按 "名称 + Path（+ Domain）" 定位要删的 Cookie，
     * 所以这里的 Path 必须和写入时完全一致，否则删不掉。
     */
    public static void clear(HttpServletResponse response, boolean secure) {
        ResponseCookie cookie = ResponseCookie
                .from(Constants.REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(secure)
                .sameSite("Strict")
                .path(Constants.REFRESH_COOKIE_PATH)
                .maxAge(0)                            // 立即过期 = 通知浏览器删除
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}