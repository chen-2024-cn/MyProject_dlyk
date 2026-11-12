package com.cyk.util;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cyk.model.TUser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 双 Token 的 JWT 工具类单测：纯 JUnit，不启动 Spring，不依赖 DB / Redis。
 */
class JWTUtilsTest {

    @BeforeAll
    static void initSecret() {
        // 正式运行时 SECRET 由 JwtConfig 从环境变量注入；单测不启动 Spring，手动赋值一个测试密钥
        JWTUtils.SECRET = "unit-test-secret-unit-test-secret-0123456789";
    }

    @Test
    void accessToken_signAndVerify_ok() {
        String jwt = JWTUtils.createAccessToken("{\"id\":1}", "sid-1", 60);

        DecodedJWT decoded = JWTUtils.verifyAccessToken(jwt);

        assertEquals("sid-1", decoded.getClaim("sid").asString());
        assertEquals("{\"id\":1}", decoded.getClaim("user").asString());
    }

    @Test
    void refreshToken_claimsRoundTrip() {
        String refresh = JWTUtils.createRefreshToken(7, "sid-9", "jti-9", true, 60);

        DecodedJWT decoded = JWTUtils.verifyRefreshToken(refresh);

        assertEquals("7", decoded.getSubject());
        assertEquals("sid-9", decoded.getClaim("sid").asString());
        assertEquals("jti-9", decoded.getId());
        assertTrue(decoded.getClaim("rm").asBoolean());
    }

    @Test
    void refreshToken_cannotBeUsedAsAccessToken() {
        String refresh = JWTUtils.createRefreshToken(1, "sid-1", "jti-1", true, 60);

        assertThrows(JWTVerificationException.class, () -> JWTUtils.verifyAccessToken(refresh));
    }

    @Test
    void accessToken_cannotBeUsedAsRefreshToken() {
        String access = JWTUtils.createAccessToken("{}", "sid-1", 60);

        assertThrows(JWTVerificationException.class, () -> JWTUtils.verifyRefreshToken(access));
    }

    @Test
    void expiredAccessToken_throwsTokenExpiredException() {
        // 有效期给负数 = 十秒前就已过期
        String expired = JWTUtils.createAccessToken("{}", "sid-1", -10);

        assertThrows(TokenExpiredException.class, () -> JWTUtils.verifyAccessToken(expired));
    }

    @Test
    void forgedPayload_isRejectedBySignature() {
        String jwt = JWTUtils.createAccessToken("{\"id\":1}", "sid-1", 60);
        String[] parts = jwt.split("\\.");

        // 攻击者把负载里的用户 id 改成 999，想冒充别人，但没有密钥，无法重新签名
        String forgedJson = "{\"tokenType\":\"access\",\"sid\":\"sid-1\",\"user\":\"{\\\"id\\\":999}\"}";
        String forgedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(forgedJson.getBytes(StandardCharsets.UTF_8));
        String forged = parts[0] + "." + forgedPayload + "." + parts[2];

        assertThrows(JWTVerificationException.class, () -> JWTUtils.verifyAccessToken(forged));
    }

    @Test
    void tokenUserJson_isWhitelist_noPasswordNoAuthorities() {
        TUser u = new TUser();
        u.setId(7);
        u.setLoginAct("alice");
        u.setName("爱丽丝");
        u.setLoginPwd("$2a$10$hash");
        u.setRoleList(List.of("admin"));
        u.setPermissionList(List.of("user:list"));

        String json = JWTUtils.toTokenUserJson(u);
        TUser back = JSONUtils.toBean(json, TUser.class);

        assertEquals(7, back.getId());
        assertEquals("alice", back.getLoginAct());
        assertEquals("爱丽丝", back.getName());
        assertNull(back.getLoginPwd());
        assertNull(back.getRoleList());
        assertNull(back.getPermissionList());
        assertFalse(json.contains("hash"));
        assertFalse(json.contains("admin"));
    }
}