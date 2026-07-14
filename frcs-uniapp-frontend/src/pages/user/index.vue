<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '../../store/user'
const userStore = useUserStore()
const user = computed(() => userStore.userInfo)
function recharge() { uni.showToast({ title: '充值功能待接入', icon: 'none' }) }
function goStudy() { uni.navigateTo({ url: '/pages/study/index' }) }
function logout() { userStore.logout(); uni.reLaunch({ url: '/pages/login/login' }) }
</script>
<template><view class="page"><image class="avatar" :src="user?.avatar || '/static/default-avatar.png'" mode="aspectFill" /><view>昵称：{{ user?.username || '未登录用户' }}</view><view>邮箱：{{ user?.email || '-' }}</view><button @click="recharge">充值</button><button @click="goStudy">海外法律知识</button><button @click="logout">退出登录</button></view></template>
<style scoped lang="scss">.page { padding: 24rpx; } .avatar { width: 96rpx; height: 96rpx; background: #ddd; }</style>
