<!-- src/pages/case/original.vue：通过后端 original-proxy 代理展示原始判决文书 -->
<template>
  <view class="original-container">
    <view v-if="webviewSrc" class="webview-wrap">
      <web-view :src="webviewSrc" />
    </view>

    <view v-else class="empty-tip">
      <text>缺少原文地址</text>
    </view>

    <view class="bottom-bar">
      <button class="action-btn" @tap="reload">刷新原文</button>
      <button class="action-btn primary" @tap="copyLink">复制原链接</button>
      <button class="action-btn primary" @tap="openExternal">外部打开</button>
    </view>

    <view class="notice">
      <text>提示：微信小程序 web-view 需要配置业务域名；开发工具中已关闭 url 校验，可直接预览。若原文站点响应较慢，后端会自动重试抓取。</text>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { API_BASE_URL } from '../../api/config'

const rawUrl = ref('')
const webviewSrc = ref('')

onLoad((query) => {
  if (query?.url) {
    rawUrl.value = String(query.url)
    webviewSrc.value = `${API_BASE_URL}/cases/original-proxy?url=${encodeURIComponent(rawUrl.value)}`
  }
})

const reload = () => {
  if (!rawUrl.value) return
  webviewSrc.value = `${API_BASE_URL}/cases/original-proxy?retry=true&url=${encodeURIComponent(rawUrl.value)}`
}

const copyLink = () => {
  if (!rawUrl.value) return
  uni.setClipboardData({ data: rawUrl.value })
}

const openExternal = () => {
  if (!rawUrl.value) return
  uni.setClipboardData({
    data: rawUrl.value,
    success: () => uni.showToast({ title: '原文链接已复制，请在浏览器打开', icon: 'none' }),
  })
}
</script>

<style scoped lang="scss">
.original-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.webview-wrap {
  flex: 1;
  height: 0;
}

.empty-tip {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #999;
}

.bottom-bar {
  display: flex;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #e4e7ed;

  .action-btn {
    flex: 1;
    height: 76rpx;
    line-height: 76rpx;
    margin: 0;
    border-radius: 38rpx;
    background: #fff;
    color: #218cff;
    border: 2rpx solid #218cff;
    font-size: 28rpx;
    &::after { border: none; }
    &.primary {
      background: #218cff;
      color: #fff;
      border: none;
    }
  }

  .action-btn + .action-btn {
    margin-left: 16rpx;
  }
}

.notice {
  padding: 16rpx 24rpx;
  color: #909399;
  font-size: 22rpx;
  line-height: 1.5;
  background: #fff;
}
</style>
