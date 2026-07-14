<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'
import { useAppStore } from './store/app'

onLaunch(() => {
  useAppStore().initialize()
})
</script>

<style lang="scss">
page {
  background: #f7f8fa;
  color: #1f2937;
}
</style>
