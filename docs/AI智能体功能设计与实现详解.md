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
    // id, orderNo, userId, abilityKey, abilityName, price, status, paidTime, createTime
}
```

状态机**单向**推进：`待支付 → 已支付` 或 `待支付 → 已取消`，禁止回退。

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
<!-- WHERE 锁定期望状态，杜绝 已支付→取消 等非法回退与并发双花 -->
<update id="transitStatus">
  update t_ai_payment_order
  <set>
    status = #{targetStatus},
    <if test="targetStatus == 1">paid_time = NOW(),</if>
  </set>
  where order_no = #{orderNo} and status = #{expectStatus}
</update>
```

### 4.3 付费开通状态服务

**文件**：`service/impl/AiPremiumAbilityServiceImpl.java`

**核心原则：数据库是付费状态唯一事实来源（Single Source of Truth），Redis 仅作加速层，任何缓存丢失/故障都必须能通过回源查询自愈（最终一致性）。**

#### 4.3.1 写路径（开通）——事务提交后才写缓存

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

> **为什么不能在事务内同步写缓存？** Redis 写操作不参与数据库事务，若在事务提交前写缓存，一旦事务回滚，会出现「未付款用户被判定已开通」的资损级误放行。且写缓存失败只记日志告警、不抛异常——事实已落库，读路径回源自愈即可。

#### 4.3.2 读路径（判定）——三级判定 + 降级容错

判定顺序：**① 正缓存命中→放行 / 负缓存命中→拦截（防缓存穿透）→ ② 均未命中→订单表点查（走索引）→ ③ 按结果回填正/负缓存（懒加载自愈）**：

```java
// ① 缓存判定（正/负），Redis 异常时降级直查数据库，不阻断业务
// ② 回源：countPaidByUserAndAbility 点查（走 idx_user_ability 索引）
// ③ 回填自愈：已付费写正缓存；未付费写短期负缓存防穿透
```

关键实现要点：

| 机制 | 说明 |
|---|---|
| **正缓存** | `dlyk:ai:premium:{userId}:{abilityKey}` = `1`，有效期 30 天（`AI_PREMIUM_ABILITY_EXPIRE_SECONDS`），存在即代表已开通 |
| **负缓存防穿透** | 未付费用户写入 `{正缓存Key}:deny` 标记，有效期 60 秒（`AI_PREMIUM_ABILITY_DENY_EXPIRE_SECONDS`），未付费请求不再反复打库 |
| **回源点查** | `countPaidByUserAndAbility` 命中 `idx_user_ability(user_id, ability_key)` 索引，替代旧版全量拉取 + 内存过滤 |
| **降级容错** | Redis 读写用 `try/catch` 包裹，故障时直查数据库，缓存层不可用不阻断付费墙主流程（可用性优先） |
| **回填自愈** | 无论正负结果都回填，保证缓存丢失/过期后下次读取即重建 |

#### 4.3.3 设计权衡（为什么不用更强一致方案）

| 备选方案 | 不采用的原因 |
|---|---|
| 分布式事务（2PC/Seata） | 付费判定是读多写少、允许秒级延迟的场景，强一致的复杂度与性能损耗不划算 |
| 订阅 binlog（Canal）异步同步 | 适用于强一致要求，对当前项目规模属于过度设计 |
| **事务提交后写缓存 + 读回源自愈** ✅ | 业界对读加速场景的标准最终一致解，成本极低且自洽 |

> **负缓存为什么只设 60 秒？** 太长会导致「刚付款的用户短时间内仍被误拦」（体验差），太短则防穿透效果弱；60 秒是生产常见折中值，且开通时会主动清除负缓存，实际不会误拦新开通用户。

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
