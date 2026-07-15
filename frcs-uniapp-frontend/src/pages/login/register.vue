<template>
  <view class="page-container">
    <view class="header">
      <image class="logo" src="/static/logo.png" mode="aspectFit" />
      <view class="title">涉外案例查询分析系统</view>
      <view class="subtitle">Foreign-related Case Analysis</view>
    </view>

    <view class="login-card">
      <view class="form-title">新用户注册</view>

      <view class="form-area">
        <view class="input-box">
          <text class="icon-placeholder">👤</text>
          <input class="input-field" v-model="formData.username" type="text" placeholder="用户名" placeholder-class="ph-color" />
        </view>

        <view class="input-box">
          <text class="icon-placeholder">✉</text> 
          <input class="input-field" v-model="formData.email" type="text" placeholder="邮箱" placeholder-class="ph-color" />
        </view>

        <view class="input-box">
          <text class="icon-placeholder">🔒</text>
          <input class="input-field" v-model="formData.password" password placeholder="密码" placeholder-class="ph-color" />
        </view>

        <view class="input-box">
          <text class="icon-placeholder">🔒</text>
          <input class="input-field" v-model="formData.confirmPassword" password placeholder="确认密码" placeholder-class="ph-color" />
        </view>
      </view>

      <view class="action-links">
        <navigator url="/pages/login/login" open-type="navigateBack" hover-class="none" class="link-gray">已有账号？返回登录</navigator>
      </view>

      <button class="submit-btn" :loading="loading" @click="handleRegister">注册</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'

const loading = ref(false)

const formData = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

/**
 * 注册逻辑处理
 */
const handleRegister = async () => {
  // 1. 基础表单校验
  if (!formData.username.trim()) {
    return uni.showToast({ title: '请输入用户名', icon: 'none' })
  }
  if (!formData.email.trim()) {
    return uni.showToast({ title: '请输入邮箱', icon: 'none' })
  }
  if (!formData.password) {
    return uni.showToast({ title: '请输入密码', icon: 'none' })
  }
  if (formData.password !== formData.confirmPassword) {
    return uni.showToast({ title: '两次输入的密码不一致', icon: 'none' })
  }

  loading.value = true
  try {
    // 模拟注册网络请求延迟（后续可替换为真实的 authApi.register(...)）
    await new Promise(resolve => setTimeout(resolve, 600))

    uni.showToast({ title: '注册成功', icon: 'success' })

    // 延迟 1 秒后返回登录页
    setTimeout(() => {
      uni.navigateBack({
        fail: () => {
          // 若无法返回（如直接进入注册页），则重定向到登录页
          uni.redirectTo({ url: '/pages/login/login' })
        }
      })
    }, 1000)
  } catch (error: any) {
    uni.showToast({ title: error.message || '注册失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}
</script>

<style scoped lang="scss">
.page-container {
  min-height: 100vh;
  background: linear-gradient(180deg, #E6F0FF 0%, #F5FAFF 40%, #FFFFFF 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 0 40rpx;
  position: relative;
  box-sizing: border-box;
}

/* 顶部信息 */
.header {
  margin-top: 160rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .logo {
    width: 140rpx;
    height: 140rpx;
    border-radius: 50%;
    background-color: #218cff;
    margin-bottom: 30rpx;
  }
  
  .title {
    font-size: 48rpx;
    font-weight: 600;
    color: #333333;
    letter-spacing: 2rpx;
    margin-bottom: 12rpx;
  }
  
  .subtitle {
    font-size: 24rpx;
    color: #999999;
    font-weight: 500;
    margin-bottom: 40rpx;
  }
}

/* 注册卡片 */
.login-card {
  width: 100%;
  background-color: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 16rpx 48rpx rgba(37, 99, 235, 0.06);
  padding: 50rpx 40rpx;
  box-sizing: border-box;

  .form-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #333333;
    text-align: center;
    margin-bottom: 40rpx;
  }
}

/* 表单输入区域 */
.form-area {
  display: flex;
  flex-direction: column;
  gap: 30rpx;

  .input-box {
    display: flex;
    align-items: center;
    background-color: #f6f6f6;
    height: 96rpx;
    border-radius: 48rpx;
    padding: 0 30rpx;
    
    .icon-placeholder {
      font-size: 32rpx;
      color: #b3b3b3;
      margin-right: 20rpx;
    }
    
    .input-field {
      flex: 1;
      height: 100%;
      font-size: 28rpx;
      color: #333;
    }
  }
}

/* placeholder 颜色覆盖 */
:deep(.ph-color) { color: #cccccc; }

/* 链接区域 */
.action-links {
  display: flex;
  justify-content: flex-end;
  margin-top: 30rpx;
  margin-bottom: 40rpx;
  padding: 0 10rpx;
  
  .link-gray {
    font-size: 24rpx;
    color: #218cff;
  }
}

/* 提交按钮 */
.submit-btn {
  width: 100%;
  height: 96rpx;
  line-height: 96rpx;
  background-color: #218cff;
  border-radius: 48rpx;
  color: #ffffff;
  font-size: 32rpx;
  font-weight: 600;
  box-shadow: 0 8rpx 20rpx rgba(33, 140, 255, 0.3);
  &::after { border: none; }
  &:active { opacity: 0.8; }
}
</style>