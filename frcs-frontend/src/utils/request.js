// utils/request.js
import axios from 'axios';
import { base } from '../api/path'; // 引入配置好的基础路径

// 创建 axios 实例
const instance = axios.create({
  baseURL: base.baseUrl, // 基础路径
  timeout: 1800000, // 摘要生成可能较久；轮询接口单独可更短
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器 - 移除了 token 相关逻辑
instance.interceptors.request.use(
  config => config,
  error => Promise.reject(error)
);

// 响应拦截器 - 移除了基于 token 的错误处理逻辑
instance.interceptors.response.use(
  response => response.data, // 只返回 data 部分，简化使用
  error => {
    if (error.response) {
      console.error('请求失败', error.response.data);
      // 把后端 JSON 挂到 error 上，便于注册/登录等展示 message
      const d = error.response.data;
      if (d && typeof d === 'object' && d.message) {
        error.serverMessage = d.message;
      }
    } else {
      console.error('请求失败', error.message);
    }
    return Promise.reject(error);
  }
);

export default instance;