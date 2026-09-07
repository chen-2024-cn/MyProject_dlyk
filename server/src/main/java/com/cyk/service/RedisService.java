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

    /**
     * 条件式原子写入（SET key value NX EX timeout）：仅当 key 不存在时写入并同时设置过期。
     * 用于缓存回填场景——并发回填时互不覆盖对方的最新结果（竞态防护）。
     * @return true 写入成功；false key 已存在、未写入
     */
    Boolean setValueIfAbsent(String key, String value, Long timeout, TimeUnit unit);

    Object getValue(String key);
    Boolean removeValue(String key);
    Boolean delete(String key); // 核心修复：引入通用的 delete 命名，避免第三方模块未对齐报错
    boolean expire(String key, Long timeOut, TimeUnit timeUnit);//设置过期时间
    Long incr(String key);//自增
    Boolean hasKey(String key);//判断是否存在key

    /**
     * 查询 key 的剩余存活时间（秒）：-2 表示 key 不存在；-1 表示 key 存在但未设置过期。
     * 用于缓存对账时校验 Redis TTL 与数据库有效期是否漂移。
     */
    Long getExpireSeconds(String key);
}
