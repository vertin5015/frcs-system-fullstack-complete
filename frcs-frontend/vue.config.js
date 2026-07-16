const { defineConfig } = require('@vue/cli-service')
module.exports = defineConfig({
  transpileDependencies: true,
  parallel: false,
  productionSourceMap: false,
  // 开发时把 /api 代理到后端，避免 8080→8122 跨域导致注册/登录 POST 失败
  devServer: {
    port: 8080,
    proxy: {
      '/api': {
        target: 'http://localhost:8122',
        changeOrigin: true,
      },
    },
  },
})
