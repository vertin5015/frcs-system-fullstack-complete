<script setup lang="ts">
withDefaults(defineProps<{ text?: string }>(), { text: '暂无数据' })
</script>

<template><view class="empty">{{ text }}</view></template>

<style scoped lang="scss">.empty { padding: 80rpx 24rpx; color: #999; text-align: center; }</style>
