import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 首页 → 检索页（tabBar 页面）之间的搜索参数传递。
 * 微信 switchTab 无法携带 query，因此先写入共享状态，再由检索页 onShow 取走执行。
 * 与网页端 Vuex searchParams 的作用一致。
 */
export interface SearchPending {
  keyword: string
  country?: string
  period?: number | ''
}

export const useSearchStore = defineStore('search', () => {
  const pending = ref<SearchPending | null>(null)

  function setPending(p: SearchPending) {
    pending.value = { ...p }
  }

  function takePending(): SearchPending | null {
    const p = pending.value
    pending.value = null
    return p
  }

  return { pending, setPending, takePending }
})
