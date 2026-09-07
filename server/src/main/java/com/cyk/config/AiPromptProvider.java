package com.cyk.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AI 提示词外置加载器（Prompt as Resource）。
 *
 * 设计要点：
 * 1. 提示词文案与代码解耦：全部存放于 classpath:prompts/*.md，产品/运营调文案不改 Java 代码；
 * 2. fail-fast：启动时（@PostConstruct）一次性全量加载，任何文件缺失/为空直接抛异常终止启动——
 *    提示词缺失属于部署错误，带病运行会让 AI 行为不可控，宁可启动失败立刻暴露；
 * 3. 常驻内存：加载后缓存为不可变 Map，运行期零 IO；调整文案后重启即生效；
 * 4. 命名占位符 {var}：渲染时按 Map 精确替换，改文案不易出错（相比 %s 顺序占位符）。
 */
@Slf4j
@Component
public class AiPromptProvider {

    /** 提示词文件统一目录 */
    private static final String PROMPT_DIR = "prompts/";

    /**
     * 启动时必须加载成功的提示词文件清单（fail-fast 校验范围）。
     * key = 逻辑名（代码引用），value = 文件名
     */
    private static final Map<String, String> PROMPT_FILES = Map.of(
            "system-common", "system-common.md",
            "system-role-user", "system-role-user.md",
            "system-role-admin", "system-role-admin.md",
            "attachment-current", "attachment-current.md",
            "attachment-carried", "attachment-carried.md",
            "business-faq", "business-faq.md"
    );

    /** 加载后的提示词缓存：逻辑名 -> 模板文本（不可变） */
    private Map<String, String> promptCache;

    @PostConstruct
    public void loadAll() {
        Map<String, String> cache = new HashMap<>();
        for (Map.Entry<String, String> entry : PROMPT_FILES.entrySet()) {
            String path = PROMPT_DIR + entry.getValue();
            try (InputStream in = new ClassPathResource(path).getInputStream()) {
                String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                if (content.isBlank()) {
                    throw new IllegalStateException("提示词文件内容为空: " + path);
                }
                cache.put(entry.getKey(), content);
            } catch (IOException e) {
                // fail-fast：提示词属于 AI 模块的核心配置，缺失时拒绝启动，立刻暴露部署问题
                throw new IllegalStateException("AI 提示词文件加载失败，服务拒绝启动: " + path, e);
            }
        }
        this.promptCache = Map.copyOf(cache);
        log.info("AI 提示词加载完成 | 共 {} 个文件: {}", promptCache.size(), Set.copyOf(promptCache.keySet()));
    }

    /**
     * 获取原始提示词模板（含 {var} 占位符）
     */
    public String getTemplate(String name) {
        String template = promptCache.get(name);
        if (template == null) {
            // 理论上不可达（启动已全量校验），防御性兜底
            throw new IllegalStateException("提示词未注册: " + name);
        }
        return template;
    }

    /**
     * 渲染提示词：把模板中的 {key} 占位符替换为变量表对应值。
     * 变量表中不存在的占位符原样保留（便于排查漏传参数）。
     *
     * @param name  提示词逻辑名
     * @param vars  变量表（key 不含花括号）
     * @return 渲染后的完整提示词（去除首尾空白）
     */
    public String render(String name, Map<String, String> vars) {
        String rendered = getTemplate(name);
        for (Map.Entry<String, String> var : vars.entrySet()) {
            rendered = rendered.replace("{" + var.getKey() + "}",
                    var.getValue() != null ? var.getValue() : "");
        }
        return rendered.strip();
    }
}
