<template>
  <view class="search-page-container">
    <view class="header-safe-area"></view>

    <view class="top-fixed-section">
      <view class="search-bar-row">
        <view class="mode-toggle-btn" @tap="toggleSearchMode">
          <text class="toggle-icon">⇅</text>
          <text class="toggle-text">{{ searchMode === 'case' ? '案例' : '条文' }}</text>
        </view>
        
        <view class="vertical-divider"></view>

        <input 
          class="search-input" 
          v-model="keyword" 
          :placeholder="searchMode === 'case' ? '输入案件关键词...' : '输入条文关键词...'" 
          placeholder-class="placeholder-style"
          confirm-type="search"
          @confirm="handleSearch(true)"
        />

        <view v-if="keyword" class="clear-btn" @tap="clearSearch">×</view>
        <view class="search-submit-btn" @tap="handleSearch(true)">
          <image class="search-icon" src="/static/icons/tab-search-active.png" mode="aspectFit" />
        </view>
      </view>

      <view class="filter-row">
        <view class="filter-btn" @tap="openCountrySelect">
          <text class="filter-text">{{ selectedCountry }}</text>
          <image class="arrow-icon" src="/static/icons/arrow-down.png" mode="aspectFit" />
        </view>

        <view class="filter-btn" v-if="searchMode === 'case'" @tap="openTimeSelect">
          <text class="filter-text">{{ selectedTime }}</text>
          <image class="arrow-icon" src="/static/icons/arrow-down.png" mode="aspectFit" />
        </view>

        <view class="filter-btn" @tap="openSourceSelect">
          <text class="filter-text">{{ selectedSource }}</text>
          <image class="arrow-icon" src="/static/icons/arrow-down.png" mode="aspectFit" />
        </view>
      </view>
    </view>

    <scroll-view 
      scroll-y 
      class="main-scroll" 
      @scrolltolower="loadMoreData"
    >
      <view class="content-wrapper">
        
        <view v-if="!isSearched" class="hot-search-section">
          <view class="section-title">
            <text>热门搜索</text>
          </view>
          <view class="tags-container">
            <view 
              class="hot-tag" 
              v-for="(tag, index) in currentHotSearches" 
              :key="index"
              @tap="clickHotSearch(tag)"
            >
              {{ tag }}
            </view>
          </view>
        </view>

        <view v-else class="results-section">
          <view class="result-stats" v-if="!loading || listData.length > 0">
            为您找到相关{{ searchMode === 'case' ? '案例' : '条文' }}共 <text class="highlight">{{ totalCount }}</text> 条
          </view>

          <view v-if="listData.length === 0 && !loading" class="empty-state">
            <text>暂无匹配的搜索结果</text>
          </view>

          <view class="list-container">
            <block v-if="searchMode === 'case'">
              <view class="result-card case-card" v-for="item in listData" :key="item.case_id" @tap="goToDetail(item.case_id)">
                <view class="card-header">
                  <view class="country-tag">{{ countryName(item.country) }}</view>
                  <view class="type-tag">{{ item.citationCount != null ? `引用 ${item.citationCount}` : '涉外案例' }}</view>
                </view>
                <view class="card-title">{{ item.case_name }}</view>
                <view class="card-subtitle">{{ item.tags || '暂无摘要' }}</view>
                <view class="card-footer">
                  <text class="date-text">{{ item.judgement_date || '-' }}</text>
                  <text class="ai-status" v-if="item.isfavored">已收藏</text>
                </view>
              </view>
            </block>

            <block v-else>
              <view class="result-card law-card" v-for="item in listData" :key="item.chunkId" @tap="goToLawDetail(item)">
                <view class="card-title">{{ item.title }}</view>
                <view class="card-footer" style="margin-top: 16rpx;">
                  <text class="type-tag">{{ item.sourceId }}</text>
                  <text class="date-text">{{ item.preview ? item.preview.slice(0, 30) + '…' : '暂无摘要' }}</text>
                </view>
              </view>
            </block>
          </view>

          <view class="load-more-text">
            <text v-if="loading">努力加载中...</text>
            <text v-else-if="!hasMore && listData.length > 0">- 已经到底啦 -</text>
          </view>
        </view>

      </view>
    </scroll-view>

    <BottomTabBar activeTab="search" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue';
import BottomTabBar from '../../components/BottomTabBar.vue';
import api from '../../api';
import { useUserStore } from '../../store/user';
import { onLoad, onShow } from '@dcloudio/uni-app'
import type { KbHit } from '../../types/case'

onShow(() => {
  uni.hideTabBar({
    animation: false // 瞬间隐藏，不要动画，避免闪烁
  })
})

const userStore = useUserStore()
// ================= 状态定义 =================
const searchMode = ref<'case' | 'law'>('case');
const keyword = ref('');
const isSearched = ref(false);

// 筛选状态
const selectedCountry = ref('全部国家');
const selectedTime = ref('全部时间');
const selectedSource = ref('全部数据源');

// 案例与条文两种形态复用同一个列表
const listData = ref<any[]>([]);
const page = ref(1);
const pageSize = 10;
const totalCount = ref(0);
const hasMore = ref(true);
const loading = ref(false);

const countryName = (code?: string) => {
  if (code === 'US') return '美国'
  if (code === 'EU') return '欧盟'
  if (code === 'JPN') return '日本'
  return code || ''
}

const safeDecode = (v?: string) => {
  if (!v) return ''
  try {
    return decodeURIComponent(v)
  } catch {
    return v
  }
}

// 从首页/搜索入口携带参数进入
onLoad((query) => {
  if (query?.keyword) keyword.value = safeDecode(String(query.keyword))
  const countryMap: Record<string, string> = { US: '美国', EU: '欧盟', JPN: '日本' }
  const timeMap: Record<string, string> = { '1': '最近一年', '3': '最近三年', '5': '最近五年', '10': '最近十年' }
  const country = safeDecode(String(query?.country || ''))
  const period = safeDecode(String(query?.period || ''))
  if (country && countryMap[country]) {
    selectedCountry.value = countryMap[country]
  }
  if (period && timeMap[period]) {
    selectedTime.value = timeMap[period]
  }
  if (keyword.value) handleSearch(true)
})

// ================= 热门搜索数据 =================
const caseHotSearches = ['苹果专利纠纷', 'GDPR数据违规', '跨境电商合同', '反倾销调查', '离岸公司股权'];
const lawHotSearches = ['美国统一商法典', '欧盟AI法案', '联邦民事诉讼规则', '海外反腐败法'];

const currentHotSearches = computed(() => {
  return searchMode.value === 'case' ? caseHotSearches : lawHotSearches;
});

// ================= 筛选交互逻辑 =================

const openCountrySelect = () => {
  const options = ['全部国家', '美国', '欧盟', '日本'];
  uni.showActionSheet({
    itemList: options,
    success: (res) => {
      if (selectedCountry.value !== options[res.tapIndex]) {
        selectedCountry.value = options[res.tapIndex];
        if (isSearched.value) handleSearch(true); 
      }
    }
  });
};

const openTimeSelect = () => {
  const options = ['全部时间', '最近一年', '最近三年', '最近五年', '最近十年'];
  uni.showActionSheet({
    itemList: options,
    success: (res) => {
      if (selectedTime.value !== options[res.tapIndex]) {
        selectedTime.value = options[res.tapIndex];
        if (isSearched.value) handleSearch(true);
      }
    }
  });
};

const openSourceSelect = () => {
  const options = ['全部数据源', '美国', '欧盟', '日本'];
  uni.showActionSheet({
    itemList: options,
    success: (res) => {
      if (selectedSource.value !== options[res.tapIndex]) {
        selectedSource.value = options[res.tapIndex];
        if (isSearched.value) handleSearch(true);
      }
    }
  });
};

// ================= 搜索与加载逻辑 =================

const toggleSearchMode = () => {
  searchMode.value = searchMode.value === 'case' ? 'law' : 'case';
  // 切换模式时重置所有筛选状态
  selectedCountry.value = '全部国家';
  selectedTime.value = '全部时间';
  selectedSource.value = '全部数据源';
  clearSearch();
};

const clickHotSearch = (tag: string) => {
  keyword.value = tag;
  handleSearch(true);
};

const clearSearch = () => {
  keyword.value = '';
  isSearched.value = false;
  listData.value = [];
  page.value = 1;
};

const handleSearch = async (isRefresh = false) => {
  if (isRefresh) {
    isSearched.value = true;
    page.value = 1;
    listData.value = [];
    hasMore.value = true;
  }

  if (loading.value || !hasMore.value) return;

  loading.value = true;
  try {
    if (searchMode.value === 'case') {
      const countryMap: Record<string, string> = { '全部国家': '', '美国': 'US', '欧盟': 'EU', '日本': 'JPN' }
      const timeMap: Record<string, string> = { '全部时间': '', '最近一年': '1', '最近三年': '3', '最近五年': '5', '最近十年': '10' }
      const sourceMap: Record<string, string> = { '全部数据源': '', '美国': 'US', '欧盟': 'EU', '日本': 'JPN' }
      const params: Record<string, unknown> = {
        keyword: keyword.value.trim(),
        language: 'zh',
        country: countryMap[selectedCountry.value] || '',
        period: timeMap[selectedTime.value] ? Number(timeMap[selectedTime.value]) : '',
        sources: sourceMap[selectedSource.value] || '',
        pagenum: page.value,
        pagesize: pageSize,
        userId: userStore.userId,
      }
      const res = await api.searchCases(params)
      if (res.code !== 200) {
        uni.showToast({ title: res.message || '搜索失败', icon: 'none' })
        return
      }
      if (isRefresh) {
        listData.value = res.data?.cases || []
        totalCount.value = res.data?.totalCount || 0
      } else {
        listData.value.push(...(res.data?.cases || []))
      }
      hasMore.value = (listData.value.length < totalCount.value)
      if (hasMore.value) page.value++
    } else {
      // 条文模式：后端无独立法律列表接口，通过知识库检索命中展示
      const res = await api.kbQuery({ question: keyword.value.trim() || '涉外法律', language: 'zh', topK: 20 })
      if (res.code !== 200) {
        uni.showToast({ title: res.message || '查询失败', icon: 'none' })
        return
      }
      listData.value = res.data?.hits || []
      totalCount.value = listData.value.length
      hasMore.value = false
    }

  } catch (error) {
    console.error('搜索失败:', error)
    uni.showToast({ title: '搜索失败', icon: 'none' });
  } finally {
    loading.value = false;
  }
};

const loadMoreData = () => {
  if (isSearched.value && searchMode.value === 'case') {
    handleSearch(false);
  }
};

const goToDetail = (id: string) => {
  uni.navigateTo({ url: `/pages/case/detail?id=${encodeURIComponent(id)}` })
}

const goToLawDetail = (item: KbHit) => {
  uni.navigateTo({
    url: `/pages/law/detail?sourceId=${encodeURIComponent(item.sourceId || '')}&title=${encodeURIComponent(item.title || '')}&preview=${encodeURIComponent(item.preview || '')}&score=${item.score ?? ''}`
  })
};
</script>

<style lang="scss" scoped>
/* ================= 页面整体 ================= */
.search-page-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #F5F7FA;
  padding-bottom: env(safe-area-inset-bottom);
}

.header-safe-area {
  padding-top: var(--status-bar-height, 44px);
  background-color: #ffffff;
}

/* ================= 顶部固定区 ================= */
.top-fixed-section {
  background-color: #ffffff;
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid #E4E7ED;
  z-index: 100;
}

.search-bar-row {
  display: flex;
  align-items: center;
  background-color: #F0F2F5;
  border-radius: 40rpx;
  height: 80rpx;
  padding: 0 20rpx;
  margin-bottom: 24rpx; 
}

.mode-toggle-btn {
  display: flex;
  align-items: center;
  padding: 0 10rpx;
  .toggle-icon { font-size: 24rpx; color: #218CFF; margin-right: 6rpx; }
  .toggle-text { font-size: 28rpx; font-weight: 500; color: #333333; }
}

.vertical-divider {
  width: 2rpx;
  height: 30rpx;
  background-color: #DCDFE6;
  margin: 0 20rpx;
}

.search-input {
  flex: 1;
  font-size: 28rpx;
  color: #333333;
}

.placeholder-style { color: #999999; }

.clear-btn {
  padding: 10rpx;
  font-size: 36rpx;
  color: #C0C4CC;
  line-height: 1;
}

.search-submit-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 60rpx;
  height: 100%;
  .search-icon { width: 36rpx; height: 36rpx; }
}

/* 筛选栏样式（适配截图：无图标、胶囊形状、平分宽度） */
.filter-row {
  display: flex;
  justify-content: space-between;
  gap: 16rpx; /* 根据项数自动调整间距 */
}

.filter-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #F5F7FA;
  padding: 14rpx 16rpx;
  border-radius: 30rpx;
  flex: 1; /* 保证三个按钮宽度一致 */
  min-width: 0; /* 防止文本过长撑破 flex */
  
  .filter-text {
    font-size: 24rpx;
    color: #606266;
    margin-right: 8rpx;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }
  
  .arrow-icon {
    width: 20rpx;
    height: 20rpx;
    flex-shrink: 0;
    opacity: 0.6;
  }
}

/* ================= 核心滚动区 ================= */
.main-scroll {
  flex: 1;
  height: 0; 
}

.content-wrapper {
  padding: 30rpx 24rpx 140rpx 24rpx; 
}

/* ================= 热门搜索 ================= */
.hot-search-section {
  .section-title {
    font-size: 30rpx;
    font-weight: 600;
    color: #333333;
    margin-bottom: 24rpx;
  }

  .tags-container {
    display: flex;
    flex-wrap: wrap;
    gap: 20rpx;
  }

  .hot-tag {
    background-color: #ffffff;
    color: #606266;
    font-size: 26rpx;
    padding: 12rpx 28rpx;
    border-radius: 30rpx;
    border: 1rpx solid #E4E7ED;
    
    &:active {
      background-color: #F0F2F5;
    }
  }
}

/* ================= 搜索结果 ================= */
.result-stats {
  font-size: 26rpx;
  color: #666666;
  margin-bottom: 24rpx;
  .highlight { color: #218CFF; font-weight: bold; margin: 0 4rpx; }
}

.list-container {
  display: flex;
  flex-direction: column;
  gap: 24rpx;
}

.result-card {
  background-color: #ffffff;
  border-radius: 16rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0,0,0,0.03);
  &:active { background-color: #FAFAFA; }
}

.case-card {
  .card-header { display: flex; margin-bottom: 16rpx; gap: 12rpx; }
  .country-tag { background: #EBF4FF; color: #218CFF; font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 6rpx; }
  .type-tag { background: #F0F2F5; color: #606266; font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 6rpx; }
  .card-title { font-size: 32rpx; font-weight: 600; color: #333; margin-bottom: 12rpx; line-height: 1.4; }
  .card-subtitle { font-size: 26rpx; color: #666; margin-bottom: 20rpx; }
  .card-footer {
    display: flex; justify-content: space-between; align-items: center;
    border-top: 1rpx solid #F0F2F5; padding-top: 16rpx;
    .date-text { font-size: 24rpx; color: #999; }
    .ai-status { font-size: 22rpx; color: #67C23A; background: #f0f9eb; padding: 4rpx 12rpx; border-radius: 6rpx; }
  }
}

.law-card {
  .card-title { font-size: 30rpx; font-weight: 500; color: #333; line-height: 1.4; }
  .card-footer {
    display: flex; justify-content: space-between; align-items: center;
    .type-tag { font-size: 24rpx; color: #218CFF; }
    .date-text { font-size: 24rpx; color: #999; }
  }
}

.empty-state { text-align: center; padding: 100rpx 0; color: #999; font-size: 28rpx; }
.load-more-text { text-align: center; padding: 30rpx 0; color: #999; font-size: 24rpx; }
</style>
