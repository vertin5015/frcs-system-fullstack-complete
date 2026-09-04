/**
 * 异步 AI 摘要的公共调用封装：
 * 先启动摘要任务，若后端已有对应语言内容则立即 DONE；否则轮询状态直到完成/失败。
 * 避免收藏页、历史弹窗切换语言时因后端只有单语摘要而拿不到另一语言内容。
 */
import api from "../api/index";

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * 后端错误文案映射（与 CaseReaderView 内保持一致）
 */
export const mapSummaryError = (raw, code, isZh) => {
  const s = String(raw || "").trim();
  const isQuota =
    code === 40301 ||
    s.includes("QUOTA_EXCEEDED") ||
    s.includes("insufficient_user_quota") ||
    s.includes("用户额度不足");
  if (isQuota) {
    return {
      quota: true,
      text: isZh
        ? "AI 摘要额度不足，请先购买次数后重试。"
        : "Insufficient AI summary credits. Please purchase credits and retry.",
    };
  }
  if (!s) {
    return {
      quota: false,
      text: isZh
        ? "获取摘要失败（请检查网络或后端服务）"
        : "Summary failed (check network/backend service).",
    };
  }
  if (s.startsWith("HTTP ") || s.startsWith('{"error"')) {
    return {
      quota: false,
      text: isZh
        ? "AI 服务暂时不可用，请稍后重试。"
        : "AI service is temporarily unavailable. Please retry later.",
    };
  }
  return { quota: false, text: s };
};

/**
 * 确保指定语言存在 AI 摘要；存在则直接返回，不存在则启动并轮询生成。
 *
 * @param {object} options
 * @param {string} options.caseId
 * @param {string} options.language zh / en
 * @param {number} options.userId
 * @param {boolean} [options.force=false]
 * @param {number} [options.tickMs=2000]
 * @param {number} [options.maxWaitMs=15*60*1000]
 * @returns {Promise<{ok:boolean, content?:string, error?:string, quota?:boolean, status?:string}>}
 */
export const ensureCaseSummary = async ({
  caseId,
  language,
  userId,
  force = false,
  tickMs = 2000,
  maxWaitMs = 15 * 60 * 1000,
}) => {
  const isZh = String(language || "zh").toLowerCase().startsWith("zh");
  const fail = (raw, code) => {
    const mapped = mapSummaryError(raw, code, isZh);
    return { ok: false, error: mapped.text, quota: mapped.quota };
  };

  if (!caseId) {
    return fail(isZh ? "缺少案例 ID" : "Missing case ID");
  }

  try {
    const start = await api.startSummaryAsync(caseId, language, userId, force);
    if (start.code !== 200) {
      return fail(start.message, start.code);
    }
    const startData = start.data || {};
    if (startData.status === "DONE" && startData.content) {
      return { ok: true, status: "DONE", content: startData.content };
    }

    const deadline = Date.now() + maxWaitMs;
    while (Date.now() < deadline) {
      await sleep(tickMs);
      const st = await api.getSummaryAsyncStatus(caseId, language, userId);
      if (st.code !== 200) {
        return fail(st.message, st.code);
      }
      const s = st.data || {};
      if (s.status === "DONE" && s.content) {
        return { ok: true, status: "DONE", content: s.content };
      }
      if (s.status === "FAILED") {
        return fail(s.errorMessage, null);
      }
    }
    return fail(
      isZh ? "摘要等待超时，请稍后重试" : "Summary timed out. Please retry later."
    );
  } catch (e) {
    return fail(e.serverMessage || e.message, null);
  }
};
