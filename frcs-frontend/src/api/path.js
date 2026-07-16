// 开发：.env.development 里 VUE_APP_BASE_URL 留空 → 走 vue.config.js 代理，请求同域 /api，无跨域
// 生产：.env.production 填实际后端根地址（如 http://your-host:8122）
const trimmed = String(process.env.VUE_APP_BASE_URL ?? '').trim();
const base = {
  baseUrl: trimmed !== '' ? trimmed : '',
};

const paths = {
  login: '/api/login',
  register: '/api/register',
  sendAuthCode: '/api/auth/send-code',
  loginByCode: '/api/auth/login-by-code',
  resetPassword: '/api/auth/reset-password',
  changePassword: '/api/auth/change-password',
  userSummaryCredits: '/api/user/summaryCredits',
  paymentPackages: '/api/payment/packages',
  paymentChannels: '/api/payment/channels',
  paymentOrder: '/api/payment/order',
  paymentMockConfirm: '/api/payment/mock/confirm',
  collectionList: '/api/cases/favorites',
  searchCases: '/api/cases/search',
  caseMeta: '/api/cases/meta',
  caseQa: '/api/cases/qa',
  getCaseSummary: '/api/cases/aisummary',
  startSummaryAsync: '/api/cases/summaryAsync/start',
  summaryAsyncStatus: '/api/cases/summaryAsync/status',
  favoriteCase: '/api/favorite/add',
  cancelFavoriteCase: '/api/favorite/delete',
  getFavoriteCases: '/api/cases/favorites',
  getHistoryCases: '/api/history/browse_history',
  kbIngest: '/api/kb/ingest',
  kbIngestCrawler: '/api/kb/ingest-crawler',
  kbQuery: '/api/kb/query',
  agentAsk: '/api/agent/ask',
};
export { base, paths };
