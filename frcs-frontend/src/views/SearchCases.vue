<template>
  <div class="search-grid-container">
    <div :class="['left-panel', { collapsed: isCollapsed }]">
      <button class="collapse-btn" :class="{ collapsed: isCollapsed }" @click="isCollapsed = !isCollapsed">
        <span v-if="isCollapsed">⮞</span>
        <span v-else>⮜</span>
      </button>
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
          <div class="filter-label">{{ lang === "zh" ? "数据源（多选）" : "Data sources" }}</div>
          <el-select
            v-model="filterSources"
            multiple
            collapse-tags
            collapse-tags-tooltip
            :placeholder="lang === 'zh' ? '不选则按国家筛选' : 'Optional: filter sources'"
            class="filter-select-el"
            style="width: 100%"
          >
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
    <div class="search-main-stack" :class="{ 'with-filter-panel': !isCollapsed }">
      <div class="search-page-toolbar">
        <span class="search-toolbar-hint">{{ lang === "zh" ? "点击案例在新标签页打开阅读" : "Open a case in a new tab." }}</span>
        <el-button type="primary" size="large" class="toolbar-back-home-btn" @click="backToHome">{{ lang === "zh" ? "返回主界面" : "Home" }}</el-button>
        <el-tag v-if="summaryCredits !== null" type="primary" effect="dark" size="large" round class="toolbar-credits-tag">
          <span class="toolbar-credits-label">{{ lang === "zh" ? "摘要剩余" : "Credits" }}</span>
          <span class="toolbar-credits-num">{{ summaryCredits }}</span>
        </el-tag>
        <el-link v-if="summaryCredits !== null" type="primary" class="toolbar-recharge-link" :underline="false" @click="goRecharge">
          {{ lang === "zh" ? "购买次数" : "Buy credits" }}
        </el-link>
      </div>
      <div class="input-area search-input-box">
        <div style="width: 100%; height: 85%; background-color: white; border-radius: 15px; border: 1px solid rgb(221.7, 222.6, 224.4); position: relative">
          <button
            style="
              position: absolute;
              right: 6px;
              bottom: 5px;
              z-index: 10;
              background-color: #409eff;
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
            style="padding-top: 12px; width: 100%"
            :autosize="{ minRows: 2, maxRows: 2 }"
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
              <a
                v-for="item in cases"
                :key="item.case_id"
                class="case-card-new case-card-link"
                :href="readerHref(item)"
                target="_blank"
                rel="noopener noreferrer"
              >
                <div class="case-card-content">
                  <div class="case-card-header">
                    <div class="case-title-wrap">
                      <el-tooltip class="box-item" effect="dark" :content="item.case_name" placement="top-start">
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
                        :style="{ fontSize: '20px', color: item.isfavored ? '#409EFF' : 'rgb(199.5, 201, 204)' }"
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
                    <span class="case-link">{{ lang === "zh" ? "新标签页阅读" : "Open in new tab" }}</span>
                  </div>
                </div>
              </a>
            </template>
          </div>
        </el-scrollbar>
        <div style="padding: 8px 0; display: flex; justify-content: center; width: 100%">
          <el-pagination size="small" layout="prev, pager, next" :total="totalCasesCount" v-model:page-size="pageSize" v-model:current-page="page" />
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
    const isCollapsed = ref(true);

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

    const backToHome = () => {
      router.push("/case-query/home");
    };

    const goRecharge = () => {
      router.push("/case-query/recharge");
    };



    return {

      lang,

      isCollapsed,

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

      backToHome,

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

.search-grid-container {

  position: relative;

  width: 100%;

  height: 100%;

  min-height: 0;

  display: flex;

  flex-direction: column;

  box-sizing: border-box;

}

.search-main-stack {

  flex: 1;

  min-height: 0;

  display: flex;

  flex-direction: column;

  padding: 8px 16px 16px 16px;

  box-sizing: border-box;

  transition: padding-left 0.3s;

}

.search-main-stack.with-filter-panel {

  padding-left: 236px;

}

.search-page-toolbar {

  display: flex;

  flex-wrap: wrap;

  align-items: center;

  gap: 10px;

  padding-bottom: 10px;

  flex-shrink: 0;

}

.search-toolbar-hint {

  font-size: 13px;

  color: #909399;

  flex: 1;

  min-width: 200px;

}

.input-area.search-input-box {

  flex-shrink: 0;

  margin-bottom: 8px;

}

.case-list-area {

  flex: 1;

  min-height: 0;

  display: flex;

  flex-direction: column;

  width: 100%;

}

.case-list-scrollbar {

  flex: 1;

  min-height: 0;

}

.case-card-link {

  text-decoration: none;

  color: inherit;

  display: block;

}

.no-border-textarea :deep(.el-textarea__inner) {

  border: none;

  resize: none;

  background-color: transparent;

  box-shadow: none;

}

.case-list-flex {

  display: flex;

  flex-wrap: wrap;

  gap: 24px;

  justify-content: flex-start;

  align-items: flex-start;

  margin-top: 8px;

  margin-bottom: 8px;

  width: 100%;

  position: relative;

  min-height: 200px;

}

.case-list-flex.is-empty {

  justify-content: center;

  align-items: center;

}

.no-cases-message {

  width: 100%;

  text-align: center;

  color: #606266;

  font-size: 16px;

  margin-top: 50px;

}

.case-list-flex :deep(.el-loading-text),

.case-detail-container :deep(.el-loading-text) {

  white-space: nowrap;

}

.case-card-new {

  background: #f2f6fc;

  border-radius: 14px;

  cursor: pointer;

  transition: box-shadow 0.2s, border 0.2s;

  width: 100%;

  min-width: 220px;

  max-width: 100%;

  min-height: 140px;

  height: auto;

  margin-bottom: -2px;

  padding: 16px 24px 12px 16px;

  display: flex;

  align-items: flex-start;

  box-sizing: border-box;

  border: 1px solid rgb(221.7, 222.6, 224.4);

  box-shadow: 0 1px 2px rgba(30, 96, 255, 0.08), 0 2px 4px rgba(30, 96, 255, 0.04);

}

.case-card-new.selected-card {

  border: 2px solid #409eff;

  box-shadow: 0 4px 12px rgba(64, 158, 255, 0.2);

}

.case-card-content {

  flex: 1;

  min-width: 0;

  width: 100%;

}

.case-card-header {

  display: flex;

  align-items: flex-start;

  gap: 10px;

  margin-bottom: 7px;

}

.case-title {

  font-weight: 600;

  font-size: 16px;

  color: #222;

  flex: 1;

  min-width: 0;

  overflow: hidden;

  display: -webkit-box;

  -webkit-box-orient: vertical;

  -webkit-line-clamp: 6;

  line-clamp: 6;

  white-space: normal;

  word-break: break-word;

  line-height: 1.35;

}

.case-title-wrap {

  flex: 1;

  min-width: 0;

}

.case-title-wrap :deep(.el-tooltip__trigger) {

  display: block;

  width: 100%;

  max-width: 100%;

}

.case-card-row {

  display: flex;

  align-items: center;

  justify-content: flex-start;

  margin-bottom: 5px;

}

.case-card-meta-grid {

  display: flex;

  flex-wrap: wrap;

  gap: 8px 22px;

  margin-bottom: 8px;

  padding: 4px 0 8px;

  border-bottom: 1px solid rgba(64, 158, 255, 0.14);

}

.case-meta-chip {

  display: inline-flex;

  align-items: baseline;

  flex-wrap: wrap;

  gap: 4px 8px;

  max-width: 100%;

}

.case-meta-chip-wide {

  flex: 1 1 100%;

}

.case-meta-label {

  font-size: 12px;

  color: #909399;

  white-space: nowrap;

}

.case-meta-value {

  font-size: 13px;

  color: #303133;

  word-break: break-word;

}

.case-meta-code {

  font-size: 12px;

  color: #606266;

  font-weight: 500;

}

.case-meta-mono {

  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;

}

.case-card-actions-row {

  justify-content: flex-end;

  margin-bottom: 0;

}

.case-tags-row.case-card-row {

  display: block;

  width: 100%;

  min-width: 0;

  margin-bottom: 6px;

}

.case-tags-tooltip-host {

  display: block;

  width: 100%;

  max-width: 100%;

  min-width: 0;

}

.case-tags-tooltip-host :deep(.el-tooltip__trigger) {

  display: block !important;

  width: 100%;

  max-width: 100%;

  min-width: 0;

  vertical-align: top;

}

.case-tags-wrap {

  box-sizing: border-box;

  width: 100%;

  max-width: 100%;

  font-size: 14px;

  color: #222;

  padding-top: 2px;

}

.case-tags-heading {

  font-weight: 600;

  font-size: 13px;

  color: #606266;

  margin-bottom: 8px;

}

.case-tags-lines {

  display: flex;

  flex-direction: column;

  gap: 8px;

}

.case-tags-line {

  font-size: 14px;

  color: #0958d9;

  line-height: 1.5;

  word-break: break-word;

  overflow-wrap: anywhere;

  padding: 6px 10px;

  border-radius: 8px;

  background: rgba(64, 158, 255, 0.08);

  border-left: 3px solid #409eff;

}

.case-link {

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

.case-toolbar {

  border-top-left-radius: 10px;

  border-top-right-radius: 10px;

  min-height: 45px;

  background-color: white;

  border: 1px solid rgb(221.7, 222.6, 224.4);

  display: flex;

  align-items: center;

  flex-wrap: wrap;

  gap: 8px;

  font-size: 16px;

  font-weight: bold;

  padding: 8px 16px;

}

.toolbar-back-home-btn {

  flex-shrink: 0;

  padding: 12px 22px;

  font-size: 16px;

  font-weight: 600;

  min-height: 44px;

  box-shadow: 0 2px 10px rgba(64, 158, 255, 0.35);

}

.toolbar-title {

  font-weight: 600;

  font-size: 16px;

  color: #409eff;

}

.toolbar-credits-tag {

  flex-shrink: 0;

  padding: 6px 14px;

  font-size: 13px;

  font-weight: 600;

  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.45);

  border: none;

}

.toolbar-credits-label {

  opacity: 0.95;

  margin-right: 6px;

}

.toolbar-credits-num {

  display: inline-block;

  min-width: 1.25em;

  font-size: 18px;

  font-weight: 800;

  letter-spacing: 0.02em;

  line-height: 1;

}

.toolbar-recharge-link {

  font-weight: 600;

  font-size: 15px;

}

.toolbar-case-name {

  font-weight: 600;

  font-size: 16px;

  color: #333;

  flex: 1;

  min-width: 120px;

  overflow: hidden;

  text-overflow: ellipsis;

  white-space: nowrap;

}

.toolbar-link {

  font-weight: 600;

  font-size: 14px;

  color: #909399;

  cursor: pointer;

}

.toolbar-icon {

  font-size: 16px;

  color: rgb(121.3, 187.1, 255);

  cursor: pointer;

  margin-left: 8px;

}

.case-detail-container {

  position: relative;

  border: 1px solid rgb(221.7, 222.6, 224.4);

  border-top: none;

  min-height: 520px;

  max-height: calc(100vh - 220px);

  background-color: white;

  border-bottom-right-radius: 10px;

  border-bottom-left-radius: 10px;

  padding: 16px;

  box-sizing: border-box;

  overflow: hidden;

  display: flex;

  flex-direction: column;

}

.detail-banner {

  margin-bottom: 8px;

  padding: 8px 12px;

  border-radius: 6px;

  font-size: 14px;

}

.detail-banner.error {

  background: #fef0f0;

  color: #c45656;

  border: 1px solid #fab6b6;

}

.case-detail-split {

  display: flex;

  flex: 1;

  gap: 16px;

  min-height: 0;

}

.pdf-pane,

.summary-pane {

  flex: 1;

  min-width: 0;

  display: flex;

  flex-direction: column;

}

.pane-label {

  font-size: 14px;

  font-weight: 600;

  color: #606266;

  margin-bottom: 8px;

  display: flex;

  align-items: center;

  justify-content: space-between;

}

.pdf-frame {

  width: 100%;

  flex: 1;

  min-height: 420px;

  border: 1px solid #e4e7ed;

  border-radius: 8px;

}

.pdf-placeholder {

  flex: 1;

  min-height: 200px;

  display: flex;

  align-items: center;

  justify-content: center;

  color: #909399;

  border: 1px dashed #dcdfe6;

  border-radius: 8px;

}

.pdf-embed-fallback {

  flex: 1;

  min-height: 200px;

  display: flex;

  flex-direction: column;

  align-items: center;

  justify-content: center;

  gap: 16px;

  padding: 24px;

  text-align: center;

  color: #606266;

  border: 1px solid #e4e7ed;

  border-radius: 8px;

  background: #fafafa;

}

.pdf-embed-fallback-text {

  margin: 0;

  font-size: 14px;

  line-height: 1.6;

  max-width: 320px;

}

.detail-status {

  font-size: 13px;

  color: #409eff;

  margin-bottom: 8px;

}

.case-detail-content {

  font-size: 15px;

  color: #222;

  line-height: 1.8;

  overflow-y: auto;

  min-height: 120px;

}

.md-body :deep(h1),

.md-body :deep(h2),

.md-body :deep(h3) {

  margin: 0.6em 0 0.3em;

}

.md-body :deep(p) {

  margin: 0.4em 0;

}

.md-body :deep(ul),

.md-body :deep(ol) {

  padding-left: 1.4em;

}

.md-body :deep(code) {

  background: #f5f7fa;

  padding: 2px 6px;

  border-radius: 4px;

}

.md-body :deep(pre) {

  background: #f5f7fa;

  padding: 12px;

  overflow: auto;

  border-radius: 8px;

}

.left-panel {

  position: absolute;

  top: 0;

  left: 0;

  width: 220px;

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

  left: -220px;

}

.collapse-btn {

  position: absolute;

  top: 87px;

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

.filter-select-el :deep(.el-select__wrapper) {

  min-height: 36px;

}

</style>
