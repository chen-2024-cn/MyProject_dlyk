package com.cyk.manager;

import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
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
