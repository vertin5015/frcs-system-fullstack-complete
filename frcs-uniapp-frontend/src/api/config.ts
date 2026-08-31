// API 基础地址：微信小程序端直连后端（无 devServer 代理，也不存在浏览器跨域）
// 开发/生产可通过 .env 中 VITE_API_BASE_URL 覆盖；默认指向已部署服务器（nginx 80 -> backend:8122）
const envBase = (import.meta.env?.VITE_API_BASE_URL as string | undefined)?.trim() || ''

export const API_BASE_URL = (envBase || 'http://120.26.60.104/api').replace(/\/+$/, '')

export const API_TIMEOUT = {
  normal: 30000,
  search: 150000,
  summaryStart: 120000,
  summaryStatus: 60000,
  qa: 120000,
  agent: 180000,
}
