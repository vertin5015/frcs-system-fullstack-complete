<template>
  <div class="search-grid-container">
  
    <div class="left-panel">
      <div class="panel-content">
        <h3>{{ lang === "zh" ? "筛选" : "Filter" }}</h3>
        <div class="filter-group">
          <div class="filter-label">{{ lang === "zh" ? "国家" : "Country" }}</div>
          <select v-model="filterCountry" class="filter-select">
            <option v-for="item in countryOptions" :key="item.value" :value="item.value">{{ lang === "zh" ? item.label : item.enLabel }}</option>
          </select>
        </div>
        <div class="filter-group">
          <div class="filter-label">{{ lang === "zh" ? "数据源（多选）" : "Data sources" }}</div>
          <el-select v-model="filterSources" multiple collapse-tags collapse-tags-tooltip :placeholder="lang === 'zh' ? '不选则按国家筛选' : 'Optional: filter sources'" class="filter-select-el">
            <el-option label="US" value="US" />
            <el-option label="EU" value="EU" />
            <el-option label="JPN" value="JPN" />
          </el-select>
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
    <div class="search-main-stack">
      <div class="search-page-toolbar">
        <span class="search-toolbar-hint">{{ lang === "zh" ? "点击案例在新标签页打开阅读" : "Open a case in a new tab." }}</span>


        <el-tag v-if="summaryCredits !== null" type="primary" effect="dark" size="large" round class="toolbar-credits-tag">
          <span class="toolbar-credits-label">{{ lang === "zh" ? "摘要剩余" : "Credits" }}</span>
          <span class="toolbar-credits-num">{{ summaryCredits }}</span>
        </el-tag>
        <el-link v-if="summaryCredits !== null" type="primary" class="toolbar-recharge-link" :underline="false" @click="goRecharge">
          {{ lang === "zh" ? "购买次数" : "Buy credits" }}
        </el-link>
      </div>
      <div class="input-area search-input-box">
        <div class="search-input-inner">
          <button
            style="
              position: absolute;
              right: 6px;
              bottom: 5px;
              z-index: 10;
              background-color: var(--frcs-accent);
              border: none;
              border-radius: 50%;
              width: 30px;
              height: 30px;
              display: flex;
              justify-content: center;
              align-items: center;
            "
            @click="doPerformSearch"
          >
            <i class="iconfont icon-sousuo" style="font-size: 20px; color: white"></i>
          </button>
          <el-input
            class="no-border-textarea"
            v-model="searchText"

            :autosize="{ minRows: 1, maxRows: 3 }"
            type="textarea"
            :placeholder="lang === 'zh' ? '请输入您想查询案件的关键字' : 'Please enter the keywords of the case you want to query'"
            @keyup.enter="performSearch"
          />
        </div>
      </div>
      <div class="case-list-area">
        <el-scrollbar class="case-list-scrollbar">
          <div
            class="case-list-flex"
            :class="{ 'is-empty': !loadingCases && cases.length === 0 }"
            v-loading="loadingCases"
            element-loading-text="正在加载案件列表..."
            element-loading-spinner="Loading"
            element-loading-background="rgba(255, 255, 255, 0.8)"
          >
            <template v-if="!loadingCases && cases.length === 0">
              <div class="no-cases-message">{{ lang === "zh" ? "暂无案件数据。" : "No case data available." }}</div>
            </template>
            <template v-else>
              <div
                v-for="item in cases"
                :key="item.case_id"
                class="case-card-new case-card-link"
              >
                <div class="case-card-content">
                  <div class="case-card-header">
                    <div class="case-title-wrap">
                      <el-tooltip class="box-item" effect="dark" :content="item.case_name" placement="top-start" popper-class="case-title-tooltip">
                        <span class="case-title">{{ item.case_name }}</span>
                      </el-tooltip>
                    </div>
                    <div
                      @click.prevent.stop="toggleFavorite(item)"
                      style="display: flex; align-items: flex-start; flex-shrink: 0; cursor: pointer; font-size: 20px; padding-top: 2px"
                      :title="item.isfavored ? (lang === 'zh' ? '取消收藏' : 'Unfavorite') : lang === 'zh' ? '收藏案件' : 'Favorite Case'"
                    >
                      <i
                        class="iconfont icon-shoucang_shixin"
                        :style="{ fontSize: '20px', color: item.isfavored ? 'var(--frcs-accent)' : 'rgb(199.5, 201, 204)' }"
                      ></i>
                      <span style="font-size: 16px">{{ item.favoritedCount }}</span>
                    </div>
                  </div>
                  <div class="case-card-meta-grid">
                    <div v-if="item.judgement_date" class="case-meta-chip">
                      <span class="case-meta-label">{{ lang === "zh" ? "日期" : "Date filed" }}</span>
                      <span class="case-meta-value">{{ item.judgement_date }}</span>
                    </div>
                    <div v-if="item.case_id" class="case-meta-chip">
                      <span class="case-meta-label">{{ lang === "zh" ? "案件编号" : "Docket" }}</span>
                      <span class="case-meta-value case-meta-mono">{{ item.case_id }}</span>
                    </div>
                    <div v-if="item.country" class="case-meta-chip">
                      <span class="case-meta-label">{{ lang === "zh" ? "国家/数据源" : "Country / source" }}</span>
                      <span class="case-meta-value">{{ showCountry(item.country) }}<span class="case-meta-code">（{{ item.country }}）</span></span>
                    </div>
                    <div v-if="courtHintFromTitle(item.case_name)" class="case-meta-chip case-meta-chip-wide">
                      <span class="case-meta-label">{{ lang === "zh" ? "法院/地点" : "Court" }}</span>
                      <span class="case-meta-value">{{ courtHintFromTitle(item.case_name) }}</span>
                    </div>
                    <div v-if="item.citationCount != null" class="case-meta-chip">
                      <span class="case-meta-label">{{ lang === "zh" ? "引用次数" : "Citations" }}</span>
                      <span class="case-meta-value">{{ item.citationCount }}</span>
                    </div>
                    <div v-if="shortSiteHost(item.original_document_url)" class="case-meta-chip case-meta-chip-wide">
                      <span class="case-meta-label">{{ lang === "zh" ? "来源站点" : "Source site" }}</span>
                      <span class="case-meta-value">{{ shortSiteHost(item.original_document_url) }}</span>
                    </div>
                  </div>
                  <div v-if="item.tags && item.tags.trim()" class="case-card-row case-tags-row">
                    <div class="case-tags-tooltip-host">
                      <el-tooltip effect="dark" :content="item.tags || ''" placement="top-start" :disabled="!item.tags">
                        <div class="case-tags-wrap">
                          <div class="case-tags-heading">关键词摘要</div>
                          <div class="case-tags-lines">
                            <div v-for="(kw, idx) in twoKeywordLines(item.tags)" :key="idx" class="case-tags-line">{{ kw }}</div>
                          </div>
                        </div>
                      </el-tooltip>
                    </div>
                  </div>
                  <div class="case-card-row case-card-actions-row">
                    <a class="case-link card-browse-link" :href="readerHref(item)" target="_blank" rel="noopener noreferrer" @click.stop>{{ lang === "zh" ? "新标签页阅读" : "Open in new tab" }}</a>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </el-scrollbar>
        <div style="padding: 8px 0; display: flex; justify-content: center; width: 100%">
          <el-pagination class="search-pagination" size="small" background layout="prev, pager, next" :total="totalCasesCount" :page-size="pageSize" :current-page="page" :disabled="loadingCases" @current-change="handlePageChange" />
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, watch, onMounted, onBeforeUnmount } from "vue";
import { useStore } from "vuex";
import { useRouter } from "vue-router";
import api from "../api/index";
import { ElNotification } from "element-plus";
import { getAuth, setAuth } from "../utils/authStorage";

export default {
  name: "SearchCases",
  setup() {
    const store = useStore();
    const router = useRouter();
    const lang = computed(() => store.getters.lang);
    const searchParams = computed(() => store.getters.searchParams);

    const loadingCases = ref(false);
    const searchEventSource = ref(null);


    const filterCountry = computed({
      get: () => normalizeOptionalParam(searchParams.value.country),
      set: (value) => store.commit("setSearchCountry", normalizeOptionalParam(value) || null),
    });

    const filterTime = computed({
      get: () => searchParams.value.period,
      set: (value) => store.commit("setSearchPeriod", value),
    });

    const filterSources = ref([]);

    const countryOptions = [
      { value: "", label: "全部", enLabel: "All" },
      { value: "US", label: "美国", enLabel: "USA" },
      { value: "JPN", label: "日本", enLabel: "Japan" },
      { value: "EU", label: "欧盟", enLabel: "European Union" },
    ];

    const cases = ref([]);
    const totalCasesCount = ref(0);
    const summaryCredits = ref(null);

    const refreshSummaryCredits = async () => {
      const uid = getAuth("userId");
      if (!uid || uid === "0") {
        summaryCredits.value = null;
        return;
      }
      try {
        const r = await api.getUserSummaryCredits(parseInt(uid, 10));
        if (r.code === 200 && r.data != null) {
          summaryCredits.value = r.data;
          setAuth("summaryCredits", String(r.data));
        }
      } catch {
        /* ignore */
      }
    };

    const page = ref(1);
    const pageSize = ref(6);

    const courtHintFromTitle = (title) => {
      if (!title || typeof title !== "string") return "";
      const m = title.trim().match(/\(([^)]+)\)\s*$/);
      return m ? m[1].trim() : "";
    };

    const shortSiteHost = (url) => {
      if (!url || typeof url !== "string") return "";
      try {
        const u = new URL(url);
        return u.hostname.replace(/^www\./i, "");
      } catch {
        return "";
      }
    };

    /** 从后端摘要字段中截取至多两条「关键词」片段，各占一行展示 */
    const truncateKwChunk = (s, max = 120) => {
      const x = (s || "").trim().replace(/^\.+|\.+$/g, "").trim();
      if (!x) return "";
      if (x.length <= max) return x;
      return `${x.slice(0, max - 1)}…`;
    };

    const twoKeywordLines = (tags) => {
      if (tags == null || typeof tags !== "string") return [];
      const t = tags.trim().replace(/^…+|…+$/g, "").trim();
      if (!t) return [];

      const splitMulti = (raw) =>
        raw
          .split(/[,，;；|、/\r\n]+/)
          .map((s) => s.trim())
          .filter(Boolean);

      let parts = splitMulti(t);
      if (parts.length >= 2) {
        return [truncateKwChunk(parts[0]), truncateKwChunk(parts[1])];
      }

      parts = t
        .split(/\s*\.\.\.\s*/)
        .map((s) => s.trim())
        .filter(Boolean);
      if (parts.length >= 2) {
        return [truncateKwChunk(parts[0]), truncateKwChunk(parts[1])];
      }

      parts = t
        .split(/\s*(?:\.{3}|…+|……+)\s*/)
        .map((s) => s.trim())
        .filter(Boolean);
      if (parts.length >= 2) {
        return [truncateKwChunk(parts[0]), truncateKwChunk(parts[1])];
      }

      const words = t.split(/\s+/).filter(Boolean);
      if (words.length >= 2) {
        return [truncateKwChunk(words[0]), truncateKwChunk(words[1])];
      }
      if (words.length === 1) {
        const w = words[0];
        if (w.length >= 44) {
          const mid = Math.floor(w.length / 2);
          return [truncateKwChunk(w.slice(0, mid)), truncateKwChunk(w.slice(mid))];
        }
        const one = truncateKwChunk(w);
        return one ? [one] : [];
      }

      return [truncateKwChunk(t)];
    };

    const showCountry = (country) => {
      if (country === "US") return lang.value === "zh" ? "美国" : "USA";
      if (country === "JPN") return lang.value === "zh" ? "日本" : "Japan";
      if (country === "EU") return lang.value === "zh" ? "欧盟" : "European Union";
      return country || "";
    };

    const searchText = computed({
      get: () => searchParams.value.keyword,
      set: (value) => store.commit("setSearchKeyword", value),
    });

    const sourcesParam = computed(() => (filterSources.value && filterSources.value.length ? filterSources.value.join(",") : ""));

    const normalizeOptionalParam = (value) => {
      if (value == null || value === "" || value === "null" || value === "undefined") {
        return "";
      }
      return value;
    };

    const performSearch = async (options = {}) => {
      const keyword = (searchText.value || "").trim();
      if (!keyword) {
        if (searchEventSource.value) {
          try {
            searchEventSource.value.close();
          } catch {
            /* ignore */
          }
          searchEventSource.value = null;
        }
        loadingCases.value = false;
        cases.value = [];
        totalCasesCount.value = 0;
        if (options.notify) {
          ElNotification({
            title: lang.value === "zh" ? "提示" : "Notice",
            message: lang.value === "zh" ? "请输入关键词后再搜索" : "Please enter keywords before searching",
            type: "info",
          });
        }
        return;
      }
      if (searchEventSource.value) {
        try {
          searchEventSource.value.close();
        } catch {
          /* ignore */
        }
        searchEventSource.value = null;
      }
      loadingCases.value = true;
      cases.value = [];
      totalCasesCount.value = 0;

      const userId = parseInt(getAuth("userId") || "0", 10);
      const params = new URLSearchParams();
      params.set("keyword", keyword);
      params.set("language", lang.value);
      const normalizedCountry = normalizeOptionalParam(filterCountry.value);
      if (normalizedCountry) {
        params.set("country", normalizedCountry);
      }
      if (filterTime.value != null && filterTime.value !== "null") {
        params.set("period", String(filterTime.value));
      }
      params.set("pagenum", String(page.value));
      params.set("pagesize", String(pageSize.value));
      params.set("userId", String(userId));
      if (sourcesParam.value) {
        params.set("sources", sourcesParam.value);
      }

      const url = `${window.location.origin}/api/cases/search-stream?${params.toString()}`;
      const es = new EventSource(url);
      searchEventSource.value = es;

      es.addEventListener("part", (e) => {
        try {
          const payload = JSON.parse(e.data);
          const chunk = payload.cases || [];
          if (chunk.length) {
            cases.value = [...cases.value, ...chunk];
            loadingCases.value = false;
          }
        } catch (err) {
          console.error(err);
        }
      });

      es.addEventListener("done", (e) => {
        try {
          const wrap = JSON.parse(e.data);
          if (wrap.code === 200 && wrap.data) {
            cases.value = wrap.data.cases || [];
            totalCasesCount.value = wrap.data.totalCount ?? 0;
          }
        } catch (err) {
          console.error(err);
        } finally {
          loadingCases.value = false;
          es.close();
          if (searchEventSource.value === es) {
            searchEventSource.value = null;
          }
        }
      });

      es.addEventListener("fail", (e) => {
        try {
          const wrap = JSON.parse(e.data);
          ElNotification({
            title: lang.value === "zh" ? "操作失败" : "Error",
            message: wrap.message || (lang.value === "zh" ? "搜索失败" : "Search failed"),
            type: "error",
          });
        } catch (err) {
          console.error(err);
        } finally {
          cases.value = [];
          totalCasesCount.value = 0;
          loadingCases.value = false;
          es.close();
          if (searchEventSource.value === es) {
            searchEventSource.value = null;
          }
        }
      });

      es.onerror = () => {
        loadingCases.value = false;
        if (searchEventSource.value === es) {
          try {
            es.close();
          } catch {
            /* ignore */
          }
          searchEventSource.value = null;
        }
      };
    };

    const readerHref = (item) => {
      return router.resolve({ path: "/case-reader", query: { caseId: item.case_id } }).href;
    };

    const handlePageChange = (nextPage) => {
      if (loadingCases.value || nextPage === page.value) return;
      page.value = nextPage;
    };

    const doPerformSearch = () => {
      page.value = 1;
      performSearch({ notify: true });
    };

    const toggleFavorite = async (item) => {
      if (getAuth("userId") === "0") {
        ElNotification({
          title: lang.value === "zh" ? "提示" : "Notice",
          message: lang.value === "zh" ? "游客用户无法收藏案件，请登录后重试。" : "Guest users cannot favorite cases, please log in and try again.",
          type: "info",
          duration: 3000,
        });
        return;
      }
      try {
        if (item.isfavored) {
          item.isfavored = false;
          item.favoritedCount = Math.max(0, item.favoritedCount - 1);
          await api.cancelFavoriteCase(item.case_id, parseInt(getAuth("userId") || "0", 10));
          ElNotification({
            title: lang.value === "zh" ? "操作成功" : "Operation Success",
            message: (lang.value === "zh" ? "案件 " : "Case ") + item.case_id + (lang.value === "zh" ? " 已取消收藏" : " Unfavorited"),
            type: "success",
          });
        } else {
          item.isfavored = true;
          item.favoritedCount += 1;
          await api.favoriteCase(item.case_id, parseInt(getAuth("userId") || "0", 10));
          ElNotification({
            title: lang.value === "zh" ? "操作成功" : "Operation Success",
            message: (lang.value === "zh" ? "案件 " : "Case ") + item.case_id + (lang.value === "zh" ? " 已收藏" : " Favorited"),
            type: "success",
          });
        }
      } catch (error) {
        console.error("收藏/取消收藏操作失败:", error);
        item.isfavored = !item.isfavored;
      }
    };

    onMounted(() => {
      performSearch();
      refreshSummaryCredits();
    });

    onBeforeUnmount(() => {
      if (searchEventSource.value) {
        try {
          searchEventSource.value.close();
        } catch {
          /* ignore */
        }
        searchEventSource.value = null;
      }
    });

    watch([page, pageSize, filterCountry, filterTime], () => {
      performSearch();
    });

    watch(filterSources, () => {
      performSearch();
    });

    watch(lang, () => {
      performSearch();
    });

    const goRecharge = () => {
      router.push("/case-query/recharge");
    };



    return {

      lang,
      filterCountry,

      filterTime,

      filterSources,

      countryOptions,

      cases,

      totalCasesCount,

      page,

      pageSize,

      searchText,

      toggleFavorite,

      doPerformSearch,
      handlePageChange,
      goRecharge,

      performSearch,

      loadingCases,

      showCountry,

      courtHintFromTitle,

      shortSiteHost,

      twoKeywordLines,

      readerHref,

      summaryCredits,

    };

  },

};

</script>



<style scoped>
.case-card-new { min-height:236px; padding:18px !important; border-radius:16px !important; background:linear-gradient(145deg,#fff 0%,#faf9f7 100%) !important; }
.case-card-header { min-height:46px; padding-bottom:12px; border-bottom:1px solid var(--frcs-border); }
.case-title { color:var(--frcs-primary) !important; font-size:16px !important; line-height:1.45; }
.case-card-meta-grid { margin-top:14px !important; gap:8px !important; }
.case-meta-chip { min-height:52px; padding:9px 10px !important; background:var(--frcs-primary-soft) !important; border:1px solid rgba(229,225,218,.82); }
.case-meta-label { font-size:10px !important; font-weight:700; letter-spacing:.05em; text-transform:uppercase; }
.case-meta-value { font-size:13px !important; font-weight:600; }
.case-tags-row { padding-top:11px; border-top:1px solid rgba(229,225,218,.72); }
.case-tags-heading { color:var(--frcs-text-2); font-size:11px; font-weight:700; letter-spacing:.04em; }
.case-tags-line { color:var(--frcs-primary-2); font-size:12px; line-height:1.45; }
.case-card-actions-row { align-items:center; margin-top:14px !important; padding-top:11px; border-top:1px solid rgba(229,225,218,.72); }
.case-link { color:var(--frcs-accent) !important; font-weight:700; }
@media(max-width:640px){ .case-card-meta-grid{grid-template-columns:1fr 1fr;} }
@media(max-width:420px){ .case-card-meta-grid{grid-template-columns:1fr;} }
.search-pagination { margin-top: 6px; }
.search-pagination :deep(button), .search-pagination :deep(.el-pager li) { min-width: 32px; height: 32px; color: var(--frcs-primary-2); background: var(--frcs-surface); border: 1px solid var(--frcs-border); border-radius: 8px; }
.search-pagination :deep(.el-pager li.is-active) { color: #fff; background: var(--frcs-primary); border-color: var(--frcs-primary); }
.search-pagination :deep(button:hover:not(:disabled)), .search-pagination :deep(.el-pager li:hover) { color: var(--frcs-accent); border-color: var(--frcs-accent); }
</style>






