<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import { caseApi } from '../../api/request'
import type { CaseInfo } from '../../types/case'
const caseInfo = ref<CaseInfo>()
onLoad(async (query) => { caseInfo.value = await caseApi.detail(Number(query?.id)) })
function favorite() { uni.showToast({ title: '已收藏', icon: 'success' }) }
function openOriginal() { uni.showToast({ title: '原文链接待接入', icon: 'none' }) }
</script>

<template><view v-if="caseInfo" class="page"><view>{{ caseInfo.title }}</view><view>类型：{{ caseInfo.type }}｜法院：{{ caseInfo.court }}</view><view>案号：{{ caseInfo.caseNumber }}｜日期：{{ caseInfo.date }}</view><view>案件简介：{{ caseInfo.summary }}</view><view>法院观点：{{ caseInfo.courtOpinion }}</view><view>法律条文：{{ caseInfo.legalProvisions?.join('、') }}</view><button @click="favorite">收藏</button><button @click="openOriginal">查看原文</button></view></template>
<style scoped lang="scss">.page { padding: 24rpx; } .page > view { margin: 20rpx 0; }</style>
