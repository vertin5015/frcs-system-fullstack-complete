<!-- src/pages/case/detail.vue -->
<template>
  <view class="detail-container" v-if="caseInfo">
    <scroll-view scroll-y class="main-scroll">
      <!-- 详情白底卡片 -->
      <view class="detail-card">
        
        <!-- 1. 标题与基本信息 -->
        <view class="section">
          <view class="section-title">
            <view class="accent-line"></view>
            <text class="title-text">{{ caseInfo.title || '未知案件名称' }}</text>
          </view>
          
          <view class="meta-grid">
            <text class="meta-item">案件类型：{{ caseInfo.type || '未知' }}</text>
            <text class="meta-item">案号：{{ caseInfo.caseNumber || '未知' }}</text>
            <text class="meta-item">法院：{{ caseInfo.court || '未知' }}</text>
            <text class="meta-item">判决日期：{{ caseInfo.date || '未知' }}</text>
          </view>
        </view>

        <!-- 虚线分割 -->
        <view class="dashed-divider"></view>

        <!-- 2. 案件简介 -->
        <view class="section">
          <view class="section-title">
            <view class="accent-line"></view>
            <text class="title-text">案件简介</text>
          </view>
          <text class="content-text">{{ caseInfo.summary || '暂无简介...' }}</text>
        </view>

        <!-- 3. 法院观点 -->
        <view class="section">
          <view class="section-title">
            <view class="accent-line"></view>
            <text class="title-text">法院观点</text>
          </view>
          <text class="content-text">{{ caseInfo.courtOpinion || '暂无观点...' }}</text>
        </view>

        <!-- 4. 相关法律条文 -->
        <view class="section">
          <view class="section-title">
            <view class="accent-line"></view>
            <text class="title-text">相关法律条文</text>
          </view>
          <text class="content-text">
            { '暂无条文...' }}
          </text>
        </view>

      </view>
    </scroll-view>

    <!-- 底部固定的操作栏 -->
    <view class="bottom-action-bar">
      <button 
        class="action-btn favorite-btn" 
        :class="{ 'is-favorited': isFavorite }" 
        @tap="handleFavorite"
      >
        <image 
          class="btn-icon" 
          :src="isFavorite ? '/static/icons/tab-star-active.png' : '/static/icons/tab-star.png'" 
          mode="aspectFit" 
        />
        <text>{{ isFavorite ? '已收藏' : '收藏' }}</text>
      </button>
      
      <button class="action-btn original-btn" @tap="openOriginal">
        查看原文
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
// 统一使用 mockCase 导出的接口和类型，废弃其他地方引入的 CaseInfo
import { fetchCaseDetailMock } from '../../api/mockCase'
import type { CaseItem } from '../../api/mockCase'

// 统一泛型类型为 CaseItem
const caseInfo = ref<CaseItem>()
// 本地维护收藏状态，用于即时 UI 响应
const isFavorite = ref(false)

onLoad(async (query) => {
  if (query?.id) {
    // 加上 mask: true 防止加载时用户点击屏幕
    uni.showLoading({ title: '加载中...', mask: true }) 
    
    try {
      const res = await fetchCaseDetailMock(Number(query.id))
      
      // 直接赋值，此时 TS 类型严格匹配
      caseInfo.value = res
      // 同步判断 favorate 属性
      isFavorite.value = !!(res as any).favorate 
      
      // 成功时隐藏
      uni.hideLoading()
    } catch (e) {
      // 失败时：先隐藏 loading，再显示 toast[cite: 17]
      uni.hideLoading()
      setTimeout(() => {
        uni.showToast({ title: '加载失败，未找到该案例', icon: 'none' })
      }, 50) 
    }
  }
})

// 处理收藏点击
const handleFavorite = () => {
  isFavorite.value = !isFavorite.value
  
  if (caseInfo.value) {
    // 强制赋予 favorate 属性，若本地 mock 返回的是对象引用，这里的值修改会直接作用并保留到 mock 数据源中[cite: 17]
    (caseInfo.value as any).favorate = isFavorite.value
  }

  if (isFavorite.value) {
    uni.showToast({ title: '已收藏', icon: 'success' })
  } else {
    uni.showToast({ title: '已取消收藏', icon: 'none' })
  }
}

// 查看原文
const openOriginal = () => {
  uni.showToast({ title: '正在开发中', icon: 'none' })
}
</script>

<style scoped lang="scss">
.detail-container {
  height: 100vh;
  background-color: #F5F7FA;
  display: flex;
  flex-direction: column;
}

.main-scroll {
  flex: 1;
  height: 0;
  
  .detail-card {
    background-color: #ffffff;
    margin: 24rpx;
    border-radius: 16rpx;
    padding: 30rpx;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
    margin-bottom: 160rpx; /* 留出底部按钮的空间 */
  }
}

/* ================= 各个小节通用样式 ================= */
.section {
  margin-bottom: 40rpx;
  
  &:last-child {
    margin-bottom: 10rpx;
  }
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;
  
  .accent-line {
    width: 8rpx;
    height: 32rpx;
    background-color: #218CFF;
    border-radius: 4rpx;
    margin-right: 16rpx;
  }
  
  .title-text {
    font-size: 32rpx;
    font-weight: bold;
    color: #333333;
  }
}

/* ================= 顶部元数据网格 ================= */
.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx 10rpx;
  padding: 0 10rpx;
  
  .meta-item {
    font-size: 26rpx;
    color: #666666;
  }
}

/* 虚线分割 */
.dashed-divider {
  height: 1px;
  border-top: 2rpx dashed #E4E7ED;
  margin: 30rpx 0;
}

/* 正文内容 */
.content-text {
  font-size: 28rpx;
  color: #606266;
  line-height: 1.6;
  padding: 0 10rpx;
  display: block; /* 保证换行生效 */
}

/* ================= 底部操作栏 ================= */
.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  background-color: #ffffff;
  border-top: 1rpx solid #E4E7ED;
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 10rpx rgba(0, 0, 0, 0.03);
  z-index: 99;
  
  .action-btn {
    flex: 1;
    height: 80rpx;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 40rpx;
    font-size: 30rpx;
    font-weight: 500;
    
    &::after {
      border: none;
    }
  }
  
  .favorite-btn {
    background-color: #ffffff;
    color: #218CFF;
    border: 2rpx solid #218CFF;
    margin-right: 20rpx;
    
    .btn-icon {
      width: 36rpx;
      height: 36rpx;
      margin-right: 8rpx;
    }
    
    &:active { background-color: #F0F8FF; }
  }
  
  /* 已收藏状态的按钮样式变化 */
  .is-favorited {
    background-color: #F0F8FF;
  }

  .original-btn {
    background-color: #218CFF;
    color: #ffffff;
    margin-left: 20rpx;
    
    &:active { background-color: #1A73D9; }
  }
}
</style>