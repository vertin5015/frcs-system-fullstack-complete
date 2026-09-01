<template>
  <view class="recharge-container">
    <scroll-view scroll-y class="main-scroll">
      <view class="content-wrapper">
        <view class="header-card">
          <view class="header-title">购买 AI 摘要次数</view>
          <view class="header-sub">
            当前剩余：
            <text class="credits-num">{{ creditsDisplay }}</text>
            次
          </view>
          <view class="channel-hint">{{ channelHint }}</view>
        </view>

        <view class="pkg-grid">
          <view class="pkg-card" v-for="p in packages" :key="p.id" @tap="buy(p)">
            <view class="pkg-title">{{ p.label }}</view>
            <view class="pkg-credits">{{ p.credits }} 次</view>
            <view class="pkg-price">{{ formatPrice(p) }}</view>
            <button class="pkg-btn" :loading="orderingId === p.id">购买</button>
          </view>
        </view>

        <view v-if="packages.length === 0" class="empty-state">
          <text>套餐加载中…</text>
        </view>
      </view>
    </scroll-view>

    <!-- 模拟支付确认弹窗 -->
    <view class="modal-overlay" v-if="mockDialogVisible">
      <view class="modal-card">
        <view class="modal-header">
          <text class="modal-title">模拟支付</text>
          <view class="close-btn" @tap="mockDialogVisible = false">
            <text class="close-icon">×</text>
          </view>
        </view>
        <view class="modal-body">
          <view class="order-no">订单号：<text class="order-code">{{ pendingOrderNo }}</text></view>
          <view class="mock-tip">开发/测试环境点击确认即可完成支付并到账。</view>
        </view>
        <view class="modal-footer">
          <button class="cancel-btn" @tap="mockDialogVisible = false">取消</button>
          <button class="confirm-btn" :loading="mockConfirming" @tap="confirmMock">确认支付</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '../../api'
import { useUserStore } from '../../store/user'
import type { PaymentPackage } from '../../types/user'

const userStore = useUserStore()
const packages = ref<PaymentPackage[]>([])
const orderingId = ref('')
const mockDialogVisible = ref(false)
const pendingOrderNo = ref('')
const mockConfirming = ref(false)
const channelHint = ref('加载中…')
const summaryCredits = ref<number | null>(userStore.getCredits())

const creditsDisplay = computed(() => {
  if (userStore.isGuest) return '—（游客）'
  if (summaryCredits.value != null) return String(summaryCredits.value)
  return '—'
})

const formatPrice = (p: PaymentPackage) => {
  const c = (p.currency || '').toUpperCase()
  if (c === 'CNY') return `¥${(p.priceCents / 100).toFixed(2)}`
  return `${(p.priceCents / 100).toFixed(2)} ${c}`
}

const loadCredits = async () => {
  if (userStore.isGuest) return
  try {
    const res = await api.getUserSummaryCredits(userStore.userId)
    if (res.code === 200 && res.data != null) {
      summaryCredits.value = res.data
      userStore.updateCredits(res.data)
    }
  } catch {
    /* ignore */
  }
}

const loadPackages = async () => {
  try {
    const res = await api.getPaymentPackages('zh')
    if (res.code === 200 && res.data) {
      packages.value = res.data
    } else {
      uni.showToast({ title: res.message || '加载套餐失败', icon: 'none' })
    }
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '加载套餐失败', icon: 'none' })
  }
}

const loadChannels = async () => {
  try {
    const res = await api.getPaymentChannels()
    if (res.code === 200 && res.data) {
      const d = res.data as Record<string, unknown>
      const parts: string[] = []
      if (d.mock) parts.push('模拟支付')
      if (d.stripe) parts.push('Stripe')
      if (d.alipay) parts.push('支付宝')
      if (d.wechatPay) parts.push('微信')
      channelHint.value = `支付方式：${parts.join(' / ') || '—'}${d.noteZh ? ` ${d.noteZh}` : ''}`
    }
  } catch {
    channelHint.value = '支付方式加载失败'
  }
}

const buy = async (p: PaymentPackage) => {
  if (userStore.isGuest) {
    uni.showToast({ title: '请先登录', icon: 'none' })
    return
  }
  orderingId.value = p.id
  try {
    const res = await api.createPaymentOrder(userStore.userId, p.id)
    if (res.code !== 200) {
      uni.showToast({ title: res.message || '下单失败', icon: 'none' })
      return
    }
    const d = res.data || {}
    if (d.stripeCheckoutUrl) {
      // 小程序端无法直接跳转外部 Stripe，复制链接提示
      uni.setClipboardData({
        data: d.stripeCheckoutUrl,
        success: () => uni.showToast({ title: 'Stripe 支付链接已复制', icon: 'none' }),
      })
      return
    }
    if (d.mockPay && d.orderNo) {
      pendingOrderNo.value = d.orderNo
      mockDialogVisible.value = true
    }
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '下单失败', icon: 'none' })
  } finally {
    orderingId.value = ''
  }
}

const confirmMock = async () => {
  mockConfirming.value = true
  try {
    const res = await api.confirmMockPayment(userStore.userId, pendingOrderNo.value)
    if (res.code !== 200) {
      uni.showToast({ title: res.message || '确认失败', icon: 'none' })
      return
    }
    uni.showToast({ title: '支付成功，次数已到账', icon: 'success' })
    mockDialogVisible.value = false
    await loadCredits()
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '确认失败', icon: 'none' })
  } finally {
    mockConfirming.value = false
  }
}

onLoad(async () => {
  await loadChannels()
  await loadPackages()
  await loadCredits()
})
</script>

<style scoped lang="scss">
.recharge-container {
  height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.main-scroll { flex: 1; height: 0; }

.content-wrapper {
  padding: 24rpx;
  padding-bottom: 60rpx;
}

.header-card {
  background: linear-gradient(135deg, #218cff 0%, #5cb3ff 100%);
  border-radius: 24rpx;
  padding: 40rpx;
  color: #fff;
  margin-bottom: 30rpx;

  .header-title { font-size: 36rpx; font-weight: 600; margin-bottom: 16rpx; }
  .header-sub { font-size: 28rpx; opacity: 0.95; }
  .credits-num { font-size: 40rpx; font-weight: 700; }
  .channel-hint { font-size: 22rpx; opacity: 0.8; margin-top: 12rpx; }
}

.pkg-grid {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
}

.pkg-card {
  width: calc(50% - 12rpx);
  background: #fff;
  border-radius: 20rpx;
  padding: 36rpx 24rpx;
  margin-bottom: 24rpx;
  text-align: center;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);

  .pkg-title { font-size: 28rpx; font-weight: 600; color: #333; margin-bottom: 12rpx; }
  .pkg-credits { font-size: 24rpx; color: #666; margin-bottom: 12rpx; }
  .pkg-price { font-size: 36rpx; font-weight: 700; color: #218cff; margin-bottom: 20rpx; }
  .pkg-btn {
    width: 100%;
    height: 72rpx;
    line-height: 72rpx;
    background: #218cff;
    color: #fff;
    font-size: 28rpx;
    border-radius: 36rpx;
    margin: 0;
    &::after { border: none; }
  }
}

.empty-state {
  text-align: center;
  padding: 80rpx 0;
  color: #999;
  font-size: 28rpx;
}

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-card {
  width: 80%;
  background: #fff;
  border-radius: 32rpx;
  padding: 40rpx;

  .modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 30rpx;
    .modal-title { font-size: 34rpx; font-weight: 600; color: #333; }
    .close-btn {
      width: 44rpx; height: 44rpx; border-radius: 50%;
      border: 3rpx solid #218cff;
      display: flex; align-items: center; justify-content: center;
      .close-icon { font-size: 34rpx; color: #218cff; line-height: 1; margin-top: -4rpx; }
    }
  }

  .modal-body {
    .order-no { font-size: 28rpx; color: #333; margin-bottom: 16rpx; }
    .order-code { color: #218cff; }
    .mock-tip { font-size: 24rpx; color: #999; line-height: 1.6; }
  }

  .modal-footer {
    display: flex;
    margin-top: 40rpx;

    .cancel-btn {
      margin-right: 24rpx;
    }

    button {
      flex: 1; height: 80rpx; line-height: 80rpx;
      border-radius: 40rpx; font-size: 28rpx;
      &::after { border: none; }
    }
    .cancel-btn { background: #f0f2f5; color: #666; }
    .confirm-btn { background: #218cff; color: #fff; }
  }
}
</style>
