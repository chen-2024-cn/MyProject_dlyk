# DLYK CRM·AI 智能体功能设计与实现详解

> 项目：DLYK 旅途管理系统 · AI 业务领航员
> 技术栈：Spring Boot 3.5 · LangChain4j · MySQL · Redis · Vue 3 + Element Plus · SSE
> 文档版本：v1.0（2026-08-29）

---

## 一、整体概述

### 1.1 产品定位

本智能体是 CRM 系统中面向**业务职能**的 AI 助手（代号「AI 业务领航员」），核心特征是**角色感知**：

- 用户登录后，系统从登录态实时解析其角色；
- 根据角色（**普通用户** / **管理员**）为同一套大模型装配**完全不同的工具包**；
- 提供若干**增值付费能力**，需完成模拟支付流程后才能使用。

### 1.2 功能清单总览

| 角色 | 免费能力 | 付费能力 |
|------|---------|---------|
| **普通用户** | ①查询本人订单 ②交易跟进提醒 ③业务疑难解答 ④产品行情咨询 | ⑤经营深度洞察报告（¥9.9） ⑥交易趋势预测（¥4.9） |
| **管理员** | ①导出用户Excel ②下载导入模板 ③附件批量导入 ④全局订单查询 ⑤用户/角色盘点 ⑥修改用户权限 | ⑦全局深度洞察（¥9.9） ⑧全局趋势预测（¥4.9） |

> 同一个用户，用管理员账号登录看到「管理驾驶舱」，用普通账号登录看到「旅客服务窗」，**界面和可用能力完全是两套**。

### 1.3 技术架构图

```
┌─────────────────────────────────────────────────────────────┐
│                    AiAgentController（REST/SSE 入口）         │
│  /api/ai/profile    角色画像（能力清单按角色下发）             │
│  /api/ai/stream-chat SSE 流式对话（不传权限，服务端判定）      │
│  /api/ai/payment/*   订单创建/支付/取消/查询                 │
│  /api/ai/file/*      附件上传/下载（管理员专属）              │
└───────────────────────────┬─────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────┐
│          AiAssistantServiceImpl（角色编排中枢）              │
│  ① AiRoleResolverService 实时查库判定角色                    │
│  ② 按角色物理挂载工具包（Admin/User Toolkit）                 │
│  ③ AiAgentContext 请求级身份快照（跨异步线程安全）            │
│  ④ Flux.merge(模型token流, 工具事件流) → SSE                 │
└───────────────────────────┬─────────────────────────────────┘
                             ▼
┌─────────────────────────────────────────────────────────────┐
│         AbstractAiToolkit（工具基类 · 付费墙）                │
│  premiumGate()：工具执行层硬校验付费状态（最后一道闸门）       │
│  未开通 → 推送 [AI-EVENT:PREMIUM_REQUIRED:xxx] + 引导文案    │
│  文件生成 → [AI-EVENT:FILE_READY:文件名]                     │
└─────────────────────────────────────────────────────────────┘
```

---

## 二、后端分层实现详解

### 2.1 能力目录（单一事实来源）

**文件**：`result/ai/AiAbility.java`

能力目录用**枚举**定义，是整个付费体系的唯一事实来源，新增能力只改这一个文件：

```java
@Getter
@RequiredArgsConstructor
public enum AiAbility {

    // ========== 普通用户 · 免费 ==========
    MY_ORDERS("my_orders", "我的订单查询", "查询我名下的交易订单、金额与阶段进展",
              Audience.USER, false, null),

    TRAN_REMINDER("tran_reminder", "交易跟进提醒", "列出我近期需要联系客户跟进的交易提醒",
              Audience.USER, false, null),

    BUSINESS_FAQ("business_faq", "业务疑难解答", "解答交易阶段、线索转化、客户管理等业务流程问题",
              Audience.USER, false, null),

    PRODUCT_CONSULT("product_consult", "产品行情咨询", "查询在售产品的报价与行情信息",
              Audience.USER, false, null),

    // ========== 管理员 · 免费 ==========
    USER_EXPORT("user_export", "用户数据导出", "将全部用户数据导出为 Excel 并生成下载链接",
              Audience.ADMIN, false, null),

    // ...... 同理省略

    // ========== 增值付费（全角色通用） ==========
    DEEP_INSIGHT("deep_insight", "经营深度洞察报告",
              "汇总转化漏斗、来源分布与订单规模，生成多维经营洞察报告",
              Audience.ALL, true, new BigDecimal("9.90")),

    TREND_FORECAST("trend_forecast", "交易趋势预测",
              "基于月度交易额走势进行环比推演与趋势预判",
              Audience.ALL, true, new BigDecimal("4.90"));

    /** 受众范围：普通用户 / 管理员 / 全角色 */
    public enum Audience { USER, ADMIN, ALL }
}
```

**设计要点**：
- `Audience` 字段控制前端能力商店的**可见范围**；
- `isPremium()` + `price` 决定是否走付费墙；
- 金额用 `BigDecimal` 字符串构造，杜绝浮点精度问题。

### 2.2 角色判定（权威化）

**文件**：`service/impl/AiRoleResolverServiceImpl.java`

旧版方案由前端传 `permissions`，可被伪造。新版改为服务端实时查库：

```java
@Service
public class AiRoleResolverServiceImpl implements AiRoleResolverService {

    @Resource
    private TRoleMapper tRoleMapper;

    @Override
    public boolean isAdmin(TUser user) {
        if (user == null || user.getId() == null) return false;
        List<TRole> roles = tRoleMapper.selectByUserId(user.getId());
        return roles != null && roles.stream()
                .anyMatch(r -> Constants.ROLE_ADMIN.equals(r.getRole()));
    }
}
```

> **关键**：既不信前端传参，也不信 JWT 里的旧快照（登录后角色可能被改），每次都查库。

### 2.3 请求级上下文（跨线程身份快照）

**文件**：`service/ai/AiAgentContext.java`

LangChain4j 的工具调用发生在大模型 HTTP 客户端的**异步回调线程**，不是 Tomcat 请求线程，`ThreadLocal` 会丢。因此把身份"快照式"绑进不可变对象：

```java
@Getter
public class AiAgentContext {
    private final Integer userId;          // 登录人 ID
    private final String userName;          // 登录人姓名
    private final boolean admin;            // 是否管理员（服务端判定）
    private final Consumer<String> eventEmitter;  // 向前端推事件
    private final String attachmentFileName;      // 本轮附件名
    private final String attachmentBase64;         // 本轮附件内容

    public AiAgentContext(Integer userId, String userName, boolean admin,
                          Consumer<String> eventEmitter,
                          String attachmentFileName, String attachmentBase64) { ... }
}
```

### 2.4 编排中枢（角色物理隔离）

**文件**：`service/impl/AiAssistantServiceImpl.java`

这是整个系统的**大脑**，按角色物理挂载工具包：

```java
@Override
public Flux<String> chatStream(TUser user, String memoryId, String message, String attachmentFileId) {
    Integer userId = user.getId();
    boolean isAdmin = aiRoleResolverService.isAdmin(user);

    // ① 加载本轮附件（管理员导入场景）
    Attachment attachment = loadAttachment(attachmentFileId);

    // ② 结构化事件通道：工具异步线程 → SSE 流
    Sinks.Many<String> eventSink = Sinks.many().unicast().onBackpressureBuffer();

    // ③ 构造请求级上下文（身份快照绑定进不可变对象）
    AiAgentContext context = new AiAgentContext(userId, user.getName(), isAdmin,
            event -> emitEvent(eventSink, event),
            attachment != null ? attachment.fileName() : null,
            attachment != null ? attachment.base64() : null);

    // ④ 按角色挂载对应工具包（物理隔离）
    BusinessAgent agent = buildAgent(context, isAdmin);

    // ⑤ 组装角色感知的增强提示词
    String augmentedMessage = buildAugmentedMessage(user.getName(), isAdmin, message, attachment);

    // ⑥ 合并模型流与事件流，模型流终结时关闭事件流
    String memKey = memKey(userId, memoryId);
    Flux<String> modelStream = agent.chat(memKey, augmentedMessage)
            .doFinally(signal -> eventSink.tryEmitComplete());
    return Flux.merge(modelStream, eventSink.asFlux())
            .onErrorResume(error -> Flux.just("\n\n抱歉，本次服务出现异常..."));
}

/** 按角色构建 Agent 并装配工具包（每次请求新建实例，防并发串流）*/
private BusinessAgent buildAgent(AiAgentContext context, boolean isAdmin) {
    Object toolkit = isAdmin
        ? new AdminAgentToolkit(context, aiPremiumAbilityService, userService,
            tUserMapper, tRoleMapper, tTranMapper, redisService, statisticService)
        : new UserAgentToolkit(context, aiPremiumAbilityService,
            tTranMapper, tProductMapper, statisticService);

    return AiServices.builder(BusinessAgent.class)
            .streamingChatModel(l4jStreamingChatModel)
            .chatMemoryProvider(memId ->
                memoryCache.computeIfAbsent(memId.toString(),
                    k -> MessageWindowChatMemory.withMaxMessages(20)))
            .tools(toolkit)          // ← 核心：物理隔离
            .build();
}
```

**两个关键机制**：
1. **物理隔离**：普通用户的会话里根本不存在 `exportUsersExcel`、`updateUserPermission` 等管理工具，不是"运行时拒绝"，而是"压根不注册"；
2. **事件流合并**：`Flux.merge` 需所有源都 complete 才结束，事件 Sink 不会自完成，必须挂在模型流 `doFinally` 里 `tryEmitComplete`。

---

## 三、工具层实现（核心业务能力）

### 3.1 工具基类 · 付费墙

**文件**：`service/ai/toolkit/AbstractAiToolkit.java`

所有工具包继承此类。付费墙**下沉到工具执行层**，大模型即使被 Prompt 诱导也绕不过：

```java
@Slf4j
public abstract class AbstractAiToolkit {

    /** 付费能力未开通时推送给前端的结构化事件前缀 */
    public static final String EVENT_PREMIUM_REQUIRED_PREFIX = "[AI-EVENT:PREMIUM_REQUIRED:";
    /** 文件就绪事件前缀 */
    public static final String EVENT_FILE_READY_PREFIX = "[AI-EVENT:FILE_READY:";

    protected final AiAgentContext context;
    protected final AiPremiumAbilityService premiumAbilityService;

    /**
     * 付费能力闸口：已开通返回 null；未开通返回引导文案 + 推送开通卡片事件给前端。
     */
    protected String premiumGate(AiAbility ability) {
        if (premiumAbilityService.isGranted(context.getUserId(), ability)) {
            return null;
        }
        // 向前端推送结构化事件（确定性透出，不依赖大模型转述）
        context.getEventEmitter().accept(EVENT_PREMIUM_REQUIRED_PREFIX + ability.getKey() + "]");
        log.info("AI 付费墙拦截 | userId={}, ability={}", context.getUserId(), ability.getKey());
        return String.format(
                "【付费能力未开通】「%s」为增值付费能力（¥%s）。"
                        + "请礼貌告知用户需开通后才能使用，并引导其点击对话中的开通卡片完成购买。",
                ability.getName(), ability.getPrice());
    }
}
```

### 3.2 普通用户工具包

**文件**：`service/ai/toolkit/UserAgentToolkit.java`

所有订单查询在 SQL 层强制 `create_by = 当前登录人`，**无法越权**：

#### ① 查询我的订单
```java
@Tool("查询当前登录用户自己名下的交易订单列表，可返回订单流水号、客户姓名、金额、阶段、创建时间；"
        + "这是数据范围安全的查询，只会返回该用户本人创建的订单")
public String queryMyOrders(
        @P(value = "阶段字典ID；不确定时省略该参数", required = false) Integer stage,
        @P(value = "返回条数上限，最多20条；默认10条", required = false) Integer limit) {
    int safeLimit = limit == null || limit <= 0 || limit > 20 ? 10 : limit;
    // SQL 强制 create_by = context.getUserId()
    List<TTran> orders = tTranMapper.selectRecentByCreateBy(context.getUserId(), stage, safeLimit);
    // ... 格式化为自然语言文本返回给大模型
}
```

对应 SQL（`TTranMapper.xml`）：
```xml
<select id="selectRecentByCreateBy" resultMap="TranResultMap">
  select tt.*, tdv.type_value stageName, tc2.full_name customerName
  from t_tran tt ...
  where tt.create_by = #{createBy}   <!-- 强制限定登录人 -->
    <if test="stage != null">and tt.stage = #{stage}</if>
  order by tt.create_time desc
  limit #{limit}
</select>
```

#### ② 交易跟进提醒
```java
@Tool("查询当前登录用户自己在指定天数内需要跟进联系客户的交易提醒")
public String queryMyFollowUpReminder(
        @P(value = "未来多少天内需要跟进；默认3天", required = false) Integer withinDays) {
    int days = withinDays == null || withinDays <= 0 ? 3 : Math.min(withinDays, 30);
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.DAY_OF_MONTH, days);
    // 查询下次联系时间 <= 截止日、且未成交(stage!=42)的订单
    List<TTran> reminders = tTranMapper.selectUpcomingFollowUp(context.getUserId(), cal.getTime(), 20);
    // ... 格式化
}
```

#### ⑤ 经营深度洞察报告（付费）
```java
@Tool("生成当前登录用户的经营深度洞察报告（增值付费能力）")
public String generateDeepInsightReport() {
    String gate = premiumGate(AiAbility.DEEP_INSIGHT);  // ← 付费墙
    if (gate != null) return gate;
    // 通过后才执行业务：汇总订单、金额、成交数、全局参照
    List<TTran> orders = tTranMapper.selectRecentByCreateBy(context.getUserId(), null, 20);
    // ......
}
```

### 3.3 管理员工具包

**文件**：`service/ai/toolkit/AdminAgentToolkit.java`

每个入口都做**二次角色断言**（纵深防御）：

```java
/** 二次角色断言：纵深防御，所有管理工具入口统一执行 */
private String adminGuard() {
    if (!context.isAdmin()) {
        log.warn("AI 管理工具越权拦截 | userId={}", context.getUserId());
        return "【权限不足】该操作仅限管理员使用。";
    }
    return null;
}
```

#### ① 导出用户 Excel（返回下载卡片）
```java
@Tool("导出全部用户数据为 Excel 文件（管理员专属）。执行成功后前端会自动收到下载卡片")
public String exportUsersExcel() {
    String deny = adminGuard();
    if (deny != null) return deny;
    List<TUser> users = tUserMapper.selectAllUsers();
    List<UserExportRow> rows = ...;   // 转换为导出行
    String fileName = "用户数据导出_" + ... + ".xlsx";
    EasyExcel.write(resolveExportFile(fileName), UserExportRow.class)
             .sheet("用户数据").doWrite(rows);
    // 向前端推送"文件就绪"事件 → 前端渲染下载按钮
    context.getEventEmitter().accept(EVENT_FILE_READY_PREFIX + fileName + "]");
    return "导出完成，已向前端推送下载卡片。";
}
```

#### ③ 附件批量导入（服务端读暂存区，不信前端传内容）
```java
@Tool("将本轮对话中用户上传的 Excel 附件批量导入用户（管理员专属）")
public String importUsersFromAttachment() {
    String deny = adminGuard();
    if (deny != null) return deny;
    String base64Content = context.getAttachmentBase64();  // ← 从上下文取服务端暂存的文件
    if (base64Content == null || base64Content.isBlank()) {
        return "【导入失败】本轮对话未携带 Excel 附件，请先上传文件。";
    }
    byte[] bytes = Base64.getDecoder().decode(base64Content.trim());
    List<UserImportRow> rows = new ArrayList<>();
    // EasyExcel 解析 ...
    int imported = userService.aiImportUsers(rows, context.getUserId());
    return String.format("导入完成：成功导入 %d 条。", imported);
}
```

#### ⑥ 修改用户权限（带防自锁保护）
```java
@Tool("修改指定用户的账号状态（启用/禁用）与角色权限（管理员专属）。"
        + "修改前必须向用户复述变更内容并得到确认；被禁用的账号会立即强制下线")
public String updateUserPermission(
        @P("目标用户ID") Integer userId,
        @P(value = "账号启用状态：1启用 0禁用；不修改时省略该参数", required = false) Integer accountEnabled,
        @P(value = "新的角色ID列表，逗号分隔；不修改时省略该参数", required = false) String roleIds) {
    String deny = adminGuard();
    if (deny != null) return deny;
    // 防自锁：不允许管理员禁用自己
    if (userId.equals(context.getUserId()) && accountEnabled != null && accountEnabled == 0) {
        return "【操作拒绝】你不能禁用当前登录的管理员账号本身。";
    }
    R result = userService.aiUpdateUserRolesAndStatus(userId, accountEnabled, parseRoleIds(roleIds),
            context.getUserId());
    // ......
}
```

---

## 四、增值付费体系（订单状态机）

### 4.1 订单实体与状态机

**文件**：`model/TAiPaymentOrder.java` + `db/ai_premium_schema.sql`

```java
public class TAiPaymentOrder implements Serializable {
    public static final int STATUS_PENDING   = 0; // 待支付
    public static final int STATUS_PAID      = 1; // 已支付
    public static final int STATUS_CANCELLED = 2; // 已取消
    // id, orderNo, userId, abilityKey, abilityName, price, status, paidTime, expireTime, createTime
}
```

状态机**单向**推进：`待支付 → 已支付` 或 `待支付 → 已取消`，禁止回退。

`expire_time`（能力到期时间 = `paid_time` + 30 天）是**付费有效期的唯一事实来源**：
付费墙只认 `status=1 AND expire_time > NOW()`；多次购买续费叠加时取 `MAX(expire_time)`。

### 4.2 支付服务（幂等 + 乐观锁）

**文件**：`service/impl/AiPaymentServiceImpl.java`

```java
@Transactional(rollbackFor = Exception.class)
public R payOrder(Integer userId, String orderNo) {
    TAiPaymentOrder order = tAiPaymentOrderMapper.selectByOrderNo(orderNo);
    // 归属校验：订单不存在或不属于当前登录人 → 统一按"找不到"处理（不泄露他人订单存在性）
    if (order == null || !order.getUserId().equals(userId)) {
        return R.FAIL(CodeEnum.AI_ORDER_NOT_FOUND);
    }
    // 幂等：已支付直接返回成功（模拟支付网关重复回调场景）
    if (order.getStatus() == TAiPaymentOrder.STATUS_PAID) return R.OK(order);

    // 状态机推进：待支付 → 已支付（SQL 乐观锁，并发双花只有一个成功）
    int updated = tAiPaymentOrderMapper.transitStatus(orderNo,
            TAiPaymentOrder.STATUS_PENDING, TAiPaymentOrder.STATUS_PAID);
    if (updated == 0) return R.FAIL(CodeEnum.AI_PAYMENT_FAILED);

    // 开通对应增值能力
    AiAbility ability = AiAbility.fromKey(order.getAbilityKey());
    if (ability != null) aiPremiumAbilityService.grantAbility(userId, ability);
    return R.OK(...);
}
```

对应乐观锁 SQL（`TAiPaymentOrderMapper.xml`）：
```xml
<!-- WHERE 锁定期望状态，杜绝 已支付→取消 等非法回退与并发双花；
     推进至已支付时同批写入 paid_time 与 expire_time（有效期 30 天），
     保证 DB 自身即付费有效期唯一事实来源，不依赖 Redis TTL 表达业务到期 -->
<update id="transitStatus">
  update t_ai_payment_order
  <set>
    status = #{targetStatus},
    <if test="targetStatus == 1">
      paid_time = NOW(),
      expire_time = DATE_ADD(NOW(), INTERVAL 30 DAY),
    </if>
  </set>
  where order_no = #{orderNo} and status = #{expectStatus}
</update>
```

### 4.3 付费开通状态服务

**文件**：`service/impl/AiPremiumAbilityServiceImpl.java`

**核心原则：数据库是付费状态与付费有效期（`expire_time`）的唯一事实来源（Single Source of Truth），Redis 仅作加速层，任何缓存丢失/故障/漂移都能通过回源查询自愈（最终一致性）。**

一致性由**三道防线**闭环保障：

| 防线 | 机制 | 覆盖场景 |
|---|---|---|
| ① 写路径 | 订单事务 `afterCommit` 才写缓存，TTL 动态对齐 DB 剩余有效期 | 事务回滚、应用内正常支付流程 |
| ② 读路径 | 正缓存→负缓存→DB 点查→SETNX 回填 | 缓存丢失/过期、并发竞态、Redis 故障降级 |
| ③ 对账兜底 | 定时任务周期扫描缓存与 DB 收敛 | **绕过应用直改数据库**（手工 UPDATE/DBA 订正） |

#### 4.3.1 有效期语义：expire_time 是唯一事实来源

旧版本存在一个**语义级不同步**：DB 的 `status=1` 是永久有效的，而 Redis 正缓存 TTL 固定 30 天——30 天后缓存过期→回源仍判定"已开通"→再回填 30 天，"30 天有效期"形同虚设。

企业级修复：订单表增加 `expire_time` 字段（= `paid_time` + 30 天，由状态机 SQL 在支付推进时同批写入）：

- **付费墙判定条件**：`status=1 AND expire_time > NOW()`（点查取 `MAX(expire_time)`，续费叠加自然取最晚到期者）；
- **缓存 TTL 动态对齐**：正缓存 TTL = DB 剩余有效期（`calcGrantTtlSeconds`），两侧到期语义完全一致；
- **存量迁移**：`db/ai_premium_migration_v2_expire_time.sql`（加列 + 按 paid_time/create_time 回填存量订单）。

#### 4.3.2 写路径（开通）——事务提交后才写缓存

`grantAbility` 通过 `TransactionSynchronizationManager` 注册 `afterCommit` 钩子，订单状态推进的事务**真正提交成功后**才写正缓存并清除负缓存：

```java
@Override
public void grantAbility(Integer userId, AiAbility ability) {
    if (userId == null || ability == null) return;

    // 缓存写入延迟到宿主事务"提交成功后"执行，
    // 避免事务回滚后 Redis 残留开通标记造成"未付款却已开通"的资损误放行
    if (TransactionSynchronizationManager.isSynchronizationActive()) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncGrantedCache(userId, ability);
            }
        });
    } else {
        syncGrantedCache(userId, ability); // 防御性分支：无事务上下文直接写
    }
}
```

> **为什么不能在事务内同步写缓存？** Redis 写操作不参与数据库事务，若在事务提交前写缓存，一旦事务回滚，会出现「未付款用户被判定已开通」的资损级误放行。且写缓存失败只记日志告警、不抛异常——事实已落库，读路径回源 + 对账任务双重自愈即可。

`syncGrantedCache` 在钩子里**重新点查 DB** 拿最新 `expire_time` 计算 TTL，而不是写死 30 天——即使同一能力被续费叠加，缓存 TTL 也始终与 DB 真相对齐。

#### 4.3.3 读路径（判定）——三级判定 + 降级容错

判定顺序：**① 正缓存命中→放行 / 负缓存命中→拦截（防缓存穿透）→ ② 均未命中→订单表点查（走索引）→ ③ 按结果 SETNX 回填正/负缓存（懒加载自愈）**：

关键实现要点：

| 机制 | 说明 |
|---|---|
| **正缓存** | `dlyk:ai:premium:{userId}:{abilityKey}` = `1`，TTL = DB 剩余有效期（最小 1 秒防脏写入），存在即代表已开通 |
| **负缓存防穿透** | 未付费用户写入 `{正缓存Key}:deny` 标记，有效期 60 秒（`AI_PREMIUM_ABILITY_DENY_EXPIRE_SECONDS`），未付费请求不再反复打库 |
| **回源点查** | `selectMaxExpireTimeOfPaid` 命中 `idx_user_ability(user_id, ability_key)` 索引，条件含 `expire_time > NOW()`，替代旧版全量拉取 + 内存过滤 |
| **SETNX 回填** | 回填用 `setValueIfAbsent`（`SET NX EX`），并发请求互不覆盖对方的最新结果（竞态防护） |
| **降级容错** | Redis 读写用 `try/catch` 包裹，故障时直查数据库，缓存层不可用不阻断付费墙主流程（可用性优先） |

#### 4.3.4 对账任务——第三道防线（覆盖手工改库）

**文件**：`task/AiPremiumCacheReconcileTask.java`

应用外的旁路修改（手工 `UPDATE t_ai_payment_order SET status=...`、DBA 数据订正、迁移脚本）不经过 `afterCommit` 钩子，缓存侧完全无感知——这是"数据库改了、Redis 没同步"的典型成因。对账任务周期性（默认 5 分钟，`project.task.ai-reconcile-delay`）将缓存与 DB 收敛：

**对账 key 空间推导**（避免全表扫描/Redis SCAN）：`selectActiveUserIds`（近 200 个有订单行为的去重用户）× `AiAbility` 付费能力枚举。

| 规则 | DB 真相 | Redis 现状 | 纠偏动作 | 纠正方向 |
|---|---|---|---|---|
| ② | 无有效开通 | 正缓存存在 | 删除正缓存 | **误放行（资损，优先纠正）** |
| ① | 已开通 | 正缓存缺失 | 补写正缓存（TTL=剩余有效期） | 误拦截 |
| ③ | 已开通 | TTL 与 DB 剩余有效期漂移>60s（含 -1 永久） | 重对齐 TTL | 语义漂移 |
| ④ | 已开通 | 负缓存存在 | 删除负缓存 | 误拦截 |

任务级 `try/catch`：Redis/DB 故障时跳过本轮、下轮重试，绝不影响调度线程；仅实际纠偏时输出 WARN 日志（附带"请核查订单表变更来源"提示，便于回溯谁动了数据库），平稳运行无日志噪音。

> **手工改库后的生效时间**：改完数据库最长等一个对账周期（默认 5 分钟）Redis 自动收敛；等不及可手动删 key：`redis-cli -a 123456 DEL dlyk:ai:premium:{userId}:{abilityKey}`（正缓存）或加 `:deny` 后缀（负缓存）。
> **注意**：手工把订单改成已支付时必须同步维护 `expire_time`（如 `DATE_ADD(NOW(), INTERVAL 30 DAY)`），否则付费墙按"无有效开通"处理——这是 DB 作为唯一事实来源的直接体现。

#### 4.3.5 设计权衡（为什么不用更强一致方案）

| 备选方案 | 不采用的原因 |
|---|---|
| 分布式事务（2PC/Seata） | 付费判定是读多写少、允许分钟级收敛的场景，强一致的复杂度与性能损耗不划算 |
| 订阅 binlog（Canal）异步同步 | 能把手工改库的感知窗口缩到秒级，但引入独立中间件，对当前项目规模属于过度设计；若未来要求秒级收敛可平滑演进 |
| **afterCommit 写缓存 + 读回源自愈 + 周期对账** ✅ | 零额外中间件的业界标准最终一致解：正常流秒级一致，旁路修改分钟级收敛，成本极低且自洽 |

> **负缓存为什么只设 60 秒？** 太长会导致「刚付款的用户短时间内仍被误拦」（体验差），太短则防穿透效果弱；60 秒是生产常见折中值，且开通时会主动清除负缓存，实际不会误拦新开通用户。
> **对账周期为什么默认 5 分钟？** 周期即"手工改库后的最大不一致窗口"，按业务容忍度权衡：越短一致性越好但 DB 点查压力越大（每轮 = 活跃用户数 × 付费能力数 次索引点查）。

---

## 五、控制器层与接口契约

**文件**：`web/AiAgentController.java`

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/ai/profile` | GET | 角色 + 能力清单（含开通状态） |
| `/api/ai/stream-chat` | GET(SSE) | 流式对话；可选 `attachmentFileId` |
| `/api/ai/payment/order` | POST | 下单 `{abilityKey}`（幂等） |
| `/api/ai/payment/pay` | POST | 支付 `{orderNo}`（模拟网关） |
| `/api/ai/payment/cancel` | POST | 取消 `{orderNo}` |
| `/api/ai/payment/orders` | GET | 我的订单列表 |
| `/api/ai/file/upload` | POST | 上传 Excel（admin） |
| `/api/ai/file/download` | GET | 下载生成的文件（admin） |

控制器**只做入口路由**，身份取 `Authentication.getPrincipal()`：
```java
@GetMapping(value = "/api/ai/stream-chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<String> streamChat(@RequestParam String message,
                               @RequestParam String memoryId,
                               @RequestParam(value="attachmentFileId", required=false) String attachmentFileId,
                               Authentication authentication) {
    TUser user = currentUser(authentication);
    return aiAssistantService.chatStream(user, memoryId, message, attachmentFileId);
}
```

---

## 六、前端实现（按角色渲染）

**文件**：`front/src/view/AiAssistantView.vue`

### 6.1 角色画像驱动渲染
```js
const profile = ref(null)
const isAdmin = computed(() => profile.value?.role === 'ADMIN')
const abilities = computed(() => profile.value?.abilities || [])

const loadProfile = async () => {
  const r = await doGet('/api/ai/profile', {})
  if (r.data?.code === 200) profile.value = r.data.data
}
```

### 6.2 快捷指令按角色动态生成
```js
const quickCommands = computed(() => {
  if (!isAdmin.value) {
    return [
      { label: '📦 查询我的订单', text: '查询我最近的订单', type: 'primary' },
      { label: '⏰ 跟进提醒', text: '未来3天我有哪些交易需要跟进联系？', type: 'success' },
      // ......
    ]
  }
  return [
    { label: '📤 导出用户Excel', text: '导出全部用户数据为Excel', type: 'primary' },
    { label: '📥 导入用户', text: '把我刚才上传的Excel附件批量导入用户', type: 'success' },
    // ......
  ]
})
```

### 6.3 SSE 事件帧解析（付费卡片/下载按钮）
大模型文本流里混有结构化事件帧，前端用正则抽取并渲染成卡片：
```js
const EVENT_REGEX = /\[AI-EVENT:(PREMIUM_REQUIRED|FILE_READY):([^\]]*)\]/g

const extractEvents = (msg) => {
  EVENT_REGEX.lastIndex = 0
  let m
  while ((m = EVENT_REGEX.exec(msg.rawBuffer)) !== null) {
    const ev = { raw: m[0], type: m[1], key: m[2] }
    if (!msg.events.some(x => x.raw === ev.raw)) {
      if (ev.type === 'PREMIUM_REQUIRED') {
        msg.events.push({ ...ev, abilityName, price, done }) // 开通卡片
      } else if (ev.type === 'FILE_READY') {
        msg.events.push({ ...ev, fileName })                 // 下载按钮
      }
    }
    msg.rawBuffer = msg.rawBuffer.replace(m[0], '')
  }
  // 处理分片截断的未闭合事件片段......
}
```

### 6.4 付费开通流程（下单→确认→支付→刷新）
```js
const buyAbility = async (ab) => {
  const r = await doPostJson('/api/ai/payment/order', { abilityKey: ab.key })
  if (r.data?.code !== 200) return ElMessage.error(r.data?.msg || '下单失败')
  const order = r.data.data
  await ElMessageBox.confirm(`确认支付 ¥${order.price} 开通「${order.abilityName}」？`, '支付确认', {...})
  const pay = await doPostJson('/api/ai/payment/pay', { orderNo: order.orderNo })
  if (pay.data?.code === 200) {
    ElMessage.success(`「${order.abilityName}」开通成功！`)
    await loadProfile()   // 刷新能力状态
  }
}
```

---

## 七、企业级安全设计要点总结

| 维度 | 设计 |
|------|------|
| **角色判定** | 服务端实时查库，不信前端传参、不信 JWT 快照 |
| **能力物理隔离** | 普通用户会话中不装载管理员工具，从根源杜绝越权 |
| **二次角色断言** | 管理工具入口再做 `adminGuard()`，纵深防御 |
| **数据行级隔离** | 用户查订单在 SQL 强制 `create_by = 登录人` |
| **跨线程身份** | 不可变 `AiAgentContext` 快照，杜绝 ThreadLocal 跨线程丢失 |
| **付费墙硬校验** | 付费状态下沉工具执行层，Prompt 诱导绕不过 |
| **支付幂等** | 订单状态机 + SQL 乐观锁 + 归属校验 |
| **文件通道安全** | 上传服务端重命名为随机 fileId、下载白名单校验、附件体积上限 |

---

## 八、部署与运行步骤

1. **建表**：执行 `server/db/ai_premium_schema.sql` 创建 `t_ai_payment_order` 订单表。
2. **启动**：`mvnw spring-boot:run`（后端）+ `npm run dev`（前端）。
3. **体验**：
   - 用**管理员账号**登录进入 AI 页 → 自动切换「管理驾驶舱」；
   - 用**普通账号**登录进入 AI 页 → 看到的是「旅客服务窗」；
   - 点击 💎 付费能力的「立即开通」→ 模拟支付 → 立即可用；
   - 打开「我的充值订单」可查看订单状态机流转。

---

## 九、扩展指南

新增一个能力**只需三步**：
1. 在 `AiAbility.java` 添加枚举项（key/名称/说明/受众/是否付费/价格）；
2. 在对应工具包（`UserAgentToolkit` 或 `AdminAgentToolkit`）实现 `@Tool` 方法；
3. 付费工具开头调用 `premiumGate(ability)` 即接入付费墙，免费工具直接写业务逻辑。

前端无需改动——能力清单由 `/api/ai/profile` 接口下发，快捷指令按需补充即可。

---

## 十、提示词外置化（Prompt as Resource）

### 10.1 设计动机

系统角色提示词、业务知识库等文案原先硬编码在 Java 代码中，每次调整话术都需要改代码重新编译。
现已外置为 classpath 资源文件，**产品/运营调文案不改 Java 代码，改完重启即生效**。

### 10.2 文件清单（`server/src/main/resources/prompts/`）

| 文件 | 用途 | 占位符 |
|------|------|--------|
| `system-common.md` | 通用角色设定（领航员人设） | `{userName}` |
| `system-role-user.md` | 普通用户的能力清单与数据边界 | 无 |
| `system-role-admin.md` | 管理员的能力清单与操作准则 | 无 |
| `attachment-current.md` | 本轮上传附件的说明 | `{fileName}` |
| `attachment-carried.md` | 会话回捞附件的中性说明 | `{fileName}` |
| `business-faq.md` | CRM 业务知识库全文（答疑工具注入） | `{question}` |

### 10.3 加载机制（`config/AiPromptProvider.java`）

```java
@PostConstruct
public void loadAll() {
    // 启动时一次性全量加载 prompts/*.md
    // 任何文件缺失或为空 → 抛 IllegalStateException 终止启动（fail-fast）
}

public String render(String name, Map<String, String> vars) {
    // 模板中的 {key} 命名占位符按变量表精确替换
}
```

设计要点：
1. **fail-fast**：提示词缺失属于部署错误，启动即失败立刻暴露，避免 AI 带病运行；
2. **常驻内存**：加载后缓存为不可变 Map，运行期零 IO；
3. **命名占位符**：`{userName}` 相比 `%s` 顺序占位符，改文案时不易错位。

### 10.4 仍保留在代码中的文案（有意为之）

- `@Tool("...")` / `@P("...")` 注解描述：Java 注解值必须是编译期常量，技术上无法外置；
- 工具返回的操作结果话术（如「导出完成：共 N 条」）：属于业务逻辑输出而非提示词，
  与数据强耦合，保留在代码中更利于维护。
