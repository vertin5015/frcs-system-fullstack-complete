<!--
  src/pages/case/original.vue
  原始判决文书页：通过后端 /cases/original-proxy 代理展示。
  说明：
  - 微信 web-view 会覆盖整页，普通按钮无法点击，操作栏必须用 cover-view；
  - web-view 的 src 不支持动态修改，“刷新原文”通过重建页面 + retry=true 实现；
  - 微信小程序无法直接唤起外部浏览器，“外部打开”复制链接并提示。
-->
<template>
  <view class="original-page">
    <!-- 有效链接：web-view 全屏 + cover-view 操作栏 -->
    <web-view v-if="webviewSrc" :src="webviewSrc" />
    <cover-view v-if="webviewSrc" class="action-bar">
      <cover-view class="action-btn" @tap="reload">刷新原文</cover-view>
      <cover-view class="action-btn primary" @tap="copyLink">复制链接</cover-view>
      <cover-view class="action-btn primary" @tap="openExternal">外部打开</cover-view>
    </cover-view>

    <!-- 无效/缺失链接：普通页面提示 -->
    <view v-else class="fallback">
      <text class="fallback-icon">🔗</text>
      <text class="fallback-title">无法打开原文</text>
      <text class="fallback-desc">{{ errorText }}</text>
      <button class="fallback-btn" @tap="copyLink">复制原文链接</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { API_BASE_URL } from '../../api/config'
import { useOriginalStore } from '../../store/original'

const originalStore = useOriginalStore()
const rawUrl = ref('')
const webviewSrc = ref('')
const errorText = ref('')

const safeDecode = (v?: string) => {
  if (!v) return ''
  try {
    return decodeURIComponent(v)
  } catch {
    return v
  }
}

const buildProxySrc = (url: string, retry: boolean) =>
  `${API_BASE_URL}/cases/original-proxy?url=${encodeURIComponent(url)}${retry ? '&retry=true' : ''}`

const initPage = (url: string, retry: boolean) => {
  rawUrl.value = url
  webviewSrc.value = ''
  errorText.value = ''

  if (!url) {
    errorText.value = '未找到原文链接，该案例可能没有原始文书地址。'
    return
  }
  if (!/^https?:\/\//i.test(url)) {
    errorText.value = '原文地址格式无效，仅支持 http/https 链接。'
    return
  }
  webviewSrc.value = buildProxySrc(url, retry)
}

onLoad((query) => {
  // 优先取共享 store（由案例详情页写入，避免 query 编解码问题）；query 作为直达兜底
  const storeUrl = originalStore.url
  const storeRetry = originalStore.retry
  originalStore.clear()

  const queryUrl = safeDecode(String(query?.url || ''))
  const queryRetry = query?.retry === '1' || query?.retry === 'true'

  initPage(storeUrl || queryUrl, storeRetry || queryRetry)
})

/** 刷新原文：web-view src 不支持动态修改，重建页面并携带 retry=true */
const reload = () => {
  if (!rawUrl.value) return
  originalStore.open(rawUrl.value, true)
  uni.redirectTo({ url: '/pages/case/original' })
}

const copyLink = () => {
  if (!rawUrl.value) {
    uni.showToast({ title: '暂无可复制的链接', icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: rawUrl.value,
    success: () => uni.showToast({ title: '原文链接已复制', icon: 'success' }),
  })
}

/** 微信无法直接唤起外部浏览器：复制链接并给出操作提示 */
const openExternal = () => {
  if (!rawUrl.value) {
    uni.showToast({ title: '暂无可打开的链接', icon: 'none' })
    return
  }
  uni.setClipboardData({
    data: rawUrl.value,
    success: () => {
      uni.showModal({
        title: '请用浏览器打开',
        content: '微信小程序无法直接唤起外部浏览器，原文链接已复制，请粘贴到浏览器访问。',
        showCancel: false,
        confirmText: '好的',
        confirmColor: '#218CFF',
      })
    },
  })
}
</script>

<style scoped lang="scss">
.original-page {
  height: 100vh;
  width: 100%;
  background: #f5f7fa;
}

/* web-view 默认铺满导航栏以下区域，操作栏用 cover-view 悬浮在底部 */
.action-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 999;
  height: 110rpx;
  background: #ffffff;
  border-top: 1rpx solid #e4e7ed;
  display: flex;
  align-items: center;
  justify-content: space-around;
  box-shadow: 0 -4rpx 12rpx rgba(0, 0, 0, 0.06);

  .action-btn {
    display: flex;
    align-items: center;
    justify-content: center;
    width: 210rpx;
    height: 72rpx;
    border-radius: 36rpx;
    font-size: 26rpx;
    color: #218cff;
    border: 2rpx solid #218cff;
    background: #ffffff;
    line-height: 72rpx;
    text-align: center;
    box-sizing: border-box;

    &.primary {
      background: #218cff;
      color: #ffffff;
      border: none;
    }

    &:active {
      opacity: 0.8;
    }
  }
}

/* 无有效链接时的兜底页面（此时没有 web-view，用普通视图即可） */
.fallback {
  height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 0 60rpx;
  text-align: center;

  .fallback-icon {
    font-size: 96rpx;
    line-height: 1;
    margin-bottom: 32rpx;
    opacity: 0.7;
  }

  .fallback-title {
    font-size: 34rpx;
    font-weight: 600;
    color: #333333;
    margin-bottom: 16rpx;
  }

  .fallback-desc {
    font-size: 26rpx;
    color: #999999;
    line-height: 1.6;
    margin-bottom: 48rpx;
  }

  .fallback-btn {
    width: 320rpx;
    height: 80rpx;
    line-height: 80rpx;
    padding: 0;
    margin: 0;
    background: #218cff;
    color: #ffffff;
    font-size: 28rpx;
    border-radius: 40rpx;
    &::after {
      border: none;
    }
  }
}
</style>
