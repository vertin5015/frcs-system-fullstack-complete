<script setup lang="ts">
import { onMounted, ref } from 'vue'
import CaseCard from '../../components/CaseCard.vue'
import Empty from '../../components/Empty.vue'
import { caseApi } from '../../api/request'
import type { CaseInfo } from '../../types/case'
const favorites = ref<CaseInfo[]>([])
onMounted(async () => { favorites.value = (await caseApi.list()).slice(0, 1) })
</script>
<template><view class="page"><view>收藏案例</view><CaseCard v-for="item in favorites" :key="item.id" :case-info="item" /><Empty v-if="!favorites.length" text="暂无收藏案例" /></view></template>
<style scoped lang="scss">.page { padding: 24rpx; }</style>
