<template>
  <view class="study-container">
    <view class="top-fixed-section">
      <view class="page-title-row">
        <text class="page-title">海外法律知识</text>
        <text class="page-sub">基于本地知识库 RAG 检索</text>
      </view>

      <view class="search-box">
        <input
          class="search-input"
          v-model="question"
          placeholder="输入法律问题，例如：GDPR 跨境数据传输要求…"
          placeholder-class="placeholder-style"
          confirm-type="search"
          @confirm="ask()"
        />
        <view class="search-btn-circle" @tap="ask">
          <text class="search-icon">问</text>
        </view>
      </view>

      <scroll-view scroll-x class="hot-scroll">
        <view class="hot-tags">
          <view
            class="hot-tag"
            v-for="(tag, index) in hotQuestions"
            :key="index"
            @tap="clickHot(tag)"
          >
            {{ tag }}
          </view>
        </view>
      </scroll-view>
    </view>

    <scroll-view scroll-y class="main-scroll">
      <view class="content-wrapper">
        <view v-if="loading" class="loading-state">
          <text>正在检索知识库…</text>
        </view>

        <view v-else-if="answer" class="answer-card">
          <view class="answer-title">AI 解答</view>
          <text class="answer-text">{{ answer }}</text>
        </view>

        <view v-if="hits.length > 0" class="hits-section">
          <view class="section-title">
            <text>相关条文（{{ hits.length }}）</text>
          </view>
          <view
            class="law-card"
            v-for="hit in hits"
            :key="hit.chunkId"
            @tap="goLawDetail(hit)"
          >
            <view class="card-left">
              <image class="law-icon" src="/static/icons/tab-book.png" mode="aspectFit" />
              <view class="law-info">
                <text class="law-title">{{ hit.title }}</text>
                <view class="law-tags">
                  <text class="tag source-tag">{{ hit.sourceId }}</text>
                  <text class="tag score-tag">相似度 {{ ((hit.score || 0) * 100).toFixed(1) }}%</text>
                </view>
                <text class="law-preview">{{ hit.preview || '暂无片段' }}</text>
              </view>
            </view>
            <image class="arrow-icon" src="/static/icons/arrow-right.png" mode="aspectFit" />
          </view>
        </view>

        <view v-if="!loading && !answer && hits.length === 0" class="empty-state">
          <text>输入问题或点击上方热门问题开始查询</text>
        </view>
      </view>
    </scroll-view>

    <BottomTabBar activeTab="study" />
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import BottomTabBar from '@/components/BottomTabBar.vue'
import api from '../../api'
import type { KbHit } from '../../types/case'
import { onShow } from '@dcloudio/uni-app'

onShow(() => {
  uni.hideTabBar({ animation: false })
})

const question = ref('')
const answer = ref('')
const hits = ref<KbHit[]>([])
const loading = ref(false)

const hotQuestions = [
  'GDPR 跨境数据传输要求',
  '美国外观设计专利侵权赔偿',
  '欧盟反垄断滥用市场支配地位',
  '日本不正当竞争与商标保护',
]

const clickHot = (tag: string) => {
  question.value = tag
  ask()
}

const ask = async () => {
  const q = question.value.trim()
  if (!q) {
    uni.showToast({ title: '请输入问题', icon: 'none' })
    return
  }
  loading.value = true
  answer.value = ''
  hits.value = []
  try {
    const res = await api.kbQuery({ question: q, language: 'zh', topK: 15 })
    if (res.code !== 200) {
      uni.showToast({ title: res.message || '查询失败', icon: 'none' })
      return
    }
    answer.value = res.data?.answer || ''
    hits.value = res.data?.hits || []
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '查询失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goLawDetail = (hit: KbHit) => {
  uni.navigateTo({
    url: `/pages/law/detail?sourceId=${encodeURIComponent(hit.sourceId || '')}&title=${encodeURIComponent(hit.title || '')}&preview=${encodeURIComponent(hit.preview || '')}&score=${hit.score ?? ''}`
  })
}
</script>

<style lang="scss" scoped>
.study-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f5f7fa;
  padding-bottom: env(safe-area-inset-bottom);
}

.top-fixed-section {
  background: #fff;
  padding: 24rpx;
  border-bottom: 1rpx solid #e4e7ed;
}

.page-title-row {
  display: flex;
  align-items: baseline;
  margin-bottom: 20rpx;
  .page-title { font-size: 36rpx; font-weight: 600; color: #333; margin-right: 16rpx; }
  .page-sub { font-size: 22rpx; color: #999; }
}

.search-box {
  display: flex;
  align-items: center;
  background: #f0f2f5;
  border-radius: 40rpx;
  height: 80rpx;
  padding: 0 10rpx 0 28rpx;
  margin-bottom: 20rpx;

  .search-input { flex: 1; font-size: 28rpx; color: #333; }
  .placeholder-style { color: #999; }
  .search-btn-circle {
    width: 60rpx; height: 60rpx; border-radius: 50%;
    background: #218cff; color: #fff;
    display: flex; align-items: center; justify-content: center;
    font-size: 26rpx; font-weight: 600;
  }
}

.hot-scroll { white-space: nowrap; }
.hot-tags { display: inline-flex; gap: 16rpx; padding: 4rpx 0; }
.hot-tag {
  background: #f5f7fa;
  color: #606266;
  font-size: 24rpx;
  padding: 10rpx 24rpx;
  border-radius: 28rpx;
  border: 1rpx solid #e4e7ed;
  &:active { background: #ebf4ff; color: #218cff; }
}

.main-scroll { flex: 1; height: 0; }
.content-wrapper { padding: 24rpx; padding-bottom: 140rpx; }

.loading-state {
  text-align: center;
  padding: 80rpx 0;
  color: #218cff;
  font-size: 28rpx;
}

.answer-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 30rpx;
  margin-bottom: 30rpx;
  border-left: 6rpx solid #218cff;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);

  .answer-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 16rpx; }
  .answer-text { font-size: 28rpx; color: #444; line-height: 1.75; white-space: pre-wrap; }
}

.hits-section {
  .section-title { font-size: 30rpx; font-weight: 600; color: #333; margin-bottom: 20rpx; }
}

.law-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 26rpx 24rpx;
  margin-bottom: 20rpx;
  display: flex;
  justify-content: space-between;
  align-items: center;
  box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.03);

  .card-left { display: flex; align-items: flex-start; flex: 1; padding-right: 20rpx; }
  .law-icon { width: 40rpx; height: 40rpx; margin-right: 20rpx; margin-top: 4rpx; opacity: 0.8; }
  .law-info { flex: 1; display: flex; flex-direction: column; }
  .law-title { font-size: 30rpx; color: #333; font-weight: 500; line-height: 1.4; margin-bottom: 12rpx; }
  .law-tags { display: flex; gap: 12rpx; margin-bottom: 12rpx; }
  .tag { font-size: 22rpx; padding: 4rpx 12rpx; border-radius: 6rpx; }
  .source-tag { background: #ebf4ff; color: #218cff; }
  .score-tag { background: #f0f2f5; color: #606266; }
  .law-preview {
    font-size: 24rpx;
    color: #909399;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }
  .arrow-icon { width: 32rpx; height: 32rpx; opacity: 0.4; }
}

.empty-state {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 26rpx;
}
</style>
