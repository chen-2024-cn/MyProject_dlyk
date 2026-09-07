package com.cyk.config.handler;


import com.cyk.constants.Constants;
import com.cyk.model.TUser;
import com.cyk.result.CodeEnum;
import com.cyk.result.R;

import com.cyk.service.RedisService;
import com.cyk.util.JSONUtils;
import com.cyk.util.ResponseUtils;
import jakarta.annotation.Resource;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;

/**
 * 退出成功处理器
 *
 */
@Component
public class MyLogoutSuccessHandler implements LogoutSuccessHandler {


    @Resource
    private RedisService redisService;

    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        //退出成功，执行该方法，在该方法中返回json给前端，就行了
        TUser tUser = (TUser)authentication.getPrincipal();

        // 单设备登录互斥保护：只有"当前退出设备的token仍是Redis中的最新token"才清除登录态。
        // 否则说明该账号已在其他设备重新登录（当前设备是被顶掉的旧设备），
        // 若此处无条件删除，会把新设备的有效token误删，导致新登录的设备也被迫下线。
        String key = Constants.REDIS_JWT_KEY + tUser.getId();
        String currentToken = request.getHeader(Constants.TOKEN_NAME);
        String redisToken = (String) redisService.getValue(key);
        if (StringUtils.hasText(redisToken) && redisToken.equals(currentToken)) {
            redisService.removeValue(key);
        }

        // AI 聊天记录与登录态同生命周期：退出登录即永久清除。
        // 注意此处不做"最新token"校验——聊天记录绑定的是登录人本身而非设备，
        // 任何一次真实退出都意味着该登录人主动结束了本次登录会话。
        redisService.removeValue(Constants.REDIS_AI_CHAT_HISTORY_KEY + tUser.getId());

        //退出成功的统一结果
        R result = R.OK(CodeEnum.USER_LOGOUT);

        //把R对象转成json
        String resultJSON = JSONUtils.toJSON(result);

        //把R以json返回给前端
        ResponseUtils.write(response, resultJSON);
    }
}
