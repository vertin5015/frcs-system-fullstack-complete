<script setup lang="ts">
import LoginForm from '../../components/LoginForm.vue'
import type { LoginPayload, User } from '../../types/user'
import { useUserStore } from '../../store/user'

const userStore = useUserStore()
function login(payload: LoginPayload) {
  const user: User = { id: 1, username: '法律用户', email: payload.email || 'guest@example.com', avatar: '' }
  userStore.setLogin('mock-token', user)
  uni.reLaunch({ url: '/pages/home/index' })
}
function guestAccess() { login({ email: 'guest@example.com' }) }
</script>

<template>
  <view class="page"><text>涉外案例查询与法律知识系统</text><LoginForm @submit="login" /><button open-type="getUserInfo">微信登录</button><button @click="guestAccess">游客访问</button><navigator url="/pages/login/register">注册账号</navigator><navigator url="/pages/login/resetPassword">忘记密码</navigator></view>
</template>

<style scoped lang="scss">.page { padding: 32rpx; }</style>
