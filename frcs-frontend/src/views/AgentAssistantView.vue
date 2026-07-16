<template>
  <div class="agent-page">
    <div class="agent-toolbar">
      <div>
        <h2>Legal RAG Agent</h2>
        <p>RAG answer, case search trace, and retrieved evidence.</p>
      </div>
      <div class="toolbar-controls">
        <el-select v-model="form.language" class="control-select">
          <el-option label="Chinese" value="zh" />
          <el-option label="English" value="en" />
        </el-select>
        <el-switch
          v-model="form.refreshCases"
          active-text="Search first"
          inactive-text="KB only"
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
          placeholder="Ask a legal question"
        />
        <div class="filters">
          <el-select v-model="form.country" clearable placeholder="Country">
            <el-option label="US" value="US" />
            <el-option label="EU" value="EU" />
            <el-option label="JPN" value="JPN" />
          </el-select>
          <el-input v-model="form.sources" placeholder="Sources: US,EU,JPN" />
          <el-select v-model="form.period" clearable placeholder="Period">
            <el-option label="1 year" :value="1" />
            <el-option label="3 years" :value="3" />
            <el-option label="5 years" :value="5" />
            <el-option label="10 years" :value="10" />
          </el-select>
          <el-input-number v-model="form.topK" :min="1" :max="10" />
        </div>
        <div class="actions">
          <el-button :icon="Delete" @click="resetForm">Clear</el-button>
          <el-button type="primary" :icon="ChatDotRound" :loading="loading" @click="askAgent">
            Ask
          </el-button>
        </div>
      </section>

      <section class="answer-panel">
        <div class="answer-head">
          <h3>Answer</h3>
          <el-tag v-if="result" type="success">{{ result.route }}</el-tag>
        </div>
        <el-input
          :model-value="answer"
          type="textarea"
          :rows="13"
          resize="none"
          readonly
          placeholder="Answer will appear here"
        />
      </section>
    </div>

    <div v-if="result" class="result-grid">
      <section>
        <h3>Trace</h3>
        <el-timeline>
          <el-timeline-item
            v-for="(step, index) in result.trace || []"
            :key="index"
            :timestamp="step.status"
            placement="top"
          >
            <strong>{{ step.name }}</strong>
            <div class="muted">{{ step.detail }}</div>
          </el-timeline-item>
        </el-timeline>
      </section>

      <section>
        <h3>Retrieved KB</h3>
        <el-empty v-if="!result.kbHits || result.kbHits.length === 0" description="No KB hits" />
        <div v-else class="hit-list">
          <article v-for="(hit, index) in result.kbHits" :key="index" class="hit-item">
            <div class="hit-title">{{ hit.title || hit.sourceId || `Hit ${index + 1}` }}</div>
            <p>{{ hit.preview }}</p>
          </article>
        </div>
      </section>

      <section>
        <h3>Related Cases</h3>
        <el-empty v-if="!result.relatedCases || result.relatedCases.length === 0" description="No cases" />
        <div v-else class="hit-list">
          <article v-for="item in result.relatedCases" :key="item.caseId" class="hit-item">
            <div class="hit-title">{{ item.caseName || item.caseId }}</div>
            <p>{{ item.summary }}</p>
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
import api from "../api";
import { getAuth } from "../utils/authStorage";

export default {
  name: "AgentAssistantView",
  setup() {
    const loading = ref(false);
    const result = ref(null);
    const answer = computed(() => (result.value && result.value.answer ? result.value.answer : ""));
    const form = reactive({
      question: "",
      language: "zh",
      country: "",
      sources: "US,EU,JPN",
      period: null,
      topK: 5,
      refreshCases: false,
    });

    const resetForm = () => {
      form.question = "";
      form.country = "";
      form.sources = "US,EU,JPN";
      form.period = null;
      form.topK = 5;
      form.refreshCases = false;
      result.value = null;
    };

    const askAgent = async () => {
      if (!form.question.trim()) {
        ElMessage.warning("Please enter a question");
        return;
      }

      loading.value = true;
      try {
        const payload = {
          ...form,
          question: form.question.trim(),
          userId: Number(getAuth("userId") || 0),
        };
        const res = await api.agentAsk(payload);
        if (res.code === 200 && res.data) {
          result.value = res.data;
        } else {
          ElMessage.error(res.message || "Agent request failed");
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
      ChatDotRound,
      Delete,
      resetForm,
      askAgent,
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
.filters,
.actions,
.answer-head {
  display: flex;
  align-items: center;
  gap: 10px;
}

.control-select {
  width: 130px;
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

.filters {
  display: grid;
  grid-template-columns: 120px minmax(160px, 1fr) 130px 120px;
  margin-top: 12px;
}

.actions {
  justify-content: flex-end;
  margin-top: 12px;
}

.answer-head {
  justify-content: space-between;
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

@media (max-width: 980px) {
  .agent-toolbar,
  .toolbar-controls,
  .agent-grid,
  .result-grid {
    display: flex;
    flex-direction: column;
    align-items: stretch;
  }

  .filters {
    grid-template-columns: 1fr;
  }
}
</style>
