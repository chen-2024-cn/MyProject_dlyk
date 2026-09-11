package com.cyk.exception;

/**
 * 业务异常（预期内失败）。
 *
 * <p>企业级异常分层规范：</p>
 * <ul>
 *   <li>{@code BusinessException} —— 业务规则不满足（如"手机号已存在"、"只能变更为相邻阶段"），
 *       message 是<b>面向用户</b>的安全提示文案，允许原样返回前端；</li>
 *   <li>其他 {@code Exception} —— 系统故障（SQL 报错、NPE 等），message 可能包含
 *       表结构/驱动栈等敏感细节，{@link com.cyk.config.handler.GlobalExceptionHandler}
 *       一律脱敏为统一文案，仅落服务端日志。</li>
 * </ul>
 *
 * <p>此前 Service 层普遍 {@code throw new RuntimeException("用户可见文案")}，
 * 与系统异常混用同一通道，导致全局处理器无法区分"业务提示可透传"与"系统细节必须脱敏"，
 * 本类即为该二分法的落地载体。</p>
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
