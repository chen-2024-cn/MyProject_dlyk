package com.cyk.config.handler;

import com.cyk.constants.Constants;
import com.cyk.result.R;
import com.cyk.service.RedisService;
import com.cyk.util.JSONUtils;
import com.cyk.util.ResponseUtils;

import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * 登录失败的处理器
 *
 */
@Component
public class MyAuthenticationFailureHandler implements AuthenticationFailureHandler {

    @Resource
    private RedisService redisService;//注入redis

    private static final int MAX_LOGIN_FAIL_COUNT = 5; // 最大登录失败次数

    private static final Logger log = LoggerFactory.getLogger(MyAuthenticationFailureHandler.class);
    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.error("登录失败", exception);  // 打印具体异常

        String username = request.getParameter("loginAct");
        String outputMessage = exception.getMessage();

        if (username != null && !username.trim().isEmpty()) {
            String failKey = Constants.REDIS_LOGIN_FAIL_KEY + username;
            String lockKey = Constants.REDIS_LOGIN_LOCK_KEY + username;

            // 1. 如果账户已经被锁定，或者抛出的是 LockedException，则提示锁定
            if (exception instanceof LockedException || redisService.hasKey(lockKey)) {
                outputMessage = "该账号因连续多次密码输入错误已被锁定，请在大约 30 分钟后再试！";
            } else if (exception instanceof BadCredentialsException) {
                // 2. 只有密码错误或账号不存在（Spring Security 将 UsernameNotFound 统一包装为 BadCredentialsException）才算作登录失败尝试
                Long currentFailCount = redisService.incr(failKey); // 增加失败次数

                if (currentFailCount == 1) {
                    redisService.expire(failKey, Constants.LOGIN_FAIL_EXPIRE_MINUTES, TimeUnit.MINUTES); // 计数器10分钟后失效
                }

                if (currentFailCount >= MAX_LOGIN_FAIL_COUNT) {
                    // 锁定账号 30分钟：改为原子写入（SET + 过期时间一条命令完成），消除原 setValue+expire 两步非原子风险
                    redisService.setValue(lockKey, "LOCKED", Constants.LOGIN_LOCK_EXPIRE_MINUTES, TimeUnit.MINUTES);
                    redisService.removeValue(failKey); // 锁定后清空失败次数计数器开始新一轮
                    outputMessage = "密码输入错误已达 " + MAX_LOGIN_FAIL_COUNT + " 次，账户已被锁定30分钟！";
                } else {
                    outputMessage = "用户名或密码错误！您还可以尝试 " + (MAX_LOGIN_FAIL_COUNT - currentFailCount) + " 次。";
                }
            }
        }

        //登录失败的统一结果
        R result = R.FAIL(outputMessage);

        //把R对象转成json
        String resultJSON = JSONUtils.toJSON(result);

        //把R以json返回给前端
        ResponseUtils.write(response, resultJSON);
    }
}
