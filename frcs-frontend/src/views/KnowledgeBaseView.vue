<template>
  <div class="kb-page">
    <div class="kb-card">
      <h2>{{ lang === "zh" ? "本地知识库" : "Local Knowledge Base" }}</h2>

      <div class="row">
        <h3>{{ lang === "zh" ? "手动入库" : "Manual Ingest" }}</h3>
        <el-input v-model="manual.sourceId" :placeholder="lang === 'zh' ? '来源ID（可选）' : 'Source ID (optional)'" />
        <el-input v-model="manual.title" :placeholder="lang === 'zh' ? '标题（可选）' : 'Title (optional)'" />
        <el-input v-model="manual.content" type="textarea" :rows="6" :placeholder="lang === 'zh' ? '粘贴要入库的文本' : 'Paste text to ingest'" />
        <el-button type="primary" :loading="loadingManual" @click="doManualIngest">{{ lang === "zh" ? "执行入库" : "Ingest" }}</el-button>
      </div>

      <div class="row">
        <h3>{{ lang === "zh" ? "从爬取源入库" : "Ingest from crawler sources" }}</h3>
        <el-input v-model="crawler.keyword" :placeholder="lang === 'zh' ? '关键词（必填）' : 'Keyword (required)'" />
        <div class="inline">
          <el-select v-model="crawler.country" clearable :placeholder="lang === 'zh' ? '国家(可选)' : 'Country(optional)'">
            <el-option label="US" value="US" />
            <el-option label="EU" value="EU" />
            <el-option label="JPN" value="JPN" />
          </el-select>
          <el-select v-model="crawler.period" clearable :placeholder="lang === 'zh' ? '时间(可选)' : 'Period(optional)'">
            <el-option :label="lang === 'zh' ? '最近一年' : '1 year'" :value="1" />
            <el-option :label="lang === 'zh' ? '最近三年' : '3 years'" :value="3" />
            <el-option :label="lang === 'zh' ? '最近五年' : '5 years'" :value="5" />
            <el-option :label="lang === 'zh' ? '最近十年' : '10 years'" :value="10" />
          </el-select>
          <el-input-number v-model="crawler.limit" :min="1" :max="500" />
        </div>
        <el-input v-model="crawler.sources" :placeholder="lang === 'zh' ? '数据源如 US,EU,JPN（可选）' : 'sources like US,EU,JPN(optional)'" />
        <el-button type="primary" :loading="loadingCrawler" @click="doCrawlerIngest">{{ lang === "zh" ? "爬取并入库" : "Crawl & Ingest" }}</el-button>
      </div>

      <div class="row">
        <h3>{{ lang === "zh" ? "知识库问答" : "KB Query" }}</h3>
        <el-input v-model="query.question" type="textarea" :rows="3" :placeholder="lang === 'zh' ? '输入你的问题' : 'Ask a question'" />
        <div class="inline">
          <el-input-number v-model="query.topK" :min="1" :max="12" />
          <el-button type="success" :loading="loadingQuery" @click="doQuery">{{ lang === "zh" ? "提问" : "Ask" }}</el-button>
        </div>
        <el-input v-model="answer" type="textarea" :rows="8" readonly :placeholder="lang === 'zh' ? '回答会显示在这里' : 'Answer will appear here'" />
      </div>
    </div>
  </div>
</template>

<script>
import { computed, reactive, ref } from "vue";
import { useStore } from "vuex";
import { ElMessage } from "element-plus";
import api from "../api";

export default {
  name: "KnowledgeBaseView",
  setup() {
    const store = useStore();
    const lang = computed(() => store.getters.lang);
    const loadingManual = ref(false);
    const loadingCrawler = ref(false);
    const loadingQuery = ref(false);
    const answer = ref("");

    const manual = reactive({
      sourceId: "",
      title: "",
      content: "",
      language: "zh",
    });
    const crawler = reactive({
      keyword: "",
      country: "",
      period: null,
      sources: "",
      limit: 50,
    });
    const query = reactive({
      question: "",
      language: "zh",
      topK: 5,
    });

    const doManualIngest = async () => {
      if (!manual.content || !manual.content.trim()) {
        ElMessage.warning(lang.value === "zh" ? "请输入入库内容" : "Please input content");
        return;
      }
      loadingManual.value = true;
      try {
        const res = await api.kbIngest({ ...manual, language: lang.value });
        if (res.code === 200) {
          ElMessage.success(res.message || (lang.value === "zh" ? "入库成功" : "Ingested"));
        } else {
          ElMessage.error(res.message || (lang.value === "zh" ? "入库失败" : "Ingest failed"));
        }
      } finally {
        loadingManual.value = false;
      }
    };

    const doCrawlerIngest = async () => {
      if (!crawler.keyword || !crawler.keyword.trim()) {
        ElMessage.warning(lang.value === "zh" ? "请输入关键词" : "Please input keyword");
        return;
      }
      loadingCrawler.value = true;
      try {
        const res = await api.kbIngestCrawler({ ...crawler });
        if (res.code === 200) {
          ElMessage.success(res.message || (lang.value === "zh" ? "爬取源入库成功" : "Crawler ingest success"));
        } else {
          ElMessage.error(res.message || (lang.value === "zh" ? "爬取源入库失败" : "Crawler ingest failed"));
        }
      } finally {
        loadingCrawler.value = false;
      }
    };

    const doQuery = async () => {
      if (!query.question || !query.question.trim()) {
        ElMessage.warning(lang.value === "zh" ? "请输入问题" : "Please input question");
        return;
      }
      loadingQuery.value = true;
      try {
        const res = await api.kbQuery({ ...query, language: lang.value });
        if (res.code === 200 && res.data) {
          answer.value = res.data.answer || "";
        } else {
          ElMessage.error(res.message || (lang.value === "zh" ? "查询失败" : "Query failed"));
        }
      } finally {
        loadingQuery.value = false;
      }
    };

    return {
      lang,
      manual,
      crawler,
      query,
      answer,
      loadingManual,
      loadingCrawler,
      loadingQuery,
      doManualIngest,
      doCrawlerIngest,
      doQuery,
    };
  },
};
</script>

<style scoped>
.kb-page {
  padding: 16px;
}
.kb-card {
  background: #fff;
  border: 1px solid #ebeef5;
  border-radius: 10px;
  padding: 16px;
}
.row {
  margin-bottom: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.inline {
  display: flex;
  gap: 10px;
  align-items: center;
}
</style>
