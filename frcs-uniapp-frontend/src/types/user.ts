export interface User {
  id: number
  username: string
  email: string
  avatar: string
}

export interface LoginPayload {
  email: string
  password?: string
  captcha?: string
}
