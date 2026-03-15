<script setup>
import { computed, onMounted, ref } from 'vue'
import {
  adminListUsers,
  adminListJobs,
  adminSendMessage,
  adminListSentMessages,
  adminListInquiries,
  adminReplyToInquiry
} from '../../api/api'

const userId = computed(() => localStorage.getItem('userId'))

const userList = ref([])
const jobList = ref([])
const sentList = ref([])
const inquiryList = ref([])
const loadingUsers = ref(false)
const loadingSent = ref(false)
const loadingInquiries = ref(false)
const sending = ref(false)
const error = ref('')
const success = ref('')

const form = ref({
  receiverId: null,
  title: '',
  content: ''
})

const replyDialog = ref(false)
const replyInquiry = ref(null)
const replyContent = ref('')
const replySending = ref(false)
const replyError = ref('')

const userOptions = computed(() => {
  return userList.value.filter(u => (u.role || '').toLowerCase() !== 'admin')
})

async function fetchUsers() {
  loadingUsers.value = true
  try {
    const data = await adminListUsers()
    userList.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '加载用户列表失败'
  } finally {
    loadingUsers.value = false
  }
}

async function fetchJobs() {
  try {
    const data = await adminListJobs()
    jobList.value = Array.isArray(data) ? data : []
  } catch (_) {
    jobList.value = []
  }
}

async function fetchSent() {
  const id = userId.value
  if (!id) return
  loadingSent.value = true
  try {
    const data = await adminListSentMessages(id)
    sentList.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '加载已发送列表失败'
  } finally {
    loadingSent.value = false
  }
}

async function fetchInquiries() {
  const id = userId.value
  if (!id) return
  loadingInquiries.value = true
  try {
    const data = await adminListInquiries(id)
    inquiryList.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '加载用户咨询失败'
  } finally {
    loadingInquiries.value = false
  }
}

function openReply(m) {
  replyInquiry.value = m
  replyContent.value = ''
  replyError.value = ''
  replyDialog.value = true
}

function closeReply() {
  replyDialog.value = false
  replyInquiry.value = null
}

async function submitReply() {
  const id = userId.value
  if (!id || !replyInquiry.value) return
  replyError.value = ''
  replySending.value = true
  try {
    await adminReplyToInquiry(replyInquiry.value.id, id, replyContent.value)
    closeReply()
    await fetchInquiries()
    success.value = '回复已发送'
    window.dispatchEvent(new CustomEvent('admin-messages-updated'))
  } catch (e) {
    replyError.value = e.message || '回复失败'
  } finally {
    replySending.value = false
  }
}

async function handleSend() {
  const id = userId.value
  if (!id) {
    error.value = '请先登录'
    return
  }
  if (!form.value.receiverId) {
    error.value = '请选择接收用户'
    return
  }
  const title = (form.value.title || '').trim()
  if (!title) {
    error.value = '请输入通知标题'
    return
  }
  success.value = ''
  error.value = ''
  sending.value = true
  try {
    await adminSendMessage(id, {
      receiverId: form.value.receiverId,
      title,
      content: (form.value.content || '').trim()
    })
    success.value = '发送成功'
    form.value.title = ''
    form.value.content = ''
    await fetchSent()
  } catch (e) {
    error.value = e.message || '发送失败'
  } finally {
    sending.value = false
  }
}

function receiverName(receiverId) {
  const u = userList.value.find(x => x.id === receiverId)
  return u ? (u.username || u.phone || u.email || '一位用户') : '一位用户'
}

function senderName(senderId) {
  const u = userList.value.find(x => x.id === senderId)
  return u ? (u.username || u.phone || u.email || '一位用户') : '一位用户'
}

function statusText(s) {
  if (s === 'accepted') return '已接受'
  if (s === 'declined') return '已拒绝'
  return '待处理'
}

function formatTime(t) {
  if (!t) return '--'
  const d = new Date(t)
  return isNaN(d.getTime()) ? t : d.toLocaleString('zh-CN')
}

/** 将内容中的 [岗位ID: xxx] 替换为岗位名称 */
function inquiryContentDisplay(content) {
  if (!content || typeof content !== 'string') return '暂无内容'
  return content.replace(/\[岗位ID:\s*(\d+)\]/g, (_, id) => {
    const job = jobList.value.find(j => j.id === Number(id))
    return job ? `岗位：${job.jobName || '未知岗位'}` : '岗位：未知'
  })
}

onMounted(() => {
  fetchUsers()
  fetchJobs()
  fetchSent()
  fetchInquiries()
})
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <h1 class="admin-page-title">我的消息</h1>
      <p class="admin-page-subtitle">发送面试通知、查看并回复用户咨询</p>
    </div>

    <div v-if="error" class="admin-error">{{ error }}</div>
    <div v-if="success" class="admin-success">{{ success }}</div>

    <div class="admin-card admin-messages-inquiries">
      <h3 class="admin-messages-section-title">用户咨询</h3>
      <div v-if="loadingInquiries" class="admin-loading">加载中…</div>
      <div v-else-if="!inquiryList.length" class="admin-messages-empty">暂无用户咨询</div>
      <div v-else class="inquiry-cards">
        <article v-for="m in inquiryList" :key="m.id" class="inquiry-card">
          <div class="inquiry-card-head">
            <span class="inquiry-card-from">{{ senderName(m.senderId) }}</span>
            <span class="inquiry-card-time">{{ formatTime(m.createTime) }}</span>
          </div>
          <h4 class="inquiry-card-title">{{ m.title }}</h4>
          <p class="inquiry-card-content">{{ inquiryContentDisplay(m.content) }}</p>
          <div class="inquiry-card-footer">
            <span class="inquiry-card-status" :class="m.status === 'replied' ? 'inquiry-card-status--done' : 'inquiry-card-status--pending'">
              {{ m.status === 'replied' ? '已回复' : '待回复' }}
            </span>
            <button
              type="button"
              class="inquiry-card-btn"
              :class="{ 'inquiry-card-btn--done': m.status === 'replied' }"
              :disabled="m.status === 'replied'"
              @click="openReply(m)"
            >
              {{ m.status === 'replied' ? '已回复' : '回复' }}
            </button>
          </div>
        </article>
      </div>
    </div>

    <div class="admin-card admin-messages-send">
      <h3 class="admin-messages-section-title">发送面试通知</h3>
      <div class="admin-messages-form">
        <div class="form-row">
          <label class="form-label">接收用户</label>
          <select
            v-model="form.receiverId"
            class="form-input admin-messages-select"
            :disabled="loadingUsers"
          >
            <option :value="null">请选择用户</option>
            <option
              v-for="u in userOptions"
              :key="u.id"
              :value="u.id"
            >
              {{ u.username || u.phone || u.email }} ({{ u.phone || u.email }})
            </option>
          </select>
        </div>
        <div class="form-row">
          <label class="form-label">通知标题</label>
          <input
            v-model="form.title"
            class="form-input"
            type="text"
            placeholder="例如：XX 公司面试邀请"
          />
        </div>
        <div class="form-row">
          <label class="form-label">通知内容（选填）</label>
          <textarea
            v-model="form.content"
            class="form-input admin-messages-textarea"
            rows="4"
            placeholder="面试时间、地点、要求等"
          />
        </div>
        <div class="admin-messages-form-actions">
          <button
            type="button"
            class="primary-btn"
            :disabled="sending || loadingUsers"
            @click="handleSend"
          >
            {{ sending ? '发送中…' : '发送通知' }}
          </button>
        </div>
      </div>
    </div>

    <div class="admin-card admin-messages-sent">
      <h3 class="admin-messages-section-title">已发送通知</h3>
      <div v-if="loadingSent" class="admin-loading">加载中…</div>
      <div v-else-if="!sentList.length" class="admin-messages-empty">暂无已发送通知</div>
      <div v-else class="sent-cards">
        <article v-for="m in sentList" :key="m.id" class="sent-card">
          <div class="sent-card-head">
            <span class="sent-card-to">{{ receiverName(m.receiverId) }}</span>
            <span class="sent-card-time">{{ formatTime(m.createTime) }}</span>
          </div>
          <h4 class="sent-card-title">{{ m.title }}</h4>
          <span class="sent-card-status" :class="'sent-card-status--' + (m.status || 'pending')">
            {{ statusText(m.status) }}
          </span>
        </article>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="replyDialog"
        class="reply-modal-mask"
        @click.self="closeReply"
      >
        <div class="reply-modal-box" @click.stop>
          <h3 class="reply-modal-title">回复用户咨询</h3>
          <p v-if="replyInquiry" class="reply-modal-desc">{{ replyInquiry.title }}</p>
          <p v-if="replyInquiry?.content" class="reply-modal-content">{{ inquiryContentDisplay(replyInquiry.content) }}</p>
          <div class="form-row">
            <label class="form-label">回复内容</label>
            <textarea
              v-model="replyContent"
              class="form-input admin-messages-textarea"
              rows="4"
              placeholder="请输入回复内容"
            />
          </div>
          <p v-if="replyError" class="admin-error-inline">{{ replyError }}</p>
          <div class="reply-modal-actions">
            <button type="button" class="admin-btn" @click="closeReply">取消</button>
            <button type="button" class="admin-btn admin-btn--primary" :disabled="replySending" @click="submitReply">
              {{ replySending ? '发送中…' : '发送回复' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.admin-messages-inquiries { margin-bottom: 24px; }
.inquiry-cards { display: flex; flex-direction: column; gap: 16px; }
.inquiry-card {
  padding: 20px; border-radius: 16px; border: 1px solid #e2e8f0; background: #fafbfc;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.inquiry-card:hover { border-color: #c7d2fe; box-shadow: 0 4px 12px rgba(79, 70, 229, 0.08); }
.inquiry-card-head { display: flex; align-items: center; justify-content: space-between; gap: 12px; margin-bottom: 10px; }
.inquiry-card-from { font-size: 14px; font-weight: 600; color: #334155; }
.inquiry-card-time { font-size: 12px; color: #94a3b8; }
.inquiry-card-title { font-size: 16px; font-weight: 600; color: #1e293b; margin: 0 0 8px; }
.inquiry-card-content {
  font-size: 14px; color: #475569; line-height: 1.5; margin: 0 0 14px;
  display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden;
}
.inquiry-card-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding-top: 14px; border-top: 1px solid #e2e8f0; }
.inquiry-card-status { font-size: 12px; font-weight: 500; }
.inquiry-card-status--pending { color: #d97706; }
.inquiry-card-status--done { color: #059669; }
.inquiry-card-btn {
  padding: 6px 16px; border-radius: 10px; font-size: 13px; font-weight: 500; cursor: pointer;
  border: 1px solid #6366f1; background: #6366f1; color: #fff;
}
.inquiry-card-btn:hover:not(:disabled) { background: #4f46e5; border-color: #4f46e5; }
.inquiry-card-btn:disabled { opacity: 0.7; cursor: default; }
.inquiry-card-btn--done { background: #e2e8f0; border-color: #cbd5e1; color: #64748b; }
.sent-cards { display: flex; flex-direction: column; gap: 12px; }
.sent-card {
  padding: 16px 20px; border-radius: 14px; border: 1px solid #e2e8f0; background: #fff;
  display: flex; align-items: center; gap: 16px; flex-wrap: wrap;
}
.sent-card-head { display: flex; align-items: center; justify-content: space-between; gap: 8px; flex: 1; min-width: 0; }
.sent-card-to { font-weight: 600; color: #334155; }
.sent-card-time { font-size: 12px; color: #94a3b8; }
.sent-card-title { font-size: 15px; font-weight: 500; color: #1e293b; margin: 0; flex: 1 1 200px; }
.sent-card-status { font-size: 12px; font-weight: 500; flex-shrink: 0; }
.sent-card-status--pending { color: #d97706; }
.sent-card-status--accepted { color: #059669; }
.sent-card-status--declined { color: #64748b; }
.reply-modal-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 9999;
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.reply-modal-box {
  background: #fff; border-radius: 20px; padding: 24px; max-width: 480px; width: 100%;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
}
.reply-modal-title { font-size: 18px; font-weight: 600; margin: 0 0 8px; color: #1e293b; }
.reply-modal-desc { font-size: 14px; color: #64748b; margin: 0 0 8px; }
.reply-modal-content { font-size: 14px; color: #475569; margin: 0 0 16px; padding: 12px; background: #f8fafc; border-radius: 10px; white-space: pre-wrap; }
.reply-modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; }
.admin-error-inline { color: #dc2626; font-size: 13px; margin-top: 8px; }
.admin-messages-send {
  margin-bottom: 24px;
}

.admin-messages-section-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px;
}

.admin-messages-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.admin-messages-select {
  cursor: pointer;
}

.admin-messages-textarea {
  resize: vertical;
  min-height: 80px;
}

.admin-messages-form-actions {
  margin-top: 8px;
}

.admin-messages-empty {
  padding: 32px;
  text-align: center;
  color: #64748b;
}

.admin-messages-status {
  font-size: 13px;
}

.admin-messages-status--pending {
  color: #d97706;
}

.admin-messages-status--accepted {
  color: #059669;
}

.admin-messages-status--declined {
  color: #64748b;
}

.admin-success {
  padding: 12px 16px;
  color: #059669;
  background: #ecfdf5;
  border-radius: 12px;
  margin-bottom: 16px;
}
</style>
