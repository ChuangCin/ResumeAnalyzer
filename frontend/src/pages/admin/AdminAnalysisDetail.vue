<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { adminGetAnalysisDetail, adminSendMessage } from '../../api/api'
import { ArrowLeft, Document } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const analysisId = computed(() => route.params.id)

const detail = ref(null)
const loading = ref(false)
const error = ref('')
const sendModalVisible = ref(false)
const sendTitle = ref('')
const sendContent = ref('')
const sendLoading = ref(false)
const sendError = ref('')
const sendSuccess = ref('')

const userId = computed(() => localStorage.getItem('userId'))

const suggestionHigh = computed(() => {
  const s = detail.value?.suggestions
  if (!s || typeof s !== 'object') return []
  return Array.isArray(s.high) ? s.high : []
})
const suggestionMedium = computed(() => {
  const s = detail.value?.suggestions
  if (!s || typeof s !== 'object') return []
  return Array.isArray(s.medium) ? s.medium : []
})
const suggestionLow = computed(() => {
  const s = detail.value?.suggestions
  if (!s || typeof s !== 'object') return []
  return Array.isArray(s.low) ? s.low : []
})

const advantages = computed(() => {
  const h = detail.value?.highlights
  if (!h || typeof h !== 'string') return []
  return h.split(/[,，。、；;]/).map(s => s.trim()).filter(Boolean)
})

const scoreItems = computed(() => {
  if (!detail.value) return []
  const d = detail.value
  return [
    { label: '项目经验', value: d.experienceScore ?? 0, max: 40 },
    { label: '技能匹配', value: d.skillScore ?? 0, max: 20 },
    { label: '内容完整性', value: d.completenessScore ?? 0, max: 15 },
    { label: '结构清晰度', value: d.structureScore ?? 0, max: 15 },
    { label: '表达专业性', value: d.expressionScore ?? 0, max: 10 }
  ]
})

async function fetchDetail() {
  const id = analysisId.value
  if (!id) return
  loading.value = true
  error.value = ''
  detail.value = null
  try {
    const data = await adminGetAnalysisDetail(id)
    detail.value = data
  } catch (e) {
    error.value = e.message || '加载详情失败'
  } finally {
    loading.value = false
  }
}

function formatTime(t) {
  if (!t) return '--'
  const d = new Date(t)
  return isNaN(d.getTime()) ? t : d.toLocaleString('zh-CN')
}

function openPdf() {
  const resumeId = detail.value?.resumeId
  if (!resumeId) return
  const base = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'
  window.open(`${base}/pdf/${resumeId}`, '_blank')
}

function openSendModal() {
  sendTitle.value = '面试邀请'
  sendContent.value = ''
  sendError.value = ''
  sendSuccess.value = ''
  sendModalVisible.value = true
}

async function submitSend() {
  const receiverId = detail.value?.userId
  if (!receiverId) {
    sendError.value = '无法获取接收用户'
    return
  }
  const title = (sendTitle.value || '').trim()
  if (!title) {
    sendError.value = '请输入通知标题'
    return
  }
  const adminId = userId.value
  if (!adminId) {
    sendError.value = '请先登录'
    return
  }
  sendLoading.value = true
  sendError.value = ''
  try {
    await adminSendMessage(adminId, {
      receiverId: Number(receiverId),
      title,
      content: (sendContent.value || '').trim()
    })
    sendSuccess.value = '已发送面试通知'
    setTimeout(() => {
      sendModalVisible.value = false
    }, 800)
  } catch (e) {
    sendError.value = e.message || '发送失败'
  } finally {
    sendLoading.value = false
  }
}

watch(analysisId, () => fetchDetail(), { immediate: true })
onMounted(() => fetchDetail())
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-header admin-detail-header">
      <button type="button" class="admin-back-btn" @click="router.push('/admin/analysis')">
        <el-icon><ArrowLeft /></el-icon>
        返回分析列表
      </button>
    </div>

    <div v-if="error" class="admin-error">{{ error }}</div>
    <div v-else-if="loading" class="admin-loading">加载中…</div>

    <template v-else-if="detail">
      <div class="admin-card admin-detail-card">
        <div class="admin-detail-row">
          <span class="admin-detail-label">用户名</span>
          <span class="admin-detail-value">{{ detail.username || '--' }}</span>
        </div>
        <div class="admin-detail-row admin-detail-score-row">
          <span class="admin-detail-label">综合得分</span>
          <span
            class="admin-detail-score"
            :class="(detail.score >= 80 ? 'admin-detail-score--high' : detail.score >= 60 ? 'admin-detail-score--mid' : 'admin-detail-score--low')"
          >
            {{ detail.score ?? '--' }} / 100
          </span>
        </div>
        <div class="admin-detail-row">
          <span class="admin-detail-label">分析时间</span>
          <span class="admin-detail-value">{{ formatTime(detail.createTime) }}</span>
        </div>
        <div class="admin-detail-block">
          <h4 class="admin-detail-block-title">核心评价摘要</h4>
          <p class="admin-detail-summary">{{ detail.summary || '--' }}</p>
        </div>
        <div v-if="advantages.length" class="admin-detail-block">
          <h4 class="admin-detail-block-title">优势亮点</h4>
          <div class="admin-detail-tags">
            <span v-for="(item, i) in advantages" :key="i" class="admin-detail-tag">{{ item }}</span>
          </div>
        </div>
        <div class="admin-detail-block">
          <h4 class="admin-detail-block-title">多维评分</h4>
          <div class="admin-detail-bars">
            <div v-for="(item, i) in scoreItems" :key="i" class="admin-detail-bar">
              <span class="admin-detail-bar-label">{{ item.label }}</span>
              <span class="admin-detail-bar-value">{{ item.value }}/{{ item.max }}</span>
            </div>
          </div>
        </div>
        <div v-if="suggestionHigh.length || suggestionMedium.length || suggestionLow.length" class="admin-detail-block">
          <h4 class="admin-detail-block-title">改进建议</h4>
          <div v-for="(item, i) in suggestionHigh" :key="'h'+i" class="admin-suggestion admin-suggestion--high">
            <strong>{{ item.title }}</strong> {{ item.content }}
          </div>
          <div v-for="(item, i) in suggestionMedium" :key="'m'+i" class="admin-suggestion admin-suggestion--medium">
            <strong>{{ item.title }}</strong> {{ item.content }}
          </div>
          <div v-for="(item, i) in suggestionLow" :key="'l'+i" class="admin-suggestion admin-suggestion--low">
            <strong>{{ item.title }}</strong> {{ item.content }}
          </div>
        </div>
      </div>

      <div class="admin-card admin-detail-card">
        <h4 class="admin-detail-block-title">简历</h4>
        <p class="admin-detail-desc">查看该用户上传的简历文件（PDF）</p>
        <button type="button" class="primary-btn" :disabled="!detail.resumeId" @click="openPdf">
          <el-icon><Document /></el-icon>
          查看简历 PDF
        </button>
      </div>

      <div class="admin-card admin-detail-card admin-detail-actions">
        <p class="admin-detail-desc">向该用户发送面试通知，用户将在「我的消息」中收到并可接受邀请。</p>
        <button type="button" class="primary-btn admin-detail-send-btn" @click="openSendModal">
          向该用户发送面试通知
        </button>
      </div>
    </template>

    <!-- 发送面试通知弹窗 -->
    <div v-if="sendModalVisible" class="admin-modal-mask" @click.self="sendModalVisible = false">
      <div class="admin-modal">
        <h3 class="admin-modal-title">发送面试通知</h3>
        <div class="form-row">
          <label class="form-label">通知标题</label>
          <input v-model="sendTitle" class="form-input" type="text" placeholder="例如：XX 公司面试邀请" />
        </div>
        <div class="form-row">
          <label class="form-label">通知内容（选填）</label>
          <textarea v-model="sendContent" class="form-input admin-modal-textarea" rows="4" placeholder="面试时间、地点等" />
        </div>
        <p v-if="sendError" class="admin-modal-error">{{ sendError }}</p>
        <p v-if="sendSuccess" class="admin-modal-success">{{ sendSuccess }}</p>
        <div class="admin-modal-actions">
          <button type="button" class="admin-modal-btn admin-modal-btn--cancel" @click="sendModalVisible = false">取消</button>
          <button type="button" class="primary-btn admin-modal-btn" :disabled="sendLoading" @click="submitSend">
            {{ sendLoading ? '发送中…' : '发送' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-detail-header {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
}

.admin-back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px solid #e2e8f0;
  border-radius: 10px;
  background: #fff;
  color: #475569;
  font-size: 14px;
  cursor: pointer;
}

.admin-back-btn:hover {
  background: #f8fafc;
  color: #334155;
}

.admin-detail-card {
  margin-bottom: 20px;
}

.admin-detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.admin-detail-label {
  font-size: 14px;
  color: #64748b;
  min-width: 90px;
}

.admin-detail-value {
  font-size: 14px;
  color: #1e293b;
}

.admin-detail-score-row .admin-detail-value { display: none; }
.admin-detail-score {
  font-size: 18px;
  font-weight: 600;
}

.admin-detail-score--high { color: #22c55e; }
.admin-detail-score--mid { color: #f59e0b; }
.admin-detail-score--low { color: #ef4444; }

.admin-detail-block {
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #f1f5f9;
}

.admin-detail-block-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 10px;
}

.admin-detail-summary {
  font-size: 14px;
  color: #475569;
  line-height: 1.6;
  margin: 0;
}

.admin-detail-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.admin-detail-tag {
  padding: 4px 12px;
  border-radius: 999px;
  background: #eef2ff;
  color: #4f46e5;
  font-size: 13px;
}

.admin-detail-bars {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.admin-detail-bar {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #475569;
}

.admin-detail-bar-label { font-weight: 500; }
.admin-detail-bar-value { color: #64748b; }

.admin-suggestion {
  font-size: 13px;
  margin-bottom: 8px;
  padding: 8px 12px;
  border-radius: 8px;
}

.admin-suggestion--high { background: #fef2f2; color: #991b1b; }
.admin-suggestion--medium { background: #fffbeb; color: #92400e; }
.admin-suggestion--low { background: #f0fdf4; color: #166534; }

.admin-detail-desc {
  font-size: 14px;
  color: #64748b;
  margin: 0 0 12px;
}

.admin-detail-actions {
  margin-top: 24px;
}

.admin-detail-send-btn {
  margin-top: 8px;
}

.admin-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(15, 23, 42, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.admin-modal {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  width: 90%;
  max-width: 440px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.25);
}

.admin-modal-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 20px;
}

.admin-modal-textarea {
  resize: vertical;
  min-height: 80px;
}

.admin-modal-error { color: #dc2626; font-size: 13px; margin: 8px 0 0; }
.admin-modal-success { color: #059669; font-size: 13px; margin: 8px 0 0; }

.admin-modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
}

.admin-modal-btn {
  padding: 10px 18px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  border: none;
}

.admin-modal-btn--cancel {
  background: #f1f5f9;
  color: #475569;
}

.admin-modal-btn--cancel:hover {
  background: #e2e8f0;
}
</style>
