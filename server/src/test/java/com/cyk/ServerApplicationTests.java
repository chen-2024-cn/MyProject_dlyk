package com.cyk;

import com.cyk.constants.Constants;
import com.cyk.model.TUser;
import com.cyk.service.RedisService;
import com.cyk.util.JWTUtils;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

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

        System.out.println("========== Redis数据类型诊断 ==========");
        System.out.println("Redis Key: " + redisKey);
        System.out.println("Redis Value是否为null: " + (redisValue == null));

        if (redisValue != null) {
            System.out.println("Redis Value的实际类型: " + redisValue.getClass().getName());
            System.out.println("Redis Value的内容: " + redisValue);
            System.out.println("是否是String类型: " + (redisValue instanceof String));

            if (redisValue instanceof String) {
                System.out.println("✓ 数据类型正确,可以正常转换为String");
                String redisToken = (String) redisValue;
                System.out.println("转换后的Token长度: " + redisToken.length());
            } else {
                System.out.println("✗ 数据类型错误!期望String,实际是: " + redisValue.getClass().getName());
                System.out.println("这就是导致ClassCastException的原因!");
            }
        } else {
            System.out.println("Redis中没有找到该key对应的值");
        }
        System.out.println("=====================================");
    }

}
