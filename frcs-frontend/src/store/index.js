import { createStore } from 'vuex'

export default createStore({
  state: {
    lang: 'zh', // 当前语言设置
    searchParams: {
      keyword: '',   // 搜索关键字
      period: null,    // 时间范围
      country: null,   // 国家
    },
    // 收藏夹页面的搜索参数状态
    favoriteSearchParams: {
      pagenum: 1, // 收藏夹页码，默认为第一页
      selectedIndex: -1, // 当前选中收藏案例的索引 (相对于当前页的 cases 数组)，默认为 -1 (表示未选中)
    }
  },
  getters: {
    lang: state => state.lang, // 获取当前语言
    searchParams: state => state.searchParams, // 获取主搜索页参数
    favoriteSearchParams: state => state.favoriteSearchParams // 获取收藏夹页参数
  },
  mutations: {
    setLang(state, lang) {
      state.lang = lang; // 设置语言
      // state.searchParams.language = lang; // 移除此行，不再同步到主搜索参数
    },
    setSearchParams(state, params) {
      // 合并传入的参数，但不包含 language 字段
      const {  ...restParams } = params; // 解构出 language，避免其被合并
      state.searchParams = { ...state.searchParams, ...restParams };
      // state.searchParams.language = state.lang; // 移除此行
    },
    setSearchKeyword(state, keyword) {
      state.searchParams.keyword = keyword;
    },
    setSearchPeriod(state, period) {
      state.searchParams.period = period;
    },
    setSearchCountry(state, country) {
      state.searchParams.country = country;
    },

    // 收藏夹页面的 mutations
    setFavoriteSearchPagenum(state, pagenum) {
      state.favoriteSearchParams.pagenum = pagenum;
    },
    setFavoriteSearchSelectedIndex(state, selectedIndex) {
      state.favoriteSearchParams.selectedIndex = selectedIndex;
    },

    // 允许一次性设置多个收藏夹搜索参数
    setFavoriteSearchParams(state, params) {
      state.favoriteSearchParams = { ...state.favoriteSearchParams, ...params };
    },
  },
  actions: {
  },
  modules: {
  }
})