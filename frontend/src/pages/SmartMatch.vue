<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowRight,
  ChatDotSquare,
  Collection,
  Document,
  MagicStick,
  Message,
  UploadFilled,
  User,
  UserFilled,
  SwitchButton
} from '@element-plus/icons-vue'
import { avatarUrl, avatarStorageKey, getUnreadCount } from '../api/api'
import {
  getResumeList,
  getMatchByResumeId,
  runMatchForResume,
  getAnalysisByResumeId,
  sendInquiry
} from '../api/api'

const router = useRouter()
const route = useRoute()
const userId = computed(() => localStorage.getItem('userId'))
const headerAvatarUrl = computed(() => avatarUrl(userId.value ? localStorage.getItem(avatarStorageKey(userId.value)) : null))

function handleCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'messages') router.push('/messages')
  else if (cmd === 'logout') {
    const uid = userId.value
    if (uid) localStorage.removeItem(avatarStorageKey(uid) || '')
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    router.push('/login')
  }
}

const loading = ref(false)
const error = ref('')
const resumes = ref([])
const resumeMatches = ref({})
const resumeAnalysis = ref({})
const matchRunningId = ref(null)
const inquiryDialog = ref(false)
const inquiryJob = ref({ jobName: '', jobId: null })
const inquiryForm = ref({ title: '', content: '' })
const inquirySending = ref(false)
const inquiryError = ref('')
const unreadCount = ref(0)

async function fetchResumes() {
  const id = userId.value
  if (!id) {
    router.push('/login')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await getResumeList(id)
    resumes.value = Array.isArray(data) ? data : []
    resumeMatches.value = {}
    resumeAnalysis.value = {}
    for (const r of resumes.value) {
      try {
        const matchData = await getMatchByResumeId(r.id)
        resumeMatches.value[r.id] = Array.isArray(matchData) ? matchData : []
      } catch (_) {
        resumeMatches.value[r.id] = []
      }
      try {
        const analysisData = await getAnalysisByResumeId(r.id)
        const list = Array.isArray(analysisData) ? analysisData : (analysisData ? [analysisData] : [])
        resumeAnalysis.value[r.id] = list.length ? list[0] : null
      } catch (_) {
        resumeAnalysis.value[r.id] = null
      }
    }
  } catch (e) {
    error.value = e.message || '加载简历失败'
    resumes.value = []
  } finally {
    loading.value = false
  }
}

async function runMatch(resumeId) {
  matchRunningId.value = resumeId
  error.value = ''
  try {
    await runMatchForResume(resumeId)
    ElMessage.success('匹配任务已提交，请稍后点击「刷新匹配结果」查看')
    try {
      const data = await getMatchByResumeId(resumeId)
      resumeMatches.value[resumeId] = Array.isArray(data) ? data : []
    } catch (_) {
      resumeMatches.value[resumeId] = []
    }
  } catch (e) {
    error.value = e.message || '匹配失败'
  } finally {
    matchRunningId.value = null
  }
}

async function refreshMatch(resumeId) {
  try {
    const data = await getMatchByResumeId(resumeId)
    resumeMatches.value[resumeId] = Array.isArray(data) ? data : []
    ElMessage.success('已刷新')
  } catch (e) {
    error.value = e.message || '刷新失败'
  }
}

function openInquiry(job) {
  if (!job || typeof job !== 'object') return
  const jobName = job.jobName || '岗位'
  const jobId = job.jobId != null ? job.jobId : null
  inquiryJob.value = { jobName, jobId }
  inquiryForm.value = {
    title: `关于「${jobName}」的咨询`,
    content: ''
  }
  inquiryError.value = ''
  inquiryDialog.value = true
}

function closeInquiry() {
  inquiryDialog.value = false
}

async function submitInquiry() {
  const id = userId.value
  if (!id) {
    inquiryError.value = '请先登录'
    return
  }
  const title = (inquiryForm.value.title || '').trim()
  if (!title) {
    inquiryError.value = '请填写咨询标题'
    return
  }
  inquiryError.value = ''
  inquirySending.value = true
  try {
    const payload = {
      title,
      content: (inquiryForm.value.content || '').trim()
    }
    if (inquiryJob.value.jobId != null) payload.jobId = inquiryJob.value.jobId
    await sendInquiry(id, payload)
    closeInquiry()
    ElMessage.success('咨询已发送，请到「我的消息」查看回复')
  } catch (e) {
    inquiryError.value = e.message || '发送失败'
  } finally {
    inquirySending.value = false
  }
}

function matchesFor(resumeId) {
  return resumeMatches.value[resumeId] || []
}

function analysisFor(resumeId) {
  return resumeAnalysis.value[resumeId] || null
}

function formatDate(t) {
  if (!t) return '--'
  const d = new Date(t)
  return isNaN(d.getTime()) ? t : d.toLocaleString('zh-CN', { month: '2-digit', day: '2-digit' })
}

onMounted(async () => {
  await fetchResumes()
  const id = userId.value
  if (id) {
    try {
      const n = await getUnreadCount(id)
      unreadCount.value = typeof n === 'number' ? n : (n ?? 0)
    } catch (_) {
      unreadCount.value = 0
    }
  }
})
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
        <button
          class="upload-menu-item"
          :class="{ 'upload-menu-item--active': route.path === '/upload' }"
          type="button"
          @click="router.push('/upload')"
        >
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon upload-menu-icon--upload"><UploadFilled /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">上传简历</div>
              <div class="upload-menu-sub">AI 分析简历</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/upload'" class="upload-menu-arrow"><ArrowRight /></el-icon>
        </button>
        <button
          class="upload-menu-item"
          :class="{ 'upload-menu-item--active': route.path === '/resume-library' }"
          type="button"
          @click="router.push('/resume-library')"
        >
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon"><Document /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">简历库</div>
              <div class="upload-menu-sub">管理所有简历</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/resume-library'" class="upload-menu-arrow"><ArrowRight /></el-icon>
        </button>
        <button
          class="upload-menu-item"
          :class="{ 'upload-menu-item--active': route.path === '/smart-match' }"
          type="button"
          @click="router.push('/smart-match')"
        >
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon"><UserFilled /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">智能匹配</div>
              <div class="upload-menu-sub">简历匹配岗位并发起咨询</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/smart-match'" class="upload-menu-arrow"><ArrowRight /></el-icon>
        </button>
        <div class="upload-menu-section-title upload-menu-section-title--spaced">知识库</div>
        <button
          class="upload-menu-item"
          :class="{ 'upload-menu-item--active': route.path === '/knowledge' }"
          type="button"
          @click="router.push('/knowledge')"
        >
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon"><Collection /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">知识库管理</div>
              <div class="upload-menu-sub">管理知识文档</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/knowledge'" class="upload-menu-arrow"><ArrowRight /></el-icon>
        </button>
        <button
          class="upload-menu-item"
          :class="{ 'upload-menu-item--active': route.path === '/qa-assistant' }"
          type="button"
          @click="router.push('/qa-assistant')"
        >
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon"><ChatDotSquare /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">问答助手</div>
              <div class="upload-menu-sub">基于知识库问答</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/qa-assistant'" class="upload-menu-arrow"><ArrowRight /></el-icon>
        </button>
      </nav>
    </aside>

    <div class="upload-main smart-match-main">
      <header class="smart-match-header">
        <div class="smart-match-title-block">
          <div class="upload-title">智能匹配</div>
          <div class="upload-subtitle">
            将简历库中的简历与现有岗位匹配，匹配后可向管理员发起咨询
          </div>
        </div>
        <div class="smart-match-user-wrap">
          <el-dropdown trigger="click" @command="handleCommand">
            <div class="smart-match-avatar" aria-haspopup="true">
              <img v-if="headerAvatarUrl" class="smart-match-avatar-img" :src="headerAvatarUrl" alt="头像" />
              <el-icon v-else :size="24"><UserFilled /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <span class="avatar-menu-inner"><el-icon><User /></el-icon>个人中心</span>
                </el-dropdown-item>
                <el-dropdown-item command="messages" class="msg-dropdown-item">
                  <span class="avatar-menu-inner"><el-icon><Message /></el-icon>我的消息</span>
                  <span v-if="unreadCount > 0" class="msg-unread-dot"></span>
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <span class="avatar-menu-inner"><el-icon><SwitchButton /></el-icon>退出</span>
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <div v-if="error" class="smart-match-error">{{ error }}</div>
      <div v-else-if="loading" class="smart-match-loading">加载中…</div>
      <div v-else-if="!resumes.length" class="smart-match-empty">
        <el-icon :size="48" class="smart-match-empty-icon"><Document /></el-icon>
        <p>暂无简历，请先上传简历并完成分析</p>
        <button type="button" class="primary-btn" @click="router.push('/upload')">去上传</button>
      </div>
      <div v-else class="smart-match-cards">
        <div v-for="r in resumes" :key="r.id" class="smart-match-card page-card">
          <div class="smart-match-card-head">
            <div class="smart-match-resume-info">
              <el-icon class="smart-match-doc-icon"><Document /></el-icon>
              <div>
                <div class="smart-match-resume-name">{{ r.fileName || '未命名简历' }}</div>
                <div class="smart-match-resume-meta">
                  上传于 {{ formatDate(r.uploadTime) }}
                  <template v-if="analysisFor(r.id)">
                    · 分析得分 {{ analysisFor(r.id).score ?? '--' }} 分
                  </template>
                </div>
              </div>
            </div>
            <div class="smart-match-actions">
              <button
                v-if="matchesFor(r.id).length === 0"
                type="button"
                class="primary-btn smart-match-run-btn"
                :disabled="matchRunningId === r.id"
                @click="runMatch(r.id)"
              >
                {{ matchRunningId === r.id ? '匹配中…' : '一键匹配岗位' }}
              </button>
              <button
                type="button"
                class="smart-match-refresh-btn"
                :disabled="matchRunningId === r.id"
                @click="refreshMatch(r.id)"
              >
                刷新匹配结果
              </button>
            </div>
          </div>
          <div v-if="matchesFor(r.id).length > 0" class="smart-match-list">
            <div class="smart-match-list-title">匹配岗位</div>
            <div
              v-for="(m, idx) in matchesFor(r.id)"
              :key="(m.id || m.jobId) + '-' + r.id + '-' + idx"
              class="smart-match-item"
            >
              <div class="smart-match-item-main">
                <span class="smart-match-job-name">{{ m.jobName || '岗位' }}</span>
                <span class="smart-match-score" :class="m.matchScore >= 70 ? 'smart-match-score--high' : m.matchScore >= 50 ? 'smart-match-score--mid' : 'smart-match-score--low'">
                  匹配度 {{ m.matchScore ?? 0 }}%
                </span>
              </div>
              <button
                type="button"
                class="smart-match-inquiry-btn"
                @click.stop.prevent="openInquiry(m)"
              >
                发起咨询
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <Teleport to="body">
      <div
        v-if="inquiryDialog"
        class="inquiry-modal-mask"
        @click.self="closeInquiry"
      >
        <div class="inquiry-modal-box" @click.stop>
          <h3 class="inquiry-modal-title">向管理员发起咨询</h3>
          <p class="inquiry-modal-desc">咨询将发送给管理员，您可在「我的消息」中查看回复。</p>
          <p v-if="inquiryJob.jobName" class="inquiry-modal-job">岗位：{{ inquiryJob.jobName }}</p>
          <div class="form-row">
            <label class="form-label">咨询标题</label>
            <input v-model="inquiryForm.title" class="form-input" type="text" placeholder="例如：关于该岗位的咨询" />
          </div>
          <div class="form-row">
            <label class="form-label">咨询内容（选填）</label>
            <textarea v-model="inquiryForm.content" class="form-input inquiry-textarea" rows="4" placeholder="请简要描述您的问题或意向" />
          </div>
          <p v-if="inquiryError" class="inquiry-modal-err">{{ inquiryError }}</p>
          <div class="inquiry-modal-actions">
            <button type="button" class="admin-btn" @click="closeInquiry">取消</button>
            <button type="button" class="admin-btn admin-btn--primary" :disabled="inquirySending" @click="submitInquiry">
              {{ inquirySending ? '发送中…' : '发送咨询' }}
            </button>
          </div>
        </div>
      </div>
    </Teleport>
  </div>
</template>

<style scoped>
.smart-match-main { display: flex; flex-direction: column; }
.smart-match-header {
  display: flex; align-items: center; justify-content: space-between; gap: 16px;
  margin-bottom: 20px; flex-shrink: 0;
}
.smart-match-title-block { flex: 1; min-width: 0; }
.smart-match-user-wrap { flex-shrink: 0; }
.smart-match-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #4f46e5); color: #fff;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s; overflow: hidden;
}
.smart-match-avatar:hover {
  transform: scale(1.05); box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}
.smart-match-avatar-img {
  width: 100%; height: 100%; object-fit: cover; display: block;
}
.smart-match-error { color: #dc2626; padding: 12px 0; }
.smart-match-loading { text-align: center; color: #64748b; padding: 48px; }
.smart-match-empty {
  text-align: center; color: #64748b; padding: 48px 24px;
  display: flex; flex-direction: column; align-items: center; gap: 16px;
}
.smart-match-empty-icon { opacity: 0.5; }
.smart-match-cards { display: flex; flex-direction: column; gap: 20px; max-width: 720px; }
.smart-match-card { padding: 20px; }
.smart-match-card-head {
  display: flex; align-items: flex-start; justify-content: space-between; gap: 16px; margin-bottom: 16px;
}
.smart-match-resume-info { display: flex; align-items: center; gap: 12px; }
.smart-match-doc-icon { font-size: 24px; color: #6366f1; }
.smart-match-resume-name { font-size: 16px; font-weight: 600; color: #1e293b; }
.smart-match-resume-meta { font-size: 13px; color: #64748b; margin-top: 4px; }
.smart-match-actions { display: flex; align-items: center; gap: 10px; flex-shrink: 0; }
.smart-match-list-title { font-size: 13px; font-weight: 600; color: #64748b; margin-bottom: 12px; }
.smart-match-list { display: flex; flex-direction: column; gap: 10px; }
.smart-match-item {
  display: flex; align-items: center; justify-content: space-between; gap: 12px;
  padding: 12px 16px; background: #f8fafc; border-radius: 12px; border: 1px solid #e2e8f0;
  position: relative;
}
.smart-match-item-main { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.smart-match-job-name { font-weight: 500; color: #334155; }
.smart-match-score { font-size: 13px; font-weight: 600; }
.smart-match-score--high { color: #22c55e; }
.smart-match-score--mid { color: #f59e0b; }
.smart-match-score--low { color: #ef4444; }
.smart-match-inquiry-btn {
  padding: 6px 14px; border-radius: 8px; border: 1px solid #6366f1; background: #fff; color: #6366f1;
  font-size: 13px; cursor: pointer; flex-shrink: 0; position: relative; z-index: 2;
}
.smart-match-inquiry-btn:hover { background: #eef2ff; }
.smart-match-refresh-btn {
  padding: 6px 12px; font-size: 12px; color: #64748b; background: #f1f5f9; border: none; border-radius: 8px; cursor: pointer;
}
.smart-match-refresh-btn:hover:not(:disabled) { background: #e2e8f0; color: #475569; }
.smart-match-refresh-btn:disabled { opacity: 0.6; cursor: not-allowed; }
.inquiry-modal-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.45); z-index: 9999;
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.inquiry-modal-box {
  background: #fff; border-radius: 20px; padding: 24px; max-width: 480px; width: 100%;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
}
.inquiry-modal-title { font-size: 18px; font-weight: 600; margin: 0 0 8px; color: #1e293b; }
.inquiry-modal-desc { font-size: 14px; color: #64748b; margin: 0 0 8px; }
.inquiry-modal-job { font-size: 14px; color: #475569; margin: 0 0 16px; padding: 10px 12px; background: #f8fafc; border-radius: 10px; }
.form-row { margin-bottom: 16px; }
.form-label { display: block; font-size: 14px; font-weight: 500; color: #334155; margin-bottom: 6px; }
.form-input { width: 100%; padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 10px; font-size: 14px; box-sizing: border-box; }
.inquiry-textarea { resize: vertical; min-height: 80px; }
.inquiry-modal-err { color: #dc2626; font-size: 13px; margin-top: 8px; }
.inquiry-modal-actions { display: flex; justify-content: flex-end; gap: 12px; margin-top: 20px; }
</style>
