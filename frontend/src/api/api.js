import { API_BASE_URL } from './axios'
import { del, downloadBlob, get, post, put, uploadFile } from './request'

export const apiBaseUrl = API_BASE_URL

export function avatarUrl(avatar) {
  if (!avatar) return null
  return `${API_BASE_URL}/avatars/${encodeURIComponent(avatar)}`
}

/** 当前登录用户头像在 localStorage 的 key（按用户隔离，避免管理员/用户互相覆盖） */
export function avatarStorageKey(userId) {
  return userId ? `avatar_${userId}` : null
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

/** 岗位简要列表（id, jobName），用于消息页展示岗位名称 */
export function getJobSummaryList() {
  return get('/jobs/summary')
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

/** 分页查询岗位，返回 { list, total } */
export function adminListJobsPage(page = 1, size = 10) {
  return get(`/admin/jobs/page?page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`)
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

/** 分页查询简历分析结果，返回 { list, total } */
export function adminListAnalysisPage(page = 1, size = 10) {
  return get(`/admin/analysis/page?page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`)
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

/** 一键匹配（后端异步执行，约 5 秒内返回） */
export function runMatchForResume(resumeId) {
  return post(`/match/resume/${encodeURIComponent(resumeId)}/run`, null, { timeout: 15000 })
}

export function getResumeList(userId) {
  return get(`/resume/list?userId=${encodeURIComponent(userId)}`)
}

/** 分页查询简历列表，返回 { list, total } */
export function getResumeListPage(userId, page = 1, size = 10) {
  return get(`/resume/list/page?userId=${encodeURIComponent(userId)}&page=${encodeURIComponent(page)}&size=${encodeURIComponent(size)}`)
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

/** 未读消息数（Redis 缓存） */
export function getUnreadCount(userId) {
  return get(`/message/unread-count?userId=${encodeURIComponent(userId)}`)
}

/** 进入消息页时调用，清零未读 */
export function markMessagesRead(userId) {
  return post(`/message/read?userId=${encodeURIComponent(userId)}`)
}

export function getMyInquiries(userId) {
  return get(`/message/my-inquiries?userId=${encodeURIComponent(userId)}`)
}

export function sendInquiry(userId, payload) {
  return post(`/message/inquiry?userId=${encodeURIComponent(userId)}`, payload)
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

export function adminListInquiries(adminId) {
  return get(`/admin/messages/inquiries?adminId=${encodeURIComponent(adminId)}`)
}

/** 管理员未回复咨询数量（小红点） */
export function adminGetInquiryUnreadCount(adminId) {
  return get(`/admin/messages/inquiries/unread-count?adminId=${encodeURIComponent(adminId)}`)
}

export function adminReplyToInquiry(inquiryId, adminId, content) {
  return post(`/admin/messages/inquiry/${encodeURIComponent(inquiryId)}/reply?adminId=${encodeURIComponent(adminId)}`, { content })
}

// ---------- 知识库管理 ----------
/** 知识库文档列表（当前用户） */
export function getKnowledgeDocList(userId) {
  return get(`/knowledge/list?userId=${encodeURIComponent(userId)}`)
}

/** 上传知识库文档（PDF、DOCX、Markdown 等），异步分块与向量化 */
export function uploadKnowledgeDoc(userId, file) {
  return uploadFile('/knowledge/upload', file, { data: { userId: Number(userId) || userId } })
}

/** 手动触发一次索引构建（方案 B） */
export function triggerKnowledgeIndex(userId) {
  return post(`/knowledge/trigger-index?userId=${encodeURIComponent(userId)}`)
}

/** 删除知识库文档 */
export function deleteKnowledgeDoc(docId, userId) {
  return del(`/knowledge/${encodeURIComponent(docId)}?userId=${encodeURIComponent(userId)}`)
}

/** 知识库统计（文档数、分块数等） */
export function getKnowledgeStats(userId) {
  return get(`/knowledge/stats?userId=${encodeURIComponent(userId)}`)
}

/** 基于知识库的流式问答（SSE），POST 问题，返回 ReadableStream；onChunk(text), onSources(snippets[]), onDone(), onError(err) */
export function qaStream(userId, question, { onChunk, onSources, onDone, onError }) {
  const token = localStorage.getItem('token')
  const url = `${API_BASE_URL}/qa/stream`
  fetch(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: token ? `Bearer ${token}` : ''
    },
    body: JSON.stringify({ userId: Number(userId) || userId, question })
  })
    .then(async res => {
      if (!res.ok) throw new Error(res.statusText || '请求失败')
      const reader = res.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''
      while (true) {
        const { done, value } = await reader.read()
        if (done) {
          if (onDone) onDone()
          return
        }
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''
        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const data = line.slice(6).trim()
            if (data === '[DONE]') continue
            try {
              const parsed = JSON.parse(data)
              if (parsed.type === 'sources' && onSources) onSources({
                snippets: parsed.snippets || [],
                chunkCount: parsed.chunkCount ?? 0,
                uploadedDocCount: parsed.uploadedDocCount ?? 0,
                indexingTriggered: parsed.indexingTriggered ?? false
              })
              else if (parsed.content && onChunk) onChunk(parsed.content)
            } catch (_) {}
          }
        }
      }
    })
    .catch(err => {
      if (onError) onError(err)
    })
}

