package com.cyk.config.filter;


import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.cyk.constants.Constants;
import com.cyk.manager.AuthorityManager;
import com.cyk.model.TUser;
import com.cyk.result.CodeEnum;
import com.cyk.result.R;
import com.cyk.service.RedisService;
import com.cyk.util.JSONUtils;
import com.cyk.util.JWTUtils;
import com.cyk.util.LoginSession;
import com.cyk.util.ResponseUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
public class TokenVerifyFilter extends OncePerRequestFilter {

    @Resource
    private RedisService redisService;

    @Resource
    private AuthorityManager authorityManager;


    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String requestURI = request.getRequestURI();
        
        // 核心修复：放行公开的匿名请求（登录、注册、找回密码邮件验证码、修改密码提交）
        // 刷新接口靠 HttpOnly Cookie 里的 Refresh Token 认证，请求里没有 Access Token，必须放行
        if (requestURI.equals(Constants.LOGIN_URI)
                || requestURI.equals(Constants.REFRESH_URI)
                || requestURI.equals(Constants.LOGOUT_BY_COOKIE_URI)
                || requestURI.equals("/api/register")
                || requestURI.equals("/api/password/reset/code")
                || requestURI.equals("/api/password/reset")
                || requestURI.equals("/api/system/info/public")) {
            // 验证通过，不做JWT校验，让Filter链继续执行
            filterChain.doFilter(request, response);

        } else {
            String token = request.getHeader(Constants.TOKEN_NAME);

            //901 没带令牌
            if (!StringUtils.hasText(token)) {
                //token验证未通过的统一结果
                reject(response, CodeEnum.TOKEN_IS_EMPTY);
                return;
            }

            // 校验签名、过期时间、令牌用途（必须是 access）
            DecodedJWT decoded;
            try {
                decoded = JWTUtils.verifyAccessToken(token);
            } catch (TokenExpiredException e) {
                //906 签名正确，但过期了
                reject(response, CodeEnum.ACCESS_TOKEN_EXPIRED);
                return;
            } catch (JWTVerificationException e) {
                // 902：被篡改、格式损坏、拿 Refresh 冒充 Access 等。刷新也救不了，只能重新登录
                // 只记录异常描述，绝不把令牌原文写进日志
                log.warn("Access Token 校验未通过: {}", e.getMessage());
                reject(response, CodeEnum.TOKEN_IS_ERROR);
                return;
            }


            // 直接复用上面验过的 decoded，不再调用 parseUserFromJWT（那会把 HMAC 验签重复算一遍）
            TUser tUser = JSONUtils.toBean(decoded.getClaim("user").asString(), TUser.class);

            LoginSession session = LoginSession.parse((String)redisService.getValue(Constants.REDIS_JWT_KEY + tUser.getId()));

            // 903：Redis 里没有会话（已退出、已过期、被管理员踢下线，或是升级前的旧格式数据）
            if (session == null) {
                reject(response, CodeEnum.TOKEN_IS_EXPIRED);
                return;
            }

            // 905：单设备登录互斥。令牌本身合法，但会话已被别处的新登录替换（sid 对不上）
            if (!session.sid().equals(decoded.getClaim("sid").asString())) {
                reject(response, CodeEnum.TOKEN_IS_ELSEWHERE);
                return;
            }

            // 令牌只证明"你是谁"。角色与权限每次请求按用户 ID 向服务端取，不信任令牌里的任何权限信息。
            // 放在所有廉价校验（901/902/906/903/905）之后：被拒绝的请求不会白白查库
            authorityManager.fill(tUser);

            //jwt验证通过了，那么在spring security的上下文环境中要设置一下，设置当前这个人是登录过的，你后续不要再拦截他了
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(tUser, tUser.getLoginPwd(), tUser.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);


            //验证jwt通过了 ，让Filter链继续执行，也就是继续执行下一个Filter
            filterChain.doFilter(request, response);
        }
    }

    //认证失败统一接口
    private void reject(HttpServletResponse response, CodeEnum codeEnum) throws IOException {
        R result = R.FAIL(codeEnum);
        String resultJSON = JSONUtils.toJSON(result);
        ResponseUtils.write(response, resultJSON);
    }
}
