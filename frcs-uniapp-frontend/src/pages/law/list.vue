<template>
  <view class="law-list-page">
    <view class="search-box">
      <input
        v-model="keyword"
        placeholder="输入条文关键词…"
        placeholder-class="placeholder-style"
        confirm-type="search"
        @confirm="search()"
      />
      <button class="search-btn" @tap="search">搜索</button>
    </view>

    <scroll-view scroll-y class="main-scroll">
      <view class="content-wrapper">
        <view v-if="loading" class="loading-state">正在检索知识库…</view>
        <view v-else-if="hits.length === 0" class="empty-state">暂无匹配条文</view>
        <view
          v-else
          class="law-card"
          v-for="hit in hits"
          :key="hit.chunkId"
          @tap="goDetail(hit)"
        >
          <view class="law-title">{{ hit.title }}</view>
          <view class="law-meta">{{ hit.sourceId }} · 相似度 {{ ((hit.score || 0) * 100).toFixed(1) }}%</view>
          <view class="law-preview">{{ hit.preview || '暂无片段' }}</view>
        </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import api from '../../api'
import type { KbHit } from '../../types/case'

const keyword = ref('')
const hits = ref<KbHit[]>([])
const loading = ref(false)

onLoad((query) => {
  if (query?.keyword) {
    try {
      keyword.value = decodeURIComponent(String(query.keyword))
    } catch {
      keyword.value = String(query.keyword)
    }
    search()
  }
})

const search = async () => {
  const q = keyword.value.trim()
  if (!q) {
    uni.showToast({ title: '请输入关键词', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await api.kbQuery({ question: q, language: 'zh', topK: 20 })
    if (res.code !== 200) {
      uni.showToast({ title: res.message || '查询失败', icon: 'none' })
      return
    }
    hits.value = res.data?.hits || []
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '查询失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

const goDetail = (hit: KbHit) => {
  uni.navigateTo({
    url: `/pages/law/detail?sourceId=${encodeURIComponent(hit.sourceId || '')}&title=${encodeURIComponent(hit.title || '')}&preview=${encodeURIComponent(hit.preview || '')}&score=${hit.score ?? ''}`
  })
}
</script>

<style scoped lang="scss">
.law-list-page {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.search-box {
  display: flex;
  align-items: center;
  gap: 16rpx;
  padding: 20rpx 24rpx;
  background: #fff;
  border-bottom: 1rpx solid #e4e7ed;

  input {
    flex: 1;
    background: #f0f2f5;
    border-radius: 36rpx;
    height: 72rpx;
    padding: 0 28rpx;
    font-size: 28rpx;
  }
  .search-btn {
    margin: 0;
    width: 140rpx;
    height: 72rpx;
    line-height: 72rpx;
    background: #218cff;
    color: #fff;
    font-size: 28rpx;
    border-radius: 36rpx;
    &::after { border: none; }
  }
}

.main-scroll { flex: 1; height: 0; }
.content-wrapper { padding: 24rpx; }

.law-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 26rpx 24rpx;
  margin-bottom: 20rpx;
  .law-title { font-size: 30rpx; font-weight: 500; color: #333; margin-bottom: 10rpx; }
  .law-meta { font-size: 24rpx; color: #218cff; margin-bottom: 10rpx; }
  .law-preview {
    font-size: 24rpx;
    color: #909399;
    line-height: 1.5;
    display: -webkit-box;
    -webkit-box-orient: vertical;
    -webkit-line-clamp: 2;
    overflow: hidden;
  }
}

.loading-state, .empty-state {
  text-align: center;
  padding: 100rpx 0;
  color: #999;
  font-size: 28rpx;
}
</style>
