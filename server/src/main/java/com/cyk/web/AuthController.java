package com.cyk.web;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cyk.constants.Constants;
import com.cyk.model.TUser;
import com.cyk.result.CodeEnum;
import com.cyk.result.R;
import com.cyk.service.RedisService;
import com.cyk.service.UserService;
import com.cyk.util.JWTUtils;
import com.cyk.util.LoginSession;
import com.cyk.util.RefreshCookieHelper;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 令牌刷新。登录由 Spring Security 的 formLogin 处理，退出由 logout 处理，这里只负责刷新。
 */
@Slf4j
@RestController
public class AuthController {
    @Resource
    private RedisService redisService;

    @Resource
    private UserService userService;

    @Value("${auth.cookie-secure:false}")
    private boolean cookieSecure;

    /**
     * 用 Cookie 里的 Refresh Token 换一对新令牌（轮换）。
     * 该接口已在过滤器和 SecurityConfig 中放行：它靠 Cookie 认证，请求里没有 Access Token。
     */
    @PostMapping(Constants.REFRESH_URI)
    public R refresh(@CookieValue(name = Constants.REFRESH_COOKIE_NAME, required = false) String refreshToken,
                     HttpServletResponse response) {
        // 901：没有 Cookie。未登录用户打开登录页时的正常情况，不需要清 Cookie
        if (!StringUtils.hasText(refreshToken)) {
            return R.FAIL(CodeEnum.TOKEN_IS_EMPTY);
        }
        //1.本地校验：签名、过期、用途（Refresh Token 不能冒充 Access Token）等
        DecodedJWT decoded;
        try {
            decoded = JWTUtils.verifyRefreshToken(refreshToken);
        } catch (TokenExpiredException e) {
            return fail(response, CodeEnum.TOKEN_IS_EXPIRED);   // Refresh 本身过期：会话寿命到头
        } catch (JWTVerificationException e) {
            log.warn("Refresh Token 校验未通过: {}", e.getMessage());
            return fail(response, CodeEnum.TOKEN_IS_ERROR);
        }
        Integer userId = Integer.valueOf(decoded.getSubject());// 解析 userId，确保它是整数，避免脏数据写进 Redis
        String sid = decoded.getClaim("sid").asString();
        Boolean rememberMe = Boolean.TRUE.equals(decoded.getClaim("rm").asBoolean());

        //2.远程校验：redis 里存的"当前有效会话"（sid + jti）是否与令牌一致
        String key = Constants.REDIS_JWT_KEY + userId;
        LoginSession session = LoginSession.parse((String) redisService.getValue(key));

        if (session == null) {                                   // 已退出、过期，或被管理员踢下线
            return fail(response, CodeEnum.TOKEN_IS_EXPIRED);
        }
        if (!session.sid().equals(sid)) {                        // 别处重新登录，本设备被顶下线
            return fail(response, CodeEnum.TOKEN_IS_ELSEWHERE);
        }
        if (!session.jti().equals(decoded.getId())) {
            // 重放检测：sid 对得上，jti 对不上 = 这是一张已经用过的旧 Refresh。
            // 无法分辨是被盗还是并发误用，宁可错杀：销毁整个会话
            redisService.delete(key);
            log.warn("检测到 Refresh Token 重放，已销毁会话 | userId={}", userId);
            return fail(response, CodeEnum.TOKEN_IS_NONE_MATCH);
        }

        // 3. 重新查库：权限变更最迟在这里生效；账号被禁用、锁定的，不再签发新令牌
        TUser user = userService.getUserInfo(userId);
        if (user == null || !user.isEnabled() || !user.isAccountNonLocked()
                || !user.isAccountNonExpired() || !user.isCredentialsNonExpired()) {
            redisService.delete(key);
            return fail(response, CodeEnum.TOKEN_IS_EXPIRED);
        }


        // 4. 轮换：签发新一对令牌，sid 不变（仍是同一次登录），jti 换新（旧 Refresh 就此作废）
        long refreshTtl = rememberMe ? Constants.EXPIRE_TIME : Constants.DEFAULT_EXPIRE_TIME;
        String newJti = UUID.randomUUID().toString();
        String accessToken = JWTUtils.createAccessToken(JWTUtils.toTokenUserJson(user), sid, Constants.ACCESS_TTL_SECONDS);
        String newRefresh = JWTUtils.createRefreshToken(userId, sid, newJti, rememberMe, refreshTtl);

        boolean rotated = redisService.compareAndSet(key, session.toRedisValue(),
                new LoginSession(sid, newJti).toRedisValue(), refreshTtl);
        if (!rotated) {
            // CAS 失败 = 读取之后 Redis 里的值被别人改过，先看现在是什么状态再决定怎么回应
            //CAS 失败后不能一律当重放处理。如果是另一台设备恰好在这几毫秒里登录，Redis 里已经是别人的新会话，一律删除就把人家刚登录的会话误杀了。所以要重新读一次，看 sid 对不对再决定。
            LoginSession now = LoginSession.parse((String) redisService.getValue(key));
            if (now == null) {
                return fail(response, CodeEnum.TOKEN_IS_EXPIRED);     // 期间被退出、被踢下线
            }
            if (!now.sid().equals(sid)) {
                return fail(response, CodeEnum.TOKEN_IS_ELSEWHERE);   // 别处刚登录，不能删人家的新会话
            }
            redisService.delete(key);                                 // 同一次登录里 jti 被抢先轮换：按重放处理
            log.warn("并发刷新竞争失败，按重放处理并销毁会话 | userId={}", userId);
            return fail(response, CodeEnum.TOKEN_IS_NONE_MATCH);
        }

        RefreshCookieHelper.write(response, newRefresh, rememberMe, refreshTtl, cookieSecure);

        return R.OK(accessToken);

    }

    /**
     * 靠 Refresh Cookie 退出，不需要 Access Token。
     * 用于 Access 已失效，或常规退出接口异常时的兜底：HttpOnly Cookie 只有服务端能清，
     * 不清的话，用户回到登录页会被残留的 Cookie 自动登录回来，"退出"看起来完全失灵。
     * 无论 Cookie 是否有效，最后都清除 Cookie，结果恒为成功（退出是幂等操作）。
     * CSRF：SameSite=Strict 已挡住跨站请求；何况被人强制退出的危害很小。
     */
    @PostMapping(Constants.LOGOUT_BY_COOKIE_URI)
    public R logoutByCookie(@CookieValue(name = Constants.REFRESH_COOKIE_NAME, required = false) String refreshToken,
                            HttpServletResponse response) {
        if (StringUtils.hasText(refreshToken)) {
            try {
                DecodedJWT decoded = JWTUtils.verifyRefreshToken(refreshToken);
                String key = Constants.REDIS_JWT_KEY + decoded.getSubject();
                LoginSession session = LoginSession.parse((String) redisService.getValue(key));
                // 和常规退出一致：只删自己这次登录的会话，不误删别的设备刚建立的会话
                if (session != null && session.sid().equals(decoded.getClaim("sid").asString())) {
                    redisService.delete(key);
                }
            } catch (JWTVerificationException e) {
                // 过期或被篡改：没有可删的会话，照样往下清 Cookie
            }
        }
        RefreshCookieHelper.clear(response, cookieSecure);
        return R.OK();
    }

    private R fail(HttpServletResponse response, CodeEnum codeEnum) {
        // 清除 Cookie，通知浏览器删除
        RefreshCookieHelper.clear(response, cookieSecure);
        return R.FAIL(codeEnum);
    }
}
