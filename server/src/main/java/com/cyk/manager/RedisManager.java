package com.cyk.manager;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.TimeUnit;
import com.cyk.util.JSONUtils;

@Component
public class RedisManager {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    public Object getValue(String key) {
       return redisTemplate.opsForList().range(key,0, -1);
    }
    public <T> void setValue( String key, Collection<T> data) {
        String json = JSONUtils.toJSON(data);
         redisTemplate.opsForValue().set(key, json);
    }

    /**
     * 带过期时间的原子写入（底层 SET key value EX timeout 一条命令完成存储+过期）：
     * 避免"写入"与"设置过期"两步非原子操作，同时杜绝缓存永不过期导致的
     * "数据库已变更、缓存永远脏"问题（配合写库成功后删缓存的 Cache-Aside 策略）。
     */
    public <T> void setValue(String key, Collection<T> data, long timeout, TimeUnit unit) {
        String json = JSONUtils.toJSON(data);
        redisTemplate.opsForValue().set(key, json, timeout, unit);
    }

    public <T> T getObjectValue(String key, Class<T> clazz) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        String json = value.toString();
        return JSONUtils.fromJSON(json, clazz);
    }

    public <T> List<T> getListValue(String key, Class<T> elementClass) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        String json = value.toString();
        return JSONUtils.fromJSONArray(json, elementClass);
    }
}
