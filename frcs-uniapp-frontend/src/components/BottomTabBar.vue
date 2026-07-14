<script setup lang="ts">
const tabs = [
  { text: '首页', url: '/pages/home/index' }, { text: '检索', url: '/pages/case/list' },
  { text: '学法', url: '/pages/study/index' }, { text: '收藏', url: '/pages/favorite/index' },
  { text: '记录', url: '/pages/history/index' }, { text: '个人', url: '/pages/user/index' },
]
function navigate(url: string) { uni.navigateTo({ url }) }
</script>

<template>
  <view class="bottom-tab-bar">
    <text v-for="tab in tabs" :key="tab.url" @click="navigate(tab.url)">{{ tab.text }}</text>
  </view>
</template>

<style scoped lang="scss">
.bottom-tab-bar { display: flex; justify-content: space-around; padding: 24rpx 8rpx; background: #fff; }
</style>
