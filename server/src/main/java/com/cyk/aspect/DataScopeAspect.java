package com.cyk.aspect;

import com.cyk.commons.DataScope;
import com.cyk.constants.Constants;
import com.cyk.model.TUser;
import com.cyk.query.BaseQuery;
import com.cyk.util.JWTUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.List;

@Aspect
@Component
public class DataScopeAspect {
    //aspectJ 实现aop

    //切入点
    @Pointcut(value = "@annotation(com.cyk.commons.DataScope)")
    public void pointCut() {
    }

    @Around(value = "pointCut()")
    public Object process(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        //获取方法上的注解
        DataScope annotation = signature.getMethod().getAnnotation(DataScope.class);

        String tableAlias = annotation.tableAlias();
        String tableField = annotation.tableField();

        //在spring web中获取当前请求的request对象
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();

        String token = request.getHeader(Constants.TOKEN_NAME);
        //从token中解析出当前用户的身份
        TUser tUser = JWTUtils.parseUserFromJWT(token);
            List<String> roleList = tUser.getRoleList();

        if (!roleList.contains("admin")) {
            //只查询用户自己的数据（普通用户）
            Object params = point.getArgs()[0];
            if (params instanceof BaseQuery) {
                BaseQuery query = (BaseQuery) params;
                query.setFilterSQL(" and " + tableAlias + "." + tableField + "=" + tUser.getId()); //select * from t_user tu where tu.id = ... （普通用户）
            }
        }
        System.out.println("目标方法执行之前");
        Object res = point.proceed();
        System.out.println("目标方法执行之后");
        return res;
    }
}
