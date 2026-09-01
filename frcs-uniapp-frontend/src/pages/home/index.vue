<template>
  <view class="home-container">
    <scroll-view scroll-y class="main-scroll">
      <view class="content-wrapper">
        
        <view class="banner-container" @tap="handleBannerClick">
          <swiper class="banner-swiper" indicator-dots indicator-color="rgba(255,255,255,0.5)" indicator-active-color="#ffffff" autoplay circular>
            <swiper-item v-for="item in bannerList" :key="item.id">
              <view class="banner-card">
                <view class="text-content">
                  <text class="banner-title">{{ item.title }}</text>
                  <text class="banner-subtitle">{{ item.subtitle }}</text>
                </view>
                <view class="banner-deco">
                  <view class="deco-circle circle-1"></view>
                  <view class="deco-circle circle-2"></view>
                  <text class="deco-symbol">⚖️</text>
                </view>
              </view>
            </swiper-item>
          </swiper>
        </view>

        <view class="search-card">
          <view class="filter-row">
            <view class="filter-btn" @tap="openCountrySelect">
              <text class="filter-icon">🌍</text>
              <text class="filter-text">{{ selectedCountry }}</text>
              <text class="arrow-icon">▾</text>
            </view>

            <view class="filter-btn" @tap="openTimeSelect">
              <text class="filter-icon">📅</text>
              <text class="filter-text">{{ selectedTime }}</text>
              <text class="arrow-icon">▾</text>
            </view>
          </view>

          <view class="textarea-wrapper">
            <textarea 
              class="search-input" 
              v-model="keyword" 
              maxlength="100" 
              placeholder="请输入关键词，例如：跨境合同争议..." 
              placeholder-class="placeholder-style"
            />
            <text class="char-count">{{ keyword.length }}/100</text>
          </view>

          <button class="search-btn" @tap="handleSearch">检索案例</button>
        </view>

        <view class="favorites-section">
          <view class="section-header">
            <view class="header-left">
              <text class="title-icon">⭐</text>
              <text class="title-text">最近收藏</text>
            </view>
            <view class="header-right" @tap="handleViewAllFavorites">
              <text class="view-all-text">查看全部</text>
              <text class="arrow-right">></text>
            </view>
          </view>

          <view class="favorites-list">
            <view 
              class="favorite-card" 
              v-for="caseInfo in favoriteCases" 
              :key="caseInfo.caseId"
              @tap="handleCaseDetail(caseInfo.caseId)"
            >
              <view class="card-header">
                <view class="country-tag">
                  <view class="country-dot"></view>
                  <text>{{ countryName(caseInfo.country) }}</text>
                </view>
                <view class="ai-status" v-if="caseInfo.tags">
                  <text class="check-icon">✓</text>
                  <text class="status-text">已收藏</text>
                </view>
              </view>

              <view class="card-body">
                <view class="text-group">
                  <text class="title clamp-2">{{ caseInfo.caseName }}</text>
                  <text class="en-title clamp-2">{{ caseInfo.tags || '暂无摘要' }}</text>
                </view>
                <text class="nav-arrow">›</text>
              </view>

              <view class="card-footer">
                <text class="time-icon">🕐</text>
                <text class="time-text">{{ caseInfo.judgementDate || '-' }}</text>
              </view>
            </view>
          </view>

          <view v-if="!loadingFavorites && favoriteCases.length === 0" class="empty-favorites">
            <text class="empty-icon">📌</text>
            <text class="empty-text">{{ userStore.isGuest ? '游客模式不提供收藏，请登录查看' : '暂无收藏案件' }}</text>
          </view>
        </view>

      </view>
    </scroll-view>

    <view class="floating-ai-btn" @tap="handleAIChat">
      <view class="ai-content">
        <text class="ai-icon">🤖</text>
        <text class="ai-text">问AI</text>
      </view>
    </view>

    <BottomTabBar activeTab="home" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BottomTabBar from '../../components/BottomTabBar.vue'
import { onShow } from '@dcloudio/uni-app'
import api from '../../api'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()

onShow(() => {
  uni.hideTabBar({
    animation: false // 瞬间隐藏，不要动画，避免闪烁
  })
  loadRecentFavorites()
})

// ================= 数据定义 =================
const bannerList = ref([
  {
    id: 1,
    title: '涉外案例查询分析',
    subtitle: '检索美国、欧盟、日本相关案例，\n查看原文、AI摘要、收藏与历史记录。',
  }
])

const keyword = ref('')
const selectedCountry = ref('全部国家')
const selectedTime = ref('全部时间')

const favoriteCases = ref<any[]>([])
const loadingFavorites = ref(false)

const countryName = (code?: string) => {
  if (code === 'US') return '美国'
  if (code === 'EU') return '欧盟'
  if (code === 'JPN') return '日本'
  return code || ''
}

const loadRecentFavorites = async () => {
  if (userStore.isGuest) {
    favoriteCases.value = []
    return
  }
  loadingFavorites.value = true
  try {
    const res = await api.getFavoriteCases({
      userId: userStore.userId,
      language: 'zh',
      country: '',
      period: '',
      pagenum: 1,
      pagesize: 10,
    })
    if (res.code === 200) {
      favoriteCases.value = res.data?.favoriteInfoList || []
    } else {
      favoriteCases.value = []
    }
  } catch (e) {
    console.error('加载最近收藏失败:', e)
    favoriteCases.value = []
  } finally {
    loadingFavorites.value = false
  }
}

// ================= 交互方法 =================
const handleBannerClick = () => {
  uni.navigateTo({ url: '/pages/case/list' })
}

const openCountrySelect = () => {
  uni.showActionSheet({
    itemList: ['全部国家', '美国', '欧盟', '日本'],
    success: (res) => {
      const options = ['全部国家', '美国', '欧盟', '日本']
      selectedCountry.value = options[res.tapIndex]
    }
  })
}

const openTimeSelect = () => {
  uni.showActionSheet({
    itemList: ['全部时间', '最近一年', '最近三年', '最近五年', '最近十年'],
    success: (res) => {
      const options = ['全部时间', '最近一年', '最近三年', '最近五年', '最近十年']
      selectedTime.value = options[res.tapIndex]
    }
  })
}

const handleSearch = () => {
  if (!keyword.value.trim()) {
    uni.showToast({ title: '请输入关键词', icon: 'none' })
    return
  }
  const countryMap: Record<string, string> = { '全部国家': '', '美国': 'US', '欧盟': 'EU', '日本': 'JPN' }
  const timeMap: Record<string, string> = { '全部时间': '', '最近一年': '1', '最近三年': '3', '最近五年': '5', '最近十年': '10' }
  uni.navigateTo({
    url: `/pages/case/list?keyword=${encodeURIComponent(keyword.value)}&country=${countryMap[selectedCountry.value] || ''}&period=${timeMap[selectedTime.value] || ''}`
  })
}

const handleViewAllFavorites = () => {
  const targetPath = '/pages/favorite/index'
  uni.switchTab({
    url: targetPath,
    fail: () => uni.reLaunch({ url: targetPath })
  })
}

const handleCaseDetail = (caseId: string) => {
  uni.navigateTo({ url: `/pages/case/detail?id=${caseId}` })
}

const handleAIChat = () => {
  uni.navigateTo({ url: '/pages/agent/index' })
}
</script>

<style lang="scss" scoped>
/* ================= 整体页面样式 ================= */
.home-container {
  height: 100vh; /* 撑满全屏，适配原生导航栏下方区域 */
  background-color: #F5F7FA;
  display: flex;
  flex-direction: column;
  position: relative;
  padding-bottom: env(safe-area-inset-bottom);
}

.main-scroll {
  flex: 1;
  height: 0; /* Flex 布局下的滚动区域高度黑科技 */
  
  .content-wrapper {
    padding: 24rpx;
    padding-bottom: 140rpx; /* 留出足够高度给底部导航栏，防止内容被遮挡 */
  }
}

/* ================= Banner 样式 ================= */
.banner-container {
  margin-bottom: 30rpx; /* 加大区块间距 */
  border-radius: 20rpx;
  overflow: hidden;
  box-shadow: 0 8rpx 24rpx rgba(33, 140, 255, 0.12);
}

.banner-swiper {
  height: 280rpx;
}

.banner-card {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #218CFF 0%, #5CB3FF 100%);
  position: relative;
  display: flex;
  align-items: center;
  padding: 40rpx;
  box-sizing: border-box;
}

.text-content {
  flex: 1;
  z-index: 2;
  display: flex;
  flex-direction: column;
}

.banner-title {
  font-size: 36rpx;
  font-weight: bold;
  color: #ffffff;
  margin-bottom: 16rpx;
}

.banner-subtitle {
  font-size: 24rpx;
  color: rgba(255, 255, 255, 0.9);
  line-height: 1.5;
  white-space: pre-line;
}

.banner-deco {
  position: absolute;
  right: -20rpx;
  bottom: -20rpx;
  width: 240rpx;
  height: 240rpx;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: center;

  .deco-circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.16);

    &.circle-1 { width: 240rpx; height: 240rpx; }
    &.circle-2 { width: 150rpx; height: 150rpx; background: rgba(255, 255, 255, 0.22); }
  }

  .deco-symbol {
    position: relative;
    z-index: 2;
    font-size: 88rpx;
    opacity: 0.95;
  }
}

/* ================= 搜索区域样式 ================= */
.search-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  margin-bottom: 30rpx;
}

.filter-row {
  display: flex;
  justify-content: space-between;
  margin-bottom: 24rpx;
}

.filter-btn {
  display: flex;
  align-items: center;
  background-color: #F5F7FA;
  padding: 18rpx 24rpx;
  border-radius: 14rpx;
  flex: 1;
  margin: 0 12rpx;
  
  &:first-child { margin-left: 0; }
  &:last-child { margin-right: 0; }

  .filter-icon { font-size: 30rpx; line-height: 1; margin-right: 10rpx; }
  .filter-text { flex: 1; font-size: 28rpx; color: #333333; }
  .arrow-icon { font-size: 22rpx; line-height: 1; color: #999999; margin-left: 6rpx; }
}

.textarea-wrapper {
  background-color: #F5F7FA;
  border-radius: 16rpx;
  padding: 24rpx;
  position: relative;
  margin-bottom: 30rpx;
}

.search-input {
  width: 100%;
  height: 150rpx;
  font-size: 28rpx;
  color: #333333;
  line-height: 1.5;
}

.placeholder-style { color: #999999; }
.char-count { position: absolute; right: 24rpx; bottom: 24rpx; font-size: 24rpx; color: #999999; }

.search-btn {
  background-color: #218CFF;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 500;
  border-radius: 16rpx;
  height: 88rpx;
  line-height: 88rpx;
  &::after { border: none; }
  &:active { background-color: #1a73d9; }
}

/* ================= 收藏区域及卡片样式 ================= */
.favorites-section {
  background-color: #FFFFFF;
  border-radius: 24rpx;
  padding: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24rpx;
}

.header-left {
  display: flex;
  align-items: center;
  .title-icon { font-size: 30rpx; line-height: 1; margin-right: 10rpx; }
  .title-text { font-size: 32rpx; font-weight: 600; color: #333333; }
}

.header-right {
  display: flex;
  align-items: center;
  padding: 10rpx 0;
  .view-all-text { font-size: 26rpx; color: #666666; margin-right: 8rpx; }
  .arrow-right { font-size: 26rpx; color: #666666; }
}

.favorites-list {
  display: flex;
  flex-direction: column;

  .favorite-card + .favorite-card {
    margin-top: 24rpx;
  }
}

.empty-favorites {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  padding: 70rpx 0;

  .empty-icon { font-size: 64rpx; line-height: 1; margin-bottom: 16rpx; opacity: 0.7; }
  .empty-text { color: #999999; font-size: 26rpx; }
}

.favorite-card {
  background-color: #ffffff;
  border: 1rpx solid #E4E7ED;
  border-radius: 16rpx;
  padding: 26rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
  &:active { background-color: #FAFAFA; }
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.country-tag {
  display: flex;
  align-items: center;
  background-color: #EBF4FF;
  color: #218CFF;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
  .country-dot {
    width: 12rpx;
    height: 12rpx;
    border-radius: 50%;
    background: #218CFF;
    margin-right: 8rpx;
  }
}

.ai-status {
  display: flex;
  align-items: center;
  .check-icon {
    font-size: 22rpx;
    line-height: 1;
    color: #67C23A;
    font-weight: bold;
    margin-right: 6rpx;
  }
  .status-text { font-size: 22rpx; color: #67C23A; }
}

.card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20rpx;
}

.text-group {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding-right: 20rpx;
  .title { font-size: 30rpx; font-weight: 500; color: #333333; margin-bottom: 10rpx; }
  .en-title { font-size: 24rpx; color: #999999; }
}

.nav-arrow { font-size: 40rpx; line-height: 1; color: #C0C4CC; }

.card-footer {
  display: flex;
  align-items: center;
  .time-icon { font-size: 24rpx; line-height: 1; margin-right: 8rpx; }
  .time-text { font-size: 24rpx; color: #999999; }
}

/* ================= 悬浮 AI 按钮样式 ================= */
.floating-ai-btn {
  position: fixed;
  right: 32rpx;
  bottom: 150rpx; /* 调整高度使其刚好悬浮在导航栏之上 */
  width: 110rpx;
  height: 110rpx;
  background-color: #ffffff;
  border-radius: 50%;
  box-shadow: 0 8rpx 24rpx rgba(33, 140, 255, 0.25);
  border: 2rpx solid #EBF4FF;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 900;
  &:active {
    background-color: #F5F7FA;
    transform: scale(0.95);
  }
}

.ai-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  .ai-icon { font-size: 44rpx; line-height: 1; margin-bottom: 4rpx; }
  .ai-text { font-size: 20rpx; color: #218CFF; font-weight: 600; }
}
</style>
