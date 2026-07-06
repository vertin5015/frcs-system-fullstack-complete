import instance from "../utils/request";
import { paths } from "./path";
import { ElNotification } from "element-plus";
// 登录（POST JSON，避免密码中的 &、+、# 等在 GET 查询串中被破坏）
const login = async (email, password) => {
  try {
    const response = await instance.post(paths.login, {
      email,
      password,
    });
    console.log("Login response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Login failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

// 注册（POST JSON）
const register = async (username, email, password) => {
  try {
    const response = await instance.post(paths.register, {
      username,
      email,
      password,
    });
    console.log("Register response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Register failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

const sendAuthCode = async (email, purpose) => {
  const response = await instance.post(paths.sendAuthCode, { email, purpose });
  if (response.code === 0) {
    ElNotification({
      title: "操作失败",
      message: response.message,
      type: "error",
    });
  }
  return response;
};

const loginByCode = async (email, code) => {
  try {
    const response = await instance.post(paths.loginByCode, { email, code });
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    ElNotification({
      title: "操作失败",
      message: error.serverMessage || error.message,
      type: "error",
    });
    throw error;
  }
};

const resetPasswordByCode = async (email, code, newPassword) => {
  const response = await instance.post(paths.resetPassword, { email, code, newPassword });
  if (response.code === 0) {
    ElNotification({
      title: "操作失败",
      message: response.message,
      type: "error",
    });
  }
  return response;
};

const changePasswordApi = async (email, oldPassword, newPassword) => {
  const response = await instance.post(paths.changePassword, { email, oldPassword, newPassword });
  if (response.code === 0) {
    ElNotification({
      title: "操作失败",
      message: response.message,
      type: "error",
    });
  }
  return response;
};

// 获取收藏案例列表
const getCollectionList = async (Credential) => {
  try {
    const response = await instance.get(paths.collectionList, Credential);
    console.log("Collection List response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Get Collection List failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

// 案件搜索（sources 可选：逗号分隔 US,EU,JPN）
const getCaseMeta = async (caseId, language, userId) => {
  return await instance.get(paths.caseMeta, {
    params: { caseId, language, userId },
    timeout: 30000,
  });
};

const postCaseQa = async (caseId, question, language, userId) => {
  return await instance.post(paths.caseQa, { caseId, question, language, userId }, { timeout: 120000 });
};

const searchCases = async (keyword, language, country, period, pagenum, pagesize, userId, sources) => {
  try {
    const params = {
      keyword: keyword,
      language: language,
      country: country,
      period: period,
      pagenum: pagenum,
      pagesize: pagesize,
      userId: userId,
    };
    if (sources != null && sources !== "") {
      params.sources = sources;
    }
    const response = await instance.get(paths.searchCases, {
      params,
    });
    console.log("Search Cases response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Search Cases failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

// 获取案件ai生成总结
const getCaseSummary = async (caseId, language, userId) => {
  try {
    const response = await instance.get(paths.getCaseSummary, {
      params: {
        caseId: caseId,
        language: language,
        userId: userId,
      },
    });
    console.log("Case Summary response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Get Case Summary failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

/** 启动异步摘要（立即返回 RUNNING / DONE） */
const startSummaryAsync = async (caseId, language, userId, force = false) => {
  return await instance.get(paths.startSummaryAsync, {
    params: { caseId, language, userId, force },
    timeout: 120000,
  });
};

/** 查询异步摘要状态 */
const getSummaryAsyncStatus = async (caseId, language, userId) => {
  return await instance.get(paths.summaryAsyncStatus, {
    params: { caseId, language, userId },
    timeout: 60000,
  });
};

/** 当前用户 AI 摘要剩余次数（游客不传或 0 返回 null） */
const getUserSummaryCredits = async (userId) => {
  return await instance.get(paths.userSummaryCredits, {
    params: { userId },
    timeout: 30000,
  });
};

const getPaymentPackages = async (language) => {
  return await instance.get(paths.paymentPackages, {
    params: { language },
    timeout: 30000,
  });
};

const getPaymentChannels = async () => {
  return await instance.get(paths.paymentChannels, { timeout: 30000 });
};

const createPaymentOrder = async (userId, packageId) => {
  return await instance.post(paths.paymentOrder, { userId, packageId }, { timeout: 60000 });
};

const confirmMockPayment = async (userId, orderNo) => {
  return await instance.post(paths.paymentMockConfirm, { userId, orderNo }, { timeout: 30000 });
};

// 收藏案件
const favoriteCase = async (caseId, userId) => {
  try {
    const response = await instance.get(paths.favoriteCase, {
      params: {
        caseId: caseId,
        userId: userId,
      },
    });
    console.log("Favorite Case response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Favorite Case failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

// 取消收藏
const cancelFavoriteCase = async (caseId, userId) => {
  try {
    const response = await instance.delete(paths.cancelFavoriteCase, {
      params: {
        caseId: caseId,
        userId: userId,
      },
    });
    console.log("Cancel Favorite Case response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Cancel Favorite Case failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

// 获取收藏案件列表
const getFavoriteCases = async (language, country, userId, period, pagenum, pagesize) => {
  try {
    const response = await instance.get(paths.getFavoriteCases, {
      params: {
        language: language,
        country: country,
        userId: userId,
        period: period,
        pagenum: pagenum,
        pagesize: pagesize,
      },
    });
    console.log("Get Favorite Cases response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Get Favorite Cases failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

// 获取历史浏览案件列表
const getHistoryCases = async (language, country, userId, period, pagenum, pagesize) => {
  try {
    const response = await instance.get(paths.getHistoryCases, {
      params: {
        language: language,
        country: country,
        userId: userId,
        period: period,
        pagenum: pagenum,
        pagesize: pagesize,
      },
    });
    console.log("Get History Cases response:", response);
    if (response.code === 0) {
      ElNotification({
        title: "操作失败",
        message: response.message,
        type: "error",
      });
    }
    return response;
  } catch (error) {
    console.error("Get History Cases failed:", error);
    ElNotification({
      title: "操作失败",
      message: error.message,
      type: "error",
    });
    throw error;
  }
};

const kbIngest = async (payload) => {
  return await instance.post(paths.kbIngest, payload, { timeout: 120000 });
};

const kbIngestCrawler = async (payload) => {
  return await instance.post(paths.kbIngestCrawler, payload, { timeout: 180000 });
};

const kbQuery = async (payload) => {
  return await instance.post(paths.kbQuery, payload, { timeout: 120000 });
};
const api = {
  login,
  register,
  sendAuthCode,
  loginByCode,
  resetPasswordByCode,
  changePasswordApi,
  getCollectionList,
  getCaseMeta,
  postCaseQa,
  searchCases,
  getCaseSummary,
  startSummaryAsync,
  getSummaryAsyncStatus,
  getUserSummaryCredits,
  getPaymentPackages,
  getPaymentChannels,
  createPaymentOrder,
  confirmMockPayment,
  favoriteCase,
  cancelFavoriteCase,
  getFavoriteCases,
  getHistoryCases,
  kbIngest,
  kbIngestCrawler,
  kbQuery,
};
export default api;
