<script setup lang="ts">
export interface FilterOption { label: string; value: string }
const props = withDefaults(defineProps<{ countries?: FilterOption[]; dates?: FilterOption[]; sources?: FilterOption[] }>(), { countries: () => [], dates: () => [], sources: () => [] })
const emit = defineEmits<{ change: [filters: { country: string; date: string; source: string }] }>()

function notify() { emit('change', { country: '', date: '', source: '' }) }
void props
</script>

<template>
  <view class="filter-bar">
    <button size="mini" @click="notify">国家</button>
    <button size="mini" @click="notify">时间</button>
    <button size="mini" @click="notify">数据来源</button>
  </view>
</template>

<style scoped lang="scss">
.filter-bar { display: flex; gap: 16rpx; padding: 16rpx 0; }
</style>
