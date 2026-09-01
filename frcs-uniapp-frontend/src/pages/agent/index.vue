<template>
  <view class="agent-container">
    <view class="top-bar">
      <view class="filters">
        <view class="filter-btn" @tap="openCountrySelect">
          <text class="filter-text">{{ countryName(selectedCountry) }}</text>
          <text class="arrow">▾</text>
        </view>
        <view class="filter-btn" @tap="openSourceSelect">
          <text class="filter-text">{{ selectedSource === '' ? '全部数据源' : selectedSource }}</text>
          <text class="arrow">▾</text>
        </view>
        <view class="filter-btn" @tap="toggleRefresh">
          <text class="filter-text">{{ refreshCases ? '先检索案例' : '仅知识库' }}</text>
          <text class="arrow">{{ refreshCases ? '✓' : '' }}</text>
        </view>
      </view>
      <view class="credits" v-if="credits !== null">摘要剩余 {{ credits }} 次</view>
    </view>

    <scroll-view scroll-y class="chat-scroll" :scroll-into-view="scrollInto" scroll-with-animation>
      <view class="chat-list">
        <view v-if="messages.length === 0" class="welcome-card">
          <text class="welcome-title">涉外法律 RAG 助手</text>
          <text class="welcome-sub">可基于案例检索与本地知识库回答涉外法律问题，例如：</text>
          <view
            class="welcome-tag"
            v-for="(q, i) in quickQuestions"
            :key="i"
            @tap="send(q)"
          >{{ q }}</view>
        </view>

        <view
          v-for="(m, i) in messages"
          :key="i"
          class="msg-row"
          :class="m.role"
        >
          <view class="bubble">
            <view v-if="m.role === 'assistant' && m.route" class="route-tag">{{ m.route }}</view>
            <text class="msg-text">{{ m.text }}</text>

            <block v-if="m.role === 'assistant' && m.relatedCases && m.relatedCases.length">
              <view class="sub-title">相关案例</view>
              <view
                class="related-item"
                v-for="c in m.relatedCases.slice(0, 5)"
                :key="c.case_id"
                @tap="goCase(c.case_id)"
              >
                <text class="related-name">{{ c.case_name }}</text>
                <text class="related-meta">{{ countryName(c.country) }} · {{ c.judgement_date || '-' }}</text>
              </view>
            </block>

            <block v-if="m.role === 'assistant' && m.kbHits && m.kbHits.length">
              <view class="sub-title">知识库命中</view>
              <view
                class="related-item"
                v-for="h in m.kbHits.slice(0, 5)"
                :key="h.chunkId"
                @tap="goLaw(h)"
              >
                <text class="related-name">{{ h.title }}</text>
                <text class="related-meta">{{ h.sourceId }} · {{ h.preview ? h.preview.slice(0, 40) + '…' : '' }}</text>
              </view>
            </block>

            <block v-if="m.role === 'assistant' && m.trace && m.trace.length">
              <view class="sub-title" @tap="m.traceExpanded = !m.traceExpanded">
                执行轨迹 {{ m.traceExpanded ? '▲' : '▼' }}
              </view>
              <view v-if="m.traceExpanded" class="trace-list">
                <view v-for="(t, ti) in m.trace" :key="ti" class="trace-item">
                  <text class="trace-name">{{ t.name }}</text>
                  <text class="trace-status">{{ t.status }}</text>
                  <text class="trace-detail">{{ t.detail }}</text>
                </view>
              </view>
            </block>
          </view>
        </view>

        <view v-if="loading" class="msg-row assistant">
          <view class="bubble">
            <text class="msg-text">正在检索并生成回答…</text>
          </view>
        </view>
        <view id="chat-bottom" style="height: 20rpx"></view>
      </view>
    </scroll-view>

    <view class="input-bar">
      <input
        class="chat-input"
        v-model="input"
        placeholder="输入法律问题…"
        placeholder-class="placeholder-style"
        confirm-type="send"
        :disabled="loading"
        @confirm="send()"
      />
      <button class="send-btn" :disabled="loading || !input.trim()" @tap="send()">发送</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { nextTick, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import api from '../../api'
import { useUserStore } from '../../store/user'
import type { AgentResult, KbHit } from '../../types/case'

interface ChatMessage {
  role: 'user' | 'assistant'
  text: string
  route?: string
  relatedCases?: AgentResult['relatedCases']
  kbHits?: KbHit[]
  trace?: AgentResult['trace']
  traceExpanded?: boolean
}

const userStore = useUserStore()
const messages = ref<ChatMessage[]>([])
const input = ref('')
const loading = ref(false)
const scrollInto = ref('')
const credits = ref<number | null>(userStore.getCredits())

const selectedCountry = ref('')
const selectedSource = ref('')
const refreshCases = ref(true)

const quickQuestions = [
  '美国外观设计专利侵权的赔偿规则',
  'GDPR 对跨境数据传输有哪些要求',
  '欧盟反垄断中滥用市场支配地位的认定',
  '日本法院如何处理商标不正当竞争',
]

const countryName = (code?: string) => {
  if (code === 'US') return '美国'
  if (code === 'EU') return '欧盟'
  if (code === 'JPN') return '日本'
  return '全部国家'
}

const scrollToBottom = () => {
  nextTick(() => {
    scrollInto.value = 'chat-bottom'
  })
}

const refreshCredits = async () => {
  if (userStore.isGuest) return
  try {
    const res = await api.getUserSummaryCredits(userStore.userId)
    if (res.code === 200 && res.data != null) {
      credits.value = res.data
      userStore.updateCredits(res.data)
    }
  } catch {
    /* ignore */
  }
}

const send = async (quick?: string) => {
  const q = (quick || input.value).trim()
  if (!q || loading.value) return
  if (userStore.isGuest) {
    uni.showToast({ title: '游客模式暂不支持 AI 问答，请登录后使用', icon: 'none' })
    return
  }

  input.value = ''
  messages.value.push({ role: 'user', text: q })
  loading.value = true
  scrollToBottom()
  try {
    const res = await api.agentAsk({
      userId: userStore.userId,
      question: q,
      language: 'zh',
      country: selectedCountry.value || undefined,
      sources: selectedSource.value || 'US,EU,JPN',
      period: null,
      topK: 5,
      refreshCases: refreshCases.value,
    })
    if (res.code !== 200) {
      messages.value.push({
        role: 'assistant',
        text: res.message || '请求失败，请稍后重试',
      })
      return
    }
    const d = res.data || {}
    messages.value.push({
      role: 'assistant',
      text: d.answer || '（无回答）',
      route: d.route,
      relatedCases: d.relatedCases || [],
      kbHits: d.kbHits || [],
      trace: d.trace || [],
      traceExpanded: false,
    })
    refreshCredits()
  } catch (e: any) {
    messages.value.push({
      role: 'assistant',
      text: e.serverMessage || '网络异常，请稍后重试',
    })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

const goCase = (caseId: string) => {
  uni.navigateTo({ url: `/pages/case/detail?id=${encodeURIComponent(caseId)}` })
}

const goLaw = (hit: KbHit) => {
  uni.navigateTo({
    url: `/pages/law/detail?sourceId=${encodeURIComponent(hit.sourceId || '')}&title=${encodeURIComponent(hit.title || '')}&preview=${encodeURIComponent(hit.preview || '')}&score=${hit.score ?? ''}`
  })
}

const openCountrySelect = () => {
  const options = ['全部国家', '美国', '欧盟', '日本']
  const map: Record<string, string> = { '全部国家': '', '美国': 'US', '欧盟': 'EU', '日本': 'JPN' }
  uni.showActionSheet({
    itemList: options,
    success: (res) => {
      selectedCountry.value = map[options[res.tapIndex]]
    },
  })
}

const openSourceSelect = () => {
  const options = ['全部数据源', 'US', 'EU', 'JPN']
  uni.showActionSheet({
    itemList: options,
    success: (res) => {
      selectedSource.value = options[res.tapIndex] === '全部数据源' ? '' : options[res.tapIndex]
    },
  })
}

const toggleRefresh = () => {
  refreshCases.value = !refreshCases.value
}

onLoad(() => {
  refreshCredits()
})
</script>

<style scoped lang="scss">
.agent-container {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
}

.top-bar {
  background: #fff;
  padding: 16rpx 24rpx;
  border-bottom: 1rpx solid #e4e7ed;

  .filters { display: flex; }
  .filter-btn + .filter-btn { margin-left: 16rpx; }
  .filter-btn {
    display: flex;
    align-items: center;
    background: #f5f7fa;
    border-radius: 30rpx;
    padding: 12rpx 22rpx;
    font-size: 24rpx;
    color: #606266;
    .filter-text { margin-right: 8rpx; }
    .arrow { color: #999; font-size: 20rpx; }
  }
  .credits {
    margin-top: 12rpx;
    font-size: 22rpx;
    color: #218cff;
  }
}

.chat-scroll { flex: 1; height: 0; }
.chat-list { padding: 24rpx; padding-bottom: 40rpx; }

.welcome-card {
  background: #fff;
  border-radius: 20rpx;
  padding: 32rpx;
  margin-bottom: 24rpx;

  .welcome-title { display: block; font-size: 34rpx; font-weight: 600; color: #333; margin-bottom: 12rpx; }
  .welcome-sub { display: block; font-size: 26rpx; color: #666; margin-bottom: 20rpx; }
  .welcome-tag {
    display: inline-block;
    background: #ebf4ff;
    color: #218cff;
    font-size: 24rpx;
    padding: 10rpx 22rpx;
    border-radius: 28rpx;
    margin: 0 12rpx 12rpx 0;
    &:active { opacity: 0.8; }
  }
}

.msg-row {
  display: flex;
  margin-bottom: 24rpx;
  &.user { justify-content: flex-end; }
  &.assistant { justify-content: flex-start; }

  .bubble {
    max-width: 88%;
    background: #fff;
    border-radius: 20rpx;
    padding: 24rpx;
    box-shadow: 0 2rpx 10rpx rgba(0, 0, 0, 0.03);
    &.user {
      background: #218cff;
    }
  }

  &.user .msg-text { color: #fff; }
  .msg-text {
    font-size: 28rpx;
    color: #333;
    line-height: 1.65;
    white-space: pre-wrap;
    word-break: break-word;
  }

  .route-tag {
    display: inline-block;
    background: #f0f9eb;
    color: #67c23a;
    font-size: 22rpx;
    padding: 4rpx 16rpx;
    border-radius: 20rpx;
    margin-bottom: 12rpx;
  }

  .sub-title {
    font-size: 26rpx;
    font-weight: 600;
    color: #333;
    margin: 20rpx 0 12rpx;
  }

  .related-item {
    background: #f7f9fc;
    border-radius: 12rpx;
    padding: 16rpx 20rpx;
    margin-bottom: 12rpx;
    display: flex;
    flex-direction: column;
    .related-name { font-size: 26rpx; color: #218cff; margin-bottom: 6rpx; }
    .related-meta { font-size: 22rpx; color: #999; }
  }

  .trace-list {
    background: #fafbfc;
    border-radius: 12rpx;
    padding: 16rpx;
    .trace-item {
      display: flex;
      flex-direction: column;
      padding: 8rpx 0;
      border-bottom: 1rpx solid #f0f2f5;
      &:last-child { border-bottom: none; }
      .trace-name { font-size: 24rpx; font-weight: 600; color: #333; }
      .trace-status { font-size: 22rpx; color: #218cff; }
      .trace-detail { font-size: 22rpx; color: #999; }
    }
  }
}

.input-bar {
  display: flex;
  align-items: center;
  padding: 20rpx 24rpx;
  padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
  background: #fff;
  border-top: 1rpx solid #e4e7ed;

  .chat-input {
    flex: 1;
    margin-right: 16rpx;
    background: #f0f2f5;
    border-radius: 36rpx;
    height: 76rpx;
    padding: 0 28rpx;
    font-size: 28rpx;
  }
  .placeholder-style { color: #999; }
  .send-btn {
    margin: 0;
    width: 150rpx;
    height: 76rpx;
    line-height: 76rpx;
    background: #218cff;
    color: #fff;
    font-size: 28rpx;
    border-radius: 38rpx;
    &::after { border: none; }
    &[disabled] { background: #a0cfff; }
  }
}
</style>
