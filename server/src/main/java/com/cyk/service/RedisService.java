package com.cyk.service;

import java.util.concurrent.TimeUnit;

public interface RedisService {
    void setValue(String key, String value);
    Object getValue(String key);
    Boolean removeValue(String key);
    boolean expire(String key, Long timeOut, TimeUnit timeUnit);//设置过期时间
    Long incr(String key);//自增
    Boolean hasKey(String key);//判断是否存在key
}
