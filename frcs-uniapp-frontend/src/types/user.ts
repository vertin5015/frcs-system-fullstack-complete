export interface User {
  id: number
  username: string
  email?: string
  avatar?: string
  summaryCredits?: number
}

export interface LoginPayload {
  email: string
  password?: string
  captcha?: string
}

/** 后端 /api/login 返回（LoginResVO） */
export interface LoginResult {
  userId: number
  username: string
  summaryCredits?: number
}

/** 支付套餐 */
export interface PaymentPackage {
  id: string
  credits: number
  priceCents: number
  currency: string
  label: string
}

/** 创建订单返回 */
export interface CreateOrderResult {
  orderNo: string
  mockPay: boolean
  stripeCheckoutUrl?: string
}
