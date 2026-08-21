package com.cyk.config.handler;

import com.cyk.result.CodeEnum;
import com.cyk.result.R;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 统一异常处理类，controller发生了异常，统一用该类进行处理
 *
 */
@RestControllerAdvice //aop。拦截标注了@RestController的controller中的所有方法
//@ControllerAdvice //aop。拦截标注了@Controller的controller中的所有方法
public class GlobalExceptionHandler {

    /**
     * 异常处理的方法（controller方法发生了异常，那么就使用该方法来处理）
     *
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public R handException(Exception e) {
        e.printStackTrace(); //在控制台打印异常信息
        return R.FAIL(e.getMessage());
    }

    /**
     * 专门处理唯一键/主键冲突
     * @param e
     * @return
     */
    @ExceptionHandler(value = DuplicateKeyException.class)
    public R handleDuplicateKeyException(DuplicateKeyException e) {
        e.printStackTrace();

        String message = e.getMessage();
        if (message != null) {
            return R.FAIL(CodeEnum.DUPLICATE_EXCEPTION);
        }

        return R.FAIL(CodeEnum.DATA_ACCESS_EXCEPTION);
    }

    /**
     * 异常的精确匹配，先精确匹配，匹配不到了，就找父类的异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = DataAccessException.class)
    public R handException3(DataAccessException e) {
        e.printStackTrace(); //在控制台打印异常信息
        return R.FAIL(CodeEnum.DATA_ACCESS_EXCEPTION);
    }


    /**
     * 权限不足的异常处理
     *
     * @param e
     * @return
     */
    @ExceptionHandler(value = AccessDeniedException.class)
    public R handException(AccessDeniedException e) {
        e.printStackTrace(); //在控制台打印异常信息
        return R.FAIL(CodeEnum.ACCESS_DENIED);
    }
}
