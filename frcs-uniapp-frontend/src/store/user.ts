import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { LoginResult, User } from '../types/user'
import { storage } from '../utils/storage'

const TOKEN_KEY = 'frcs_token'
const USER_KEY = 'frcs_user'
const USER_ID_KEY = 'frcs_userId'
const USERNAME_KEY = 'frcs_username'
const EMAIL_KEY = 'frcs_userEmail'
const CREDITS_KEY = 'frcs_summaryCredits'

export const useUserStore = defineStore('user', () => {
  const token = ref(storage.get<string>(TOKEN_KEY) || '')
  const userInfo = ref<User | null>(storage.get<User>(USER_KEY))
  const isLoggedIn = computed(() => token.value === 'true')
  const userId = computed(() => Number(storage.get<string>(USER_ID_KEY) || userInfo.value?.id || 0))
  const isGuest = computed(() => userId.value === 0)

  function setLogin(result: LoginResult, email?: string) {
    token.value = 'true'
    storage.set(TOKEN_KEY, 'true')
    storage.set(USER_ID_KEY, String(result.userId))
    storage.set(USERNAME_KEY, result.username)
    if (email) storage.set(EMAIL_KEY, email)
    if (result.summaryCredits != null && result.summaryCredits !== undefined) {
      storage.set(CREDITS_KEY, String(result.summaryCredits))
    } else {
      storage.remove(CREDITS_KEY)
    }
    userInfo.value = {
      id: result.userId,
      username: result.username,
      email,
      summaryCredits: result.summaryCredits,
    }
    storage.set(USER_KEY, userInfo.value)
  }

  /** 游客模式：与网页端一致，仅本地登录态（userId=0），不调用后端 */
  function guestLogin() {
    token.value = 'true'
    storage.set(TOKEN_KEY, 'true')
    storage.set(USER_ID_KEY, '0')
    storage.set(USERNAME_KEY, '游客')
    storage.remove(EMAIL_KEY)
    storage.remove(CREDITS_KEY)
    userInfo.value = { id: 0, username: '游客' }
    storage.set(USER_KEY, userInfo.value)
  }

  function getCredits(): number | null {
    const v = storage.get<string>(CREDITS_KEY)
    return v == null || v === '' ? null : Number(v)
  }

  function updateCredits(credits: number | null) {
    if (credits == null) return
    storage.set(CREDITS_KEY, String(credits))
    if (userInfo.value) {
      userInfo.value.summaryCredits = credits
      storage.set(USER_KEY, userInfo.value)
    }
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    ;[TOKEN_KEY, USER_KEY, USER_ID_KEY, USERNAME_KEY, EMAIL_KEY, CREDITS_KEY].forEach((k) => storage.remove(k))
  }

  return {
    token,
    userInfo,
    isLoggedIn,
    isGuest,
    userId,
    getCredits,
    updateCredits,
    setLogin,
    guestLogin,
    logout,
  }
})
