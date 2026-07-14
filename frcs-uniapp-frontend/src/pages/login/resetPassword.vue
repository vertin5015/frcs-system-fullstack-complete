<script setup lang="ts">
import { reactive } from 'vue'
const form = reactive({ email: '', captcha: '', newPassword: '' })
function submit() { uni.showToast({ title: '密码重置请求已提交', icon: 'none' }); void form }
</script>

<template><view class="page"><input v-model="form.email" placeholder="邮箱" /><input v-model="form.captcha" placeholder="验证码" /><input v-model="form.newPassword" password placeholder="新密码" /><button @click="submit">重置密码</button></view></template>
<style scoped lang="scss">.page { padding: 32rpx; } input { margin: 16rpx 0; background: #fff; padding: 20rpx; }</style>
