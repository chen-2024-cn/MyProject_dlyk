package dev.langchain4j.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;

/**
 * {@link JacksonJsonCodec} 的宽容化子类。
 *
 * 放置在 {@code dev.langchain4j.internal} 包内，是为了继承包私有的 {@code JacksonJsonCodec}
 * （这是 LangChain4j 官方 SPI 覆盖默认 JSON 编解码器的标准做法，能完整保留官方为
 * LocalDate/LocalDateTime/LocalTime 注册的自定义序列化器，不丢失任何默认行为）。
 *
 * 仅覆写两个 fromJson 入口，在解析前把大模型偶发输出的 Python 风格字面量
 * （None/True/False/NaN）修正为合法 JSON，其余方法全部沿用默认实现。
 */
public class TolerantJacksonJsonCodec extends JacksonJsonCodec {

    private static final Logger log = LoggerFactory.getLogger(TolerantJacksonJsonCodec.class);

    @Override
    public <T> T fromJson(String json, Type type) {
        return super.fromJson(sanitize(json), type);
    }

    @Override
    public <T> T fromJson(String json, Class<T> clazz) {
        return super.fromJson(sanitize(json), clazz);
    }

    /**
     * 把值位置的 Python 风格字面量修正为 JSON 合法 token。
     * 只匹配「结构符号（左括号/冒号/逗号）+ 可选空白 + 字面量」后跟逗号/括号的位置，
     * 不会误伤字符串内容（如用户消息里出现的单词 None）。
     */
    private String sanitize(String json) {
        if (json == null || json.isBlank()) {
            return json;
        }
        // 极端情况：整段参数就是一个 Python 字面量（非对象），直接兜底为空对象
        String trimmed = json.trim();
        if ("None".equals(trimmed) || "null".equals(trimmed)) {
            return "{}";
        }
        String fixed = json
                .replaceAll("([\\{,:]\\s*)None(?=\\s*[,}\\]])", "$1null")
                .replaceAll("([\\{,:]\\s*)True(?=\\s*[,}\\]])", "$1true")
                .replaceAll("([\\{,:]\\s*)False(?=\\s*[,}\\]])", "$1false")
                .replaceAll("([\\{,:]\\s*)NaN(?=\\s*[,}\\]])", "$1null");
        if (!fixed.equals(json)) {
            log.warn("AI 工具参数出现非法 JSON 字面量（Python 风格），已自动修正 | 原文: {}", json);
        }
        return fixed;
    }
}
