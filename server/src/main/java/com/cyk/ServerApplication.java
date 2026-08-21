package com.cyk;

import com.cyk.constants.Constants;
import com.cyk.model.TUser;
import com.cyk.util.JWTUtils;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.HashMap;
import java.util.Map;


@MapperScan(basePackages = {"com.cyk.mapper"})
@SpringBootApplication
public class ServerApplication {


    public static final Map<String, Object> cacheMap = new HashMap<>();
    public static void main(String[] args) {
        SpringApplication.run(ServerApplication.class, args);


    }

}
