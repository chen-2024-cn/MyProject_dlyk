package com.cyk.config;

import com.cyk.config.filter.TokenVerifyFilter;
import com.cyk.config.handler.MyAccessDeniedHandler;
import com.cyk.config.handler.MyAuthenticationFailureHandler;
import com.cyk.config.handler.MyAuthenticationSuccessHandler;
import com.cyk.config.handler.MyLogoutSuccessHandler;
import com.cyk.constants.Constants;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.annotation.web.configurers.RequestCacheConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

@EnableMethodSecurity//开启方法级别的权限检查
@Configuration
public class SecurityConfig {

    @Resource
    private MyAuthenticationSuccessHandler myAuthenticationSuccessHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Resource
    private MyLogoutSuccessHandler myLogoutSuccessHandler;

    @Resource
    private MyAuthenticationFailureHandler myAuthenticationFailureHandler;

    @Resource
    private TokenVerifyFilter tokenVerifyFilter;

    @Resource
    private MyAccessDeniedHandler myAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity, CorsConfigurationSource corsConfiguration) throws Exception {
        return httpSecurity
                .formLogin((formLogin) ->{
                    formLogin.loginProcessingUrl(Constants.LOGIN_URI) //登录处理地址，不需要写controller
                            .usernameParameter("loginAct")
                            .passwordParameter("loginPwd")
                            .successHandler(myAuthenticationSuccessHandler)
                            .failureHandler(myAuthenticationFailureHandler);
                })
                .authorizeHttpRequests((authorize) -> {
                    authorize.requestMatchers("/api/login",
                                    "/api/register",
                                    "/api/password/reset/code",
                                    "/api/password/reset")
                            .permitAll()
                            .anyRequest()
                            .authenticated();//其他任何请求需要登录后才能访问

                })
                .csrf(AbstractHttpConfigurer::disable)//禁用跨站请求伪造
                //支持跨域请求
                .cors((cors) ->{
                    cors.configurationSource(corsConfiguration);
                })
                .sessionManagement((session) ->{
                    //session创建策略
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);//无session状态
                })
                //添加自定义的filter
                .addFilterBefore(tokenVerifyFilter, LogoutFilter.class)
                //退出登录
                .logout((logout) ->{
                    logout.logoutUrl("/api/logout")
                            .logoutSuccessHandler(myLogoutSuccessHandler);
                })
                //无权限时的处理
                .exceptionHandling((t) -> {
                    t.accessDeniedHandler(myAccessDeniedHandler);
                })
                .build();

    }

    //跨域
    @Bean
    public CorsConfigurationSource corsConfiguration(){
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList("*"));//前端请求路径
        configuration.setAllowedMethods(Arrays.asList("*"));//post,get,delete,put
        configuration.setAllowedHeaders(Arrays.asList("*"));//允许请求头
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
