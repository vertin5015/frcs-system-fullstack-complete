<script setup lang="ts">
import type { LawInfo } from '../types/law'

defineProps<{ lawInfo: LawInfo }>()

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/law/detail?id=${id}` })
}
</script>

<template>
  <view class="law-card" @click="openDetail(lawInfo.id)">
    <view>{{ lawInfo.title }}</view>
    <view>{{ lawInfo.country }} · {{ lawInfo.category }}</view>
    <view>更新时间：{{ lawInfo.updatedAt || '-' }}</view>
  </view>
</template>

<style scoped lang="scss">
.law-card { padding: 24rpx; margin: 16rpx 0; background: #fff; }
</style>
