import { API_BASE_URL } from './config'

/** 后端统一返回体 JSONReturnBean */
export interface ApiResult<T = unknown> {
  code: number
  message?: string
  data: T
}

export interface RequestOptions {
  method?: 'GET' | 'POST' | 'DELETE' | 'PUT'
  params?: Record<string, unknown>
  data?: Record<string, unknown> | string | ArrayBuffer
  timeout?: number
}

function buildQuery(params?: Record<string, unknown>): string {
  if (!params) return ''
  const parts: string[] = []
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return
    parts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
  })
  return parts.length ? `?${parts.join('&')}` : ''
}

/**
 * uni.request 封装：直接 resolve 后端 JSONReturnBean（{ code, message, data }），
 * HTTP 非 2xx 或网络异常时 reject，并把后端 message 挂到 error.serverMessage。
 */
export function request<T = unknown>(path: string, options: RequestOptions = {}): Promise<ApiResult<T>> {
  const { method = 'GET', params, data, timeout = 30000 } = options
  const url = `${API_BASE_URL}${path.startsWith('/') ? path : `/${path}`}${buildQuery(params)}`

  return new Promise((resolve, reject) => {
    uni.request({
      url,
      method,
      data,
      timeout,
      header: { 'Content-Type': 'application/json' },
      success: (res) => {
        const body = res.data as ApiResult<T> | undefined
        if (res.statusCode >= 200 && res.statusCode < 300) {
          resolve(body || ({ code: 200, data: undefined } as ApiResult<T>))
          return
        }
        const err = new Error(body?.message || `请求失败（${res.statusCode}）`)
        ;(err as any).serverMessage = body?.message
        ;(err as any).statusCode = res.statusCode
        reject(err)
      },
      fail: (err) => {
        const e = new Error(err.errMsg || '网络异常，请稍后重试')
        ;(e as any).serverMessage = e.message
        reject(e)
      },
    })
  })
}

export const httpGet = <T = unknown>(path: string, params?: Record<string, unknown>, timeout?: number) =>
  request<T>(path, { method: 'GET', params, timeout })

export const httpPost = <T = unknown>(path: string, data?: Record<string, unknown> | string | ArrayBuffer, timeout?: number) =>
  request<T>(path, { method: 'POST', data, timeout })

export const httpDelete = <T = unknown>(path: string, params?: Record<string, unknown>, timeout?: number) =>
  request<T>(path, { method: 'DELETE', params, timeout })
