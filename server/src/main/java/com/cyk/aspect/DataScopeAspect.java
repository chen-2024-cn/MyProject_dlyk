package com.cyk.aspect;

import com.cyk.commons.DataScope;
import com.cyk.model.TUser;
import com.cyk.query.BaseQuery;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
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

        // 当前登录人由 TokenVerifyFilter 放进 SecurityContext：身份来自令牌，角色来自服务端
        TUser tUser = (TUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<String> roleList = tUser.getRoleList();

        if (!roleList.contains("admin")) {
            //只查询用户自己的数据（普通用户）
            Object params = point.getArgs()[0];
            if (params instanceof BaseQuery) {
                BaseQuery query = (BaseQuery) params;
                query.setFilterSQL(" and " + tableAlias + "." + tableField + "=" + tUser.getId()); //select * from t_user tu where tu.id = ... （普通用户）
            }
        } else if (point.getArgs()[0] instanceof BaseQuery adminQuery) {
            // 【安全修复】admin 分支必须显式清空 filterSQL（纵深防御第 2 层）。
            // filterSQL 以 ${} 字符串拼接进 SQL，一旦外部能写入即构成注入（CWE-89）；
            // 第 1 层是 SecureBinderAdvice 的 setDisallowedFields 全局拒收，
            // 此处兜底保证「进入 SQL 的过滤片段只能由本切面生成」，与角色无关。
            adminQuery.setFilterSQL(null);
        }
        Object methodArgs = point.getArgs()[0];
        log.debug("数据权限切面：目标方法执行之前，用户ID：{}，过滤条件：{}",
                tUser.getId(), (methodArgs instanceof BaseQuery q) ? q.getFilterSQL() : null);
        Object res = point.proceed();
        log.debug("数据权限切面：目标方法执行之后");
        return res;
    }
}
