<!-- src/pages/favorite/index.vue -->
<template>
  <view class="favorite-container">

    <!-- 顶部状态栏占位 -->
    <view class="header-safe-area"></view>

    <scroll-view scroll-y class="main-scroll" @scrolltolower="loadMore">
      <view class="content-wrapper">
        
        <!-- ================= 顶部搜索与筛选卡片 ================= -->
        <view class="search-filter-card">
          <!-- 搜索框 -->
          <view class="search-box">
            <input 
              class="search-input" 
              v-model="searchQuery" 
              placeholder="请输入您想查询案件的关键词..." 
              placeholder-class="placeholder-style"
              @confirm="handleSearch"
            />
            <view class="search-btn-circle" @tap="handleSearch">
              <image class="search-icon-white" src="/static/icons/tab-search-active.png" mode="aspectFit" />
            </view>
          </view>

          <!-- 筛选栏 -->
          <view class="filter-row">
            <view class="filter-item" @tap="openCountrySelect">
              <image class="filter-icon" src="/static/icons/earth-blue.png" mode="aspectFit" />
              <text class="filter-text">{{ selectedCountry }}</text>
              <image class="arrow-icon" src="/static/icons/arrow-down-blue.png" mode="aspectFit" />
            </view>
            <view class="filter-item" @tap="openSourceSelect">
              <image class="filter-icon" src="/static/icons/database-blue.png" mode="aspectFit" />
              <text class="filter-text">{{ selectedSource }}</text>
              <image class="arrow-icon" src="/static/icons/arrow-down-blue.png" mode="aspectFit" />
            </view>
            <view class="filter-item" @tap="openTimeSelect">
              <image class="filter-icon" src="/static/icons/calendar-blue.png" mode="aspectFit" />
              <text class="filter-text">{{ selectedTime }}</text>
              <image class="arrow-icon" src="/static/icons/arrow-down-blue.png" mode="aspectFit" />
            </view>
          </view>
        </view>

        <!-- ================= 案例列表区域 ================= -->
        <view class="list-card">
          <template v-if="filteredFavorites.length > 0">
            <view 
              class="favorite-card" 
              v-for="caseInfo in filteredFavorites" 
              :key="caseInfo.caseId"
              @tap="handleCardClick(caseInfo.caseId)"
            >
              <view class="card-header">
                <view class="country-tag">
                  <image class="country-icon" src="/static/icons/flag.png" mode="aspectFit" />
                  <text>{{ countryName(caseInfo.country) }}</text>
                </view>
                <view class="star-btn" @tap.stop="handleUnfavorite(caseInfo.caseId)">
                  <text class="star-text">★ 取消收藏</text>
                </view>
              </view>

              <view class="card-body">
                <view class="text-group">
                  <text class="title">{{ caseInfo.caseName }}</text>
                  <text class="en-title">{{ caseInfo.tags || '暂无摘要' }}</text>
                </view>
                <image class="nav-arrow" src="/static/icons/arrow-right.png" mode="aspectFit" />
              </view>

              <view class="card-footer">
                <image class="time-icon" src="/static/icons/time.png" mode="aspectFit" />
                <text class="time-text">{{ caseInfo.judgementDate || '-' }}</text>
              </view>
            </view>
          </template>

          <view class="empty-state" v-else>
            <text class="empty-text">{{ userStore.isGuest ? '游客用户无法访问收藏夹，请登录后重试' : '未找到符合条件的收藏案例' }}</text>
          </view>

          <view v-if="favoritesList.length > 0" class="load-more-text">
            <text v-if="loading">正在加载更多…</text>
            <text v-else-if="!hasMore">- 已经到底啦 -</text>
          </view>
        </view>

      </view>
    </scroll-view>

    <!-- ================= 底部导航栏 ================= -->
    <BottomTabBar activeTab="favorite" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import BottomTabBar from '../../components/BottomTabBar.vue'
import api from '../../api'
import { useUserStore } from '../../store/user'
import { onShow } from '@dcloudio/uni-app'
import type { FavoriteInfo } from '../../types/case'

onShow(() => {
  uni.hideTabBar({
    animation: false // 瞬间隐藏，不要动画，避免闪烁
  })
  fetchFavorites(true)
})

const userStore = useUserStore()
// ================= 状态定义 =================
const favoritesList = ref<FavoriteInfo[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 10
const totalCount = ref(0)
const hasMore = ref(true)

// 筛选条件
const searchQuery = ref('')
const selectedCountry = ref('全部')
const selectedSource = ref('全部')
const selectedTime = ref('全部')

const countryName = (code?: string) => {
  if (code === 'US') return '美国'
  if (code === 'EU') return '欧盟'
  if (code === 'JPN') return '日本'
  return code || ''
}

const countryCode = (label: string) => {
  if (label === '美国') return 'US'
  if (label === '欧盟') return 'EU'
  if (label === '日本') return 'JPN'
  return ''
}

const periodValue = (label: string) => {
  const map: Record<string, number> = { '最近一年': 1, '最近三年': 3, '最近五年': 5, '最近十年': 10 }
  return map[label] ?? 0
}

const fetchFavorites = async (isRefresh = false) => {
  if (userStore.isGuest) {
    favoritesList.value = []
    return
  }
  if (loading.value) return
  if (!isRefresh && !hasMore.value) return
  if (isRefresh) {
    page.value = 1
    favoritesList.value = []
    hasMore.value = true
  }

  loading.value = true
  try {
    const res = await api.getFavoriteCases({
      userId: userStore.userId,
      language: 'zh',
      country: countryCode(selectedCountry.value),
      period: periodValue(selectedTime.value) || '',
      pagenum: page.value,
      pagesize: pageSize,
    })
    if (res.code !== 200) {
      uni.showToast({ title: res.message || '加载失败', icon: 'none' })
      return
    }
    const list = res.data?.favoriteInfoList || []
    totalCount.value = res.data?.totalCount || 0
    if (isRefresh) {
      favoritesList.value = list
    } else {
      favoritesList.value.push(...list)
    }
    hasMore.value = favoritesList.value.length < totalCount.value
    if (hasMore.value) page.value++
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const loadMore = () => {
  fetchFavorites(false)
}

// ================= 计算属性：本地交叉筛选逻辑 =================
const filteredFavorites = computed(() => {
  return favoritesList.value.filter(item => {
    // 1. 关键词筛选
    const kw = searchQuery.value.trim().toLowerCase()
    const matchKeyword = !kw || 
      (item.caseName || '').toLowerCase().includes(kw) ||
      (item.tags || '').toLowerCase().includes(kw)

    // 2. 国家筛选
    const matchCountry = selectedCountry.value === '全部' || countryCode(selectedCountry.value) === item.country

    // 3. 数据源筛选
    const matchSource = selectedSource.value === '全部' || countryCode(selectedSource.value) === item.country

    // 4. 时间筛选 (这里使用简单的字符串包含或前缀匹配模拟)
    let matchTime = true
    if (selectedTime.value !== '全部') {
      const year = (item.judgementDate || '').substring(0, 4)
      const py = parseInt(year, 10)
      if (selectedTime.value === '2023年及以后' && parseInt(year) >= 2023) matchTime = true
      else if (selectedTime.value === '2022年' && year === '2022') matchTime = true
      else if (selectedTime.value === '2021年及以前' && parseInt(year) <= 2021) matchTime = true
      else {
        // 最近一年/三年/五年/十年 按当前年份回推
        const now = new Date().getFullYear()
        if (selectedTime.value === '最近一年' && py >= now - 1) matchTime = true
        else if (selectedTime.value === '最近三年' && py >= now - 3) matchTime = true
        else if (selectedTime.value === '最近五年' && py >= now - 5) matchTime = true
        else if (selectedTime.value === '最近十年' && py >= now - 10) matchTime = true
        else matchTime = false
      }
    }

    return matchKeyword && matchCountry && matchSource && matchTime
  })
})

// ================= 交互方法 =================

const handleSearch = () => {
  // 触发 computed 重新计算
  // 在实际业务中可能触发埋点，此处无需多余操作
}

const openCountrySelect = () => {
  const options = ['全部', '美国', '欧盟', '日本']
  uni.showActionSheet({
    itemList: options,
    success: (res) => {
      if (selectedCountry.value !== options[res.tapIndex]) {
        selectedCountry.value = options[res.tapIndex]
        fetchFavorites(true)
      }
    }
  })
}

const openSourceSelect = () => {
  const options = ['全部', '美国', '欧盟', '日本']
  uni.showActionSheet({
    itemList: options,
    success: (res) => {
      if (selectedSource.value !== options[res.tapIndex]) {
        selectedSource.value = options[res.tapIndex]
      }
    }
  })
}

const openTimeSelect = () => {
  const options = ['全部', '最近一年', '最近三年', '最近五年', '最近十年']
  uni.showActionSheet({
    itemList: options,
    success: (res) => {
      if (selectedTime.value !== options[res.tapIndex]) {
        selectedTime.value = options[res.tapIndex]
        fetchFavorites(true)
      }
    }
  })
}

const handleCardClick = (id: string) => {
  uni.navigateTo({ url: `/pages/case/detail?id=${encodeURIComponent(id)}` })
}

const handleUnfavorite = async (caseId: string) => {
  if (userStore.isGuest) return
  try {
    const res = await api.cancelFavoriteCase(caseId, userStore.userId)
    if (res.code !== 200) {
      uni.showToast({ title: res.message || '取消收藏失败', icon: 'none' })
      return
    }
    favoritesList.value = favoritesList.value.filter((item) => item.caseId !== caseId)
    uni.showToast({ title: '已取消收藏', icon: 'none' })
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '操作失败', icon: 'none' })
  }
}
</script>

<style lang="scss" scoped>
/* ================= 整体页面样式 ================= */
.favorite-container {
  height: 100vh;
  background-color: #F0F4FA; /* 淡淡的蓝灰背景底色 */
  display: flex;
  flex-direction: column;
  position: relative;
  padding-bottom: env(safe-area-inset-bottom);
}

.header-safe-area {
  padding-top: var(--status-bar-height, 44px);
  background-color: #ffffff; /* 确保顶部原生导航栏区域无缝过渡 */
}

.main-scroll {
  flex: 1;
  height: 0;
  
  .content-wrapper {
    padding: 24rpx;
    padding-bottom: 140rpx;
  }
}

/* ================= 搜索与筛选卡片 ================= */
.search-filter-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 30rpx 24rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(33, 140, 255, 0.05);
}

.search-box {
  display: flex;
  align-items: center;
  border: 2rpx solid #E4E7ED;
  border-radius: 40rpx;
  height: 80rpx;
  padding: 0 10rpx 0 30rpx;
  margin-bottom: 30rpx;
  
  .search-input {
    flex: 1;
    font-size: 28rpx;
    color: #333333;
  }
  
  .placeholder-style {
    color: #999999;
  }
  
  .search-btn-circle {
    width: 64rpx;
    height: 64rpx;
    background-color: #4A90E2;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .search-icon-white {
      width: 32rpx;
      height: 32rpx;
      filter: brightness(0) invert(1);
    }
    
    &:active {
      opacity: 0.8;
    }
  }
}

.filter-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.filter-item {
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #F5F7FA;
  padding: 12rpx 20rpx;
  border-radius: 12rpx;
  flex: 1;
  margin: 0 10rpx;
  
  &:first-child { margin-left: 0; }
  &:last-child { margin-right: 0; }

  .filter-icon {
    width: 28rpx;
    height: 28rpx;
    margin-right: 8rpx;
  }
  
  .filter-text {
    font-size: 24rpx;
    color: #4A90E2; /* 主题蓝 */
    flex: 1;
    text-align: center;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .arrow-icon {
    width: 20rpx;
    height: 20rpx;
    margin-left: 4rpx;
  }
}

/* ================= 列表卡片容器 ================= */
.list-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(33, 140, 255, 0.05);
  display: flex;
  flex-direction: column;
  gap: 20rpx;
}

.favorite-card {
  background-color: #ffffff;
  border: 1rpx solid #E4E7ED;
  border-radius: 16rpx;
  padding: 24rpx;
  box-shadow: 0 2rpx 8rpx rgba(0,0,0,0.02);
  
  &:active {
    background-color: #FAFAFA;
  }
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
  font-size: 26rpx;
  font-weight: bold;
  color: #333333;
  
  .country-icon {
    width: 36rpx;
    height: 36rpx;
    margin-right: 12rpx;
  }
}

.ai-status {
  display: flex;
  align-items: center;
  background-color: #F0F9EB;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  
  .check-icon {
    width: 20rpx;
    height: 20rpx;
    margin-right: 6rpx;
  }
  
  .status-text {
    font-size: 20rpx;
    color: #67C23A;
  }
}

.card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16rpx;
}

.text-group {
  display: flex;
  flex-direction: column;
  flex: 1;
  padding-right: 20rpx;
  
  .title {
    font-size: 28rpx;
    font-weight: 500;
    color: #333333;
    margin-bottom: 8rpx;
    line-height: 1.4;
  }
  
  .en-title {
    font-size: 24rpx;
    color: #999999;
  }
}

.nav-arrow {
  width: 32rpx;
  height: 32rpx;
}

.card-footer {
  display: flex;
  align-items: center;
  
  .time-icon {
    width: 24rpx;
    height: 24rpx;
    margin-right: 8rpx;
    opacity: 0.6;
  }
  
  .time-text {
    font-size: 24rpx;
    color: #999999;
  }
}

.empty-state {
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 100rpx 0;
  
  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}

.load-more-text {
  text-align: center;
  padding: 24rpx 0;
  color: #999999;
  font-size: 24rpx;
}

.star-btn {
  background: #f0f9eb;
  padding: 6rpx 16rpx;
  border-radius: 20rpx;
  .star-text {
    font-size: 22rpx;
    color: #67c23a;
  }
}
</style>
