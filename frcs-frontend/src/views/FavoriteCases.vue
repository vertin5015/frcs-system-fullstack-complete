<template>
  <div class="search-grid-container">
    <div :class="['left-panel', { collapsed: isCollapsed }]">
      <button class="collapse-btn" :class="{ collapsed: isCollapsed }" @click="isCollapsed = !isCollapsed">
        <span v-if="isCollapsed">⮞</span>
        <span v-else>⮜</span>
      </button>
      <div v-show="!isCollapsed" class="panel-content">
        <h3 style="margin-bottom: 18px">{{ lang === "zh" ? "收藏夹" : "Favorites" }}</h3>
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
        <div class="filter-group">
          <div class="filter-label">{{ lang === "zh" ? "提示" : "Tips" }}</div>
          <div style="color: #888; font-size: 13px">
            {{ lang === "zh" ? "点击右侧卡片可查看详情，点击星星可取消收藏。" : "Click the card to view details, click the star to unfavorite." }}
          </div>
        </div>
      </div>
    </div>
    <div style="grid-area: input-area; background-color: transparent"></div>
    <div class="case-list-area">
      <el-scrollbar style="width: 100%; height: 590px">
        <div class="case-list-flex" v-loading="loadingCases" element-loading-text="正在加载收藏案件..." element-loading-spinner="Loading" element-loading-background="rgba(255, 255, 255, 0.8)">
          <template v-if="!loadingCases && cases.length === 0">
            <div class="no-cases-message">
              {{ lang === "zh" ? "暂无收藏案件。" : "No favorite cases found." }}
            </div>
          </template>
          <template v-else>
            <div v-for="(item, index) in cases" :key="item.case_id" class="case-card-new" @click="selectCase(index)" :class="{ 'selected-card': index === selectIndex }">
              <div class="case-card-content">
                <div class="case-card-header">
                  <el-tooltip class="box-item" effect="dark" :content="item.caseName" ; placement="top-start">
                    <span class="case-title">{{ item.caseName }}</span>
                  </el-tooltip>
                  <i
                    class="iconfont icon-shoucang_shixin"
                    :style="{
                      marginLeft: 'auto',
                      fontSize: '20px',
                      color: '#409EFF',
                      cursor: 'pointer',
                    }"
                    @click.stop="unFavorite(item.caseId)"
                    :title="lang === 'zh' ? '取消收藏' : 'Unfavorite'"
                  ></i>
                </div>
                <div class="case-card-row">
                  <span class="case-country">
                    {{ showCountry(item.country) }}
                  </span>
                  <span class="case-date">{{ item.judgementDate }}</span>
                  <span style="margin-left: 26px"
                    >{{ lang === "zh" ? "引用" : "cite" }} <span style="color: #0958d9">{{ item.citationCount }}</span> {{ lang === "zh" ? "次" : "times" }}</span
                  >
                </div>
                <div class="case-card-row">
                  <span class="case-tags">
                    {{ lang === "zh" ? "关键词" : "Tags" }}：
                    <span style="color: #0958d9">{{ item.tags || (lang === "zh" ? "暂无关键词" : "No Tags") }}</span>
                  </span>
                  <span class="case-link" @click="getCaseSummary">{{ lang === "zh" ? "查看" : "View" }}</span>
                </div>
              </div>
            </div>
          </template>
        </div>
      </el-scrollbar>
      <div style="padding: 8px 0; display: flex; justify-content: center; width: 100%">
        <el-pagination
          size="small"
          layout="prev, pager, next"
          :total="totalCasesCount"
          :page-size="pageSize"
          :current-page="page"
          @update:current-page="page = $event"
          @update:page-size="pageSize = $event"
        />
      </div>
    </div>
    <div style="grid-area: case-content-area; background-color: transparent">
      <div
        style="
          border-top-left-radius: 10px;
          border-top-right-radius: 10px;
          height: 45px;
          background-color: white;
          border: 1px solid rgb(221.7, 222.6, 224.4);
          display: flex;
          align-items: center;
          font-size: 20px;
          font-weight: bold;
          padding: 0 24px;
        "
      >
        <span style="font-weight: 600; font-size: 16px; color: #409eff">
          {{ lang === "zh" ? "智能案件分析 :" : "Case Analysis :" }}
        </span>
        <el-tooltip class="box-item" effect="dark" :content="cases[selectIndex]?.caseName || (lang === 'zh' ? '请选择案件' : 'Please select a case')" ; placement="top-start">
          <span
            style="max-width: 700px; font-weight: 600; font-size: 16px; color: #333; margin-left: 10px; flex: 1; overflow: hidden; text-overflow: ellipsis; white-space: nowrap"
            :title="cases[selectIndex]?.caseName || (lang === 'zh' ? '请选择案件' : 'Please select a case')"
          >
            {{ cases[selectIndex]?.caseName || (lang === "zh" ? "请选择案件" : "Please select a case") }}
          </span>
        </el-tooltip>
        <span style="font-weight: 600; font-size: 16px; color: #909399; cursor: pointer; margin-left: 24px" @click="openOriginUrl">
          {{ lang === "zh" ? "查看原始判决文书" : "View Original Judgment" }}
        </span>
        <i class="iconfont icon-xiazai" style="font-size: 16px; margin-left: 16px; color: rgb(121.3, 187.1, 255); cursor: pointer" @click="downloadWord"></i>
      </div>
      <div
        class="case-detail-container"
        style="
          width: 100%;
          border: 1px solid rgb(221.7, 222.6, 224.4);
          border-top: none;
          height: 555px;
          background-color: white;
          border-bottom-right-radius: 10px;
          border-bottom-left-radius: 10px;
          padding: 0px 32px 0 32px;
          box-sizing: border-box;
          overflow-y: auto;
        "
        v-loading="loadingDetail"
        element-loading-text="正在加载案件详情..."
        element-loading-spinner="Loading"
        element-loading-background="rgba(255, 255, 255, 0.8)"
      >
        <div v-html="caseDetailHtml" class="case-detail-content"></div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted } from "vue";
import MarkdownIt from "markdown-it";
import HtmlDocx from "html-docx-js/dist/html-docx";
import { saveAs } from "file-saver";
import { useStore } from "vuex";
import api from "../api/index"; // 确保你有一个 api 模块来处理后端请求
import { ElNotification } from "element-plus";
import { getAuth } from "../utils/authStorage";

export default {
  name: "FavoriteCases",
  setup() {
    // ==============================
    // 1. 状态管理变量
    // ==============================
    const store = useStore();
    const lang = computed(() => store.getters.lang); // 从 Vuex 获取收藏夹搜索参数，用于初始值
    const favoriteSearchParams = computed(() => store.getters.favoriteSearchParams);
    // 加载状态变量
    const loadingCases = ref(false); // 控制案件列表加载动画
    const loadingDetail = ref(false); // 控制案件详情加载动画

    // 布局控制状态
    const isCollapsed = ref(true);

    // 收藏案例数据和总数
    const cases = ref([]); // 将不再是mock数据，而是通过API获取
    const totalCasesCount = ref(0); // 从后端获取的总数

    // 分页状态 (现在是本地 ref，从 Vuex 获取初始值)
    const page = ref(favoriteSearchParams.value.pagenum || 1);
    const pageSize = ref(5); // 页面中设置为5，这是固定的

    // 选中案例的索引 (现在是本地 ref，从 Vuex 获取初始值)
    const selectIndex = ref(favoriteSearchParams.value.selectedIndex || -1);
    const selectCaseId = computed(() => {
      if (selectIndex.value === -1 || !cases.value[selectIndex.value]) {
        return null; // 如果没有选中案例，返回 null
      } else {
        return cases.value[selectIndex.value].caseId; // 返回选中案例的 ID
      }
    });
    // 筛选状态 (现在是本地 ref，从 Vuex 获取初始值)
    const filterCountry = ref(favoriteSearchParams.value.country || null); // 从 Vuex 获取初始值
    const filterTime = ref(favoriteSearchParams.value.period || null); // 从 Vuex 获取初始值

    // ==============================
    // 2. 配置数据
    // ==============================
    // 国家选项 (扩展以匹配更广泛的数据)
    const countryOptions = [
      { value: null, label: "全部", enLabel: "All" },
      { value: "US", label: "美国", enLabel: "USA" },
      { value: "JPN", label: "日本", enLabel: "Japan" },
      { value: "EU", label: "欧盟", enLabel: "European Union" },
    ];

    const caseDetailContent = ref(""); // 用于存储 AI 分析结果，初始为空

    // Markdown 处理器
    const md = new MarkdownIt();

    // 案例详情HTML
    const caseDetailHtml = computed(() => md.render(caseDetailContent.value));

    const getFavoriteCases = async () => {
      loadingCases.value = true; // 开始加载案件列表
      try {
        const params = {
          userId: getAuth("userId"), // 从本地存储获取用户ID
          language: lang.value,
          pagenum: page.value,
          pagesize: pageSize.value,
          country: filterCountry.value,
          period: filterTime.value,
        };
        console.log("获取收藏案件的参数", params);
        const response = await api.getFavoriteCases(lang.value, filterCountry.value, parseInt(getAuth("userId") || "0", 10), filterTime.value, page.value, pageSize.value);
        if (response.code === 200) {
          console.log("获取收藏案件成功:", response.data);
          cases.value = response.data.favoriteInfoList || []; // 假设后端返回的数据结构中包含 cases 数组
          totalCasesCount.value = response.data.totalCount || 0; // 假设后端返回的数据结构中包含 totalCount

          // 确保选中索引有效
          if (cases.value.length > 0) {
            // 如果当前选中索引有效且在新数据中，则保持
            // 否则，默认选中第一个
            if (selectIndex.value === -1 || selectIndex.value >= cases.value.length) {
              selectIndex.value = 0;
            }
            // getCaseSummary() 会在 selectCaseId 的 watch 中触发
          } else {
            selectIndex.value = -1; // 如果没有案件，重置选中索引
            caseDetailContent.value = lang.value === "zh" ? "暂无收藏案件。" : "No favorite cases found.";
          }
        } else {
          console.error("Failed to fetch favorite cases:", response.message);
          cases.value = [];
          totalCasesCount.value = 0;
          selectIndex.value = -1;
          caseDetailContent.value = lang.value === "zh" ? "获取收藏案件失败。" : "Failed to retrieve favorite cases.";
        }
      } catch (error) {
        console.error("Error fetching favorite cases:", error);
        cases.value = [];
        totalCasesCount.value = 0;
        selectIndex.value = -1;
        caseDetailContent.value = lang.value === "zh" ? "获取收藏案件失败。" : "Failed to retrieve favorite cases.";
      } finally {
        loadingCases.value = false; // 结束加载案件列表
      }
    };

    const jumpAndGetFavoriteCases = async () => {
      loadingCases.value = true; // 开始加载案件列表
      try {
        const params = {
          userId: getAuth("userId"),
          language: lang.value,
          pagenum: favoriteSearchParams.value.pagenum || 1,
          pagesize: pageSize.value,
          country: filterCountry.value,
          period: filterTime.value,
        };
        console.log("参数", params);
        console.log("获取收藏案件的参数 (从Home跳转)", params);
        const response = await api.getFavoriteCases(lang.value, filterCountry.value, parseInt(getAuth("userId") || "0", 10), filterTime.value, page.value, pageSize.value);
        if (response.code === 200) {
          console.log("获取收藏案件成功:", response.data);
          cases.value = response.data.favoriteInfoList || [];
          totalCasesCount.value = response.data.totalCount || 0;

          // 尝试选中之前保存的索引
          if (cases.value.length > 0) {
            const savedIndex = favoriteSearchParams.value.selectedIndex;
            if (savedIndex !== -1 && savedIndex < cases.value.length) {
              selectIndex.value = savedIndex;
            } else {
              selectIndex.value = 0; // 如果保存的索引无效，则选中第一个
            }
          } else {
            selectIndex.value = -1;
            caseDetailContent.value = lang.value === "zh" ? "暂无收藏案件。" : "No favorite cases found.";
          }
        } else {
          console.error("Failed to fetch favorite cases:", response.message);
          cases.value = [];
          totalCasesCount.value = 0;
          selectIndex.value = -1;
          caseDetailContent.value = lang.value === "zh" ? "获取收藏案件失败。" : "Failed to retrieve favorite cases.";
        }
      } catch (error) {
        console.error("Error fetching favorite cases:", error);
        cases.value = [];
        totalCasesCount.value = 0;
        selectIndex.value = -1;
        caseDetailContent.value = lang.value === "zh" ? "获取收藏案件失败。" : "Failed to retrieve favorite cases.";
      } finally {
        loadingCases.value = false; // 结束加载案件列表
      }
    };

    // const changeLangAndGetFavoriteCases = async () => {
    //     loadingCases.value = true; // 开始加载案件列表
    //     try {
    //         const params = {
    //             userId: sessionStorage.getItem("userId"),
    //             language: lang.value,
    //             pagenum: page.value,
    //             pagesize: pageSize.value,
    //             country: filterCountry.value,
    //             period: filterTime.value,
    //         };
    //         console.log("获取收藏案件的参数 (语言切换)", params);
    //         const response = await api.getFavoriteCases(lang.value,filterCountry.value,parseInt(sessionStorage.getItem("userId")),filterTime.value,page.value,pageSize.value);
    //         if (response.code === 200) {
    //             console.log("获取收藏案件成功:", response.data);
    //             cases.value = response.data.cases || [];
    //             totalCasesCount.value = response.data.totalCount || 0;

    //             // 语言切换后，尝试保持当前选中案件
    //             const currentSelectedCaseId = cases.value[selectIndex.value]?.case_id;
    //             if (currentSelectedCaseId) {
    //                 const newIndex = cases.value.findIndex(c => c.case_id === currentSelectedCaseId);
    //                 if (newIndex !== -1) {
    //                     selectIndex.value = newIndex;
    //                 } else if (cases.value.length > 0) {
    //                     selectIndex.value = 0; // 如果之前选中的案例在新列表中不存在，则选中第一个
    //                 } else {
    //                     selectIndex.value = -1;
    //                 }
    //             } else if (cases.value.length > 0) {
    //                 selectIndex.value = 0; // 如果之前没有选中，则选中第一个
    //             } else {
    //                 selectIndex.value = -1;
    //             }
    //         } else {
    //             console.error("Failed to fetch favorite cases:", response.message);
    //             cases.value = [];
    //             totalCasesCount.value = 0;
    //             selectIndex.value = -1;
    //             caseDetailContent.value = lang.value === "zh" ? "获取收藏案件失败。" : "Failed to retrieve favorite cases.";
    //         }
    //     } catch (error) {
    //         console.error("Error fetching favorite cases:", error);
    //         cases.value = [];
    //         totalCasesCount.value = 0;
    //         selectIndex.value = -1;
    //         caseDetailContent.value = lang.value === "zh" ? "获取收藏案件失败。" : "Failed to retrieve favorite cases.";
    //     } finally {
    //         loadingCases.value = false; // 结束加载案件列表
    //     }
    // };

    onMounted(() => {
      // 页面加载时获取收藏夹数据
      if (favoriteSearchParams.value.selectedIndex !== -1) {
        console.log("home跳转到收藏夹页面并获取数据");
        jumpAndGetFavoriteCases();
      } else {
        getFavoriteCases();
      }
    });

    const getCaseSummary = async () => {
      if (selectIndex.value === -1 || !cases.value[selectIndex.value]) {
        caseDetailContent.value = lang.value === "zh" ? "暂无详细内容。" : "No detailed content available.";
        return;
      }
      loadingDetail.value = true; // 开始加载案件详情
      try {
        const params = {
          caseId: selectCaseId.value,
          language: lang.value,
          userId: parseInt(getAuth("userId") || "0", 10),
        };
        caseDetailContent.value = lang.value === "zh" ? "正在加载 AI 分析结果，请稍候..." : "Loading AI analysis result, please wait...";
        console.log("获取 AI 分析结果的参数", params);
        const response = await api.getCaseSummary(selectCaseId.value, lang.value, parseInt(getAuth("userId") || "0", 10));
        console.log("AI分析结果", response);
        caseDetailContent.value = response.data || (lang.value === "zh" ? "未获取到 AI 分析结果。" : "No AI analysis result obtained.");
      } catch (error) {
        console.error("获取AI分析结果失败:", error);
        caseDetailContent.value = lang.value === "zh" ? "获取 AI 分析结果失败。" : "Failed to retrieve AI analysis result.";
      } finally {
        loadingDetail.value = false; // 结束加载案件详情
      }
    };

    watch([filterCountry, filterTime, page, lang], () => {
      // 当筛选条件或分页变化时重新获取数据
      store.commit("setFavoriteSearchParams", {
        pagenum: page.value,
        // selectedIndex 不需要在这里更新，因为 getFavoriteCases 会自动处理选中逻辑
        country: filterCountry.value, // 确保筛选条件也保存
        period: filterTime.value, // 确保筛选条件也保存
      });
      getFavoriteCases();
      getCaseSummary();
    });

    watch(selectCaseId, () => {
      console.log("当前选中案件的ID", selectCaseId.value);
      store.commit("setFavoriteSearchParams", {
        pagenum: page.value,
        selectedIndex: selectIndex.value, // 保存当前选中索引
        country: filterCountry.value,
        period: filterTime.value,
      });
      getCaseSummary();
    });

    // watch(lang, () => {
    //   // 当语言变化时重新获取收藏夹数据
    //   changeLangAndGetFavoriteCases();
    // });

    // 处理案例卡片点击事件
    const selectCase = (index) => {
      selectIndex.value = index;
    };

    // 导出Word文档
    const downloadWord = () => {
      const selectedCase = cases.value[selectIndex.value];
      if (!selectedCase) {
        console.warn("No case selected for download.");
        alert(lang.value === "zh" ? "请先选择一个案件再下载。" : "Please select a case to download.");
        return;
      }

      const displayCountry = (countryOptions.find((opt) => opt.value === selectedCase.country) || {})[lang.value === "zh" ? "label" : "enLabel"] || selectedCase.country;

      const html = `
        <html>
          <head><meta charset="utf-8"/></head>
          <body>
            <h1>${selectedCase.case_name || (lang.value === "zh" ? "案件详情" : "Case Details")}</h1>
            <p><strong>${lang.value === "zh" ? "国家" : "Country"}:</strong> ${displayCountry}</p>
            <p><strong>${lang.value === "zh" ? "法院" : "Court"}:</strong> ${selectedCase.court || "N/A"}</p>
            <p><strong>${lang.value === "zh" ? "判决日期" : "Judgment Date"}:</strong> ${selectedCase.judgement_date}</p>
            <p><strong>${lang.value === "zh" ? "关键词" : "Tags"}:</strong> ${selectedCase.tags}</p>
            <hr/>
            ${caseDetailHtml.value}
          </body>
        </html>
      `;
      const blob = HtmlDocx.asBlob(html);
      saveAs(blob, `${selectedCase.case_name || (lang.value === "zh" ? "案件详情" : "Case Details")}.docx`);
    };

    // 打开原始判决文书链接
    const openOriginUrl = () => {
      const selectedCase = cases.value[selectIndex.value];
      if (selectedCase && selectedCase.originalDocumentUrl) {
        window.open(selectedCase.originalDocumentUrl, "_blank");
      } else {
        console.warn("Current case has no original URL or no case is selected.");
        alert(lang.value === "zh" ? "当前案件没有原始链接或未选择案件。" : "No original link available for this case or no case is selected.");
      }
    };

    const showCountry = (country) => {
      if (country === "US") return lang.value === "zh" ? "美国" : "USA";
      else if (country === "JPN") return lang.value === "zh" ? "日本" : "Japan";
      else if (country === "EU") return lang.value === "zh" ? "欧盟" : "European Union";
      return country || "";
    };

    // 取消收藏
    const unFavorite = async (caseId) => {
      if (!confirm(lang.value === "zh" ? "确定要取消收藏此案件吗？" : "Are you sure you want to unfavorite this case?")) {
        return;
      }

      try {
        const userId = getAuth("userId");
        const response = await api.cancelFavoriteCase(caseId, parseInt(userId)); // 调用取消收藏API
        if (response.code === 200) {
          ElNotification({
            title: lang.value === "zh" ? "成功" : "Success",
            message: lang.value === "zh" ? "案件已取消收藏。" : "Case has been unfavorited.",
            type: "success",
            duration: 2000,
          });
          // 重新获取收藏列表以更新UI
          // 调用 fetchFavoriteCases 会自动处理 selectIndex 的调整
          getFavoriteCases();
        } else {
          alert(lang.value === "zh" ? "取消收藏失败：" + response.message : "Failed to unfavorite: " + response.message);
        }
      } catch (error) {
        console.error("Failed to unfavorite case:", error);
        alert(lang.value === "zh" ? "取消收藏失败，请稍后再试。" : "Failed to unfavorite, please try again later.");
      }
    };

    // ==============================
    // 5. 返回数据
    // ==============================
    return {
      // 状态变量
      isCollapsed,
      page,
      pageSize,
      selectIndex,
      filterCountry,
      filterTime,
      cases, // 现在是动态获取的
      totalCasesCount, // 动态获取的总数
      loadingCases, // 暴露给模板
      loadingDetail, // 暴露给模板

      // 配置数据
      countryOptions,

      // 计算属性
      lang,
      caseDetailHtml,

      // 方法函数
      selectCase, // 将 case selection 逻辑封装
      showCountry,
      getFavoriteCases,
      downloadWord,
      openOriginUrl,
      unFavorite,
      getCaseSummary,
    };
  },
};
</script>

<style scoped>
/* 保持原有的样式，并添加 selected-card 样式 */
.search-grid-container {
  position: relative;
  width: 100%;
  height: 100%;
  display: grid;
  grid-template-columns: 1fr 8fr 1fr 22fr 1fr;
  grid-template-rows: 20fr 100fr;
  grid-template-areas:
    "blank1 case-list-area blank2 case-content-area blank3"
    "blank1 case-list-area blank2 case-content-area blank3"
    "blank1 case-list-area blank2 case-content-area blank3";
}
.case-list-area {
  grid-area: case-list-area;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  width: 100%;
}
.case-list-flex {
  display: flex;
  flex-wrap: wrap;
  gap: 21px;
  justify-content: flex-start;
  margin-top: 8px;
  margin-bottom: 8px;
  width: 100%;
  min-height: 400px; /* 确保在没有案件时也有一定的区域 */
  align-content: flex-start; /* 当内容不足时，让项目从顶部开始 */
  position: relative; /* 为 v-loading 提供定位上下文 */
  align-items: center; /* 垂直居中加载动画 */
  justify-content: center; /* 水平居中加载动画 */
}

.no-cases-message,
.loading-message {
  width: 100%;
  text-align: center;
  color: #606266;
  font-size: 16px;
  margin-top: 50px;
}
/* 确保加载文本不被截断或断行 */
.case-list-flex :deep(.el-loading-text),
.case-detail-container :deep(.el-loading-text) {
  white-space: nowrap; /* 防止文本换行 */
}

.case-card-new {
  background: #f2f6fc;
  border-radius: 14px;
  cursor: pointer;
  transition: box-shadow 0.2s, border 0.2s; /* 添加 border 过渡 */
  width: 100%;
  min-width: 220px;
  max-width: 100%;
  height: 100px;
  margin-bottom: -2px;
  padding: 16px 24px 12px 16px;
  display: flex;
  align-items: center;
  box-sizing: border-box;
  border: 1px solid transparent; /* 默认透明边框 */
  border: 1px solid #e0e6ed; /* 默认边框颜色 */
  /* 推荐最终样式 */
  box-shadow: 
  0 1px 2px rgba(30, 96, 255, 0.08), /* 第一层：轻微边缘阴影，带有浅蓝色 */
  0 2px 4px rgba(30, 96, 255, 0.04); /* 第二层：非常浅的扩散阴影，几乎若有若无 */
}
.case-card-new:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}
.case-card-new.selected-card {
  border: 2px solid #409eff; /* 蓝色边框 */
  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2); /* 蓝色阴影 */
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
  font-size: 16px;
  color: #222;
  flex: 1; /* 允许标题占据可用空间 */
  text-overflow: ellipsis; /* 显示省略号 */
  max-width: 18em; /* 限制最大宽度 */
  white-space: nowrap; /* 防止换行 */
  overflow: hidden; /* 超出部分隐藏 */
}
.case-card-row {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  margin-bottom: 2px;
}
.case-country {
  font-size: 14px;
  color: #222;
  font-weight: light;
  margin-right: 24px;
}
.case-court {
  font-size: 14px;
  color: #222;
  font-weight: light;
  margin-right: 24px;
}
.case-date {
  font-size: 14px;
  color: #222;
  font-weight: light;
  margin-left: 10px;
}
.case-tags {
  font-size: 14px;
  color: #222;
  font-weight: light;
  max-width: 16em; /* 限制最大宽度 */
  white-space: nowrap; /* 防止换行 */
  overflow: hidden; /* 超出部分隐藏 */
  text-overflow: ellipsis; /* 超出部分显示省略号 */
}
.case-link {
  float: right;
  color: rgb(115.2, 117.6, 122.4);
  font-size: 14px;
  font-weight: 500;
  margin-left: auto;
  text-decoration: none;
}
.case-link:hover {
  text-decoration: underline;
  color: rgb(121.3, 187.1, 255);
}
.case-detail-content {
  font-size: 15px;
  color: #222;
  line-height: 1.8;
  /* 确保当内容很短时，加载动画依然能居中显示 */
  min-height: 100px; /* 根据实际内容区域大小调整 */
}

/* 案件详情容器，添加定位属性以确保 v-loading 生效 */
.case-detail-container {
  position: relative;
}

.left-panel {
  position: absolute;
  top: 0;
  left: 0;
  width: 200px;
  height: 93%;
  background: #fff;
  transition: left 0.3s;
  z-index: 20;
  border-right: 1px solid #eee;
  display: flex;
  flex-direction: column;
  padding-top: 40px;
}
.left-panel.collapsed {
  left: -200px;
}
.collapse-btn {
  position: absolute;
  top: 103px;
  right: -26px;
  width: 28px;
  height: 28px;
  background: #409eff;
  color: white;
  border: 1px solid #eee;
  border-radius: 50%;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(205, 208, 214, 0.12);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  z-index: 21;
  transition: right 0.3s;
}
.collapse-btn.collapsed {
  right: -26px;
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
</style>
