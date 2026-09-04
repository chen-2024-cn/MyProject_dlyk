package com.cyk.config;

import dev.langchain4j.internal.Json;
import dev.langchain4j.internal.TolerantJacksonJsonCodec;
import dev.langchain4j.spi.json.JsonCodecFactory;

/**
 * LangChain4j JSON 编解码器工厂（企业级防御性修复）。
 *
 * 通过 META-INF/services/dev.langchain4j.spi.json.JsonCodecFactory 这一官方 SPI 生效：
 * LangChain4j 的 {@code Json} 类在静态块用 ServiceLoader 加载本工厂，
 * 用它产出的 {@link TolerantJacksonJsonCodec} 替换默认严格实现。
 * 该工厂只影响 LangChain4j 内部的 JSON 处理，不影响项目自身的 Jackson 用法。
 */
public class TolerantJsonCodecFactory implements JsonCodecFactory {

    @Override
    public Json.JsonCodec create() {
        return new TolerantJacksonJsonCodec();
    }
}
