<template>
  <view class="history-container">
    
    <view class="header-safe-area"></view>

    <scroll-view scroll-y class="main-scroll" v-if="records.length > 0">
      <view class="content-wrapper">
        
        <view class="records-card">
          
          <template v-if="recentRecords.length > 0">
            <view class="group-header first-header">
              <text class="header-text">一周内</text>
            </view>
            
            <view 
              class="case-item recent-item" 
              v-for="item in recentRecords" 
              :key="item.id"
              @tap="handleCardClick"
            >
              <view class="card-header">
                <view class="country-tag">
                  <image class="country-icon" src="/static/icons/flag.png" mode="aspectFit" />
                  <text>{{ item.country }}</text>
                </view>
                <view class="ai-status" v-if="item.aiSummaryStatus === 'completed'">
                  <image class="check-icon" src="/static/icons/check-green.png" mode="aspectFit" />
                  <text class="status-text">AI摘要已生成</text>
                </view>
              </view>

              <view class="card-body">
                <text class="title">{{ item.title }}</text>
                <text class="en-title">{{ item.englishTitle }}</text>
              </view>

              <view class="card-footer">
                <view class="time-box">
                  <image class="time-icon" src="/static/icons/time.png" mode="aspectFit" />
                  <text class="time-text">{{ formatTime(item.viewTime) }}</text>
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
              :key="item.id"
              :class="{ 'no-border': index === earlierRecords.length - 1 }"
              @tap="handleCardClick"
            >
              <view class="earlier-left">
                <view class="country-tag">
                  <image class="country-icon" src="/static/icons/flag.png" mode="aspectFit" />
                  <text>{{ item.country }}</text>
                </view>
                <text class="title">{{ item.title }}</text>
                <text class="en-title">{{ item.englishTitle }}</text>
              </view>
              
              <view class="earlier-right">
                <image class="star-icon" src="/static/icons/tab-star.png" mode="aspectFit" />
                <image class="nav-arrow" src="/static/icons/arrow-right.png" mode="aspectFit" />
              </view>
            </view>
          </template>

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
import { ref, onMounted, computed } from 'vue'
import BottomTabBar from '../../components/BottomTabBar.vue'
// 从你的 mockCase 文件中引入数据和真实类型
import { mockCases } from '../../api/mockCase'
import type { CaseItem } from '../../api/mockCase'

// 定义页面专属的历史记录类型：在 CaseItem 基础上扩展前端 UI 渲染需要的 viewTime
interface HistoryRecord extends CaseItem {
  viewTime: number;
}

// 初始状态列表为空
const records = ref<HistoryRecord[]>([])

// 获取记录并处理
onMounted(() => {
  /* 因为你提到：“对于用户浏览时刻为离开其详情页才进行计算，暂时无需在此实现”。
    所以为了能展示并测试“一周内/一周外”的滑动 UI 效果，我们直接在这里使用 mockCases 
    手动拼装几条带有模拟 viewTime 的数据进行渲染测试。
    等后期详情页记录逻辑写好后，此处直接替换为读取 storage 或 API 即可。
  */
  const now = new Date('2026-07-16T16:47:00+09:00').getTime() 
  
  // 截取前6条 mock 数据，前3条作为一周内，后3条作为一周外
  if (mockCases && mockCases.length > 0) {
    records.value = mockCases.slice(0, 6).map((item, index) => ({
      ...item,
      // index < 3 算在一周内，其他算在 10 天前（一周外）
      viewTime: index < 3 ? now - index * 86400000 : now - 10 * 86400000
    })).sort((a, b) => b.viewTime - a.viewTime) // 按时间倒序
  }
})

// 计算属性：一周内的数据
const recentRecords = computed(() => {
  const oneWeekAgo = new Date('2026-07-16T16:47:00+09:00').getTime() - 7 * 24 * 3600 * 1000
  return records.value.filter(item => item.viewTime >= oneWeekAgo)
})

// 计算属性：一周外的数据
const earlierRecords = computed(() => {
  const oneWeekAgo = new Date('2026-07-16T16:47:00+09:00').getTime() - 7 * 24 * 3600 * 1000
  return records.value.filter(item => item.viewTime < oneWeekAgo)
})

// 时间格式化：输出 2026/7/16
const formatTime = (timestamp: number) => {
  const d = new Date(timestamp)
  return `${d.getFullYear()}/${d.getMonth() + 1}/${d.getDate()}`
}

// 点击卡片反馈
const handleCardClick = () => {
  uni.showToast({
    title: '正在开发中',
    icon: 'none',
    duration: 2000
  })
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
</style>