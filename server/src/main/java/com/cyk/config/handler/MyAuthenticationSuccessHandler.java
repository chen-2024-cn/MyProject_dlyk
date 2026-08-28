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

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class MyAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    @Resource
    RedisService redisService;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //登录成功，执行该方法，在该方法中返回json给前端，就行了
        TUser tUser = (TUser) authentication.getPrincipal();

        //将用户对象tUser转json作为负载数据放入jwt
        String userJSON = JSONUtils.toJSON(tUser);
        //1、生成jwt
        String jwt = JWTUtils.createJWT(userJSON);
        //2、写入Redis，根据“记住我”决定过期时间，并原子写入（SET + EX 一条命令完成）
        String key = Constants.REDIS_JWT_KEY + tUser.getId();
        String rememberMe = request.getParameter("rememberMe");
        long ttl = Boolean.parseBoolean(rememberMe) ? Constants.EXPIRE_TIME : Constants.DEFAULT_EXPIRE_TIME;
        redisService.setValue(key, jwt, ttl, TimeUnit.SECONDS);

        // 安全提示：严禁将完整的 JWT 明文写入日志（等同于泄露登录凭证），
        // 只记录 Redis Key 与 JWT 的摘要长度，便于排查单设备登录问题且不留存敏感值。
        log.info("登录成功：用户[{}]，Redis Token Key：{}，JWT长度：{}，TTL：{} 秒，TTL类型：{}",
                tUser.getLoginAct(), key, jwt.length(), ttl, Boolean.parseBoolean(rememberMe) ? "记住我" : "普通会话");

        // 3、清除登录失败和锁定缓存
        String failKey = Constants.REDIS_LOGIN_FAIL_KEY + tUser.getLoginAct();
        String lockKey = Constants.REDIS_LOGIN_LOCK_KEY + tUser.getLoginAct();
        redisService.removeValue(failKey);
        redisService.removeValue(lockKey);

        //登录成功的统一结果
        R result = R.OK(jwt);

        //把R对象转成json
        String resultJSON = JSONUtils.toJSON(result);

        //把R以json返回给前端
        ResponseUtils.write(response, resultJSON);
    }
}
