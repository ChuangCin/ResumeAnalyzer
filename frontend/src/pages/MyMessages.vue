<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Document, MagicStick, Message, UploadFilled } from '@element-plus/icons-vue'
import { getMessageList, acceptMessage, declineMessage } from '../api/api'

const router = useRouter()
const userId = computed(() => localStorage.getItem('userId'))

const messages = ref([])
const loading = ref(false)
const error = ref('')
const actionLoadingId = ref(null)

async function fetchMessages() {
  const id = userId.value
  if (!id) {
    router.push('/login')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await getMessageList(id)
    messages.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '加载消息失败'
    messages.value = []
  } finally {
    loading.value = false
  }
}

async function handleAccept(m) {
  if (!userId.value) return
  actionLoadingId.value = m.id
  try {
    await acceptMessage(m.id, userId.value)
    await fetchMessages()
  } catch (e) {
    error.value = e.message || '接受失败'
  } finally {
    actionLoadingId.value = null
  }
}

async function handleDecline(m) {
  if (!userId.value) return
  actionLoadingId.value = m.id
  try {
    await declineMessage(m.id, userId.value)
    await fetchMessages()
  } catch (e) {
    error.value = e.message || '操作失败'
  } finally {
    actionLoadingId.value = null
  }
}

function formatTime(t) {
  if (!t) return '--'
  const d = new Date(t)
  return isNaN(d.getTime()) ? t : d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function statusText(status) {
  if (status === 'accepted') return '已接受'
  if (status === 'declined') return '已拒绝'
  return '待处理'
}

onMounted(() => fetchMessages())
</script>

<template>
  <div class="upload-layout">
    <aside class="upload-sidebar">
      <div class="upload-logo">
        <div class="upload-logo-icon">
          <el-icon :size="18"><MagicStick /></el-icon>
        </div>
        <div class="upload-logo-text">
          <div class="upload-logo-title">AI Interview</div>
          <div class="upload-logo-sub">智能面试助手</div>
        </div>
      </div>
      <nav class="upload-menu">
        <div class="upload-menu-section-title">简历与面试</div>
        <button class="upload-menu-item" type="button" @click="router.push('/upload')">
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon upload-menu-icon--upload"><UploadFilled /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">上传简历</div>
              <div class="upload-menu-sub">AI 分析简历</div>
            </div>
          </div>
        </button>
        <button class="upload-menu-item" type="button" @click="router.push('/resume-library')">
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon"><Document /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">简历库</div>
              <div class="upload-menu-sub">管理所有简历</div>
            </div>
          </div>
        </button>
      </nav>
    </aside>

    <div class="upload-main">
      <div class="upload-header">
        <div class="upload-title-block">
          <div class="upload-title">我的消息</div>
          <div class="upload-subtitle">查看面试通知，接受或拒绝邀请</div>
        </div>
      </div>

      <div class="messages-card page-card">
        <div v-if="error" class="messages-error">{{ error }}</div>
        <div v-else-if="loading" class="messages-loading">加载中…</div>
        <template v-else>
          <div v-if="!messages.length" class="messages-empty">
            <el-icon :size="48" class="messages-empty-icon"><Message /></el-icon>
            <p>暂无消息</p>
          </div>
          <ul v-else class="messages-list">
            <li
              v-for="m in messages"
              :key="m.id"
              class="messages-item"
              :class="{ 'messages-item--read': m.status !== 'pending' }"
            >
              <div class="messages-item-main">
                <div class="messages-item-head">
                  <h4 class="messages-item-title">{{ m.title || '面试通知' }}</h4>
                  <span class="messages-item-status" :class="'messages-item-status--' + (m.status || 'pending')">
                    {{ statusText(m.status) }}
                  </span>
                </div>
                <p class="messages-item-content">{{ m.content || '暂无详细内容' }}</p>
                <span class="messages-item-time">{{ formatTime(m.createTime) }}</span>
                <div v-if="m.status === 'pending'" class="messages-item-actions">
                  <button
                    type="button"
                    class="primary-btn messages-btn messages-btn--accept"
                    :disabled="actionLoadingId === m.id"
                    @click="handleAccept(m)"
                  >
                    {{ actionLoadingId === m.id ? '处理中…' : '接受邀请' }}
                  </button>
                  <button
                    type="button"
                    class="messages-btn messages-btn--decline"
                    :disabled="actionLoadingId === m.id"
                    @click="handleDecline(m)"
                  >
                    拒绝
                  </button>
                </div>
              </div>
            </li>
          </ul>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.messages-card {
  max-width: 720px;
}

.messages-error {
  color: #dc2626;
  padding: 12px 0;
}

.messages-loading {
  text-align: center;
  color: #6b7280;
  padding: 48px 16px;
}

.messages-empty {
  text-align: center;
  color: #9ca3af;
  padding: 48px 16px;
}

.messages-empty-icon {
  display: block;
  margin: 0 auto 16px;
  opacity: 0.5;
}

.messages-list {
  list-style: none;
  margin: 0;
  padding: 0;
}

.messages-item {
  padding: 16px 0;
  border-bottom: 1px solid #e5e7eb;
}

.messages-item:last-child {
  border-bottom: none;
}

.messages-item--read .messages-item-title {
  font-weight: 500;
  color: #6b7280;
}

.messages-item-main {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.messages-item-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.messages-item-title {
  font-size: 15px;
  font-weight: 600;
  color: #111827;
  margin: 0;
}

.messages-item-status {
  font-size: 12px;
  flex-shrink: 0;
}

.messages-item-status--pending {
  color: #d97706;
}

.messages-item-status--accepted {
  color: #059669;
}

.messages-item-status--declined {
  color: #6b7280;
}

.messages-item-content {
  font-size: 14px;
  color: #4b5563;
  margin: 0;
  line-height: 1.5;
}

.messages-item-time {
  font-size: 12px;
  color: #9ca3af;
}

.messages-item-actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 8px;
}

.messages-btn {
  padding: 8px 16px;
  border-radius: 10px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: none;
}

.messages-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.messages-btn--accept {
  background: linear-gradient(135deg, #4f46e5, #6366f1);
  color: #fff;
}

.messages-btn--decline {
  background: #f3f4f6;
  color: #4b5563;
}

.messages-btn--decline:hover:not(:disabled) {
  background: #e5e7eb;
}
</style>
