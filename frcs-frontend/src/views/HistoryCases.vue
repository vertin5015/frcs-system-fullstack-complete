<template>
  <div class="history-container">
    <div class="history-header">
      {{ lang === "zh" ? "历史记录" : "History Records" }}
    </div>
    <button class="collapse-btn" :class="{ collapsed: isCollapsed }" @click="isCollapsed = !isCollapsed">
      <span v-if="isCollapsed">⮜</span>
      <span v-else>⮞</span>
    </button>
    <div :class="['right-panel', { collapsed: isCollapsed }]" :style="isCollapsed ? { pointerEvents: 'none' } : {}">
      <div v-show="!isCollapsed" class="panel-content">
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
    <div class="card-area" :class="{ expand: isCollapsed }">
      <div class="card-grid" v-loading="loading" element-loading-text="正在加载历史记录..." element-loading-spinner="Loading" element-loading-background="rgba(255, 255, 255, 0.8)">
        <template v-if="!loading && cases.length === 0">
          <div class="no-cases-message">
            {{ lang === "zh" ? "暂无历史记录。" : "No history records found." }}
          </div>
        </template>
        <template v-else>
          <div v-for="item in cases" :key="item.id" class="case-card-new">
            <div class="case-card-content">
              <div class="case-card-header" style="display: flex; justify-content: space-between; align-items: center">
                <el-tooltip class="box-item" effect="dark" :content="item.caseName" placement="top-start">
                  <span class="case-title">{{ item.caseName }}</span>
                </el-tooltip>
                <el-tooltip class="box-item" effect="dark" :content="lang === 'zh' ? '原卷链接' : 'Original Link'" placement="top-start">
                  <i class="iconfont icon-lianjie custom-hover-purple" style="font-size: 20px; cursor: pointer" @click="openOriginalLink(item.originalDocumentUrl)"></i>
                </el-tooltip>
              </div>
              <div class="case-card-row">
                <span class="case-country">{{ showCountry(item.country) }} </span>

                <span class="case-date">{{ lang === "zh" ? "案件号" : "CaseId" }} <span style="color: #0958d9;">{{ item.caseId }}</span></span>
              </div>
              <div class="case-card-row">
                <span class="case-tags">
                  {{ lang === "zh" ? "关键词" : "Tags" }}：<span style="color: #0958d9">{{ item.tags || "" }}</span>
                </span>
                <span style="font-size: 14px; color: #222; margin-left: 15px">{{ item.judgementDate }}</span>
                <span class="case-link" @click="showCase(item)">{{ lang === "zh" ? "查看" : "View" }}</span>
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

export default {
  name: "HistoryCases",
  setup() {
    const store = useStore();
    const lang = computed(() => store.getters.lang);

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
    const isCollapsed = ref(true);

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

    const showCase = async (item) => {
      dialogCase.value = item;
      dialogVisible.value = true; // 先打开弹窗，以便显示加载状态
      caseDetailHtml.value = lang.value === "zh" ? "加载案件详情中..." : "Loading case details..."; // 设置加载提示
      detailLoading.value = true; // 设置详情加载状态为 true

      try {
        // 调用API获取案件的详细分析内容
        const params = {
          caseId: item.caseId, // 使用 item.case_id 作为案件的唯一标识符，因为它在你的后端数据中是唯一的
          language: lang.value, // 语言
        };
        console.log("查看详细分析params:", params);
        const detailResponse = await api.getCaseSummary(item.caseId, lang.value, parseInt(getAuth("userId") || "0", 10)); // 假设API通过caseId获取详情

        if (detailResponse && detailResponse.code === 200 && detailResponse.data) {
          const detailedContent = detailResponse.data;

          // 根据当前语言获取国家名
          const displayCountry = (countryOptions.find((opt) => opt.value === item.country) || {})[lang.value === "zh" ? "label" : "enLabel"] || item.country;

          // 构建弹窗内容，所有静态字符串都根据 lang.value 动态选择
          caseDetailHtml.value = md.render(`
### ${lang.value === "zh" ? "智能案件分析" : "Intelligent Case Analysis"}

**${lang.value === "zh" ? "案件名称：" : "Case Name:"}** ${item.caseName}

**${lang.value === "zh" ? "国家：" : "Country:"}** ${displayCountry}

**${lang.value === "zh" ? "日期：" : "Date:"}** ${item.judgementDate}
**${lang.value === "zh" ? "关键词：" : "Tags:"}** ${item.tags}

---

${detailedContent || (lang.value === "zh" ? "未找到详细分析内容。" : "No detailed analysis content found.")}
          `);
        } else {
          // 处理详情API返回的错误
          const errorMessage = detailResponse && detailResponse.message ? detailResponse.message : lang.value === "zh" ? "获取案件详情失败" : "Failed to fetch case details";
          caseDetailHtml.value = `<p style="color: red;">${errorMessage}</p>`;
          console.error("Case Detail API Error:", errorMessage);
        }
      } catch (error) {
        console.error("Error fetching case detail:", error);
        caseDetailHtml.value = `<p style="color: red;">${lang.value === "zh" ? "加载案件详情时发生网络错误。" : "Network error occurred while loading case details."}</p>`;
      } finally {
        detailLoading.value = false; // 详情加载完成
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
      isCollapsed,
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
    };
  },
};
</script>

<style scoped>
/* 样式保持不变 */
.history-container {
  position: relative;
  width: 100%;
  height: 100%;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: row;
}

.history-header {
  position: absolute;
  top: -2px;
  left: 46px;
  font-size: 18px;
  font-weight: bold;
  color: #409eff;
  z-index: 20;
  letter-spacing: 2px;
  user-select: none;
}

.right-panel {
  position: absolute;
  top: 0;
  right: 0;
  width: 200px;
  height: 100%;
  background: #fff;
  transition: right 0.3s;
  z-index: 10;
  border-left: 1px solid #eee;
  display: flex;
  flex-direction: column;
  padding-top: 40px;
}
.right-panel.collapsed {
  right: -200px;
}
.collapse-btn {
  position: absolute;
  top: 20px;
  right: 184px;
  width: 32px;
  height: 32px;
  background: #409eff;
  color: white;
  border: 1px solid #eee;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(205, 208, 214, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  z-index: 11;
  transition: right 0.3s;
}

/* 折叠时按钮只露出一半 */
.collapse-btn.collapsed {
  right: -16px; /* 负一半宽度，只露出一半 */
}
.panel-content {
  padding: 10px 12px;
  width: 100%;
  box-sizing: border-box;
}
.filter-group {
  margin-bottom: 18px;
}
.filter-label {
  font-size: 15px;
  color: #222;
  margin-bottom: 6px;
  font-weight: 500;
}
.filter-select {
  width: 100%;
  padding: 4px 8px;
  border: 1px solid #eee;
  border-radius: 4px;
  font-size: 14px;
  margin-top: 4px;
  background: #fafbfc;
}
.card-area {
  flex: 1;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  align-items: flex-start;
  transition: padding-right 0.3s;
  padding-right: 200px; /* 预留右侧面板宽度 */
  box-sizing: border-box;
  overflow: hidden;
}
.card-area.expand {
  padding-right: 0 !important;
  transition: padding-right 0.3s;
}
.card-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  grid-template-rows: repeat(4, 1fr); /* 4行 */
  gap: 14px 20px;
  width: 100%;
  height: calc(100% - 60px);
  margin-top: 32px; /* 让卡片整体下移，避免与标题重叠 */
  background: #fff;
  box-sizing: border-box;
  overflow: hidden;
  padding-left: 32px;
  padding-right: 32px;
  position: relative; /* 为 v-loading 提供定位上下文 */
  min-height: 400px; /* 确保在没有案件时也有足够的空间显示加载动画 */
  justify-content: center; /* 水平居中内容 */
  align-items: center; /* 垂直居中内容 */
}
.case-card-new {
  background: #f2f6fc;

  border-radius: 14px;
  cursor: pointer;
  width: 95%;
  height: 95%;
  margin: auto;
  padding: 16px 24px 12px 16px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  overflow: hidden;
  border: 1px solid #e4e7ed;
  box-shadow: 0 1px 2px rgba(30, 96, 255, 0.08), /* 第一层：轻微边缘阴影，带有浅蓝色 */ 0 2px 4px rgba(30, 96, 255, 0.04); /* 第二层：非常浅的扩散阴影，几乎若有若无 */
}
.case-card-new.empty-card {
  background: transparent;
  border: none;
  box-shadow: none;
  cursor: default;
  pointer-events: none;
}
.case-card-content {
  width: 100%;
}
.case-card-header {
  display: flex;
  align-items: center;
  margin-bottom: 6px;
}
.case-title {
  font-weight: 600;
  max-width: 14em; /* 限制最大宽度 */
  white-space: nowrap; /* 防止换行 */
  overflow: hidden; /* 超出部分隐藏 */
  text-overflow: ellipsis; /* 超出部分显示省略号 */
  font-size: 16px;
  color: #222;
}
.case-card-row {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 6px;
}
.case-country {
  font-size: 14px;
  color: #222;
  margin-right: 24px;
}
.case-court {
  font-size: 14px;
  color: #222;
  margin-right: 24px;
}

.case-tags {
  font-size: 14px;
  color: #222;
}
.case-link {
  float: right;
  color: #409eff;
  font-size: 14px;
  font-weight: 500;
  margin-left: auto;
  text-decoration: underline;
  cursor: pointer;
}
.case-link:hover {
  color: #1867c0;
}
.history-pagination {
  width: 100%;
  display: flex;
  justify-content: flex-start;
  align-items: center;
  margin: 20px 0 0 0; /* 让分页栏紧贴卡片区 */

  background: #fff;
  flex-shrink: 0;
  padding-left: 24px;
}

/* 加载和空状态消息样式 */
.loading-message,
.no-cases-message {
  width: 100%;
  text-align: center;
  padding: 50px;
  font-size: 16px;
  color: #666;
  position: absolute; /* 使其能在 v-loading 覆盖下显示 */
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  pointer-events: none; /* 确保不影响 v-loading 的点击事件 */
}

/* 确保加载文本不被截断或断行 */
.card-grid :deep(.el-loading-text),
.dialog-detail-content :deep(.el-loading-text) {
  white-space: nowrap; /* 防止文本换行 */
}

/* 弹窗内容区域样式，提供定位上下文给v-loading */
.dialog-detail-content {
  position: relative;
  min-height: 200px; /* 确保弹窗内容在加载时也有一定高度 */
  display: flex; /* 使用 flexbox 居中加载动画 */
  align-items: center; /* 垂直居中 */
  justify-content: center; /* 水平居中 */
}
.custom-hover-purple {
  cursor: pointer;
  transition: color 0.3s ease; /* 平滑过渡效果 */
  color: #0958d9;
}

.custom-hover-purple:hover {
  color: #52c41a; /* 紫色，例如：#9933CC 是经典的紫色 */
}
</style>
