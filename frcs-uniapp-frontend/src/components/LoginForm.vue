<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { LoginPayload } from '../types/user'

const mode = ref<'password' | 'captcha'>('password')
const form = reactive<LoginPayload>({ email: '', password: '', captcha: '' })
const emit = defineEmits<{ submit: [payload: LoginPayload] }>()
function submit() { emit('submit', { ...form, password: mode.value === 'password' ? form.password : undefined }) }
</script>

<template>
  <view>
    <button size="mini" @click="mode = mode === 'password' ? 'captcha' : 'password'">切换至{{ mode === 'password' ? '验证码' : '密码' }}登录</button>
    <input v-model="form.email" placeholder="邮箱" />
    <input v-if="mode === 'password'" v-model="form.password" password placeholder="密码" />
    <input v-else v-model="form.captcha" placeholder="验证码" />
    <button @click="submit">登录</button>
  </view>
</template>
