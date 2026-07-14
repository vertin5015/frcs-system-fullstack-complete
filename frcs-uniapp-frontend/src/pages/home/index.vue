<script setup lang="ts">
import { onMounted, ref } from 'vue'
import SearchBar from '../../components/SearchBar.vue'
import CaseCard from '../../components/CaseCard.vue'
import BottomTabBar from '../../components/BottomTabBar.vue'
import { caseApi } from '../../api/request'
import type { CaseInfo } from '../../types/case'
const cases = ref<CaseInfo[]>([])
onMounted(async () => { cases.value = await caseApi.list() })
function search(keyword: string) { uni.navigateTo({ url: `/pages/case/list?keyword=${encodeURIComponent(keyword)}` }) }
</script>

<template><view class="page"><view class="title">涉外案例查询与法律知识系统</view><SearchBar placeholder="搜索案例" @search="search" /><view>案例推荐</view><CaseCard v-for="item in cases" :key="item.id" :case-info="item" /><BottomTabBar /></view></template>
<style scoped lang="scss">.page { padding: 24rpx; } .title { font-size: 36rpx; font-weight: 600; }</style>
