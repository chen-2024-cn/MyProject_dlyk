package com.cyk.service;

import com.cyk.result.ai.AiAbility;

/**
 * AI 增值能力开通状态服务。
 * 管理「用户是否已付费开通某个增值能力」的判定与写入，是付费墙的运行时闸门。
 */
public interface AiPremiumAbilityService {

    /**
     * 判断用户是否有权使用指定能力：
     * - 免费能力：恒为 true；
     * - 付费能力：查询开通记录（Redis 快速路径 + DB 兜底）。
     */
    boolean isGranted(Integer userId, AiAbility ability);

    /**
     * 写入能力开通记录（支付成功回调时调用），带默认有效期。
     */
    void grantAbility(Integer userId, AiAbility ability);

    /**
     * 查询某用户当前已开通的付费能力 key 数量（用于前端角标展示）
     */
    int countGranted(Integer userId);
}
