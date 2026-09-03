import { ref } from 'vue'
import { defineStore } from 'pinia'

/**
 * 原文地址传递：web-view 页面的 URL 不经过页面 query 传输，
 * 避免 uni-app 在不同平台对参数解码不一致导致双重编码（后端报“仅支持 http/https”）。
 */
export const useOriginalStore = defineStore('original', () => {
  const url = ref('')
  const retry = ref(false)

  function open(u: string, needRetry = false) {
    url.value = u
    retry.value = needRetry
  }

  function clear() {
    url.value = ''
    retry.value = false
  }

  return { url, retry, open, clear }
})
