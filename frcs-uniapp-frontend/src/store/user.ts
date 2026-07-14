import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import type { User } from '../types/user'
import { storage } from '../utils/storage'

const TOKEN_KEY = 'frcs_token'
const USER_KEY = 'frcs_user'

export const useUserStore = defineStore('user', () => {
  const token = ref(storage.get<string>(TOKEN_KEY) || '')
  const userInfo = ref<User | null>(storage.get<User>(USER_KEY))
  const isLoggedIn = computed(() => Boolean(token.value))

  function setLogin(sessionToken: string, user: User) {
    token.value = sessionToken
    userInfo.value = user
    storage.set(TOKEN_KEY, sessionToken)
    storage.set(USER_KEY, user)
  }

  function logout() {
    token.value = ''
    userInfo.value = null
    storage.remove(TOKEN_KEY)
    storage.remove(USER_KEY)
  }

  return { token, userInfo, isLoggedIn, setLogin, logout }
})
