package com.cyk.config;

import com.cyk.util.JWTUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT属性配置器，用于将application.yml中的安全密钥注入到工具类的静态字段中
 */
@Configuration
public class JwtConfig {

    // 不设默认兜底：缺少 JWT_SECRET 环境变量时占位符解析失败 → 启动被拒绝（fail-fast），
    // 从根本上杜绝「已泄露的硬编码默认密钥」继续藏在源码中生效。
    @Value("${jwt.secret}")
    public void setSecret(String secret) {
        JWTUtils.SECRET = secret;
    }
}
