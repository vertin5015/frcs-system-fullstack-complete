<script setup lang="ts">
import { ref } from 'vue'
const categories = ['美国', '欧盟', '日本', '韩国']
const activeCategory = ref(categories[0])
const knowledge = ref(['跨境合同订立要点', '海外数据合规基础', '争议解决条款说明'])
</script>

<template><view class="page"><view>海外法律知识</view><view class="categories"><button v-for="item in categories" :key="item" size="mini" @click="activeCategory = item">{{ item }}</button></view><view>当前分类：{{ activeCategory }}</view><view v-for="item in knowledge" :key="item">{{ item }}</view></view></template>
<style scoped lang="scss">.page { padding: 24rpx; } .categories { display: flex; gap: 12rpx; margin: 24rpx 0; }</style>
