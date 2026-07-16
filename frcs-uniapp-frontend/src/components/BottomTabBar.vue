<template>
  <!-- 底部占位块，防止页面内容被绝对定位的导航栏遮挡 -->
  <view class="tab-bar-placeholder"></view>

  <!-- 真正的自定义底部导航栏 -->
  <view class="bottom-tab-bar">
    <view 
      v-for="item in tabList" 
      :key="item.id"
      class="tab-item"
      :class="{ active: activeTab === item.id }"
      @click="handleSwitchTab(item)"
    >
      <!-- 图标：假设你使用的是 iconfont -->
      <text class="iconfont" :class="[item.icon, activeTab === item.id ? 'active-icon' : '']"></text>
      <!-- 文字 -->
      <text class="tab-text">{{ item.text }}</text>
    </view>
  </view>
</template>

<script setup>
import { defineProps } from 'vue'

// 接收父组件传入的当前高亮 tab 的 id
const props = defineProps({
  activeTab: {
    type: String,
    required: true
  }
})

// 底部导航栏配置数据（已移除“浏览历史”，保留核心的 5 个）
// 注意：这里的 path 必须与 pages.json 中 tabBar.list 里的 pagePath 完全对应
const tabList = [
  { id: 'home', text: '首页', icon: 'icon-home', path: '/pages/home/index' },
  { id: 'search', text: '检索', icon: 'icon-search', path: '/pages/case/list' },
  { id: 'study', text: '学法', icon: 'icon-study', path: '/pages/study/index' },
  { id: 'favorite', text: '收藏', icon: 'icon-star', path: '/pages/favorite/index' },
  { id: 'user', text: '我的', icon: 'icon-user', path: '/pages/user/index' }
]

// 处理 Tab 切换逻辑
const handleSwitchTab = (item) => {
  // 如果点击的是当前已经激活的 tab，则不执行任何操作，避免重复触发
  if (props.activeTab === item.id) return

  // 确保跳转路径以 '/' 开头
  const targetPath = item.path.startsWith('/') ? item.path : '/' + item.path
  
  // 使用 switchTab 触发原生底层切换，实现无缝保活、告别白屏刷新
  uni.switchTab({
    url: targetPath,
    fail: (err) => {
      console.error('切换 Tab 失败，请检查 pages.json 中是否正确配置了 tabBar 字段', err)
    }
  })
}
</script>

<style scoped>
/* 底部占位块：高度 = 导航栏内容高度 + 底部安全距离 */
.tab-bar-placeholder {
  height: calc(100rpx + env(safe-area-inset-bottom));
  width: 100%;
  background-color: transparent;
}

/* 底部导航栏主容器 */
.bottom-tab-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  width: 100%;
  height: calc(100rpx + env(safe-area-inset-bottom));
  background-color: #ffffff;
  display: flex;
  justify-content: space-around;
  align-items: center;
  box-shadow: 0 -2rpx 10rpx rgba(0, 0, 0, 0.05);
  padding-bottom: env(safe-area-inset-bottom); /* 适配全面屏底部安全区 */
  z-index: 999;
}

/* 单个 Tab 按钮 */
.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100rpx;
  color: #999999; /* 默认文字颜色 */
  transition: all 0.2s ease; /* 添加轻微的过渡动画，让交互更现代 */
}

/* 图标样式设定 */
.iconfont {
  font-size: 40rpx;
  margin-bottom: 6rpx;
}

/* 文字样式设定 */
.tab-text {
  font-size: 24rpx;
}

/* 激活状态样式 */
.tab-item.active .tab-text,
.tab-item.active .active-icon {
  color: #007AFF; /* 激活时的主题色，可根据你的项目 UI 调整 */
  font-weight: bold;
}
</style>