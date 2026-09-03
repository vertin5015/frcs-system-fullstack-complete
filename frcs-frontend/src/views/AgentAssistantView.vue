<template>
  <div class="agent-page">
    <div class="agent-toolbar">
      <div>
        <h2>{{ lang === "zh" ? "涉外法律智能问答" : "Legal RAG Agent" }}</h2>
        <p>{{ lang === "zh" ? "RAG 回答、案例检索轨迹与检索证据展示" : "RAG answer, case search trace, and retrieved evidence." }}</p>
      </div>
      <div class="toolbar-controls">
        <el-switch v-model="form.refreshCases" :active-text="lang === 'zh' ? '先检索案例' : 'Search first'" :inactive-text="lang === 'zh' ? '仅知识库' : 'KB only'" />
      </div>
    </div>

    <div class="agent-grid">
      <section class="ask-panel">
        <el-input class="agent-question" v-model="form.question" type="textarea" :rows="8" resize="none" :placeholder="lang === 'zh' ? '请输入法律问题' : 'Ask a legal question'" />
        <div class="filters">
          <div class="filter-item filter-country"><label>{{ lang === "zh" ? "国家" : "Country" }}</label><el-select v-model="form.country" clearable :placeholder="lang === 'zh' ? '国家' : 'Country'"><el-option label="US" value="US" /><el-option label="EU" value="EU" /><el-option label="JPN" value="JPN" /></el-select></div>
          <div class="filter-item filter-sources"><label>{{ lang === "zh" ? "数据源" : "Data source" }}</label><el-input v-model="form.sources" :placeholder="lang === 'zh' ? '如 US,EU,JPN' : 'e.g. US,EU,JPN'" /></div>
          <div class="filter-item filter-period"><label>{{ lang === "zh" ? "时间范围" : "Period" }}</label><el-select v-model="form.period" clearable :placeholder="lang === 'zh' ? '时间范围' : 'Period'"><el-option :label="lang === 'zh' ? '1 年' : '1 year'" :value="1" /><el-option :label="lang === 'zh' ? '3 年' : '3 years'" :value="3" /><el-option :label="lang === 'zh' ? '5 年' : '5 years'" :value="5" /><el-option :label="lang === 'zh' ? '10 年' : '10 years'" :value="10" /></el-select></div>
          <div class="filter-item filter-top-k"><label>{{ lang === "zh" ? "返回条数" : "Top K" }}</label><el-input-number v-model="form.topK" :min="1" :max="10" controls-position="right" /></div>
        </div>
        <div class="actions"><el-button :icon="Delete" @click="resetForm">{{ lang === "zh" ? "清空" : "Clear" }}</el-button><el-button type="primary" :icon="ChatDotRound" :loading="loading" @click="askAgent">{{ lang === "zh" ? "提问" : "Ask" }}</el-button></div>
      </section>

      <section class="answer-panel">
        <div class="answer-head"><h3>{{ lang === "zh" ? "回答" : "Answer" }}</h3><el-tag v-if="result" type="success">{{ result.route }}</el-tag></div>
        <el-input class="agent-answer" :model-value="answer" type="textarea" :rows="13" resize="none" readonly :placeholder="lang === 'zh' ? '回答内容将显示在这里' : 'Answer will appear here'" />
      </section>
    </div>

    <div v-if="result" class="result-grid">
      <section><div class="section-head"><h3>{{ lang === "zh" ? "检索轨迹" : "Trace" }}</h3></div><el-timeline><el-timeline-item v-for="(step, index) in result.trace || []" :key="index"><strong>{{ step.name }}</strong><div class="muted">{{ step.detail }}</div></el-timeline-item></el-timeline></section>
      <section><div class="section-head"><h3>{{ lang === "zh" ? "知识库命中" : "Retrieved KB" }}</h3><span v-if="result.kbHits && result.kbHits.length" class="muted">{{ lang === "zh" ? `命中 ${result.kbHits.length} 条` : `${result.kbHits.length} hits` }}</span></div><el-empty v-if="!result.kbHits || result.kbHits.length === 0" :description="lang === 'zh' ? '暂无知识库命中' : 'No KB hits'" /><div v-else class="hit-list"><article v-for="(hit, index) in result.kbHits" :key="index" class="hit-item"><div class="hit-title">{{ hit.title || hit.sourceId || (lang === "zh" ? `命中 ${index + 1}` : `Hit ${index + 1}`) }}</div><p>{{ hit.preview }}</p></article></div></section>
      <section><div class="section-head"><h3>{{ lang === "zh" ? "相关案例" : "Related Cases" }}</h3><span v-if="result.relatedCases && result.relatedCases.length" class="muted">{{ lang === "zh" ? `共 ${result.relatedCases.length} 条` : `${result.relatedCases.length} found` }}</span></div><el-empty v-if="!result.relatedCases || result.relatedCases.length === 0" :description="lang === 'zh' ? '暂无相关案例' : 'No related cases'" /><div v-else class="case-list"><article v-for="(item, index) in result.relatedCases" :key="item.case_id || index" class="case-card"><div class="case-title">{{ item.case_name || item.case_id || (lang === "zh" ? `案例 ${index + 1}` : `Case ${index + 1}`) }}</div><div class="case-meta"><el-tag v-if="item.country" size="small" effect="plain">{{ item.country }}</el-tag><span v-if="item.judgement_date" class="muted">{{ item.judgement_date }}</span></div><p v-if="item.tags" class="case-tags">{{ item.tags }}</p><div class="case-foot"><span v-if="item.citationCount != null" class="muted">{{ lang === "zh" ? `引用 ${item.citationCount}` : `Cited ${item.citationCount}` }}</span><span v-if="item.favoritedCount != null" class="muted">{{ lang === "zh" ? `收藏 ${item.favoritedCount}` : `★ ${item.favoritedCount}` }}</span><el-button v-if="item.original_document_url" link type="primary" size="small" class="case-original-btn" @click="openOriginal(item.original_document_url)">{{ lang === "zh" ? "原文" : "Original doc" }}</el-button></div></article></div></section>
    </div>
  </div>
</template>

<script>
import { computed, reactive, ref } from "vue";
import { ChatDotRound, Delete } from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useStore } from "vuex";
import api from "../api";
import { getAuth } from "../utils/authStorage";

export default {
  name: "AgentAssistantView",
  setup() {
    const store = useStore();
    const lang = computed(() => store.getters.lang);
    const loading = ref(false);
    const result = ref(null);
    const answer = computed(() => (result.value && result.value.answer ? result.value.answer : ""));
    const form = reactive({ question: "", country: "", sources: "US,EU,JPN", period: null, topK: 5, refreshCases: true });
    const openOriginal = (url) => { if (url) window.open(url, "_blank", "noopener"); };
    const resetForm = () => { form.question = ""; form.country = ""; form.sources = "US,EU,JPN"; form.period = null; form.topK = 5; form.refreshCases = true; result.value = null; };
    const askAgent = async () => {
      if (!form.question.trim()) { ElMessage.warning(lang.value === "zh" ? "请输入问题" : "Please enter a question"); return; }
      result.value = null; loading.value = true;
      try {
        const payload = { ...form, question: form.question.trim(), language: lang.value, userId: Number(getAuth("userId") || 0) };
        const res = await api.agentAsk(payload);
        if (res.code === 200 && res.data) result.value = res.data;
        else ElMessage.error(res.message || (lang.value === "zh" ? "智能问答请求失败" : "Agent request failed"));
      } finally { loading.value = false; }
    };
    return { form, result, answer, loading, lang, ChatDotRound, Delete, resetForm, askAgent, openOriginal };
  },
};
</script>

<style scoped>
.agent-page { min-height:calc(100vh - 68px); padding:28px clamp(18px,3vw,42px); background:var(--frcs-bg); }
.agent-toolbar h2 { color:var(--frcs-primary); font-size:24px; }
.agent-toolbar p, .muted { color:var(--frcs-text-2); }
.agent-grid, .result-grid { gap:18px; }
.ask-panel, .answer-panel, .result-grid section { background:var(--frcs-surface); border:1px solid var(--frcs-border); border-radius:var(--frcs-radius-md); box-shadow:var(--frcs-shadow-sm); padding:18px; }
.actions { margin-top:16px; }
.hit-item, .case-card { border:1px solid var(--frcs-border); border-radius:var(--frcs-radius-sm); background:#fbfcfd; }
.hit-item:hover, .case-card:hover { border-color:rgba(47,93,126,.35); box-shadow:var(--frcs-shadow-sm); }
.hit-title, .case-title { color:var(--frcs-text) !important; }
.case-foot { border-top-color:var(--frcs-border); }

/* Agent form rhythm and content-fit widths */
.agent-page { width: 100%; max-width: 1280px; margin: 0 auto; }
.agent-toolbar { display: flex; align-items: flex-start; justify-content: space-between; gap: 20px; margin-bottom: 18px; }
.agent-toolbar h2 { margin: 0; }
.agent-toolbar p { margin: 6px 0 0; }
.agent-grid { display: grid; grid-template-columns: minmax(0, 1.15fr) minmax(360px, .85fr); gap: 18px; align-items: start; }
.ask-panel, .answer-panel { min-width: 0; }
.agent-question, .agent-answer { width: 100%; }
.agent-question .el-textarea__inner { min-height: 170px !important; }
.filters { display: flex; flex-wrap: wrap; gap: 14px 16px; margin-top: 18px; padding-top: 16px; border-top: 1px solid var(--frcs-border); }
.filter-item { display: flex; flex-direction: column; gap: 7px; min-width: 0; }
.filter-item label { color: var(--frcs-text-2); font-size: 11px; font-weight: 700; letter-spacing: .05em; text-transform: uppercase; }
.filter-country, .filter-period { flex: 0 1 170px; width: 170px; }
.filter-sources { flex: 1 1 220px; min-width: 200px; }
.filter-top-k { flex: 0 0 120px; width: 120px; }
.filter-item .el-select, .filter-item .el-input, .filter-item .el-input-number { width: 100%; }
.actions { display: flex; align-items: center; gap: 12px; margin-top: 20px; padding-top: 16px; border-top: 1px solid var(--frcs-border); }
.actions .el-button { min-width: 96px; }
.agent-answer .el-textarea__inner { min-height: 286px !important; }
.result-grid { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 18px; margin-top: 18px; }
.result-grid section { min-width: 0; }
.hit-list, .case-list { display: flex; flex-direction: column; gap: 10px; }
.hit-item, .case-card { min-width: 0; padding: 14px; }
.hit-item p, .case-card p { overflow-wrap: anywhere; }
@media (max-width: 980px) { .agent-grid { grid-template-columns: 1fr; } .result-grid { grid-template-columns: 1fr 1fr; } .agent-answer .el-textarea__inner { min-height: 220px !important; } }
@media (max-width: 680px) { .agent-page { padding: 20px 12px; } .agent-toolbar { flex-direction: column; gap: 12px; } .filters { flex-direction: column; } .filter-country, .filter-period, .filter-sources, .filter-top-k { width: 100%; flex-basis: auto; } .actions { flex-wrap: wrap; } .result-grid { grid-template-columns: 1fr; } }
</style>
