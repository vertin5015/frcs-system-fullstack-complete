<script setup lang="ts">
import { onLoad } from '@dcloudio/uni-app'
import { ref } from 'vue'
import SearchBar from '../../components/SearchBar.vue'
import FilterBar from '../../components/FilterBar.vue'
import CaseCard from '../../components/CaseCard.vue'
import Empty from '../../components/Empty.vue'
import { caseApi } from '../../api/request'
import type { CaseInfo } from '../../types/case'
const cases = ref<CaseInfo[]>([])
async function search(keyword = '') { cases.value = await caseApi.list({ keyword }) }
onLoad((query) => { search(typeof query?.keyword === 'string' ? query.keyword : '') })
</script>

<template><view class="page"><SearchBar @search="search" /><FilterBar @change="search()" /><CaseCard v-for="item in cases" :key="item.id" :case-info="item" /><Empty v-if="!cases.length" /></view></template>
<style scoped lang="scss">.page { padding: 24rpx; }</style>
