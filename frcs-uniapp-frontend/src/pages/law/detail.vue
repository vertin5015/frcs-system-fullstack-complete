<template>
  <view class="law-detail-page">
    <scroll-view scroll-y class="main-scroll">
      <view class="content-wrapper">
        <view v-if="law" class="detail-card">
          <view class="law-title">{{ law.title }}</view>
          <view class="law-meta">
            <text class="meta-item">来源：{{ law.sourceId || '-' }}</text>
            <text v-if="law.score != null" class="meta-item">相似度：{{ (law.score * 100).toFixed(1) }}%</text>
          </view>

          <view class="divider"></view>

          <view class="section-title">条文内容（检索片段）</view>
          <text class="content-text">{{ law.content }}</text>

          <view class="divider"></view>

          <view class="section-title">AI 解读</view>
          <button
            class="ai-btn"
            :loading="interpreting"
            @tap="interpret"
          >{{ interpretation ? '重新解读' : '生成 AI 解读' }}</button>
          <view v-if="interpreting" class="status-text">正在生成解读…</view>
          <view v-if="interpretation" class="interpretation-text">{{ interpretation }}</view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '../../api'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()

const law = ref<{ title: string; sourceId: string; content: string; score?: number }>()
const interpretation = ref('')
const interpreting = ref(false)

const safeDecode = (v?: string) => {
  if (!v) return ''
  try {
    return decodeURIComponent(v)
  } catch {
    return v
  }
}

onLoad((query) => {
  const title = query?.title ? safeDecode(String(query.title)) : ''
  const preview = query?.preview ? safeDecode(String(query.preview)) : ''
  const sourceId = query?.sourceId ? safeDecode(String(query.sourceId)) : ''
  const score = query?.score && query.score !== '' ? Number(query.score) : undefined
  law.value = {
    title: title || '法律条文',
    sourceId,
    content: preview || '（当前仅返回检索片段，详细全文可在知识库中检索）',
    score,
  }
})

const interpret = async () => {
  if (!law.value?.title) return
  if (userStore.isGuest) {
    uni.showToast({ title: '请登录后使用 AI 解读', icon: 'none' })
    return
  }
  interpreting.value = true
  try {
    const res = await api.agentAsk({
      userId: userStore.userId,
      question: `请解读《${law.value.title}》的核心内容与适用要点`,
      language: 'zh',
      refreshCases: false,
      topK: 5,
    })
    if (res.code !== 200) {
      uni.showToast({ title: res.message || '生成失败', icon: 'none' })
      return
    }
    interpretation.value = res.data?.answer || '（无回答）'
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '生成失败', icon: 'none' })
  } finally {
    interpreting.value = false
  }
}
</script>

<style scoped lang="scss">
.law-detail-page {
  height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

.main-scroll { flex: 1; height: 0; }
.content-wrapper { padding: 24rpx; }

.detail-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);

  .law-title { font-size: 36rpx; font-weight: 600; color: #333; line-height: 1.4; margin-bottom: 16rpx; }
  .law-meta { display: flex; flex-wrap: wrap; gap: 20rpx; margin-bottom: 10rpx; }
  .meta-item { font-size: 24rpx; color: #218cff; background: #ebf4ff; padding: 6rpx 16rpx; border-radius: 20rpx; }
  .divider { height: 1px; background: #f0f2f5; margin: 30rpx 0; }
  .section-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; }
  .content-text {
    font-size: 28rpx;
    color: #606266;
    line-height: 1.75;
    white-space: pre-wrap;
    word-break: break-word;
    display: block;
  }
  .ai-btn {
    width: 260rpx;
    height: 76rpx;
    line-height: 76rpx;
    background: #218cff;
    color: #fff;
    font-size: 28rpx;
    border-radius: 38rpx;
    margin: 0 0 20rpx 0;
    &::after { border: none; }
  }
  .status-text { font-size: 26rpx; color: #218cff; margin-bottom: 16rpx; }
  .interpretation-text {
    font-size: 28rpx;
    color: #222;
    line-height: 1.75;
    white-space: pre-wrap;
    background: #f7f9fc;
    padding: 24rpx;
    border-radius: 12rpx;
    border-left: 6rpx solid #218cff;
  }
}
</style>
