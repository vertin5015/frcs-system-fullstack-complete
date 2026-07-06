<template>
  <div class="home-view">
    <!-- 页面顶部插入图像 -->
    <div style="grid-template-areas: icon-area; display: flex; justify-content: center; align-items: center">
      <div
        style="
          box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
          background-color: red;
          height: 110px;
          width: 110px;
          display: flex;
          justify-content: center;
          align-items: center;
          border-radius: 10px;
          background-color: #f2f6fc;
        "
      >
        <i class="iconfont icon-falvfagui" style="font-size: 70px; color: #409eff"></i>
      </div>
    </div>
    <div style="grid-template-areas: 'favorite-cases'; display: flex; justify-content: center; align-items: center">
      <div class="favorite-cases" style="margin-bottom: 20px; background-color: transparent; width: 990px">
        <div class="section-header">
          <h3 style="font-weight: bold; display: inline-block; color: #409eff">{{ recentFavoritesText }}</h3>
        </div>
        <el-scrollbar>
          <div
            class="scrollbar-flex-content"
            v-loading="loadingFavorites"
            element-loading-text="正在加载最近收藏案件..."
            element-loading-spinner="Loading"
            element-loading-background="rgba(255, 255, 255, 0.8)"
          >
            <template v-if="!loadingFavorites && favoriteCases.length === 0">
              <div class="no-favorites-message">
                <i class="iconfont icon-tishi2" style="color: #409eff; font-size: 30px; vertical-align: middle"></i>
                <span style="font-size: 17px; color: #595959; font-weight: 600; vertical-align: middle; padding-left: 10px">
                  <template v-if="username === '游客'">
                    <template v-if="lang === 'zh'">
                      游客不提供此功能，请
                      <span style="color: #409eff; text-decoration: underline;cursor: pointer;" @click="logout">登录</span>
                      以查看收藏的案件
                    </template>
                    <template v-else>
                      Please
                      <span style="color: #409eff; text-decoration: underline" @click="logout">log in</span>
                      to view your favorite cases
                    </template>
                  </template>
                  <template v-else>
                    {{ lang === "zh" ? "您还没有收藏任何案件" : "You have not favorited any cases yet" }}
                  </template>
                </span>
              </div>
            </template>
            <template v-else>
              <div v-for="caseItem in favoriteCases.slice(0, 20)" :key="caseItem.case_id" class="scrollbar-demo-item" @click="goToCaseDetail(caseItem)">
                <div style="width: 220px">
                  <el-tooltip class="box-item" effect="dark" :content="caseItem.caseName" ; placement="top-start">
                    <h4
                      style="
                        font-size: 16px;
                        color: #222;
                        font-weight: 600;
                        margin-top: -2px;
                        margin-bottom: 18px;
                        margin-left: 10px;
                        overflow: hidden;
                        text-overflow: ellipsis;
                        white-space: nowrap;
                        max-width: 14em;
                      "
                    >
                      {{ caseItem.caseName }}
                    </h4>
                  </el-tooltip>
                  <div
                    style="
                      position: relative; /* 父容器需要相对定位 */
                      display: flex;
                      align-items: center; /* 垂直居中对齐 */
                      font-size: 14px;
                      color: #667085;
                      margin-bottom: 10px;
                      margin-top: -6px;
                      margin-left: 10px;
                      width: 100%; /* 确保占据整个宽度以便于子元素的绝对定位 */
                    "
                  >
                    <!-- 左侧：国家 -->
                    <span>{{ showCountry(caseItem.country) }}</span>

                    <!-- 右侧：日期，绝对定位到右边10px -->
                    <span style="position: absolute; right: 30px; /* 固定在右侧10px */ top: 50%; transform: translateY(-50%); /* 垂直居中 */ font-size: 14px; color: #667085">
                      {{ caseItem.judgementDate }}
                    </span>
                  </div>
                  <div
                    style="
                      display: flex;
                      justify-content: space-between; /* 关键：两端对齐 */
                      align-items: center; /* 垂直居中 */
                      font-size: 14px;
                      color: #4e5969;
                      line-height: 1.5;
                      margin-left: 10px;
                      margin-top: -2px;
                      overflow: hidden;
                      white-space: nowrap;
                      width: 100%; /* 确保占满父容器，以便两端对齐生效 */
                    "
                  >
                    <!-- 左侧：tags -->
                    <div style="overflow: hidden; text-overflow: ellipsis; color: #0958d9">
                      {{ caseItem.tags }}
                    </div>

                    <!-- 右侧：引用次数 -->
                    <span style="margin-left: 8px; flex-shrink: 0; margin-left: 0; margin-right: 40px">
                      {{ lang === "zh" ? "引用" : "cite" }} <span style="color: #0958d9">{{ caseItem.citationCount }}</span> {{ lang === "zh" ? "次" : "times" }}
                    </span>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </el-scrollbar>
      </div>
    </div>

    <div class="search-area">
      <div style="width: 990px; height: 100%">
        <div
          class="search-entry-hint"
          @click="goSearchPage"
          style="
            margin-bottom: 12px;
            padding: 10px 14px;
            background: linear-gradient(90deg, #ecf5ff, #f0f9eb);
            border: 1px solid #b3d8ff;
            border-radius: 8px;
            cursor: pointer;
            font-size: 14px;
            color: #303133;
          "
        >
          <span style="color: #409eff; font-weight: 600">{{
            lang === "zh" ? "案例检索（新）" : "Case search (new)"
          }}</span>
          {{
            lang === "zh"
              ? "：多数据源筛选、左侧原文 PDF、右侧 AI 摘要（异步生成）。点此进入或点击下方搜索。"
              : ": multi-source filters, original PDF + AI summary. Click here or use search below."
          }}
        </div>
        <div class="filter-bar">
          <div class="filter-group">
            <span class="filter-label">{{ lang === "zh" ? "国家：" : "Country:" }}</span>
            <el-radio-group v-model="filter.country" size="small">
              <el-radio-button v-for="item in countryOptions" :key="item.value" :label="item.value">{{ lang === "zh" ? item.label_zh : item.label_en }}</el-radio-button>
            </el-radio-group>
          </div>
          <div class="filter-group">
            <span class="filter-label">{{ lang === "zh" ? "判决时间：" : "Judgment Time:" }}</span>
            <el-radio-group v-model="filter.period" size="small">
              <el-radio-button v-for="item in timeOptions" :key="item.value" :label="item.value">{{ lang === "zh" ? item.label_zh : item.label_en }}</el-radio-button>
            </el-radio-group>
          </div>
        </div>
        <div class="input-box-outer">
          <div class="input-box-inner" :class="{ focused: isInputFocused }">
            <el-input
              @focus="isInputFocused = true"
              @blur="isInputFocused = false"
              v-model="searchText"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 3 }"
              class="styled-textarea"
              :placeholder="searchPlaceholderText"
            />
            <button class="send-btn" @click="searchCases">
              <i class="iconfont icon-sousuo" style="font-size: 23px"></i>
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import { useStore } from "vuex";
import api from "../api/index";
import { getAuth, clearAllAuth } from "../utils/authStorage";

export default {
  name: "HomeView",
  setup() {
    const store = useStore();
    const router = useRouter();

    const isInputFocused = ref(false);
    const username = ref(getAuth("username") || "游客");

    const loadingFavorites = ref(false);

    const filter = computed(() => store.getters.searchParams);
    const searchText = computed({
      get: () => store.getters.searchParams.keyword,
      set: (value) => store.commit("setSearchKeyword", value),
    });
    const lang = computed(() => store.getters.lang);

    const countryOptions = [
      { value: null, label_zh: "全部", label_en: "All" },
      { value: "US", label_zh: "美国", label_en: "USA" },
      { value: "JPN", label_zh: "日本", label_en: "Japan" },
      { value: "EU", label_zh: "欧盟", label_en: "European Union" },
    ];
    const timeOptions = [
      { value: null, label_zh: "全部", label_en: "All" },
      { value: 1, label_zh: "最近一年", label_en: "Last 1 Year" },
      { value: 3, label_zh: "最近三年", label_en: "Last 3 Years" },
      { value: 5, label_zh: "最近五年", label_en: "Last 5 Years" },
      { value: 10, label_zh: "最近十年", label_en: "Last 10 Years" },
    ];

    const recentFavoritesText = computed(() => {
      return lang.value === "zh" ? "最近收藏案件" : "Recently Favorited Cases";
    });
    const searchPlaceholderText = computed(() => {
      return lang.value === "zh" ? "请输入您想查询案件的关键字" : "Enter keywords to search for cases";
    });

    const favoriteCases = ref([]);

    const getCollectionList = async () => {
      if (getAuth("userId") === "0") {
        favoriteCases.value = [];
        return;
      }
      loadingFavorites.value = true;
      try {
        const params = {
          userId: getAuth("userId"),
          language: store.getters.lang,
          pagenum: 1,
          pagesize: 30,
          country: null,
          period: null,
        };
        console.log("获取收藏列表参数:", params);
        const response = await api.getFavoriteCases(lang.value, null, parseInt(getAuth("userId") || "0", 10), null, 1, 30);
        if (response.code === 200) {
          favoriteCases.value = response.data.favoriteInfoList;
          console.log("获取收藏列表成功:", favoriteCases.value);
        } else {
          console.error("获取收藏列表失败:", response.message);
          favoriteCases.value = [];
        }
      } catch (error) {
        console.error("获取收藏列表时发生错误:", error);
        favoriteCases.value = [];
      } finally {
        loadingFavorites.value = false;
      }
    };

    const logout = () => {
      clearAllAuth();
      router.push("/login");
    };

    const searchCases = () => {
      store.commit("setSearchParams", {
        keyword: searchText.value,
        country: filter.value.country,
        period: filter.value.period,
      });
      router.push("/case-query/search");
    };

    /** 直接进入案例检索页（新功能所在页面） */
    const goSearchPage = () => {
      store.commit("setSearchParams", {
        keyword: searchText.value,
        country: filter.value.country,
        period: filter.value.period,
      });
      router.push("/case-query/search");
    };

    const showCountry = (country) => {
      if (country === "US") return lang.value === "zh" ? "美国" : "USA";
      else if (country === "JPN") return lang.value === "zh" ? "日本" : "Japan";
      else if (country === "EU") return lang.value === "zh" ? "欧盟" : "European Union";
      return country || "";
    };
    const goToCaseDetail = (clickedCaseItem) => {
      const FAVORITE_CASES_PAGE_SIZE = 5;

      const indexInFullList = favoriteCases.value.findIndex((item) => item.caseId === clickedCaseItem.caseId);
      console.log("点击的案件在收藏列表中的索引:", indexInFullList);
      if (indexInFullList !== -1) {
        const targetPagenum = Math.floor(indexInFullList / FAVORITE_CASES_PAGE_SIZE) + 1;
        const targetSelectedIndex = indexInFullList % FAVORITE_CASES_PAGE_SIZE;

        console.log("准备 commit", { pagenum: targetPagenum, selectedIndex: targetSelectedIndex });
        store.commit("setFavoriteSearchParams", {
          pagenum: targetPagenum,
          selectedIndex: targetSelectedIndex,
        });
        console.log("commit 已执行");
        console.log(`跳转到收藏夹：案件ID ${clickedCaseItem.case_id}, 目标页码: ${targetPagenum}, 选中索引: ${targetSelectedIndex}`);
        router.push("/case-query/favorite");
      } else {
        console.warn("点击的案件未在当前收藏列表中找到，无法跳转到详情。");
        router.push("/case-query/favorite");
      }
    };

    onMounted(() => {
      getCollectionList();
    });

    watch(lang, () => {
      getCollectionList();
    });
    watch(
      () => filter.value.country,
      (newVal) => {
        store.commit("setSearchCountry", newVal);
      }
    );
    watch(
      () => filter.value.period,
      (newVal) => {
        store.commit("setSearchPeriod", newVal);
      }
    );

    return {
      searchText,
      filter,
      favoriteCases,
      username,
      searchCases,
      goSearchPage,
      goToCaseDetail,
      showCountry,
      logout,
      isInputFocused,
      recentFavoritesText,
      searchPlaceholderText,
      lang,
      countryOptions,
      timeOptions,
      loadingFavorites,
    };
  },
};
</script>

<style scoped>
.home-view {
  padding: 20px;
  display: flex;
  display: grid;
  grid-template-areas:
    "icon-area"
    "favorite-cases"
    "search-area";
  grid-template-rows: 110px 250px 220px;
  font-size: 14px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
  font-size: 16px;
}

.case-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
}

.search-area {
  background: transparent;

  padding: 32px 0 0 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  font-size: 14px;
}
.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 24px;
  margin-bottom: 18px;
  justify-content: flex-start;
  width: 100%;
  max-width: 1100px;
  font-size: 14px;
}
.filter-group {
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  white-space: nowrap;
}
.filter-label {
  font-weight: 500;
  color: #667085;
  margin-right: 8px;
  font-size: 15px;
  white-space: nowrap;
}
.input-box-outer {
  width: 100%;
  max-width: 1100px;
  display: flex;
  justify-content: center;
  background-color: transparent;
}
.input-box-inner {
  position: relative;
  width: 100%;
  background: #fff;
  cursor: pointer;
  border-radius: 18px;
  border: 2px solid #e0e6ed; /* 边框颜色 */
  box-shadow: 0 2px 4px rgba(30, 96, 255, 0.08), /* 第一层：轻微边缘阴影，带有浅蓝色 */ 0 2px 4px rgba(30, 96, 255, 0.04); /* 第二层：非常浅的扩散阴影，几乎若有若无 */
  padding: 0;
  display: flex;
  align-items: flex-end;
  transition: box-shadow 0.2s;
  font-size: 14px;
}
.input-box-inner:hover {
  border: 2px solid #409eff; /* 悬停时的边框颜色 */
}

.styled-textarea {
  width: 100%;
  border: none;
  background: transparent;
  font-size: 16px;
  padding: 24px 60px 24px 24px;
  border-radius: 18px;
  outline: none;
  resize: none;
  box-shadow: none;
}
.styled-textarea ::v-deep .el-textarea__inner {
  border: none !important;
  background: transparent !important;
  font-size: 16px;
  padding: 24px 60px 24px 24px;
  border-radius: 30px;
  outline: none;
  resize: none;
  box-shadow: none;
}
.send-btn {
  position: absolute;
  right: 18px;
  bottom: 18px;
  background: #3a8ee6;
  border: none;
  border-radius: 50%;
  width: 44px;
  height: 44px;
  display: flex;
  justify-content: center;
  align-items: center;
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  transition: background 0.2s;
  box-shadow: 0 2px 4px rgba(30, 96, 255, 0.08), /* 第一层：轻微边缘阴影，带有浅蓝色 */ 0 3px 6px rgba(30, 96, 255, 0.04); /* 第二层：非常浅的扩散阴影，几乎若有若无 */
}
.send-btn:hover {
  background: #409eff;
}

.scrollbar-flex-content {
  display: flex;
  width: fit-content; /* 保持原有的宽度设置 */
  font-size: 14px;
  position: relative; /* 为 v-loading 提供定位上下文，这是居中的关键 */
  min-height: 120px; /* 确保加载动画有足够的显示空间 */
  align-items: center; /* 垂直居中 flex 子项 */
  justify-content: center; /* 水平居中 flex 子项 */
  flex-wrap: nowrap; /* 确保子项不换行，保持横向布局 */
}

/* 确保加载文本不被截断或断行 */
.scrollbar-flex-content :deep(.el-loading-text) {
  white-space: nowrap; /* 防止文本换行 */
}

.scrollbar-demo-item {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 240px;
  height: 120px;
  margin: 10px 10px 10px 0;
  text-align: left;
  border-radius: 8px;
  background: #f2f6fc;
  color: #222;
  border: 1px solid #e0e6ed; /* 边框颜色 */
  box-shadow: 0 1px 2px rgba(30, 96, 255, 0.08), /* 第一层：轻微边缘阴影，带有浅蓝色 */ 0 2px 4px rgba(30, 96, 255, 0.04); /* 第二层：非常浅的扩散阴影，几乎若有若无 */
  cursor: pointer;
  transition: box-shadow 0.2s;
  font-size: 14px;
}
.scrollbar-demo-item:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
}

.no-favorites-message {
  width: 100%; /* 确保在 flex 容器中占据整行 */
  text-align: center;
  color: #606266;
  font-size: 16px;
  margin-top: 50px;
  flex-basis: 100%; /* 确保它占据整行 */
}

.filter-bar,
.filter-group,
.filter-label,
.el-radio-button__inner,
.el-select,
.el-select__input,
.el-select__selected-item,
.el-select-dropdown__item {
  font-size: 16px !important;
}
.filter-label {
  font-weight: 600;
}
.input-box-inner.focused {
  border-color: #409eff;
  /* 可选：加一点阴影增强反馈 */
  box-shadow: 0 0 4px rgba(64, 158, 255, 0.3);
}
</style>
