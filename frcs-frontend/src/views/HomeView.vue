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
        <i class="iconfont icon-falvfagui" style="font-size: 70px; color: var(--frcs-accent)"></i>
      </div>
    </div>
    <div style="grid-template-areas: 'favorite-cases'; display: flex; justify-content: center; align-items: center">
      <div class="favorite-cases" style="margin-bottom: 20px; background-color: transparent; width: 990px">
        <div class="section-header">
          <h3 style="font-weight: bold; display: inline-block; color: var(--frcs-accent)">{{ recentFavoritesText }}</h3>
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
                <i class="iconfont icon-tishi2" style="color: var(--frcs-accent); font-size: 30px; vertical-align: middle"></i>
                <span style="font-size: 17px; color: #595959; font-weight: 600; vertical-align: middle; padding-left: 10px">
                  <template v-if="username === '游客'">
                    <template v-if="lang === 'zh'">
                      游客不提供此功能，请
                      <span style="color: var(--frcs-accent); text-decoration: underline;cursor: pointer;" @click="logout">登录</span>
                      以查看收藏的案件
                    </template>
                    <template v-else>
                      Please
                      <span style="color: var(--frcs-accent); text-decoration: underline" @click="logout">log in</span>
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
            background: linear-gradient(90deg, var(--frcs-primary-soft), #fafaf9);
            border: 1px solid #b3d8ff;
            border-radius: 8px;
            cursor: pointer;
            font-size: 14px;
            color: #303133;
          "
        >
          <span style="color: var(--frcs-accent); font-weight: 600">{{
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
        <div class="input-area search-input-box home-search-box">
          <div class="search-input-inner">
            <button class="home-search-button" type="button" :aria-label="lang === 'zh' ? '搜索案例' : 'Search cases'" @click="searchCases">
              <i class="iconfont icon-sousuo"></i>
            </button>
            <el-input
              v-model="searchText"
              class="no-border-textarea"
              type="textarea"
              :autosize="{ minRows: 1, maxRows: 3 }"
              :placeholder="searchPlaceholderText"
              @keyup.enter="searchCases"
            />
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
.scrollbar-demo-item { width:272px; height:160px; padding:18px !important; border-radius:16px !important; background:linear-gradient(145deg,#fff 0%,#faf9f7 100%) !important; }
.scrollbar-demo-item > div { width:100% !important; }
.scrollbar-demo-item h4 { margin:0 0 14px !important; color:var(--frcs-primary) !important; font-size:15px !important; line-height:1.4; }
.scrollbar-demo-item .home-card-meta { display:flex; justify-content:space-between; align-items:center; }
.scrollbar-demo-item .home-card-meta span { color:var(--frcs-text-2); font-size:12px; }
.scrollbar-demo-item .home-card-tags { margin-top:12px; padding-top:10px; border-top:1px solid var(--frcs-border); color:var(--frcs-primary-2); font-size:12px; overflow:hidden; text-overflow:ellipsis; white-space:nowrap; }
@media(max-width:640px){ .scrollbar-demo-item{width:250px;} }
</style>

<style scoped>
/* Home page layout repair */
.home-view { width: 100%; max-width: 1280px; min-height: calc(100vh - 72px); margin: 0 auto; padding: 32px clamp(16px, 3vw, 42px) 40px; display: flex; flex-direction: column; gap: 28px; }
.home-view > :first-child { display: flex !important; justify-content: center; align-items: center; }
.home-view > :first-child > div { width: 88px !important; height: 88px !important; border-radius: 18px !important; background: var(--frcs-primary-soft) !important; box-shadow: var(--frcs-shadow-sm) !important; }
.home-view > :first-child i { font-size: 52px !important; }
.home-view > :nth-child(2) { display: block !important; }
.favorite-cases { width: 100% !important; max-width: 100%; margin: 0 !important; padding: 22px; background: var(--frcs-surface) !important; border: 1px solid var(--frcs-border); border-radius: var(--frcs-radius-lg); box-shadow: var(--frcs-shadow-sm); }
.section-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 14px; }
.section-header h3 { margin: 0 !important; color: var(--frcs-primary) !important; font-size: 18px; }
.scrollbar-flex-content { display: flex; gap: 14px; min-height: 176px; padding: 4px 2px 10px; }
.no-favorites-message { width: 100%; min-height: 150px; display: flex; align-items: center; justify-content: center; text-align: center; }
.scrollbar-demo-item { flex: 0 0 272px; cursor: pointer; }
.home-view .search-area { display: flex; justify-content: center; width: 100%; }
.home-view .search-area > div { width: 100% !important; height: auto !important; }
.search-entry-hint { color: var(--frcs-text) !important; border-color: var(--frcs-border) !important; border-radius: var(--frcs-radius-md) !important; }
.filter-bar { display: flex; flex-wrap: wrap; gap: 14px 24px; padding: 14px 16px; background: var(--frcs-surface); border: 1px solid var(--frcs-border); border-radius: var(--frcs-radius-md); }
.filter-bar .filter-group { display: flex; align-items: center; flex-wrap: wrap; gap: 8px; margin: 0; }
.filter-bar .filter-label { margin: 0; color: var(--frcs-text-2); font-size: 12px; font-weight: 700; }
.input-box-outer { width: 100%; margin-top: 14px; padding: 8px; background: var(--frcs-surface); border: 1px solid var(--frcs-border); border-radius: var(--frcs-radius-lg); box-shadow: var(--frcs-shadow-sm); }
.input-box-inner { position: relative; display: flex; align-items: center; width: 100%; }
.input-box-inner .el-textarea { width: 100%; }
.input-box-inner .styled-textarea .el-textarea__inner { min-height: 46px !important; padding: 11px 52px 11px 14px; resize: none; border: 0; box-shadow: none !important; background: transparent; }
.send-btn { position: absolute; right: 6px; top: 50%; transform: translateY(-50%); width: 36px; height: 36px; display: grid; place-items: center; border: 0; border-radius: 50%; color: #fff; background: var(--frcs-accent); cursor: pointer; transition: transform var(--frcs-motion), background-color var(--frcs-motion); }
.send-btn:hover { transform: translateY(-50%) scale(1.04); background: var(--frcs-primary); }
@media (max-width: 680px) { .home-view { padding: 20px 12px 28px; gap: 20px; } .favorite-cases { padding: 16px; } .scrollbar-demo-item { flex-basis: 246px; } .filter-bar .filter-group { width: 100%; align-items: flex-start; } .filter-bar .el-radio-group { flex: 1; } }

</style>

<style scoped>
/* Home search uses the exact SearchCases visual treatment */
.home-search-box { margin-top: 14px; }
.home-search-box .search-input-inner { min-height: 54px; }
.home-search-button { position: absolute; right: 6px; top: 50%; z-index: 2; width: 36px; height: 36px; display: grid; place-items: center; transform: translateY(-50%); border: 0; border-radius: 50%; color: #fff; background: var(--frcs-accent); cursor: pointer; transition: transform var(--frcs-motion), background-color var(--frcs-motion); }
.home-search-button:hover { transform: translateY(-50%) scale(1.04); background: var(--frcs-primary); }
.home-search-button i { font-size: 20px; }
.home-search-box .no-border-textarea .el-textarea__inner { min-height: 42px !important; padding: 10px 52px 10px 12px; resize: none; border: 0; box-shadow: none !important; background: transparent; }

</style>
