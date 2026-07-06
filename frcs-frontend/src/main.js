import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import { syncSessionAuthToLocal } from './utils/authStorage'

syncSessionAuthToLocal()
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import "./assets/icons/logo.css"
const app = createApp(App)
app.use(store)
app.use(router)

// Dev server rebuild/restart leaves stale chunk URLs in the browser; reload once fixes ChunkLoadError.
router.onError((err) => {
  const msg = err?.message || ''
  const chunkFailed =
    err?.name === 'ChunkLoadError' ||
    (/Loading chunk/i.test(msg) && /failed/i.test(msg))
  if (!chunkFailed) return
  if (window.__vueChunkReloaded) return
  window.__vueChunkReloaded = true
  window.location.reload()
})

app.use(ElementPlus)
app.mount('#app')
