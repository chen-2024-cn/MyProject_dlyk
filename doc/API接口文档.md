# DLYK 智路 CRM · 后端 API 接口文档

> 版本基准：`server` 模块（Spring Boot 3 + Spring Security + MyBatis + Redis + LangChain4j）
> 生成时间：2026-10-18（认证章节于 2026-11 同步为双 Token）｜ 文档来源：逐一核对源码（12 个 Controller、约 90 个端点、65 处 `@PreAuthorize`、46 个权限码），非人工臆造
> 配套阅读：`docs/项目介绍与面试手册.md`、`docs/登录认证_测试与面试手册.md`、`docs/源码学习手册_*`
>
> 📌 **认证相关章节已于 2026-11 同步为双 Token 方案**：Access Token 只放前端内存、走请求头；Refresh Token 放 HttpOnly Cookie；前后端同源。涉及的章节：§1.1、§1.5、§2、§3.3、§5.1、§5.6、§5.8、§6.1、§7、§8.4、§9.2、§11.5、§18、附录 A/C/D。其余章节未改动。流程图、测试用例与面试问答见 `docs/登录认证_测试与面试手册.md`。

---

## 目录

- [一、全局约定](#一全局约定)
- [二、认证机制](#二认证机制)
- [三、统一响应与错误码](#三统一响应与错误码)
- [四、分页约定](#四分页约定)
- [五、安全机制（必读）](#五安全机制必读)
- [六、接口总览索引](#六接口总览索引)
- [七、认证与会话模块](#七认证与会话模块)
- [八、用户管理模块](#八用户管理模块)
- [九、个人资料模块](#九个人资料模块)
- [十、线索管理模块](#十线索管理模块)
- [十一、客户管理模块](#十一客户管理模块)
- [十二、交易管理模块](#十二交易管理模块)
- [十三、市场活动模块](#十三市场活动模块)
- [十四、产品管理模块](#十四产品管理模块)
- [十五、字典管理模块](#十五字典管理模块)
- [十六、统计分析模块](#十六统计分析模块)
- [十七、系统设置模块](#十七系统设置模块)
- [十八、AI 业务智能体模块](#十八ai-业务智能体模块)
- [附录 A：数据结构字典](#附录-a数据结构字典)
- [附录 B：权限码总表](#附录-b权限码总表)
- [附录 C：curl 联调速查](#附录-ccurl-联调速查)
- [附录 D：已知不一致与待改进项](#附录-d已知不一致与待改进项)

---

## 一、全局约定

### 1.1 服务地址

| 项 | 值 | 出处 |
|---|---|---|
| 后端端口 | `8089` | `application.yml` → `server.port` |
| 上下文路径 | `/`（无前缀） | `application.yml` → `server.servlet.context-path` |
| 后端根地址 | `http://localhost:8089` | — |
| 前端 baseURL | `""`（空串，请求发往页面所在的同一个源） | `front/src/http/httpRequest.js` → `axios.defaults.baseURL = ""` |
| 前端开发端口 | `5173`（Vite 默认） | — |
| 开发期代理 | `/api` → `http://localhost:8089` | `front/vite.config.js` → `server.proxy` |

> 前端现在**走 Vite 代理，与页面同源**：浏览器看到的请求地址是 `http://localhost:5173/api/...`，由 Vite 转发给 8089。同源是双 Token 方案的前提——Refresh Cookie 带 `SameSite=Strict`，跨站请求不会携带它。上线部署时要用 Nginx 之类的反向代理，让前端页面和 `/api` 保持同源。
> 直接用 Postman 或 curl 调后端时，仍然访问 `http://localhost:8089`，不经过代理。

### 1.2 字符编码与时间格式

| 项 | 值 |
|---|---|
| 字符集 | `UTF-8` |
| JSON 时间格式 | `yyyy-MM-dd HH:mm:ss`（全局） |
| 时区 | `GMT+8`（东八区） |
| 前端传日期给后端的格式 | `yyyy-MM-dd HH:mm:ss`（**必须严格一致**，见 §5.4） |

出处：`application.yml`
```yaml
spring:
  jackson:
    time-zone: GMT+8
    date-format: yyyy-MM-dd HH:mm:ss
```

### 1.3 请求格式总表（本项目共 4 种）

| 格式 | `Content-Type` | 前端调用方式 | 后端接收注解 | 用在哪些接口 |
|---|---|---|---|---|
| ① URL 查询串 | 无 | `doGet(url, params)` | `@RequestParam` / 对象自动绑定 | 所有 GET 查询、批量删除 |
| ② **表单键值对** | `application/x-www-form-urlencoded` | `doPost(url, data)` | **无注解的对象参数**（`@ModelAttribute`） | 所有 POST 新增（活动/产品/线索/交易/字典/备注）、`/api/tran/stage`、`/api/login` |
| ③ JSON | `application/json` | `doPostJson` / `doPut` | `@RequestBody` | 所有 PUT 编辑、批量删除（产品/活动）、AI 支付 |
| ④ 文件 multipart | `multipart/form-data` | `doUploadFile(url, formData)` | `MultipartFile file` | `/api/importExcel`、`/api/ai/file/upload` |

> 🔑 **最重要的坑**：POST 与 PUT 在本项目里**格式不统一**——POST 新增几乎全是**表单**，PUT 修改几乎全是 **JSON**。用 Postman / curl 联调时选错格式会报
> `415 Unsupported Media Type` 或 `400 Required request body is missing`。
> 每个接口下方都标注了「格式」，务必照抄。

### 1.4 HTTP 方法语义

| 方法 | 语义 | 幂等 | 示例 |
|---|---|---|---|
| `GET` | 查询，无副作用 | 是 | `GET /api/clues` |
| `POST` | 新增 / 触发动作 | 否（本项目部分做了幂等，见 AI 下单） | `POST /api/clue` |
| `PUT` | 全量或部分更新 | 是 | `PUT /api/clue` |
| `DELETE` | 删除 | 是 | `DELETE /api/clue/{id}` |

### 1.5 跨域（CORS）

后端全局放开（`SecurityConfig.corsConfiguration()`）：

```java
configuration.setAllowedOrigins(Arrays.asList("*"));  // 任意来源
configuration.setAllowedMethods(Arrays.asList("*"));  // 任意方法
configuration.setAllowedHeaders(Arrays.asList("*"));  // 任意请求头
```

CSRF 已禁用（`csrf(AbstractHttpConfigurer::disable)`），Session 策略为 `STATELESS`（不创建 HttpSession）。

> 前端已改为同源，这套通配配置已经不再需要：它没有开 `allowCredentials`，Cookie 本来就带不过去，所以不是漏洞，只是多余。生产环境应收窄 `AllowedOrigins` 为具体域名白名单，或直接去掉跨域配置（见附录 D.6）。

---

## 二、认证机制

### 2.1 令牌体系（双 Token）

```
POST /api/login（表单：loginAct + loginPwd + rememberMe）
        ↓ Spring Security 表单认证
MyAuthenticationSuccessHandler
        ↓
① 密码 BCrypt 比对（Spring Security 内置 DaoAuthenticationProvider）
② 生成两个 UUID：sid（会话 ID，整个登录期间不变）、jti（首张 Refresh 的编号，每次刷新都换新）
③ 签发两张 JWT：Access Token（15 分钟）、Refresh Token（记住我 7 天 / 否则 60 分钟）
④ 写入 Redis：key = "apex_sales:user:login:{userId}"，value = "sid:jti"，TTL = Refresh 有效期
   （同一账号再次登录会覆盖这个值 → 旧设备的 sid 对不上 → 单设备互斥）
⑤ Refresh Token 写入 HttpOnly Cookie（Set-Cookie 响应头）
⑥ 清除该账号的登录失败计数与锁定标记，更新"最近登录时间"
        ↓
返回 R.OK(accessToken)   ← data 是【字符串】（Access Token），不是用户对象
```

| 项 | Access Token | Refresh Token |
|---|---|---|
| 作用 | 随每个业务请求携带，证明"你是谁" | 只用来换新的 Access Token |
| 有效期 | **15 分钟**（`Constants.ACCESS_TTL_SECONDS`），与"记住我"无关 | 记住我 **7 天**（`EXPIRE_TIME` = 604800 秒）/ 否则 **60 分钟**（`DEFAULT_EXPIRE_TIME` = 3600 秒） |
| 前端存放 | **JS 内存**（`util.js` 的模块变量），刷新页面即丢 | **HttpOnly Cookie** `apex_sales_refresh`，前端代码读不到 |
| 携带方式 | 请求头 `Authorization` | 浏览器自动携带，且只会发往 `/api/auth/**` |
| 载荷 | `tokenType=access`、`sid`、`user={id,loginAct,name}`、`exp` | `tokenType=refresh`、`sub=userId`、`jti`、`sid`、`rm`（记住我）、`exp` |
| 校验方法 | `JWTUtils.verifyAccessToken` | `JWTUtils.verifyRefreshToken` |

| 其他 | 值 |
|---|---|
| 签名算法与库 | HS256，`java-jwt` 4.4.0 |
| 签名密钥来源 | 环境变量 `JWT_SECRET`（**无默认值，缺失则后端拒绝启动**） |
| 令牌用途声明 | `tokenType`（`access` / `refresh`）：两种令牌不能互相冒充，用途不符一律按"令牌有误"处理 |
| Redis Key | `apex_sales:user:login:{userId}`，值是 `sid:jti`（**不是 JWT**） |
| 令牌里没有的东西 | 密码哈希、角色、权限、菜单——权限每次请求按用户 ID 查库（见 §5.1） |
| 前端存储 | 没有任何令牌写入 `localStorage` / `sessionStorage` |

### 2.2 令牌传递方式

**Access Token：请求头（所有业务接口，没有例外）**

```http
GET /api/clues?current=1 HTTP/1.1
Authorization: eyJhbGciOiJIUzI1NiJ9.eyJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJzaWQiOi...
```

> ⚠️ **本项目没有 `Bearer ` 前缀**，`Authorization` 头的值就是裸 JWT 字符串。
> 写成 `Bearer eyJ...` 会导致 `TokenVerifyFilter` 验签失败返回 `902`。
> 出处：`front/src/http/httpRequest.js` 请求拦截器 → `config.headers['Authorization'] = token`

> 改造前，`/api/exportExcel`、`/api/ai/stream-chat`、`/api/ai/file/download` 三个接口把令牌拼在 URL 参数里（URL 会进入访问日志、浏览器历史和 Referer）。现在已全部改成请求头：导出和下载用 axios 的 `responseType: 'blob'`，SSE 用 `fetch` + `ReadableStream`，`TokenVerifyFilter` 里已经没有任何从 URL 取令牌的代码。

**Refresh Token：Cookie（前端代码从不接触它）**

| 属性 | 值 | 作用 |
|---|---|---|
| `HttpOnly` | 是 | `document.cookie` 读不到，XSS 偷不走 |
| `Secure` | 由 `auth.cookie-secure` 控制（环境变量 `COOKIE_SECURE`，默认 `false`） | 生产环境必须设为 `true`（只走 HTTPS） |
| `SameSite` | `Strict` | 跨站发起的请求不携带，挡住 CSRF |
| `Path` | `/api/auth` | 只有 `/api/auth/**` 的请求才携带，其他接口和日志里都看不到它 |
| `Max-Age` | 记住我 = 604800；否则**不设置**（会话 Cookie，关闭浏览器即消失） | 区分"记住我" |
| 清除方式 | 同名、同 Path、`Max-Age=0` | Path 必须和写入时一致，否则浏览器删不掉 |

### 2.3 Access Token 校验流程（TokenVerifyFilter）

任何非白名单请求依次通过（便宜的检查在前，查库放最后，被拒绝的请求不会白白访问数据库）：

| 序号 | 校验内容 | 失败返回 | 说明 |
|---|---|---|---|
| ① | `Authorization` 请求头非空 | `901` 请求Token参数为空 | 未登录，或前端内存里没有令牌 |
| ② | 验签、验格式、验用途（必须是 `access`） | `902` 请求Token有误 | 被篡改、格式损坏、拿 Refresh 冒充 Access |
| ③ | 验过期 | `906` Access Token 已过期，需要刷新 | 签名正确但超过 15 分钟；前端收到后**静默刷新**，用户无感 |
| ④ | Redis 里有该用户的会话 | `903` 请求Token已过期 | 已退出 / 会话到期 / 被禁用 / 改过密码 / 被踢下线 |
| ⑤ | 令牌里的 `sid` 等于 Redis 里的 `sid` | `905` 您的账号已在其他设备登录 | **单设备登录互斥**（异地登录顶号） |

②③ 在同一次验证里完成：过期（`TokenExpiredException`）归 `906`，其余校验失败归 `902`；捕获顺序不能反，因为前者是后者的子类。

全部通过后：`AuthorityManager.fill()` 按用户 ID 查库装载角色和按钮权限，把 `TUser` 包进 `UsernamePasswordAuthenticationToken` 写入 `SecurityContextHolder`，后续 Controller 通过 `Authentication` 参数或 `@PreAuthorize` 即可拿到身份。

> `code=904`（TOKEN_IS_NONE_MATCH）现在由**刷新接口**使用（Refresh 重放检测，见 §7.8），过滤器本身不返回 904。

### 2.4 免鉴权白名单（两处，必须同步改）

白名单在**两个文件**里各维护一份，新增公开接口时**两处都要加**，否则会出现「过滤器放行了但 Security 拦住」或反之。

**白名单 A：`TokenVerifyFilter.doFilterInternal()` — 跳过 JWT 校验**
```
/api/login                       登录
/api/auth/refresh                刷新令牌（靠 Cookie 认证，请求里没有 Access Token）
/api/auth/logout                 靠 Cookie 退出（兜底）
/api/register                    注册
/api/password/reset/code         忘记密码-发验证码
/api/password/reset              忘记密码-提交重置
/api/system/info/public          公开系统信息
```

**白名单 B：`SecurityConfig.authorizeHttpRequests()` — 跳过认证**
```
/api/login  /api/auth/refresh  /api/auth/logout  /api/register
/api/password/reset/code  /api/password/reset  /api/system/info/public
+ dispatcherTypeMatchers(ASYNC, ERROR).permitAll()   ← SSE 异步收尾派发，必须放行
```

> 💡 `dispatcherTypeMatchers(ASYNC, ERROR)` 这一行很关键：SSE 是异步请求，流结束后 Tomcat 会做一次 `ASYNC`/`ERROR` 派发，此时请求线程上的 `SecurityContext` 已被清空，不放行会抛 `Access Denied`。这是 Spring Security 6.1+ 处理异步流式接口的官方方案。

### 2.5 双层鉴权模型

```
第一层：认证（Authentication）—— "你是谁"
   由 TokenVerifyFilter + SecurityContext 完成
   未通过 → 9xx 业务码

第二层：授权（Authorization）—— "你能不能做这件事"
   由 @PreAuthorize + @EnableMethodSecurity 完成（方法级鉴权）
   未通过 → MyAccessDeniedHandler → {"code":500,"msg":"权限不足"}

第三层：数据权限（Data Scope）—— "你能看到哪些行的数据"
   由 @DataScope 注解 + DataScopeAspect 切面完成
   非 admin 用户的分页列表自动追加 SQL 过滤：and ta.owner_id = #{当前用户id}
```

**当前启用数据权限的接口（仅 2 个 Mapper）**：

| Mapper 方法 | 表别名.字段 | 影响的接口 |
|---|---|---|
| `TActivityMapper`（活动分页） | `ta.owner_id` | `GET /api/activities` |
| `TUserMapper`（用户分页） | `tu.id` | `GET /api/users` |

> `admin` 角色不做过滤（切面显式置 `filterSQL = null`）；普通用户只能看到自己名下的活动/自己这条用户记录。
> 线索、客户、交易列表**未挂 `@DataScope`**，即普通用户可查全量——这是业务口径问题，见[附录 D](#附录-d已知不一致与待改进项)。

---

## 三、统一响应与错误码

### 3.1 统一响应信封 `R`

除**文件下载类接口**（返回二进制流）外，所有接口都返回同一信封结构：

```java
public class R {
    private int    code;   // 业务结果码
    private String msg;    // 提示文案（可直接展示给用户）
    private Object data;   // 业务数据：对象 / 数组 / 字符串 / null
}
```

**成功示例**
```json
{
  "code": 200,
  "msg": "成功",
  "data": { "pageNum": 1, "pageSize": 10, "total": 37, "list": [ /* ... */ ] }
}
```

**失败示例**
```json
{ "code": 500, "msg": "该用户已存在" }
```

> 📌 失败时 `data` 字段为 `null`，Jackson 默认仍会序列化输出（本项目未配置 `NON_NULL`），前端判空请用 `r.data?.data`。

### 3.2 关键认知：HTTP 状态码 ≠ 业务结果

**本项目所有业务失败都返回 `HTTP 200`**，成败只体现在响应体的 `code` 字段。前端必须判两层：

```js
const r = await doGet('/api/clues', { current: 1 })
if (r.data?.code === 200) {        // r.status(HTTP)=200 且 r.data.code(业务)=200
  const list = r.data.data.list    // 才是真数据
} else {
  ElMessage.error(r.data.msg)      // 否则展示后端文案
}
```

| 层次 | 含义 | 本项目实际取值 |
|---|---|---|
| HTTP 状态码 | 传输层结果 | 正常业务恒为 `200`；仅文件下载校验失败会返回 `400`/`404` |
| `R.code` | 业务层结果 | `200` 成功，其余见下表 |

**例外：会返回非 200 HTTP 状态码的场景**

| HTTP 状态 | 场景 | 有无 JSON 体 |
|---|---|---|
| `400 Bad Request` | `/api/ai/file/download` 文件名非法（含 `/` `\` `..` 或非 xlsx/xls） | ❌ 空体 |
| `404 Not Found` | `/api/ai/file/download` 文件不存在 | ❌ 空体 |
| `415 Unsupported Media Type` | Content-Type 与接口期望不符（如给表单接口发 JSON） | Spring 默认错误体 |

### 3.3 完整错误码表

**通用码**

| code | msg | 触发场景 | 前端处理 |
|---|---|---|---|
| `200` | 成功 | 正常 | 取 `data` |
| `500` | 失败 | 未指定文案的通用失败（`R.FAIL()`） | 提示 |
| `500` | 数据库操作失败 | `DataAccessException` | 提示 |
| `500` | 权限不足 | `AccessDeniedException`（`@PreAuthorize` 未通过） | 提示 + 检查权限码 |
| `500` | 数据已存在，请勿重复添加 | `DuplicateKeyException`（并发撞唯一键兜底） | 提示 |
| `500` | 服务器繁忙，请稍后重试 | 其他未分类系统异常（**响应脱敏**，堆栈只落服务端日志） | 提示 |
| `500` | 缺少必填参数：xxx | `MissingServletRequestParameterException` | 提示 |
| `500` | 参数「xxx」格式不正确 | `MethodArgumentTypeMismatchException` | 提示 |
| `500` | {字段名}：{校验消息} | `MethodArgumentNotValidException`（`@Valid`） | 提示 |
| `500` | 自定义业务文案 | `BusinessException`（业务规则拦截，如"非相邻阶段不可推进"） | 提示 |

**令牌认证码（900 段）—— 由 `TokenVerifyFilter` 与刷新接口返回，非 Controller**

| code | msg | 含义 | 前端处理 |
|---|---|---|---|
| `901` | 请求Token参数为空 | 请求头没带令牌；或刷新接口没收到 Cookie（未登录用户打开登录页时的正常情况） | 普通请求：弹窗「是否重新登录」→ 跳登录页；`restoreSession()` 里静默当作"未登录" |
| `902` | 请求Token有误 | 验签失败、格式损坏、拿 Refresh 冒充 Access（或反过来） | 同上 |
| `903` | 请求Token已过期 | Redis 里已没有会话：已退出 / 会话到期 / 被禁用 / 改过密码 / 被踢；或刷新时发现账号已失效；或 Refresh 本身过期 | 同上 |
| `904` | 请求Token不匹配 | **Refresh 重放**：`sid` 对得上、`jti` 对不上（旧 Refresh 被再次使用），或并发刷新竞争失败。服务端销毁整个会话 | 同上 |
| `905` | 您的账号已在其他设备登录 | **异地登录顶号**（`sid` 不一致） | 专属弹窗「账号异地登录」→ 强制跳登录页 |
| `906` | Access Token 已过期，需要刷新 | 签名正确但已超过 15 分钟，**只由过滤器返回** | **静默刷新**：调 `POST /api/auth/refresh`，成功后重放原请求，用户无感；刷新失败则按刷新接口返回的 901~905 处理 |

前端拦截逻辑（`httpRequest.js` 响应拦截器，按下面的顺序处理）：
1. 若是 `responseType: 'blob'` 的下载请求，且返回体其实是 JSON，先把 Blob 还原成对象（否则读不到 `code`）；
2. `code === 906` 且请求没被标记过 `_retried` → `refreshAccessToken()`：成功就用 `axios(response.config)` 重放；被后端拒绝就用刷新接口返回的 901~905 顶替本次响应；网络故障只让本次请求失败，不弹"重新登录"；
3. `code === 905` → 清令牌，弹「账号异地登录」，整页回登录页；
4. `code > 900`（901~904）→ 弹「是否重新登录」。

并用模块级布尔锁 `authAlertOpen` 保证：**页面挂载时多个并发请求同时收到 9xx，只弹一个对话框**（这是修过的真实缺陷，旧版会叠出两层以上弹窗）。刷新本身也做了防并发：同一页面内用 `refreshPromise` 单飞，跨标签页用 `navigator.locks` 串行。

**退出登录**：`R.OK(CodeEnum.USER_LOGOUT)` → `{"code":200,"msg":"退出成功"}`（注意 code 是 200 不是专门码）

**AI 增值付费码（3000 段）**

| code | msg | 触发场景 |
|---|---|---|
| `3001` | 指定的增值能力不存在或未上架 | `abilityKey` 非法，或该能力 `premium=false`（免费能力不该下单） |
| `3002` | 订单不存在或无权操作 | `orderNo` 不存在，**或订单不属于当前登录人**（防横向越权，统一按"找不到"处理，不泄露他人订单存在性） |
| `3003` | 订单状态不允许该操作 | 对已取消订单调支付；对已支付订单调取消 |
| `3004` | 支付网关处理失败，请稍后重试 | 状态机乐观锁推进失败（并发双花，只有一个请求能成功） |

### 3.4 全局异常处理策略（GlobalExceptionHandler）

按异常性质二分处理，核心是**响应脱敏**：

| 异常类型 | 处理策略 | 日志级别 |
|---|---|---|
| `BusinessException` | **透传** message（开发者手写、面向用户的文案） | `warn` |
| 参数绑定/校验类（4 种） | 现场生成安全的字段级提示 | `warn` |
| 其余 `Exception` | **统一为「服务器繁忙，请稍后重试」**，绝不用 `e.getMessage()` | `error`（含完整堆栈） |

设计理由：底层异常 message 会暴露表名、字段名、驱动版本、SQL 片段，攻击者可用这些信息构造后续攻击（信息泄露类漏洞 CWE-209）。

---

## 四、分页约定

### 4.1 请求

所有列表接口统一用 PageHelper，参数名固定：

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `current` | Integer | 否 | 页码，从 **1** 开始；不传或传 null 时后端默认为 1 |

**每页条数固定为 10，不可由前端传参**（`Constants.PAGE_SIZE = 10`）。

```http
GET /api/clues?current=2&fullName=张&source=1 HTTP/1.1
```

### 4.2 响应（PageHelper 的 `PageInfo` 结构）

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "pageNum": 2,          // 当前页码
    "pageSize": 10,        // 每页条数
    "size": 10,            // 当前页实际条数
    "total": 37,           // 总记录数
    "pages": 4,            // 总页数
    "list": [ /* 本页数据 */ ],
    "startRow": 11,
    "endRow": 20,
    "prePage": 1,
    "nextPage": 3,
    "isFirstPage": false,
    "isLastPage": false,
    "hasPreviousPage": true,
    "hasNextPage": true,
    "navigatePages": 8,
    "navigatepageNums": [1,2,3,4],
    "navigateFirstPage": 1,
    "navigateLastPage": 4
  }
}
```

前端常用字段：`data.total`（总条数，给分页器）、`data.list`（表格数据）、`data.pages`（总页数）。

### 4.3 筛选条件传参方式

各列表接口除 `current` 外，其余筛选字段由 Spring **自动绑定到 Query 对象**（不是 `@RequestParam`）：直接平铺在查询串上，字段名与 Query 类的属性名一一对应（驼峰）。

```http
GET /api/users?current=1&loginAct=zhang&accountEnabled=1&roleId=2
```
等价于后端 `UserQuery userQuery` 的 `loginAct="zhang"`、`accountEnabled=1`、`roleId=2`。不传的字段为 `null`，SQL 里用 `<if test="xxx != null">` 动态拼接，因此**所有筛选字段都是可选的，全不传即全量分页**。

---

## 五、安全机制（必读）

本项目做过多轮安全加固，以下机制会直接影响你怎么调用接口。

### 5.1 令牌负载最小化与权限服务端装载

JWT 的 payload 只是 **base64 编码，不是加密**——任何持有令牌的人（浏览器插件、XSS 攻击者）解码即可看到内容。所以令牌里只放识别身份所需的最少信息。

**演进过程**：
1. 最早的版本把整个 `TUser` 序列化进令牌，等于把 BCrypt 密码哈希发给了客户端，可离线爆破；
2. 第一次修复：序列化前 `setLoginPwd(null)`（黑名单：只清已知敏感字段）；
3. 现在：`JWTUtils.toTokenUserJson()` 用**白名单**——只放 `id`、`loginAct`、`name`。以后有人给 `TUser` 加了敏感字段，也不会悄悄进到每个令牌里。

**令牌里不再有角色和权限**。原因：
- **体积**：实测带权限的令牌有 5671 字符，逼近 Tomcat 默认 8KB 的请求头上限；
- **新鲜度**：令牌里的权限是签发时刻的快照，管理员撤销权限后要等令牌过期才生效；
- **可见性**：载荷人人可读。

**权限怎么来**：`TokenVerifyFilter` 在每个请求通过前面的校验之后，调 `AuthorityManager.fill()` 按用户 ID 查库装载角色和按钮权限。管理员改完角色，对方的**下一个请求**就按新权限判定。

> `AuthorityManager` 刻意只依赖两个 Mapper，不依赖 `UserService`：否则会形成 `SecurityConfig → TokenVerifyFilter → AuthorityManager → UserService → PasswordEncoder（定义在 SecurityConfig）` 的循环依赖。"admin 隐式拥有全部按钮权限"这条规则也只在 `AuthorityManager.resolveButtonPermissions()` 里定义一份，`UserServiceImpl` 反过来调用它。
> 代价：每个请求（包括 5 秒一次的心跳）要查 2 次库。后续可以加 Redis 缓存，改角色时主动删缓存。

**推论**：
- `authentication.getPrincipal()` 拿到的 `TUser` 里，`id`/`loginAct`/`name` 来自令牌，`roleList`/`permissionList` 来自数据库，`loginPwd` 为 null；
- 涉及密码校验或最新资料的场景，仍然必须重新查库：
  - `UserController.changePassword()` → `userService.getUserById()` 实时查（需要完整密码哈希）
  - `UserController.LoginInfo()` → `userService.getUserInfo()` 实时查（用户改完资料立刻可见）

### 5.2 `filterSQL` 参数注入防护（两层纵深）

`BaseQuery.filterSQL` 是数据权限切面写入的 SQL 过滤片段，最终以 `${filterSQL}`（**字符串直接拼接，非预编译**）进 SQL。若外部可写入即构成 SQL 注入（CWE-89）。

| 防护层 | 实现 | 效果 |
|---|---|---|
| 第 1 层 | `SecureBinderAdvice` → `binder.setDisallowedFields("filterSQL")` | 全局拒收：任何 Controller、任何 Query 子类，URL 上带 `?filterSQL=or 1=1` 都被绑定层丢弃 |
| 第 2 层 | `DataScopeAspect` admin 分支显式 `adminQuery.setFilterSQL(null)` | 兜底：任何旁路都无法携带过滤片段进 SQL |

> ⚠️ `@InitBinder` **只作用于表单/查询串绑定**（`@ModelAttribute`、`@RequestParam`），不影响 `@RequestBody` 的 Jackson 反序列化。但 `@RequestBody` 的写路径最终都经 `BeanUtils.copyProperties` 拷进实体（实体无 `filterSQL` 属性），故不构成注入面。

### 5.3 权限收敛（历史漏洞已修）

以下接口早期**无任何鉴权**，审计后收敛到具体权限码：

| 接口 | 收敛后权限 | 修复的风险 |
|---|---|---|
| `GET /api/clue/{value}`（手机查重） | `clue:add` 或 `clue:edit` | 任意登录用户可遍历探测系统内是否存在某手机号（客户隐私枚举泄露） |
| `POST /api/clue/customer`（线索转客户） | `clue:edit` | 转化是不可逆核心动作（线索 `state` 置 -1），任意用户可伪造转化 |
| `GET /api/tran/remark`、`GET /api/clue/remark`、活动备注读取 | `tran:view` / `clue:view` / `activity:view` | 跟进记录含客户沟通隐私 |
| `POST /api/tran/stage`（阶段推进） | `tran:edit` | 直接影响成交金额统计，任意用户可篡改 |
| `GET /api/customer/options`（客户下拉） | `tran:add` 或 `tran:edit` | 下拉含全量客户姓名 |
| `GET /api/user/{id}`（用户详情） | `user:view` | 详情返回前置空 `loginPwd`，避免 BCrypt 哈希随详情下发 |

**AI 模块的角色隔离走的是"物理隔离"而非运行时拦截**：普通用户会话只挂载 `UserAgentToolkit`，管理员会话只挂载 `AdminAgentToolkit`——管理员工具在普通用户的会话里**根本不存在**，模型无法调用。角色判定实时查库（`AiRoleResolverService`），不信任前端传参（旧版前端传 `permissions` 方案存在伪造风险，已废弃）。

### 5.4 日期参数格式必须严格对齐

`POST /api/tran/stage` 的 `expectedDate` 加了 `@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")`：

**踩过的坑（已实测复现修复）**：前端 `el-date-picker` 设了 `value-format="YYYY-MM-DD HH:mm:ss"`，传的是字符串；后端旧版 `@RequestParam Date` 无 `@DateTimeFormat` 时 Spring 无法绑定，抛 `MethodArgumentTypeMismatchException` → 500。

**规则**：表单接口传日期，一律用 `yyyy-MM-dd HH:mm:ss` 格式字符串。

### 5.5 自我保护边界

| 接口 | 规则 | 返回 |
|---|---|---|
| `DELETE /api/user/{id}` | 禁止删除当前登录人自己 | `{"code":500,"msg":"不能删除当前登录的账号，请使用其他管理员账号操作"}` |
| `DELETE /api/user?ids=` | 批量名单中含自己时整体拒绝 | `{"code":500,"msg":"删除名单中包含当前登录账号，请先将其移除"}` |
| `POST/PUT/DELETE` 活动备注 | 非管理员只能操作自己创建的备注；**管理员可操作任意备注** | `{"code":500,"msg":"只能修改自己创建的备注"}` |

> 删自己的账号会连带清掉 Redis 里的登录 token，导致"刚删完就被踢下线"的混乱状态，故直接拒绝。

### 5.6 登录防爆破与限流

| 机制 | 阈值 | Redis Key |
|---|---|---|
| 登录失败计数 | 连续 5 次失败 → 锁定账号 | `apex_sales:login:fail:{loginAct}`（计数器 10 分钟过期） |
| 账号锁定 | 30 分钟 | `apex_sales:login:lock:{loginAct}` |
| 忘记密码发码限流 | 同一账号 60 秒内只能发 1 次 | `apex_sales:reset_pwd:limit:{loginAct}` |
| 重置验证码有效期 | 300 秒（5 分钟） | `apex_sales:reset_pwd:code:{loginAct}` |
| 验证码错误次数熔断 | 连续 5 次错误 → 验证码物理抹除 | `apex_sales:reset_pwd:fail_count:{loginAct}` |

**登录账号会先归一化再拼 Redis key**（`Constants.normalizeLoginAct`：去首尾空格 + 转小写）。原因：数据库 `login_act` 的排序规则是 `utf8mb3_general_ci`，不区分大小写、忽略尾随空格，`admin`、`Admin`、`admin ` 登录的是同一个账号；如果 key 直接拼原始输入，攻击者换着写法就能绕过锁定无限次试密码。失败计数 key、锁 key 的所有读写位置（失败处理器、`loadUserByUsername`、成功处理器、删除用户、找回密码成功）必须用同一个归一化结果。

**计数器自愈**：`INCR` 和 `EXPIRE` 是两条命令，进程恰好在两条之间挂掉，计数器就永不过期。所以每次失败都检查 TTL，发现是 `-1`（存在但没设过期）就补上。找回密码的错误计数同理，并且用原子的 `INCR` 代替"读出、加一、写回"，并发提交不会互相覆盖。

登录失败提示会**告知剩余尝试次数**：`"用户名或密码错误！您还可以尝试 3 次。"`
账号锁定时：`"密码输入错误已达 5 次，账户已被锁定30分钟！"`
已锁定状态下再试：`"该账号因连续多次密码输入错误已被锁定，请在大约 30 分钟后再试！"`
管理员手动锁定（数据库 `account_no_locked=0`，没有 Redis 锁）：`"该账号已被管理员锁定，请联系管理员处理。"`
账号被禁用：`"该账号已被禁用，请联系管理员处理。"`
账号或密码已过期：`"该账号或密码已过期，请联系管理员处理。"`

### 5.6.1 会话失效清单（什么时候会话被销毁）

登录会话就是 Redis 里的 `apex_sales:user:login:{userId}`，删掉它，该用户的 Access（下一个请求）和 Refresh（下一次刷新）都会失效：

| 触发场景 | 代码位置 |
|---|---|
| 用户点击退出（仅当 `sid` 与 Redis 一致） | `MyLogoutSuccessHandler` |
| 靠 Cookie 退出（兜底，同样只删自己这次登录的会话） | `AuthController.logoutByCookie` |
| 修改本人密码 | `UserServiceImpl.changePassword` |
| 找回密码成功 | `UserServiceImpl.executeResetPassword` |
| 管理员编辑用户：填了新密码，或把账号设为禁用 / 锁定 / 账号过期 / 密码过期 | `UserServiceImpl.updateUser` |
| 管理员编辑用户：角色集合发生变化 | `UserServiceImpl.invalidateSessionIfRolesChanged` |
| 删除用户（单个 / 批量） | `UserServiceImpl.deleteById` / `batchDelUserId` |
| AI 工具禁用账号或改角色 | `UserServiceImpl.aiUpdateUserRolesAndStatus` |
| 刷新时发现 Refresh 被重放（`jti` 对不上，或并发刷新竞争失败） | `AuthController.refresh` |
| 刷新时发现账号已被禁用 / 锁定 / 过期 | `AuthController.refresh` |

### 5.7 文件接口的路径穿越防护

`/api/ai/file/download` 的 `fileName` 做白名单校验，任一条不满足即 `400`：
- 非空
- 不含 `/`、`\`、`..`
- 必须以 `.xlsx` 或 `.xls` 结尾

`/api/ai/file/upload` 上传后**服务端随机重命名**为 16 位 hex 的 `fileId.xlsx`，客户端原始文件名只记录在日志里、绝不参与路径拼接，从根源杜绝路径注入。

AI 附件读取时的额外校验：`attachmentFileId` 必须匹配 `^[a-zA-Z0-9]{8,32}$`，且文件体积 ≤ 1.5 MB（`ATTACHMENT_MAX_BYTES`）。

---

## 六、接口总览索引

图例：**鉴权**列中 `—` 表示仅需登录（无 `@PreAuthorize`），`公开` 表示完全免登录。

### 6.1 认证与会话（7 个，另有 2 个靠 Cookie 认证的接口，不占序号）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 1 | POST | `/api/login` | 公开 | 登录，响应体返回 Access Token，同时 `Set-Cookie` 写入 Refresh Token |
| 2 | POST | `/api/logout` | 需登录 | 退出登录（清会话、清 Cookie） |
| 3 | POST | `/api/register` | 公开 | 注册（不绑定任何角色） |
| 4 | GET | `/api/password/reset/code` | 公开 | 忘记密码-获取验证码 |
| 5 | PUT | `/api/password/reset` | 公开 | 忘记密码-核验并重置 |
| 6 | GET | `/api/login/info` | — | 当前登录人完整信息（实时查库） |
| 7 | GET | `/api/login/free` | — | 免登录探活（前端 5 秒一次的心跳） |
| — | POST | `/api/auth/refresh` | 公开（靠 Cookie） | **刷新令牌**：用 Refresh Cookie 换新的 Access，同时轮换 Refresh |
| — | POST | `/api/auth/logout` | 公开（靠 Cookie） | **靠 Cookie 退出**：Access 已失效或常规退出异常时的兜底，永远返回 200 |

> 两个新增接口放行的原因：它们靠 Refresh Cookie 认证，请求里没有 Access Token。它们的 Cookie `Path=/api/auth`，浏览器只会在请求这个路径下的接口时才带上。

### 6.2 用户管理（8 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 8 | GET | `/api/users` | `user:list` | 用户分页（受数据权限约束） |
| 9 | GET | `/api/user/{id}` | `user:view` | 用户详情（密码置空） |
| 10 | POST | `/api/user` | `user:add` | 新增用户（表单） |
| 11 | PUT | `/api/user` | `user:edit` | 编辑用户（JSON） |
| 12 | DELETE | `/api/user/{id}` | `user:delete` | 删除单个用户 |
| 13 | DELETE | `/api/user` | `user:delete` | 批量删除用户 |
| 14 | GET | `/api/owner` | — | 负责人下拉（Redis 缓存） |
| 15 | GET | `/api/roles` | `user:add` 或 `user:edit` | 角色下拉 |

### 6.3 个人资料（2 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 16 | PUT | `/api/profile` | — | 修改本人姓名/手机/邮箱 |
| 17 | PUT | `/api/profile/password` | — | 修改本人密码 |

### 6.4 线索管理（12 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 18 | GET | `/api/clues` | `clue:list` | 线索分页（多条件筛选） |
| 19 | GET | `/api/clue/detail/{id}` | `clue:view` | 线索详情 |
| 20 | POST | `/api/clue` | `clue:add` | 新增线索（表单） |
| 21 | PUT | `/api/clue` | `clue:edit` | 编辑线索（JSON） |
| 22 | DELETE | `/api/clue/{id}` | `clue:delete` | 删除单个线索 |
| 23 | DELETE | `/api/clue/batch` | `clue:delete` | 批量删除线索 |
| 24 | GET | `/api/clue/{value}` | `clue:add` 或 `clue:edit` | 手机号查重 |
| 25 | POST | `/api/importExcel` | `clue:import` | Excel 批量导入线索 |
| 26 | GET | `/api/clue/remark` | `clue:view` | 线索跟进记录分页 |
| 27 | POST | `/api/clue/remark` | `clue:edit` | 新增跟进记录 |
| 28 | PUT | `/api/clue/remark/update` | `clue:edit` | 编辑跟进记录 |
| 29 | DELETE | `/api/clue/remark/delete/{id}` | `clue:delete` | 删除跟进记录 |

### 6.5 客户管理（5 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 30 | POST | `/api/clue/customer` | `clue:edit` | 线索转客户（不可逆） |
| 31 | GET | `/api/customers` | `customer:list` | 客户分页 |
| 32 | GET | `/api/customer/{id}` | `customer:view` | 客户详情 |
| 33 | DELETE | `/api/customer/{id}` | `customer:delete` | 删除客户 |
| 34 | GET | `/api/exportExcel` | `customer:export` | 导出客户 Excel（二进制流） |

### 6.6 交易管理（14 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 35 | GET | `/api/trans` | `tran:list` | 交易分页（可按阶段筛选） |
| 36 | GET | `/api/tran/{id}` | `tran:view` | 交易详情 |
| 37 | POST | `/api/trans` | `tran:add` | 新增交易（表单） |
| 38 | PUT | `/api/trans` | `tran:edit` | 编辑交易（JSON） |
| 39 | DELETE | `/api/tran/{id}` | `tran:delete` | 删除单笔交易 |
| 40 | DELETE | `/api/tran/batch` | `tran:delete` | 批量删除交易 |
| 41 | POST | `/api/tran/stage` | `tran:edit` | **交易阶段推进/回退** |
| 42 | GET | `/api/tran/{tranId}/history` | `tran:view` | 阶段变更历史 |
| 43 | GET | `/api/customer/{customerId}/trans` | `tran:list` | 某客户下的交易分页 |
| 44 | GET | `/api/customer/options` | `tran:add` 或 `tran:edit` | 客户下拉 |
| 45 | GET | `/api/tran/remark` | `tran:view` | 交易跟进记录分页 |
| 46 | POST | `/api/tran/remark` | `tran:edit` | 新增跟进记录 |
| 47 | PUT | `/api/tran/remark` | `tran:edit` | 编辑跟进记录 |
| 48 | DELETE | `/api/tran/remark/{id}` | `tran:delete` | 删除跟进记录 |

### 6.7 市场活动（9 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 49 | GET | `/api/activities` | `activity:list` | 活动分页（**受数据权限约束**） |
| 50 | POST | `/api/activities` | `activity:add` | 新增活动（表单） |
| 51 | PUT | `/api/activities/{id}` | `activity:edit` | 编辑活动（JSON） |
| 52 | DELETE | `/api/activities/{id}` | `activity:delete` | 删除活动 |
| 53 | POST | `/api/activities/batch-delete` | `activity:delete` | 批量删除活动（JSON） |
| 54 | GET | `/api/activities/{activityId}/remarks` | `activity:view` | 活动备注列表（不分页） |
| 55 | POST | `/api/activities/{activityId}/remarks` | `activity:edit` | 新增备注 |
| 56 | PUT | `/api/activities/remarks/{remarkId}` | `activity:edit` | 编辑备注 |
| 57 | DELETE | `/api/activities/remarks/{remarkId}` | `activity:delete` | 删除备注（逻辑删除） |

### 6.8 产品管理（5 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 58 | GET | `/api/products` | `product:list` | 产品分页（可按指导价区间筛选） |
| 59 | POST | `/api/products` | `product:add` | 新增产品（表单） |
| 60 | PUT | `/api/products/{id}` | `product:edit` | 编辑产品（JSON） |
| 61 | DELETE | `/api/products/{id}` | `product:delete` | 删除产品 |
| 62 | POST | `/api/products/batch-delete` | `product:delete` | 批量删除产品（JSON） |

### 6.9 字典管理（10 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 63 | GET | `/api/dicvalue/{typeCode}` | — | **本地缓存**字典值（下拉框专用，高性能） |
| 64 | GET | `/api/dictypes` | `dictype:list` | 字典类型分页 |
| 65 | GET | `/api/dictypes/all` | `dictype:list` 或 `dicvalue:list` | 全部字典类型（不分页） |
| 66 | POST | `/api/dictypes` | `dictype:add` | 新增类型（表单） |
| 67 | PUT | `/api/dictypes/{id}` | `dictype:edit` | 编辑类型（JSON） |
| 68 | DELETE | `/api/dictypes/{id}` | `dictype:delete` | 删除类型 |
| 69 | GET | `/api/dicvalues` | `dicvalue:list` | 字典值分页 |
| 70 | POST | `/api/dicvalues` | `dicvalue:add` | 新增字典值（表单） |
| 71 | PUT | `/api/dicvalues/{id}` | `dicvalue:edit` | 编辑字典值（JSON） |
| 72 | DELETE | `/api/dicvalues/{id}` | `dicvalue:delete` | 删除字典值 |

### 6.10 统计分析（4 个，均仅需登录）

| # | 方法 | 路径 | 说明 |
|---|---|---|---|
| 73 | GET | `/api/summary/data` | 首页概览六项汇总指标 |
| 74 | GET | `/api/saleFunnel/data` | 销售漏斗（线索→客户→交易→成交） |
| 75 | GET | `/api/sourcePie/data` | 线索来源分布饼图 |
| 76 | GET | `/api/trend/data` | 月度趋势（线索数/客户数/交易额） |

### 6.11 系统设置（3 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 77 | GET | `/api/system/info` | `system:view` | 系统信息（完整） |
| 78 | GET | `/api/system/info/public` | 公开 | 系统信息（脱敏子集，含主题码） |
| 79 | PUT | `/api/system/info` | `system:edit` | 修改系统信息（JSON） |

### 6.12 AI 业务智能体（10 个）

| # | 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|---|
| 80 | GET | `/api/ai/profile` | — | AI 角色画像 + 能力清单 |
| 81 | GET | `/api/ai/stream-chat` | — | **SSE 流式对话（核心）** |
| 82 | GET | `/api/ai/reset` | — | 清空会话记忆 + 聊天记录 |
| 83 | GET | `/api/ai/history` | — | 恢复聊天记录 |
| 84 | POST | `/api/ai/payment/order` | — | 创建付费能力订单 |
| 85 | POST | `/api/ai/payment/pay` | — | 模拟支付并完成开通 |
| 86 | POST | `/api/ai/payment/cancel` | — | 取消待支付订单 |
| 87 | GET | `/api/ai/payment/orders` | — | 我的充值订单列表 |
| 88 | POST | `/api/ai/file/upload` | `admin` | 上传待导入 Excel |
| 89 | GET | `/api/ai/file/download` | `admin` | 下载 AI 生成的 Excel（二进制流） |

---

## 七、认证与会话模块

### 7.1 登录

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/login` |
| 鉴权 | **公开**（白名单） |
| 请求格式 | **表单** `application/x-www-form-urlencoded` |
| 实现方 | Spring Security 内置 `formLogin`（**没有 Controller**），成功走 `MyAuthenticationSuccessHandler` |

**请求参数**（都在表单里，没有特殊请求头）

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `loginAct` | String | 是 | 登录账号（被 `usernameParameter("loginAct")` 映射为用户名字段）。大小写不敏感、忽略首尾空格 |
| `loginPwd` | String | 是 | 登录密码明文（`passwordParameter("loginPwd")`），后端用 BCrypt 比对 |
| `rememberMe` | String | 否 | `"true"` → Refresh Token 和 Cookie 有效期 7 天（持久 Cookie）；其他或不传 → 60 分钟（会话 Cookie，关闭浏览器即消失） |

**请求示例**
```http
POST /api/login HTTP/1.1
Host: localhost:8089
Content-Type: application/x-www-form-urlencoded

loginAct=admin&loginPwd=admin123&rememberMe=true
```

**成功响应** —— 注意 `data` 是 **Access Token 字符串本身**，不是用户对象；Refresh Token 在 `Set-Cookie` 响应头里：
```http
HTTP/1.1 200 OK
Content-Type: application/json;charset=UTF-8
Set-Cookie: apex_sales_refresh=eyJ...; Path=/api/auth; Max-Age=604800; HttpOnly; SameSite=Strict

{
  "code": 200,
  "msg": "成功",
  "data": "eyJhbGciOiJIUzI1NiJ9.eyJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJzaWQiOi..."
}
```
`rememberMe=false` 时 `Set-Cookie` 里没有 `Max-Age`；`COOKIE_SECURE=true` 时还会带 `Secure`。

**失败响应**（`MyAuthenticationFailureHandler`，HTTP 仍为 200）
```json
{ "code": 500, "msg": "用户名或密码错误！您还可以尝试 4 次。" }
{ "code": 500, "msg": "密码输入错误已达 5 次，账户已被锁定30分钟！" }
{ "code": 500, "msg": "该账号因连续多次密码输入错误已被锁定，请在大约 30 分钟后再试！" }
{ "code": 500, "msg": "该账号已被管理员锁定，请联系管理员处理。" }
{ "code": 500, "msg": "该账号已被禁用，请联系管理员处理。" }
```
账号不存在和密码错误的提示**完全相同**（Spring Security 把"账号不存在"也包装成 `BadCredentialsException`），防止枚举账号。

**登录成功后后端还做了**：更新"最近登录时间"（`TUserMapper.updateLastLoginTimeById`，单列更新）、清除该账号的失败计数和锁。

**前端处理流程**（`LoginForm.vue` 的 `handleLogin`）
```js
const response = await doPost('/api/login', { loginAct, loginPwd, rememberMe })
if (response.data.code === 200) {
  removeToken()                          // 先清内存令牌 + 旧权限缓存（防串号）
  setAccessToken(response.data.data)     // 令牌只放内存；Refresh Cookie 由浏览器保管，JS 碰不到
  await router.push(resolveRedirect())   // 回到登录前想去的页面，没有就去 /dashboard
} else {
  ElMessage.error(response.data.msg)
}
```

`resolveRedirect()` 读 `?redirect=`（路由守卫在未登录时带上的原目标页），**只接受站内路径**：必须以 `/` 开头，且不能以 `//` 或 `/\` 开头（协议相对地址，会跳去别的站点），也不能指向登录页自己。不做这个校验就是"开放重定向"：攻击者发一条 `?redirect=//evil.com` 的链接，用户登录成功后就被带去钓鱼站。

> ⚠️ **易错点 1**：登录响应的 `data` 是字符串，**不能** `response.data.data.loginAct`（恒为 `undefined`）。用户名要跳转后调 `/api/login/info` 获取。
> ⚠️ **易错点 2**：登录接口用 `doPost`（表单），不要用 `doPostJson`——后端 formLogin 只认 `x-www-form-urlencoded`。
> ⚠️ **易错点 3**：`router.push` 之前必须先 `setAccessToken`。路由守卫的 `restoreSession()` 发现内存里已有令牌就直接放行，否则会拿 Cookie 多跑一次刷新接口。
> ⚠️ **易错点 4**：Postman 调用时，Cookie 管理器会自动保存 `apex_sales_refresh`，后续请求 `/api/auth/refresh` 不需要手动带它。

---

### 7.2 退出登录

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/logout`（Spring Security 的 `logoutUrl("/api/logout")` 对 GET 也生效，前端实际用 GET） |
| 鉴权 | 需登录（`Authorization` 请求头里的 Access Token） |
| 请求格式 | 无参数 |
| 实现方 | Spring Security 内置 `LogoutFilter` + `MyLogoutSuccessHandler`。`TokenVerifyFilter` 加在 `LogoutFilter` **之前**，所以要先通过认证才能执行退出 |

**响应**
```json
{ "code": 200, "msg": "退出成功" }
```

**后端做了三件事**
1. **清除 Redis 会话**——但有"`sid` 校验"：只有请求里 Access Token 的 `sid` 等于 Redis 里的 `sid` 才删。防止被顶号的旧设备退出时误删新设备刚建立的会话。
2. **清除 Refresh Cookie**——无论上一步删没删 Redis，都清（Cookie 属于本设备）。写法：同名、同 `Path=/api/auth`、`Max-Age=0`；Path 必须和写入时完全一致，浏览器按"名称 + Path"定位要删的 Cookie，写错了删不掉。
3. **清除 AI 聊天记录**（`apex_sales:ai:chat_history:{userId}`）——聊天记录与登录态同生命周期，退出即永久清除。此处不做 `sid` 校验：聊天记录绑定的是登录人本身而非设备。

**前端退出流程**（`DashboardView.vue` 的 `logout`）
```
停心跳 → GET /api/logout
├─ 200 → 清内存令牌与权限缓存 → 整页回到 /
└─ 非 200，或抛出真正的 Error（如网络中断）→ 弹「退出异常，是否强制退出？」
      ├─ 取消 → 恢复心跳
      └─ 确认 → POST /api/auth/logout（见 §7.8.1，让服务端清 HttpOnly Cookie）→ 清内存令牌 → 整页回到 /
```
若退出请求收到 9xx，拦截器已经弹了对应提示，这里不重复弹。

> 为什么强制退出必须调 `/api/auth/logout`：Refresh Cookie 是 **HttpOnly** 的，只有服务端能清。如果前端只清内存就回登录页，登录页的 `freeLogin` 会拿残留的 Cookie 刷新成功，把用户直接送回工作台，"退出"看起来完全失灵。

---

### 7.3 注册

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/register` |
| 鉴权 | **公开**（白名单） |
| 请求格式 | **JSON** `application/json` |
| 接收 | `@RequestBody UserQuery` |

**请求体**（只取这三个字段，其余忽略）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `loginAct` | String | 是 | 登录账号，为空返回 `"用户名不能为空"` |
| `loginPwd` | String | 是 | 密码明文，后端 BCrypt 加密后存储 |
| `name` | String | 是 | 姓名，为空则 Service 返回 0 → `"注册失败，请稍后重试"` |

```json
{ "loginAct": "zhangsan", "loginPwd": "abc123456", "name": "张三" }
```

**响应**
```json
{ "code": 200, "msg": "成功", "data": "注册成功，快去登录吧！" }
{ "code": 500, "msg": "该用户已存在" }
{ "code": 500, "msg": "用户名不能为空" }
{ "code": 500, "msg": "注册失败，请稍后重试" }
```

> 🔒 **关键安全设计**：注册**只创建账号，不自动绑定任何角色**。角色由管理员审核后在用户管理页分配——避免匿名用户注册即获得线索录入、客户导出等业务权限。
> 注册成功后账号初始状态：`accountNoExpired=1`、`credentialsNoExpired=1`、`accountNoLocked=1`、`accountEnabled=1`。
> 注册会主动失效负责人下拉缓存（`evictOwnerCache()`）。

---

### 7.4 忘记密码 · 获取验证码

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/password/reset/code` |
| 鉴权 | **公开**（白名单） |
| 请求格式 | URL 查询串 |

**请求参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `loginAct` | String | 是 | 登录账号 |
| `email` | String | 是 | **必须与该账号绑定的邮箱完全一致**（这是身份核验手段） |

```http
GET /api/password/reset/code?loginAct=zhangsan&email=zhangsan@example.com
```

**响应**
```json
{ "code": 200, "msg": "成功", "data": "核验验证码已发出，请注意查看（本地联调可在终端/后台直接复制）" }
{ "code": 500, "msg": "用户名和安全邮箱参数不可为空哦。" }
{ "code": 500, "msg": "账号不存在或与绑定的安全邮箱不匹配。" }
{ "code": 500, "msg": "发送验证码太频繁了，请稍候 60 秒后再试！" }
```

> ⚠️ **本地联调必读**：项目**没有接 SMTP 邮件服务**，验证码只打印在后端日志里：
> ```
> [CRM 业务防线：重置密码验证码] 账号: zhangsan | 绑定邮箱: xxx@qq.com | 本地虚拟核验码: 483920 (该验证码在5分钟内有效)
> ```
> 验证码为 **6 位纯数字**（`SecureRandom` 生成，不是可预测的 `Math.random`），Redis Key = `apex_sales:reset_pwd:code:{loginAct}`，有效期 300 秒。
> 注意 `msg` 里的提示文案写的是"请注意查看（邮箱）"，与实际"仅日志输出"不符——文案与实现不一致，见[附录 D](#附录-d已知不一致与待改进项)。

---

### 7.5 忘记密码 · 核验并重置

| 项 | 内容 |
|---|---|
| 路径 | `PUT /api/password/reset` |
| 鉴权 | **公开**（白名单） |
| 请求格式 | **JSON** |
| 接收 | `@RequestBody Map<String,String>` |
| 事务 | `@Transactional(rollbackFor = Exception.class)` |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `loginAct` | String | 是 | 登录账号 |
| `code` | String | 是 | 6 位验证码（后端比对时会 `trim()`） |
| `newPassword` | String | 是 | 新密码明文，后端 BCrypt 加密落库 |

```json
{ "loginAct": "zhangsan", "code": "483920", "newPassword": "newPass@2026" }
```

**响应**
```json
{ "code": 200, "msg": "成功", "data": "恭喜您已在 Service 事务保护下成功重设密码！" }
{ "code": 500, "msg": "必填校验项录入残漏，修改拒绝！" }
{ "code": 500, "msg": "验证码不存在或早已失效，请重新生成并获取" }
{ "code": 500, "msg": "您输入的验证码有误，还可尝试 2 次！" }
{ "code": 500, "msg": "该验证码连续验证失败次数已达 5 次上限，当前验证码已被废止！请重新获取验证码。" }
{ "code": 500, "msg": "重置动作受重组保护，暂无法找到指定要重载的账号数据对象- admin。" }
```

**防爆破逻辑**：验证码错误时用原子 `INCR` 累加 `apex_sales:reset_pwd:fail_count:{loginAct}`（与验证码同寿命，TTL 为 `-1` 时自动补上），达到 5 次即**物理抹除**验证码与计数器，必须重新申请。

**重置成功后**：清除验证码与计数器两个 Key；**销毁该账号的登录会话**（`apex_sales:user:login:{userId}`，其他设备的 Access 和 Refresh 都立刻失效）；**解除登录锁与失败计数**（用户已通过邮箱验证拿回账号，没必要再让他等 30 分钟）。

---

### 7.6 获取当前登录人信息

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/login/info` |
| 鉴权 | 需登录（无权限码要求） |
| 请求参数 | 无 |

**响应 `data`（`TUser`）关键字段**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 用户 ID |
| `loginAct` | String | 登录账号 |
| `loginPwd` | **String / null** | **已脱敏**，`getUserInfo()` 内置空，绝不下发哈希 |
| `name` | String | 姓名 |
| `phone` | String | 手机 |
| `email` | String | 邮箱 |
| `accountEnabled` | Integer | 账号启用状态：`1` 启用 / `0` 禁用 |
| `accountNoLocked` | Integer | 是否未锁定：`1` 正常 / `0` 锁定 |
| `accountNoExpired` | Integer | 账号是否未过期 |
| `credentialsNoExpired` | Integer | 凭证是否未过期 |
| `lastLoginTime` | Date | 最近登录时间（登录成功时更新，本接口只读） |
| `createTime` | Date | 创建时间 |
| `roleList` | String[] | **角色标识列表**，如 `["admin"]`、`["sales"]` |
| `permissionList` | String[] | **权限码列表**，如 `["clue:list","clue:add","user:view"]`，前端按钮显隐依据 |
| `menuPermissionList` | TPermission[] | 菜单树（`parent_id` 层级 + `icon` + 路由 `url`），前端动态渲染侧边栏 |
| `roleIds` | Integer[] | 角色 ID 列表 |
| `createByUser` | TUser | 创建人（关联对象） |
| `editByUser` | TUser | 编辑人（关联对象） |

```json
{
  "code": 200,
  "msg": "成功",
  "data": {
    "id": 1,
    "loginAct": "admin",
    "loginPwd": null,
    "name": "管理员",
    "phone": "13800000000",
    "email": "admin@dlyk.com",
    "accountEnabled": 1,
    "accountNoLocked": 1,
    "lastLoginTime": "2026-10-18 09:12:33",
    "roleList": ["admin"],
    "permissionList": ["activity:list", "activity:add", "clue:list", "..."],
    "menuPermissionList": [
      { "id": 1, "name": "市场活动", "type": "menu", "parentId": 0, "orderNo": 1, "icon": "OfficeBuilding", "subPermissionList": [ /* ... */ ] }
    ]
  }
}
```

> 🔧 **核心修复点（面试可讲）**：本接口**不再回吐令牌里的旧数据**，而是实时查库拿最新资料 + 角色/权限。否则用户改完手机号要重新登录才能看到变化。
> "最近登录时间"现在由**登录成功处理器**更新（`MyAuthenticationSuccessHandler` → `TUserMapper.updateLastLoginTimeById`，单列精准更新）。以前挂在本接口上，每打开一次页面就更新一次，记录的其实是"最近打开页面的时间"。
>
> 💡 前端把 `permissionList` / `roleList` 缓存到 `sessionStorage`（键 `apex_sales_permission_cache`），供 `v-hasPermission` 指令读取。修这个缺陷前，每个权限按钮都触发一次 `/api/login/info`，一页十几个按钮 = 十几个并发请求（N+1 请求风暴）。缓存并发时共用同一个 in-flight Promise（单飞），请求结束后释放，权限只留在 `sessionStorage` 一个来源；登录时的 `removeToken()` 会把它清掉，防止新账号沿用上一个账号的权限。

---

### 7.7 免登录探活

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/login/free` |
| 鉴权 | 需登录（无权限码） |
| 响应 | `{"code":200,"msg":"成功"}` |

用途：**前端每 5 秒一次的心跳**（`DashboardView.vue`）。它走完整的 `TokenVerifyFilter`，所以旧设备被顶下线（905）、会话被销毁（903）后，最多 5 秒就能感知并弹窗。心跳前会先检查内存里有没有令牌，没有就停掉心跳，避免令牌被清除后还每 5 秒发一次无令牌的请求、叠出一堆 901 弹框。

---

### 7.8 刷新令牌

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/auth/refresh` |
| 鉴权 | **公开**（白名单），**靠 Refresh Cookie 认证**，请求里没有 Access Token |
| 请求格式 | 无参数、无请求体；浏览器自动携带 Cookie `apex_sales_refresh`（`Path=/api/auth`） |
| 实现方 | `AuthController.refresh` |

**成功响应**：新的 Access Token 在 `data` 里，**新的 Refresh Token 在 `Set-Cookie` 里**（Refresh 每次刷新都换新，这叫"轮换"）。
```json
{ "code": 200, "msg": "成功", "data": "eyJhbGciOiJIUzI1NiJ9.eyJ0b2tlblR5cGUiOiJhY2Nlc3MiLCJzaWQiOi..." }
```
沿用原来的 `rm` 声明（记住我），所以新 Cookie 的有效期不变；`sid` 不变（仍是同一次登录），`jti` 换新。

**后端判定顺序**

| 步骤 | 条件 | 返回 | 是否清 Cookie |
|---|---|---|---|
| 1 | 没有 Cookie | `901` | 否（未登录用户打开登录页时的正常情况） |
| 2 | Refresh 签名、格式、用途（必须是 `refresh`）校验 | 过期 → `903`；篡改或类型不符 → `902` | 是 |
| 3 | Redis 里没有会话 | `903` | 是 |
| 4 | `sid` 不一致 | `905`（别处刚登录；**不删** Redis，那是新设备的会话） | 是 |
| 5 | `jti` 不一致 | `904`（**重放**：这是一张已经用过的旧 Refresh；**删除** Redis 会话） | 是 |
| 6 | 查库：用户不存在、禁用、锁定、账号过期、密码过期 | `903`（并删除 Redis 会话） | 是 |
| 7 | 轮换：用 Lua 脚本「值还是旧的才写新的」原子写入 Redis | 失败说明读取之后被别人改过，按下表处理 | — |
| 8 | 写入成功 | `200` + 新 Access + `Set-Cookie` 新 Refresh | 写新的 |

第 7 步 CAS 失败时重新读一次 Redis 再决定：没了 → `903`；`sid` 变了 → `905`（别处刚登录，不能误删人家的新会话）；`sid` 没变但 `jti` 被抢先轮换 → 删除会话，`904`。

**为什么需要原子 CAS**：原来的流程是「读会话、比对、查库、写新值」四步。两个请求带着**同一张** Refresh 几乎同时到达，都能读到旧 `jti` 并通过比对，各写一个新 `jti`，后写的覆盖先写的——要么先到的那份 Cookie 被覆盖后下次刷新误触发 904，要么攻击者和用户同时刷新时重放没被发现。Lua 脚本在 Redis 里一次执行完，期间不会插入别的命令。

```lua
if redis.call('GET', KEYS[1]) == ARGV[1] then
  redis.call('SET', KEYS[1], ARGV[2], 'EX', ARGV[3]) return 1
else return 0 end
```
> `RedisUtil` 把 value 序列化器配成 `StringRedisSerializer`，脚本参数才会按原样的字符串传给 Redis，比较才能成立。若以后改成 JSON 序列化器，字符串会多一层引号，比较永远失败，所有用户 15 分钟后都会被踢。

**前端调用时机**（`httpRequest.js`）
- **页面刷新或新开标签页**：`router.beforeEach` → `restoreSession()`，内存里没有令牌就调刷新；
- **登录页打开时**：`freeLogin()` → `restoreSession()`，成功就直接进工作台（免登录）；
- **收到 906**：响应拦截器静默刷新，成功后重放原请求。

同一页面内用 `refreshPromise` 保证同一时刻最多一个刷新请求；跨标签页用 `navigator.locks.request('apex_sales-refresh-lock', ...)` 串行，避免两个标签页拿同一张旧 Refresh 去刷新、后到的被误判为重放。刷新用没挂拦截器的独立 axios 实例（`refreshClient`，超时 10 秒），避免刷新失败时又触发刷新，形成递归。

---

### 7.8.1 靠 Cookie 退出（兜底）

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/auth/logout` |
| 鉴权 | **公开**（白名单），靠 Refresh Cookie |
| 请求格式 | 无参数、无请求体 |
| 实现方 | `AuthController.logoutByCookie` |

**响应**：恒为 `{"code":200,"msg":"成功"}`（退出是幂等操作），同时 `Set-Cookie` 清掉 Refresh Cookie。

- Cookie 有效时：校验 Refresh，`sid` 与 Redis 一致才删会话，不会误删别的设备刚建立的会话；
- Cookie 无效、过期、被篡改，或根本没有 Cookie：没有可删的会话，照样清 Cookie 并返回 200；
- CSRF：`SameSite=Strict` 已挡住跨站请求；何况被人强制退出的危害很小。

用途：Access Token 已失效，或常规 `/api/logout` 异常时的兜底，前端「强制退出」会先调它。

---

## 八、用户管理模块

### 8.1 用户分页查询

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/users` |
| 鉴权 | `user:list` |
| 请求格式 | URL 查询串（自动绑定到 `UserQuery`） |
| **数据权限** | ✅ 命中 `@DataScope(tu.id)`——非 admin 只能查到**自己这一条**记录 |

**查询参数**（除 `current` 外全部可选）

| 参数 | 类型 | 说明 |
|---|---|---|
| `current` | Integer | 页码，默认 1 |
| `loginAct` | String | 登录账号（模糊匹配） |
| `name` | String | 姓名（模糊匹配） |
| `phone` | String | 手机号 |
| `email` | String | 邮箱 |
| `accountEnabled` | Integer | 启用状态：`1` 启用 / `0` 禁用 |
| `accountNoLocked` | Integer | 锁定状态：`1` 未锁定 / `0` 锁定 |
| `roleId` | Integer | **按角色筛选**（列表筛选用，单值） |

```http
GET /api/users?current=1&name=张&accountEnabled=1&roleId=2 HTTP/1.1
Authorization: eyJhbGc...
```

**响应**：`data` 为 `PageInfo<TUser>`，结构见 §4.2，`list` 元素字段见 §7.6（`loginPwd` 已脱敏或不含）。

> ⚠️ **`roleId` 与 `roleIds` 是两个不同字段，语义完全不同**，这是本模块最容易搞混的点：
> - `roleId`（Integer 单值）→ **筛选**语义：「只看销售角色的账号」，用于 GET 列表
> - `roleIds`（List\<Integer\>）→ **提交**语义：管理员为该用户分配哪些角色，用于 POST/PUT 写入
>
> 📌 该接口的筛选能力是后加的：旧版只接收页码，前端搜索框只能过滤当前页 10 条、跨页彻底失效。

---

### 8.2 用户详情

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/user/{id}` |
| 鉴权 | `user:view` |
| 路径参数 | `id`（Integer，用户 ID） |

> ⚠️ **源码中该映射写的是 `"api/user/{id}"`（缺前导斜杠）**。Spring 会自动补全，实际访问 `/api/user/3` 可正常工作，但属于代码瑕疵。见[附录 D](#附录-d已知不一致与待改进项)。

**响应**
```json
{
  "code": 200, "msg": "成功",
  "data": {
    "id": 3, "loginAct": "zhangsan", "loginPwd": null, "name": "张三",
    "phone": "13800001111", "email": "zhangsan@example.com",
    "accountEnabled": 1, "accountNoLocked": 1,
    "roleIds": [2], "roleList": ["sales"],
    "createTime": "2026-08-01 10:00:00", "lastLoginTime": "2026-10-17 18:22:05"
  }
}
```

**安全处理**：`getUserById()` **同时被改密校验复用**（那条路径需要真实密码哈希做 BCrypt 比对），所以不能在 Service 层脱敏；但详情接口面向前端展示，Controller 里显式 `t.setLoginPwd(null)`，避免 BCrypt 哈希随用户详情下发。

用户不存在时 `data` 为 `null`。

---

### 8.3 新增用户

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/user` |
| 鉴权 | `user:add` |
| 请求格式 | **表单**（`@ModelAttribute UserQuery`）⚠️ 不是 JSON |
| 必需请求头 | `Authorization`（后端会显式用它解析"创建人"ID） |

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `loginAct` | String | 是 | 登录账号，唯一索引 |
| `loginPwd` | String | 是 | 密码明文，后端 BCrypt 加密存储 |
| `name` | String | 是 | 姓名 |
| `phone` | String | 否 | 手机号，唯一索引 |
| `email` | String | 否 | 邮箱，唯一索引 |
| `accountEnabled` | Integer | 否 | `1` 启用 / `0` 禁用 |
| `roleIds` | Integer[] | 否 | 分配的角色 ID 列表（管理员显式分配） |

**表单请求示例**（数组字段用重复 key，Spring 默认支持）
```http
POST /api/user HTTP/1.1
Authorization: eyJhbGc...
Content-Type: application/x-www-form-urlencoded

loginAct=lisi&loginPwd=abc123456&name=李四&phone=13900002222&email=lisi@x.com&accountEnabled=1&roleIds=2&roleIds=3
```

**curl 等价写法**
```bash
curl -X POST "http://localhost:8089/api/user" \
  -H "Authorization: $TOKEN" \
  -d "loginAct=lisi" -d "loginPwd=abc123456" -d "name=李四" \
  -d "roleIds=2" -d "roleIds=3"
```

**响应**
```json
{ "code": 200, "msg": "成功" }
{ "code": 500, "msg": "失败" }
{ "code": 500, "msg": "登录账号「lisi」已存在，请更换" }
{ "code": 500, "msg": "手机号「13900002222」已被其他账号占用" }
{ "code": 500, "msg": "邮箱「lisi@x.com」已被其他账号占用" }
```

**内部流程**
```
① checkUserUniqueness：账号/手机/邮箱三项前置精准查重（库层都有唯一索引，
   不预查会报误导性的「邮箱或者电话重复」）
② BCrypt 加密密码
③ 从请求头 Authorization 解析出登录人 ID → 写入 createBy
④ insertSelective 落库
⑤ assignRoles(userId, roleIds) 分配角色（先清空旧绑定再批量写入）
⑥ evictOwnerCache：失效负责人下拉缓存（Cache-Aside，写库成功即删缓存）
```

---

### 8.4 编辑用户

| 项 | 内容 |
|---|---|
| 路径 | `PUT /api/user` |
| 鉴权 | `user:edit` |
| 请求格式 | **JSON**（`@RequestBody UserQuery`）⚠️ 与新增不同 |
| 必需请求头 | `Authorization`（解析"编辑人"ID） |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `id` | Integer | 是 | 目标用户 ID |
| `loginAct`/`name`/`phone`/`email` | String | 否 | 选择性更新（`updateByPrimaryKeySelective`，null 字段不动） |
| `loginPwd` | String | 否 | **传了才改密码**（非空时 BCrypt 加密后更新） |
| `accountEnabled` | Integer | 否 | 启用/禁用账号 |
| `roleIds` | Integer[] | 否 | **不传（null）= 不动角色**；传空数组 = 清空全部角色 |

```json
{ "id": 3, "name": "张三丰", "phone": "13800001111", "accountEnabled": 0, "roleIds": [2] }
```

**响应**
```json
{ "code": 200, "msg": "成功" }
{ "code": 500, "msg": "失败" }
{ "code": 500, "msg": "登录账号「xxx」已存在，请更换" }
```

**关键机制：高风险变更即踢下线（撤权闭环）**

```
updateUser() 在更新成功之后判断：
  ├─ 填了新密码？
  ├─ 账号被设为 禁用 / 锁定 / 账号过期 / 密码过期？（字段值恰好等于 0）
  │        ↓ 任一为真
  │   redisService.delete("apex_sales:user:login:" + id)   → 该用户立即下线
  └─ assignRoles() 返回「角色集合是否真的变化」
             ↓ 若变化
        invalidateSessionIfRolesChanged() 同样删除该会话
```

判断字段是否等于 0 时用 `Integer.valueOf(0).equals(...)`：前端没传的字段是 `null`，`null` 不会被误当成 0，所以只改姓名、手机号这类普通编辑**不会**误踢人。

**历史背景**：双 Token 改造之前，`@PreAuthorize` 依据的是令牌里签发时刻的权限快照，改完角色后旧令牌仍按旧权限行事（最长可持续 7 天），所以必须踢人。**现在权限每个请求都按用户 ID 查库**，角色变更对已登录用户的下一个请求就会生效，不再有越权窗口；保留踢人是有意的保守做法——让对方重新走一次登录，界面（菜单、按钮缓存）与后端鉴权立刻保持一致。如果以后觉得打扰，可以删掉角色变更的踢人逻辑，安全性不受影响。

**禁用 / 锁定 / 改密码为什么也要踢**：Access Token 15 分钟内本来仍然有效（过滤器只查 Redis 会话和 `sid`，不查账号状态），不删会话的话，被禁用的人最长还能继续用 15 分钟。AI 工具路径（`aiUpdateUserRolesAndStatus`）本来就是立即删会话的，这里与它对齐。

**为什么要做变更检测**：前端提交时**总是**带上 `roleIds`（未改动时传原值），所以"编辑用户资料"这个动作无论是否动角色都会走到角色分配逻辑。若不检测变化，管理员仅改个名字也会把该用户踢下线——无谓打扰。

> 对比：`/api/profile`（本人改自己资料）**不涉及角色**，也不需要请求头 Authorization。

---

### 8.5 删除单个用户

| 项 | 内容 |
|---|---|
| 路径 | `DELETE /api/user/{id}` |
| 鉴权 | `user:delete` |
| 路径参数 | `id`（Integer） |

**响应**
```json
{ "code": 200, "msg": "成功" }
{ "code": 500, "msg": "失败" }
{ "code": 500, "msg": "不能删除当前登录的账号，请使用其他管理员账号操作" }
{ "code": 500, "msg": "该账号名下仍关联业务数据，无法直接删除；如需停用请改为「禁用账号」" }
```

**内部流程**（`@Transactional`，任一步失败整体回滚）
```
① tUserRoleMapper.deleteByUserId(id)  先清角色绑定，避开 t_user_role 的
   外键 ON DELETE RESTRICT
② redisService.delete("apex_sales:user:login:" + id)  清 Redis token，被删账号立即失效
③ 清登录失败计数与账号锁缓存
④ deleteByPrimaryKey(id)
   ↳ 若被业务数据外键引用（t_activity.owner_id / create_by 等均为 RESTRICT）
     → 捕获 DataAccessException → 抛 BusinessException 友好提示
⑤ evictOwnerCache()
```

**设计取舍**：CRM 语义下，负责人/创建人不应连带删掉业务数据（线索、客户、交易、活动）。所以遇到外键引用**不级联删除，而是阻止并引导改用"禁用账号"**。

**自我保护**：Controller 层先判断 `isSelf()`——删自己会同步清掉当前会话 token，导致"刚删完就被踢下线"的混乱状态。

---

### 8.6 批量删除用户

| 项 | 内容 |
|---|---|
| 路径 | `DELETE /api/user` |
| 鉴权 | `user:delete` |
| 请求格式 | URL 查询串 |

**参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `ids` | String | 是 | 逗号分隔的 ID 串，如 `2,5,9`。后端 `split(",")` 转 List |

```http
DELETE /api/user?ids=2,5,9 HTTP/1.1
Authorization: eyJhbGc...
```

**响应**
```json
{ "code": 200, "msg": "成功" }
{ "code": 500, "msg": "删除名单中包含当前登录账号，请先将其移除" }
{ "code": 500, "msg": "所选账号中有人名下仍关联业务数据，无法直接删除；如需停用请改为「禁用账号」" }
```

**成功判定**：`受影响行数 >= ids.size()` 才返回 OK——少删一条即整体判失败。事务内批量中任一账号被外键引用，**整体回滚**。

> ⚠️ `ids` 传非数字（如 `ids=abc`）时，`Integer::parseInt` 抛异常 → 全局异常处理器返回 `{"code":500,"msg":"参数「ids」格式不正确"}`。

---

### 8.7 负责人下拉列表

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/owner` |
| 鉴权 | 需登录（**无权限码要求**，多处下拉复用） |
| 请求参数 | 无 |
| 缓存 | ✅ Redis `apex_sales:user:owner`，TTL 10 分钟 |

**响应 `data`（`List<TUser>` 精简字段）**
```json
{
  "code": 200, "msg": "成功",
  "data": [
    { "id": 1, "name": "管理员" },
    { "id": 3, "name": "张三" }
  ]
}
```

**Cache-Aside（旁路缓存）模式三步**
```java
CacheUtils.getCacheData(
    () -> redisManager.getListValue("apex_sales:user:owner", TUser.class),  // ① 先查 Redis
    () -> tUserMapper.selectByOwner(),                                // ② 未命中查库
    (t) -> redisManager.setValue(key, t, 10, MINUTES)                 // ③ 回写缓存
);
```

**缓存失效时机**：用户新增 / 删除 / 批量删除 / 编辑（可能改名）/ 注册 均调 `evictOwnerCache()` 删 key。
**TTL 兜底的意义**：即使有人**绕过应用直连数据库**改数据（或漏删缓存），10 分钟后缓存自动失效并回源重建，保证**最终一致**而非永久脏数据。

> 负责人列表是用户表的**整体快照**，任何用户增删改都无法做局部更新，只能整体删除重建。

---

### 8.8 角色下拉列表

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/roles` |
| 鉴权 | `user:add` 或 `user:edit`（`hasAnyAuthority`） |
| 请求参数 | 无 |

**响应 `data`（`List<TRole>`）**
```json
{
  "code": 200, "msg": "成功",
  "data": [
    { "id": 1, "role": "admin", "roleName": "系统管理员" },
    { "id": 2, "role": "sales", "roleName": "销售专员" }
  ]
}
```

用途：用户管理页新增/编辑用户时分配角色（提交时作为 `roleIds`）。权限约束在 `user:add`/`user:edit` 是因为只有那两个场景才需要角色下拉。

---

## 九、个人资料模块

> 本模块两个接口**都无 `@PreAuthorize`**，任何登录用户均可操作**本人**资料。
> 后端强制以 `authentication.getPrincipal()` 的 ID 为准，**不接受前端传 id**，从根源杜绝横向越权改他人资料。

### 9.1 修改本人资料

| 项 | 内容 |
|---|---|
| 路径 | `PUT /api/profile` |
| 鉴权 | 需登录 |
| 请求格式 | **JSON** |

**请求体**（只有这三个字段生效，其余**全部被忽略**）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `name` | String | 否 | 姓名 |
| `phone` | String | 否 | 手机号 |
| `email` | String | 否 | 邮箱 |

```json
{ "name": "张三丰", "phone": "13800001111", "email": "zs@x.com" }
```

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

**安全设计**：Controller 里 `new TUser()` 只 setId（取自登录态）+ 三个白名单字段，**不做 `BeanUtils.copyProperties`**——即使前端恶意多传 `id`、`loginPwd`、`accountEnabled`、`roleIds`，也无法篡改身份、密码或账号状态（**字段级白名单**优于"传什么改什么"）。

---

### 9.2 修改本人密码

| 项 | 内容 |
|---|---|
| 路径 | `PUT /api/profile/password` |
| 鉴权 | 需登录 |
| 请求格式 | **JSON** |
| 接收 | `@RequestBody Map<String,String>` |

**请求体**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `oldPassword` | String | 是 | 原密码**明文**（后端用 `passwordEncoder.matches()` 比对） |
| `newPassword` | String | 是 | 新密码明文（后端 BCrypt 加密后落库） |

```json
{ "oldPassword": "abc123456", "newPassword": "newPass@2026" }
```

**响应**
```json
{ "code": 200, "msg": "成功" }
{ "code": 500, "msg": "原密码错误" }
{ "code": 500, "msg": "当前用户不存在" }
{ "code": 500, "msg": "失败" }
```
原密码错误会打 `warn` 日志（含账号名），便于审计暴力尝试。

**改密成功后会销毁该账号的登录会话**（`apex_sales:user:login:{userId}` 被删除）：其他设备上的 Access 在下一个请求、Refresh 在下一次刷新就会失效。**当前设备也会被下线**，前端提示「密码修改成功，请重新登录」后跳回登录页。理由：用户改密码往往是因为怀疑账号泄露，如果别人手里的 Refresh 还能继续刷新，改密码就起不到作用。

**为什么必须实时查库而非用令牌里的数据**（两个独立原因）：
1. **令牌里根本没有密码哈希**（白名单只放 `id`/`loginAct`/`name`）；即使以前放了，也是登录那一刻的快照，同一会话内改一次密码后哈希已变，第二次起 `matches()` 必然失败。
2. **`getUserInfo()` 会置空密码**：那个方法是给前端展示用的（脱敏），拿不到哈希。所以这里必须用**带完整字段**的 `getUserById()`。

---

## 十、线索管理模块

> 线索（Clue）是 CRM 漏斗最上游的原始商机。**转客户后线索 `state` 被置为 `-1`（已转客户），此动作不可逆。**

### 10.1 线索分页查询

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/clues` |
| 鉴权 | `clue:list` |
| 请求格式 | URL 查询串（自动绑定 `ClueQuery`） |
| 数据权限 | ❌ 未挂 `@DataScope`，普通用户可见全量线索 |

**查询参数**（除 `current` 外全部可选，可任意组合）

| 参数 | 类型 | 说明 |
|---|---|---|
| `current` | Integer | 页码，默认 1 |
| `fullName` | String | 客户姓名（模糊） |
| `phone` | String | 手机号 |
| `ownerId` | Integer | 负责人 ID（下拉取自 `/api/owner`） |
| `activityId` | Integer | 所属市场活动 ID |
| `source` | Integer | 线索来源（字典 `source` 的 **id**，见[附录 A.2](#a2-业务字典枚举)） |
| `intentionState` | Integer | 意向状态（字典 `intentionState`：`46` 有意向 / `47` 无意向 / `48` 意向不明） |
| `converted` | Boolean | 是否已转化（`true` = 已转客户） |
| `state` | Integer | 线索状态（字典 `clueState` 的 id） |

```http
GET /api/clues?current=1&fullName=张&source=16&intentionState=46 HTTP/1.1
Authorization: eyJhbGc...
```

**响应 `data.list[]` 元素结构（`TClue`）**

除本表字段外，每个对象还会带一组 **`xxxDO` 关联对象**（后端联表查出，供前端直接展示名称，免去二次查字典）：

| 关联字段 | 类型 | 内容 |
|---|---|---|
| `ownerDO` | TUser | 负责人（`id` + `name`） |
| `activityDO` | TActivity | 所属活动 |
| `appellationDO` | TDicValue | 称呼字典项（`id` + `typeValue`） |
| `needLoanDO` | TDicValue | 是否需要贷款 |
| `intentionStateDO` | TDicValue | 意向状态 |
| `sourceDO` | TDicValue | 来源 |
| `stateDO` | TDicValue | 线索状态 |
| `intentionProductDO` | TProduct | 意向产品 |

```json
{
  "id": 8, "ownerId": 3, "activityId": 2, "fullName": "张先生",
  "appellation": 18, "phone": "13800001111", "weixin": "zs_wx", "qq": "12345678",
  "email": "zs@x.com", "age": 32, "job": "工程师", "yearIncome": 250000.00,
  "address": "北京市海淀区", "needLoan": 49, "intentionState": 46,
  "intentionProduct": 1, "state": 10, "source": 16,
  "description": "对A5感兴趣", "nextContactTime": "2026-10-20 10:00:00",
  "createTime": "2026-10-01 09:12:00", "createBy": 1,
  "ownerDO": { "id": 3, "name": "张三" },
  "sourceDO": { "id": 16, "typeValue": "网络广告" },
  "stateDO":  { "id": 10, "typeValue": "试图联系" }
}
```

> 📌 该接口的筛选能力是后加的：旧版只接收页码，线索模块长期只能"翻页浏览"，前端搜索栏即使渲染出来也不生效。现由 Spring 把查询串自动绑定到 `ClueQuery`，**不传任何条件时等价于原全量分页行为**（向后兼容）。

---

### 10.2 线索详情

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/clue/detail/{id}` |
| 鉴权 | `clue:view` |

**响应**：`data` 为单个 `TClue`（结构同 §10.1 的 list 元素）。

> ⚠️ 路径是 `/api/clue/detail/{id}`，**不是** `/api/clue/{id}`——因为 `/api/clue/{value}` 已被手机号查重接口占用（见 §10.7），这是历史路由设计的妥协。

---

### 10.3 新增线索

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/clue` |
| 鉴权 | `clue:add` |
| 请求格式 | **表单**（`@ModelAttribute ClueQuery`） |
| 必需请求头 | `Authorization`（解析出 `createBy`） |

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `ownerId` | Integer | 是 | 负责人 ID |
| `activityId` | Integer | 否 | 来源活动 ID |
| `fullName` | String | 是 | 客户姓名 |
| `appellation` | Integer | 否 | 称呼（字典 `appellation` 的 id） |
| `phone` | String | 是 | 手机号，**唯一性校验** |
| `weixin` / `qq` / `email` | String | 否 | 联系方式 |
| `age` | Integer | 否 | 年龄 |
| `job` | String | 否 | 职业 |
| `yearIncome` | BigDecimal | 否 | 年收入 |
| `address` | String | 否 | 地址 |
| `needLoan` | Integer | 否 | 是否需要贷款（`49` 需要 / `50` 不需要） |
| `intentionState` | Integer | 否 | 意向状态 |
| `intentionProduct` | Integer | 否 | 意向产品 ID |
| `source` | Integer | 否 | 线索来源 |
| `description` | String | 否 | 描述 |
| `nextContactTime` | Date | 否 | 下次联系时间（`yyyy-MM-dd HH:mm:ss`） |

**响应**
```json
{ "code": 200, "msg": "成功" }
{ "code": 500, "msg": "手机号已存在" }
```

后端自动填充：`createBy`（从请求头 token 解析的登录人 ID）、`createTime`。`state` 若不传由数据库默认值决定。

---

### 10.4 编辑线索

| 项 | 内容 |
|---|---|
| 路径 | `PUT /api/clue` |
| 鉴权 | `clue:edit` |
| 请求格式 | **JSON**（`@RequestBody ClueQuery`）⚠️ 与新增不同 |
| 必需请求头 | `Authorization`（解析出 `editBy`） |

**请求体**：字段同 §10.3，**必须带 `id`**。采用 `updateByPrimaryKeySelective`，null 字段不更新。

```json
{ "id": 8, "intentionState": 46, "nextContactTime": "2026-10-25 14:00:00", "description": "已电话沟通" }
```

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

> 📌 编辑**不做手机号查重**（查重只在新增时）。若需要改手机号，前端应先调 §10.7 校验。

---

### 10.5 删除线索

| 项 | 内容 |
|---|---|
| 路径 | `DELETE /api/clue/{id}` |
| 鉴权 | `clue:delete` |
| 事务 | ✅ `@Transactional` |

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

**内部两步（同一事务，任一步失败整体回滚）**
```
① tCustomerMapper.updateClueIdToNullByClueId(id)  解除客户关联
   （t_customer.clue_id 引用 t_clue，直接删会撞外键）
② tClueMapper.deleteByPrimaryKey(id)              物理删除线索
```

> ⚠️ **是物理删除，不是逻辑删除**（尽管 `TClueRemark` 有 `deleted` 字段做逻辑删，线索主体是物理删）。
> 已转化为客户的线索被删除后，客户记录仍保留（只是 `clue_id` 变 null），**成交金额统计不受影响**。

---

### 10.6 批量删除线索

| 项 | 内容 |
|---|---|
| 路径 | `DELETE /api/clue/batch` |
| 鉴权 | `clue:delete` |
| 参数 | `ids`（String，逗号分隔，如 `1,5,9`） |

```http
DELETE /api/clue/batch?ids=1,5,9 HTTP/1.1
```

**响应**：`{"code":200,"msg":"成功"}`（成功判定：受影响行数 `== ids.size()`，少一条即失败）

逻辑与单删一致，在**一个事务内**循环执行"解关联 + 删线索"，整批原子。

---

### 10.7 手机号查重（语义反直觉，务必看清）

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/clue/{value}` |
| 鉴权 | `clue:add` 或 `clue:edit` |
| 路径参数 | `value`（String，待查重手机号） |
| 调用时机 | 新增/编辑线索表单的手机号输入框 `blur` 事件 |

**响应语义**

| 返回 | 含义 | 前端应做的事 |
|---|---|---|
| `code=200` | **手机号可用**（库中不存在） | 放行提交 |
| `code=500` | **手机号已存在**（重复） | 标红提示"该手机号已存在" |

```json
{ "code": 200, "msg": "成功" }     ← 可以使用
{ "code": 500, "msg": "失败" }     ← 已被占用
```

> 🔴 **最容易理解反的接口**：`code=500` **不是**服务器错误，而是"查重命中"的正常业务结果。
> 源码：`boolean b = clueService.checkPhone(phone); return b ? R.OK() : R.FAIL();`
> 而 `checkPhone` 的实现是 `selectByPhone(phone) > 0 ? false : true`——**存在返回 false**。
> 所以链路是：存在 → false → `R.FAIL()`。**200 = 不存在（可用），500 = 存在（重复）**。

**权限收敛原因**：旧版无任何鉴权，登录态下任意用户（包括零角色账号）都能用它遍历探测系统内是否存在某手机号——线索手机号属客户隐私，构成**枚举泄露面**。现只在新增/编辑场景需要，故收敛到 `clue:add`/`clue:edit`；`clue:view`（列表查看者）不授予。

---

### 10.8 Excel 批量导入线索

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/importExcel` |
| 鉴权 | `clue:import` |
| 请求格式 | **multipart/form-data** |
| 必需请求头 | `Authorization`（用于解析每条记录的 `createBy`） |

**表单字段**

| 字段名 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `file` | File | 是 | Excel 文件，**字段名必须是 `file`**（前端 `formData.append('file', ...)` 必须一致） |

**实现**：EasyExcel 逐行读取 → `UploadDataListener` 批量入库
```java
EasyExcel.read(inputStream, TClue.class, new UploadDataListener(tClueMapper, token))
       .sheet().doRead();
```

**响应**：`{"code":200,"msg":"成功"}`

**文件大小限制**（`application.yml`）
```yaml
spring.servlet.multipart:
  max-file-size: 5MB        # 单文件上限
  max-request-size: 10MB    # 单次请求总上限
```
超限时 Spring 抛 `MaxUploadSizeExceededException` → 全局异常处理统一返回 `{"code":500,"msg":"服务器繁忙，请稍后重试"}`。

> Excel 表头需与 `TClue` 的 `@ExcelProperty` 注解对应。导入模板可参考线索模块页面提供的下载入口。

---

### 10.9 线索跟进记录（备注）

四件套，权限分别为 `clue:view` / `clue:edit` / `clue:edit` / `clue:delete`。

| # | 方法 | 路径 | 格式 | 说明 |
|---|---|---|---|---|
| 1 | GET | `/api/clue/remark?current=1&clueId=8` | 查询串 | 分页查询某线索的跟进记录 |
| 2 | POST | `/api/clue/remark` | **表单** | 新增跟进记录 |
| 3 | PUT | `/api/clue/remark/update` | **JSON** | 编辑跟进记录 |
| 4 | DELETE | `/api/clue/remark/delete/{id}` | 路径 | 删除跟进记录 |

**`ClueRemarkQuery` 字段**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 备注 ID（编辑/删除用） |
| `clueId` | Integer | 所属线索 ID |
| `noteContent` | String | 跟进内容 |
| `noteWay` | Integer | 跟进方式（字典 `noteWay` 的 id，如 `61`=电话） |

**新增请求（表单）**
```http
POST /api/clue/remark HTTP/1.1
Authorization: eyJhbGc...
Content-Type: application/x-www-form-urlencoded

clueId=8&noteContent=电话沟通了需求，客户表示周末到店&noteWay=61
```

**编辑请求（JSON）**
```json
{ "id": 15, "clueId": 8, "noteContent": "修订：客户改到下周一到店", "noteWay": 61 }
```

**响应 `data.list[]` 元素（`TClueRemark`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 备注 ID |
| `clueId` | Integer | 线索 ID |
| `noteContent` | String | 内容 |
| `noteWay` | Integer | 方式（字典 id） |
| `noteWayName` | String | 方式名称（后端查字典回填，前端直接展示） |
| `createBy` / `createByName` | Integer / String | 创建人 ID 与姓名（后端回填） |
| `createTime` | Date | 创建时间 |
| `editBy` / `editByName` | Integer / String | 编辑人 |
| `editTime` | Date | 编辑时间 |
| `deleted` | Integer | `0` 正常 / `1` 已删除 |

**注意**：`noteWayName` 和 `createByName` 是**写入时冗余落库**的（不是查询时联表）——好处是列表查询快、且历史记录不随字典/用户改名而变（**快照语义**）；代价是字典改值后老记录仍显示旧名称。删除是**物理删**（`deleteByPrimaryKey`），`deleted` 字段在跟进记录这条链路上未被使用。

> 🔒 **权限收敛**：跟进记录含客户沟通隐私，读取收敛到 `clue:view`（与查看详情同级）。旧版无任何鉴权。

---

## 十一、客户管理模块

### 11.1 线索转客户（核心不可逆动作）

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/clue/customer` |
| 鉴权 | `clue:edit`（转化等同于对线索的高阶编辑） |
| 请求格式 | **表单**（`CustomerQuery`） |
| 必需请求头 | `Authorization`（解析出 `createBy`） |
| 事务 | ✅ |
| 实现 | `CustomerManager.convertCustomer()`（Manager 层承载跨表编排） |

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `clueId` | Integer | 是 | 要转化的线索 ID |
| `product` | Integer | 否 | 意向产品 ID |
| `description` | String | 否 | 描述 |
| `nextContactTime` | Date | 否 | 下次联系时间（`yyyy-MM-dd HH:mm:ss`） |

```http
POST /api/clue/customer HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Authorization: eyJhbGc...

clueId=8&product=1&description=已确认购买意向&nextContactTime=2026-10-25 10:00:00
```

**响应**
```json
{ "code": 200, "msg": "成功" }
{ "code": 500, "msg": "失败" }
{ "code": 500, "msg": "线索不存在，无法转化为客户。" }
{ "code": 500, "msg": "该线索已经转过客户，不能再转了." }
```

**内部三步（同一事务）**
```
① 校验线索：不存在 → BusinessException；state == -1（已转） → BusinessException
② 向 t_customer 插一条记录（BeanUtils.copyProperties 复制同名字段 + createBy/createTime）
   客户的姓名/手机/地址等信息由 clue_id 关联反查，不重复存储
③ 把 t_clue.state 置为 -1（已转客户）→ 线索从"待处理"列表中消失
```

> 🔒 **健壮性修复**：线索 ID 不存在时，旧版直接 `tClue.getState()` 会 NPE → 500 系统异常；现改为显式 `BusinessException` 业务提示。
> 🔒 **权限收敛**：这是不可逆的核心业务动作，旧版无任何鉴权，任意登录用户都能伪造转化。

---

### 11.2 客户分页查询

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/customers` |
| 鉴权 | `customer:list` |
| 请求格式 | URL 查询串（自动绑定 `CustomerQuery`） |
| 数据权限 | ❌ 未挂 `@DataScope` |

**查询参数**（全部可选）

| 参数 | 类型 | 说明 |
|---|---|---|
| `current` | Integer | 页码 |
| `fullName` | String | 客户姓名（模糊）——**注意：`fullName` 实际存在关联的线索表上** |
| `phone` | String | 手机号 |
| `ownerId` | Integer | 负责人 ID |
| `activityId` | Integer | 来源活动 ID |
| `source` | Integer | 线索来源（字典 id） |
| `intentionState` | Integer | 意向状态（字典 id） |
| `product` | Integer | 意向产品 ID |

**响应 `data.list[]` 元素（`TCustomer`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 客户 ID |
| `clueId` | Integer | 来源线索 ID |
| `product` | Integer | 意向产品 ID |
| `description` | String | 描述 |
| `nextContactTime` | Date | 下次联系时间 |
| `createTime` / `createBy` | Date / Integer | 创建信息 |
| `editTime` / `editBy` | Date / Integer | 编辑信息 |
| `clueDO` | TClue | **关联的线索全量对象**（客户的姓名/手机/地址等都在这里） |
| `ownerDO` | TUser | 负责人 |
| `activityDO` | TActivity | 来源活动 |
| `appellationDO` / `needLoanDO` / `intentionStateDO` / `stateDO` | TDicValue | 字典项 |
| `intentionProductDO` | TProduct | 意向产品 |

前端展示客户姓名的路径是 `row.clueDO.fullName`——**客户表的瘦身设计**：只存客户特有的字段（意向产品、下次联系时间），身份信息与线索共用一份，避免双写不一致。

> 📌 方法名从 `cluePage` 修正为 `customerPage`（原为历史复制粘贴遗留：客户控制器里用线索语义命名会严重误导维护者）。

---

### 11.3 客户详情

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/customer/{id}` |
| 鉴权 | `customer:view` |

**响应**：`data` 为单个 `TCustomer`（含 `clueDO` 等关联对象，结构同 §11.2）。

---

### 11.4 删除客户

| 项 | 内容 |
|---|---|
| 路径 | `DELETE /api/customer/{id}` |
| 鉴权 | `customer:delete` |

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

> ⚠️ 删客户**不影响**原线索——线索的 `state = -1` 状态不会回滚，该线索依然是"已转化"，无法再次转化。这是有意的业务语义（转化动作不可逆）。

---

### 11.5 导出客户 Excel（二进制流）

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/exportExcel` |
| 鉴权 | `customer:export` |
| **令牌通道** | 请求头 `Authorization`（和普通接口一样）。改造前走 URL 参数，已废弃 |
| 响应类型 | `application/octet-stream`（**非 JSON**） |

**参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `ids` | String | 否 | 逗号分隔客户 ID，如 `1,3,5`；**不传则导出全量** |

```http
GET /api/exportExcel?ids=1,3 HTTP/1.1
Authorization: eyJhbGc...
```

**响应头**
```http
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-disposition: attachment;filename=%E5%AE%A2%E6%88%B7%E4%BF%A1%E6%81%AF%E6%95%B0%E6%8D%AE1729238400000.xlsx
```

- 固定文件名前缀「客户信息数据」+ **当前时间戳**（`Constants.EXCEL_FILE_NAME + System.currentTimeMillis()`），避免浏览器缓存同名文件
- 文件名做了 URL 编码（中文兼容）
- 用 EasyExcel 直接写入 `response.getOutputStream()` 并 flush，**全程流式，不在内存里攒完整文件**

**导出列（`CustomerExcel`）**：与前端表格列对齐的客户基础信息（姓名、手机、来源、意向产品、状态等）。

**前端调用**（`CustomerView.vue` → `doDownload`，在 `httpRequest.js` 里）
```js
await doDownload('/api/exportExcel', ids ? { ids } : {}, '客户信息数据.xlsx')
```
`doDownload` 用 axios 的 `responseType: 'blob'` 发请求，拿到 Blob 后用 `URL.createObjectURL` 加 `<a download>` 触发保存；文件名从 `Content-Disposition` 里解析（前端与后端同源，浏览器才允许读这个响应头），解析不出就用兜底名。旧版用隐藏 iframe，节点永不移除还会泄露内存，现在没有 iframe 了。

**出错的表现**：
- 鉴权失败或令牌过期：过滤器**在方法执行前**就写回 JSON 错误体（`Content-Type: application/json`）。`blob` 模式下它也会被包成 Blob，响应拦截器会先按 `Blob.type` 判断，是 JSON 就还原成对象，906 静默刷新、905 弹窗才能照常工作。
- 权限不足：`{"code":500,"msg":"权限不足"}`，同样在拦截器里还原后由 `doDownload` 抛出，页面弹出提示。

> 该方法返回 `void`，Spring 不做任何序列化——文件下载类接口都是这个模式（对比 AI 模块的 `/api/ai/file/download`）。

---

## 十二、交易管理模块

> 交易（Tran）对应商机推进管道（Pipeline），核心是**阶段流转 + 金额**，直接影响首页统计的成交额。

### 12.0 交易阶段字典（必读，所有阶段相关接口的取值依据）

字典 `typeCode = 'stage'`，可通过 `GET /api/dicvalue/stage` 拉取。仓库种子数据如下：

| `id`（接口传这个） | `order`（推进顺序） | `typeValue`（展示名） | 是否终态 |
|---:|---:|---|---|
| `12` | 1 | 01创建交易 | 否（**新建交易的默认阶段**） |
| `37` | 3 | 02确认清单 | 否 |
| `40` | 4 | 03交付定金 | 否 |
| `35` | 5 | 04产品检验 | 否 |
| `42` | 6 | 05付款成交 | ✅ **终态** |
| `21` | 7 | 06丢失关闭 | ✅ **终态** |

**终态判定规则**（`isTerminalStage`）：取字典 `stage` 中 `order` 最大的 **2** 个值（`Constants.TRAN_TERMINAL_STAGE_DEPTH = 2`）。

> 💡 **设计亮点**：不写死 `order in (6,7)`，而是"最大值前 N 位"。因为字典由管理员在「字典数据」页可维护——若未来新增阶段（如 `order=8` 售后回访），终态语义会随字典**自动漂移**，无需改代码。业务口径调整只改常量 `TRAN_TERMINAL_STAGE_DEPTH` 即可。

> 🔴 **发现一处种子数据隐患**：`order` 序列为 `1,3,4,5,6,7`，**缺 2**。而阶段推进的相邻校验是 `|Δorder| == 1`，意味着从「01创建交易」(order=1) 推进到「02确认清单」(order=3) 会被判为"只能变更为相邻阶段"而拒绝。
> 若实测中第一步推进失败，请检查 `t_dic_value` 里是否有 `order=2` 的 stage 记录（可能在运行库中被修正过）。详见[附录 D](#附录-d已知不一致与待改进项)。

---

### 12.1 交易分页查询

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/trans` |
| 鉴权 | `tran:list` |
| 请求格式 | URL 查询串（自动绑定 `TranQuery`） |

**查询参数**（全部可选）

| 参数 | 类型 | 说明 |
|---|---|---|
| `current` | Integer | 页码，默认 1 |
| `customerId` | Integer | 按客户筛选（客户详情页复用） |
| `stage` | Integer | **按交易阶段筛选**（管道视图核心能力，见 §12.0） |
| `tranNo` | String | 流水号模糊搜索 |
| `money` | BigDecimal | 金额**下限**（语义是「不低于」，不是等于） |

```http
GET /api/trans?current=1&stage=42&money=10000 HTTP/1.1
Authorization: eyJhbGc...
```

**响应 `data.list[]` 元素（`TTran`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 交易 ID |
| `tranNo` | String | **交易流水号**，格式 `TR + yyyyMMdd + 4位序列`，如 `TR202610180007`（后端生成，前端不可传） |
| `customerId` | Integer | 客户 ID |
| `customerName` | String | 客户姓名（后端联查回填） |
| `money` | BigDecimal | 交易金额 |
| `expectedDate` | Date | 预计成交日期 |
| `stage` | Integer | 当前阶段（字典 `stage` 的 id） |
| `stageDO` | TDicValue | 阶段字典对象（`typeValue` = "05付款成交"） |
| `description` | String | 描述 |
| `nextContactTime` | Date | 下次联系时间（**AI「跟进提醒」工具就查这个字段**） |
| `createTime` / `createBy` | Date / Integer | 创建信息 |
| `createByDO` | TUser | 创建人对象 |
| `editTime` / `editBy` | Date / Integer | 编辑信息 |

> 📌 **功能补强说明**：旧版只允许传 `customerId` 和 `money`，且 `money` 是**等值匹配**（几乎无法命中，等于废功能）；`stage` 完全不能传，导致销售最关心的"按阶段看管道"做不到。现新增 `stage` / `tranNo`，并把 `money` 语义从「等于」修正为「不低于」（与市场活动模块 `cost >= budget` 口径对齐）。

---

### 12.2 交易详情

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/tran/{id}` |
| 鉴权 | `tran:view` |

**响应**：`data` 为单个 `TTran`（结构同 §12.1）。

---

### 12.3 新增交易

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/trans` |
| 鉴权 | `tran:add` |
| 请求格式 | **表单**（`TranQuery`） |
| 必需请求头 | `Authorization` |
| 事务 | ✅（插交易 + 插首条阶段历史，两步同生同死） |

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `customerId` | Integer | 是 | 客户 ID（下拉取自 `/api/customer/options`） |
| `money` | BigDecimal | 是 | 交易金额 |
| `expectedDate` | Date | 否 | 预计成交日期（`yyyy-MM-dd HH:mm:ss`） |
| `nextContactTime` | Date | 否 | 下次联系时间 |
| `description` | String | 否 | 描述 |
| ~~`tranNo`~~ | — | — | **传了也没用**，后端强制生成 |
| ~~`stage`~~ | — | — | **传了也没用**，后端强制设为 `12`（01创建交易） |

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

**内部流程**
```
① 生成流水号 tranNo = "TR" + yyyyMMdd + 4位当日序列（Redis INCR 原子自增）
② 强制 stage = 12（"01创建交易"），忽略前端传值
③ createBy = 从请求头 token 解析的登录人 ID；createTime = now
④ insertSelective(t_tran)
⑤ 写入首条阶段历史 t_tran_history（stage/money/expectedDate/createBy/createTime）
```

**🔑 流水号并发设计（这是本项目质量很高的一处，面试重点）**

| 层次 | 实现 |
|---|---|
| **旧版缺陷** | `SELECT MAX(tran_no) 再 +1`，典型 read-then-write **竞态**：两个并发请求读到同一 MAX 值 → 生成重复流水号 |
| **修复方案** | Redis `INCR` 原子自增——单命令完成"读取并递增"，多实例/多线程下天然不重号 |
| **冷启动对齐** | Key 可能因过期/重启而缺失，直接 INCR 会从 1 开始与库中已有号冲突。故 key 不存在时，**先用 DB 当日最大序列号回填 Redis**（`SETNX + EX`，并发回填互不覆盖），再自增 |
| **降级兜底** | Redis 整体不可用时回退到 DB `MAX+1` 方式并记 `warn` 日志——"可用性优先"：交易创建这一核心写操作不能因缓存故障而不可用 |

Redis Key：`Constants.REDIS_TRAN_NO_SEQ_KEY + datePrefix`（如 `...TR20261018`），TTL 数小时。

---

### 12.4 编辑交易

| 项 | 内容 |
|---|---|
| 路径 | `PUT /api/trans` |
| 鉴权 | `tran:edit` |
| 请求格式 | **JSON**（`@RequestBody TranQuery`） |
| 必需请求头 | `Authorization` |

**请求体**：字段同 §12.3，**必须带 `id`**。

```json
{ "id": 15, "money": 268000.00, "expectedDate": "2026-11-05 00:00:00", "description": "客户改为分期" }
```

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

> ⚠️ 这个接口能改 `stage` 但**不做相邻性校验、不写阶段历史**——正规改阶段请走 §12.5。用 `PUT /api/trans` 改阶段会绕过全部业务规则并导致历史断档。

---

### 12.5 交易阶段推进/回退（业务核心）⭐

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/tran/stage` |
| 鉴权 | `tran:edit` |
| 请求格式 | **表单**（全部 `@RequestParam`） |
| 必需请求头 | `Authorization` |
| 事务 | ✅（改阶段 + 记历史，两步原子） |
| 返回类型 | `void` 语义（Service 层无返回值，靠抛 `BusinessException` 表达失败） |

**表单字段**

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `tranId` | Integer | 是 | 交易 ID |
| `stage` | Integer | 是 | 目标阶段（字典 id，见 §12.0） |
| `money` | BigDecimal | 否 | 同步更新金额；**不传则保留原值** |
| `expectedDate` | Date | 否 | 同步更新预计成交日期，**格式必须 `yyyy-MM-dd HH:mm:ss`** |

```http
POST /api/tran/stage HTTP/1.1
Content-Type: application/x-www-form-urlencoded
Authorization: eyJhbGc...

tranId=15&stage=40&money=268000.00&expectedDate=2026-11-05 00:00:00
```

**响应**
```json
{ "code": 200, "msg": "成功" }
{ "code": 500, "msg": "交易不存在" }
{ "code": 500, "msg": "阶段数据异常" }
{ "code": 500, "msg": "只能变更为相邻阶段" }
{ "code": 500, "msg": "交易已处于终态「05付款成交」，不能再向前推进；如需纠错请使用回退" }
```

**四条校验规则（按执行顺序）**

```
① 交易存在性：selectByPrimaryKey(tranId) == null → BusinessException("交易不存在")
        ↓
② 字典有效性：当前阶段 或 目标阶段 在字典中查不到 → BusinessException("阶段数据异常")
        ↓
③ 相邻性（双向）：|当前order - 目标order| != 1 → BusinessException("只能变更为相邻阶段")
   ↳ 这一条同时保证了"不能跨级跳跃"，所以向前推进只可能是 order+1，无需另拦
        ↓
④ 终态禁止向前推进：
   forward = 目标order > 当前order
   若 forward 且 当前已是终态 → BusinessException("交易已处于终态「xxx」…")
```

**终态规则的三种情形解读**（源码注释原文）

| 情形 | 行为 | 理由 |
|---|---|---|
| ① 当前**非**终态，向前推进 | ✅ 放行（含"推进至成交"这个正常终局动作） | 正常业务流 |
| ② 当前**已**终态，方向向前 | ❌ 拒绝 | 「05付款成交」和「06丢失关闭」是**互斥的终局结果**，已成交后再标丢失属业务错误，会影响成交金额统计 |
| ③ 方向回退 | ✅ 放行 | 纠错 / 重开丢失单是正常的，由规则③（相邻校验）承接即可 |

**成功后**：写入 `t_tran_history`（新阶段的 stage/money/expectedDate/createBy/createTime），供 §12.6 审计查询。

> 🔧 **实测修复过的 bug（很有故事性）**：前端 `el-date-picker` 配了 `value-format="YYYY-MM-DD HH:mm:ss"`，传的是字符串。后端旧版 `@RequestParam Date expectedDate` **没加 `@DateTimeFormat`**，Spring 无法绑定 → 抛 `MethodArgumentTypeMismatchException` → 500（已实测复现）。修复方式是加注解并与前端 `value-format` 严格对齐。
> **教训**：表单接口传日期，前后端格式约定必须显式写在两端，否则一定错。

> 🔒 **权限收敛**：阶段推进直接影响成交金额统计，旧版任意登录用户可篡改任意交易阶段，现收敛到 `tran:edit`。同时把业务失败从裸 `RuntimeException` 改为 `BusinessException` 通道（前者会被全局兜底脱敏成「服务器繁忙」，用户看不到真实原因）。

---

### 12.6 阶段变更历史

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/tran/{tranId}/history` |
| 鉴权 | `tran:view` |
| 返回 | `List<TTranHistory>`（**不分页**，按时间序列全量） |

**响应 `data[]` 元素**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 历史记录 ID |
| `tranId` | Integer | 交易 ID |
| `stage` | Integer | 该次变更后的阶段（字典 id） |
| `money` | BigDecimal | 该时刻的金额快照 |
| `expectedDate` | Date | 该时刻的预计成交日期 |
| `createTime` | Date | 变更时间 |
| `createBy` | Integer | 操作人 ID |

```json
{
  "code": 200, "msg": "成功",
  "data": [
    { "id": 1, "tranId": 15, "stage": 12, "money": 200000.00, "createTime": "2026-10-01 10:00:00", "createBy": 3 },
    { "id": 2, "tranId": 15, "stage": 40, "money": 268000.00, "createTime": "2026-10-18 15:20:11", "createBy": 3 }
  ]
}
```

用途：交易详情页的时间轴组件，展示"谁在什么时候把单子推到了哪个阶段"。**只追加不修改**（append-only 审计日志）。

---

### 12.7 删除交易

| 项 | 内容 |
|---|---|
| 路径 | `DELETE /api/tran/{id}` |
| 鉴权 | `tran:delete` |

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

---

### 12.8 批量删除交易

| 项 | 内容 |
|---|---|
| 路径 | `DELETE /api/tran/batch` |
| 鉴权 | `tran:delete` |
| 参数 | `ids`（String，逗号分隔） |

**响应**：成功判定为 `受影响行数 == ids.size()`。

> ⚠️ `ids` 传非数字会经 `Integer::parseInt` 抛异常 → 全局处理器返回 `{"code":500,"msg":"参数「ids」格式不正确"}`（旧版会把 Spring 的整段英文校验栈 + 内部类名回吐前端，已在异常处理层收敛）。

---

### 12.9 某客户下的交易分页

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/customer/{customerId}/trans` |
| 鉴权 | `tran:list` |
| 路径参数 | `customerId`（Integer） |
| 查询参数 | `current`（Integer，可选，默认 1） |

**响应**：`PageInfo<TTran>`（结构见 §4.2 + §12.1）。用途：客户详情页的"关联交易"标签页。

---

### 12.10 客户下拉选项

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/customer/options` |
| 鉴权 | `tran:add` 或 `tran:edit` |
| 请求参数 | 无 |
| 返回 | `List<TCustomer>`（含 `clueDO`，前端取 `clueDO.fullName` 作显示名） |

> 🔒 **权限收敛原因**：下拉数据含全量客户姓名，只有新建/编辑交易页面需要，故收敛到 `tran:add`/`tran:edit`。

---

### 12.11 交易跟进记录（备注）四件套

| # | 方法 | 路径 | 鉴权 | 格式 |
|---|---|---|---|---|
| 1 | GET | `/api/tran/remark` | `tran:view` | 查询串 `current` + `tranId`（**tranId 必填**） |
| 2 | POST | `/api/tran/remark` | `tran:edit` | **表单** `TranRemarkQuery` |
| 3 | PUT | `/api/tran/remark` | `tran:edit` | **JSON** `TranRemarkQuery` |
| 4 | DELETE | `/api/tran/remark/{id}` | `tran:delete` | 路径参数 |

**`TranRemarkQuery` 字段**：`id`、`tranId`、`noteContent`、`noteWay`（字典 `noteWay` 的 id）

**响应 `data.list[]` 元素（`TTranRemark`）**：字段结构与 `TClueRemark` 完全同构（见 §10.9），仅把 `clueId` 换成 `tranId`：
`id` / `tranId` / `noteWay` / `noteWayName`（写入时冗余）/ `noteContent` / `createBy` / `createByName`（写入时冗余）/ `createTime` / `editBy` / `editByName` / `editTime` / `deleted`

**新增示例（表单）**
```
tranId=15&noteContent=客户已确认配置，等财务走款&noteWay=61
```

> ⚠️ **路由不对称**：编辑走 `PUT /api/tran/remark`（ID 在 body 里），而线索备注编辑走 `PUT /api/clue/remark/update`（多了 `/update` 后缀）。两个模块的 URL 风格不统一，调用时别照抄另一模块的路径。
> 📌 **AI 关联**：AI 工具「交易跟进提醒」查的就是 `t_tran.next_contact_time` 临近且未成交的记录——跟进记录本身不参与该查询。

---

## 十三、市场活动模块

> ⚠️ **本模块有一个必须注意的字段名映射**：请求/响应中叫 **`budget`（预算）**，但数据库列和实体类 `TActivity` 里叫 **`cost`**。
> - 写操作（POST/PUT）：传 `budget`，Service 层显式 `activity.setCost(query.getBudget())` 完成映射
> - 读操作（GET）：**响应里返回的是 `cost`**，前端取 `row.cost` 不是 `row.budget`
> - 列表筛选 `budget` 的语义是「实际花费 **不低于** 该预算」（`ta.cost >= #{budget}`）

### 13.1 活动分页查询

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/activities` |
| 鉴权 | `activity:list` |
| **数据权限** | ✅ 命中 `@DataScope(ta.owner_id)`——非 admin 只能看到**自己负责的活动** |

**查询参数**（全部可选）

| 参数 | 类型 | 说明 |
|---|---|---|
| `current` | Integer | 页码 |
| `ownerId` | Integer | 负责人 ID |
| `name` | String | 活动名称（模糊） |
| `startTime` | Date | 区间起点，格式 `yyyy-MM-dd HH:mm:ss` |
| `endTime` | Date | 区间终点 |
| `budget` | BigDecimal | 花费下限 |

**🔑 时间筛选是「区间重叠」语义，不是「包含于」语义**（`TActivityMapper.xml:63-71`）：
```sql
and ta.end_time   >= #{startTime}    -- 活动结束不早于查询起点
and ta.start_time <= #{endTime}      -- 活动开始不晚于查询终点
```
旧版写的是 `start_time >= S AND end_time <= E`，要求活动**整段落在**查询区间内——查"2026 年 10 月有哪些活动"时，一个跨月活动（9/20 开始、10/5 结束）会被漏掉。现修正为重叠判定，符合业务直觉。

**响应 `data.list[]` 元素（`TActivity`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 活动 ID |
| `ownerId` | Integer | 负责人 ID |
| `ownerDo` | TUser | 负责人对象（⚠️ JSON 键名是 **`ownerDo`**，小写 o——其他模块是 `ownerDO`，前端取值别照抄） |
| `name` | String | 活动名称 |
| `startTime` / `endTime` | Date | 起止时间 |
| `cost` | BigDecimal | **实际花费**（⚠️ 不是 `budget`） |
| `description` | String | 描述 |
| `createTime` / `createBy` / `editTime` / `editBy` | — | 审计字段 |

> 📌 `description` 字段是**修复过的真实缺陷**：原 `ActivityQuery` 缺这个字段，前端填写的描述在参数绑定阶段就被丢弃，再叠加 Service 层未 `setDescription`，导致活动描述**无论如何都存不进库**（实测新建后恒为空）。补齐字段并贯通持久化链路。

---

### 13.2 新增活动

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/activities` |
| 鉴权 | `activity:add` |
| 请求格式 | **表单**（`ActivityQuery`） |

**表单字段**：`ownerId`（是）、`name`（是）、`startTime`、`endTime`、`budget`、`description`

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

**三条业务校验（`BusinessException`，文案直达前端）**
| 校验 | 失败文案 |
|---|---|
| 名称非空 | `活动名称不能为空` |
| `endTime >= startTime` | `活动结束时间不能早于开始时间` |
| `budget >= 0`（`signum() < 0` 判定） | `活动预算不能为负数` |

日期字段用 `@DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")` 标注，与前端 `el-date-picker` 的 `value-format` 对齐。

---

### 13.3 编辑活动

| 项 | 内容 |
|---|---|
| 路径 | `PUT /api/activities/{id}` |
| 鉴权 | `activity:edit` |
| 请求格式 | **JSON**（`@RequestBody ActivityQuery`） |
| 路径参数 | `id`（会覆盖 body 里的 id） |

**请求体**（⚠️ 金额字段仍叫 **`budget`**，因为 `@RequestBody` 接的是 `ActivityQuery`，Service 层再做 `budget → cost` 映射）

```json
{ "ownerId": 3, "name": "国庆车展", "startTime": "2026-10-01 09:00:00", "endTime": "2026-10-07 18:00:00", "budget": 50000, "description": "线下展会" }
```

路径 `{id}` 会覆盖 body 里的 `id`（`activityQuery.setId(id)`），所以 body 里**不需要传 id**。

> 🔁 **本模块的字段名不对称，再强调一次**：
> - **请求**（POST 表单 / PUT JSON）→ 写 `budget`
> - **响应**（GET 返回的 `TActivity`）→ 读 `cost`
> - **列表筛选**参数 → 传 `budget`（语义是 cost 下限）

**响应**：`{"code":200,"msg":"成功"}`；校验失败同上三条文案。

---

### 13.4 删除活动

| 项 | 内容 |
|---|---|
| 路径 | `DELETE /api/activities/{id}` |
| 鉴权 | `activity:delete` |

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`（`i == 1` 才算成功）

---

### 13.5 批量删除活动

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/activities/batch-delete` |
| 鉴权 | `activity:delete` |
| 请求格式 | **JSON** |
| 接收 | 私有静态内部类 `BatchIdsRequest { List<Integer> ids }` |

> ⚠️ 用 `POST` 而非 `DELETE`。原因：`DELETE` 带请求体在部分网关/代理实现下会被丢弃，业界普遍用 POST 承载批量删除。

```json
{ "ids": [1, 5, 9] }
```

**响应**
```json
{ "code": 200, "msg": "成功", "data": "已删除 3 个活动" }
{ "code": 500, "msg": "请选择要删除的活动" }
```
注意成功时 `data` 是**人类可读的字符串**（含实际删除数量），不是布尔或对象。

**内部**：Service 层在**单事务内级联清理备注后**批量删除活动（避免孤儿备注），任一失败整体回滚。

> 🔧 **修复的既有缺陷**：前端「批量删除」按钮一直在调这个端点，但后端**从未实现**，请求落到全局异常处理返回 500（实测已复现）。现补齐。

---

### 13.6 活动备注（跟进记录）

| # | 方法 | 路径 | 鉴权 | 格式 | 说明 |
|---|---|---|---|---|---|
| 1 | GET | `/api/activities/{activityId}/remarks` | `activity:view` | 路径 | 列出某活动全部未删除备注（**不分页**，`List` 直返） |
| 2 | POST | `/api/activities/{activityId}/remarks` | `activity:edit` | **表单** | 新增备注，body 直接绑定 `TActivityRemark` 实体 |
| 3 | PUT | `/api/activities/remarks/{remarkId}` | `activity:edit` | **JSON** | 编辑备注 |
| 4 | DELETE | `/api/activities/remarks/{remarkId}` | `activity:delete` | **JSON**（body 可空） | **逻辑删除**（`deleted=1`） |

> ⚠️ 源码中 1、2 两个映射写的是 `"api/activities/..."`（**缺前导斜杠**），Spring 会自动补全，实际可正常访问。见[附录 D](#附录-d已知不一致与待改进项)。

**1）列出备注** —— 响应 `data[]`（`TActivityRemark`）

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 备注 ID |
| `activityId` | Integer | 活动 ID |
| `noteContent` | String | 内容 |
| `createBy` / `createByName` | Integer / String | 创建人 |
| `createTime` | Date | 创建时间 |
| `editBy` / `editTime` | Integer / Date | 编辑信息 |
| `deleted` | Integer | `0` 正常 / `1` 已删除（列表接口只返回 `0`） |

**2）新增备注**（`activityId` / `createBy` / `createTime` / `deleted` 全部由**后端强制填充**，前端传了也会被覆盖）
```http
POST /api/activities/5/remarks HTTP/1.1
Content-Type: application/x-www-form-urlencoded

noteContent=展会现场收集名片 120 张
```
`createBy` 从 `SecurityContextHolder` 取当前登录人 ID（不是从请求头解析 token，这是本 Controller 的独有写法）。

**3）编辑备注**
```json
{ "noteContent": "修订：现场收集名片 135 张" }
```
后端只取 `noteContent`（**字段级白名单**，即使多传 `createBy`/`deleted` 也无法篡改归属或状态），自动填 `editBy` / `editTime`。

**4）删除备注**（逻辑删除）

---

### 13.7 备注归属校验规则（三个写操作共用）

| 当前用户 | 能否编辑/删除他人创建的备注 | 依据 |
|---|---|---|
| **管理员**（`admin` 角色） | ✅ 可以操作**任意**备注 | 管理职责 |
| 普通用户 | ❌ 只能操作**自己创建**的备注 | `existing.getCreateBy().equals(currentUser.getId())` |

失败文案：`{"code":500,"msg":"只能修改自己创建的备注"}` / `"只能删除自己创建的备注"`
备注不存在或已删除：`{"code":500,"msg":"备注不存在"}`
未取到登录态：`{"code":500,"msg":"未登录"}`

> 🔧 **修复的逻辑倒置**：原版对管理员返回「管理员不能修改备注」，而删除接口却**允许**管理员删任意备注——两者自相矛盾且违反管理职责。现统一为：管理员可编辑任意备注。

---

## 十四、产品管理模块

### 14.1 产品分页查询

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/products` |
| 鉴权 | `product:list` |

**查询参数**（全部可选）

| 参数 | 类型 | 说明 |
|---|---|---|
| `current` | Integer | 页码 |
| `name` | String | 产品名称（模糊） |
| `guidePriceS` | BigDecimal | 指导价**区间下限** |
| `guidePriceE` | BigDecimal | 指导价**区间上限** |
| `quotation` | BigDecimal | 经销商报价 |
| `state` | Integer | 上下架状态（`1` 在售 / `0` 下架，字典 `product` 无关，是独立字段） |

```http
GET /api/products?current=1&guidePriceS=200000&guidePriceE=400000&state=1
```

**响应 `data.list[]` 元素（`TProduct`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 产品 ID |
| `name` | String | 产品名称 |
| `guidePriceS` | BigDecimal | 指导价起 |
| `guidePriceE` | BigDecimal | 指导价止 |
| `quotation` | BigDecimal | **经销商报价**（AI「产品行情咨询」工具返回的就是它） |
| `state` | Integer | 状态 |
| `createTime` / `createBy` / `editTime` / `editBy` | — | 审计字段 |

---

### 14.2 新增 / 编辑 / 删除产品

| 项 | 新增 | 编辑 | 删除 |
|---|---|---|---|
| 方法+路径 | `POST /api/products` | `PUT /api/products/{id}` | `DELETE /api/products/{id}` |
| 鉴权 | `product:add` | `product:edit` | `product:delete` |
| 格式 | **表单** | **JSON**（body 里的 id 会被路径 id 覆盖） | 路径参数 |

**表单/JSON 字段**：`name`、`guidePriceS`、`guidePriceE`、`quotation`、`state`

```json
{ "name": "奥迪 A5", "guidePriceS": 298000, "guidePriceE": 428000, "quotation": 276000, "state": 1 }
```

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

---

### 14.3 批量删除产品

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/products/batch-delete` |
| 鉴权 | `product:delete` |
| 请求格式 | **JSON**（与活动批量删除**同款结构**，保持前端调用心智一致） |

```json
{ "ids": [2, 4] }
```

**响应**
```json
{ "code": 200, "msg": "成功", "data": "已删除 2 个产品" }
{ "code": 500, "msg": "请选择要删除的产品" }
{ "code": 500, "msg": "产品「奥迪 A5」正被客户意向引用，无法删除" }
```

**事务语义**：**任一产品被客户意向引用**（`t_clue.intention_product` 外键引用）时，Service 抛 `BusinessException`，**整体回滚**（不是跳过该项继续），由全局异常处理器透传友好文案。

---

## 十五、字典管理模块

### 15.1 ⭐ 字典值下拉（本地内存缓存，全项目唯一）

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/dicvalue/{typeCode}` |
| 鉴权 | 需登录（**无权限码**） |
| 数据源 | ⚡ **`ServerApplication.cacheMap`（JVM 本地 Map，不查库、不查 Redis）** |

**路径参数 `typeCode` 可选值（`DicEnum`）**

| typeCode | 数据来源 | 返回类型 |
|---|---|---|
| `appellation` | `t_dic_value` | `List<TDicValue>` |
| `source` | `t_dic_value` | `List<TDicValue>` |
| `clueState` | `t_dic_value` | `List<TDicValue>` |
| `intentionState` | `t_dic_value` | `List<TDicValue>` |
| `needLoan` | `t_dic_value` | `List<TDicValue>` |
| `noteWay` | `t_dic_value` | `List<TDicValue>` |
| `stage` | `t_dic_value` | `List<TDicValue>` |
| **`product`** | ⚠️ **`t_product` 表**（特殊分支） | `List<TProduct>` |
| **`activity`** | ⚠️ **`t_activity` 表**（特殊分支） | `List<TActivity>` |

```json
// GET /api/dicvalue/stage
{
  "code": 200, "msg": "成功",
  "data": [
    { "id": 12, "typeCode": "stage", "typeValue": "01创建交易", "order": 1, "remark": null },
    { "id": 42, "typeCode": "stage", "typeValue": "05付款成交", "order": 6, "remark": null }
  ]
}
```

> ⚠️ **两级特殊性**：
> 1. `product` / `activity` 两个 typeCode **不查字典表**，代码里显式 `instanceof` 分支后强转为对应实体列表——前端下拉能直接用产品名/活动名，省一次接口调用；
> 2. 数据来自**启动时加载的 JVM 本地 Map**，所以：**在字典管理页改了值，本接口不会立即生效**（要么重启，要么依赖定时任务刷新，见 `DataTask`）。这是本模块最容易踩的坑。
>
> 若传了未知 typeCode，`cacheMap.get()` 返回 null → `data` 为 `null`（不报错）。

---

### 15.2 字典类型（增删改查）

| # | 方法 | 路径 | 鉴权 | 格式 |
|---|---|---|---|---|
| 1 | GET | `/api/dictypes` | `dictype:list` | 查询串 `current` + `DicTypeQuery` |
| 2 | GET | `/api/dictypes/all` | `dictype:list` **或** `dicvalue:list` | 无参数，**不分页** |
| 3 | POST | `/api/dictypes` | `dictype:add` | **表单** |
| 4 | PUT | `/api/dictypes/{id}` | `dictype:edit` | **JSON**（路径 id 覆盖 body id） |
| 5 | DELETE | `/api/dictypes/{id}` | `dictype:delete` | 路径参数 |

**`DicTypeQuery` 字段**：`id`、`typeCode`、`typeName`、`remark`

**响应 `TDicType`**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 类型 ID |
| `typeCode` | String | 类型编码（如 `stage`），**全项目下拉数据的索引键** |
| `typeName` | String | 类型中文名（如 `交易阶段`） |
| `remark` | String | 备注 |
| `dicValueList` | `List<TDicValue>` | 该类型下的字典值（`/all` 接口返回，分页接口视 SQL 而定） |
| `valueCount` | Integer | **该类型下的字典值数量**（`/all` 接口专用，目录总览展示） |

> 📌 `/api/dictypes/all` 权限特意放宽为 `hasAnyAuthority('dictype:list','dicvalue:list')`：字典类型页和字典数据页分属**不同权限组**，只授予字典数据权限的用户也需要在维护页的类型下拉中看到类型列表。这个"或"关系是刻意设计，不是漏配。

---

### 15.3 字典值（增删改查）

| # | 方法 | 路径 | 鉴权 | 格式 |
|---|---|---|---|---|
| 1 | GET | `/api/dicvalues` | `dicvalue:list` | 查询串 `current` + `DicValueQuery` |
| 2 | POST | `/api/dicvalues` | `dicvalue:add` | **表单** |
| 3 | PUT | `/api/dicvalues/{id}` | `dicvalue:edit` | **JSON** |
| 4 | DELETE | `/api/dicvalues/{id}` | `dicvalue:delete` | 路径参数 |

**`DicValueQuery` 字段**

| 字段 | 类型 | 说明 |
|---|---|---|
| `typeCode` | String | 所属字典类型编码（列表筛选 / 新增时指定） |
| `typeValue` | String | 字典值文本（如 `05付款成交`） |
| `order` | Integer | **排序号——交易阶段推进的相邻性校验完全依赖它** |
| `remark` | String | 备注 |

> 🔴 **修改 `stage` 字典的 `order` 会直接影响交易阶段流转**（相邻校验 `|Δorder| == 1`）。改动前务必确认序列连续，否则会阻断正常推进。`order` 断号的隐患见[附录 D](#附录-d已知不一致与待改进项)。

---

## 十六、统计分析模块

> 四个接口**均无 `@PreAuthorize`**，仅需登录。为首页仪表盘（`DashboardView.vue` / `StatisticView.vue`）提供数据，返回结构直接对齐 ECharts 配置项，前端拿到即可用。

### 16.1 概览汇总指标

`GET /api/summary/data`

**响应 `data`（`SummaryData`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `effectiveActivityCount` | Integer | **有效**市场活动数（`start_time <= now() <= end_time`） |
| `totalActivityCount` | Integer | 市场活动总数 |
| `totalClueCount` | Integer | 线索总数 |
| `totalCustomerCount` | Integer | 客户总数 |
| `successTranAmount` | BigDecimal | **成交**交易额（stage = 05付款成交） |
| `totalTranAmount` | BigDecimal | 总交易额（含未成交） |

```json
{
  "code": 200, "msg": "成功",
  "data": {
    "effectiveActivityCount": 3, "totalActivityCount": 12,
    "totalClueCount": 87, "totalCustomerCount": 24,
    "successTranAmount": 1268000.00, "totalTranAmount": 5430000.00
  }
}
```

### 16.2 销售漏斗

`GET /api/saleFunnel/data` → 返回 `List<NameValue>`，直接喂给 ECharts funnel。

```json
{ "code": 200, "msg": "成功",
  "data": [
    { "value": 20,  "name": "成交" },
    { "value": 60,  "name": "交易" },
    { "value": 80,  "name": "客户" },
    { "value": 100, "name": "线索" }
  ] }
```

体现 CRM 漏斗转化：线索 → 客户 → 交易 → 成交，数值递减。
> 📌 该漏斗也是 AI 付费能力「经营深度洞察报告」的核心数据源之一（`generateGlobalDeepInsight` 工具汇总漏斗/来源/订单三块）。

### 16.3 来源分布

`GET /api/sourcePie/data` → 返回 `List<NameValue>`（ECharts pie 格式）

```json
{ "code": 200, "msg": "成功",
  "data": [ { "value": 18, "name": "网络广告" }, { "value": 11, "name": "门店参观" } ] }
```

### 16.4 月度趋势

`GET /api/trend/data` → 返回 `TrendData`（**四个平行数组，按下标对齐**，是 ECharts 多轴图的标准喂法）

| 字段 | 类型 | 说明 |
|---|---|---|
| `monthList` | String[] | 月份标签，如 `["2026-06","2026-07",...]` |
| `clueNumList` | Integer[] | 每月新增线索数 |
| `customerNumList` | Integer[] | 每月新增客户数 |
| `tranAmountList` | BigDecimal[] | 每月交易额 |

```json
{ "code": 200, "msg": "成功",
  "data": {
    "monthList": ["2026-08","2026-09","2026-10"],
    "clueNumList": [12, 18, 25],
    "customerNumList": [4, 7, 9],
    "tranAmountList": [320000.00, 560000.00, 890000.00]
  } }
```
> AI 付费能力「交易趋势预测」就是基于 `tranAmountList` 做环比推演。

---

## 十七、系统设置模块

### 17.1 获取系统信息（完整）

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/system/info` |
| 鉴权 | `system:view` |

**响应 `data`（`TSystemInfo`）全字段**

| 字段 | 说明 |
|---|---|
| `id` | 主键（系统只有一行配置） |
| `systemCode` | **系统标识/主题编码**（前端据此加载 `module-theme.css` 主题变量） |
| `name` | 系统名称（如「智路 CRM」） |
| `site` | 站点地址 |
| `logo` | Logo 图片地址 |
| `title` | 浏览器标签页标题 |
| `description` | 描述（meta description） |
| `keywords` | SEO 关键词 |
| `shortcuticon` | 页签图标 |
| `tel` / `weixin` / `email` / `address` | 联系方式 |
| `version` | 版本号（如 `v0.1.2`） |
| `closeMsg` | **系统关闭时展示的公告文案** |
| `isopen` | **开关状态**：`"true"` 开放 / `"false"` 关闭维护（字符串，不是布尔！） |
| `createTime` / `createBy` | 审计字段 |

### 17.2 获取公开系统信息（免登录）

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/system/info/public` |
| 鉴权 | **公开**（Security 白名单 + Filter 白名单） |
| 用途 | 登录页也要有正确的 Logo / 标题 / 主题 / 维护公告 |

**脱敏策略**：新建一个空 `TSystemInfo`，**只复制白名单字段**：
`systemCode`、`name`、`version`、`title`、`description`、`logo`、`shortcuticon`、`isopen`、`closeMsg`
—— `site` / `keywords` / `tel` / `weixin` / `email` / `address` **不下发**（避免公开面泄露企业联系方式）。

**兜底**：库里没有配置行时返回硬编码默认值（`name="智路 CRM"`, `version="v0.1.2"`, `description="设计精美，操作便捷"`, `isopen="true"`），防空指针导致登录页白屏。

```json
{ "code": 200, "msg": "成功",
  "data": { "systemCode": "default", "name": "智路 CRM", "version": "v0.1.2",
            "title": "智路CRM", "description": "设计精美，操作便捷", "logo": null,
            "shortcuticon": null, "isopen": "true", "closeMsg": null } }
```

> 🔧 `systemCode` 被显式加入白名单是**修复过的隐患**：漏了它会导致公开端拉取主题时主题编码丢失，前端配色退化为默认。

### 17.3 修改系统信息

| 项 | 内容 |
|---|---|
| 路径 | `PUT /api/system/info` |
| 鉴权 | `system:edit` |
| 请求格式 | **JSON**（直接绑定 `TSystemInfo` 实体） |

```json
{ "id": 1, "name": "智路 CRM", "title": "智路CRM 管理系统", "version": "v1.0.0",
  "isopen": "true", "closeMsg": "", "systemCode": "green", "logo": "/static/logo.png" }
```

**响应**：`{"code":200,"msg":"成功"}` / `{"code":500,"msg":"失败"}`

> ⚠️ 本接口**直接绑定实体类**（不是 Query/DTO），前端传什么字段就改什么字段（`updateByPrimaryKeySelective` 语义，null 不更新）。这是全项目唯一一处 PUT 直接吃实体的接口——好处是省事，代价是缺少字段白名单，`createBy` 之类审计字段理论上可被篡改。见[附录 D](#附录-d已知不一致与待改进项)。

> 📌 **AI 关联**：AI 管理员能力「系统运行巡检」复用的就是本模块（`SYSTEM_INSPECT` 工具 + JVM 运行环境信息）。

---

## 十八、AI 业务智能体模块

> 本模块是项目最大的技术亮点，采用 **LangChain4j + SSE + Tool Calling** 实现"角色感知的业务智能体"。
> 深度原理另见 `docs/源码学习手册_02_AI模块深度解析.md`，这里只描述接口契约。

### 18.0 模块架构与能力矩阵

```
前端 fetch + ReadableStream（自己解析 SSE）
   ↓ GET /api/ai/stream-chat（SSE）
TokenVerifyFilter（请求头取 token）
   ↓
AiAgentController.streamChat(Authentication)
   ↓
AiAssistantServiceImpl.chatStream()   ← 编排中枢
   ├── AiRoleResolverService.isAdmin()   实时查库判角色（不信前端、不信令牌）
   ├── AdminAgentToolkit / UserAgentToolkit   按角色【物理隔离】挂载工具
   ├── AiPromptProvider   提示词模板（classpath:prompts/*.md，启动时加载 fail-fast）
   ├── MessageWindowChatMemory   会话记忆（窗口 20 条，key = "userId:memoryId"）
   ├── AiChatHistoryService   聊天记录持久化（Redis）
   ├── Sinks.Many<String>   工具侧路事件通道
   └── Flux.merge(模型token流, 事件帧流) → SSE
```

**能力目录（`AiAbility` 枚举 = 增值付费体系的唯一事实来源）**

| key | 名称 | 可见范围 | 付费 | 价格 | 对应对话指令示例 |
|---|---|---|---|---|---|
| `my_orders` | 我的订单查询 | USER | 免费 | — | 「查询我最近的订单」 |
| `tran_reminder` | 交易跟进提醒 | USER | 免费 | — | 「未来3天我有哪些交易需要跟进联系？」 |
| `business_faq` | 业务疑难解答 | USER | 免费 | — | 「交易阶段是怎么流转的？」 |
| `product_consult` | 产品行情咨询 | USER | 免费 | — | 「查询当前在售的产品和报价」 |
| `user_export` | 用户数据导出 | ADMIN | 免费 | — | 「导出全部用户数据为Excel」 |
| `user_import` | 用户数据导入 | ADMIN | 免费 | — | 「把我刚才上传的Excel附件批量导入用户」 |
| `tran_overview` | 全局订单查询 | ADMIN | 免费 | — | 「全局查询最近的交易订单」 |
| `user_permission` | 用户权限管理 | ADMIN | 免费 | — | 「查询所有用户的账号状态与角色」「查询全部可用角色」 |
| `system_inspect` | 系统运行巡检 | ADMIN | 免费 | — | 「巡检系统运行状态」 |
| `deep_insight` | 💎 经营深度洞察报告 | **ALL** | **付费** | **¥9.90** | 「生成我的经营深度洞察报告」/「生成全局经营深度洞察报告」 |
| `trend_forecast` | 💎 交易趋势预测 | **ALL** | **付费** | **¥4.90** | 「预测我的交易趋势」 |

可见范围规则（`isVisible()`）：`ALL` → 双方可见；`ADMIN` → 仅管理员；`USER` → 仅普通用户。

---

### 18.1 AI 角色画像

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/ai/profile` |
| 鉴权 | 需登录（**无权限码**，AI 模块对全员开放） |
| 请求参数 | 无（身份完全从登录态判定） |
| 前端调用时机 | 进入 AI 模块 `onMounted` 时；付费开通成功后再次调用刷新状态 |

**响应 `data`（`AiRoleProfile`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `userId` | Integer | 登录人 ID（前端据此生成确定性会话 ID `AI-BIZ-{userId}`） |
| `role` | String | `"ADMIN"` 或 `"USER"`（常量在 `AiRoleProfile.ROLE_ADMIN/ROLE_USER`） |
| `userName` | String | 姓名（用于欢迎语与提示词渲染） |
| `abilities` | `AiAbilityVO[]` | **按角色过滤后**的能力清单 |

**`AiAbilityVO` 结构**

| 字段 | 类型 | 说明 |
|---|---|---|
| `key` | String | 能力唯一标识（下单时传它；也是 `PREMIUM_REQUIRED` 事件的内容） |
| `name` | String | 能力名称（下单时**冗余为快照**存入订单表，防枚举改名后历史订单显示错乱） |
| `description` | String | 能力说明 |
| `premium` | Boolean | 是否付费能力 |
| `price` | BigDecimal | 价格（免费能力为 `null`） |
| `purchased` | Boolean | **运行时开通状态**：免费能力恒 `true`；付费能力查 `isGranted()` |

```json
{
  "code": 200, "msg": "成功",
  "data": {
    "userId": 3, "role": "ADMIN", "userName": "张三",
    "abilities": [
      { "key": "user_export", "name": "用户数据导出", "description": "将全部用户数据导出为 Excel 并生成下载链接",
        "premium": false, "price": null, "purchased": true },
      { "key": "deep_insight", "name": "经营深度洞察报告", "description": "汇总转化漏斗、来源分布与订单规模，生成多维经营洞察报告",
        "premium": true, "price": 9.90, "purchased": false },
      { "key": "trend_forecast", "name": "交易趋势预测", "description": "基于月度交易额走势进行环比推演与趋势预判",
        "premium": true, "price": 4.90, "purchased": true }
    ]
  }
}
```

> 💡 每次调用会遍历枚举并对**每个付费能力**查一次开通状态（`isGranted` 走 Redis 正/负缓存，成本低）。若能力清单变大，可考虑批量查询优化。

---

### 18.2 ⭐ AI 流式对话（SSE）

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/ai/stream-chat` |
| 鉴权 | 需登录（**无权限码**，付费能力在工具层拦截） |
| 响应类型 | `produces = text/event-stream`（SSE），返回 `Flux<String>` |
| **令牌通道** | 请求头 `Authorization`。改造前走 URL 查询参数（`EventSource` 不能设置请求头），现在改用 `fetch`，已废弃 URL 通道 |
| 前端封装 | `front/src/http/sseClient.js` → `createSseConnection(AI_STREAM_CHAT, {...})` |

**查询参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `message` | String | 是 | 用户输入的自然语言指令（需 `encodeURIComponent`） |
| `memoryId` | String | 是 | 会话 ID。前端约定为 `AI-BIZ-{userId}`（确定性，切模块/刷新可延续上下文）；profile 加载失败时退化为随机 ID |
| `attachmentFileId` | String | 否 | 附件凭证，须匹配 `^[a-zA-Z0-9]{8,32}$`，来自 §18.9 的上传响应 |
```http
GET /api/ai/stream-chat?message=%E6%9F%A5%E8%AF%A2%E6%88%91%E6%9C%80%E8%BF%91%E7%9A%84%E8%AE%A2%E5%8D%95&memoryId=AI-BIZ-3 HTTP/1.1
Authorization: eyJhbGc...
Accept: text/event-stream
```

**响应报文**
```http
HTTP/1.1 200 OK
Content-Type: text/event-stream;charset=UTF-8
Transfer-Encoding: chunked
Cache-Control: no-cache

data:您

data:好

data:，已为您查询到 3 条订单：

data:1. TR202610180007 ...

data:[AI-EVENT:FILE_READY:用户数据导出_20261018153012.xlsx]
```

**SSE 帧格式**：每帧 `data:` + 内容，帧间以**空行**（`\n\n`）分隔；冒号后第一个空格被客户端自动吃掉。前端 `event.data` 拿到的就是冒号后那段字符串。

**帧的两种性质（关键设计）**

| 类别 | 来源 | 前端处理 |
|---|---|---|
| **模型 token** | 大模型逐字生成，经 `Flux` 推送 | 追加到 `rawBuffer`，经 `formatMessage()` 做轻量 Markdown 渲染后显示（打字机效果） |
| **结构化事件帧** `[AI-EVENT:类型:内容]` | Java 工具代码通过 `Sinks` **侧路注入**，不经模型 | 正则摘出，渲染成可交互卡片（付费开通按钮 / 文件下载按钮） |

**两种事件类型**

| 事件标记 | 含义 | 前端渲染 | 触发条件 |
|---|---|---|---|
| `[AI-EVENT:PREMIUM_REQUIRED:{abilityKey}]` | 触发了付费能力但未开通 | 「💎 增值能力待开通」卡片 + `¥{price} 立即开通` 按钮 | `AbstractAiToolkit.premiumGate()` 判定未开通 |
| `[AI-EVENT:FILE_READY:{fileName}]` | Excel 已生成到服务端磁盘 | 「📦 文件已生成」卡片 + 下载按钮（调 §18.10） | 导出用户 / 下载导入模板工具执行成功 |

> 🎯 **为什么不让大模型自己说"请前往开通"**：模型可能忘记提、可能把价格说错、可能编造不存在的按钮。事件帧由 Java 代码硬写，**确定性 100%**——这是"业务关键交互不依赖大模型转述"的设计原则。

**前端解析要点**（`extractEvents()`）
- 事件标记可能被**网络分片拆断**（如先收到 `[AI-EVENT:PREMIU`，下一个 chunk 才补 `M_REQUIRED:deep_insight]`），所以用 `rawBuffer` 缓冲 + 检查末尾是否残留未闭合的 `[AI-EVENT`，残留部分暂不显示、等后续分片补齐；
- 卡片去重（按 `raw` 全文比对）；
- 流结束后若仍有未闭合片段，直接当正文展示（极端情况兜底，不丢字）。

**后端流的合并与终结（最容易踩坑的地方）**
```java
Flux<String> modelStream = agent.chat(memKey, augmentedMessage)
        .doOnNext(replyBuffer::append)                      // 聚合成完整回复
        .doOnComplete(() -> persistAiReply(...))            // 仅正常完成才落存（错误走 onErrorResume 兜底，避免重复）
        .doFinally(signal -> eventSink.tryEmitComplete());  // ★ 关键
return Flux.merge(modelStream, eventSink.asFlux())
        .onErrorResume(error -> Flux.just(fallback));
```
> 🔴 **`Flux.merge` 要等所有源都完成才结束，而事件 Sink 不会自己完成**。若不在 `doFinally` 里主动 `tryEmitComplete()`，SSE 连接永不关闭，前端一直转圈。这是实测踩过的坑。

**错误降级**：模型调用异常时返回友好文案并落存历史：
```
\n\n抱歉，本次服务出现异常：{原始消息}。请稍后重试或检查后端大模型配置。
```

**前端的 SSE 客户端**（`sseClient.js`，`createSseConnection`，对外接口不变）：用 `fetch` 发请求、`ReadableStream` 读响应体、`TextDecoder` 解码（`stream: true` 让解码器记住被切在两个数据块之间的半个汉字），再按 SSE 格式自己解析：按行切，`data:` 行累积，**空行表示一个事件结束**，多行 `data:` 用 `\n` 拼接，以 `:` 开头的是注释，冒号后若有一个空格要去掉。网络数据块可能在任意位置被切开，所以要用缓冲区攒够一整行再处理。

**出错表现**：认证失败时过滤器返回的是 `Content-Type: application/json` 的 JSON，而成功的流是 `text/event-stream`。`openStream` 靠响应的 `Content-Type` 区分，不能靠 HTTP 状态码（认证失败的 HTTP 状态仍是 200）：
- 是 JSON 且 `code === 906`：调 `refreshAccessToken()` 刷新一次再重试（最多重试一次，防止死循环）；
- 其他 JSON 错误：抛出带 `msg` 的错误，走 `onError`，AI 气泡里显示「⚠️ 连接中断…」，**不会**再出现原始 JSON；
- 其他 9xx 的弹窗由工作台 5 秒一次的心跳（走 axios）触发，这里不重复弹。

**结束判定是确定的，不再靠猜**：流读到 `done` 就是服务端正常结束，直接走 `onComplete`；网络断开会让 `read()` 抛错，走 `onError`。旧版 `EventSource` 的规范行为是"服务端正常关闭连接也会触发 `onerror`"，只能用"最近 1500ms 还在收数据就算正常"的时间差启发式去猜，现在不需要了。

**中断与不自动重连**：`close()` 调用 `AbortController.abort()`，随后 `read()` 抛 `AbortError`，但收尾标志已置位，不会重复回调；`close()` 仍然触发 `onComplete`（和旧版一致，组件卸载时依赖它）。`EventSource` 出错时会自动重连，而重连等于把这条 AI 指令重发一遍，可能让下单之类的工具被重复执行；`fetch` 不会自动重连。

---

### 18.3 清空会话记忆

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/ai/reset` |
| 鉴权 | 需登录 |
| 参数 | `memoryId`（String，必填） |

**响应**：`{"code":200,"msg":"成功"}`

**做了四件事**（产品语义：「清空对话记忆」= 重新开始一段全新会话）
```
① memoryCache.remove("userId:memoryId") + chatMemory.clear()   上下文记忆
② sessionAttachmentCache.remove(...)                            会话级附件暂存
③ aiChatHistoryService.clearHistory(userId)                     Redis 聊天记录（清空全部，不限该 memoryId）
```

> ⚠️ 记忆里与 `userId` 绑定，所以**清空会连带清掉该用户所有 memoryId 的历史记录**（当前产品只有单一会话入口，语义上没问题；若将来支持多会话需调整）。

---

### 18.4 获取聊天记录

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/ai/history` |
| 鉴权 | 需登录 |
| 参数 | 无（按登录人 ID 取） |
| 存储 | Redis `apex_sales:ai:chat_history:{userId}`，**上限 100 条**（超出滚动丢弃最早的） |

**响应 `data`（`List<AiChatHistoryItem>`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `role` | String | `"user"` 或 `"ai"` |
| `content` | String | 消息正文 |
| `time` | String | 展示时间，格式 **`HH:mm:ss`**（不是完整时间戳，与前端气泡格式对齐） |

```json
{ "code": 200, "msg": "成功",
  "data": [
    { "role": "user", "content": "查询我最近的订单", "time": "15:30:12" },
    { "role": "ai",   "content": "您名下共有 3 笔订单…", "time": "15:30:15" }
  ] }
```

**持久化边界**：登录态内（切模块 / 刷新 / 关浏览器再开）**完整恢复**；退出登录时由 `MyLogoutSuccessHandler` **永久清除**。

> 📌 **产品取舍（注释里写明的）**：付费开通卡片、文件下载按钮这类**瞬时交互事件不恢复**——恢复历史记录时 `events` 数组固定为空。理由是下载链接/开通按钮属于当轮交互，隔天恢复出来点击可能已失效，反而误导。
> 📌 **空回复不落存**：客户端中途断连、模型无输出时不写入历史，避免界面出现空气泡。

---

### 18.5 创建付费能力订单

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/ai/payment/order` |
| 鉴权 | 需登录 |
| 请求格式 | **JSON**（`@RequestBody AiPaymentQuery`） |
| 事务 | ✅ |
| **幂等** | ✅ 同一用户对同一能力已有待支付订单时**直接复用**，不产生垃圾单 |

```json
{ "abilityKey": "deep_insight" }
```

**响应 `data`（`TAiPaymentOrder`）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Long | 主键 |
| `orderNo` | String | 订单号：`AI` + `yyyyMMddHHmmss` + 4 位随机数，如 `AI202610181530120483` |
| `userId` | Integer | 归属用户 |
| `abilityKey` | String | 能力 key |
| `abilityName` | String | 能力名称（**下单时的名称快照**） |
| `price` | BigDecimal | 价格（**从枚举取值，不信前端**） |
| `status` | Integer | `0` 待支付 / `1` 已支付 / `2` 已取消 |
| `paidTime` | Date | 支付时间（未支付为 null） |
| `expireTime` | Date | 开通到期时间（支付成功后 = 支付时间 + 30 天） |
| `createTime` | Date | 下单时间 |
| `remark` | String | 备注 |

```json
{ "code": 200, "msg": "成功",
  "data": { "id": 12, "orderNo": "AI202610181530120483", "userId": 3,
            "abilityKey": "deep_insight", "abilityName": "经营深度洞察报告",
            "price": 9.90, "status": 0, "paidTime": null, "expireTime": null,
            "createTime": "2026-10-18 15:30:12", "remark": null } }
```

**错误**：`3001`（能力不存在 / 该能力是免费的，不该下单）

> 🔒 **价格由服务端从 `AiAbility` 枚举取，绝不信任前端传值**——旧版若让前端传 price 就是任意金额购买的漏洞。
> 🔒 订单号注释里明确写了"单机演示足够唯一，**生产应接入发号器**"——当前用 `ThreadLocalRandom` 4 位随机数，高并发下有极低概率重号。

---

### 18.6 模拟支付（完成开通）

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/ai/payment/pay` |
| 鉴权 | 需登录（**归属校验在 Service 层**，不是权限码） |
| 请求格式 | JSON |
| 事务 | ✅（推进订单状态 + 开通能力，同事务强一致） |
| **幂等** | ✅ 已支付订单重复调用直接返回成功（模拟支付网关重复回调） |

```json
{ "orderNo": "AI202610181530120483" }
```

**响应**：`data` 为**最新的**订单对象（`status=1`，`paidTime` / `expireTime` 已填充）

**错误**

| code | 场景 |
|---|---|
| `3002` | 订单不存在，**或订单不属于当前登录人**（统一按"找不到"处理，不泄露他人订单存在性） |
| `3003` | 订单已是「已取消」状态 |
| `3004` | 状态机推进失败（**并发双花**：SQL 乐观锁 `UPDATE ... WHERE orderNo=? AND status=0` 只有一个请求能成功） |

**状态机推进设计（企业级）**
```sql
-- transitStatus：状态机单向推进由 SQL 层乐观锁保证，天然幂等
UPDATE t_ai_payment_order SET status = #{to} WHERE order_no = #{orderNo} AND status = #{from}
```
返回 0 行即代表并发失败 → `AI_PAYMENT_FAILED`。无需额外加锁，数据库行锁已解决。

**开通缓存的一致性闭环（三道防线，本项目质量最高的设计之一）**

| 防线 | 机制 |
|---|---|
| **写路径** | 订单事务 `afterCommit` 才写 Redis 正缓存。原因：Redis 写不参与数据库事务，若在提交前写缓存，一旦事务回滚会出现**「未付款用户被判定已开通」的资损级误放行** |
| **读路径** | 正缓存命中→放行 / 负缓存命中→拦截（**防缓存穿透**：未付费请求不每次打库）/ 均未命中→DB 点查（`idx_user_ability` 索引，取未过期已支付订单的 `MAX(expire_time)`）→ **`SETNX` 回填**（防并发覆盖竞态） |
| **对账兜底** | `AiPremiumCacheReconcileTask` 周期扫描（默认 5 分钟）缓存与 DB 收敛，覆盖"绕过应用直改数据库"的旁路场景 |

**Redis Key 设计**

| Key | 含义 | TTL |
|---|---|---|
| `apex_sales:ai:premium:{userId}:{abilityKey}` | 已开通标记（value = `"1"`） | **动态对齐 DB 剩余有效期**（最长 30 天），两侧到期语义完全一致 |
| `apex_sales:ai:premium:{userId}:{abilityKey}:deny` | **负缓存**（确认未开通） | 60 秒 |

> 负缓存 60 秒的取值权衡：太长会导致"刚付款用户短时间内仍被误拦"的体验窗口，太短则防穿透效果差。实际开通时**主动清除负缓存**，所以新开通用户不会被误拦。
> Redis 故障时**降级直查数据库**，缓存层不可用不阻断主流程（可用性优先）。

---

### 18.7 取消订单

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/ai/payment/cancel` |
| 鉴权 | 需登录 + Service 层归属校验 |
| 请求格式 | JSON `{ "orderNo": "AI..." }` |

**响应**：`data` 为最新订单对象（`status=2`）

**错误**：`3002`（不存在/非本人）、`3003`（**已支付订单不允许取消**——模拟支付不支持退款，退款走独立客服流程）、`3004`（乐观锁推进失败）

---

### 18.8 我的充值订单列表

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/ai/payment/orders` |
| 鉴权 | 需登录 |
| 参数 | 无（只能看自己的，SQL 层 `WHERE user_id = #{登录人id}` 强制过滤） |

**响应**：`data` 为 `List<TAiPaymentOrder>`（字段见 §18.5），**不分页**，前端弹窗表格直接渲染。

---

### 18.9 上传待导入 Excel（管理员专属）

| 项 | 内容 |
|---|---|
| 路径 | `POST /api/ai/file/upload` |
| 鉴权 | ⚠️ **`admin` 角色**（`@PreAuthorize("hasAuthority('admin')")`） |
| 请求格式 | **multipart/form-data** |
| 表单字段 | `file`（必须叫这个名字，`accept=".xlsx,.xls"`） |
| 大小限制 | 单文件 5MB（`spring.servlet.multipart.max-file-size`） |

**响应 `data`（`UploadResult` record）**

| 字段 | 类型 | 说明 |
|---|---|---|
| `fileId` | String | **16 位随机 hex**（`UUID` 去横线取前 16 位），后续作为 `attachmentFileId` 传入对话 |
| `fileName` | String | 客户端原始文件名（仅用于界面展示） |

```json
{ "code": 200, "msg": "成功", "data": { "fileId": "3f2a9c1b7e5d4a8f", "fileName": "用户批量导入.xlsx" } }
```

**错误**
```json
{ "code": 500, "msg": "未接收到有效文件" }
{ "code": 500, "msg": "仅支持 .xlsx / .xls 格式的 Excel 文件" }
```

**服务端行为**
```
① 校验非空 + 扩展名白名单（.xlsx / .xls）
② 随机生成 fileId，重命名为 {fileId}.xlsx 存入 target/ai-export/upload/
   ↳ 文件名由服务端决定，客户端原始名只进日志，绝不参与路径拼接 → 无路径注入面
③ Files.copy 落盘（REPLACE_EXISTING）
④ 记录审计日志：operator / fileId / originalName / size
```

**导入模板列（`UserImportRow`）**：`loginAct`、`loginPwd`、`name`、`phone`、`email`
> 未提供密码的行统一用 `Constants.AI_IMPORT_DEFAULT_PASSWORD = "apexSales@2026"`。

**附件的两阶段使用（前端一次性消费 + 服务端会话级暂存）**
```
上传 → 前端拿到 fileId 存 attachment.value
     ↓ 发消息时带上 attachmentFileId，然后 attachment.value = null（一次性消费）
服务端 loadAttachment(fileId) → 读盘 → Base64 → 存入 sessionAttachmentCache（key = userId:memoryId）
     ↓ 本轮没带 fileId 时从 sessionAttachmentCache 回捞
为什么要回捞：模型对敏感操作采用「先确认再执行」，用户的确认回复发生在【后续轮次】，
             不再携带 attachmentFileId，若无暂存则导入工具拿不到文件
     ↓ 导入真正成功后
调用 consumeAttachment() 钩子移除暂存 → 防下一轮重复导入
```
附件体积额外限制 1.5MB（`ATTACHMENT_MAX_BYTES`，防超大 payload 打爆内存）。
提示词会随附件语境变化：本轮上传用 `attachment-current.md`（含"直接调用导入工具、无需再索要文件"强指令），回捞场景用 `attachment-carried.md`（中性提示，避免与上一轮"等待确认"的语境冲突）。

---

### 18.10 下载 AI 生成的 Excel（管理员专属，二进制流）

| 项 | 内容 |
|---|---|
| 路径 | `GET /api/ai/file/download` |
| 鉴权 | ⚠️ **`admin` 角色** |
| **令牌通道** | 请求头 `Authorization`（改造前走 URL 参数，已废弃） |
| 响应类型 | `application/octet-stream`（**非 JSON**，方法返回 `void`，手写 response） |

**参数**

| 参数 | 类型 | 必填 | 说明 |
|---|---|---|---|
| `fileName` | String | 是 | 文件名（来自 `FILE_READY` 事件），如 `用户数据导出_20261018153012.xlsx` |

**前端调用**（`AiAssistantView.vue` → `doDownload`）：`await doDownload('/api/ai/file/download', { fileName }, fileName)`，和客户导出同一套 blob 下载机制，见 §11.5。

**响应头**
```http
HTTP/1.1 200 OK
Content-Type: application/octet-stream
Content-disposition: attachment;filename=%E7%94%A8%E6%88%B7%E6%95%B0%E6%8D%AE%E5%AF%BC%E5%87%BA_20261018153012.xlsx
```

**错误（返回纯 HTTP 状态码，无 JSON 体）**

| HTTP 状态 | 条件 |
|---|---|
| `400 Bad Request` | `fileName` 为空 / 含 `/` 或 `\` 或 `..` / 不以 `.xlsx`、`.xls` 结尾 |
| `404 Not Found` | 文件在 `target/ai-export/` 下不存在 |

**AI 工具生成的文件命名规则**

| 工具 | 文件名格式 | 示例 |
|---|---|---|
| `exportUsersExcel` | `用户数据导出_yyyyMMddHHmmss.xlsx` | `用户数据导出_20261018153012.xlsx` |
| `downloadImportTemplate` | `用户导入模板_yyyyMMddHHmmss.xlsx` | `用户导入模板_20261018154201.xlsx` |

输出目录：`Constants.AI_EXPORT_DIR = "target/ai-export"`（相对工作目录，即 `server/target/ai-export/`）

> ⚠️ **`mvn clean` 会清空 `target/` 目录，所有已生成的下载文件一并丢失**。前端界面上残留的「下载文件」卡片会 404。这是 `target/` 作为产物目录的固有特性，生产应改到工作目录外（如 `/data/dlyk/ai-export`）。

> 🔒 时间戳命名天然避免同名覆盖；中文文件名做了 `URLEncoder.encode` 兼容；下载接口同样受 `admin` 角色约束，令牌走请求头，不再出现在 URL 里。

---

### 18.11 AI 工具清单（模型可调用的后端能力）

**普通用户工具包 `UserAgentToolkit`（6 个）**

| 工具方法 | `@Tool` 说明（模型据此决定调用） | 参数 |
|---|---|---|
| `queryMyOrders` | 查询当前登录用户名下的交易订单（返回流水号、客户姓名、金额、阶段、创建时间） | 条数上限等可选参数 |
| `queryMyFollowUpReminder` | 查询指定天数内需跟进联系客户的交易提醒（下次联系时间临近且未成交） | 天数 |
| `answerBusinessFaq` | 解答 CRM 业务流程疑难（交易阶段含义、线索转化流程、客户管理规则，内置知识库） | `question` |
| `queryProductsOnSale` | 查询在售产品目录及经销商报价 | — |
| `generateDeepInsightReport` | 💎 生成当前用户的经营深度洞察报告 | — |
| `forecastTranTrend` | 💎 预测当前用户的交易趋势（按月环比推演） | — |

**管理员工具包 `AdminAgentToolkit`（9 个）**

| 工具方法 | `@Tool` 说明 |
|---|---|
| `exportUsersExcel` | 导出全部用户数据为 Excel（成功后前端自动收到下载卡片） |
| `downloadImportTemplate` | 生成用户批量导入的标准 Excel 模板 |
| `importUsersFromAttachment` | 将当前会话上传的 Excel 附件批量导入用户 |
| `queryAllOrders` | 全局查询所有用户交易订单（可按客户姓名模糊搜索、按阶段筛选）；`customerName` 标注 `required=false`（不需要时**省略该参数**而非按姓名过滤） |
| `queryAllUsers` | 查询全部用户及其角色、账号状态（权限管理前的人员盘点），limit 最多 50，默认 20 |
| `queryRoleList` | 查询系统全部可用角色及 ID（**分配角色前必须先获取本清单**） |
| `updateUserPermission` | 修改指定用户的账号状态与角色权限 |
| `generateGlobalDeepInsight` | 💎 全局经营深度洞察报告（汇总转化漏斗、来源分布、订单与交易金额） |
| `forecastGlobalTrend` | 💎 全局交易趋势预测（按月度汇总交易额走势做趋势推演） |

**付费墙机制（`AbstractAiToolkit.premiumGate()`）**
```java
if (premiumAbilityService.isGranted(userId, ability)) return null;   // 放行
// 未开通：双管齐下
context.getEventEmitter().accept("[AI-EVENT:PREMIUM_REQUIRED:" + ability.getKey() + "]");  // ① 推前端卡片
return "【付费能力未开通】「xxx」为增值付费能力（¥9.90）。系统已向用户推送快捷开通卡片，…";      // ② 回给模型的引导文案
```
> 🔒 **安全模型**：大模型可能受 Prompt 诱导调用付费工具，所以付费校验**必须发生在工具执行层（服务端）**，不能依赖模型自觉、更不能依赖前端开关（前端 `purchased=false` 只是 UI 表现，绕过 UI 直接调接口照样会被工具层拦住）。

**工具调用的完整回合**
```
第1轮：LangChain4j 把「提示词 + 20条记忆 + 全部工具的名称/说明/参数schema」发给大模型
      → 模型返回 tool_call（"我要调 queryMyOrders，参数是 xxx"）
      → LangChain4j 在后端反射执行该 @Tool 方法
      → 方法内查 MySQL（MyBatis），组装结果文本 return；若撞付费墙则侧路推事件帧
第2轮：把工具执行结果再发给模型
      → 模型基于真实数据生成自然语言回答，逐 token 流式吐回
```

---

## 附录 A：数据结构字典

### A.1 `TUser` 完整字段

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 用户 ID（主键自增） |
| `loginAct` | String | 登录账号（唯一索引） |
| `loginPwd` | String | 密码 BCrypt 哈希。**对外接口一律脱敏为 null**（注册/详情/登录信息），仅改密校验内部使用 |
| `name` | String | 用户姓名 |
| `phone` | String | 手机号（唯一索引） |
| `email` | String | 邮箱（唯一索引，**也是忘记密码的身份核验依据**） |
| `accountNoExpired` | Integer | 账号是否未过期：`1` 未过期 / `0` 已过期 |
| `credentialsNoExpired` | Integer | 凭证（密码）是否未过期：`1` / `0` |
| `accountNoLocked` | Integer | 账号是否未锁定：`1` 正常 / `0` 锁定 |
| `accountEnabled` | Integer | 账号是否启用：`1` 启用 / `0` 禁用 |
| `createTime` / `createBy` | Date / Integer | 创建信息 |
| `editTime` / `editBy` | Date / Integer | 编辑信息 |
| `lastLoginTime` | Date | 最近登录时间（**单列精准更新**，不整表回写） |
| `roleList` | String[] | 角色标识，如 `["admin"]`、`["saler"]` |
| `roleIds` | Integer[] | 角色 ID 列表（编辑回显用；**登录构建用户时为 null，不进 JWT**） |
| `permissionList` | String[] | 权限码列表，前后端鉴权依据 |
| `menuPermissionList` | TPermission[] | 菜单权限树（前端动态渲染侧边栏） |
| `createByUser` / `editByUser` | TUser | 创建人 / 编辑人关联对象 |

> 🔑 **超级管理员隐式全权限**：凡角色含 `admin`，`resolveButtonPermissions()` 直接返回系统内**全部**按钮权限码，与 `t_role_permission` 绑定表的完整性解耦。
> 修复的真实回归：种子数据只要漏配一行，管理员就会被自己的权限注解挡在外面——实测环境缺 `tran:add/edit/delete` 与 `customer:delete`，导致交易新建/编辑/删除、阶段推进、客户删除全部不可用。"管理员功能可用"是系统基本可用性契约，不应被运维数据完整性问题击穿。（当前种子数据 `ApexSales_data.sql` 已全部补齐这 4 个权限码，隐式全权限是防运维漏配的长期防线。）
> 注意：只提升**按钮级**权限，不影响菜单树（`menuPermissionList`），侧边栏导航结构仍由实际配置决定。

### A.2 业务字典枚举

通过 `GET /api/dicvalue/{typeCode}` 获取。**下表 `id` 列就是各业务接口要传的值**。

**`stage` 交易阶段**（⭐ 交易模块核心，推进顺序由 `order` 决定）

| id | order | typeValue | 备注 |
|---:|---:|---|---|
| 12 | 1 | 01创建交易 | 新建交易的默认阶段 |
| 37 | 3 | 02确认清单 | ⚠️ order 断号（缺 2） |
| 40 | 4 | 03交付定金 | |
| 35 | 5 | 04产品检验 | |
| 42 | 6 | 05付款成交 | **终态** |
| 21 | 7 | 06丢失关闭 | **终态** |

**`clueState` 线索状态**

| id | order | typeValue |
|---:|---:|---|
| -1 | 0 | 已转客户（转化后系统写入） |
| 10 | 1 | 试图联系 |
| 6 | 2 | 将来联系 |
| 27 | 3 | 已联系 |
| 1 | 4 | 虚假线索 |
| 7 | 5 | 丢失线索 |
| 24 | 6 | 未联系 |
| 30 | 7 | 需要条件 |

**`source` 线索来源**

| id | typeValue | ｜ | id | typeValue |
|---:|---|---|---:|---|
| 16 | 网络广告 | | 2 | 知乎 |
| 33 | 懂车帝 | | 17 | 视频直播 |
| 39 | 员工介绍 | | 25 | 朋友圈 |
| 45 | 门店参观 | | 3 | 车展会 |
| 43 | 官方网站 | | 36 | 易车网 |
| 23 | 合作伙伴 | | 22 | 地图 |
| 44 | 公众号 | | 14 | 汽车之家 |

**`appellation` 称呼**：`18` 先生 / `20` 夫人 / `41` 女士 / `11` 博士 / `5` 教授

**`intentionState` 意向状态**：`46` 有意向 / `47` 无意向 / `48` 意向不明

**`needLoan` 是否贷款**：`49` 需要 / `50` 不需要

**`noteWay` 跟踪方式**（跟进记录用）：`61` 电话 / `62` 微信 / `63` QQ / `64` 面聊 / `65` 其他

**`userState` 用户状态**：`58` 正常 / `59` 锁定 / `60` 禁用

**`returnState` 回访状态**：`8` 未启动 / `28` 推迟 / `26` 进行中 / `29` 完成 / `32` 等待某人

**`returnPriority` 回访优先级**：`19` 高 / `4` 最高 / `15` 低 / `38` 最低 / `34` 常规

**`sex` 性别**：`51` 男 / `52` 女 ｜ **`educational` 学历**：`53` 小学 / `54` 初中 / `55` 高中 / `56` 大学 / `57` 研究生

> `transactionType`（交易类型）在 `t_dic_type` 中已定义但**种子数据无任何字典值**，当前接口未使用。

**`t_role` 角色表**

| id | role | roleName |
|---:|---|---|
| 1 | `admin` | 管理员 |
| 2 | `saler` | 销售员 |
| 3 | `manager` | 销售经理 |
| 4 | `marketing `（⚠️ **尾部有一个空格**） | 市场营销 |
| 5 | `accountant` | 会计 |

**`t_permission` 权限表结构**

| 字段 | 说明 |
|---|---|
| `name` | 权限中文名，如「市场活动-列表」 |
| `code` | **权限码**，如 `activity:list`；菜单级为 null |
| `url` | 前端路由，如 `/dashboard/activity`；按钮级为 null |
| `type` | `menu`（菜单）/ `button`（按钮） |
| `parentId` | 父权限 ID（构成菜单树，顶级为 0） |
| `orderNo` | 同级排序号 |
| `icon` | 图标名（Element Plus Icons，如 `OfficeBuilding`） |

### A.3 备注类实体（三张表同构）

`TClueRemark` / `TTranRemark` / `TActivityRemark` 结构一致，只差外键字段名（`clueId` / `tranId` / `activityId`）：

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | Integer | 备注 ID |
| `noteContent` | String | 内容 |
| `noteWay` | Integer | 方式（字典 id，活动备注无此字段） |
| `noteWayName` | String | **写入时冗余落库**的名称快照 |
| `createBy` / `createByName` | Integer / String | 创建人（`createByName` 同为写入时快照） |
| `createTime` | Date | |
| `editBy` / `editTime` | Integer / Date | |
| `deleted` | Integer | `0` 正常 / `1` 已删除（**仅活动备注真的用了逻辑删除**） |

> 📌 **快照 vs 联表的取舍**：`noteWayName` / `createByName` 在写入时冗余存库（而非查询时联表），好处是列表查询快、历史记录不随字典改值/用户改名而变（审计场景要的正是"当时写的是什么"）；代价是字典改值后老记录显示旧名称。

### A.4 AI 模块专用结构

| 类 | 用途 |
|---|---|
| `AiAbility`（枚举） | 能力目录，**增值付费体系的唯一事实来源** |
| `AiAbilityVO` | 能力对外视图（含运行时 `purchased` 状态） |
| `AiRoleProfile` | 角色画像（`userId` / `role` / `userName` / `abilities`） |
| `AiChatHistoryItem` | 聊天历史项（`role` / `content` / `time`），实现 `Serializable` 以存入 Redis |
| `TAiPaymentOrder` | 付费订单实体 |
| `UserExportRow` | AI 导出的用户 Excel 行：`id` / `loginAct` / `name` / `phone` / `email` / `accountStatus` / `roleNames` / `createTime` |
| `UserImportRow` | AI 导入模板行：`loginAct` / `loginPwd` / `name` / `phone` / `email` |
| `AiAgentContext` | **请求级不可变身份快照**（userId / userName / isAdmin / 事件发射钩子 / 附件名 / 附件内容 / 附件消费钩子），跨线程安全 |
| `UploadResult`（record） | 上传结果：`fileId` / `fileName` |

### A.5 Redis Key 命名规范总表

命名约定：`项目名:模块名:功能名:唯一业务参数`

| Key | 内容 | TTL |
|---|---|---|
| `apex_sales:user:login:{userId}` | **当前有效会话**，值为 `sid:jti`（**不是 JWT**）：`sid` 是单设备互斥的判据，`jti` 是 Refresh 重放检测的判据 | 3600s / 604800s（记住我）；每次刷新重置为满值 |
| `apex_sales:user:owner` | 负责人下拉列表缓存 | 10 分钟（兜底自愈） |
| `apex_sales:login:fail:{loginAct}` | 登录失败计数 | 10 分钟 |
| `apex_sales:login:lock:{loginAct}` | 账号锁定标记 | 30 分钟 |
| `apex_sales:reset_pwd:limit:{loginAct}` | 发验证码限流 | 60 秒 |
| `apex_sales:reset_pwd:code:{loginAct}` | 重置密码验证码 | 300 秒 |
| `apex_sales:reset_pwd:fail_count:{loginAct}` | 验证码错误计数（5 次熔断） | 300 秒 |
| `apex_sales:ai:premium:{userId}:{abilityKey}` | 增值能力已开通标记 | 动态对齐 DB 剩余有效期（≤30 天） |
| `apex_sales:ai:premium:{userId}:{abilityKey}:deny` | **负缓存**（防穿透） | 60 秒 |
| `apex_sales:ai:chat_history:{userId}` | AI 聊天记录（上限 100 条） | 登录态内，登出即删 |
| `{REDIS_TRAN_NO_SEQ_KEY}{TRyyyyMMdd}` | 交易流水号当日序列 | 数小时 |

---

## 附录 B：权限码总表

数据库共定义 **46 个**权限码，代码中实际用到（`@PreAuthorize`）**33 个**。

### B.1 权限码 → 接口映射

| 权限码 | 中文名 | 保护的接口 |
|---|---|---|
| **admin** | 管理员角色标识 | `POST /api/ai/file/upload`、`GET /api/ai/file/download`（注意这是**角色码**不是权限码，写法 `hasAuthority('admin')`） |
| `activity:list` | 市场活动-列表 | `GET /api/activities` |
| `activity:add` | 市场活动-录入 | `POST /api/activities` |
| `activity:edit` | 市场活动-编辑 | `PUT /api/activities/{id}`、备注的增/改 |
| `activity:view` | 市场活动-查看 | `GET /api/activities/{id}/remarks` |
| `activity:delete` | 市场活动-删除 | `DELETE /api/activities/{id}`、`POST /api/activities/batch-delete`、`DELETE /api/activities/remarks/{id}` |
| `activity:search` | 市场活动-搜索 | ⚪ **仅前端按钮控制**，后端接口未用（筛选走 `activity:list`） |
| `clue:list` | 线索管理-列表 | `GET /api/clues` |
| `clue:add` | 线索管理-录入 | `POST /api/clue`；`GET /api/clue/{value}`（或 `clue:edit`） |
| `clue:edit` | 线索管理-编辑 | `PUT /api/clue`、`POST /api/clue/customer`（转客户）、跟进记录增/改；`GET /api/clue/{value}`（或 `clue:add`） |
| `clue:view` | 线索管理-查看 | `GET /api/clue/detail/{id}`、`GET /api/clue/remark` |
| `clue:delete` | 线索管理-删除 | `DELETE /api/clue/{id}`、`DELETE /api/clue/batch`、`DELETE /api/clue/remark/delete/{id}` |
| `clue:import` | 线索管理-导入 | `POST /api/importExcel` |
| `customer:list` | 客户管理-列表 | `GET /api/customers` |
| `customer:view` | 客户管理-查看 | `GET /api/customer/{id}` |
| `customer:export` | 客户管理-导出 | `GET /api/exportExcel` |
| `customer:delete` | 客户管理-删除 | `DELETE /api/customer/{id}` |
| `tran:list` | 交易管理-列表 | `GET /api/trans`、`GET /api/customer/{customerId}/trans` |
| `tran:add` | 交易管理-新建 | `POST /api/trans`；`GET /api/customer/options`（或 `tran:edit`） |
| `tran:edit` | 交易管理-编辑 | `PUT /api/trans`、`POST /api/tran/stage`、跟进记录增/改；`GET /api/customer/options`（或 `tran:add`） |
| `tran:view` | 交易管理-查看 | `GET /api/tran/{id}`、`/history`、`GET /api/tran/remark` |
| `tran:delete` | 交易管理-删除 | `DELETE /api/tran/{id}`、`/batch`、`/remark/{id}` |
| `product:list` | 产品管理-列表 | `GET /api/products` |
| `product:add` | 产品管理-录入 | `POST /api/products` |
| `product:edit` | 产品管理-编辑 | `PUT /api/products/{id}` |
| `product:delete` | 产品管理-删除 | `DELETE /api/products/{id}`、`POST /api/products/batch-delete` |
| `product:view` | 产品管理-查看 | ⚪ 后端未用（详情接口不存在） |
| `dictype:list` | 字典类型-列表 | `GET /api/dictypes`、`/api/dictypes/all`（或 `dicvalue:list`） |
| `dictype:add` | 字典类型-录入 | `POST /api/dictypes` |
| `dictype:edit` | 字典类型-编辑 | `PUT /api/dictypes/{id}` |
| `dictype:delete` | 字典类型-删除 | `DELETE /api/dictypes/{id}` |
| `dictype:view` | 字典类型-查看 | ⚪ 后端未用 |
| `dicvalue:list` | 字典数据-列表 | `GET /api/dicvalues`、`/api/dictypes/all`（或 `dictype:list`） |
| `dicvalue:add` | 字典数据-录入 | `POST /api/dicvalues` |
| `dicvalue:edit` | 字典数据-编辑 | `PUT /api/dicvalues/{id}` |
| `dicvalue:delete` | 字典数据-删除 | `DELETE /api/dicvalues/{id}` |
| `dicvalue:view` | 字典数据-查看 | ⚪ 后端未用 |
| `user:list` | 用户管理-列表 | `GET /api/users` |
| `user:add` | 用户管理-录入 | `POST /api/user`；`GET /api/roles`（或 `user:edit`） |
| `user:edit` | 用户管理-编辑 | `PUT /api/user`；`GET /api/roles`（或 `user:add`） |
| `user:view` | 用户管理-查看 | `GET /api/user/{id}` |
| `user:delete` | 用户管理-删除 | `DELETE /api/user/{id}`、`DELETE /api/user` |
| `system:view` | 系统设置-查看 | `GET /api/system/info` |
| `system:edit` | 系统设置-编辑 | `PUT /api/system/info` |
| `system:add` / `system:list` / `system:delete` | 系统设置-其他 | ⚪ 后端未用 |

### B.2 无需任何权限码的接口（仅需登录）

| 接口 | 说明 |
|---|---|
| `GET /api/login/info` | 本人信息 |
| `GET /api/login/free` | 免登录探活 |
| `GET /api/owner` | 负责人下拉（多处复用） |
| `PUT /api/profile` | 本人资料 |
| `PUT /api/profile/password` | 本人密码 |
| `GET /api/dicvalue/{typeCode}` | 字典下拉（本地缓存，全员高频） |
| `GET /api/summary/data` 等 4 个统计接口 | 首页仪表盘 |
| `GET /api/ai/profile` / `stream-chat` / `reset` / `history` | AI 会话 |
| `POST /api/ai/payment/*`、`GET /api/ai/payment/orders` | AI 付费（付费能力的开通状态在**工具层**判定，不在接口层） |

### B.3 完全免登录（公开）

```
POST /api/login               登录
POST /api/register            注册
GET  /api/password/reset/code 忘记密码-发验证码
PUT  /api/password/reset      忘记密码-重置
GET  /api/system/info/public  公开系统信息（登录页需要 Logo/标题/主题/维护公告）
```

---

## 附录 C：curl 联调速查

### C.1 准备工作

**Windows PowerShell**（注意用 `curl.exe`，因为 PowerShell 的 `curl` 是 `Invoke-WebRequest` 的别名，语法完全不同）

```powershell
# 1. 登录并取出 JWT（-d 表示表单字段）
# -c cookies.txt 把响应里的 Set-Cookie（Refresh Cookie）存进文件，后面刷新要用
$resp = curl.exe -s -c cookies.txt -X POST "http://localhost:8089/api/login" `
  -H "Content-Type: application/x-www-form-urlencoded" `
  -d "loginAct=admin" -d "loginPwd=admin123" -d "rememberMe=false"
$resp                      # 看到 {"code":200,"msg":"成功","data":"eyJ..."}

# 2. 手动把 data 里的字符串存进变量
$TOKEN = "粘贴上面 data 的完整字符串"

# 3. 带 token 查询（-H 指定请求头；注意【没有】Bearer 前缀）
curl.exe -s "http://localhost:8089/api/clues?current=1" -H "Authorization: $TOKEN"
```

> 💡 Access Token **不再存放在浏览器存储里**（只在 JS 内存中），F12 的 Application 面板看不到。拿令牌的方式只有两种：调登录接口时从响应体取；或用 Postman，登录响应的 `data` 手动复制进环境变量。Access 只有 15 分钟有效期，过期后返回 `906`，再调一次刷新接口（见 C.3）即可，不需要重新登录。

**macOS / Linux / Git Bash**
```bash
TOKEN=$(curl -s -c cookies.txt -X POST "http://localhost:8089/api/login" \
  -d "loginAct=admin&loginPwd=admin123" | sed -E 's/.*"data":"([^"]+)".*/\1/')
echo $TOKEN
curl -s "http://localhost:8089/api/clues?current=1" -H "Authorization: $TOKEN"
```

### C.2 三种格式的模板（选错必报错）

**① GET 查询串**
```bash
curl "http://localhost:8089/api/clues?current=1&fullName=张&source=16" -H "Authorization: $TOKEN"
```

**② POST 表单**（本项目所有"新增"接口）—— **不要加 `-d '...'` 的 JSON 写法**
```bash
curl -X POST "http://localhost:8089/api/products" -H "Authorization: $TOKEN" \
  -d "name=奥迪A5&guidePriceS=298000&guidePriceE=428000&quotation=276000&state=1"
# curl 的 -d 默认就是 application/x-www-form-urlencoded，无需额外指定 Content-Type
```

**③ JSON**（所有 PUT 编辑接口 + 付费下单）—— **必须显式加 `-H "Content-Type: application/json"`**
```bash
curl -X PUT "http://localhost:8089/api/products/5" -H "Authorization: $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"name":"奥迪A5改款","quotation":279000,"state":1}'
```

**④ 文件上传**（`-F` 即 multipart，字段名必须是 `file`）
```bash
curl -X POST "http://localhost:8089/api/ai/file/upload" -H "Authorization: $TOKEN" \
  -F "file=@C:/path/to/users.xlsx"
```

### C.3 特殊场景

**刷新令牌**（Access 过期返回 `906` 时；`-b` 带上 Cookie，`-c` 保存刷新后新的 Cookie，因为每次刷新 Refresh 都会换新）
```bash
TOKEN=$(curl -s -b cookies.txt -c cookies.txt -X POST "http://localhost:8089/api/auth/refresh" \
  | sed -E 's/.*"data":"([^"]+)".*/\1/')
```
> 如果忘了 `-c cookies.txt`，下一次刷新用的就是已经作废的旧 Refresh，会被判定为重放（`904`），整个会话被销毁，必须重新登录。

**SSE 流式对话**（`-N` = `--no-buffer`，**不加它就看不到打字机效果**，会等全部返回才一次性打印；令牌走请求头）
```bash
curl -N "http://localhost:8089/api/ai/stream-chat?message=查询我最近的订单&memoryId=AI-BIZ-1" -H "Authorization: $TOKEN"
```

**文件下载**（令牌走请求头；`-OJ` 按 Content-Disposition 的文件名保存）
```bash
curl -OJ "http://localhost:8089/api/exportExcel" -H "Authorization: $TOKEN"
curl -OJ "http://localhost:8089/api/ai/file/download?fileName=用户数据导出_20261018153012.xlsx" -H "Authorization: $TOKEN"
```
> PowerShell 下中文文件名建议用 `encodeURIComponent` 后的形式，否则可能因编码问题收到 `400`。

**靠 Cookie 退出**（不需要 Access Token）
```bash
curl -s -b cookies.txt -c cookies.txt -X POST "http://localhost:8089/api/auth/logout"
```

**批量删除（两种风格并存，别记错）**
```bash
# 逗号分隔字符串走查询串（线索/交易/用户）
curl -X DELETE "http://localhost:8089/api/clue/batch?ids=1,5,9" -H "Authorization: $TOKEN"

# JSON 数组走 POST（活动/产品）
curl -X POST "http://localhost:8089/api/products/batch-delete" \
  -H "Authorization: $TOKEN" -H "Content-Type: application/json" -d '{"ids":[2,4]}'
```

### C.4 常见报错速查

| 报错 | 原因 | 解法 |
|---|---|---|
| `415 Unsupported Media Type` | 给表单接口发了 JSON，或反之 | 对照 §1.3 与每个接口标注的「格式」 |
| `400 Required request body is missing` | PUT JSON 接口忘了加 `-d` | 补上 body |
| `400 Required request parameter 'x' is not present` | 少传了必填查询参数 | 按文档补齐 |
| `400 Required request part 'file' is not present` | multipart 字段名不是 `file` | `-F "file=@..."` |
| `500 参数「ids」格式不正确` | `ids` 传了非数字 | 改成 `1,5,9` |
| `code=902 请求Token有误` | token 加了 `Bearer ` 前缀，或被 shell 变量截断 | 去掉前缀；PowerShell 里用 `"$TOKEN"` 双引号 |
| `code=906 Access Token 已过期，需要刷新` | Access 只有 15 分钟有效期 | 调 `POST /api/auth/refresh`（见 C.3），不需要重新登录 |
| `code=903 请求Token已过期` | 会话已不存在：已退出、Refresh 过期、改过密码、被禁用、被管理员踢下线 | 重新登录 |
| `code=904 请求Token不匹配` | Refresh 被重放：用了一张已经轮换掉的旧 Cookie（常见于 curl 忘了 `-c` 保存新 Cookie） | 重新登录 |
| `code=905 账号已在其他设备登录` | 同账号在别处登录过（Redis 只留最新 token） | 重新登录，或用另一个账号 |
| `code=500 权限不足` | 缺对应权限码，或**角色非 admin** | admin 拥有全部按钮权限，测试时优先用 admin 账号 |

---

## 附录 D：已知不一致与待改进项

> 这一节是**审计源码后如实记录**的问题清单。面试时主动说出来（"我知道哪里不好、为什么这么取舍、怎么改"）比声称"没问题"可信度高得多。

### D.1 路由书写瑕疵（不影响功能，Spring 自动补全）

| 位置 | 问题 | 影响 |
|---|---|---|
| `UserController.java:101` | `@GetMapping("api/user/{id}")` **缺前导 `/`** | 无（Spring 自动补全） |
| `ActivityController.java:101,113` | `@GetMapping("api/activities/{id}/remarks")`、`@PostMapping(...)` **缺前导 `/`** | 无 |

建议统一加上 `/`，与其余接口保持一致，避免后续维护者误以为路径有前缀差异。

### D.2 URL 风格不统一

| 同类功能 | 线索模块 | 交易模块 | 活动模块 |
|---|---|---|---|
| 查详情 | `/api/clue/detail/{id}`（多了 `detail`，因为 `/api/clue/{value}` 被查重占用） | `/api/tran/{id}` | — |
| 编辑备注 | `PUT /api/clue/remark/update`（多了 `/update`） | `PUT /api/tran/remark` | `PUT /api/activities/remarks/{remarkId}` |
| 删备注 | `DELETE /api/clue/remark/delete/{id}`（多了 `/delete`） | `DELETE /api/tran/remark/{id}` | `DELETE /api/activities/remarks/{remarkId}` |
| 批量删除 | `DELETE /api/clue/batch?ids=1,2`（查询串） | `DELETE /api/tran/batch?ids=1,2` | `POST /api/activities/batch-delete`（JSON） |

三个模块各自演化，没有统一 REST 规范。改进方向：新增接口一律遵循 `/api/{资源复数}` + 标准方法语义，批量操作用 `POST /api/{资源}/batch-delete` + JSON body（活动/产品的做法更现代，建议向它对齐）。

### D.3 字段名不对称（活动模块最严重）

- 请求传 `budget`，响应返回 `cost`，筛选参数又叫 `budget`（语义是 cost 下限）。三个语境三种名字，联调极易踩坑。
- `TActivity.ownerDo`（小写 o）vs 其他模块的 `ownerDO`（大写 O）——JSON 键名不一致，前端复制粘贴代码会取到 undefined。

### D.4 文案与实现不符

`generateResetCode()` 返回 `"核验验证码已发出，请注意查看（本地联调可在终端/后台直接复制）"` —— 但项目**根本没配 SMTP**，验证码只进日志，真实环境用户永远收不到邮件。要么补邮件服务，要么把文案改成"验证码请联系管理员获取"。

同理 `resetPassword` 的成功文案 `"恭喜您已在 Service 事务保护下成功重设密码！"` 把内部实现细节（Service 事务）暴露给了用户，用户看不懂。这两处像是**开发过程中写给 AI 看的注释混进了用户文案**。

### D.5 字典种子数据的两处隐患

**① `stage` 字典 `order` 断号（缺 2）**
序列是 `1,3,4,5,6,7`。而阶段推进校验 `|Δorder| == 1`，意味着从「01创建交易」(1) 推进到「02确认清单」(3) 会被判为"只能变更为相邻阶段"而拒绝。**第一步就卡死**。
> 若实测发现交易无法从第一阶段推进，请检查运行库 `t_dic_value` 里是否存在 `order=2` 的 stage 记录（可能已被修正）。修复方式：把 6 条 stage 的 order 重排为连续的 `1~6`。

**② 角色码 `marketing ` 带尾部空格**
`id=4` 的角色 `role` 值是 `'marketing '`（结尾一个空格）。Spring Security 的 authority 是**精确字符串匹配**，用户若被授予该角色，`hasAuthority('marketing')` 恒为 false。建议 `UPDATE t_role SET role='marketing' WHERE id=4`。

### D.6 安全层面的待改进项

| 项 | 现状 | 风险 / 改进方向 |
|---|---|---|
| **CORS 全开** | `setAllowedOrigins(Arrays.asList("*"))` | 前端已改为同源，这条配置现在不再需要。它没有开 `allowCredentials`，Cookie 本来就带不过去，所以不是漏洞，只是多余；确认没有跨域直连后端的场景后，收窄为域名白名单或直接去掉 |
| ~~token 走 URL~~ | 已修复：`exportExcel`、`ai/stream-chat`、`ai/file/download` 三处全部改成请求头，`TokenVerifyFilter` 里没有从 URL 取令牌的代码 | — |
| **`PUT /api/system/info` 直接绑定实体** | `@RequestBody TSystemInfo`，无字段白名单 | 对比 `/api/profile` 的显式白名单做法。前端理论上可篡改 `createBy` 等审计字段。建议改用 DTO |
| **`@DataScope` 只挂在 2 个 Mapper** | 活动分页、用户分页 | 线索、客户、交易列表**不做数据隔离**，普通销售可见全量客户数据（含手机号）。若业务要求"销售只看自己的客户"，需补挂 |
| **无接口限流** | 除忘记密码发码有 60 秒限流 | AI 对话接口调用大模型**有真实成本**，无限流意味着单用户可刷爆 token 额度。建议加令牌桶（Redis + Lua）或 Sentinel |
| **`/api/dicvalue/{typeCode}` 本地缓存不刷新** | 数据来自启动时加载的 `ServerApplication.cacheMap` | 字典管理页改完值，下拉接口不生效（要重启或等定时任务）。这是"本地缓存换性能"的取舍，需在管理页面给操作者明确提示 |

### D.7 AI 模块的架构级短板

| 项 | 现状 | 改进方向 |
|---|---|---|
| **会话记忆存进程内 Map** | `ConcurrentHashMap<String, ChatMemory> memoryCache` | ① 服务重启记忆全丢；② **多实例部署时请求打到不同机器会拿到不同记忆**（会话不连续）。应外置到 Redis |
| **附件暂存同样在进程内存** | `sessionAttachmentCache` | 同上，且 Base64 存内存，大附件会占堆 |
| ~~SSE 结束判定是启发式~~ | 已修复：改用 `fetch` + `ReadableStream`，读到流结束（`done`）就是正常结束，网络断开抛错走 `onError`，不再靠 1500ms 时间差猜 | `DONE_MARKER = "[DONE]"` 的兼容逻辑仍保留，服务端并未推这一帧，可忽略 |
| **`onMessage` 里滚动滞后一拍** | `scrollToBottom()` 没等 `nextTick` | 每个 chunk 用的是上一个 chunk 渲染完的高度。因增量极小肉眼无感，属可接受取舍。严谨做法：`watch(messages, scrollToBottom, {flush:'post'})` |
| **无并发控制** | `loading` 只是前端标志 | 同一用户开两个标签页同时发消息，会并发写同一个 `ChatMemory`（key 相同）；`MessageWindowChatMemory` 非线程安全，可能消息错乱。应在服务端按 memoryId 加锁或用串行队列 |
| **付费订单号有重号概率** | `AI` + 时间戳 + `ThreadLocalRandom` 4 位随机 | 源码注释自己写了"单机演示足够，**生产应接入发号器**"。建议改用 Redis INCR（与交易流水号同一套范式） |
| **文件产物在 `target/` 下** | `Constants.AI_EXPORT_DIR = "target/ai-export"` | `mvn clean` 会清空，界面上残留的下载卡片全部 404。生产应放工作目录外（如 `/data/dlyk/ai-export`） |
| **提示词是软约束** | 增强消息里写"你是管理员/普通用户，能做 xxx" | 模型可能被诱导绕过。**真正的硬闸门是两处**：① 工具包按角色物理隔离（普通用户会话里根本不存在管理员工具）② 工具层的付费墙 + 角色断言。软约束只负责让回答更贴合身份，不承担安全职责 |

### D.8 登录认证模块：已修复与仍待改进（2026-11 审查）

**已修复**

| 编号 | 问题 | 修复 |
|---|---|---|
| B1 | 登录锁定可被大小写或首尾空格绕过 | 失败计数 key 与锁 key 统一用 `Constants.normalizeLoginAct` 归一化 |
| B2 | 改密码、找回密码后旧会话仍有效 | 密码写库成功后删除 Redis 会话 |
| B3 | 刷新接口非原子，并发刷新可互相踢下线 | Lua 脚本 CAS，失败后重新读状态再决定 `903` / `905` / `904` |
| B4 | 禁用、锁定账号后对方最长还能用 15 分钟 | `updateUser` 里这几类变化发生时立即删会话 |
| B5 | 强制退出清不掉 HttpOnly Cookie，回登录页被自动登录 | 新增 `POST /api/auth/logout`；前端退出流程补 try/catch |
| I1 | 登录后总是去工作台，不回跳原页面 | `resolveRedirect()`，带开放重定向校验 |
| I2 | 失败计数器可能永不过期 | TTL 为 `-1` 时补过期时间 |
| I4 | "最近登录时间"其实是"最近打开页面时间" | 移到登录成功处理器 |
| I6 | 验证码用 `Math.random`；错误计数并发可多试 | `SecureRandom`；原子 `INCR` |
| I9 | 输错一次密码就打一份完整堆栈 | 降为 `warn` 且不带堆栈 |
| I11 | 管理员手动锁定时提示"30 分钟后再试" | 按异常类型给出准确文案 |

**仍待改进**

| 编号 | 问题 | 为什么还没做 |
|---|---|---|
| I3 | 会话没有绝对过期：每次刷新都把 Redis TTL 重置满，"记住我"的人只要每 7 天内活跃一次就永远不用重新输密码 | 要先定产品规则：最长连续登录几天 |
| I5 | 每个请求（含 5 秒心跳）查 2 次库装载权限 | 要设计 Redis 缓存及失效策略（改角色时删缓存） |
| I7 | `allowedOrigins("*")` 已经不需要 | 要确认没有跨域直连后端的场景 |
| I12 | 登录接口没有 CSRF 防护（"登录 CSRF"） | 校验 `Origin` 请求头依赖部署域名 |
| — | 验证码明文写进日志 | 本地没有邮件服务，联调要靠它；上线必须接邮件或短信 |

### D.9 建议的下一步动作（按性价比排序）

| 优先级 | 事项 | 理由 |
|---|---|---|
| 🔴 高 | 修 `stage` 字典 order 断号 | 直接阻断核心业务（交易阶段无法推进） |
| 🔴 高 | 修 `marketing ` 尾部空格 | 该角色完全拿不到权限 |
| 🟡 中 | SSE 补推 `[DONE]` 哨兵帧 | 前端已有兼容逻辑，服务端加一行即可把"猜"变成"确定" |
| 🟡 中 | 统一批量删除风格为 `POST {资源}/batch-delete` + JSON | 消除前端心智负担 |
| 🟡 中 | AI 对话加限流 | 控成本 + 防滥用 |
| 🟢 低 | 补前导斜杠、统一 `ownerDO` 大小写、改文案 | 代码整洁度，无功能影响 |

---

> **文档维护约定**：新增/修改接口时，请同步更新本文档对应小节（路径、格式、参数、鉴权、响应示例、错误码）。
> 本文档由源码逆向生成，若发现与实际行为不符，**以源码为准**并请修正本文档。

