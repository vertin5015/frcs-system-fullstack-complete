<template>
  <view class="page">
    <view class="form-card">
      <view class="title">重置密码</view>

      <view class="input-box">
        <text class="icon-placeholder">✉</text>
        <input v-model="form.email" type="text" placeholder="邮箱" placeholder-class="ph-color" />
      </view>

      <view class="input-box-group">
        <view class="input-box code-input">
          <text class="icon-placeholder">▷</text>
          <input v-model="form.code" type="number" placeholder="验证码" placeholder-class="ph-color" />
        </view>
        <button class="get-code-btn" :disabled="countdown > 0" @click="sendCode">
          {{ countdown > 0 ? `${countdown}s后重试` : '获取验证码' }}
        </button>
      </view>

      <view class="input-box">
        <text class="icon-placeholder">🔒</text>
        <input v-model="form.newPassword" password placeholder="新密码（至少8位含字母和数字）" placeholder-class="ph-color" />
      </view>

      <view class="input-box">
        <text class="icon-placeholder">🔒</text>
        <input v-model="form.confirm" password placeholder="确认新密码" placeholder-class="ph-color" />
      </view>

      <button class="submit-btn" :loading="loading" @click="submit">重置密码</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import api from '../../api'

const form = reactive({ email: '', code: '', newPassword: '', confirm: '' })
const loading = ref(false)
const countdown = ref(0)
let timer: number | null = null

const emailValid = (v: string) => /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/.test(v)
const passwordValid = (v: string) => /^(?=.*[A-Za-z])(?=.*\d)[\w!@#$%^&*()_+\-=.]{8,}$/.test(v)

const sendCode = async () => {
  if (!form.email) return uni.showToast({ title: '请先填写邮箱', icon: 'none' })
  if (!emailValid(form.email)) return uni.showToast({ title: '请输入正确的邮箱格式', icon: 'none' })
  if (countdown.value > 0) return
  try {
    const res = await api.sendAuthCode(form.email, 'RESET')
    if (res.code !== 200) return uni.showToast({ title: res.message || '发送失败', icon: 'none' })
    countdown.value = res.data?.cooldownSeconds || 60
    timer = setInterval(() => {
      if (countdown.value > 0) countdown.value--
      else if (timer) clearInterval(timer)
    }, 1000)
    if (res.data?.devCode) uni.showModal({ title: '开发模式验证码', content: `验证码：${res.data.devCode}` })
    uni.showToast({ title: '验证码已发送', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '发送失败', icon: 'none' })
  }
}

async function submit() {
  if (!form.email || !form.code || !form.newPassword || !form.confirm) {
    return uni.showToast({ title: '请填写完整信息', icon: 'none' })
  }
  if (form.newPassword !== form.confirm) {
    return uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
  }
  if (!passwordValid(form.newPassword)) {
    return uni.showToast({ title: '密码需至少8位且包含字母和数字', icon: 'none' })
  }
  loading.value = true
  try {
    const res = await api.resetPasswordByCode(form.email.trim(), form.code.trim(), form.newPassword)
    if (res.code !== 200) return uni.showToast({ title: res.message || '重置失败', icon: 'none' })
    uni.showToast({ title: '密码重置成功', icon: 'success' })
    setTimeout(() => uni.navigateBack({ fail: () => uni.redirectTo({ url: '/pages/login/login' }) }), 1200)
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '重置失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.page {
  min-height: 100vh;
  background: linear-gradient(180deg, #e6f0ff 0%, #f5faff 40%, #ffffff 100%);
  padding: 80rpx 40rpx;
  box-sizing: border-box;
}
.form-card {
  background: #fff;
  border-radius: 32rpx;
  padding: 50rpx 40rpx;
  box-shadow: 0 16rpx 48rpx rgba(37, 99, 235, 0.06);
  .title { font-size: 36rpx; font-weight: 600; color: #333; text-align: center; margin-bottom: 40rpx; }
}
.input-box {
  display: flex; align-items: center; background: #f6f6f6; height: 96rpx; border-radius: 48rpx; padding: 0 30rpx; margin-bottom: 30rpx;
  .icon-placeholder { font-size: 32rpx; color: #b3b3b3; margin-right: 20rpx; }
  input { flex: 1; height: 100%; font-size: 28rpx; color: #333; }
}
.input-box-group { display: flex; align-items: center; margin-bottom: 30rpx;
  .code-input { flex: 1; margin-bottom: 0; margin-right: 20rpx; }
  .get-code-btn { width: 220rpx; height: 96rpx; line-height: 96rpx; background: #218cff; color: #fff; font-size: 26rpx; border-radius: 48rpx; margin: 0; padding: 0;
    &::after { border: none; }
    &[disabled] { background: #a0cfff; }
  }
}
.submit-btn { width: 100%; height: 96rpx; line-height: 96rpx; background: #218cff; border-radius: 48rpx; color: #fff; font-size: 32rpx; font-weight: 600;
  &::after { border: none; }
}
:deep(.ph-color) { color: #cccccc; }
</style>
