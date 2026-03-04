import { http } from './axios'

export function get(url, config) {
  return http.get(url, config)
}

export function post(url, data, config) {
  return http.post(url, data, config)
}

export function put(url, data, config) {
  return http.put(url, data, config)
}

export function del(url, config) {
  return http.delete(url, config)
}

export function uploadFile(url, file, { fieldName = 'file', data = {}, config = {} } = {}) {
  const form = new FormData()
  form.append(fieldName, file)
  Object.entries(data).forEach(([k, v]) => form.append(k, v))
  return http.post(url, form, {
    ...config,
    headers: {
      ...(config.headers || {}),
      'Content-Type': 'multipart/form-data'
    }
  })
}

export function downloadBlob(url, config) {
  return http.get(url, { ...config, responseType: 'blob' })
}

