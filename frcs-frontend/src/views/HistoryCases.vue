<template>
  <div class="history-container">
<div class="left-panel">
      <div class="panel-content">
        <h3 style="margin-bottom: 18px">{{ lang === "zh" ? "筛选" : "Filter" }}</h3>
        <div class="filter-group">
          <div class="filter-label">{{ lang === "zh" ? "国家" : "Country" }}</div>
          <select v-model="filterCountry" class="filter-select">
            <option v-for="item in countryOptions" :key="item.value" :value="item.value">
              {{ lang === "zh" ? item.label : item.enLabel }}
            </option>
          </select>
        </div>
        <div class="filter-group">
          <div class="filter-label">{{ lang === "zh" ? "判决时间" : "Judgment Time" }}</div>
          <select v-model="filterTime" class="filter-select">
            <option value="null">{{ lang === "zh" ? "全部" : "All" }}</option>
            <option value="1">{{ lang === "zh" ? "最近一年" : "Last 1 year" }}</option>
            <option value="3">{{ lang === "zh" ? "最近三年" : "Last 3 years" }}</option>
            <option value="5">{{ lang === "zh" ? "最近五年" : "Last 5 years" }}</option>
            <option value="10">{{ lang === "zh" ? "最近十年" : "Last 10 years" }}</option>
          </select>
        </div>
      </div>
    </div>
    <div class="card-area">
      <div class="history-header">{{ lang === "zh" ? "历史记录" : "History Records" }}</div>
      <div class="card-grid" v-loading="loading" element-loading-text="正在加载历史记录..." element-loading-spinner="Loading" element-loading-background="rgba(255, 255, 255, 0.8)">
        <template v-if="!loading && cases.length === 0">
          <div class="no-cases-message">
            {{ lang === "zh" ? "暂无历史记录。" : "No history records found." }}
          </div>
        </template>
        <template v-else>
          <div v-for="item in cases" :key="item.id" class="case-card-new">
            <div class="case-card-content">
              <div class="case-card-header" style="display: flex; justify-content: space-between; align-items: center"><div class="case-title-wrap">
                  <el-tooltip class="box-item" effect="dark" :content="item.caseName" placement="top-start" popper-class="case-title-tooltip">
                    <span class="case-title">{{ item.caseName }}</span>
                  </el-tooltip>
                </div>
                <el-tooltip class="box-item" effect="dark" :content="lang === 'zh' ? '原卷链接' : 'Original Link'" placement="top-start">
                  <i class="iconfont icon-lianjie custom-hover-purple" style="font-size: 20px; cursor: pointer" @click="openOriginalLink(item.originalDocumentUrl)"></i>
                </el-tooltip>
              </div>
              <div class="case-card-row case-card-meta-row"><span class="case-country meta-item"><small>{{ lang === "zh" ? "国家" : "Country" }}</small>{{ showCountry(item.country) }} </span>

                <span class="case-date meta-item"><small>{{ lang === "zh" ? "案件号" : "Case ID" }}</small><b>{{ item.caseId }}</b></span>
              </div>
              <div class="case-card-row case-card-summary-row"><span class="case-tags"><small>{{ lang === "zh" ? "关键词" : "Keywords" }}</small>
                  <span class="tags-value">{{ item.tags || "" }}</span>
                </span>
                <span class="case-date meta-item"><small>{{ lang === "zh" ? "日期" : "Date" }}</small>{{ item.judgementDate }}</span>
                <span class="case-link card-action-link" @click="showCase(item)">{{ lang === "zh" ? "查看" : "View" }}</span>
              </div>
            </div>
          </div>
          <div v-for="n in Math.max(pageSize - cases.length, 0)" :key="'empty' + n" class="case-card-new empty-card"></div>
        </template>
      </div>
      <div class="history-pagination">
        <el-pagination :page-size="pageSize" :pager-count="11" layout="prev, pager, next" :total="totalCasesCount" v-model:current-page="page" />
      </div>
    </div>
    <el-dialog v-model="dialogVisible" :title="dialogCase?.case_name" width="800px" top="60px" :close-on-click-modal="false">
      <div class="dialog-lang-row">
        <span class="dialog-lang-label">{{ lang === "zh" ? "摘要语言" : "Summary language" }}</span>
        <el-switch
          v-model="switchLang"
          :active-value="'en'"
          :inactive-value="'zh'"
          active-text="EN"
          inactive-text="中文"
          size="small"
          @change="onDialogLangToggle"
        />
      </div>
      <div v-loading="detailLoading" element-loading-text="正在加载案件详情..." element-loading-spinner="Loading" element-loading-background="rgba(255, 255, 255, 0.8)" class="dialog-detail-content">
        <div v-html="caseDetailHtml"></div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted } from "vue";
import { useStore } from "vuex";
import MarkdownIt from "markdown-it";
// 导入您的API模块，根据您的实际路径修改
import api from "@/api"; // 假设您的api模块在 src/api/index.js
import { ElNotification } from "element-plus";
import { getAuth } from "../utils/authStorage";
import { ensureCaseSummary } from "../utils/caseSummaryFlow";

export default {
  name: "HistoryCases",
  setup() {
    const store = useStore();
    const lang = computed(() => store.getters.lang);
    const switchLang = computed({
      get: () => store.state.lang,
      set: (val) => {
        store.commit("setLang", val);
        localStorage.setItem("lang", val);
      },
    });

    const countryOptions = [
      { value: null, label: "全部", enLabel: "All" },
      { value: "US", label: "美国", enLabel: "USA" },
      { value: "JPN", label: "日本", enLabel: "Japan" },
      { value: "EU", label: "欧盟", enLabel: "European Union" },
    ];

    // 存储从后端获取的案件数据
    const cases = ref([]);
    // 总案件数，用于分页组件
    const totalCasesCount = ref(0);
    // 加载状态
    const loading = ref(false); // 控制整个历史案件列表的加载状态

    // 过滤器
    const filterCountry = ref(null);
    const filterTime = ref(null);


    // 分页
    const page = ref(1);
    // 根据您的要求，pageSize 固定为 16
    const pageSize = ref(16);

    // 弹窗相关
    const dialogVisible = ref(false);
    const dialogCase = ref(null);
    const md = new MarkdownIt();
    const caseDetailHtml = ref("");
    const detailLoading = ref(false); // 控制案件详情弹窗内容的加载状态
    const dialogRequestSeq = ref(0); // 快速切换语言时丢弃过期请求结果

    const openOriginalLink = (url) => {
      if (!url) {
        ElNotification({
          title: lang.value === "zh" ? "错误" : "Error",
          message: lang.value === "zh" ? "无效的链接。" : "Invalid URL.",
          type: "error",
        });
        return;
      }

      // 在新标签页打开链接
      window.open(url, "_blank");
    };

    /**
     * 获取历史案件数据
     */
    const fetchHistoryCases = async () => {
      loading.value = true; // 设置加载状态为 true
      try {
        const userId = getAuth("userId"); // 从本地存储获取 userId
        if (!userId) {
          console.warn("User ID not found in storage. Cannot fetch history cases.");
          cases.value = [];
          totalCasesCount.value = 0;
          // 可以考虑给用户一个提示，但这里由模板中的 no-cases-message 处理
          return;
        }
        const params = {
          country: filterCountry.value,
          period: filterTime.value,
          pagenum: page.value,
          pagesize: pageSize.value,
          language: lang.value,
          userId: parseInt(getAuth("userId") || "0", 10),
        };
        //console.log("Fetch history cases params:", params);
        console.log("获取浏览历史参数:", params);
        const response = await api.getHistoryCases(lang.value, filterCountry.value, parseInt(getAuth("userId") || "0", 10), filterTime.value, page.value, pageSize.value); // 调用API

        if (response.code === 200 && response.data) {
          cases.value = response.data.browseHistoryInfoList || [];
          totalCasesCount.value = response.data.totalCount || 0;
        } else {
          // 处理API返回的错误
          const errorMessage = response.message || (lang.value === "zh" ? "获取历史记录失败" : "Failed to fetch history records");
          console.error("API Error:", errorMessage);
          cases.value = [];
          totalCasesCount.value = 0;
        }
        //console.log("Fetched history cases:", cases.value);
      } catch (error) {
        console.error("Error fetching history cases:", error);
        cases.value = [];
        totalCasesCount.value = 0;
      } finally {
        loading.value = false; // 无论成功或失败，最后都要将加载状态设置为 false
      }
    };

    const buildDialogHtml = (item, detailedContent) => {
      const displayCountry =
        (countryOptions.find((opt) => opt.value === item.country) || {})[lang.value === "zh" ? "label" : "enLabel"] || item.country;
      return md.render(`
### ${lang.value === "zh" ? "智能案件分析" : "Intelligent Case Analysis"}

**${lang.value === "zh" ? "案件名称：" : "Case Name:"}** ${item.caseName}

**${lang.value === "zh" ? "国家：" : "Country:"}** ${displayCountry}

**${lang.value === "zh" ? "日期：" : "Date:"}** ${item.judgementDate}
**${lang.value === "zh" ? "关键词：" : "Tags:"}** ${item.tags}

---

${detailedContent || (lang.value === "zh" ? "未找到详细分析内容。" : "No detailed analysis content found.")}
      `);
    };

    const loadDialogDetail = async (item) => {
      if (!item) {
        return;
      }
      const seq = ++dialogRequestSeq.value;
      dialogCase.value = item;
      dialogVisible.value = true; // 先打开弹窗，以便显示加载状态
      caseDetailHtml.value = lang.value === "zh" ? "加载案件详情中..." : "Loading case details..."; // 设置加载提示
      detailLoading.value = true; // 设置详情加载状态为 true

      try {
        const detailResult = await ensureCaseSummary({
          caseId: item.caseId,
          language: lang.value,
          userId: parseInt(getAuth("userId") || "0", 10),
        });
        if (seq !== dialogRequestSeq.value) {
          return;
        }
        if (detailResult.ok) {
          caseDetailHtml.value = buildDialogHtml(item, detailResult.content);
        } else {
          caseDetailHtml.value = `<p style="color: red;">${
            detailResult.error || (lang.value === "zh" ? "获取案件详情失败" : "Failed to fetch case details")
          }</p>`;
          console.error("Case Detail API Error:", detailResult.error);
        }
      } catch (error) {
        console.error("Error fetching case detail:", error);
        if (seq === dialogRequestSeq.value) {
          caseDetailHtml.value = `<p style="color: red;">${
            lang.value === "zh" ? "加载案件详情时发生网络错误。" : "Network error occurred while loading case details."
          }</p>`;
        }
      } finally {
        if (seq === dialogRequestSeq.value) {
          detailLoading.value = false; // 详情加载完成
        }
      }
    };

    const showCase = (item) => {
      loadDialogDetail(item);
    };

    const onDialogLangToggle = () => {
      if (dialogCase.value) {
        loadDialogDetail(dialogCase.value);
      }
    };

    // 监听过滤条件和页码的变化，自动重新获取数据
    watch(
      [page, filterCountry, filterTime, lang],
      () => {
        // 当筛选条件或语言变化时，重置页码到第一页
        // 但对于单纯的页码变化，不重置页码
        // 这里的逻辑可以根据实际需求调整，例如：
        // if (oldFilterCountry !== filterCountry.value || oldFilterTime !== filterTime.value || oldLang !== lang.value) {
        //   page.value = 1;
        // }
        fetchHistoryCases();
      },
      { immediate: true } // immediate: true 使得组件初次加载时也会立即执行一次 fetchHistoryCases
    );

    const showCountry = (country) => {
      if (country === "US") return lang.value === "zh" ? "美国" : "USA";
      else if (country === "JPN") return lang.value === "zh" ? "日本" : "Japan";
      else if (country === "EU") return lang.value === "zh" ? "欧盟" : "European Union";
      return country || "";
    };
    // 组件挂载时获取数据
    onMounted(() => {
      // 初始加载由 watch 的 immediate 属性处理
      // 确保本地存储中有 userId，如果没有，需要引导用户登录
      //fetchHistoryCases();
    });

    return {
      lang,
      switchLang,

      filterCountry,
      filterTime,
      countryOptions,
      page,
      pageSize, // 暴露 pageSize 给模板
      cases, // 现在直接用 cases
      totalCasesCount, // 暴露 totalCasesCount 给分页组件
      loading, // 暴露 loading 状态
      dialogVisible,
      dialogCase,
      caseDetailHtml,
      showCountry,
      showCase,
      openOriginalLink,
      detailLoading, // 暴露 detailLoading 状态
      onDialogLangToggle,
    };
  },
};
</script>

<style scoped>
.case-card-new { min-height:184px; padding:18px !important; border-radius:16px !important; background:linear-gradient(145deg,#fff 0%,#faf9f7 100%) !important; }
.case-card-header { min-height:44px; padding-bottom:12px; border-bottom:1px solid var(--frcs-border); }
.case-title { color:var(--frcs-primary) !important; font-size:16px !important; line-height:1.45; }
.case-card-meta-row { display:grid !important; grid-template-columns:1fr 1fr; gap:8px !important; margin-top:14px !important; }
.meta-item { display:flex; flex-direction:column; gap:3px; min-width:0; color:var(--frcs-text); font-size:13px; }
.meta-item small,.case-tags small { color:var(--frcs-text-2); font-size:11px; font-weight:700; text-transform:uppercase; letter-spacing:.04em; }
.meta-item b { color:var(--frcs-accent); font-weight:700; }
.case-card-summary-row { align-items:flex-end !important; justify-content:space-between; gap:12px !important; padding-top:11px; border-top:1px solid rgba(229,225,218,.72); }
.case-tags { min-width:0; display:flex; flex-direction:column; gap:4px; }
.tags-value { display:block; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; color:var(--frcs-primary-2) !important; }
.card-action-link { flex:0 0 auto; color:var(--frcs-accent) !important; font-weight:700; }
@media(max-width:640px){ .case-card-meta-row{grid-template-columns:1fr 1fr;} .case-card-summary-row{flex-wrap:wrap;} }
.dialog-lang-row { display:flex; align-items:center; justify-content:flex-end; gap:8px; margin-bottom:8px; }
.dialog-lang-label { font-size:13px; color:var(--frcs-text-2); }
.dialog-detail-content { position:relative; min-height:200px; display:flex; align-items:center; justify-content:center; }
</style>




