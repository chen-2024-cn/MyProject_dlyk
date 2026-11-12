package com.cyk.config.handler;



import com.cyk.constants.Constants;
import com.cyk.model.TUser;
import com.cyk.result.R;
import com.cyk.service.RedisService;
import com.cyk.util.JSONUtils;
import com.cyk.util.JWTUtils;
import com.cyk.util.ResponseUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import com.cyk.util.LoginSession;
import com.cyk.util.RefreshCookieHelper;
import org.springframework.beans.factory.annotation.Value;
import java.util.UUID;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Resource
    RedisService redisService;

    @Resource
    private com.cyk.mapper.TUserMapper tUserMapper;

    @Value("${auth.cookie-secure:false}")
    private boolean cookieSecure;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //登录成功，执行该方法，在该方法中返回json给前端，就行了
        TUser tUser = (TUser) authentication.getPrincipal();

        //1、"记住我"现在只决定 Refresh Token（以及对应 Cookie 与 Redis 会话）的有效期；
        //   Access Token 固定 15 分钟，与"记住我"无关
        String key = Constants.REDIS_JWT_KEY + tUser.getId();
        boolean rememberMe = Boolean.parseBoolean(request.getParameter("rememberMe"));
        long refreshTtl = rememberMe ? Constants.EXPIRE_TIME : Constants.DEFAULT_EXPIRE_TIME;

        // 负载用白名单构造：只放 id / 账号 / 姓名，密码哈希、角色、权限、菜单一概不放
        String userJSON = JWTUtils.toTokenUserJson(tUser);

        //2、会话标识：sid 本次登录期间不变；jti 是首张 Refresh 的编号，之后每次刷新都会换新
        String sid = UUID.randomUUID().toString();
        String jti = UUID.randomUUID().toString();

        //3、签发双 Token
        String accessToken = JWTUtils.createAccessToken(userJSON, sid, Constants.ACCESS_TTL_SECONDS);
        String refreshToken = JWTUtils.createRefreshToken(tUser.getId(), sid, jti, rememberMe, refreshTtl);

        //4、Redis 记录"当前唯一有效的会话"。同一账号再次登录会覆盖这个值，
        //   旧设备的 sid / jti 立刻对不上 —— 单设备互斥语义保持不变
        redisService.setValue(key, new LoginSession(sid, jti).toRedisValue(), refreshTtl, TimeUnit.SECONDS);

        //5、Refresh Token 放进 HttpOnly Cookie，JS 永远读不到
        RefreshCookieHelper.write(response, refreshToken, rememberMe, refreshTtl, cookieSecure);

        // 安全提示：严禁把令牌明文写入日志，只记录 key、sid 前 8 位和有效期
        log.info("登录成功：用户[{}]，Redis Key：{}，sid：{}…，Refresh TTL：{} 秒，TTL类型：{}",
                tUser.getLoginAct(), key, sid.substring(0, 8), refreshTtl, rememberMe ? "记住我" : "普通会话");

        // 清除登录失败和锁定缓存。key 必须和失败处理器、loadUserByUsername 用同一个归一化规则，
        // 这里的 tUser 是数据库里查到的账号，不管用户当时输入的是 Admin 还是 admin，清掉的都是同一个 key
        String normalized = Constants.normalizeLoginAct(tUser.getLoginAct());
        redisService.removeValue(Constants.REDIS_LOGIN_FAIL_KEY + normalized);
        redisService.removeValue(Constants.REDIS_LOGIN_LOCK_KEY + normalized);

        // 记录最近登录时间（单列更新）。放在登录成功这里才名副其实：
        // 以前挂在 /api/login/info 上，每打开一次页面就更新一次，记录的其实是"最近打开页面的时间"
        tUserMapper.updateLastLoginTimeById(tUser.getId());

        //登录成功的统一结果：响应体里依然是 R.OK(token)，前端取 response.data.data 的写法不用改，
        //只是这个 token 现在是 15 分钟的 Access Token
        ResponseUtils.write(response, JSONUtils.toJSON(R.OK(accessToken)));
    }
}
