package com.cyk.util;

import org.springframework.util.ObjectUtils;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class CacheUtils {

    /**
     *  带有缓存的查询方法
     * @param cacheSelector
     * @param databaseSelector
     * @param saveCache
     * @return
     * @param <T>
     */
    public static <T> T getCacheData(Supplier<T> cacheSelector, Supplier<T> databaseSelector, Consumer<T> saveCache) {
        T data = cacheSelector.get();//从redis中查询
        if (ObjectUtils.isEmpty(data)) {//redis中没有
            data = databaseSelector.get();//从数据库中查询
            if (!ObjectUtils.isEmpty(data)) {//数据库中查询到
                //数据放入redis
                saveCache.accept(data);
            }
        }
        return data;
    }
}
