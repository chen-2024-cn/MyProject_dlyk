package com.cyk.service.impl;

import com.cyk.service.RedisService;
import jakarta.annotation.Resource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class RedisServiceImpl implements RedisService {
    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    @Override
    public void setValue(String key, String value) {
        redisTemplate.opsForValue().set(key,value);
    }

    @Override
    public Object getValue(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    @Override
    public Boolean removeValue(String key) {
        return redisTemplate.delete(key);
    }

    @Override
    public boolean expire(String key, Long timeout, TimeUnit timeUnit) {
       return redisTemplate.expire(key,timeout, timeUnit);
    }

    @Override
    public Long incr(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    @Override
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }


}
