package com.cyk.service.ai.toolkit;

import com.cyk.config.AiPromptProvider;
import com.cyk.mapper.TProductMapper;
import com.cyk.mapper.TTranMapper;
import com.cyk.model.TTran;
import com.cyk.result.ai.AiAbility;
import com.cyk.service.AiPremiumAbilityService;
import com.cyk.service.StatisticService;
import com.cyk.service.ai.AiAgentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 普通用户工具包单测：纯 Mockito，不启动 Spring、不连 DB/Redis。
 *
 * 覆盖论文第 4 章「工具级权限控制」在普通用户侧的两条防线：
 * 1. 数据范围级越权防护：订单查询强制限定 create_by = 当前登录人；
 * 2. 执行层付费墙：未开通的增值能力在工具执行时被拦截并透出开通卡片事件。
 */
@ExtendWith(MockitoExtension.class)
class UserAgentToolkitTest {

    private static final Integer USER_ID = 1001;

    @Mock
    private TTranMapper tTranMapper;
    @Mock
    private TProductMapper tProductMapper;
    @Mock
    private StatisticService statisticService;
    @Mock
    private AiPromptProvider promptProvider;
    @Mock
    private AiPremiumAbilityService premiumAbilityService;

    /** 捕获工具层推向前端的结构化事件帧 */
    private AtomicReference<String> emittedEvent;
    private AiAgentContext context;

    @BeforeEach
    void setUp() {
        emittedEvent = new AtomicReference<>();
        context = new AiAgentContext(
                USER_ID, "张三", false,
                emittedEvent::set,
                null, null,
                () -> { });
    }

    private UserAgentToolkit newToolkit() {
        return new UserAgentToolkit(context, premiumAbilityService,
                tTranMapper, tProductMapper, statisticService, promptProvider);
    }

    private TTran tran(String tranNo, String customer, String money) {
        TTran t = new TTran();
        t.setTranNo(tranNo);
        t.setCustomerName(customer);
        t.setMoney(new BigDecimal(money));
        return t;
    }

    // ==================== 防线一：数据范围强制限定为当前登录人 ====================

    @Test
    void queryMyOrders_forcesCreateByToCurrentUser() {
        // 模拟攻击：即便调用方试图越权，SQL 也必须以当前登录人过滤。
        // 这里验证 mapper 收到的 createBy 参数永远是 USER_ID，而非任何外部传入值。
        when(tTranMapper.selectRecentByCreateBy(anyInt(), any(), anyInt()))
                .thenReturn(List.of(tran("T20260101-001", "李四", "50000")));

        String result = newToolkit().queryMyOrders(null, 10);

        ArgumentCaptor<Integer> createByCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(tTranMapper).selectRecentByCreateBy(createByCaptor.capture(), any(), anyInt());
        assertEquals(USER_ID, createByCaptor.getValue(), "订单查询的数据范围必须锁定为当前登录人");
        assertTrue(result.contains("T20260101-001"));
    }

    @Test
    void queryMyOrders_emptyResult_givesFriendlyMessage() {
        when(tTranMapper.selectRecentByCreateBy(eq(USER_ID), any(), anyInt()))
                .thenReturn(new ArrayList<>());

        String result = newToolkit().queryMyOrders(null, null);

        assertTrue(result.contains("暂无"));
    }

    @Test
    void queryMyOrders_limitIsClampedToSafeUpperBound() {
        // limit 传一个超出上限的值，应被钳制到安全范围（防一次拉取过多数据）。
        when(tTranMapper.selectRecentByCreateBy(anyInt(), any(), anyInt()))
                .thenReturn(new ArrayList<>());

        newToolkit().queryMyOrders(null, 99999);

        ArgumentCaptor<Integer> limitCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(tTranMapper).selectRecentByCreateBy(anyInt(), any(), limitCaptor.capture());
        assertTrue(limitCaptor.getValue() <= 20, "返回条数上限必须被钳制，防止越界拉取");
    }

    @Test
    void queryMyFollowUpReminder_scopesToCurrentUser() {
        when(tTranMapper.selectUpcomingFollowUp(anyInt(), any(), anyInt()))
                .thenReturn(new ArrayList<>());

        newToolkit().queryMyFollowUpReminder(3);

        ArgumentCaptor<Integer> createByCaptor = ArgumentCaptor.forClass(Integer.class);
        verify(tTranMapper).selectUpcomingFollowUp(createByCaptor.capture(), any(), anyInt());
        assertEquals(USER_ID, createByCaptor.getValue(), "跟进提醒同样必须限定为当前登录人");
    }

    // ==================== 防线二：执行层付费墙 ====================

    @Test
    void deepInsight_whenNotGranted_isBlockedAndEmitsPremiumEvent() {
        // 未开通付费能力：工具应在执行层被拦截，并向前端推送开通卡片事件。
        when(premiumAbilityService.isGranted(USER_ID, AiAbility.DEEP_INSIGHT)).thenReturn(false);

        String result = newToolkit().generateDeepInsightReport();

        assertTrue(result.contains("付费能力未开通"), "未开通时必须返回付费墙引导文案");
        assertEquals(
                AbstractAiToolkit.EVENT_PREMIUM_REQUIRED_PREFIX + AiAbility.DEEP_INSIGHT.getKey() + "]",
                emittedEvent.get(),
                "未开通时必须向前端透出结构化开通卡片事件");
        // 付费墙命中后不应再去查库生成报告
        verify(tTranMapper, never()).selectRecentByCreateBy(anyInt(), any(), anyInt());
    }

    @Test
    void deepInsight_whenGranted_passesThroughAndGeneratesReport() {
        // 已开通：放行并正常生成报告。
        when(premiumAbilityService.isGranted(USER_ID, AiAbility.DEEP_INSIGHT)).thenReturn(true);
        when(tTranMapper.selectRecentByCreateBy(eq(USER_ID), isNull(), anyInt()))
                .thenReturn(List.of(tran("T1", "李四", "50000"), tran("T2", "王五", "30000")));
        lenient().when(statisticService.loadSummaryData()).thenReturn(null);

        String result = newToolkit().generateDeepInsightReport();

        assertNull(emittedEvent.get(), "已开通不应再推送付费事件");
        assertTrue(result.contains("经营深度洞察报告"));
        assertTrue(result.contains("80000"), "报告应汇总本人订单总金额");
    }

    @Test
    void trendForecast_whenNotGranted_isBlocked() {
        when(premiumAbilityService.isGranted(USER_ID, AiAbility.TREND_FORECAST)).thenReturn(false);

        String result = newToolkit().forecastTranTrend();

        assertTrue(result.contains("付费能力未开通"));
        assertEquals(
                AbstractAiToolkit.EVENT_PREMIUM_REQUIRED_PREFIX + AiAbility.TREND_FORECAST.getKey() + "]",
                emittedEvent.get());
        verify(tTranMapper, never()).selectTranAmountByMonthByCreateBy(anyInt());
    }

    @Test
    void trendForecast_insufficientData_givesGuidance() {
        when(premiumAbilityService.isGranted(USER_ID, AiAbility.TREND_FORECAST)).thenReturn(true);
        when(tTranMapper.selectTranAmountByMonthByCreateBy(USER_ID)).thenReturn(new ArrayList<>());

        String result = newToolkit().forecastTranTrend();

        assertTrue(result.contains("数据不足"), "趋势数据不足时应给出引导而非报错");
    }
}
