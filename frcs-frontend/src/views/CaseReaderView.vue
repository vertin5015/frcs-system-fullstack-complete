<template>
  <div class="reader-root" :class="{ 'is-dragging': dragging }" ref="rootRef">
    <header class="reader-toolbar">
      <el-button type="primary" plain @click="backToSearch">{{ lang === "zh" ? "返回搜索" : "Back to search" }}</el-button>
      <el-button type="primary" plain @click="backToHome">{{ lang === "zh" ? "返回主界面" : "Home" }}</el-button>
      <span class="reader-title">{{ caseMeta?.case_name || "—" }}</span>
      <span class="reader-layout-hint">{{ lang === "zh" ? "拖动中间分隔条可调整左右宽度" : "Drag the divider to resize panes" }}</span>
      <el-button class="reader-open" @click="openOriginUrl">{{ lang === "zh" ? "外部打开原文" : "Open externally" }}</el-button>
    </header>

    <div v-if="loadError" class="reader-error">{{ loadError }}</div>

    <div v-else class="reader-split" ref="splitContainerRef">
        <section class="reader-left" :style="{ flex: `0 0 ${leftPct}%`, minWidth: 0 }">
        <div class="pane-label pane-label-row">
          <span>{{ lang === "zh" ? "原始文档" : "Original Document" }}</span>
          <el-button size="small" text type="primary" @click="reloadOriginal">
            {{ lang === "zh" ? "刷新原文" : "Reload" }}
          </el-button>
        </div>
        <div v-if="pdfUrl && canEmbedOriginalInFrame" class="reader-frame-wrap">
          <iframe class="reader-frame" :src="pdfUrl" title="original" :key="originalFrameKey" @load="onOriginalLoaded" />
        </div>
        <div v-else-if="pdfUrl && !canEmbedOriginalInFrame" class="reader-fallback">
          <p>{{ lang === "zh" ? "当前页面内嵌失败，请点击外部打开。" : "Embedding failed. Open externally." }}</p>
          <el-button type="primary" @click="openOriginUrl">{{ lang === "zh" ? "外部打开原文" : "Open externally" }}</el-button>
        </div>
        <div v-else class="reader-placeholder">{{ lang === "zh" ? "无原文链接" : "No document URL" }}</div>
      </section>

      <div class="reader-gutter" @pointerdown.prevent="startDrag" :title="lang === 'zh' ? '拖拽调整左右宽度' : 'Drag to resize'" />
      <div v-if="dragging" class="reader-drag-shield" />

      <section class="reader-right">
        <div class="pane-label pane-label-row">
          <span>{{ lang === "zh" ? "AI 摘要" : "AI Summary" }}</span>
          <el-button size="small" type="primary" plain text :disabled="loadingDetail" @click="retrySummary">
            {{ lang === "zh" ? "重新生成" : "Regenerate" }}
          </el-button>
        </div>
        <div class="reader-summary-wrap" v-loading="loadingDetail">
          <div v-if="detailError" class="detail-banner error">
            <span>{{ detailError }}</span>
          </div>
          <div v-if="detailStatusText" class="detail-status">{{ detailStatusText }}</div>
          <div v-html="caseDetailHtml" class="case-detail-content md-body"></div>
        </div>

        <div class="qa-pane">
          <div class="pane-label">{{ lang === "zh" ? "本案问答" : "Q&A" }}</div>
          <p class="qa-tip">{{ lang === "zh" ? "基于上方摘要回答；若需先摘要，请点「重新生成」或等待生成完成。" : "Answers use the summary above." }}</p>
          <div class="qa-messages">
            <div v-for="(m, i) in qaMessages" :key="i" :class="['qa-bubble', m.role]">
              <span class="qa-role">{{ m.role === "user" ? (lang === "zh" ? "问" : "Q") : (lang === "zh" ? "答" : "A") }}</span>
              <div class="qa-text">{{ m.text }}</div>
            </div>
          </div>
          <div class="qa-input-row">
            <el-input v-model="qaInput" type="textarea" :rows="2" :placeholder="lang === 'zh' ? '输入与本案相关的问题…' : 'Ask about this case…'" :disabled="!canAsk" />
            <el-button type="primary" :loading="qaLoading" :disabled="!canAsk || !qaInput.trim()" @click="sendQa">
              {{ lang === "zh" ? "发送" : "Send" }}
            </el-button>
          </div>
          <p v-if="guestBlock" class="qa-guest">{{ lang === "zh" ? "登录后可使用本案问答。" : "Log in to use Q&A." }}</p>
        </div>
      </section>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onUnmounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useStore } from "vuex";
import MarkdownIt from "markdown-it";
import api from "../api/index";
import { ElMessage } from "element-plus";
import { getAuth, setAuth } from "../utils/authStorage";

export default {
  name: "CaseReaderView",
  setup() {
    const route = useRoute();
    const router = useRouter();
    const store = useStore();
    const lang = computed(() => store.getters.lang);

    const caseMeta = ref(null);
    const loadError = ref("");
    const caseId = ref("");
    const loadingDetail = ref(false);
    const detailError = ref("");
    const isQuotaError = ref(false);
    const detailStatusText = ref("");
    const caseDetailContent = ref("");
    const summaryCredits = ref(null);
    const originalFrameKey = ref(0);
    let pollAbort = false;

    const leftPct = ref(52);
    const splitContainerRef = ref(null);
    const dragging = ref(false);

    const qaMessages = ref([]);
    const qaInput = ref("");
    const qaLoading = ref(false);

    const guestBlock = computed(() => getAuth("userId") === "0");

    const canAsk = computed(() => {
      if (guestBlock.value) return false;
      return !!caseDetailContent.value?.trim();
    });

    const rawOriginalUrl = computed(() => caseMeta.value?.original_document_url || "");
    const pdfUrl = computed(() => {
      const raw = rawOriginalUrl.value;
      if (!raw) return "";
      return `/api/cases/original-proxy?url=${encodeURIComponent(raw)}`;
    });

    // 不再按来源域名禁用内嵌，统一先尝试 iframe。
    const canEmbedOriginalInFrame = computed(() => !!pdfUrl.value);

    const md = new MarkdownIt({ html: false, linkify: true, breaks: true });
    const caseDetailHtml = computed(() => md.render(caseDetailContent.value || ""));

    const refreshSummaryCredits = async () => {
      const uid = getAuth("userId");
      if (!uid || uid === "0") {
        summaryCredits.value = null;
        return;
      }
      try {
        const r = await api.getUserSummaryCredits(parseInt(uid, 10));
        if (r.code === 200 && r.data != null) {
          summaryCredits.value = r.data;
          setAuth("summaryCredits", String(r.data));
        }
      } catch {
        /* ignore */
      }
    };

    const loadMeta = async () => {
      const id = route.query.caseId;
      if (!id) {
        loadError.value = lang.value === "zh" ? "缺少 caseId 参数" : "Missing caseId";
        return;
      }
      caseId.value = id;
      const uid = parseInt(getAuth("userId") || "0", 10);
      try {
        const r = await api.getCaseMeta(id, lang.value, uid);
        if (r.code !== 200 || !r.data) {
          loadError.value = r.message || (lang.value === "zh" ? "加载案例失败" : "Failed to load case");
          return;
        }
        caseMeta.value = r.data;
      } catch (e) {
        loadError.value = lang.value === "zh" ? "网络错误" : "Network error";
      }
    };

    const sleep = (ms) => new Promise((r) => setTimeout(r, ms));

    const runSummary = async (force = false) => {
      if (!caseId.value) return;
      pollAbort = false;
      loadingDetail.value = true;
      detailError.value = "";
      isQuotaError.value = false;
      detailStatusText.value = lang.value === "zh" ? "正在启动摘要任务…" : "Starting summary…";
      caseDetailContent.value = "";
      let pollFinishedOk = false;
      try {
        const userId = parseInt(getAuth("userId") || "0", 10);
        if (userId === 0) {
          detailError.value = lang.value === "zh" ? "请登录后使用 AI 摘要。" : "Please log in for AI summary.";
          detailStatusText.value = "";
          return;
        }
        const start = await api.startSummaryAsync(caseId.value, lang.value, userId, force);
        if (start.code !== 200) {
          const mapped = mapSummaryError(start.message, start.code, lang.value);
          detailError.value = mapped.text;
          isQuotaError.value = mapped.quota;
          detailStatusText.value = "";
          await refreshSummaryCredits();
          return;
        }
        const d = start.data || {};
        if (d.status === "DONE" && d.content) {
          caseDetailContent.value = d.content;
          detailStatusText.value = "";
          await refreshSummaryCredits();
          pollFinishedOk = true;
          return;
        }
        detailStatusText.value = lang.value === "zh" ? "正在生成摘要，请稍候…" : "Generating…";
        const deadline = Date.now() + 30 * 60 * 1000;
        while (Date.now() < deadline && !pollAbort) {
          await sleep(2000);
          const st = await api.getSummaryAsyncStatus(caseId.value, lang.value, userId);
          if (st.code !== 200) {
            const mapped = mapSummaryError(st.message, st.code, lang.value);
            detailError.value = mapped.text || (lang.value === "zh" ? "查询摘要状态失败" : "Could not get summary status");
            isQuotaError.value = mapped.quota;
            detailStatusText.value = "";
            break;
          }
          const s = st.data || {};
          detailStatusText.value =
            s.status === "RUNNING"
              ? lang.value === "zh"
                ? "正在生成摘要，请稍候…"
                : "Generating…"
              : "";
          if (s.status === "DONE" && s.content) {
            caseDetailContent.value = s.content;
            detailStatusText.value = "";
            await refreshSummaryCredits();
            pollFinishedOk = true;
            break;
          }
          if (s.status === "FAILED") {
            const mapped = mapSummaryError(s.errorMessage, null, lang.value);
            detailError.value = mapped.text || (lang.value === "zh" ? "摘要失败" : "Summary failed");
            isQuotaError.value = mapped.quota;
            detailStatusText.value = "";
            break;
          }
        }
        if (!pollFinishedOk && !detailError.value && !pollAbort) {
          detailError.value =
            lang.value === "zh" ? "摘要等待超时，请稍后点击「重新生成」" : "Timed out. Tap Regenerate to retry.";
          detailStatusText.value = "";
        }
      } catch (e) {
        detailStatusText.value = "";
        const extra = e.serverMessage || e.message;
        const mapped = mapSummaryError(extra, null, lang.value);
        isQuotaError.value = mapped.quota;
        detailError.value = mapped.text;
      } finally {
        loadingDetail.value = false;
        if (detailError.value) {
          detailStatusText.value = "";
        }
      }
    };

    const mapSummaryError = (raw, code, l) => {
      const zh = l === "zh";
      const s = String(raw || "").trim();
      const isQuota =
        code === 40301 ||
        s.includes("QUOTA_EXCEEDED") ||
        s.includes("insufficient_user_quota") ||
        s.includes("用户额度不足");
      if (isQuota) {
        return {
          quota: true,
          text: zh ? "AI 摘要额度不足，请先购买次数后重试。" : "Insufficient AI summary credits. Please purchase credits and retry.",
        };
      }
      if (!s) {
        return {
          quota: false,
          text: zh ? "获取摘要失败（请检查网络或后端服务）" : "Summary failed (check network/backend service).",
        };
      }
      // 避免把整段 HTTP JSON 错误直接展示给用户
      if (s.startsWith("HTTP ") || s.startsWith("{\"error\"")) {
        return {
          quota: false,
          text: zh ? "AI 服务暂时不可用，请稍后重试。" : "AI service is temporarily unavailable. Please retry later.",
        };
      }
      return { quota: false, text: s };
    };

    const sendQa = async () => {
      const q = qaInput.value.trim();
      if (!q || !caseId.value) return;
      const userId = parseInt(getAuth("userId") || "0", 10);
      if (userId === 0) {
        ElMessage.warning(lang.value === "zh" ? "请先登录" : "Please log in");
        return;
      }
      qaMessages.value.push({ role: "user", text: q });
      qaInput.value = "";
      qaLoading.value = true;
      try {
        const r = await api.postCaseQa(caseId.value, q, lang.value, userId);
        if (r.code !== 200) {
          qaMessages.value.push({ role: "assistant", text: r.message || "Error" });
        } else {
          qaMessages.value.push({ role: "assistant", text: r.data || "" });
        }
      } catch (e) {
        qaMessages.value.push({ role: "assistant", text: lang.value === "zh" ? "请求失败" : "Request failed" });
      } finally {
        qaLoading.value = false;
      }
    };

    const openOriginUrl = () => {
      const u = rawOriginalUrl.value;
      if (u) window.open(u, "_blank");
      else ElMessage.warning(lang.value === "zh" ? "无原文链接" : "No URL");
    };

    const reloadOriginal = () => {
      originalFrameKey.value += 1;
    };

    const onOriginalLoaded = () => {};

    const backToSearch = () => router.push("/case-query/search");
    const backToHome = () => router.push("/case-query/home");
    const goRecharge = () => router.push("/case-query/recharge");

    let moveHandler = null;
    let upHandler = null;

    const stopDrag = () => {
      dragging.value = false;
      if (moveHandler) {
        window.removeEventListener("pointermove", moveHandler);
        moveHandler = null;
      }
      if (upHandler) {
        window.removeEventListener("pointerup", upHandler);
        window.removeEventListener("pointercancel", upHandler);
        window.removeEventListener("blur", upHandler);
        upHandler = null;
      }
      document.body.style.userSelect = "";
      document.body.style.cursor = "";
    };

    const startDrag = (event) => {
      stopDrag();
      dragging.value = true;
      document.body.style.userSelect = "none";
      document.body.style.cursor = "col-resize";
      moveHandler = (e) => {
        if (!dragging.value) return;
        const el = splitContainerRef.value;
        if (!el) return;
        const rect = el.getBoundingClientRect();
        let p = ((e.clientX - rect.left) / rect.width) * 100;
        p = Math.min(85, Math.max(15, p));
        leftPct.value = p;
      };
      upHandler = () => stopDrag();
      window.addEventListener("pointermove", moveHandler);
      window.addEventListener("pointerup", upHandler);
      window.addEventListener("pointercancel", upHandler);
      window.addEventListener("blur", upHandler);
      moveHandler(event);
    };

    onUnmounted(() => {
      pollAbort = true;
      stopDrag();
    });

    const retrySummary = () => {
      runSummary(true);
    };

    onMounted(async () => {
      await loadMeta();
      if (caseMeta.value) runSummary();
      refreshSummaryCredits();
    });

    return {
      lang,
      caseMeta,
      loadError,
      pdfUrl,
      canEmbedOriginalInFrame,
      loadingDetail,
      detailError,
      isQuotaError,
      detailStatusText,
      caseDetailHtml,
      summaryCredits,
      originalFrameKey,
      leftPct,
      dragging,
      splitContainerRef,
      startDrag,
      openOriginUrl,
      reloadOriginal,
      backToSearch,
      backToHome,
      goRecharge,
      qaMessages,
      qaInput,
      qaLoading,
      sendQa,
      canAsk,
      guestBlock,
      retrySummary,
      onOriginalLoaded,
    };
  },
};
</script>

<style scoped>
.reader-root {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  overflow: hidden;
}
.reader-root.is-dragging,
.reader-root.is-dragging * {
  cursor: col-resize !important;
}
.reader-toolbar {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  padding: 10px 16px;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  z-index: 5;
}
.reader-title {
  flex: 1;
  min-width: 120px;
  font-weight: 600;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.reader-layout-hint {
  flex-shrink: 0;
  font-size: 12px;
  color: #909399;
}
.reader-open {
  flex-shrink: 0;
}
.reader-error {
  padding: 24px;
  color: #f56c6c;
}
.reader-split {
  flex: 1;
  display: flex;
  flex-direction: row;
  align-items: stretch;
  min-height: 0;
  padding: 8px;
  gap: 0;
}
.reader-left {
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}
.reader-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e4e7ed;
  overflow: hidden;
}
.reader-gutter {
  width: 12px;
  flex-shrink: 0;
  cursor: col-resize;
  background: linear-gradient(90deg, #e4e7ed 0, #dcdfe6 50%, #e4e7ed 100%);
  align-self: stretch;
  position: relative;
}
.reader-gutter::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  width: 3px;
  height: 42px;
  border-radius: 999px;
  background: #a8abb2;
  transform: translate(-50%, -50%);
}
.reader-gutter:hover {
  background: #409eff33;
}
.reader-drag-shield {
  position: fixed;
  inset: 0;
  z-index: 9999;
  cursor: col-resize;
  background: transparent;
}
.pane-label {
  font-size: 14px;
  font-weight: 600;
  color: #606266;
  padding: 8px 12px;
  border-bottom: 1px solid #ebeef5;
}
.pane-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}
.reader-frame {
  width: 100%;
  height: 100%;
  border: none;
}
.reader-frame-wrap {
  flex: 1;
  position: relative;
  width: 100%;
  min-height: 0;
}
.reader-fallback,
.reader-placeholder {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 16px;
  color: #909399;
  font-size: 14px;
}
.reader-summary-wrap {
  flex: 1;
  min-height: 0;
  overflow: auto;
  padding: 0 12px 8px;
}
.detail-banner.error {
  background: #fef0f0;
  color: #f56c6c;
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 8px;
}
.detail-status {
  font-size: 13px;
  color: #409eff;
  margin-bottom: 8px;
}
.case-detail-content {
  font-size: 15px;
  line-height: 1.75;
  color: #222;
}
.qa-pane {
  border-top: 1px solid #ebeef5;
  padding: 8px 12px 12px;
  flex-shrink: 0;
  max-height: 42vh;
  display: flex;
  flex-direction: column;
  min-height: 0;
}
.qa-tip {
  font-size: 12px;
  color: #909399;
  margin: 0 0 8px;
}
.qa-messages {
  flex: 1;
  overflow-y: auto;
  min-height: 80px;
  max-height: 180px;
  margin-bottom: 8px;
}
.qa-bubble {
  margin-bottom: 8px;
  font-size: 14px;
}
.qa-bubble.user .qa-text {
  color: #303133;
}
.qa-bubble.assistant .qa-text {
  color: #606266;
  white-space: pre-wrap;
}
.qa-role {
  font-weight: 600;
  color: #409eff;
  margin-right: 6px;
}
.qa-input-row {
  display: flex;
  gap: 8px;
  align-items: flex-end;
}
.qa-input-row :deep(.el-textarea) {
  flex: 1;
}
.qa-guest {
  font-size: 12px;
  color: #e6a23c;
  margin: 0;
}
</style>
