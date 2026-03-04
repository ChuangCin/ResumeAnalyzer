import axios from 'axios'

export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

export const http = axios.create({
  baseURL: API_BASE_URL,
  timeout: 20000
})

http.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = config.headers || {}
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

http.interceptors.response.use(
  res => {
    const payload = res?.data
    if (payload && typeof payload === 'object' && 'code' in payload) {
      if (payload.code !== 200) {
        const err = new Error(payload.message || '请求失败')
        err.code = payload.code
        err.payload = payload
        throw err
      }
      return payload.data
    }
    return payload
  },
  error => {
    const message =
      error?.response?.data?.message ||
      error?.response?.data?.error ||
      error?.message ||
      '网络错误'
    const err = new Error(message)
    err.cause = error
    throw err
  }
)

