<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { lawApi } from '../../api/request'
import type { LawInfo } from '../../types/law'
const law = ref<LawInfo>()
onLoad(async (query) => { law.value = await lawApi.detail(Number(query?.id)) })
</script>
<template><view v-if="law" class="page"><view>{{ law.title }}</view><view>条文原文：{{ law.content }}</view><view>AI 解读：{{ law.interpretation }}</view><view>修订记录：{{ law.revisions?.join('、') }}</view><view>相关案例：{{ law.relatedCaseIds?.join('、') }}</view></view></template>
<style scoped lang="scss">.page { padding: 24rpx; } .page > view { margin: 20rpx 0; }</style>
