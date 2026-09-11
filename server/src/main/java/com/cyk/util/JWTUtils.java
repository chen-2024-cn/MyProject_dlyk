package com.cyk.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cyk.model.TUser;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * jwt工具类
 *
 */
@Slf4j
public class JWTUtils {

    // 【安全修复】此处原硬编码默认密钥（即随 git 历史公开泄露的那一个），已移除。
    // 由 JwtConfig.setSecret 在启动时从环境变量 JWT_SECRET 注入；未注入则启动失败（fail-fast），
    // 确保泄露的旧密钥不会以任何形式被重新沿用。
    public static String SECRET;

    /**
     * 生成JWT （token）
     *
     * <p>【安全修复】JWT 本体必须携带过期声明（exp），与 Redis 会话 TTL 对齐：</p>
     * <ul>
     *   <li>旧版 JWT 永不过期，token 的生死完全依赖 Redis key —— 一旦 Redis 数据被
     *       恢复/迁移或校验链路被绕过，签发多年的旧 token 仍可验签通过；</li>
     *   <li>携带 exp 后，即使脱离 Redis，过期 token 也无法通过 {@link #verifyJWT}。</li>
     * </ul>
     *
     * @param userJSON      用户负载 JSON（<b>调用方必须保证不含密码哈希等敏感字段</b>）
     * @param expireSeconds 过期时长（秒），与写入 Redis 的 TTL 使用同一值，双端语义一致
     */
    public static String createJWT(String userJSON, long expireSeconds) {
        //组装头数据
        Map<String, Object> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        return JWT.create()
                //头部
                .withHeader(header)

                //负载
                .withClaim("user", userJSON)

                //过期时间：当前时间 + TTL（与 Redis 会话有效期对齐，防"僵尸 token"）
                .withExpiresAt(new Date(System.currentTimeMillis() + expireSeconds * 1000))

                //签名
                .sign(Algorithm.HMAC256(SECRET));
    }

    /**
     * 验证JWT
     *
     * @param jwt 要验证的jwt的字符串
     */
    public static Boolean verifyJWT(String jwt) {
        try {
            // 使用秘钥创建一个JWT验证器对象
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

            //验证JWT，如果没有抛出异常，说明验证通过，否则验证不通过
            jwtVerifier.verify(jwt);

            return true;
        } catch (Exception e) {
            // 验证失败场景（签名错误/过期等）属预期业务分支，记录 warn 即可；绝不上带令牌原文防泄露凭证。
            log.warn("JWT 验证未通过: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 解析JWT的数据
     *
     */
    public static void parseJWT(String jwt) {
        try {
            // 使用秘钥创建一个验证器对象
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

            //验证JWT，得到一个解码后的jwt对象
            DecodedJWT decodedJWT = jwtVerifier.verify(jwt);

            //通过解码后的jwt对象，就可以获取里面的负载数据
            Claim nickClaim = decodedJWT.getClaim("nick");
            Claim ageClaim = decodedJWT.getClaim("age");
            Claim phoneClaim = decodedJWT.getClaim("phone");
            Claim birthDayClaim = decodedJWT.getClaim("birthDay");


            String nick = nickClaim.asString();
            int age = ageClaim.asInt();
            String phone = phoneClaim.asString();
            Date birthDay = birthDayClaim.asDate();

            log.info("JWT 负载解析结果: nick={} -- age={} -- phone={} -- birthDay={}", nick, age, phone, birthDay);
        } catch (TokenExpiredException e) {
            log.error("JWT 解析失败：令牌已过期", e);
            throw new RuntimeException(e);
        }
    }

    public static TUser parseUserFromJWT(String jwt) {
        try {
            // 使用秘钥创建一个验证器对象
            JWTVerifier jwtVerifier = JWT.require(Algorithm.HMAC256(SECRET)).build();

            //验证JWT，得到一个解码后的jwt对象
            DecodedJWT decodedJWT = jwtVerifier.verify(jwt);

            //通过解码后的jwt对象，就可以获取里面的负载数据
            Claim userClaim = decodedJWT.getClaim("user");

            String userJSON = userClaim.asString();

            return JSONUtils.toBean(userJSON, TUser.class);
        } catch (TokenExpiredException e) {
            log.error("解析用户 JWT 失败：令牌已过期", e);
            throw new RuntimeException(e);
        }
    }
}
