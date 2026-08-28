package com.cyk;

import com.cyk.constants.Constants;
import com.cyk.model.TUser;
import com.cyk.service.RedisService;
import com.cyk.util.JWTUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@Slf4j
@SpringBootTest
class ServerApplicationTests {

    @Resource
    private RedisService redisService;

    @Test
    void testRedisDataType() {
        String token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyIjoie1wiaWRcIjoxLFwibG9naW5BY3RcIjpcImFkbWluXCIsXCJsb2dpblB3ZFwiOlwiJDJhJDEwJHN4d2ZQUk9KbC9ocXNCSElxbkgwei40dUx0TFAzSll1UHFkR2YxUHZ0SjRRbi9xSzdMR2NlXCIsXCJuYW1lXCI6XCLnrqHnkIblkZhcIixcInBob25lXCI6XCIxMzcwMDAwMDAwMFwiLFwiZW1haWxcIjpcImFkbWluQHFxLmNvbVwiLFwiYWNjb3VudE5vRXhwaXJlZFwiOjEsXCJjcmVkZW50aWFsc05vRXhwaXJlZFwiOjEsXCJhY2NvdW50Tm9Mb2NrZWRcIjoxLFwiYWNjb3VudEVuYWJsZWRcIjoxLFwiY3JlYXRlVGltZVwiOjE3NzE3MjQyMzIwMDAsXCJjcmVhdGVCeVwiOjIsXCJlZGl0VGltZVwiOjE3NzU1MzQyNjgwMDAsXCJlZGl0QnlcIjoxLFwibGFzdExvZ2luVGltZVwiOjE3MDIyMTQzMzkwMDAsXCJyb2xlTGlzdFwiOltcImFkbWluXCJdLFwicGVybWlzc2lvbkxpc3RcIjpudWxsLFwiY3JlYXRlQnlVc2VyXCI6bnVsbCxcImVkaXRCeVVzZXJcIjpudWxsfSJ9.WX29hsaUOwkuXaApT3MLofknzgDj4Rk5J29p323DBkY";

        TUser tUser = JWTUtils.parseUserFromJWT(token);
        String redisKey = Constants.REDIS_JWT_KEY + tUser.getId();
        Object redisValue = redisService.getValue(redisKey);

        // 企业级测试规范：测试输出同样使用 SLF4J 日志，禁止直接 System.out.println。
        // 注意：Token 属于敏感凭证，诊断时只打印长度，不打印完整内容。
        log.info("========== Redis数据类型诊断 ==========");
        log.info("Redis Key: {}", redisKey);
        log.info("Redis Value是否为null: {}", redisValue == null);

        if (redisValue != null) {
            log.info("Redis Value的实际类型: {}", redisValue.getClass().getName());
            log.info("是否是String类型: {}", redisValue instanceof String);

            if (redisValue instanceof String) {
                String redisToken = (String) redisValue;
                log.info("✓ 数据类型正确，可以正常转换为String，Token长度: {}", redisToken.length());
            } else {
                log.warn("✗ 数据类型错误! 期望String，实际是: {}，这就是导致ClassCastException的原因!", redisValue.getClass().getName());
            }
        } else {
            log.warn("Redis中没有找到该key对应的值（token 可能已过期或环境未登录）");
        }
        log.info("=====================================");
    }

}
