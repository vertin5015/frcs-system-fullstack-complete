<!-- src/pages/case/detail.vue -->
<template>
  <view class="detail-container" v-if="caseMeta">
    <scroll-view scroll-y class="main-scroll">
      <view class="detail-card">
        <!-- 1. 标题与基本信息 -->
        <view class="section">
          <view class="section-title">
            <view class="accent-line"></view>
            <text class="title-text">{{ caseMeta.case_name || '未知案件名称' }}</text>
          </view>

          <view class="meta-grid">
            <text class="meta-item">案号：{{ caseMeta.case_id || '未知' }}</text>
            <text class="meta-item">国家/数据源：{{ countryName(caseMeta.country) }}</text>
            <text class="meta-item">判决日期：{{ caseMeta.judgement_date || '未知' }}</text>
            <text class="meta-item">引用次数：{{ caseMeta.citationCount ?? 0 }}</text>
          </view>
        </view>

        <view class="dashed-divider"></view>

        <!-- 2. 关键词摘要 -->
        <view class="section">
          <view class="section-title">
            <view class="accent-line"></view>
            <text class="title-text">关键词摘要</text>
          </view>
          <text class="content-text">{{ caseMeta.tags || '暂无摘要' }}</text>
        </view>

        <view class="dashed-divider"></view>

        <!-- 3. AI 摘要 -->
        <view class="section">
          <view class="section-title ai-title-row">
            <view class="accent-line"></view>
            <text class="title-text">AI 摘要</text>
            <text v-if="summaryCredits !== null" class="credits-text">剩余 {{ summaryCredits }} 次</text>
          </view>

          <view v-if="isGuest" class="guest-tip">
            <text>游客模式无法使用 AI 摘要，请登录后使用。</text>
          </view>

          <view v-else>
            <view v-if="summaryStatusText" class="status-text">{{ summaryStatusText }}</view>
            <view v-if="summaryError" class="error-text">{{ summaryError }}</view>
            <view v-if="summaryContent" class="summary-content">{{ summaryContent }}</view>

            <view class="summary-actions">
              <button
                class="summary-btn"
                :disabled="summarizing"
                @tap="runSummary()"
              >{{ summaryContent ? '重新生成' : '生成摘要' }}</button>
            </view>
          </view>
        </view>

        <view class="dashed-divider"></view>

        <!-- 4. 本案问答 -->
        <view class="section">
          <view class="section-title">
            <view class="accent-line"></view>
            <text class="title-text">本案问答</text>
          </view>

          <view v-if="isGuest" class="guest-tip">
            <text>登录后可使用本案问答。</text>
          </view>
          <block v-else>
            <view class="qa-messages">
              <view v-for="(m, i) in qaMessages" :key="i" class="qa-bubble" :class="m.role">
                <text class="qa-role">{{ m.role === 'user' ? '问' : '答' }}</text>
                <text class="qa-text">{{ m.text }}</text>
              </view>
              <view v-if="qaLoading" class="qa-bubble assistant">
                <text class="qa-role">答</text>
                <text class="qa-text">正在思考…</text>
              </view>
            </view>

            <view class="qa-input-row">
              <input
                class="qa-input"
                v-model="qaInput"
                :placeholder="summaryContent ? '输入与本案相关的问题…' : '请先生成 AI 摘要后再提问'"
                :disabled="!summaryContent"
                confirm-type="send"
                @confirm="sendQa"
              />
              <button class="qa-send-btn" :disabled="!summaryContent || !qaInput.trim() || qaLoading" @tap="sendQa">发送</button>
            </view>
          </block>
        </view>
      </view>
    </scroll-view>

    <!-- 底部固定的操作栏 -->
    <view class="bottom-action-bar">
      <button
        class="action-btn favorite-btn"
        :class="{ 'is-favorited': isFavorite }"
        @tap="handleFavorite"
      >
        <text>{{ isFavorite ? '已收藏' : '收藏' }}</text>
      </button>

      <button class="action-btn original-btn" @tap="openOriginal">
        查看原文
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onUnload } from '@dcloudio/uni-app'
import api from '../../api'
import { useUserStore } from '../../store/user'
import type { CaseBaseInfo } from '../../types/case'

const userStore = useUserStore()
const caseMeta = ref<CaseBaseInfo>()
const caseId = ref('')

const isGuest = computed(() => userStore.isGuest)
const summaryCredits = ref<number | null>(userStore.getCredits())

// 收藏状态
const isFavorite = ref(false)

// AI 摘要状态
const summarizing = ref(false)
const summaryStatusText = ref('')
const summaryError = ref('')
const summaryContent = ref('')
let pollAbort = false
let pollTimer: number | null = null

// 本案问答
const qaMessages = ref<{ role: 'user' | 'assistant'; text: string }[]>([])
const qaInput = ref('')
const qaLoading = ref(false)

const countryName = (code?: string) => {
  if (code === 'US') return '美国'
  if (code === 'EU') return '欧盟'
  if (code === 'JPN') return '日本'
  return code || ''
}

const safeDecode = (v?: string) => {
  if (!v) return ''
  try {
    return decodeURIComponent(v)
  } catch {
    return v
  }
}

const refreshCredits = async () => {
  if (userStore.isGuest) return
  try {
    const res = await api.getUserSummaryCredits(userStore.userId)
    if (res.code === 200 && res.data != null) {
      summaryCredits.value = res.data
      userStore.updateCredits(res.data)
    }
  } catch {
    /* ignore */
  }
}

const loadMeta = async (id: string) => {
  try {
    const res = await api.getCaseMeta(id, 'zh', userStore.userId)
    if (res.code !== 200 || !res.data) {
      uni.showToast({ title: res.message || '加载案例失败', icon: 'none' })
      return
    }
    caseMeta.value = res.data
    isFavorite.value = !!res.data.isfavored
  } catch (e: any) {
    uni.showToast({ title: e.serverMessage || '网络错误', icon: 'none' })
  }
}

const sleep = (ms: number) => new Promise((r) => setTimeout(r, ms))

const mapSummaryError = (raw?: string, code?: number) => {
  const s = String(raw || '').trim()
  if (code === 40301 || s.includes('QUOTA_EXCEEDED') || s.includes('insufficient_user_quota') || s.includes('用户额度不足')) {
    return 'AI 摘要额度不足，请先购买次数后重试。'
  }
  if (!s) return '获取摘要失败（请检查网络或后端服务）'
  if (s.startsWith('HTTP ') || s.startsWith('{"error"')) return 'AI 服务暂时不可用，请稍后重试。'
  return s
}

const runSummary = async (force = false) => {
  if (!caseId.value || userStore.isGuest) return
  pollAbort = false
  summarizing.value = true
  summaryError.value = ''
  summaryStatusText.value = '正在启动摘要任务…'
  if (!force) summaryContent.value = ''

  try {
    const start = await api.startSummaryAsync(caseId.value, 'zh', userStore.userId, force)
    if (start.code !== 200) {
      summaryError.value = mapSummaryError(start.message, start.code)
      summaryStatusText.value = ''
      await refreshCredits()
      return
    }
    const d = start.data || {}
    if (d.status === 'DONE' && d.content) {
      summaryContent.value = d.content
      summaryStatusText.value = ''
      await refreshCredits()
      return
    }
    summaryStatusText.value = '正在生成摘要，请稍候…'

    const deadline = Date.now() + 10 * 60 * 1000
    while (Date.now() < deadline && !pollAbort) {
      await sleep(2000)
      const st = await api.getSummaryAsyncStatus(caseId.value, 'zh')
      if (st.code !== 200) {
        summaryError.value = mapSummaryError(st.message, st.code)
        summaryStatusText.value = ''
        break
      }
      const s = st.data || {}
      summaryStatusText.value = s.status === 'RUNNING' ? '正在生成摘要，请稍候…' : ''
      if (s.status === 'DONE' && s.content) {
        summaryContent.value = s.content
        summaryStatusText.value = ''
        await refreshCredits()
        break
      }
      if (s.status === 'FAILED') {
        summaryError.value = mapSummaryError(s.errorMessage)
        summaryStatusText.value = ''
        break
      }
    }
    if (!summaryContent.value && !summaryError.value && !pollAbort) {
      summaryError.value = '摘要等待超时，请稍后点击「重新生成」'
      summaryStatusText.value = ''
    }
  } catch (e: any) {
    summaryStatusText.value = ''
    summaryError.value = mapSummaryError(e.serverMessage || e.message)
  } finally {
    summarizing.value = false
  }
}

const handleFavorite = async () => {
  if (userStore.isGuest) {
    uni.showToast({ title: '游客用户无法收藏案件，请登录后重试', icon: 'none' })
    return
  }
  if (!caseId.value) return

  const target = !isFavorite.value
  isFavorite.value = target
  try {
    if (target) {
      const res = await api.favoriteCase(caseId.value, userStore.userId)
      if (res.code !== 200) {
        isFavorite.value = !target
        uni.showToast({ title: res.message || '收藏失败', icon: 'none' })
        return
      }
      uni.showToast({ title: '已收藏', icon: 'success' })
    } else {
      const res = await api.cancelFavoriteCase(caseId.value, userStore.userId)
      if (res.code !== 200) {
        isFavorite.value = !target
        uni.showToast({ title: res.message || '取消收藏失败', icon: 'none' })
        return
      }
      uni.showToast({ title: '已取消收藏', icon: 'none' })
    }
  } catch (e: any) {
    isFavorite.value = !target
    uni.showToast({ title: e.serverMessage || '操作失败', icon: 'none' })
  }
}

const sendQa = async () => {
  const q = qaInput.value.trim()
  if (!q || !caseId.value || userStore.isGuest) return
  if (!summaryContent.value) {
    uni.showToast({ title: '请先生成 AI 摘要后再提问', icon: 'none' })
    return
  }
  qaMessages.value.push({ role: 'user', text: q })
  qaInput.value = ''
  qaLoading.value = true
  try {
    const res = await api.postCaseQa(caseId.value, q, 'zh', userStore.userId)
    if (res.code !== 200) {
      qaMessages.value.push({ role: 'assistant', text: res.message || '请求失败' })
    } else {
      qaMessages.value.push({ role: 'assistant', text: res.data || '（无回答）' })
    }
  } catch (e: any) {
    qaMessages.value.push({ role: 'assistant', text: e.serverMessage || '请求失败' })
  } finally {
    qaLoading.value = false
  }
}

const openOriginal = () => {
  const url = caseMeta.value?.original_document_url
  if (!url) {
    uni.showToast({ title: '无原文链接', icon: 'none' })
    return
  }
  uni.navigateTo({ url: `/pages/case/original?url=${encodeURIComponent(url)}` })
}

onLoad(async (query) => {
  if (!query?.id) {
    uni.showToast({ title: '缺少 caseId 参数', icon: 'none' })
    return
  }
  caseId.value = safeDecode(String(query.id))
  uni.showLoading({ title: '加载中...', mask: true })
  await loadMeta(caseId.value)
  uni.hideLoading()
  if (caseMeta.value && !userStore.isGuest) {
    runSummary()
  }
  refreshCredits()
})

onUnload(() => {
  pollAbort = true
  if (pollTimer) clearTimeout(pollTimer)
})
</script>

<style scoped lang="scss">
.detail-container {
  height: 100vh;
  background-color: #F5F7FA;
  display: flex;
  flex-direction: column;
}

.main-scroll {
  flex: 1;
  height: 0;

  .detail-card {
    background-color: #ffffff;
    margin: 24rpx;
    border-radius: 16rpx;
    padding: 30rpx;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.03);
    margin-bottom: 160rpx;
  }
}

.section {
  margin-bottom: 40rpx;
  &:last-child { margin-bottom: 10rpx; }
}

.section-title {
  display: flex;
  align-items: center;
  margin-bottom: 24rpx;

  .accent-line {
    width: 8rpx;
    height: 32rpx;
    background-color: #218CFF;
    border-radius: 4rpx;
    margin-right: 16rpx;
  }
  .title-text {
    font-size: 32rpx;
    font-weight: bold;
    color: #333333;
    flex: 1;
  }
}

.ai-title-row .credits-text {
  font-size: 22rpx;
  color: #909399;
  font-weight: normal;
}

.meta-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20rpx 10rpx;
  padding: 0 10rpx;
  .meta-item { font-size: 26rpx; color: #666666; }
}

.dashed-divider {
  height: 1px;
  border-top: 2rpx dashed #E4E7ED;
  margin: 30rpx 0;
}

.content-text {
  font-size: 28rpx;
  color: #606266;
  line-height: 1.6;
  padding: 0 10rpx;
  display: block;
}

.guest-tip {
  background: #fdf6ec;
  color: #e6a23c;
  padding: 20rpx;
  border-radius: 12rpx;
  font-size: 26rpx;
}

.status-text {
  font-size: 26rpx;
  color: #218CFF;
  margin-bottom: 16rpx;
}

.error-text {
  font-size: 26rpx;
  color: #f56c6c;
  margin-bottom: 16rpx;
  background: #fef0f0;
  padding: 16rpx 20rpx;
  border-radius: 10rpx;
}

.summary-content {
  font-size: 28rpx;
  color: #222;
  line-height: 1.75;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f7f9fc;
  padding: 24rpx;
  border-radius: 12rpx;
  border-left: 6rpx solid #218CFF;
}

.summary-actions {
  margin-top: 24rpx;
  .summary-btn {
    width: 240rpx;
    height: 72rpx;
    line-height: 72rpx;
    background: #218CFF;
    color: #fff;
    font-size: 28rpx;
    border-radius: 36rpx;
    &::after { border: none; }
    &[disabled] { background: #a0cfff; }
  }
}

.qa-messages {
  display: flex;
  flex-direction: column;
  gap: 16rpx;
  margin-bottom: 20rpx;
  max-height: 360rpx;
  overflow-y: auto;

  .qa-bubble {
    display: flex;
    gap: 12rpx;
    font-size: 26rpx;
    line-height: 1.6;
    &.user .qa-text { color: #333; }
    &.assistant .qa-text { color: #606266; white-space: pre-wrap; }
    .qa-role {
      font-weight: 600;
      color: #218CFF;
      flex-shrink: 0;
    }
  }
}

.qa-input-row {
  display: flex;
  align-items: center;
  gap: 16rpx;

  .qa-input {
    flex: 1;
    background: #F5F7FA;
    border-radius: 36rpx;
    height: 72rpx;
    padding: 0 28rpx;
    font-size: 26rpx;
  }
  .qa-send-btn {
    width: 140rpx;
    height: 72rpx;
    line-height: 72rpx;
    background: #218CFF;
    color: #fff;
    font-size: 26rpx;
    border-radius: 36rpx;
    margin: 0;
    &::after { border: none; }
    &[disabled] { background: #a0cfff; color: #fff; }
  }
}

.bottom-action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: 120rpx;
  background-color: #ffffff;
  border-top: 1rpx solid #E4E7ED;
  display: flex;
  align-items: center;
  padding: 0 30rpx;
  padding-bottom: env(safe-area-inset-bottom);
  box-shadow: 0 -4rpx 10rpx rgba(0, 0, 0, 0.03);
  z-index: 99;

  .action-btn {
    flex: 1;
    height: 80rpx;
    display: flex;
    justify-content: center;
    align-items: center;
    border-radius: 40rpx;
    font-size: 30rpx;
    font-weight: 500;
    &::after { border: none; }
  }
  .favorite-btn {
    background-color: #ffffff;
    color: #218CFF;
    border: 2rpx solid #218CFF;
    margin-right: 20rpx;
    &:active { background-color: #F0F8FF; }
  }
  .is-favorited { background-color: #F0F8FF; }
  .original-btn {
    background-color: #218CFF;
    color: #ffffff;
    margin-left: 20rpx;
    &:active { background-color: #1A73D9; }
  }
}
</style>
