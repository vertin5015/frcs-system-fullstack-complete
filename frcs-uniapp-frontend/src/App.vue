<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAppStore } from './store/app'

onLaunch(() => {
  useAppStore().initialize()
})
</script>

<style lang="scss">
page {
  /* ===== 全局设计变量（保持主题色 #218CFF） ===== */
  --frcs-primary: #218CFF;
  --frcs-primary-light: #EBF4FF;
  --frcs-primary-deep: #1A73D9;
  --frcs-text-main: #333333;
  --frcs-text-sub: #666666;
  --frcs-text-muted: #999999;
  --frcs-bg: #F5F7FA;
  --frcs-card: #FFFFFF;
  --frcs-border: #E4E7ED;
  --frcs-success: #67C23A;
  --frcs-warning: #E6A23C;
  --frcs-danger: #F56C6C;
  --frcs-radius-lg: 24rpx;
  --frcs-radius-md: 16rpx;

  background: #F5F7FA;
  color: #333333;
  font-size: 28rpx;
  -webkit-font-smoothing: antialiased;
  box-sizing: border-box;
}

view,
text,
button,
input,
textarea {
  box-sizing: border-box;
}

/* ===== 通用工具类 ===== */
.flex-center {
  display: flex;
  align-items: center;
  justify-content: center;
}

.ellipsis {
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.clamp-2 {
  overflow: hidden;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
}

.frcs-card {
  background: #FFFFFF;
  border-radius: 24rpx;
  box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.04);
}
</style>
