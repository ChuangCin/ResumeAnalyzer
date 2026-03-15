<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Document, MagicStick, Message, UploadFilled } from '@element-plus/icons-vue'
import { getMessageList, getMyInquiries, acceptMessage, declineMessage, markMessagesRead, getJobSummaryList } from '../api/api'

const router = useRouter()
const userId = computed(() => localStorage.getItem('userId'))

const messages = ref([])
const inquiries = ref([])
const jobList = ref([])
const loading = ref(false)
const error = ref('')
const actionLoadingId = ref(null)
const activeTab = ref('received')

async function fetchMessages() {
  const id = userId.value
  if (!id) {
    router.push('/login')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [received, sentInquiries, jobData] = await Promise.all([
      getMessageList(id),
      getMyInquiries(id),
      getJobSummaryList().catch(() => [])
    ])
    messages.value = Array.isArray(received) ? received : []
    inquiries.value = Array.isArray(sentInquiries) ? sentInquiries : []
    jobList.value = Array.isArray(jobData) ? jobData : []
  } catch (e) {
    error.value = e.message || '加载消息失败'
    messages.value = []
    inquiries.value = []
  } finally {
    loading.value = false
  }
  try {
    await markMessagesRead(id)
  } catch (_) {}
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
  if (status === 'replied') return '已回复'
  return '待处理'
}

function messageTypeLabel(type) {
  if (type === 'inquiry_reply') return '咨询回复'
  if (type === 'interview') return '面试通知'
  return '消息'
}

/** 将内容中的 [岗位ID: xxx] 替换为岗位名称；emptyFallback 为空内容时的展示文案 */
function contentDisplay(content, emptyFallback) {
  const fallback = emptyFallback ?? '暂无内容'
  if (!content || typeof content !== 'string') return fallback
  return content.replace(/\[岗位ID:\s*(\d+)\]/g, (_, id) => {
    const job = jobList.value.find(j => j.id === Number(id))
    return job ? `岗位：${job.jobName || '未知岗位'}` : '岗位：未知'
  })
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

    <div class="upload-main msg-main">
      <header class="msg-header">
        <div class="upload-title-block">
          <div class="upload-title">我的消息</div>
          <div class="upload-subtitle">查看面试通知与咨询回复，管理我发出的咨询</div>
        </div>
      </header>

      <div class="msg-card page-card">
        <div v-if="error" class="msg-error">{{ error }}</div>
        <div v-else-if="loading" class="msg-loading">加载中…</div>
        <template v-else>
          <div class="msg-tabs">
            <button
              type="button"
              class="msg-tab"
              :class="{ 'msg-tab--active': activeTab === 'received' }"
              @click="activeTab = 'received'"
            >
              收到的消息
            </button>
            <button
              type="button"
              class="msg-tab"
              :class="{ 'msg-tab--active': activeTab === 'inquiries' }"
              @click="activeTab = 'inquiries'"
            >
              我发出的咨询
            </button>
          </div>

          <template v-if="activeTab === 'received'">
            <div v-if="!messages.length" class="msg-empty">
              <div class="msg-empty-icon-wrap">
                <el-icon :size="40" class="msg-empty-icon"><Message /></el-icon>
              </div>
              <p class="msg-empty-text">暂无收到的消息</p>
            </div>
            <div v-else class="msg-list">
              <article
                v-for="m in messages"
                :key="m.id"
                class="msg-item"
                :class="{ 'msg-item--read': m.status !== 'pending' }"
              >
                <div class="msg-item-type" :class="'msg-item-type--' + (m.type || '')">
                  {{ messageTypeLabel(m.type) }}
                </div>
                <h4 class="msg-item-title">{{ m.title || messageTypeLabel(m.type) }}</h4>
                <p class="msg-item-content">{{ contentDisplay(m.content, '暂无详细内容') }}</p>
                <div class="msg-item-footer">
                  <span class="msg-item-time">{{ formatTime(m.createTime) }}</span>
                  <span class="msg-item-status" :class="'msg-item-status--' + (m.status || 'pending')">
                    {{ statusText(m.status) }}
                  </span>
                </div>
                <div v-if="m.type === 'interview' && m.status === 'pending'" class="msg-item-actions">
                  <button
                    type="button"
                    class="msg-btn msg-btn--accept"
                    :disabled="actionLoadingId === m.id"
                    @click="handleAccept(m)"
                  >
                    {{ actionLoadingId === m.id ? '处理中…' : '接受邀请' }}
                  </button>
                  <button
                    type="button"
                    class="msg-btn msg-btn--decline"
                    :disabled="actionLoadingId === m.id"
                    @click="handleDecline(m)"
                  >
                    拒绝
                  </button>
                </div>
              </article>
            </div>
          </template>

          <template v-if="activeTab === 'inquiries'">
            <div v-if="!inquiries.length" class="msg-empty">
              <div class="msg-empty-icon-wrap">
                <el-icon :size="40" class="msg-empty-icon"><Message /></el-icon>
              </div>
              <p class="msg-empty-text">暂无发出的咨询</p>
              <p class="msg-empty-hint">可在「智能匹配」页面向适合岗位发起咨询</p>
            </div>
            <div v-else class="msg-list">
              <article v-for="m in inquiries" :key="m.id" class="msg-item msg-item--inquiry">
                <div class="msg-item-type msg-item-type--inquiry">我的咨询</div>
                <h4 class="msg-item-title">{{ m.title || '咨询' }}</h4>
                <p class="msg-item-content">{{ contentDisplay(m.content) }}</p>
                <div class="msg-item-footer">
                  <span class="msg-item-time">{{ formatTime(m.createTime) }}</span>
                  <span class="msg-item-status" :class="m.status === 'replied' ? 'msg-item-status--accepted' : 'msg-item-status--pending'">
                    {{ m.status === 'replied' ? '已回复' : '待回复' }}
                  </span>
                </div>
              </article>
            </div>
          </template>
        </template>
      </div>
    </div>
  </div>
</template>

<style scoped>
.msg-main { max-width: 640px; }
.msg-header { margin-bottom: 24px; }
.msg-card { padding: 24px; }
.msg-error { color: #dc2626; padding: 12px 0; }
.msg-loading { text-align: center; color: #64748b; padding: 48px 16px; }
.msg-tabs { display: flex; gap: 8px; margin-bottom: 24px; }
.msg-tab {
  padding: 10px 20px; border-radius: 12px; border: 1px solid #e2e8f0; background: #fff;
  font-size: 14px; font-weight: 500; color: #64748b; cursor: pointer; transition: all 0.2s;
}
.msg-tab:hover { border-color: #c7d2fe; color: #4f46e5; }
.msg-tab--active { background: linear-gradient(135deg, #4f46e5, #6366f1); color: #fff; border-color: transparent; }
.msg-empty {
  text-align: center; padding: 48px 24px;
  display: flex; flex-direction: column; align-items: center; gap: 16px;
}
.msg-empty-icon-wrap {
  width: 80px; height: 80px; border-radius: 50%; background: #f1f5f9;
  display: flex; align-items: center; justify-content: center;
}
.msg-empty-icon { color: #94a3b8; }
.msg-empty-text { font-size: 15px; color: #64748b; margin: 0; }
.msg-empty-hint { font-size: 13px; color: #94a3b8; margin: 0; }
.msg-list { display: flex; flex-direction: column; gap: 16px; }
.msg-item {
  padding: 20px; border-radius: 16px; border: 1px solid #e2e8f0; background: #fafbfc;
  transition: box-shadow 0.2s, border-color 0.2s;
}
.msg-item:hover { border-color: #c7d2fe; box-shadow: 0 4px 12px rgba(79, 70, 229, 0.08); }
.msg-item--read { background: #fff; }
.msg-item-type {
  display: inline-block; font-size: 11px; font-weight: 600; letter-spacing: 0.02em;
  padding: 4px 10px; border-radius: 8px; margin-bottom: 10px;
}
.msg-item-type--interview { background: #dbeafe; color: #1d4ed8; }
.msg-item-type--inquiry_reply { background: #dcfce7; color: #166534; }
.msg-item-type--inquiry { background: #e0e7ff; color: #4338ca; }
.msg-item-title { font-size: 16px; font-weight: 600; color: #1e293b; margin: 0 0 8px; }
.msg-item-content { font-size: 14px; color: #475569; line-height: 1.6; margin: 0 0 12px; }
.msg-item-footer { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
.msg-item-time { font-size: 12px; color: #94a3b8; }
.msg-item-status { font-size: 12px; font-weight: 500; }
.msg-item-status--pending { color: #d97706; }
.msg-item-status--accepted { color: #059669; }
.msg-item-status--declined { color: #64748b; }
.msg-item-actions { display: flex; gap: 10px; margin-top: 14px; padding-top: 14px; border-top: 1px solid #e2e8f0; }
.msg-btn { padding: 8px 18px; border-radius: 10px; font-size: 13px; font-weight: 500; cursor: pointer; border: none; }
.msg-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.msg-btn--accept { background: linear-gradient(135deg, #4f46e5, #6366f1); color: #fff; }
.msg-btn--decline { background: #f1f5f9; color: #475569; }
.msg-btn--decline:hover:not(:disabled) { background: #e2e8f0; }
</style>
