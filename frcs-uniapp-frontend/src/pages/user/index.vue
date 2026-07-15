<template>
  <view class="user-container">
    
    <view class="header-section">
      <view class="header-safe-area"></view>
      
      <view class="user-info-box">
        <image class="avatar" :src="user?.avatar || '/static/default-avatar.png'" mode="aspectFill" />
        <view class="info-text">
          <text class="nickname">{{ user?.username || '未登录用户' }}</text>
          <text class="email">邮箱：{{ user?.email || '-' }}</text>
        </view>
      </view>
    </view>

    <scroll-view scroll-y class="main-scroll">
      <view class="content-wrapper">
        
        <view class="balance-card">
          <view class="balance-left">
            <image class="icon-wallet" src="/static/icons/wallet.png" mode="aspectFit" />
            <text class="balance-text">当前余额：<text class="num">{{ user?.balance || '0' }}</text></text>
          </view>
          <button class="recharge-btn" @tap="handleRecharge">充值</button>
        </view>

        <view class="menu-list">
          <view class="menu-item" @tap="goStudy">
            <view class="item-left">
              <image class="menu-icon" src="/static/icons/book-blue.png" mode="aspectFit" />
              <text class="item-text">海外法律知识</text>
            </view>
            <image class="arrow-icon" src="/static/icons/arrow-right.png" mode="aspectFit" />
          </view>

          <view class="divider"></view>

          <view class="menu-item logout-item" @tap="handleLogout">
            <view class="item-left">
              <image class="menu-icon" src="/static/icons/logout.png" mode="aspectFit" />
              <text class="item-text logout-text">退出账号</text>
            </view>
          </view>
        </view>

      </view>
    </scroll-view>

    <BottomTabBar activeTab="user" />
  </view>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useUserStore } from '../../store/user' // 根据你的实际路径调整
import BottomTabBar from '../../components/BottomTabBar.vue' // 根据你的实际路径调整

const userStore = useUserStore()
const user = computed(() => userStore.userInfo)

// ================= 交互方法 =================

// 充值
function handleRecharge() {
  uni.showToast({ title: '充值功能待接入', icon: 'none' })
  // 接入后可使用: uni.navigateTo({ url: '/pages/recharge/index' })
}

// 跳转海外法律知识
function goStudy() {
  uni.navigateTo({ url: '/pages/study/index' })
}

// 退出登录
function handleLogout() {
  uni.showModal({
    title: '提示',
    content: '确定要退出当前账号吗？',
    confirmColor: '#218CFF',
    success: (res) => {
      if (res.confirm) {
        userStore.logout()
        uni.reLaunch({ url: '/pages/login/login' })
      }
    }
  })
}
</script>

<style scoped lang="scss">
/* ================= 整体页面样式 ================= */
.user-container {
  height: 100vh;
  background-color: #F5F7FA;
  display: flex;
  flex-direction: column;
  position: relative;
}

/* ================= 顶部信息区 ================= */
.header-section {
  background: linear-gradient(135deg, #218CFF 0%, #5CB3FF 100%);
  padding: 0 40rpx 60rpx 40rpx;
  border-radius: 0 0 40rpx 40rpx; /* 底部圆角 */
  box-shadow: 0 8rpx 20rpx rgba(33, 140, 255, 0.15);
}

.header-safe-area {
  padding-top: var(--status-bar-height, 44px);
  height: 44px;
}

.user-info-box {
  display: flex;
  align-items: center;
  margin-top: 20rpx;
  
  .avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 50%;
    background-color: #EBF4FF;
    border: 4rpx solid rgba(255, 255, 255, 0.5);
    margin-right: 30rpx;
  }
  
  .info-text {
    display: flex;
    flex-direction: column;
    justify-content: center;
    
    .nickname {
      font-size: 36rpx;
      font-weight: bold;
      color: #ffffff;
      margin-bottom: 12rpx;
    }
    
    .email {
      font-size: 26rpx;
      color: rgba(255, 255, 255, 0.85);
    }
  }
}

/* ================= 滚动区与主体 ================= */
.main-scroll {
  flex: 1;
  height: 0;
  margin-top: -30rpx; /* 向上偏移，产生卡片交错的层叠感 */
  z-index: 2;
  
  .content-wrapper {
    padding: 0 24rpx;
    padding-bottom: 140rpx; /* 留出底部导航栏高度 */
  }
}

/* ================= 余额卡片 ================= */
.balance-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 36rpx 30rpx;
  margin-bottom: 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
  
  .balance-left {
    display: flex;
    align-items: center;
    
    .icon-wallet {
      width: 40rpx;
      height: 40rpx;
      margin-right: 16rpx;
    }
    
    .balance-text {
      font-size: 28rpx;
      color: #333333;
      
      .num {
        font-size: 36rpx;
        font-weight: bold;
        color: #FF8F1F; /* 使用橙色突出金额 */
        margin-left: 8rpx;
      }
    }
  }
  
  .recharge-btn {
    margin: 0;
    width: 140rpx;
    height: 64rpx;
    line-height: 64rpx;
    font-size: 26rpx;
    color: #ffffff;
    background-color: #218CFF;
    border-radius: 32rpx;
    
    &::after { border: none; }
    &:active { background-color: #1a73d9; }
  }
}

/* ================= 菜单列表卡片 ================= */
.menu-list {
  background-color: #ffffff;
  border-radius: 24rpx;
  padding: 0 30rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}

.menu-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 36rpx 0;
  
  &:active {
    opacity: 0.7;
  }
  
  .item-left {
    display: flex;
    align-items: center;
    
    .menu-icon {
      width: 40rpx;
      height: 40rpx;
      margin-right: 24rpx;
    }
    
    .item-text {
      font-size: 30rpx;
      color: #333333;
    }
  }
  
  .arrow-icon {
    width: 32rpx;
    height: 32rpx;
    opacity: 0.3;
  }
}

.divider {
  height: 1rpx;
  background-color: #F0F2F5;
  width: 100%;
}

.logout-item {
  .item-left .menu-icon {
    opacity: 0.7; /* 退出图标可以稍微浅一点 */
  }
  .item-left .logout-text {
    color: #F56C6C; /* 退出使用红色警示色 */
  }
}
</style>