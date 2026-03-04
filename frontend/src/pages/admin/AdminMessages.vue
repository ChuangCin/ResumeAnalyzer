<script setup>
import { computed, onMounted, ref } from 'vue'
import { adminListUsers, adminSendMessage, adminListSentMessages } from '../../api/api'

const userId = computed(() => localStorage.getItem('userId'))

const userList = ref([])
const sentList = ref([])
const loadingUsers = ref(false)
const loadingSent = ref(false)
const sending = ref(false)
const error = ref('')
const success = ref('')

const form = ref({
  receiverId: null,
  title: '',
  content: ''
})

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
  return u ? (u.username || u.phone || u.email || `用户${receiverId}`) : `用户${receiverId}`
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

onMounted(() => {
  fetchUsers()
  fetchSent()
})
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <h1 class="admin-page-title">我的消息</h1>
      <p class="admin-page-subtitle">向用户发送面试通知，用户可在「我的消息」中查看并接受邀请</p>
    </div>

    <div v-if="error" class="admin-error">{{ error }}</div>
    <div v-if="success" class="admin-success">{{ success }}</div>

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
      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>接收人</th>
              <th>标题</th>
              <th>状态</th>
              <th>发送时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="m in sentList" :key="m.id" class="admin-table-row">
              <td>{{ receiverName(m.receiverId) }}</td>
              <td>{{ m.title }}</td>
              <td>
                <span class="admin-messages-status" :class="'admin-messages-status--' + (m.status || 'pending')">
                  {{ statusText(m.status) }}
                </span>
              </td>
              <td>{{ formatTime(m.createTime) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>
  </div>
</template>

<style scoped>
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
