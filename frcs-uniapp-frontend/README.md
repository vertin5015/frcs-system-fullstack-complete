# 涉外案例查询与法律知识系统（uni-app 微信小程序端）

Vue 3 + TypeScript + uni-app 微信小程序前端，使用 Composition API、Pinia、SCSS，已对接已部署的后端服务
（`http://120.26.60.104/api`，nginx 反代到 backend:8122）。

## 已对接功能（与网页端一致）

- 登录注册：邮箱密码登录、邮箱验证码登录、注册、忘记/重置密码、修改密码、游客模式（本地 userId=0）
- 案例检索：`/api/cases/search`（关键词、国家、判决时间、数据源筛选 + 分页）
- 案例详情：`/api/cases/meta` + 异步 AI 摘要（`summaryAsync/start` → `summaryAsync/status` 轮询）+ 本案问答（`/api/cases/qa`）+ 收藏/取消收藏 + 原文代理（`/api/cases/original-proxy` web-view）
- 收藏夹：`/api/cases/favorites`（国家/时间筛选 + 分页 + 取消收藏）
- 浏览历史：`/api/history/browse_history`（一周内/一周外分组 + 分页）
- 海外法律知识（学法）：`/api/kb/query` 知识库问答 + 命中条文详情 + AI 解读（`/api/agent/ask`）
- AI 法律助手：`/api/agent/ask` RAG 对话（相关案例、知识库命中、执行轨迹）
- 个人中心：AI 摘要剩余次数（`/api/user/summaryCredits`）+ 购买次数（`/api/payment/*` 套餐下单 + Mock 支付确认）

## 环境与后端地址

- `.env.development` / `.env.production` 中的 `VITE_API_BASE_URL` 决定后端根地址，默认 `http://120.26.60.104/api`
- 微信开发者工具已关闭 url 校验（`manifest.json` 的 `mp-weixin.setting.urlCheck=false`），http 可直接访问
- 上线发布需在微信公众平台配置 request/web-view 合法域名（https 且需 ICP 备案）

## 启动

```bash
npm install
npm run dev:mp-weixin   # 产物在 dist/dev/mp-weixin，用微信开发者工具导入
npm run type-check      # 类型检查
npm run build:mp-weixin # 生产构建
```

## 代码结构

- `src/api/config.ts`：API 基础地址与超时配置
- `src/api/request.ts`：uni.request 封装（返回后端统一 JSONReturnBean）
- `src/api/index.ts`：全部后端接口（与网页端 `src/api/index.js` 一一对应）
- `src/store/user.ts`：登录态（token="true" + userId/username/summaryCredits，与网页端 authStorage 对齐）
