// API 基础地址：微信小程序端直连后端（无 devServer 代理，也不存在浏览器跨域）
// 开发/生产可通过 .env 中 VITE_API_BASE_URL 覆盖；默认指向正式部署域名
// （https://law3.hexilab.cn/api -> 宿主机 Nginx -> 127.0.0.1:8003 后端容器）。
// 微信小程序生产环境强制 HTTPS，且需要在「小程序后台-开发管理-服务器域名」里配置该域名。
const envBase = (import.meta.env?.VITE_API_BASE_URL as string | undefined)?.trim() || ''

export const API_BASE_URL = (envBase || 'https://law3.hexilab.cn/api').replace(/\/+$/, '')

export const API_TIMEOUT = {
  normal: 30000,
  search: 150000,
  summaryStart: 120000,
  summaryStatus: 60000,
  qa: 120000,
  agent: 180000,
}
