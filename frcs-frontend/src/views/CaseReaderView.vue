<template>
  <div class="reader-root" :class="{ 'is-dragging': dragging }" ref="rootRef">
    <header class="reader-toolbar">
      <el-button type="primary" plain @click="backToSearch">{{ lang === "zh" ? "返回搜索" : "Back to search" }}</el-button>
      <el-button type="primary" plain @click="backToHome">{{ lang === "zh" ? "返回主界面" : "Home" }}</el-button>
      <span class="reader-title">{{ caseMeta?.case_name || "—" }}</span>
      <span class="reader-layout-hint">{{ lang === "zh" ? "拖动中间分隔条可调整左右宽度" : "Drag the divider to resize panes" }}</span>
      <el-switch
        v-model="switchLang"
        :active-value="'en'"
        :inactive-value="'zh'"
        active-text="EN"
        inactive-text="中文"
        class="reader-lang-switch"
        :title="lang === 'zh' ? '切换原文与 AI 摘要的语言' : 'Switch language of the document and AI summary'"
        @change="onLangToggle"
      />
      <el-button class="reader-open" @click="openOriginUrl">{{ lang === "zh" ? "外部打开原文" : "Open externally" }}</el-button>
    </header>

    <div v-if="loadError" class="reader-error">{{ loadError }}</div>

    <div v-else class="reader-split" ref="splitContainerRef">
        <section class="reader-left" :style="{ flex: `0 0 ${leftPct}%`, minWidth: 0 }">
        <div class="pane-label pane-label-row">
          <span>{{ lang === "zh" ? "原始文档" : "Original Document" }}</span>
          <div class="pane-label-actions">
            <el-radio-group v-model="origView" size="small" @change="onOrigViewChange">
              <el-radio-button label="original">{{ lang === "zh" ? "官方原文" : "Original" }}</el-radio-button>
              <el-radio-button label="translated">{{ lang === "zh" ? "中文译文" : "English translation" }}</el-radio-button>
            </el-radio-group>
            <el-button v-if="origView === 'original'" size="small" text type="primary" @click="reloadOriginal">
              {{ lang === "zh" ? "刷新原文" : "Reload" }}
            </el-button>
          </div>
        </div>
        <div v-if="origView === 'original' && pdfUrl && canEmbedOriginalInFrame" class="reader-frame-wrap">
          <iframe class="reader-frame" :src="pdfUrl" title="original" :key="originalFrameKey" @load="onOriginalLoaded" />
        </div>
        <div v-else-if="origView === 'original' && pdfUrl && !canEmbedOriginalInFrame" class="reader-fallback">
          <p>{{ lang === "zh" ? "当前页面内嵌失败，请点击外部打开。" : "Embedding failed. Open externally." }}</p>
          <el-button type="primary" @click="openOriginUrl">{{ lang === "zh" ? "外部打开原文" : "Open externally" }}</el-button>
        </div>
        <div v-else-if="origView === 'original'" class="reader-placeholder">{{ lang === "zh" ? "无原文链接" : "No document URL" }}</div>
        <div v-else class="reader-translation-wrap">
          <div v-if="origTranslateError" class="detail-banner error">
            <span>{{ origTranslateError }}</span>
          </div>
          <div v-if="origTranslateNote" class="orig-translate-note">{{ origTranslateNote }}</div>
          <div v-if="origTranslateTruncated" class="orig-translate-truncated">
            {{ lang === "zh" ? "原文较长，译文已按前若干段落截断，完整内容请查看官方原文。" : "The original is long; only leading paragraphs are translated. See the official document for the full text." }}
          </div>
          <div v-loading="origTranslating" class="reader-translation-body" element-loading-text="正在翻译原文..." element-loading-spinner="Loading" element-loading-background="rgba(255, 255, 255, 0.85)">
            <template v-if="!origTranslating">
              <div v-if="!origTranslatedContent" class="reader-placeholder">
                {{ lang === "zh" ? "暂无译文内容" : "No translation available" }}
              </div>
              <div v-else class="original-text-content">
                <p v-for="(para, idx) in originalParagraphs" :key="idx" class="original-para">{{ para }}</p>
              </div>
            </template>
          </div>
        </div>
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
    const switchLang = computed({
      get: () => store.state.lang,
      set: (val) => {
        store.commit("setLang", val);
        localStorage.setItem("lang", val);
      },
    });

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

    // 原文视图：官方原文 / 译文（按段落展示）
    const origView = ref("original");
    const origTranslating = ref(false);
    const origTranslateError = ref("");
    const origTranslatedContent = ref("");
    const origTranslateNote = ref("");
    const origTranslateTruncated = ref(false);
    const origCache = ref({ zh: "", en: "" });
    let summaryReloadQueued = false;
    let summaryRunToken = 0;
    let origTranslateToken = 0;

    const splitIntoParagraphs = (text) => {
      const t = String(text || "").trim();
      if (!t) return [];
      const byBlankLine = t.split(/\n{2,}/).map((s) => s.trim()).filter(Boolean);
      if (byBlankLine.length > 1) {
        return byBlankLine;
      }
      return t.split(/\n+/).map((s) => s.trim()).filter(Boolean);
    };
    const originalParagraphs = computed(() => splitIntoParagraphs(origTranslatedContent.value));

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

    const runSummary = async (force = false, languageCode = lang.value) => {
      if (!caseId.value) return;
      const runToken = ++summaryRunToken;
      const requestLang = languageCode === "en" ? "en" : "zh";
      pollAbort = false;
      loadingDetail.value = true;
      detailError.value = "";
      isQuotaError.value = false;
      detailStatusText.value = lang.value === "zh" ? "正在启动摘要任务…" : "Starting summary…";
      caseDetailContent.value = "";
      let pollFinishedOk = false;
      const isCurrentRun = () => runToken === summaryRunToken;
      try {
        const userId = parseInt(getAuth("userId") || "0", 10);
        if (userId === 0) {
          if (!isCurrentRun()) return;
          detailError.value = lang.value === "zh" ? "请登录后使用 AI 摘要。" : "Please log in for AI summary.";
          detailStatusText.value = "";
          return;
        }
        const start = await api.startSummaryAsync(caseId.value, requestLang, userId, force);
        if (!isCurrentRun()) return;
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
          if (!isCurrentRun()) return;
          caseDetailContent.value = d.content;
          detailStatusText.value = "";
          await refreshSummaryCredits();
          pollFinishedOk = true;
          return;
        }
        detailStatusText.value = lang.value === "zh" ? "正在生成摘要，请稍候…" : "Generating…";
        const deadline = Date.now() + 30 * 60 * 1000;
        while (Date.now() < deadline && !pollAbort && isCurrentRun()) {
          await sleep(2000);
          if (!isCurrentRun()) return;
          const st = await api.getSummaryAsyncStatus(caseId.value, requestLang, userId);
          if (!isCurrentRun()) return;
          if (st.code !== 200) {
            const mapped = mapSummaryError(st.message, st.code, lang.value);
            detailError.value = mapped.text || (lang.value === "zh" ? "查询摘要状态失败" : "Could not get summary status");
            isQuotaError.value = mapped.quota;
            detailStatusText.value = "";
            break;
          }
          const s = st.data || {};
          if (!isCurrentRun()) return;
          detailStatusText.value =
            s.status === "RUNNING"
              ? lang.value === "zh"
                ? "正在生成摘要，请稍候…"
                : "Generating…"
              : "";
          if (s.status === "DONE" && s.content) {
            if (!isCurrentRun()) return;
            caseDetailContent.value = s.content;
            detailStatusText.value = "";
            await refreshSummaryCredits();
            pollFinishedOk = true;
            break;
          }
          if (s.status === "FAILED") {
            if (!isCurrentRun()) return;
            const mapped = mapSummaryError(s.errorMessage, null, lang.value);
            detailError.value = mapped.text || (lang.value === "zh" ? "摘要失败" : "Summary failed");
            isQuotaError.value = mapped.quota;
            detailStatusText.value = "";
            break;
          }
        }
        if (!pollFinishedOk && !detailError.value && !pollAbort && isCurrentRun()) {
          detailError.value =
            lang.value === "zh" ? "摘要等待超时，请稍后点击「重新生成」" : "Timed out. Tap Regenerate to retry.";
          detailStatusText.value = "";
        }
      } catch (e) {
        if (!isCurrentRun()) return;
        detailStatusText.value = "";
        const extra = e.serverMessage || e.message;
        const mapped = mapSummaryError(extra, null, lang.value);
        isQuotaError.value = mapped.quota;
        detailError.value = mapped.text;
      } finally {
        if (isCurrentRun()) {
          loadingDetail.value = false;
          if (detailError.value) {
            detailStatusText.value = "";
          }
          if (summaryReloadQueued) {
            summaryReloadQueued = false;
            runSummary();
          }
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

    const fetchOriginalTranslation = async () => {
      if (!caseId.value) return;
      const runToken = ++origTranslateToken;
      const code = lang.value === "en" ? "en" : "zh";
      if (origCache.value[code]) {
        origTranslatedContent.value = origCache.value[code];
        origTranslating.value = false;
        return;
      }
      const isCurrentRun = () => runToken === origTranslateToken;
      origTranslating.value = true;
      origTranslateError.value = "";
      origTranslatedContent.value = "";
      origTranslateNote.value = "";
      origTranslateTruncated.value = false;
      try {
        const r = await api.translateCaseOriginal(caseId.value, code);
        if (!isCurrentRun()) return;
        if (r.code === 200 && r.data) {
          const d = r.data;
          origCache.value[code] = d.content || "";
          origTranslatedContent.value = d.content || "";
          origTranslateNote.value = d.message || "";
          origTranslateTruncated.value = !!d.truncated;
          // 原文本来就是英文且目标也是英文时，直接展示官方原文更合适
          if (code === "en" && d.sourceLanguage === "en" && origView.value === "translated") {
            origView.value = "original";
          }
        } else {
          origTranslateError.value =
            r.message || (lang.value === "zh" ? "原文翻译失败，请稍后重试" : "Translation failed. Please retry later.");
        }
      } catch (e) {
        if (!isCurrentRun()) return;
        const status = e && e.response ? e.response.status : null;
        if (status === 404) {
          origTranslateError.value =
            lang.value === "zh"
              ? "原文翻译接口不存在（404）：当前连接的后端还没有部署最新代码，请先启动已更新的后端或把 /api 指向新后端。"
              : "Translation endpoint not found (404): the connected backend is outdated. Start the updated backend or point /api to it.";
        } else {
          origTranslateError.value =
            lang.value === "zh"
              ? "原文翻译请求失败，请检查网络后重试"
              : "Translation request failed. Check network and retry.";
        }
      } finally {
        if (isCurrentRun()) {
          origTranslating.value = false;
        }
      }
    };

    const onOrigViewChange = (val) => {
      origView.value = val;
      if (val === "translated") {
        fetchOriginalTranslation();
      }
    };

    const onLangToggle = async () => {
      if (!caseMeta.value || !caseId.value) return;
      if (!loadingDetail.value) {
        await runSummary();
      } else {
        // 摘要正在生成：等当前任务结束后再按新语言生成一次
        summaryReloadQueued = true;
      }
      const shouldAutoTranslate =
        caseMeta.value?.country === "JPN" || lang.value === "zh";
      if (shouldAutoTranslate) {
        origView.value = "translated";
      }
      if (origView.value === "translated") {
        await fetchOriginalTranslation();
      }
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
      // 新标签页打开阅读器时 Vuex 默认 zh，这里恢复用户上次选择的语言
      const storedLang = localStorage.getItem("lang");
      if (storedLang === "zh" || storedLang === "en") {
        store.commit("setLang", storedLang);
      }
      await loadMeta();
      if (caseMeta.value) runSummary();
      refreshSummaryCredits();
    });

    return {
      lang,
      switchLang,
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
      origView,
      origTranslating,
      origTranslateError,
      origTranslatedContent,
      origTranslateNote,
      origTranslateTruncated,
      originalParagraphs,
      leftPct,
      dragging,
      splitContainerRef,
      startDrag,
      openOriginUrl,
      reloadOriginal,
      onOrigViewChange,
      onLangToggle,
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
.reader-root { height:100vh; display:flex; flex-direction:column; background:var(--frcs-bg); overflow:hidden; }
.reader-root.is-dragging, .reader-root.is-dragging * { cursor: col-resize !important; }
.reader-toolbar { background:var(--frcs-surface); border-bottom:1px solid var(--frcs-border); padding:12px 18px; }
.reader-title { color:var(--frcs-primary); font-size:15px; }
.reader-layout-hint, .qa-tip { color:var(--frcs-text-2); }
.reader-split { padding:14px; gap:10px; }
.reader-left, .reader-right { background:var(--frcs-surface); border:1px solid var(--frcs-border); border-radius:var(--frcs-radius-md); box-shadow:var(--frcs-shadow-sm); }
.reader-gutter { width:10px; background:transparent; }
.reader-gutter::after { width:4px; height:44px; background:var(--frcs-border); }
.reader-gutter:hover::after { background:var(--frcs-primary-2); }
.pane-label { color:var(--frcs-primary); border-bottom:1px solid var(--frcs-border); padding:10px 14px; }
.pane-label-row { display:flex; align-items:center; justify-content:space-between; gap:8px; }
.pane-label-actions { display:flex; align-items:center; gap:6px; flex-wrap:wrap; justify-content:flex-end; }
.reader-lang-switch { flex-shrink:0; margin-left:4px; }
.reader-summary-wrap { padding:0 18px 12px; }
.case-detail-content { color:var(--frcs-text); line-height:1.85; }
.qa-pane { border-top:1px solid var(--frcs-border); padding:12px 14px 14px; }
.qa-role { color:var(--frcs-primary-2); }
.qa-guest { color:var(--frcs-warning); }
.reader-translation-wrap { flex:1; min-height:0; display:flex; flex-direction:column; overflow:hidden; }
.reader-translation-body { flex:1; min-height:0; overflow-y:auto; position:relative; }
.orig-translate-note { font-size:12px; color:var(--frcs-text-2); padding:6px 12px; background:var(--frcs-bg); border-bottom:1px solid var(--frcs-border); }
.orig-translate-truncated { font-size:12px; color:var(--frcs-warning); padding:6px 12px; background:#fdf6ec; border-bottom:1px solid var(--frcs-border); }
.original-text-content { padding:12px 14px 16px; }
.original-para { margin:0 0 12px; font-size:15px; line-height:1.85; color:var(--frcs-text); text-align:justify; word-break:break-word; white-space:pre-wrap; }
.reader-translation-wrap .reader-placeholder { height:auto; min-height:120px; }
.detail-banner.error { background:#fef0f0; color:var(--frcs-danger); padding:8px; border-radius:var(--frcs-radius-sm); margin-bottom:8px; }
.detail-status { font-size:13px; color:var(--frcs-primary-2); margin-bottom:8px; }
.qa-messages { flex:1; overflow-y:auto; min-height:80px; max-height:180px; margin-bottom:8px; }
.qa-bubble { margin-bottom:8px; font-size:14px; }
.qa-bubble.user .qa-text { color:var(--frcs-text); }
.qa-bubble.assistant .qa-text { color:var(--frcs-text-2); white-space:pre-wrap; }
.qa-input-row { display:flex; gap:8px; align-items:flex-end; }
.qa-input-row :deep(.el-textarea) { flex:1; }
@media (max-width:768px) { .reader-root { height:auto; min-height:100vh; overflow:visible; } .reader-toolbar { padding:10px 12px; } .reader-layout-hint { display:none; } .reader-split { flex-direction:column; padding:10px; } .reader-left { min-height:42vh; } .reader-right { min-height:52vh; } .reader-gutter { display:none; } .qa-pane { max-height:none; } }

.reader-root { width: 100%; min-height: 100vh; height: auto; overflow: auto; }
.reader-toolbar { width: min(1280px, calc(100% - 48px)); margin: 0 auto; min-height: 64px; display: flex; align-items: center; flex-wrap: wrap; gap: 10px 12px; padding: 14px 0; background: transparent; border-bottom: 1px solid var(--frcs-border); }
.reader-title { flex: 1 1 280px; min-width: 0; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; font-weight: 700; }
.reader-layout-hint { flex: 0 1 auto; font-size: 12px; }
.reader-open { flex: 0 0 auto; }
.reader-split { width: min(1280px, calc(100% - 48px)); min-height: calc(100vh - 96px); margin: 0 auto; display: flex; align-items: stretch; gap: 12px; padding: 18px 0 28px; }
.reader-left, .reader-right { min-width: 0; min-height: 0; overflow: hidden; }
.reader-left { display: flex; flex-direction: column; }
.reader-right { flex: 1 1 0; display: flex; flex-direction: column; overflow: auto; }
.reader-frame-wrap, .reader-fallback, .reader-placeholder { flex: 1 1 auto; min-height: 520px; }
.reader-frame { display: block; width: 100%; height: 100%; min-height: 520px; border: 0; background: #f5f5f4; }
.reader-summary-wrap { flex: 0 0 auto; min-height: 220px; max-height: 52vh; overflow: auto; }
.md-body { padding-top: 6px; }
.qa-pane { flex: 0 0 auto; }
.qa-button { margin-top: 20px; }
.reader-drag-shield { position: fixed; inset: 0; z-index: 20; cursor: col-resize; }
@media (max-width: 768px) {
  .reader-toolbar, .reader-split { width: calc(100% - 24px); }
  .reader-split { min-height: 0; }
  .reader-frame-wrap, .reader-fallback, .reader-placeholder, .reader-frame { min-height: 46vh; }
  .reader-summary-wrap { max-height: none; }
}
</style>
