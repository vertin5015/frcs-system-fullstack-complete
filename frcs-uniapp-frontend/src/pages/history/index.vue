<script setup lang="ts">
import { onMounted, ref } from 'vue'
import CaseCard from '../../components/CaseCard.vue'
import Empty from '../../components/Empty.vue'
import { caseApi } from '../../api/request'
import type { CaseInfo } from '../../types/case'
const records = ref<CaseInfo[]>([])
onMounted(async () => { records.value = await caseApi.list() })
</script>
<template><view class="page"><view>用户浏览记录</view><CaseCard v-for="item in records" :key="item.id" :case-info="item" /><Empty v-if="!records.length" text="暂无浏览记录" /></view></template>
<style scoped lang="scss">.page { padding: 24rpx; }</style>
