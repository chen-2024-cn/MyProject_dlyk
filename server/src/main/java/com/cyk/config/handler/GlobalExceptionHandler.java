package com.cyk.config.handler;

import com.cyk.exception.BusinessException;
import com.cyk.result.CodeEnum;
import com.cyk.result.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.sql.SQLIntegrityConstraintViolationException;

/**
 * 统一异常处理类，controller发生了异常，统一用该类进行处理
 *
 * <p><b>安全基线（响应脱敏）</b>：面向前端的响应体只允许携带「用户可读的业务提示」，
 * 绝不回吐底层异常 message / 堆栈 —— 后者会暴露表名、字段名、驱动版本、SQL 片段等
 * 可被攻击者用于构造后续攻击的信息（信息泄露类漏洞 CWE-209）。
 * 因此本类按异常性质二分处理：</p>
 * <ol>
 *   <li>{@link BusinessException}：业务规则失败，message 由开发者显式编写、面向用户 → 透传；</li>
 *   <li>参数绑定/校验类异常：给出明确的字段级错误提示 → 由本类现场生成安全文案；</li>
 *   <li>其余 {@link Exception}：系统故障 → 完整堆栈仅落服务端日志，响应统一为「服务器繁忙」。</li>
 * </ol>
 *
 * <p>注：{@link AccessDeniedException}(权限不足) HTTP 状态仍为 200，鉴权语义由业务码承载，
 * 与 TokenVerifyFilter 的 9xx 令牌错误码体系保持一致，前端拦截器无需改造即可识别。</p>
 */
@Slf4j
@RestControllerAdvice //aop。拦截标注了@RestController的controller中的所有方法
//@ControllerAdvice //aop。拦截标注了@Controller的controller中的所有方法
public class GlobalExceptionHandler {

    /** 系统级故障对外的统一脱敏文案（禁止透传 e.getMessage()，防止泄露内部实现细节） */
    private static final String INTERNAL_ERROR_MSG = "服务器繁忙，请稍后重试";

    /**
     * 业务异常：预期内的规则失败，message 面向用户，安全透传。
     * 用 warn 级别记录（不是系统故障，无需 error 告警噪音）。
     */
    @ExceptionHandler(value = BusinessException.class)
    public R handleBusinessException(BusinessException e) {
        log.warn("业务规则拦截：{}", e.getMessage());
        return R.FAIL(e.getMessage());
    }

    /**
     * @RequestBody 参数校验失败（@Valid 触发）：提取首个字段错误生成可读提示。
     */
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public R handleValidException(MethodArgumentNotValidException e) {
        String fieldMsg = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(err -> err.getField() + "：" + err.getDefaultMessage())
                .orElse(INTERNAL_ERROR_MSG);
        log.warn("请求体参数校验失败：{}", fieldMsg);
        return R.FAIL(fieldMsg);
    }

    /**
     * 必填请求参数缺失：明确告知缺哪个参数（比 Spring 默认英文栈友好且不含实现细节）。
     */
    @ExceptionHandler(value = MissingServletRequestParameterException.class)
    public R handleMissingParam(MissingServletRequestParameterException e) {
        log.warn("缺少必填请求参数：{}", e.getParameterName());
        return R.FAIL("缺少必填参数：" + e.getParameterName());
    }

    /**
     * 参数类型不匹配（如 ids=abc 期望 Integer、stage 传非法字符、expectedDate 格式错误）。
     *
     * <p>审计中实测：{@code DELETE /api/clue/batch?ids=abc} 原先直接把 Spring 的
     * {@code Validation failed for argument [0] ... typeMismatch ...} 整段长文回吐前端，
     * 既暴露内部类名（com.cyk.web.TranController）又不友好，此处统一收敛。</p>
     */
    @ExceptionHandler(value = MethodArgumentTypeMismatchException.class)
    public R handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        log.warn("请求参数类型不匹配 | name={}, value={}", e.getName(), e.getValue());
        return R.FAIL("参数「" + e.getName() + "」格式不正确");
    }

    /**
     * 异常处理的方法（controller方法发生了异常，那么就使用该方法来处理）
     *
     * <p>兜底通道：任何未分类的系统异常都到此，<b>响应脱敏 + 日志留全</b>。</p>
     *
     * @return
     */
    @ExceptionHandler(value = Exception.class)
    public R handException(Exception e) {
        log.error("系统异常", e); //企业级规范：通过日志框架输出异常堆栈，便于采集与检索
        return R.FAIL(INTERNAL_ERROR_MSG);
    }

    /**
     * 专门处理唯一键/主键冲突
     * @param e
     * @return
     */
    @ExceptionHandler(value = DuplicateKeyException.class)
    public R handleDuplicateKeyException(DuplicateKeyException e) {
        log.error("唯一约束冲突", e);

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
        log.error("数据访问异常", e);
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
        log.warn("权限不足：{}", e.getMessage());
        return R.FAIL(CodeEnum.ACCESS_DENIED);
    }
}
