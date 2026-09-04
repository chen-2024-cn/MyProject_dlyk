package com.cyk.result.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * AI 能力视图对象（面向前端能力商店的展示模型）
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAbilityVO {

    /** 能力唯一标识 */
    private String key;

    /** 能力名称 */
    private String name;

    /** 能力说明 */
    private String description;

    /** 是否增值付费能力 */
    private Boolean premium;

    /** 付费价格（免费能力为 null） */
    private BigDecimal price;

    /** 当前用户是否已开通（免费能力恒为 true） */
    private Boolean purchased;
}
