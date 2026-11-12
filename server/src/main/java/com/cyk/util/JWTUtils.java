package com.cyk.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cyk.model.TUser;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * jwt工具类
 *
 */
public class JWTUtils {

    // 【安全修复】此处原硬编码默认密钥（即随 git 历史公开泄露的那一个），已移除。
    // 由 JwtConfig.setSecret 在启动时从环境变量 JWT_SECRET 注入；未注入则启动失败（fail-fast），
    // 确保泄露的旧密钥不会以任何形式被重新沿用。
    public static String SECRET;


    /** 自定义声明名：标记令牌用途，防止 Refresh Token 冒充 Access Token（令牌混用攻击） */
    private static final String CLAIM_TOKEN_TYPE = "tokenType";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    /**
     * 签发 Access Token（短命，随请求头携带）。
     * 负载沿用原有的 user JSON，这样项目里约 15 处 parseUserFromJWT(token) 的调用点无需任何改动。
     *
     * @param userJSON      用户负载（调用方必须已脱敏，不含密码哈希）
     * @param sid           会话 ID，登录时生成，整个会话期间不变，用于单设备互斥判断
     * @param expireSeconds 有效期（秒）
     */
    public static String createAccessToken(String userJSON, String sid, long expireSeconds) {
        return JWT.create()
                .withClaim(CLAIM_TOKEN_TYPE, TYPE_ACCESS)
                .withClaim("sid", sid)
                .withClaim("user", userJSON)
                .withExpiresAt(new Date(System.currentTimeMillis() + expireSeconds * 1000))
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 签发 Refresh Token（长命，仅存于 HttpOnly Cookie）。
     * 只携带"我是谁 + 哪次会话 + 哪一张"，不带用户权限：权限在刷新时重新查库，
     * 这样管理员修改角色后，最迟下一次刷新就生效。
     *
     * @param userId     用户 ID，写入标准声明 sub
     * @param sid        会话 ID（与同会话的 Access 相同）
     * @param jti        本张 Refresh 的唯一 ID，每次轮换都换新，用于重放检测
     * @param rememberMe 是否"记住我"，轮换时需沿用，决定下一张 Refresh 的有效期
     */
    public static String createRefreshToken(Integer userId, String sid, String jti,
                                            boolean rememberMe, long expireSeconds) {
        return JWT.create()
                .withClaim(CLAIM_TOKEN_TYPE, TYPE_REFRESH)
                .withSubject(String.valueOf(userId))
                .withJWTId(jti)
                .withClaim("sid", sid)
                .withClaim("rm", rememberMe)
                .withExpiresAt(new Date(System.currentTimeMillis() + expireSeconds * 1000))
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 构造 Access Token 的用户负载。
     *
     * <p>JWT 只是 base64 编码，不是加密，拿到令牌的人（包括被 XSS 窃取）都能读到负载。
     * 所以这里用【白名单】：只放识别身份所需的 id / 账号 / 姓名。
     * 密码哈希、角色、权限、菜单一概不放——权限由 TokenVerifyFilter 每次请求向服务端取。</p>
     *
     * <p>为什么白名单优于“黑名单置空”：旧写法是先 setLoginPwd(null) 再整体序列化，
     * 以后有人给 TUser 加一个敏感字段，会悄悄进到每个令牌里；白名单不会。</p>
     */
    public static String toTokenUserJson(TUser user) {
        Map<String, Object> slim = new HashMap<>();   // 不用 Map.of：name 可能为 null，Map.of 遇 null 会抛异常
        slim.put("id", user.getId());
        slim.put("loginAct", user.getLoginAct());
        slim.put("name", user.getName());
        return JSONUtils.toJSON(slim);
    }

     /**
     * 校验 Access Token：验签 + 验过期 + 验用途。
     * 校验不通过直接抛异常（不吞掉），调用方据异常类型区分：
     * TokenExpiredException = 已过期（可刷新）；其它 JWTVerificationException = 被篡改/用途不符（不可刷新）。
     */
    public static DecodedJWT verifyAccessToken(String jwt) {
        return verifyWithType(jwt, TYPE_ACCESS);
    }

    /** 校验 Refresh Token，规则同上，用途必须是 refresh */
    public static DecodedJWT verifyRefreshToken(String jwt) {
        return verifyWithType(jwt, TYPE_REFRESH);
    }

    private static DecodedJWT verifyWithType(String jwt, String expectedType) {
        return JWT.require(Algorithm.HMAC256(SECRET))
                .withClaim(CLAIM_TOKEN_TYPE, expectedType)
                .build()
                .verify(jwt);
    }



    /**
     * 从 Access Token 里取出用户负载。走 verifyAccessToken，所以同样校验用途：
     * 拿 Refresh Token 来调用会被拒绝。
     */
    public static TUser parseUserFromJWT(String jwt) {
        String userJSON = verifyAccessToken(jwt).getClaim("user").asString();
        return JSONUtils.toBean(userJSON, TUser.class);
    }
}
