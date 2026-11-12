package com.cyk.service.ai.toolkit;

import com.cyk.mapper.TRoleMapper;
import com.cyk.mapper.TTranMapper;
import com.cyk.mapper.TUserMapper;
import com.cyk.model.TRole;
import com.cyk.model.TUser;
import com.cyk.result.R;
import com.cyk.service.AiPremiumAbilityService;
import com.cyk.service.RedisService;
import com.cyk.service.StatisticService;
import com.cyk.service.UserService;
import com.cyk.service.ai.AiAgentContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 管理员工具包单测：纯 Mockito，不启动 Spring、不连 DB/Redis。
 *
 * 覆盖论文第 4 章「工具级权限控制」在管理员侧的纵深防御：
 * 即便物理隔离层被绕过（普通用户会话中本不应挂载管理员工具），
 * 每个危险操作入口的二次角色断言 adminGuard() 仍能兜底拦截越权调用。
 */
@ExtendWith(MockitoExtension.class)
class AdminAgentToolkitTest {

    private static final Integer ADMIN_ID = 1;
    private static final Integer NORMAL_USER_ID = 1001;

    @Mock
    private UserService userService;
    @Mock
    private TUserMapper tUserMapper;
    @Mock
    private TRoleMapper tRoleMapper;
    @Mock
    private TTranMapper tTranMapper;
    @Mock
    private RedisService redisService;
    @Mock
    private StatisticService statisticService;
    @Mock
    private AiPremiumAbilityService premiumAbilityService;

    private AtomicReference<String> emittedEvent;

    @BeforeEach
    void setUp() {
        emittedEvent = new AtomicReference<>();
    }

    /** 构造指定身份的上下文：isAdmin=false 模拟"隔离层被绕过"的极端场景 */
    private AiAgentContext contextOf(Integer userId, boolean isAdmin) {
        return new AiAgentContext(userId, "操作人", isAdmin,
                emittedEvent::set, null, null, () -> { });
    }

    private AdminAgentToolkit toolkitFor(AiAgentContext context) {
        return new AdminAgentToolkit(context, premiumAbilityService, userService,
                tUserMapper, tRoleMapper, tTranMapper, redisService, statisticService);
    }

    // ==================== 纵深防御：非管理员调用管理员工具一律被入口断言拦截 ====================

    @Test
    void exportUsers_asNonAdmin_isDeniedByGuard() {
        AdminAgentToolkit toolkit = toolkitFor(contextOf(NORMAL_USER_ID, false));

        String result = toolkit.exportUsersExcel();

        assertTrue(result.contains("权限不足"), "非管理员调用导出必须被拦截");
        verify(tUserMapper, never()).selectAllUsers();
    }

    @Test
    void queryAllOrders_asNonAdmin_isDeniedByGuard() {
        AdminAgentToolkit toolkit = toolkitFor(contextOf(NORMAL_USER_ID, false));

        String result = toolkit.queryAllOrders(null, null, null);

        assertTrue(result.contains("权限不足"));
        verify(tTranMapper, never()).selectGlobalByCondition(any(), any(), anyInt());
    }

    @Test
    void queryAllUsers_asNonAdmin_isDeniedByGuard() {
        AdminAgentToolkit toolkit = toolkitFor(contextOf(NORMAL_USER_ID, false));

        String result = toolkit.queryAllUsers(null);

        assertTrue(result.contains("权限不足"));
        verify(tUserMapper, never()).selectAllUsers();
    }

    @Test
    void updateUserPermission_asNonAdmin_isDeniedByGuard() {
        AdminAgentToolkit toolkit = toolkitFor(contextOf(NORMAL_USER_ID, false));

        String result = toolkit.updateUserPermission(2, 0, "3");

        assertTrue(result.contains("权限不足"));
        verify(userService, never()).aiUpdateUserRolesAndStatus(anyInt(), any(), anyList(), anyInt());
    }

    // ==================== 正向路径：管理员正常调用放行 ====================

    @Test
    void queryAllUsers_asAdmin_passesThrough() {
        AdminAgentToolkit toolkit = toolkitFor(contextOf(ADMIN_ID, true));
        TUser u = new TUser();
        u.setId(2);
        u.setLoginAct("zhangsan");
        u.setName("张三");
        u.setAccountEnabled(1);
        when(tUserMapper.selectAllUsers()).thenReturn(List.of(u));
        when(tRoleMapper.selectByUserId(2)).thenReturn(new ArrayList<>());

        String result = toolkit.queryAllUsers(20);

        assertTrue(result.contains("zhangsan"), "管理员应能正常查询用户清单");
        verify(tUserMapper).selectAllUsers();
    }

    @Test
    void queryRoleList_asAdmin_returnsRoles() {
        AdminAgentToolkit toolkit = toolkitFor(contextOf(ADMIN_ID, true));
        TRole role = new TRole();
        role.setId(3);
        role.setRole("admin");
        role.setRoleName("管理员");
        when(tRoleMapper.selectRoleList()).thenReturn(List.of(role));

        String result = toolkit.queryRoleList();

        assertTrue(result.contains("admin"));
    }

    // ==================== 防自锁：管理员不能禁用自己 ====================

    @Test
    void updateUserPermission_adminCannotDisableSelf() {
        AdminAgentToolkit toolkit = toolkitFor(contextOf(ADMIN_ID, true));

        String result = toolkit.updateUserPermission(ADMIN_ID, 0, null);

        assertTrue(result.contains("不能禁用当前登录") || result.contains("自我锁定"),
                "管理员禁用自己必须被拒绝，防止自我锁定");
        verify(userService, never()).aiUpdateUserRolesAndStatus(anyInt(), any(), any(), anyInt());
    }

    @Test
    void updateUserPermission_noChangeRequested_isRejected() {
        // 既不修改状态也不修改角色：属于无效请求，应被明确拒绝。
        AdminAgentToolkit toolkit = toolkitFor(contextOf(ADMIN_ID, true));

        String result = toolkit.updateUserPermission(2, null, null);

        assertTrue(result.contains("没有需要修改"));
        verify(userService, never()).aiUpdateUserRolesAndStatus(anyInt(), any(), any(), anyInt());
    }

    @Test
    void updateUserPermission_validChange_invokesService() {
        AdminAgentToolkit toolkit = toolkitFor(contextOf(ADMIN_ID, true));
        when(userService.aiUpdateUserRolesAndStatus(anyInt(), any(), any(), anyInt()))
                .thenReturn(R.OK());

        String result = toolkit.updateUserPermission(2, 1, "3,4");

        assertTrue(result.contains("修改成功"));
        verify(userService).aiUpdateUserRolesAndStatus(anyInt(), any(), any(), anyInt());
    }

    // ==================== 付费墙与角色断言的叠加：先过付费墙再过角色断言 ====================

    @Test
    void globalDeepInsight_asNonAdmin_evenIfGranted_isStillDeniedByRole() {
        // 关键场景：即便普通用户已付费开通，全局经营洞察仍因角色不符被拦截。
        // 说明付费能力与角色权限是两条独立防线，付费不能越权。
        AdminAgentToolkit toolkit = toolkitFor(contextOf(NORMAL_USER_ID, false));
        lenient().when(premiumAbilityService.isGranted(anyInt(), any())).thenReturn(true);

        String result = toolkit.generateGlobalDeepInsight();

        assertTrue(result.contains("权限不足"),
                "付费开通不等于越过角色权限，普通用户的全局洞察仍应被角色断言拦截");
        verify(statisticService, never()).loadSummaryData();
    }
}
