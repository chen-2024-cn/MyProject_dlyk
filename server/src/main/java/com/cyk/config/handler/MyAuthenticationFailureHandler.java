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
import org.springframework.security.authentication.AccountExpiredException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.CredentialsExpiredException;
import org.springframework.security.authentication.DisabledException;
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
        // 密码输错是预期内的业务分支，不是系统错误：用 warn 且不带堆栈，
        // 否则每输错一次密码就刷一屏堆栈，真正的错误会被淹没
        log.warn("登录失败：{}", exception.getMessage());

        String username = request.getParameter("loginAct");
        String outputMessage = exception.getMessage();

        if (username != null && !username.trim().isEmpty()) {
            String normalized = Constants.normalizeLoginAct(username);
            String failKey = Constants.REDIS_LOGIN_FAIL_KEY + normalized;
            String lockKey = Constants.REDIS_LOGIN_LOCK_KEY + normalized;

            // 1. Redis 里有锁：连续输错密码导致的临时锁定（30 分钟后自动解除）
            if (redisService.hasKey(lockKey)) {
                outputMessage = "该账号因连续多次密码输入错误已被锁定，请在大约 30 分钟后再试！";
            } else if (exception instanceof LockedException) {
                // 没有 Redis 锁却抛了 LockedException：是数据库里 account_no_locked=0，管理员手动锁定的，
                // 等 30 分钟没用，不能给"稍后再试"的误导文案
                outputMessage = "该账号已被管理员锁定，请联系管理员处理。";
            } else if (exception instanceof DisabledException) {
                outputMessage = "该账号已被禁用，请联系管理员处理。";
            } else if (exception instanceof AccountExpiredException || exception instanceof CredentialsExpiredException) {
                outputMessage = "该账号或密码已过期，请联系管理员处理。";
            } else if (exception instanceof BadCredentialsException) {
                // 2. 只有密码错误或账号不存在（Spring Security 将 UsernameNotFound 统一包装为 BadCredentialsException）才算作登录失败尝试
                Long currentFailCount = redisService.incr(failKey); // 增加失败次数

                // INCR 和 EXPIRE 是两条命令：进程恰好在两条之间挂掉，计数器就永远没有过期时间，
                // 这个账号从此"失败一次少一次机会"。所以不能只在第一次设置，
                // 每次都检查 TTL，发现是 -1（key 存在但没设过期）就补上，让计数器自愈
                if (currentFailCount == 1 || Long.valueOf(-1).equals(redisService.getExpireSeconds(failKey))) {
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
