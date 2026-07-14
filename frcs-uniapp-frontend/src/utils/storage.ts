export const storage = {
  get<T>(key: string): T | null {
    return uni.getStorageSync(key) || null
  },
  set<T>(key: string, value: T): void {
    uni.setStorageSync(key, value)
  },
  remove(key: string): void {
    uni.removeStorageSync(key)
  },
}
