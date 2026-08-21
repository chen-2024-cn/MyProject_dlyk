package com.cyk.util;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisUtil {

    /**
     * RedisTemplate 默认使用 JdkSerializationRedisSerializer 对键和值进行序列化。这意味着：
     * 键（Key）：传入的 String 会被序列化为 Java 对象字节数组，然后作为 Redis 键存储。因此你通过 redis-cli 直接查看时，看到的键可能是类似 \xac\xed\x00\x05t\x00\x04name 的乱码。
     * 值（Value）：传入的 Object 也会被序列化为字节数组，存入 Redis 字符串值中，所以用 GET 命令看到的是二进制数据，不是原始字符串。
     * 如果希望存储为普通字符串（便于调试或跨语言使用），可以配置 StringRedisSerializer：
     * @param factory
     * @return
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }
}
