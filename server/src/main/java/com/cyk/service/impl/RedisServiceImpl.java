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
    public void setValue(String key, String value, Long timeout, TimeUnit unit) {
        // 原子写入：底层对应 Redis 的 SET key value EX timeout，一条命令完成存储+设置过期
        // 相比先 set 再 expire 的两步写法，不存在中间宕机导致 key 永不过期的风险
        redisTemplate.opsForValue().set(key, value, timeout, unit);
    }

    @Override
    public Boolean setValueIfAbsent(String key, String value, Long timeout, TimeUnit unit) {
        // 条件式原子写入：底层对应 SET key value NX EX timeout，仅 key 不存在时写入
        // 并发回填场景下互不覆盖对方的最新结果（缓存竞态防护）
        return redisTemplate.opsForValue().setIfAbsent(key, value, timeout, unit);
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
    public Boolean delete(String key) {
        return redisTemplate.delete(key); // 调用原生的 redisTemplate.delete 实现
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

    @Override
    public Long getExpireSeconds(String key) {
        // -2：key 不存在；-1：存在但未设置过期（对账任务据此识别异常 key 并重新对齐 TTL）
        return redisTemplate.getExpire(key, TimeUnit.SECONDS);
    }

}
