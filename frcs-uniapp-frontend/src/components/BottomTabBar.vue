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
      <!-- 图标：使用 Emoji 文本，不依赖本地图片资源 -->
      <text class="tab-icon">{{ item.icon }}</text>
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
  { id: 'home', text: '首页', icon: '🏠', path: '/pages/home/index' },
  { id: 'search', text: '检索', icon: '🔍', path: '/pages/case/list' },
  { id: 'study', text: '学法', icon: '📖', path: '/pages/study/index' },
  { id: 'favorite', text: '收藏', icon: '⭐', path: '/pages/favorite/index' },
  { id: 'user', text: '我的', icon: '👤', path: '/pages/user/index' }
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
.tab-bar-placeholder { height:calc(100rpx + env(safe-area-inset-bottom)); width:100%; background-color:transparent; }
.bottom-tab-bar { position:fixed; bottom:0; left:0; width:100%; height:calc(100rpx + env(safe-area-inset-bottom)); background-color:#ffffff; display:flex; justify-content:space-around; align-items:center; box-shadow:0 -2rpx 10rpx rgba(0,0,0,.05); padding-bottom:env(safe-area-inset-bottom); z-index:999; }
.tab-item { flex:1; display:flex; flex-direction:column; justify-content:center; align-items:center; height:100rpx; color:#999999; transition:all .2s ease; }
.tab-icon { font-size:42rpx; line-height:1; margin-bottom:6rpx; }
.tab-text { font-size:24rpx; }
.tab-item.active .tab-text { color:#218CFF; font-weight:bold; }
.tab-item.active { background:linear-gradient(180deg, rgba(33,140,255,.08) 0%, rgba(33,140,255,0) 100%); }
</style>
