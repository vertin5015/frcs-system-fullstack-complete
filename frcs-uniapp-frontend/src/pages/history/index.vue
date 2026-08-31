<template>
  <view class="history-container">
    
    <view class="header-safe-area"></view>

    <scroll-view scroll-y class="main-scroll" v-if="records.length > 0" @scrolltolower="loadMore">
      <view class="content-wrapper">
        
        <view class="records-card">
          
          <template v-if="recentRecords.length > 0">
            <view class="group-header first-header">
              <text class="header-text">一周内</text>
            </view>
            
            <view 
              class="case-item recent-item" 
              v-for="item in recentRecords" 
              :key="item.caseId"
              @tap="handleCardClick(item.caseId)"
            >
              <view class="card-header">
                <view class="country-tag">
                  <image class="country-icon" src="/static/icons/flag.png" mode="aspectFit" />
                  <text>{{ countryName(item.country) }}</text>
                </view>
                <view class="ai-status" v-if="item.tags">
                  <image class="check-icon" src="/static/icons/check-green.png" mode="aspectFit" />
                  <text class="status-text">有摘要</text>
                </view>
              </view>

              <view class="card-body">
                <text class="title">{{ item.caseName }}</text>
                <text class="en-title">{{ item.tags || '暂无摘要' }}</text>
              </view>

              <view class="card-footer">
                <view class="time-box">
                  <image class="time-icon" src="/static/icons/time.png" mode="aspectFit" />
                  <text class="time-text">{{ formatTime(item.browseTime) }}</text>
                </view>
                <image class="nav-arrow" src="/static/icons/arrow-right.png" mode="aspectFit" />
              </view>
            </view>
          </template>

          <template v-if="earlierRecords.length > 0">
            <view class="group-header">
              <text class="header-text">一周外</text>
            </view>
            
            <view 
              class="case-item earlier-item" 
              v-for="(item, index) in earlierRecords" 
              :key="item.caseId"
              :class="{ 'no-border': index === earlierRecords.length - 1 }"
              @tap="handleCardClick(item.caseId)"
            >
              <view class="earlier-left">
                <view class="country-tag">
                  <image class="country-icon" src="/static/icons/flag.png" mode="aspectFit" />
                  <text>{{ countryName(item.country) }}</text>
                </view>
                <text class="title">{{ item.caseName }}</text>
                <text class="en-title">{{ item.tags || '暂无摘要' }}</text>
              </view>
              
              <view class="earlier-right">
                <image class="star-icon" src="/static/icons/tab-star.png" mode="aspectFit" />
                <image class="nav-arrow" src="/static/icons/arrow-right.png" mode="aspectFit" />
              </view>
            </view>
          </template>

        </view>
        <view class="load-more-text">
          <text v-if="loading">正在加载更多…</text>
          <text v-else-if="!hasMore">- 已经到底啦 -</text>
        </view>
      </view>
    </scroll-view>

    <view class="empty-state" v-else>
      <image class="empty-icon" src="/static/icons/empty.png" mode="aspectFit" />
      <text class="empty-text">暂无浏览记录</text>
    </view>

    <BottomTabBar activeTab="history" />
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import BottomTabBar from '../../components/BottomTabBar.vue'
import api from '../../api'
import { useUserStore } from '../../store/user'
import { onShow } from '@dcloudio/uni-app'
import type { BrowseHistoryInfo } from '../../types/case'

const records = ref<BrowseHistoryInfo[]>([])
const loading = ref(false)
const page = ref(1)
const pageSize = 20
const totalCount = ref(0)
const hasMore = ref(true)

const userStore = useUserStore()

onShow(() => {
  uni.hideTabBar({ animation: false })
  if (userStore.isGuest) {
    records.value = []
    return
  }
  fetchHistory(true)
})

const countryName = (code?: string) => {
  if (code === 'US') return '美国'
  if (code === 'EU') return '欧盟'
  if (code === 'JPN') return '日本'
  return code || ''
}

const fetchHistory = async (isRefresh = false) => {
  if (loading.value) return
  if (!isRefresh && !hasMore.value) return
  if (isRefresh) {
    page.value = 1
    records.value = []
    hasMore.value = true
  }
  loading.value = true
  try {
    const res = await api.getHistoryCases({
      userId: userStore.userId,
      language: 'zh',
      country: '',
      period: '',
      pagenum: page.value,
      pagesize: pageSize,
    })
    if (res.code !== 200) {
      uni.showToast({ title: res.message || '加载失败', icon: 'none' })
      return
    }
    const list = res.data?.browseHistoryInfoList || []
    totalCount.value = res.data?.totalCount || 0
    if (isRefresh) {
      records.value = list
    } else {
      records.value.push(...list)
    }
    hasMore.value = records.value.length < totalCount.value
    if (hasMore.value) page.value++
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const loadMore = () => fetchHistory(false)

const viewTime = (browseTime?: string) => {
  if (!browseTime) return 0
  const t = new Date(browseTime.replace(/-/g, '/')).getTime()
  return Number.isNaN(t) ? 0 : t
}

// 计算属性：一周内的数据
const recentRecords = computed(() => {
  const oneWeekAgo = Date.now() - 7 * 24 * 3600 * 1000
  return records.value
    .filter(item => viewTime(item.browseTime) >= oneWeekAgo)
    .sort((a, b) => viewTime(b.browseTime) - viewTime(a.browseTime))
})

// 计算属性：一周外的数据
const earlierRecords = computed(() => {
  const oneWeekAgo = Date.now() - 7 * 24 * 3600 * 1000
  return records.value
    .filter(item => viewTime(item.browseTime) < oneWeekAgo)
    .sort((a, b) => viewTime(b.browseTime) - viewTime(a.browseTime))
})

// 时间格式化：输出 2026/7/16
const formatTime = (browseTime?: string) => {
  if (!browseTime) return '-'
  const d = new Date(browseTime.replace(/-/g, '/'))
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`
}

// 点击卡片反馈
const handleCardClick = (caseId: string) => {
  uni.navigateTo({ url: `/pages/case/detail?id=${encodeURIComponent(caseId)}` })
}
</script>

<style scoped lang="scss">
/* ================= 页面整体布局 ================= */
.history-container {
  height: 100vh;
  background-color: #F5F7FA;
  display: flex;
  flex-direction: column;
  position: relative;
}

.header-safe-area {
  padding-top: var(--status-bar-height, 44px);
  background-color: #ffffff;
}

.main-scroll {
  flex: 1;
  height: 0;
  
  .content-wrapper {
    padding: 30rpx;
    padding-bottom: 140rpx; /* 留出底部导航栏高度 */
  }
}

/* ================= 记录卡片外层与分组 ================= */
.records-card {
  background-color: #ffffff;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  overflow: hidden;
}

.group-header {
  padding: 24rpx 0;
  text-align: center;
  border-top: 1rpx solid #EBF4FF;
  border-bottom: 1rpx solid #EBF4FF;
  background-color: #ffffff;
  position: sticky;
  top: 0;
  z-index: 10;
  
  &.first-header {
    border-top: none;
  }
  
  .header-text {
    font-size: 28rpx;
    color: #218CFF;
    font-weight: 500;
  }
}

/* ================= 列表单项通用样式 ================= */
.case-item {
  padding: 30rpx;
  border-bottom: 1rpx solid #F0F2F5;
  background-color: #ffffff;
  
  &:active {
    background-color: #FAFAFA;
  }
  
  &.no-border {
    border-bottom: none;
  }
}

.country-tag {
  display: inline-flex;
  align-items: center;
  background-color: #EBF4FF;
  color: #218CFF;
  padding: 6rpx 16rpx;
  border-radius: 8rpx;
  font-size: 24rpx;
  margin-bottom: 16rpx;
  
  .country-icon { width: 24rpx; height: 24rpx; margin-right: 8rpx; }
}

.title {
  font-size: 30rpx;
  font-weight: 600;
  color: #333333;
  margin-bottom: 8rpx;
  display: block;
}

.en-title {
  font-size: 24rpx;
  color: #999999;
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.nav-arrow {
  width: 32rpx;
  height: 32rpx;
  opacity: 0.4;
}

/* ================= 一周内样式 ================= */
.recent-item {
  .card-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .ai-status {
      display: flex;
      align-items: center;
      background-color: #F0F9EB;
      padding: 4rpx 12rpx;
      border-radius: 8rpx;
      
      .check-icon { width: 20rpx; height: 20rpx; margin-right: 6rpx; }
      .status-text { font-size: 20rpx; color: #67C23A; }
    }
  }

  .card-body {
    margin-bottom: 20rpx;
  }

  .card-footer {
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .time-box {
      display: flex;
      align-items: center;
      
      .time-icon { width: 24rpx; height: 24rpx; margin-right: 8rpx; }
      .time-text { font-size: 24rpx; color: #999999; }
    }
  }
}

/* ================= 一周外样式 ================= */
.earlier-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  .earlier-left {
    flex: 1;
    padding-right: 20rpx;
    /* 防止超长英文撑爆布局 */
    min-width: 0; 
  }
  
  .earlier-right {
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    justify-content: space-between;
    height: 100rpx;
    
    .star-icon {
      width: 36rpx;
      height: 36rpx;
      opacity: 0.5;
    }
  }
}

/* ================= 空状态 ================= */
.empty-state {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding-bottom: 200rpx;
  
  .empty-icon {
    width: 200rpx;
    height: 200rpx;
    margin-bottom: 20rpx;
    opacity: 0.5;
  }
  
  .empty-text {
    font-size: 28rpx;
    color: #999999;
  }
}

.load-more-text {
  text-align: center;
  padding: 30rpx 0;
  color: #999999;
  font-size: 24rpx;
}
</style>
