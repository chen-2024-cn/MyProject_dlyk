package com.cyk;

import com.cyk.constants.Constants;
import com.cyk.model.TUser;
import com.cyk.util.JWTUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


@MapperScan(basePackages = {"com.cyk.mapper"})
@SpringBootApplication
public class ServerApplication {


    /**
     * 字典/产品/在途活动的本地缓存，由 {@link com.cyk.task.DataTask} 定时全量刷新，
     * {@link com.cyk.web.DicController} 并发读取。
     *
     * <p>【线程安全修复】旧版用 {@code HashMap} —— 定时任务线程 put 的同时多个请求线程
     * get，HashMap 非线程安全，并发读写可能读到不一致状态甚至（扩容时）死循环/丢数据。
     * 改为 {@link ConcurrentHashMap}：读无锁高并发，写分段安全，契合「单写多读」场景。</p>
     */
    public static final Map<String, Object> cacheMap = new ConcurrentHashMap<>();
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);

    }

}
