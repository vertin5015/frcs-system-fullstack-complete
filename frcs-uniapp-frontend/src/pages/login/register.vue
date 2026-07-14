<script setup lang="ts">
import { reactive } from 'vue'
const form = reactive({ username: '', email: '', password: '', confirmPassword: '' })
function submit() { uni.showToast({ title: '注册信息已提交', icon: 'none' }); void form }
</script>

<template><view class="page"><input v-model="form.username" placeholder="用户名" /><input v-model="form.email" placeholder="邮箱" /><input v-model="form.password" password placeholder="密码" /><input v-model="form.confirmPassword" password placeholder="确认密码" /><button @click="submit">注册</button></view></template>
<style scoped lang="scss">.page { padding: 32rpx; } input { margin: 16rpx 0; background: #fff; padding: 20rpx; }</style>
