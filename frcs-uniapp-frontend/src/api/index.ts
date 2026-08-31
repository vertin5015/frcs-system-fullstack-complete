import { httpGet, httpPost, httpDelete } from './request'
import { API_TIMEOUT } from './config'
import type {
  AgentResult,
  CaseBaseInfo,
  FavoriteListResult,
  HistoryListResult,
  KbHit,
  KbQueryResult,
  SearchResult,
  SummaryStatus,
} from '../types/case'
import type { CreateOrderResult, LoginResult, PaymentPackage } from '../types/user'

/** 与网页端 src/api/path.js 保持一致的全部后端路径 */
export const paths = {
  login: '/login',
  register: '/register',
  sendAuthCode: '/auth/send-code',
  loginByCode: '/auth/login-by-code',
  resetPassword: '/auth/reset-password',
  changePassword: '/auth/change-password',
  userSummaryCredits: '/user/summaryCredits',
  paymentPackages: '/payment/packages',
  paymentChannels: '/payment/channels',
  paymentOrder: '/payment/order',
  paymentMockConfirm: '/payment/mock/confirm',
  searchCases: '/cases/search',
  caseMeta: '/cases/meta',
  caseQa: '/cases/qa',
  getCaseSummary: '/cases/aisummary',
  startSummaryAsync: '/cases/summaryAsync/start',
  summaryAsyncStatus: '/cases/summaryAsync/status',
  favoriteCase: '/favorite/add',
  cancelFavoriteCase: '/favorite/delete',
  getFavoriteCases: '/cases/favorites',
  getHistoryCases: '/history/browse_history',
  kbIngest: '/kb/ingest',
  kbIngestCrawler: '/kb/ingest-crawler',
  kbQuery: '/kb/query',
  agentAsk: '/agent/ask',
}

// ==================== 登录注册 / 认证 ====================
const login = (email: string, password: string) => httpPost<LoginResult>(paths.login, { email, password })

const register = (username: string, email: string, password: string) =>
  httpPost<string>(paths.register, { username, email, password })

const sendAuthCode = (email: string, purpose: 'LOGIN' | 'RESET') =>
  httpPost<{ cooldownSeconds: number; devCode?: string }>(paths.sendAuthCode, { email, purpose })

const loginByCode = (email: string, code: string) => httpPost<LoginResult>(paths.loginByCode, { email, code })

const resetPasswordByCode = (email: string, code: string, newPassword: string) =>
  httpPost<string>(paths.resetPassword, { email, code, newPassword })

const changePasswordApi = (email: string, oldPassword: string, newPassword: string) =>
  httpPost<string>(paths.changePassword, { email, oldPassword, newPassword })

// ==================== 案例 ====================
const searchCases = (params: Record<string, unknown>) =>
  httpGet<SearchResult>(paths.searchCases, params, API_TIMEOUT.search)

const getCaseMeta = (caseId: string, language: string, userId: number) =>
  httpGet<CaseBaseInfo>(paths.caseMeta, { caseId, language, userId }, API_TIMEOUT.normal)

const postCaseQa = (caseId: string, question: string, language: string, userId: number) =>
  httpPost<string>(paths.caseQa, { caseId, question, language, userId }, API_TIMEOUT.qa)

const getCaseSummary = (caseId: string, language: string, userId: number) =>
  httpGet<string>(paths.getCaseSummary, { caseId, language, userId }, API_TIMEOUT.qa)

const startSummaryAsync = (caseId: string, language: string, userId: number, force = false) =>
  httpGet<SummaryStatus>(paths.startSummaryAsync, { caseId, language, userId, force }, API_TIMEOUT.summaryStart)

const getSummaryAsyncStatus = (caseId: string, language: string) =>
  httpGet<SummaryStatus>(paths.summaryAsyncStatus, { caseId, language }, API_TIMEOUT.summaryStatus)

// ==================== 用户权益 ====================
const getUserSummaryCredits = (userId: number) =>
  httpGet<number | null>(paths.userSummaryCredits, { userId }, API_TIMEOUT.normal)

// ==================== 收藏 / 历史 ====================
const favoriteCase = (caseId: string, userId: number) =>
  httpGet<string>(paths.favoriteCase, { caseId, userId }, API_TIMEOUT.normal)

const cancelFavoriteCase = (caseId: string, userId: number) =>
  httpDelete<string>(paths.cancelFavoriteCase, { caseId, userId }, API_TIMEOUT.normal)

const getFavoriteCases = (params: Record<string, unknown>) =>
  httpGet<FavoriteListResult>(paths.getFavoriteCases, params, API_TIMEOUT.normal)

const getHistoryCases = (params: Record<string, unknown>) =>
  httpGet<HistoryListResult>(paths.getHistoryCases, params, API_TIMEOUT.normal)

// ==================== 支付 ====================
const getPaymentPackages = (language = 'zh') =>
  httpGet<PaymentPackage[]>(paths.paymentPackages, { language }, API_TIMEOUT.normal)

const getPaymentChannels = () => httpGet<Record<string, unknown>>(paths.paymentChannels, {}, API_TIMEOUT.normal)

const createPaymentOrder = (userId: number, packageId: string) =>
  httpPost<CreateOrderResult>(paths.paymentOrder, { userId, packageId }, API_TIMEOUT.normal)

const confirmMockPayment = (userId: number, orderNo: string) =>
  httpPost<string>(paths.paymentMockConfirm, { userId, orderNo }, API_TIMEOUT.normal)

// ==================== 知识库 / Agent ====================
const kbQuery = (payload: { question: string; language?: string; topK?: number }) =>
  httpPost<KbQueryResult>(paths.kbQuery, payload, API_TIMEOUT.agent)

const kbIngest = (payload: Record<string, unknown>) => httpPost<string>(paths.kbIngest, payload, API_TIMEOUT.agent)

const kbIngestCrawler = (payload: Record<string, unknown>) =>
  httpPost<string>(paths.kbIngestCrawler, payload, API_TIMEOUT.agent)

const agentAsk = (payload: Record<string, unknown>) =>
  httpPost<AgentResult>(paths.agentAsk, payload, API_TIMEOUT.agent)

export const api = {
  login,
  register,
  sendAuthCode,
  loginByCode,
  resetPasswordByCode,
  changePasswordApi,
  searchCases,
  getCaseMeta,
  postCaseQa,
  getCaseSummary,
  startSummaryAsync,
  getSummaryAsyncStatus,
  getUserSummaryCredits,
  favoriteCase,
  cancelFavoriteCase,
  getFavoriteCases,
  getHistoryCases,
  getPaymentPackages,
  getPaymentChannels,
  createPaymentOrder,
  confirmMockPayment,
  kbQuery,
  kbIngest,
  kbIngestCrawler,
  agentAsk,
}

export default api

export type { KbHit }
