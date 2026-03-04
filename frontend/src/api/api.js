import { API_BASE_URL } from './axios'
import { del, downloadBlob, get, post, put, uploadFile } from './request'

export const apiBaseUrl = API_BASE_URL

export function avatarUrl(avatar) {
  if (!avatar) return null
  return `${API_BASE_URL}/avatars/${encodeURIComponent(avatar)}`
}

export function register(payload) {
  return post('/user/register', payload)
}

export function login(payload) {
  return post('/user/login', payload)
}

export function getUserInfo(userId) {
  return get(`/user/${encodeURIComponent(userId)}`)
}

export function updateUserProfile(userId, payload) {
  return put(`/user/${encodeURIComponent(userId)}`, payload)
}

export function uploadAvatar(userId, file) {
  const form = new FormData()
  form.append('file', file)
  form.append('userId', userId)
  return post(`/user/${encodeURIComponent(userId)}/avatar`, form)
}

// ---------- 管理员接口 ----------
export function adminListUsers() {
  return get('/admin/users')
}

export function adminUpdateUser(id, payload) {
  return put(`/admin/users/${encodeURIComponent(id)}`, payload)
}

export function adminDeleteUser(id) {
  return del(`/admin/users/${encodeURIComponent(id)}`)
}

export function adminListJobs() {
  return get('/admin/jobs')
}

export function adminCreateJob(payload) {
  return post('/admin/jobs', payload)
}

export function adminUpdateJob(id, payload) {
  return put(`/admin/jobs/${encodeURIComponent(id)}`, payload)
}

export function adminDeleteJob(id) {
  return del(`/admin/jobs/${encodeURIComponent(id)}`)
}

export function adminListAnalysis() {
  return get('/admin/analysis')
}

export function adminGetAnalysisDetail(analysisId) {
  return get(`/admin/analysis/${encodeURIComponent(analysisId)}`)
}

export function adminGetStats() {
  return get('/admin/stats')
}

export function adminMaintenance() {
  return post('/admin/maintenance')
}

export function uploadResume(file) {
  const userId = localStorage.getItem('userId')
  if (!userId) {
    return Promise.reject(new Error('请先登录'))
  }
  return uploadFile('/resume/upload', file, { data: { userId } })
}

const ANALYSIS_TIMEOUT = 120000

export function startAnalysis(resumeId) {
  return post(`/analysis/${encodeURIComponent(resumeId)}`, null, { timeout: ANALYSIS_TIMEOUT })
}

export function getAnalysisByResumeId(resumeId) {
  return get(`/analysis/resume/${encodeURIComponent(resumeId)}`, { timeout: ANALYSIS_TIMEOUT })
}

const MATCH_TIMEOUT = 180000

export function getMatchByResumeId(resumeId) {
  return get(`/match/resume/${encodeURIComponent(resumeId)}`)
}

export function runMatchForResume(resumeId) {
  return post(`/match/resume/${encodeURIComponent(resumeId)}/run`, null, { timeout: MATCH_TIMEOUT })
}

export function getResumeList(userId) {
  return get(`/resume/list?userId=${encodeURIComponent(userId)}`)
}

export function deleteResume(resumeId, userId) {
  return del(`/resume/${encodeURIComponent(resumeId)}?userId=${encodeURIComponent(userId)}`)
}

export function downloadPdf(id) {
  return downloadBlob(`/pdf/${encodeURIComponent(id)}`)
}

// ---------- 消息（用户端）----------
export function getMessageList(userId) {
  return get(`/message/list?userId=${encodeURIComponent(userId)}`)
}

export function acceptMessage(messageId, userId) {
  return put(`/message/${encodeURIComponent(messageId)}/accept?userId=${encodeURIComponent(userId)}`)
}

export function declineMessage(messageId, userId) {
  return put(`/message/${encodeURIComponent(messageId)}/decline?userId=${encodeURIComponent(userId)}`)
}

// ---------- 消息（管理员端）----------
export function adminSendMessage(senderId, payload) {
  return post(`/admin/messages?senderId=${encodeURIComponent(senderId)}`, payload)
}

export function adminListSentMessages(senderId) {
  return get(`/admin/messages?senderId=${encodeURIComponent(senderId)}`)
}

