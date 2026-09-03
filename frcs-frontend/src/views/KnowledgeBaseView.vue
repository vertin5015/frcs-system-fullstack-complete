<template>
  <div class="kb-page">
    <div class="kb-card">
      <h2>{{ lang === "zh" ? "本地知识库" : "Local Knowledge Base" }}</h2>

      <section class="row manual-row">
        <h3>{{ lang === "zh" ? "手动入库" : "Manual Ingest" }}</h3>
        <div class="manual-fields">
          <el-input class="field-source-id" v-model="manual.sourceId" :placeholder="lang === 'zh' ? '来源ID（可选）' : 'Source ID (optional)'" />
          <el-input class="field-title" v-model="manual.title" :placeholder="lang === 'zh' ? '标题（可选）' : 'Title (optional)'" />
        </div>
        <el-input class="field-content" v-model="manual.content" type="textarea" :rows="6" :placeholder="lang === 'zh' ? '粘贴要入库的文本' : 'Paste text to ingest'" />
        <div class="row-action"><el-button type="primary" :loading="loadingManual" @click="doManualIngest">{{ lang === "zh" ? "执行入库" : "Ingest" }}</el-button></div>
      </section>

      <section class="row crawler-row">
        <h3>{{ lang === "zh" ? "从爬取源入库" : "Ingest from crawler sources" }}</h3>
        <el-input class="field-keyword" v-model="crawler.keyword" :placeholder="lang === 'zh' ? '关键词（必填）' : 'Keyword (required)'" />
        <div class="inline crawler-options">
          <el-select class="field-country" v-model="crawler.country" clearable :placeholder="lang === 'zh' ? '国家(可选)' : 'Country(optional)'">
            <el-option label="US" value="US" /><el-option label="EU" value="EU" /><el-option label="JPN" value="JPN" />
          </el-select>
          <el-select class="field-period" v-model="crawler.period" clearable :placeholder="lang === 'zh' ? '时间(可选)' : 'Period(optional)'">
            <el-option :label="lang === 'zh' ? '最近一年' : '1 year'" :value="1" /><el-option :label="lang === 'zh' ? '最近三年' : '3 years'" :value="3" /><el-option :label="lang === 'zh' ? '最近五年' : '5 years'" :value="5" /><el-option :label="lang === 'zh' ? '最近十年' : '10 years'" :value="10" />
          </el-select>
          <el-input-number class="field-limit" v-model="crawler.limit" :min="1" :max="500" />
        </div>
        <el-input class="field-sources" v-model="crawler.sources" :placeholder="lang === 'zh' ? '数据源如 US,EU,JPN（可选）' : 'sources like US,EU,JPN(optional)'" />
        <div class="row-action"><el-button type="primary" :loading="loadingCrawler" @click="doCrawlerIngest">{{ lang === "zh" ? "爬取并入库" : "Crawl & Ingest" }}</el-button></div>
      </section>

      <section class="row query-row">
        <h3>{{ lang === "zh" ? "知识库问答" : "KB Query" }}</h3>
        <el-input class="field-question" v-model="query.question" type="textarea" :rows="3" :placeholder="lang === 'zh' ? '输入你的问题' : 'Ask a question'" />
        <div class="inline query-options">
          <el-input-number class="field-top-k" v-model="query.topK" :min="1" :max="12" />
          <div class="row-action"><el-button type="success" :loading="loadingQuery" @click="doQuery">{{ lang === "zh" ? "提问" : "Ask" }}</el-button></div>
        </div>
        <el-input class="field-answer" v-model="answer" type="textarea" :rows="8" readonly :placeholder="lang === 'zh' ? '回答会显示在这里' : 'Answer will appear here'" />
      </section>
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
.kb-page { min-height:calc(100vh - 68px); } .kb-card { background:var(--frcs-surface); border:1px solid var(--frcs-border); border-radius:var(--frcs-radius-md); box-shadow:var(--frcs-shadow-sm); padding:24px; } .kb-card h2 { margin-top:0; font-size:24px; } .row { gap:12px; margin-bottom:18px; } .inline { gap:10px; } @media (max-width:768px){.kb-card{padding:16px}.inline{flex-wrap:wrap}}

/* Form rhythm and content-fit field widths */
.kb-card { max-width: 100%; }
.kb-card .row { display: flex; flex-direction: column; gap: 12px; padding: 18px 0 22px; margin: 0; border-bottom: 1px solid var(--frcs-border); }
.kb-card .row:last-child { border-bottom: 0; padding-bottom: 0; }
.kb-card .row h3 { margin: 0 0 2px; color: var(--frcs-primary); font-size: 16px; }
.kb-card .el-input, .kb-card .el-select, .kb-card .el-input-number { width: auto; max-width: 100%; }
.manual-fields { display: flex; flex-wrap: wrap; gap: 12px; width: 100%; }
.manual-fields .field-source-id { flex: 0 1 220px; }
.manual-fields .field-title { flex: 1 1 360px; }
.field-content, .field-question, .field-answer, .field-keyword, .field-sources { width: 100% !important; }
.crawler-options, .query-options { display: flex; align-items: center; flex-wrap: wrap; gap: 12px; width: 100%; }
.crawler-options .field-country, .crawler-options .field-period { flex: 0 1 180px; width: 180px; }
.crawler-options .field-limit, .query-options .field-top-k { flex: 0 0 120px; width: 120px; }
.row-action { display: flex; justify-content: flex-start; padding-top: 2px; }
.row-action .el-button { min-width: 112px; }
@media (max-width: 680px) {
  .manual-fields { flex-direction: column; }
  .manual-fields .field-source-id, .manual-fields .field-title { width: 100%; flex-basis: auto; }
  .crawler-options, .query-options { align-items: stretch; }
  .crawler-options .field-country, .crawler-options .field-period, .crawler-options .field-limit, .query-options .field-top-k { width: 100%; flex-basis: auto; }
}</style>




