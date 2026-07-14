<script setup lang="ts">
const keyword = defineModel<string>({ default: '' })
withDefaults(defineProps<{ placeholder?: string }>(), { placeholder: '请输入关键词' })
const emit = defineEmits<{ search: [keyword: string] }>()

function submit() { emit('search', keyword.value) }
</script>

<template>
  <view class="search-bar">
    <input v-model="keyword" :placeholder="placeholder" confirm-type="search" @confirm="submit" />
    <button size="mini" @click="submit">搜索</button>
  </view>
</template>

<style scoped lang="scss">
.search-bar { display: flex; gap: 16rpx; padding: 16rpx 0; }
input { flex: 1; background: #fff; padding: 16rpx; }
</style>
