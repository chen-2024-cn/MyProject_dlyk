package com.cyk.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    void setValue(String key, String value);

    /**
     * 原子写入：设置值的同时设置过期时间
     * 底层对应 Redis 的 SET key value EX timeout 命令，一条命令完成写入+过期，避免两步操作非原子
     * @param timeout 过期时长
     * @param unit    时间单位
     */
    void setValue(String key, String value, Long timeout, TimeUnit unit);

    Object getValue(String key);
    Boolean removeValue(String key);
    Boolean delete(String key); // 核心修复：引入通用的 delete 命名，避免第三方模块未对齐报错
    boolean expire(String key, Long timeOut, TimeUnit timeUnit);//设置过期时间
    Long incr(String key);//自增
    Boolean hasKey(String key);//判断是否存在key
}
