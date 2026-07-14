import { ref } from 'vue'
import { defineStore } from 'pinia'

export const useAppStore = defineStore('app', () => {
  const currentPage = ref('home')
  const systemConfig = ref({ systemName: '涉外案例查询与法律知识系统' })

  function initialize() {
    // 预留：初始化系统配置、字典数据等后端请求。
  }

  function setCurrentPage(page: string) {
    currentPage.value = page
  }

  return { currentPage, systemConfig, initialize, setCurrentPage }
})
