package com.cyk.config;

import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.InitBinder;

/**
 * 全局参数绑定安全加固。
 *
 * <p><b>审计发现的问题</b>：{@code BaseQuery.filterSQL} 是仅供服务端
 * {@link com.cyk.aspect.DataScopeAspect} 写入的数据权限过滤片段，最终以
 * {@code ${filterSQL}} 字符串拼接进 SQL（TActivityMapper / TUserMapper）。
 * 但所有继承 BaseQuery 的 Query 对象（ActivityQuery / ProductQuery / UserQuery…）
 * 通过 {@code @ModelAttribute} / 查询串直接绑定时，攻击者可以在 URL 上携带
 * {@code ?filterSQL=or 1=1} 之类的参数直达 SQL —— 典型 SQL 注入通道（CWE-89）。</p>
 *
 * <p><b>修复策略（纵深防御第 1 层）</b>：在 Web 绑定层用
 * {@code setDisallowedFields} 全局拒收该字段——无论哪个 Controller、哪个 Query 子类，
 * 外部请求参数都不可能再写入 filterSQL；该字段从此只能由服务端切面赋值。</p>
 *
 * <p>纵深防御第 2 层见 {@link com.cyk.aspect.DataScopeAspect}：admin 分支显式清空，
 * 确保任何旁路都无法携带过滤片段进入 SQL。</p>
 *
 * <p>说明：{@code @InitBinder} 只作用于表单/查询串绑定（@ModelAttribute、@RequestParam），
 * 不影响 @RequestBody 的 Jackson 反序列化；而所有 @RequestBody 写路径最终
 * BeanUtils 拷贝到实体（实体无 filterSQL 属性），不构成注入面。</p>
 */
@ControllerAdvice
public class SecureBinderAdvice {

    @InitBinder
    public void forbidServerOnlyFields(WebDataBinder binder) {
        // filterSQL：数据权限切面专用，禁止一切外部输入
        binder.setDisallowedFields("filterSQL");
    }
}
