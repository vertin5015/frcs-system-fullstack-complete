# AGENTS.md — 涉外案例查询与法律知识系统

本文件是仓库的快速索引与开发指南，供后续 AI 代理和开发者快速定位代码、理解业务链路并遵守约定。

## 1. 项目定位

软件工程课程实训项目：涉外案例查询与法律知识分析系统（Web 端 + 微信小程序端）。需求来源见 [docs/项目要求说明.md](docs/项目要求说明.md)。

核心建设目标：

- 聚合美国（CourtListener）、欧盟法院、日本裁判所、韩国法务部四大官方数据源（韩国当前未实现）；
- 关键词 + 自然语言双模式检索，多源聚合、去重、排序；
- 合规爬虫：定时增量更新 + 检索兜底抓取 + 异常站点跳过 + 防封禁限流；
- 大模型对涉外判例文书做结构化解析（AI 摘要），并支持查看原始文书；
- 多国法律知识库展示 + RAG 问答助手（Agent）；
- 用户功能：注册登录、收藏、浏览历史、充值额度（AI 摘要次数）。

## 2. 仓库结构（代码索引）

```
├── legal_cases-master/          # Spring Boot 后端（主工程）
│   └── src/main/java/com/hnu/legal_cases/
│       ├── controller/          # REST 接口层（薄，只做参数接收与响应包装）
│       ├── service/             # 业务接口
│       ├── service/impl/        # 业务实现
│       ├── client/              # 数据源客户端（CrawlerClient、fastsource/CourtListener）
│       ├── dao/                 # MyBatis Mapper 接口
│       ├── pojo/                # 实体类（对应数据库表）
│       ├── dto/                 # 请求/响应 VO（按业务分包：cases/ai/crawler/kb/agent/auth/...）
│       ├── enums/               # CountryEnum(US/EU/JPN)、LanguageEnum(zh/en)、PeriodEnum(1/3/5/10年)
│       ├── config/              # 配置类（CrawlerProperties、KbProperties、PaymentProperties、SpringAIConfiguration...）
│       ├── exception/           # ServiceException
│       ├── util/                # JSONReturnBean（统一返回）、DistributedLock（Redis 分布式锁）
│       └── resources/
│           ├── application.yml              # 默认配置（端口 8122，context-path=/api）
│           ├── application-local.yml.example# 本地开发配置模板（复制为 application-local.yml）
│           ├── application-deepseek.yml     # DeepSeek 云模型 profile
│           ├── mapper/*.xml                 # MyBatis SQL
│           └── sql/                         # schema.sql + 增量 SQL（见第 7 节）
├── frcs-frontend/               # Vue 3 Web 前端（Vue CLI + Element Plus）
│   ├── src/views/               # 页面组件（登录/检索/案例阅读/知识库/Agent/收藏/历史/充值）
│   ├── src/api/path.js          # 全部后端接口路径定义（改接口先看这里）
│   ├── src/api/index.js         # 接口封装（axios 调用 + 错误提示）
│   ├── src/utils/request.js     # axios 实例（baseURL 与响应拦截）
│   ├── src/utils/authStorage.js # 登录态（sessionStorage + localStorage 双写）
│   ├── src/router/index.js      # 路由
│   └── nginx.conf / Dockerfile  # 生产部署
├── frcs-uniapp-frontend/        # 微信小程序端（uni-app + Vue3 + TS + Pinia，已对接后端）
│   └── src/
│       ├── pages/               # 登录/首页/案例/条文/学法/收藏/历史/我的/Agent/充值
│       ├── api/                 # config.ts（后端地址）+ request.ts（uni.request 封装）+ index.ts（全部接口，与网页端 api/index.js 对齐）
│       ├── store/               # Pinia（user/app，登录态对齐网页端 authStorage）
│       ├── types/               # TS 类型（对齐后端 DTO）
│       └── pages.json           # 页面与自定义 tabBar 配置
├── docker/                      # docker/mysql/init/01-init.sql（MySQL 初始化）
├── docker-compose.yml           # mysql + redis + backend + frontend 一键编排
├── scripts/                     # deploy.sh（服务器部署）、seed-kb.sh（演示知识库注入）
└── docs/                        # 需求/数据库设计/交付说明等文档
```

## 3. 技术栈

| 端 | 技术 |
| --- | --- |
| 后端 | Java 21、Spring Boot 3.4.7、Spring WebFlux、Spring AI（OpenAI 兼容）、MyBatis + PageHelper、MySQL 8、Redis、Stripe Java、jsoup、Hutool、fastjson |
| Web 前端 | Vue 3.2、Vue CLI 5、Element Plus、Vuex、vue-router、axios、markdown-it/marked、jspdf/html2canvas/mammoth（原文展示/导出） |
| 小程序 | Vue 3 + TypeScript + uni-app（alpha）、Pinia、SCSS、Vite |
| 部署 | Docker Compose（mysql:8.4 / redis:7 / nginx），服务器约 120.26.60.104 |

## 4. 本地开发启动

### 4.1 后端

1. 环境：JDK 21、MySQL、Redis（Mac 见 [docs/MacOS开发与爬虫脚本说明.md](docs/MacOS开发与爬虫脚本说明.md)）。
2. 建库：依次执行 `legal_cases-master/src/main/resources/sql/` 下 `schema.sql` → `alter_extensions_2026.sql` → `recharge_order.sql`（老库再加 `alter_users_summary_credits.sql`）。
3. 复制 `application-local.yml.example` 为 `application-local.yml`，改成本机 MySQL/Redis 配置（不要提交该文件）。
4. 启动：

```bash
cd legal_cases-master
mvn spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=local"
```

- 服务端口 `8122`，所有接口前缀 `/api`（`server.servlet.context-path=/api`）。
- AI 默认走本地 Ollama（`http://127.0.0.1:11434`，模型 `qwen2.5:7b-instruct`）；用 DeepSeek 等云端模型时启用 `application-deepseek.yml` 或设环境变量 `SPRING_AI_OPENAI_API_KEY/BASE_URL/MODEL`（base-url 不要带 `/v1`，Spring AI 会自动拼接）。

### 4.2 Web 前端

```bash
cd frcs-frontend
npm install
npm run serve        # 8080 端口，/api 代理到 http://localhost:8122（见 vue.config.js）
```

`src/api/path.js` 中 `VUE_APP_BASE_URL` 留空时走 devServer 代理；生产构建填后端根地址。

### 4.3 微信小程序

```bash
cd frcs-uniapp-frontend
npm install
npm run dev:mp-weixin   # 产物在 dist/dev/mp-weixin，用微信开发者工具导入
```

小程序端已接入真实后端（默认 `http://120.26.60.104/api`，见 `src/api/config.ts` 与 `.env.*`）：

- 检索：`/api/cases/search`；详情：`/api/cases/meta` + 异步摘要轮询 + `/api/cases/qa`；
- 收藏 `/api/cases/favorites`、历史 `/api/history/browse_history`、额度 `/api/user/summaryCredits`；
- 学法页走 `/api/kb/query`，AI 助手走 `/api/agent/ask`，充值走 `/api/payment/*`（Mock 支付）；
- 原文经 `/api/cases/original-proxy` 用 web-view 展示；上线需配置微信合法域名（https）。

### 4.4 Docker 一键部署

```bash
cp .env.example .env   # 按需改模型 key / 密码
./scripts/deploy.sh            # git pull + 构建 + 启动 + 注入演示知识库
./scripts/deploy.sh --no-pull  # 只重建本地代码
```

容器：`frcs-mysql`(3306) / `frcs-redis`(6379) / `frcs-backend`(8122) / `frcs-frontend`(80)。nginx 将 `/api/` 反代到 `backend:8122`。

## 5. 核心业务链路

### 5.1 案例检索

`GET /api/cases/search` → `SearchCasesServiceImpl.searchCases`：

1. 参数校验（`CheckReqVOService`）→ Redis 分布式锁防止同词并发；
2. 命中 `CaseCacheService` 缓存则直接返回；
3. 未命中：`SpringAIService.extractKeyword` 做自然语言→关键词抽取 → `CrawlerService.startParallelCaseSearch` 并行查询 US/EU/JPN → 超时合并、去重 → 结果写入 `case_en_US`/`case_zh_CN` 并回填缓存。

另有 `GET /api/cases/search-stream`（SSE 边爬边返回）。

### 5.2 AI 摘要（额度消耗）

- 同步：`GET /api/cases/aisummary`；
- 异步：`GET /api/cases/summaryAsync/start` → 轮询 `/summaryAsync/status`（PENDING/RUNNING/DONE/FAILED）；
- 额度：`SummaryQuotaService` 扣 `users.summary_credits`，`usage_log` 记账，每日 Redis 上限可配（`app.quota`）；`app.quota.batch-user-ids` 白名单账号不扣费；
- 原文：`OriginalDocumentCacheService` 缓存原始文书；案例阅读页经 `/api/cases/original-proxy` 代理展示。

### 5.3 法律知识库 + RAG Agent

- 入库：`POST /api/kb/ingest`（手填）、`/api/kb/ingest-crawler`（爬虫结果）、`/api/kb/ingest-db`（数据库回灌）；
- 检索：`POST /api/kb/query`；
- 实现：`LocalKbServiceImpl` 维护本地 JSON 索引文件（`kb-index/local-kb.json`，路径见 `kb.local.index-file`），按 chunk-size=800 切分；`LocalEmbeddingServiceImpl` 用 3-gram 哈希向量（384 维）做余弦相似度，不依赖外部 embedding 服务；
- Agent：`POST /api/agent/ask` → `LegalAgentServiceImpl.ask`：
  - `refreshCases=true` 时先走案例搜索（`search_then_rag`），失败降级为 `search_failed_then_rag`；
  - 再查本地 KB，由 Spring AI 生成答案；
  - 返回 `answer / route / searchTotalCount / kbHitCount / relatedCases / kbHits / trace`。

### 5.4 用户与支付

- 注册/登录：`POST /api/register`、`POST /api/login`；验证码 `POST /api/auth/send-code`、`login-by-code`、`reset-password`、`change-password`；
- 收藏：`GET /api/favorite/add`、`DELETE /api/favorite/delete`、`GET /api/cases/favorites`；
- 历史：`GET /api/history/browse_history`；
- 充值：`GET /api/payment/packages` → `POST /api/payment/order` → `POST /api/payment/mock/confirm`（Mock 支付），Stripe 为骨架（`StripeWebhookController`，默认 disabled）。

## 6. 接口速查（全部前缀 /api）

统一返回体 `JSONReturnBean`：`code=200` 成功 / `code=0` 失败，`data` 为业务数据。后端异常统一抛 `ServiceException`，Controller 捕获后包装。

| 模块 | 路径 | 方法 | 说明 |
| --- | --- | --- | --- |
| 登录注册 | `/login`、`/register` | POST/GET | 邮箱+密码 |
| 认证 | `/auth/send-code`、`/auth/login-by-code`、`/auth/reset-password`、`/auth/change-password` | POST | 邮箱验证码流程 |
| 用户 | `/user/summaryCredits` | GET | 查询摘要额度 |
| 案例 | `/cases/search` | GET | 聚合检索（分页/国家/时间/语言） |
| 案例 | `/cases/search-stream` | GET(SSE) | 流式检索 |
| 案例 | `/cases/meta` | GET | 案例元信息 |
| 案例 | `/cases/aisummary` | GET | 同步 AI 摘要 |
| 案例 | `/cases/summaryAsync/start`、`/status` | GET | 异步摘要（轮询） |
| 案例 | `/cases/qa` | POST | 案例问答 |
| 案例 | `/cases/original-proxy` | GET | 原始文书代理 |
| 案例 | `/cases/favorites` | GET | 收藏列表 |
| 收藏 | `/favorite/add`、`/favorite/delete` | GET/DELETE | 增删收藏 |
| 历史 | `/history/browse_history` | GET | 浏览历史 |
| 知识库 | `/kb/ingest`、`/kb/ingest-crawler`、`/kb/ingest-db`、`/kb/query` | POST | KB 入库/检索 |
| Agent | `/agent/ask` | POST | RAG 问答（见 5.3） |
| 支付 | `/payment/packages`、`/payment/order`、`/payment/mock/confirm`、`/payment/channels` | GET/POST | 套餐/下单/Mock 支付 |
| 支付 | `/payment/stripe/webhook` | POST | Stripe 回调（默认关闭） |
| 管理 | `/admin/grant-credits` | POST | 发额度（请求头 `X-Admin-Secret`，配置 `app.admin.secret`） |
| 爬虫桥 | `/crawler-bridge/list/{US,EU,JPN}/crawl.json`、`/crawler-bridge/detail/crawl.json` | GET | 后端内置爬虫兜底（Docker/本地 8122 自调用） |

## 7. 数据库

设计文档见 [docs/数据库设计文档.md](docs/数据库设计文档.md)。建表脚本在 `legal_cases-master/src/main/resources/sql/`：

| 文件 | 用途 |
| --- | --- |
| `schema.sql` | 全量建库（users、data_sources、case_en_US、case_zh_CN、case_detail_info、user_favorites、browse_history） |
| `alter_extensions_2026.sql` | 增量：case_detail_info 摘要状态列 + usage_log |
| `recharge_order.sql` | 增量：充值订单表 |
| `alter_users_summary_credits.sql` | 增量：users.summary_credits 额度列 |

关键约定：

- 案例主表 `case_en_US`，`case_id` 唯一；`case_zh_CN`、`case_detail_info` 以 `case_id` 外键关联；
- `CountryEnum` 的 sourceId 对应 `data_sources.source_id`（US=0/EU=1/JPN=2）；
- 新 SQL 变更必须新增独立文件放入 `sql/` 并同步更新数据库设计文档，禁止直接改已发布脚本；
- 已知边界：KB 索引在本地 JSON 文件而非 MySQL；密码当前为明文比较；案例日期存字符串。

## 8. 关键配置与开发约定

- 后端环境变量：`SPRING_AI_OPENAI_API_KEY/BASE_URL/MODEL`、`APP_ADMIN_SECRET`、`APP_AUTH_EXPOSE_CODE_IN_RESPONSE`（联调时可置 true 让验证码直接返回）；
- 爬虫配置在 `application.yml` 的 `crawler.*`：详情/列表超时、单条响应上限；外置 Scrapy 端口 9001-9004 为可选项，Docker 与 EU/JPN 默认走后端内置 bridge；
- 额度套餐配置在 `app.payment.packages`（pkg10=10 次/990 分，pkg50=50 次/3990 分）；
- 前端登录态：`utils/authStorage.js` 双写 sessionStorage/localStorage，`token` 值为字符串 `"true"` 即已登录；
- 请求超时：`request.js` axios 默认 30 分钟（摘要耗时），轮询类接口应单独缩短；
- 代码风格：后端 Controller 薄、Service 接口 + Impl 实现、DTO 按业务分包、参数校验统一走 `CheckReqVOService`；前端 API 路径集中在 `src/api/path.js`，页面不直接拼 URL；
- 测试：新增核心链路必须补 JUnit 测试（参考 `legal_cases-master/src/test/` 现有用例）。

## 9. 已知边界与注意事项

- 韩国（KOR）数据源未实现，`CountryEnum` 仅 US/EU/JPN；需求文档中韩国站点异常需做容错，属待办；
- 小程序端已接入真实接口；微信 `web-view`/request 在生产环境需配置合法业务域名（当前开发工具 urlCheck=false）；
- KB 为本地 JSON + 哈希 embedding，非生产级向量库；升级方向是向量数据库 + 独立 `kb_document/kb_chunk/embedding` 表；
- `frcs-frontend/.env.development`、`.env.production` 当前指向服务器地址且工作区有未提交改动，改本机联调配置时注意不要提交误伤；
- `application-local.yml`、`.env` 含本机/服务器配置，不得提交 Git；
- 服务器部署后演示数据由 `scripts/seed-kb.sh` 注入；清空知识库需删除后端容器内 `kb-index/local-kb.json` 后重启。
