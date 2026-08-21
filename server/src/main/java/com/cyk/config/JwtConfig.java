package com.cyk.config;

import com.cyk.util.JWTUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT属性配置器，用于将application.yml中的安全密钥注入到工具类的静态字段中
 */
@Configuration
public class JwtConfig {

    @Value("${jwt.secret:dY8300olWQ3345;1d<3w48}")
    public void setSecret(String secret) {
        JWTUtils.SECRET = secret;
    }
}
