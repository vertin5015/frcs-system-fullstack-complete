<template>
  <view class="bottom-tab-bar">
    <view class="tab-items">
      <view 
        v-for="item in navList" 
        :key="item.id" 
        class="tab-item"
        @tap="handleSwitchTab(item)"
      >
        <image 
          class="tab-icon" 
          :src="activeTab === item.id ? item.activeIcon : item.icon" 
          mode="aspectFit" 
        />
        <text class="tab-text" :class="{ 'active-text': activeTab === item.id }">
          {{ item.name }}
        </text>
      </view>
    </view>
    <view class="safe-area-bottom"></view>
  </view>
</template>

<script setup>
import { defineProps, ref } from 'vue'

const props = defineProps({
  activeTab: {
    type: String,
    default: 'home'
  }
})

const navList = ref([
  { id: 'home', name: '首页', icon: '/static/icons/tab-home.png', activeIcon: '/static/icons/tab-home-active.png', path: '/pages/home/index' },
  { id: 'search', name: '检索', icon: '/static/icons/tab-search.png', activeIcon: '/static/icons/tab-search-active.png', path: '/pages/case/list' },
  { id: 'study', name: '学法', icon: '/static/icons/tab-book.png', activeIcon: '/static/icons/tab-book-active.png', path: '/pages/study/index' },
  { id: 'favorite', name: '收藏', icon: '/static/icons/tab-star.png', activeIcon: '/static/icons/tab-star-active.png', path: '/pages/favorite/index' },
  { id: 'history', name: '记录', icon: '/static/icons/tab-history.png', activeIcon: '/static/icons/tab-history-active.png', path: '/pages/history/index' },
  { id: 'user', name: '个人', icon: '/static/icons/tab-user.png', activeIcon: '/static/icons/tab-user-active.png', path: '/pages/user/index' }
])

const handleSwitchTab = (item) => {
  if (props.activeTab === item.id) return

  const targetPath = item.path.startsWith('/') ? item.path : '/' + item.path

  // 尝试使用 switchTab，如果失败（例如第6个Tab未在pages.json中注册为原生tabBar），则降级使用 reLaunch
  uni.switchTab({
    url: targetPath,
    fail: () => {
      uni.reLaunch({ url: targetPath })
    }
  })
}
</script>

<style lang="scss" scoped>
.bottom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background-color: #ffffff;
  border-top: 1rpx solid #E4E7ED;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.03);
  z-index: 999;
}

.tab-items {
  display: flex;
  justify-content: space-around;
  align-items: center;
  height: 100rpx;
}

.tab-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  flex: 1;
  height: 100%;
}

.tab-icon {
  width: 44rpx;
  height: 44rpx;
  margin-bottom: 6rpx;
}

.tab-text {
  font-size: 20rpx;
  color: #999999;
}

.active-text {
  color: #218CFF;
}

.safe-area-bottom {
  height: env(safe-area-inset-bottom);
}
</style>