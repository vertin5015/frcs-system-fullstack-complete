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
                <image class="banner-image" :src="item.image" mode="aspectFill" />
              </view>
            </swiper-item>
          </swiper>
        </view>

        <view class="search-card">
          <view class="filter-row">
            <view class="filter-btn" @tap="openCountrySelect">
              <image class="filter-icon" src="/static/icons/earth.png" mode="aspectFit" />
              <text class="filter-text">{{ selectedCountry }}</text>
              <image class="arrow-icon" src="/static/icons/arrow-down.png" mode="aspectFit" />
            </view>

            <view class="filter-btn" @tap="openTimeSelect">
              <image class="filter-icon" src="/static/icons/calendar.png" mode="aspectFit" />
              <text class="filter-text">{{ selectedTime }}</text>
              <image class="arrow-icon" src="/static/icons/arrow-down.png" mode="aspectFit" />
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
              <image class="title-icon" src="/static/icons/bookmark.png" mode="aspectFit" />
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
              :key="caseInfo.id"
              @tap="handleCaseDetail(caseInfo.id)"
            >
              <view class="card-header">
                <view class="country-tag">
                  <image class="country-icon" src="/static/icons/flag.png" mode="aspectFit" />
                  <text>{{ caseInfo.country }}</text>
                </view>
                <view class="ai-status" v-if="caseInfo.aiSummary">
                  <image class="check-icon" src="/static/icons/check-green.png" mode="aspectFit" />
                  <text class="status-text">AI摘要已生成</text>
                </view>
              </view>

              <view class="card-body">
                <view class="text-group">
                  <text class="title">{{ caseInfo.title }}</text>
                  <text class="en-title">{{ caseInfo.englishTitle }}</text>
                </view>
                <image class="nav-arrow" src="/static/icons/arrow-right.png" mode="aspectFit" />
              </view>

              <view class="card-footer">
                <image class="time-icon" src="/static/icons/time.png" mode="aspectFit" />
                <text class="time-text">{{ caseInfo.time }}</text>
              </view>
            </view>
          </view>
        </view>

      </view>
    </scroll-view>

    <view class="floating-ai-btn" @tap="handleAIChat">
      <view class="ai-content">
        <image class="ai-icon" src="/static/icons/ai-chat.png" mode="aspectFit" />
        <text class="ai-text">问AI</text>
      </view>
    </view>

    <BottomTabBar activeTab="home" />
  </view>
</template>

<script setup>
import { ref } from 'vue'
import BottomTabBar from '@/components/BottomTabBar.vue'

// ================= 数据定义 =================
const bannerList = ref([
  {
    id: 1,
    title: '涉外案例查询分析',
    subtitle: '检索美国、欧盟、日本相关案例，\n查看原文、AI摘要、收藏与历史记录。',
    image: '/static/images/banner_bg.png' 
  }
])

const keyword = ref('')
const selectedCountry = ref('全部国家')
const selectedTime = ref('全部时间')

const favoriteCases = ref([
  {
    id: 1,
    country: '美国',
    title: '跨境合同纠纷案件',
    englishTitle: 'Contract Dispute Case',
    time: '2026-7-9 2小时前',
    aiSummary: true
  }
])

// ================= 交互方法 =================
const handleBannerClick = () => {
  uni.navigateTo({ url: '/pages/intro/index' })
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
    itemList: ['全部时间', '最近一周', '最近一月', '最近一年'],
    success: (res) => {
      const options = ['全部时间', '最近一周', '最近一月', '最近一年']
      selectedTime.value = options[res.tapIndex]
    }
  })
}

const handleSearch = () => {
  if (!keyword.value.trim()) {
    uni.showToast({ title: '请输入关键词', icon: 'none' })
    return
  }
  uni.navigateTo({
    url: `/pages/case/list?keyword=${encodeURIComponent(keyword.value)}&country=${selectedCountry.value}&time=${selectedTime.value}`
  })
}

const handleViewAllFavorites = () => {
  const targetPath = '/pages/favorite/index'
  uni.switchTab({
    url: targetPath,
    fail: () => uni.reLaunch({ url: targetPath })
  })
}

const handleCaseDetail = (caseId) => {
  uni.navigateTo({ url: `/pages/case/detail?id=${caseId}` })
}

const handleAIChat = () => {
  uni.navigateTo({ url: '/pages/ai/chat' })
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

.banner-image {
  position: absolute;
  right: -20rpx;
  bottom: -20rpx;
  width: 240rpx;
  height: 240rpx;
  opacity: 0.8;
  z-index: 1;
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

  .filter-icon { width: 32rpx; height: 32rpx; margin-right: 12rpx; }
  .filter-text { flex: 1; font-size: 28rpx; color: #333333; }
  .arrow-icon { width: 24rpx; height: 24rpx; }
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
  background-color: rgba(255, 255, 255, 0.8);
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
  .title-icon { width: 36rpx; height: 36rpx; margin-right: 12rpx; }
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
  gap: 24rpx;
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
  .country-icon { width: 24rpx; height: 24rpx; margin-right: 8rpx; }
}

.ai-status {
  display: flex;
  align-items: center;
  .check-icon { width: 24rpx; height: 24rpx; margin-right: 8rpx; }
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

.nav-arrow { width: 32rpx; height: 32rpx; opacity: 0.5; }

.card-footer {
  display: flex;
  align-items: center;
  .time-icon { width: 24rpx; height: 24rpx; margin-right: 8rpx; }
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
  .ai-icon { width: 44rpx; height: 44rpx; margin-bottom: 4rpx; }
  .ai-text { font-size: 20rpx; color: #218CFF; font-weight: 600; }
}
</style>