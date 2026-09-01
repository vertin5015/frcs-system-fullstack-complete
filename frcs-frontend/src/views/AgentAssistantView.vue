<template>
  <div class="agent-page">
    <div class="agent-toolbar">
      <div>
        <h2>{{ lang === "zh" ? "涉外法律智能问答" : "Legal RAG Agent" }}</h2>
        <p>{{ lang === "zh" ? "RAG 回答、案例检索轨迹与检索证据展示" : "RAG answer, case search trace, and retrieved evidence." }}</p>
      </div>
      <div class="toolbar-controls">
        <el-switch
          v-model="form.refreshCases"
          :active-text="lang === 'zh' ? '先检索案例' : 'Search first'"
          :inactive-text="lang === 'zh' ? '仅知识库' : 'KB only'"
        />
      </div>
    </div>

    <div class="agent-grid">
      <section class="ask-panel">
        <el-input
          v-model="form.question"
          type="textarea"
          :rows="8"
          resize="none"
          :placeholder="lang === 'zh' ? '请输入法律问题' : 'Ask a legal question'"
        />
        <div class="filters">
          <div class="filter-item">
            <label>{{ lang === "zh" ? "国家" : "Country" }}</label>
            <el-select v-model="form.country" clearable :placeholder="lang === 'zh' ? '国家' : 'Country'">
              <el-option label="US" value="US" />
              <el-option label="EU" value="EU" />
              <el-option label="JPN" value="JPN" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>{{ lang === "zh" ? "数据源" : "Data source" }}</label>
            <el-input v-model="form.sources" :placeholder="lang === 'zh' ? '如 US,EU,JPN' : 'e.g. US,EU,JPN'" />
          </div>
          <div class="filter-item">
            <label>{{ lang === "zh" ? "时间范围" : "Period" }}</label>
            <el-select v-model="form.period" clearable :placeholder="lang === 'zh' ? '时间范围' : 'Period'">
              <el-option :label="lang === 'zh' ? '1 年' : '1 year'" :value="1" />
              <el-option :label="lang === 'zh' ? '3 年' : '3 years'" :value="3" />
              <el-option :label="lang === 'zh' ? '5 年' : '5 years'" :value="5" />
              <el-option :label="lang === 'zh' ? '10 年' : '10 years'" :value="10" />
            </el-select>
          </div>
          <div class="filter-item">
            <label>{{ lang === "zh" ? "返回条数" : "Top K" }}</label>
            <el-input-number
              v-model="form.topK"
              :min="1"
              :max="10"
              controls-position="right"
            />
          </div>
        </div>
        <div class="actions">
          <el-button :icon="Delete" @click="resetForm">{{ lang === "zh" ? "清空" : "Clear" }}</el-button>
          <el-button type="primary" :icon="ChatDotRound" :loading="loading" @click="askAgent">
            {{ lang === "zh" ? "提问" : "Ask" }}
          </el-button>
        </div>
      </section>

      <section class="answer-panel">
        <div class="answer-head">
          <h3>{{ lang === "zh" ? "回答" : "Answer" }}</h3>
          <el-tag v-if="result" type="success">{{ result.route }}</el-tag>
        </div>
        <el-input
          :model-value="answer"
          type="textarea"
          :rows="13"
          resize="none"
          readonly
          :placeholder="lang === 'zh' ? '回答内容将显示在这里' : 'Answer will appear here'"
        />
      </section>
    </div>

    <div v-if="result" class="result-grid">
      <section>
        <div class="section-head">
          <h3>{{ lang === "zh" ? "检索轨迹" : "Trace" }}</h3>
        </div>
        <el-timeline>
          <el-timeline-item
            v-for="(step, index) in result.trace || []"
            :key="index"
          >
            <strong>{{ step.name }}</strong>
            <div class="muted">{{ step.detail }}</div>
          </el-timeline-item>
        </el-timeline>
      </section>

      <section>
        <div class="section-head">
          <h3>{{ lang === "zh" ? "知识库命中" : "Retrieved KB" }}</h3>
          <span v-if="result.kbHits && result.kbHits.length" class="muted">
            {{ lang === "zh" ? `命中 ${result.kbHits.length} 条` : `${result.kbHits.length} hits` }}
          </span>
        </div>
        <el-empty
          v-if="!result.kbHits || result.kbHits.length === 0"
          :description="lang === 'zh' ? '暂无知识库命中' : 'No KB hits'"
        />
        <div v-else class="hit-list">
          <article v-for="(hit, index) in result.kbHits" :key="index" class="hit-item">
            <div class="hit-title">{{ hit.title || hit.sourceId || (lang === "zh" ? `命中 ${index + 1}` : `Hit ${index + 1}`) }}</div>
            <p>{{ hit.preview }}</p>
          </article>
        </div>
      </section>

      <section>
        <div class="section-head">
          <h3>{{ lang === "zh" ? "相关案例" : "Related Cases" }}</h3>
          <span v-if="result.relatedCases && result.relatedCases.length" class="muted">
            {{ lang === "zh" ? `共 ${result.relatedCases.length} 条` : `${result.relatedCases.length} found` }}
          </span>
        </div>
        <el-empty
          v-if="!result.relatedCases || result.relatedCases.length === 0"
          :description="lang === 'zh' ? '暂无相关案例' : 'No related cases'"
        />
        <div v-else class="case-list">
          <article
            v-for="(item, index) in result.relatedCases"
            :key="item.case_id || index"
            class="case-card"
          >
            <div class="case-title">{{ item.case_name || item.case_id || (lang === "zh" ? `案例 ${index + 1}` : `Case ${index + 1}`) }}</div>
            <div class="case-meta">
              <el-tag v-if="item.country" size="small" effect="plain">{{ item.country }}</el-tag>
              <span v-if="item.judgement_date" class="muted">{{ item.judgement_date }}</span>
            </div>
            <p v-if="item.tags" class="case-tags">{{ item.tags }}</p>
            <div class="case-foot">
              <span v-if="item.citationCount != null" class="muted">{{ lang === "zh" ? `引用 ${item.citationCount}` : `Cited ${item.citationCount}` }}</span>
              <span v-if="item.favoritedCount != null" class="muted">{{ lang === "zh" ? `收藏 ${item.favoritedCount}` : `★ ${item.favoritedCount}` }}</span>
              <el-button
                v-if="item.original_document_url"
                link
                type="primary"
                size="small"
                class="case-original-btn"
                @click="openOriginal(item.original_document_url)"
              >
                {{ lang === "zh" ? "原文" : "Original doc" }}
              </el-button>
            </div>
          </article>
        </div>
      </section>
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
    // 与顶部导航栏 / 左侧侧边栏共用的全局语言状态（zh/en）
    const lang = computed(() => store.getters.lang);
    const loading = ref(false);
    const result = ref(null);
    const answer = computed(() => (result.value && result.value.answer ? result.value.answer : ""));
    const form = reactive({
      question: "",
      country: "",
      sources: "US,EU,JPN",
      period: null,
      topK: 5,
      refreshCases: true,
    });

    const openOriginal = (url) => {
      if (url) window.open(url, "_blank", "noopener");
    };

    const resetForm = () => {
      form.question = "";
      form.country = "";
      form.sources = "US,EU,JPN";
      form.period = null;
      form.topK = 5;
      form.refreshCases = true;
      result.value = null;
    };

    const askAgent = async () => {
      if (!form.question.trim()) {
        ElMessage.warning(lang.value === "zh" ? "请输入问题" : "Please enter a question");
        return;
      }

      // 点击 Ask 后立即清空上一次的回复内容
      result.value = null;
      loading.value = true;
      try {
        const payload = {
          ...form,
          question: form.question.trim(),
          language: lang.value,
          userId: Number(getAuth("userId") || 0),
        };
        const res = await api.agentAsk(payload);
        if (res.code === 200 && res.data) {
          result.value = res.data;
        } else {
          ElMessage.error(res.message || (lang.value === "zh" ? "智能问答请求失败" : "Agent request failed"));
        }
      } finally {
        loading.value = false;
      }
    };

    return {
      form,
      result,
      answer,
      loading,
      lang,
      ChatDotRound,
      Delete,
      resetForm,
      askAgent,
      openOriginal,
    };
  },
};
</script>

<style scoped>
.agent-page {
  min-height: calc(100vh - 64px);
  padding: 18px;
  background: #f6f8fb;
}

.agent-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 16px;
}

.agent-toolbar h2,
.agent-toolbar p,
.answer-head h3,
.result-grid h3 {
  margin: 0;
}

.agent-toolbar h2 {
  font-size: 22px;
  color: #1f2937;
}

.agent-toolbar p,
.muted {
  color: #6b7280;
  font-size: 13px;
}

.toolbar-controls,
.actions,
.answer-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.agent-grid {
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(360px, 1.1fr);
  gap: 16px;
}

.ask-panel,
.answer-panel,
.result-grid section {
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  padding: 14px;
}

.actions {
  justify-content: flex-end;
  margin-top: 12px;
}

.answer-head {
  justify-content: space-between;
  margin-bottom: 10px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  margin-bottom: 10px;
}

.result-grid {
  display: grid;
  grid-template-columns: 0.8fr 1.1fr 1.1fr;
  gap: 16px;
  margin-top: 16px;
}

.hit-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow: auto;
}

.hit-item {
  border: 1px solid #edf0f5;
  border-radius: 6px;
  padding: 10px;
}

.hit-title {
  font-weight: 600;
  color: #1f2937;
  margin-bottom: 6px;
}

.hit-item p {
  margin: 0;
  color: #4b5563;
  line-height: 1.5;
  font-size: 13px;
}

.filters {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(150px, 1fr));
  gap: 10px;
  margin-top: 12px;
}

.filter-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;
}

.filter-item label {
  font-size: 12px;
  color: #6b7280;
  line-height: 1.4;
}

.filter-item :deep(.el-select),
.filter-item :deep(.el-input),
.filter-item :deep(.el-input-number) {
  width: 100%;
}

.case-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-height: 360px;
  overflow-y: auto;
  padding-right: 4px;
}

.case-card {
  border: 1px solid #edf0f5;
  border-radius: 8px;
  padding: 10px 12px;
  background: #fcfdff;
  display: flex;
  flex-direction: column;
  gap: 6px;
  min-width: 0;
}

.case-card:hover {
  border-color: #c7d2fe;
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.08);
}

.case-title {
  font-weight: 600;
  color: #1f2937;
  font-size: 13px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.case-meta {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 12px;
}

.case-tags {
  margin: 0;
  color: #4b5563;
  font-size: 12px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.case-foot {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 12px;
  border-top: 1px dashed #eef1f6;
  padding-top: 6px;
}

.case-original-btn {
  margin-left: auto;
}

.hit-list::-webkit-scrollbar,
.case-list::-webkit-scrollbar {
  width: 6px;
}

.hit-list::-webkit-scrollbar-thumb,
.case-list::-webkit-scrollbar-thumb {
  background: #d1d5db;
  border-radius: 3px;
}

.hit-list::-webkit-scrollbar-track,
.case-list::-webkit-scrollbar-track {
  background: transparent;
}

@media (max-width: 980px) {
  .agent-toolbar,
  .toolbar-controls,
  .agent-grid,
  .result-grid {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }
}
</style>
