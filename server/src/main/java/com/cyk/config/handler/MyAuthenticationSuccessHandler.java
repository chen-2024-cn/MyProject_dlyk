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
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

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
        //2、写入Redis
        String key = Constants.REDIS_JWT_KEY + tUser.getId();
        redisService.setValue(key, jwt);

        System.out.println("redis的key====="+ key + "=====设置的jwt为" + jwt);

        //3、设置jwt的过期时间
        String rememberMe = request.getParameter("rememberMe");
        if (Boolean.parseBoolean(rememberMe)) {
            //勾选了记住我
            redisService.expire(key, Constants.EXPIRE_TIME, TimeUnit.SECONDS);
        } else {
            //没有勾选
            redisService.expire(key, Constants.DEFAULT_EXPIRE_TIME, TimeUnit.SECONDS);
        }

        // 4、清除登录失败和锁定缓存
        String failKey = "dlyk:login:fail:" + tUser.getLoginAct();
        String lockKey = "dlyk:login:lock:" + tUser.getLoginAct();
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
