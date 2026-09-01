<template>
  <view class="page-container">
    <view class="header">
      <view class="logo"><text class="logo-text">法</text></view>
      <view class="title">涉外案例查询分析系统</view>
      <view class="subtitle">Foreign-related Case Analysis</view>
    </view>

    <view class="login-card">
      <view class="login-tabs">
        <view class="tab-item" @click="switchMode('password')">
          <view :class="['radio-icon', loginMode === 'password' ? 'active' : '']"></view>
          <text :class="['tab-text', loginMode === 'password' ? 'text-active' : '']">密码登录</text>
        </view>
        <view class="tab-item" @click="switchMode('code')">
          <view :class="['radio-icon', loginMode === 'code' ? 'active' : '']"></view>
          <text :class="['tab-text', loginMode === 'code' ? 'text-active' : '']">验证码登录</text>
        </view>
      </view>

      <view class="form-area">
        <view class="input-box">
          <text class="icon-placeholder">✉</text> 
          <input class="input-field" v-model="formData.email" type="text" placeholder="邮箱" placeholder-class="ph-color" />
        </view>

        <view class="input-box" v-if="loginMode === 'password'">
          <text class="icon-placeholder">🔒</text>
          <input class="input-field" v-model="formData.password" password placeholder="密码" placeholder-class="ph-color" />
        </view>

        <view class="input-box-group" v-if="loginMode === 'code'">
          <view class="input-box code-input">
            <text class="icon-placeholder">▷</text>
            <input class="input-field" v-model="formData.code" type="number" placeholder="验证码" placeholder-class="ph-color" />
          </view>
          <button class="get-code-btn" :disabled="countdown > 0" @click="sendCode">
            {{ countdown > 0 ? `${countdown}s后重试` : '获取验证码' }}
          </button>
        </view>
      </view>

      <view class="action-links">
        <view class="links-left">
          <text class="link-gray" @click="showResetModal = true">忘记密码</text>
          <text class="link-sep">|</text>
          <text class="link-gray" @click="showChangeModal = true">修改密码</text>
        </view>
        <navigator url="/pages/login/register" hover-class="none" class="link-gray">还没有账号？</navigator>
      </view>

      <button class="submit-btn" @click="handleLogin">登录</button>

      <view class="wechat-login" @click="handleWechatLogin">
        <text class="line"></text>
        <text class="wechat-text">使用微信一键登录</text>
        <text class="line"></text>
      </view>
    </view>

    <view class="guest-access" @click="guestAccess">
      <text class="guest-text">游客访问</text>
    </view>

    <view class="terms-area">
      <view class="checkbox-wrapper" @click="isAgreed = !isAgreed">
        <view :class="['radio-icon', isAgreed ? 'active' : '']"></view>
      </view>
      <text class="terms-text">登录即表示同意</text>
      <text class="terms-link">《用户协议》</text>
      <text class="terms-text">和</text>
      <text class="terms-link">《隐私政策》</text>
    </view>

    <view class="modal-overlay" v-if="showResetModal">
      <view class="modal-card">
        <view class="modal-header">
          <text class="modal-title">重置密码</text>
          <view class="close-btn" @click="closeResetModal">
            <text class="close-icon">×</text>
          </view>
        </view>
        
        <view class="form-area">
          <view class="input-box">
            <text class="icon-placeholder">✉</text>
            <input class="input-field" v-model="resetForm.email" type="text" placeholder="邮箱" placeholder-class="ph-color" />
          </view>
          
          <view class="input-box-group">
            <view class="input-box code-input">
              <text class="icon-placeholder">▷</text>
              <input class="input-field" v-model="resetForm.code" type="number" placeholder="验证码" placeholder-class="ph-color" />
            </view>
            <button class="get-code-btn" :disabled="resetCountdown > 0" @click="sendResetCode">
              {{ resetCountdown > 0 ? `${resetCountdown}s后重试` : '获取验证码' }}
            </button>
          </view>

          <view class="input-box">
            <text class="icon-placeholder">🔒</text>
            <input class="input-field" v-model="resetForm.newPassword" password placeholder="新密码" placeholder-class="ph-color" />
          </view>
        </view>

        <view class="modal-footer">
          <button class="submit-btn reset-confirm-btn" @click="handleResetPassword">确认</button>
        </view>
      </view>
    </view>

    <view class="modal-overlay" v-if="showChangeModal">
      <view class="modal-card">
        <view class="modal-header">
          <text class="modal-title">修改密码</text>
          <view class="close-btn" @click="closeChangeModal">
            <text class="close-icon">×</text>
          </view>
        </view>

        <view class="form-area">
          <view class="input-box">
            <text class="icon-placeholder">✉</text>
            <input class="input-field" v-model="changeForm.email" type="text" placeholder="邮箱" placeholder-class="ph-color" />
          </view>
          <view class="input-box">
            <text class="icon-placeholder">🔒</text>
            <input class="input-field" v-model="changeForm.oldPassword" password placeholder="当前密码" placeholder-class="ph-color" />
          </view>
          <view class="input-box">
            <text class="icon-placeholder">🔒</text>
            <input class="input-field" v-model="changeForm.newPassword" password placeholder="新密码（至少8位含字母和数字）" placeholder-class="ph-color" />
          </view>
          <view class="input-box">
            <text class="icon-placeholder">🔒</text>
            <input class="input-field" v-model="changeForm.confirm" password placeholder="确认新密码" placeholder-class="ph-color" />
          </view>
        </view>

        <view class="modal-footer">
          <button class="submit-btn reset-confirm-btn" :loading="changeLoading" @click="handleChangePassword">确认修改</button>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useUserStore } from '../../store/user'
import api from '../../api'

const userStore = useUserStore()
const loading = ref(false)

// ================= 【登录模块状态】 =================
const loginMode = ref<'password' | 'code'>('password')
const isAgreed = ref(false)
const countdown = ref(0)
let timer: number | null = null

const formData = reactive({
  email: '',
  password: '',
  code: ''
})

// ================= 【重置密码模块状态】 =================
const showResetModal = ref(false)
const resetCountdown = ref(0)
let resetTimer: number | null = null

const resetForm = reactive({
  email: '',
  code: '',
  newPassword: '',
  confirm: ''
})

// ================= 【修改密码模块状态】 =================
const showChangeModal = ref(false)
const changeLoading = ref(false)
const changeForm = reactive({
  email: '',
  oldPassword: '',
  newPassword: '',
  confirm: ''
})

// ================= 【基础工具方法】 =================
const emailValid = (v: string) => /^[\w.-]+@[\w.-]+\.[a-zA-Z]{2,}$/.test(v)
const passwordValid = (v: string) => /^(?=.*[A-Za-z])(?=.*\d)[\w!@#$%^&*()_+\-=.]{8,}$/.test(v)

/**
 * 切换登录模式并清空数据
 */
const switchMode = (mode: 'password' | 'code') => {
  if (loginMode.value !== mode) {
    loginMode.value = mode
    formData.email = ''
    formData.password = ''
    formData.code = ''
  }
}

/**
 * 获取登录验证码（对接 /api/auth/send-code，purpose=LOGIN）
 */
const sendCode = async () => {
  if (!formData.email) {
    return uni.showToast({ title: '请先填写邮箱', icon: 'none' })
  }
  if (!emailValid(formData.email)) {
    return uni.showToast({ title: '请输入正确的邮箱格式', icon: 'none' })
  }
  if (countdown.value > 0) return

  try {
    const res = await api.sendAuthCode(formData.email, 'LOGIN')
    if (res.code !== 200) {
      return uni.showToast({ title: res.message || '发送失败', icon: 'none' })
    }
    countdown.value = res.data?.cooldownSeconds || 60
    timer = setInterval(() => {
      if (countdown.value > 0) {
        countdown.value--
      } else {
        if (timer) clearInterval(timer)
      }
    }, 1000)
    if (res.data?.devCode) {
      uni.showModal({ title: '开发模式验证码', content: `验证码：${res.data.devCode}` })
    }
    uni.showToast({ title: '验证码已发送', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '发送失败', icon: 'none' })
  }
}

// ================= 【交互事件方法】 =================
const handleLogin = async() => {
  if (!isAgreed.value) {
    return uni.showToast({ title: '请先阅读并同意用户协议与隐私政策', icon: 'none' })
  }
  if (!formData.email) {
    return uni.showToast({ title: '请输入邮箱', icon: 'none' })
  }

  loading.value = true
  try {
    let res
    if (loginMode.value === 'password') {
      if (!formData.password) {
        loading.value = false
        return uni.showToast({ title: '请输入密码', icon: 'none' })
      }
      res = await api.login(formData.email, formData.password)
    } else {
      if (!formData.code) {
        loading.value = false
        return uni.showToast({ title: '请输入验证码', icon: 'none' })
      }
      res = await api.loginByCode(formData.email, formData.code.trim())
    }

    if (res.code !== 200) {
      return uni.showToast({ title: res.message || '登录失败', icon: 'none' })
    }
    // 与网页端一致：token 存字符串 "true"，并保存 userId/username/summaryCredits
    userStore.setLogin(res.data, formData.email)
    uni.showToast({ title: '登录成功', icon: 'success' })

    setTimeout(() => {
      uni.reLaunch({
        url: '/pages/home/index',
        fail: (err) => {
           console.error('跳转首页失败:', err)
        }
      })
    }, 1000)
  } catch (error: any) {
    uni.showToast({ 
      title: error.serverMessage || error.message || '登录失败', 
      icon: 'error' 
    })
  } finally {
    loading.value = false
  }
}

/**
 * 微信一键登录
 */
const handleWechatLogin = () => {
  if (!isAgreed.value) {
    return uni.showToast({ title: '请先阅读并同意用户协议与隐私政策', icon: 'none' })
  }

  // 后端当前未提供微信登录接口，先获取 code 后提示，引导使用邮箱登录
  uni.login({
    provider: 'weixin',
    success: async (res) => {
      console.log('微信 code 已获取，但后端暂未实现微信登录:', res.code)
      uni.showModal({
        title: '提示',
        content: '服务端暂未提供微信登录接口，请使用邮箱密码或验证码登录。',
        showCancel: false,
      })
    },
    fail: (err) => {
      console.error('微信授权失败:', err)
      uni.showToast({ title: '获取微信授权失败', icon: 'none' })
    }
  })
}

/**
 * 游客访问
 */
const guestAccess = async () => {
  if (!isAgreed.value) {
    return uni.showToast({ title: '请先阅读并同意用户协议与隐私政策', icon: 'none' })
  }

  loading.value = true
  try {
    // 与网页端一致：游客为本地登录态（userId=0）
    userStore.guestLogin()
    uni.showToast({ title: '已作为游客进入', icon: 'success' })
    
    setTimeout(() => {
      uni.reLaunch({ 
        url: '/pages/home/index',
        fail: (err) => console.error('游客跳转首页失败:', err)
      })
    }, 800)
  } catch (error) {
    uni.showToast({ title: '游客访问失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

/**
 * 关闭重置密码弹窗
 */
const closeResetModal = () => {
  showResetModal.value = false
  resetForm.email = ''
  resetForm.code = ''
  resetForm.newPassword = ''
  if (resetTimer) clearInterval(resetTimer)
  resetCountdown.value = 0
}

/**
 * 发送重置密码验证码
 */
const sendResetCode = () => {
  if (!resetForm.email) {
    return uni.showToast({ title: '请先填写邮箱', icon: 'none' })
  }
  if (!emailValid(resetForm.email)) {
    return uni.showToast({ title: '请输入正确的邮箱格式', icon: 'none' })
  }
  if (resetCountdown.value > 0) return

  api.sendAuthCode(resetForm.email, 'RESET')
    .then((res) => {
      if (res.code !== 200) {
        return uni.showToast({ title: res.message || '发送失败', icon: 'none' })
      }
      resetCountdown.value = res.data?.cooldownSeconds || 60
      resetTimer = setInterval(() => {
        if (resetCountdown.value > 0) {
          resetCountdown.value--
        } else {
          if (resetTimer) clearInterval(resetTimer)
        }
      }, 1000)
      if (res.data?.devCode) {
        uni.showModal({ title: '开发模式验证码', content: `验证码：${res.data.devCode}` })
      }
      uni.showToast({ title: '验证码已发送', icon: 'success' })
    })
    .catch((e: any) => {
      uni.showToast({ title: e.serverMessage || '发送失败', icon: 'none' })
    })
}

/**
 * 提交重置密码
 */
const handleResetPassword = async () => {
  if (!resetForm.email || !resetForm.code || !resetForm.newPassword) {
    return uni.showToast({ title: '请填写完整信息', icon: 'none' })
  }
  if (resetForm.newPassword !== resetForm.confirm) {
    return uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
  }
  if (!passwordValid(resetForm.newPassword)) {
    return uni.showToast({ title: '密码需至少8位且包含字母和数字', icon: 'none' })
  }

  try {
    const res = await api.resetPasswordByCode(resetForm.email, resetForm.code.trim(), resetForm.newPassword)
    if (res.code !== 200) {
      return uni.showToast({ title: res.message || '重置失败', icon: 'none' })
    }
    uni.showToast({ title: '密码重置成功', icon: 'success' })
    setTimeout(() => {
      closeResetModal()
    }, 1000)
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '重置失败', icon: 'none' })
  }
}

// ================= 【修改密码交互】 =================
const closeChangeModal = () => {
  showChangeModal.value = false
  changeForm.email = ''
  changeForm.oldPassword = ''
  changeForm.newPassword = ''
  changeForm.confirm = ''
}

const handleChangePassword = async () => {
  if (!changeForm.email || !changeForm.oldPassword || !changeForm.newPassword || !changeForm.confirm) {
    return uni.showToast({ title: '请填写完整信息', icon: 'none' })
  }
  if (!emailValid(changeForm.email)) {
    return uni.showToast({ title: '请输入正确的邮箱格式', icon: 'none' })
  }
  if (changeForm.newPassword !== changeForm.confirm) {
    return uni.showToast({ title: '两次输入的新密码不一致', icon: 'none' })
  }
  if (!passwordValid(changeForm.newPassword)) {
    return uni.showToast({ title: '密码需至少8位且包含字母和数字', icon: 'none' })
  }

  changeLoading.value = true
  try {
    const res = await api.changePasswordApi(changeForm.email, changeForm.oldPassword, changeForm.newPassword)
    if (res.code !== 200) {
      return uni.showToast({ title: res.message || '修改失败', icon: 'none' })
    }
    uni.showToast({ title: '密码已修改，请使用新密码登录', icon: 'success' })
    setTimeout(() => closeChangeModal(), 1000)
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '修改失败', icon: 'none' })
  } finally {
    changeLoading.value = false
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
}

/* 顶部信息 */
.header {
  margin-top: 200rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .logo {
    width: 140rpx;
    height: 140rpx;
    border-radius: 50%;
    background-color: #218cff;
    margin-bottom: 30rpx;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 12rpx 32rpx rgba(33, 140, 255, 0.35);

    .logo-text {
      font-size: 64rpx;
      font-weight: bold;
      color: #ffffff;
      line-height: 1;
    }
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
    margin-bottom: 60rpx;
  }
}

/* 登录卡片 */
.login-card {
  width: 100%;
  background-color: #ffffff;
  border-radius: 32rpx;
  box-shadow: 0 16rpx 48rpx rgba(37, 99, 235, 0.06);
  padding: 50rpx 40rpx;
  box-sizing: border-box;
}

/* 统一单选圆圈样式 */
.radio-icon {
  width: 28rpx;
  height: 28rpx;
  border-radius: 50%;
  border: 3rpx solid #cccccc;
  margin-right: 12rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  
  &.active {
    border-color: #218cff;
    position: relative;
    &::after {
      content: '';
      width: 14rpx;
      height: 14rpx;
      border-radius: 50%;
      background-color: #218cff;
    }
  }
}

/* Tabs 切换 */
.login-tabs {
  display: flex;
  justify-content: center;
  margin-bottom: 50rpx;

  .tab-item + .tab-item {
    margin-left: 80rpx;
  }
  
  .tab-item {
    display: flex;
    align-items: center;
    
    .tab-text {
      font-size: 30rpx;
      color: #999999;
      font-weight: 500;
      transition: color 0.2s;
    }
    .text-active {
      color: #218cff;
      font-weight: 600;
    }
  }
}

/* 表单输入区域 */
.form-area {
  display: flex;
  flex-direction: column;

  & > view {
    margin-bottom: 30rpx;

    &:last-child {
      margin-bottom: 0;
    }
  }

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

  .input-box-group {
    display: flex;
    align-items: center;
    
    .code-input {
      flex: 1;
      margin-right: 20rpx;
    }
    
    .get-code-btn {
      width: 220rpx;
      height: 96rpx;
      line-height: 96rpx;
      background-color: #218cff;
      color: #ffffff;
      font-size: 26rpx;
      border-radius: 48rpx;
      margin: 0;
      padding: 0;
      &::after { border: none; }
      &[disabled] {
        background-color: #a0cfff;
        color: #ffffff;
      }
    }
  }
}

/* placeholder 颜色覆盖 */
:deep(.ph-color) { color: #cccccc; }

/* 链接区域 */
.action-links {
  display: flex;
  justify-content: space-between;
  margin-top: 30rpx;
  margin-bottom: 50rpx;
  padding: 0 10rpx;

  .links-left {
    display: flex;
    align-items: center;

    & > text + text {
      margin-left: 16rpx;
    }
  }

  .link-sep {
    font-size: 24rpx;
    color: #cccccc;
  }

  .link-gray {
    font-size: 24rpx;
    color: #999999;
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

/* 微信文本登录 */
.wechat-login {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-top: 40rpx;
  
  .line {
    width: 60rpx;
    height: 1px;
    background-color: #eeeeee;
  }
  
  .wechat-text {
    font-size: 24rpx;
    color: #999999;
    margin: 0 20rpx;
  }
}

/* 游客访问 */
.guest-access {
  margin-top: 40rpx;
  
  .guest-text {
    font-size: 24rpx;
    color: #999999;
    border-bottom: 1px solid #cccccc;
    padding-bottom: 4rpx;
  }
}

/* 底部协议 */
.terms-area {
  position: absolute;
  bottom: 80rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  
  .checkbox-wrapper {
    padding: 10rpx;
  }
  
  .radio-icon {
    margin-right: 6rpx; 
    width: 24rpx;
    height: 24rpx;
    &.active::after {
      width: 12rpx;
      height: 12rpx;
    }
  }

  .terms-text {
    font-size: 24rpx;
    color: #999999;
  }
  
  .terms-link {
    font-size: 24rpx;
    color: #333333;
    font-weight: 500;
  }
}

/* 重置密码弹窗样式 */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 999;
}

.modal-card {
  width: 80%;
  background-color: #ffffff;
  border-radius: 40rpx;
  padding: 50rpx 40rpx;
  box-sizing: border-box;
  animation: scaleUp 0.3s ease-out;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 50rpx;
  
  .modal-title {
    font-size: 36rpx;
    font-weight: 600;
    color: #333333;
  }
  
  .close-btn {
    width: 44rpx;
    height: 44rpx;
    border-radius: 50%;
    border: 3rpx solid #218cff;
    display: flex;
    align-items: center;
    justify-content: center;
    
    .close-icon {
      font-size: 34rpx;
      color: #218cff;
      line-height: 1;
      margin-top: -4rpx;
    }
  }
}

.modal-footer {
  display: flex;
  justify-content: center;
  margin-top: 50rpx;
  
  .reset-confirm-btn {
    width: 260rpx;
    height: 88rpx;
    line-height: 88rpx;
    margin: 0;
  }
}

@keyframes scaleUp {
  from { transform: scale(0.9); opacity: 0; }
  to { transform: scale(1); opacity: 1; }
}
</style>
