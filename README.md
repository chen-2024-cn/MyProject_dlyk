# 动力云课示范 (dlyk)

面向销售团队的 **CRM（客户关系管理）系统**，覆盖线索获取、跟进、转化到交易达成的完整销售流程。

## 技术栈

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | ^3.5.29 | 前端框架（Composition API） |
| Vue Router | ^5.0.3 | 路由管理 |
| Element Plus | ^2.13.5 | UI 组件库 |
| Axios | ^1.13.6 | HTTP 客户端（JWT 拦截器） |
| ECharts | ^6.1.0 | 数据可视化 |
| Vite | ^7.3.1 | 构建工具 |

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.9 | 应用框架 |
| Spring Security | 3.5.9 | 认证授权 |
| Spring AI OpenAI | 1.0.0-M6 | AI 对话（DeepSeek） |
| MyBatis | 3.0.3 | ORM |
| MySQL | 8.x | 主数据库 |
| Redis | — | Token 缓存 |
| EasyExcel | 4.0.3 | Excel 导入导出 |
| java-jwt (Auth0) | 4.4.0 | JWT 令牌 |

## 功能模块

- **用户认证与权限** — JWT 登录、记住我、免登录、操作级权限控制（`v-hasPermission` 指令）
- **仪表盘** — 概览卡片 + 销售漏斗图、线索来源饼图、月度趋势折线图
- **线索管理** — CRUD、Excel 批量导入、批量删除、跟踪记录
- **客户管理** — 线索转化、客户列表、Excel 批量导出
- **交易管理** — CRUD、阶段推进/回退、阶段历史、跟踪记录
- **营销活动管理** — CRUD、多条件搜索、活动备注
- **产品管理** — 产品 CRUD、上下架状态
- **字典管理** — 字典类型与字典值的双栏 CRUD
- **系统配置** — 系统信息编辑、开关控制
- **个人中心** — 信息编辑、密码修改、权限查看
- **AI 对话** — 对接 DeepSeek API 的智能辅助

## 项目结构

```
动力云课示范/
├── front/                    # Vue 3 前端
│   └── src/
│       ├── main.js           # 入口
│       ├── router/           # 路由配置
│       ├── http/             # Axios 封装 + JWT 拦截器
│       └── view/             # 页面组件（15+ 页面）
├── server/                   # Spring Boot 后端
│   └── src/main/java/com/cyk/
│       ├── web/              # REST Controller
│       ├── service/          # 业务逻辑层
│       ├── manager/          # 业务编排层
│       ├── mapper/           # MyBatis 数据访问层
│       ├── model/            # 实体类
│       └── config/           # 安全配置 / JWT 过滤器
└── docs/                     # 文档
```

## 快速启动

### 环境要求

- Node.js 20.19+ 或 22.12+
- Java 21
- MySQL 8.x + Redis

### 后端

```bash
cd server
# 修改 application.yml 中的数据库和 Redis 连接配置
mvn spring-boot:run
```

默认端口：`8089`

### 前端

```bash
cd front
npm install
npm run dev
```

## 业务流程图

```
线索（Clue）
  │  导入 / 手动创建
  │  添加跟踪记录
  │
  └──→ 转化为 客户（Customer）
            │
            │  关联 产品（Product）
            │  关联 交易（Tran）
            │
            └──→ 交易阶段推进
                      ├── 初步接触
                      ├── 需求分析
                      ├── 报价
                      ├── 谈判
                      └── 成交
```

## 认证流程

项目根目录包含以下认证流程图：

- `登录流程.png`
- `免登录流程.png`
- `JWT登录流程与Redis认证.png`
