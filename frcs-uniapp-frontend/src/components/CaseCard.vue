<script setup lang="ts">
import type { CaseInfo } from '../types/case'

defineProps<{ caseInfo: CaseInfo }>()

function openDetail(id: number) {
  uni.navigateTo({ url: `/pages/case/detail?id=${id}` })
}
</script>

<template>
  <view class="case-card" @click="openDetail(caseInfo.id)">
    <view>{{ caseInfo.country }} · {{ caseInfo.title }}</view>
    <view>{{ caseInfo.englishTitle }}</view>
    <view>{{ caseInfo.court }} · {{ caseInfo.date }}</view>
    <view>AI 摘要：{{ caseInfo.aiSummaryStatus === 'completed' ? '已生成' : '待生成' }}</view>
  </view>
</template>

<style scoped lang="scss">
.case-card { padding: 24rpx; margin: 16rpx 0; background: #fff; }
</style>
