<script setup lang="ts">
import { onMounted, ref } from 'vue'
import LawCard from '../../components/LawCard.vue'
import { lawApi } from '../../api/request'
import type { LawInfo } from '../../types/law'
const laws = ref<LawInfo[]>([])
onMounted(async () => { laws.value = await lawApi.list() })
</script>
<template><view class="page"><LawCard v-for="item in laws" :key="item.id" :law-info="item" /></view></template>
<style scoped lang="scss">.page { padding: 24rpx; }</style>
