<script setup>
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  getKnowledgeDocList,
  uploadKnowledgeDoc,
  triggerKnowledgeIndex,
  deleteKnowledgeDoc,
  getKnowledgeStats,
  avatarUrl,
  avatarStorageKey,
  getUnreadCount
} from '../api/api'
import {
  ArrowRight,
  ChatDotSquare,
  Collection,
  Document,
  Delete,
  MagicStick,
  Message,
  UploadFilled,
  User,
  UserFilled,
  SwitchButton,
  FolderOpened,
  DocumentCopy,
  Loading
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const userId = computed(() => localStorage.getItem('userId'))
const headerAvatarUrl = computed(() =>
  avatarUrl(userId.value ? localStorage.getItem(avatarStorageKey(userId.value)) : null)
)

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

const unreadCount = ref(0)
const loading = ref(false)
const uploadLoading = ref(false)
const error = ref('')
const success = ref('')
const list = ref([])
const stats = ref({ docCount: 0, chunkCount: 0 })
const fileInputRef = ref(null)
const isDragging = ref(false)
const acceptTypes = '.pdf,.doc,.docx,.md,.markdown,.txt'
let processingPollTimer = null

const hasProcessing = computed(() =>
  list.value.some(item => (item.status || '').toLowerCase() === 'processing')
)

async function fetchList() {
  const id = userId.value
  if (!id) {
    error.value = '请先登录'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const data = await getKnowledgeDocList(id)
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '加载知识库列表失败'
    list.value = []
  } finally {
    loading.value = false
  }
}

async function fetchStats() {
  const id = userId.value
  if (!id) return
  try {
    const data = await getKnowledgeStats(id)
    stats.value = {
      docCount: data?.docCount ?? 0,
      chunkCount: data?.chunkCount ?? 0
    }
  } catch (_) {
    stats.value = { docCount: 0, chunkCount: 0 }
  }
}

function startProcessingPoll() {
  if (processingPollTimer) return
  processingPollTimer = setInterval(async () => {
    if (!userId.value) {
      stopProcessingPoll()
      return
    }
    await fetchList()
    await fetchStats()
    if (!hasProcessing.value) stopProcessingPoll()
  }, 2000)
}

function stopProcessingPoll() {
  if (processingPollTimer) {
    clearInterval(processingPollTimer)
    processingPollTimer = null
  }
}

watch(hasProcessing, (val) => {
  if (val) startProcessingPoll()
  else stopProcessingPoll()
})

function formatDate(t) {
  if (!t) return '--'
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  return d.toLocaleString('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  })
}

function statusText(status) {
  const map = { uploaded: '待索引', processing: '处理中', completed: '已完成', failed: '失败' }
  return map[status] || status || '--'
}

function statusClass(status) {
  if (status === 'completed') return 'kb-status--done'
  if (status === 'failed') return 'kb-status--failed'
  return 'kb-status--pending'
}

function triggerFilePick() {
  fileInputRef.value?.click()
}

function handleDragOver(e) {
  e.preventDefault()
  isDragging.value = true
}

function handleDragLeave(e) {
  e.preventDefault()
  isDragging.value = false
}

function handleDrop(e) {
  e.preventDefault()
  isDragging.value = false
  const files = e.dataTransfer?.files
  if (files?.length) handleFiles(files)
}

function handleFileChange(e) {
  const files = e.target.files
  if (files?.length) handleFiles(files)
  e.target.value = ''
}

async function handleFiles(fileList) {
  const id = userId.value
  if (!id) {
    error.value = '请先登录'
    return
  }
  const files = Array.from(fileList).filter(f => f.name)
  if (!files.length) return
  error.value = ''
  success.value = ''
  uploadLoading.value = true
  try {
    for (const file of files) {
      await uploadKnowledgeDoc(id, file)
    }
    success.value = `已上传 ${files.length} 个文档，状态为「待索引」。首次在「问答助手」提问或点击“开始建立索引”将触发构建。`
    await fetchList()
    await fetchStats()
    setTimeout(() => { if (success.value?.includes('待索引')) success.value = '' }, 6000)
  } catch (e) {
    error.value = e.message || '上传失败'
  } finally {
    uploadLoading.value = false
  }
}

async function handleTriggerIndex() {
  const id = userId.value
  if (!id) {
    error.value = '请先登录'
    return
  }
  error.value = ''
  success.value = ''
  try {
    const res = await triggerKnowledgeIndex(id)
    if (res?.triggered) success.value = '已开始建立索引，请稍候（状态将变为“处理中/已完成”）'
    else success.value = '当前无需触发索引（可能已在处理中或没有待索引文档）'
    await fetchList()
    await fetchStats()
    if (hasProcessing.value) startProcessingPoll()
  } catch (e) {
    error.value = e.message || '触发索引失败'
  }
}

async function handleDelete(item, e) {
  e.stopPropagation()
  const name = item.fileName || item.name || '该文档'
  if (!confirm(`确定要删除「${name}」吗？`)) return
  const id = userId.value
  if (!id) return
  try {
    await deleteKnowledgeDoc(item.id, id)
    success.value = '已删除'
    await fetchList()
    await fetchStats()
  } catch (err) {
    error.value = err.message || '删除失败'
  }
}

onMounted(async () => {
  await fetchList()
  await fetchStats()
  if (hasProcessing.value) startProcessingPoll()
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

onUnmounted(() => {
  stopProcessingPoll()
})
</script>

<template>
  <div class="library-layout">
    <aside class="library-sidebar">
      <div class="library-logo">
        <div class="library-logo-icon">
          <el-icon :size="18"><MagicStick /></el-icon>
        </div>
        <div class="library-logo-text">
          <div class="library-logo-title">AI Interview</div>
          <div class="library-logo-sub">智能面试助手</div>
        </div>
      </div>

      <nav class="library-menu">
        <div class="library-menu-section-title">简历与面试</div>
        <button
          class="library-menu-item"
          :class="{ 'library-menu-item--active': route.path === '/upload' }"
          type="button"
          @click="router.push('/upload')"
        >
          <div class="library-menu-left">
            <div class="library-menu-icon-circle">
              <el-icon class="library-menu-icon library-menu-icon--upload"><UploadFilled /></el-icon>
            </div>
            <div class="library-menu-text">
              <div class="library-menu-title">上传简历</div>
              <div class="library-menu-sub">AI 分析简历</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/upload'" class="library-menu-arrow"><ArrowRight /></el-icon>
        </button>
        <button
          class="library-menu-item"
          :class="{ 'library-menu-item--active': route.path === '/resume-library' }"
          type="button"
          @click="router.push('/resume-library')"
        >
          <div class="library-menu-left">
            <div class="library-menu-icon-circle">
              <el-icon class="library-menu-icon"><Document /></el-icon>
            </div>
            <div class="library-menu-text">
              <div class="library-menu-title">简历库</div>
              <div class="library-menu-sub">管理所有简历</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/resume-library'" class="library-menu-arrow"><ArrowRight /></el-icon>
        </button>
        <button
          class="library-menu-item"
          :class="{ 'library-menu-item--active': route.path === '/smart-match' }"
          type="button"
          @click="router.push('/smart-match')"
        >
          <div class="library-menu-left">
            <div class="library-menu-icon-circle">
              <el-icon class="library-menu-icon"><UserFilled /></el-icon>
            </div>
            <div class="library-menu-text">
              <div class="library-menu-title">智能匹配</div>
              <div class="library-menu-sub">简历匹配岗位并发起咨询</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/smart-match'" class="library-menu-arrow"><ArrowRight /></el-icon>
        </button>

        <div class="library-menu-section-title library-menu-section-title--spaced">知识库</div>
        <button
          class="library-menu-item library-menu-item--active"
          type="button"
        >
          <div class="library-menu-left">
            <div class="library-menu-icon-circle">
              <el-icon class="library-menu-icon"><Collection /></el-icon>
            </div>
            <div class="library-menu-text">
              <div class="library-menu-title">知识库管理</div>
              <div class="library-menu-sub">管理知识文档</div>
            </div>
          </div>
          <el-icon class="library-menu-arrow"><ArrowRight /></el-icon>
        </button>
        <button
          class="library-menu-item"
          :class="{ 'library-menu-item--active': route.path === '/qa-assistant' }"
          type="button"
          @click="router.push('/qa-assistant')"
        >
          <div class="library-menu-left">
            <div class="library-menu-icon-circle">
              <el-icon class="library-menu-icon"><ChatDotSquare /></el-icon>
            </div>
            <div class="library-menu-text">
              <div class="library-menu-title">问答助手</div>
              <div class="library-menu-sub">基于知识库问答</div>
            </div>
          </div>
          <el-icon v-if="route.path === '/qa-assistant'" class="library-menu-arrow"><ArrowRight /></el-icon>
        </button>
      </nav>
    </aside>

    <div class="library-main">
      <header class="library-header">
        <div class="library-header-text">
          <h1 class="library-title">知识库管理</h1>
          <p class="library-subtitle">上传 PDF、DOCX、Markdown 等文档，自动分块与向量化，供问答助手检索</p>
        </div>
        <div class="library-header-right">
          <el-dropdown trigger="hover" @command="handleCommand">
            <div class="library-avatar" aria-haspopup="true">
              <img v-if="headerAvatarUrl" class="library-avatar-img" :src="headerAvatarUrl" alt="头像" />
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

      <div class="kb-stats">
        <div class="kb-stat-card">
          <div class="kb-stat-icon kb-stat-icon--doc">
            <el-icon :size="22"><FolderOpened /></el-icon>
          </div>
          <div class="kb-stat-body">
            <div class="kb-stat-value">{{ stats.docCount }}</div>
            <div class="kb-stat-label">文档数量</div>
          </div>
        </div>
        <div class="kb-stat-card">
          <div class="kb-stat-icon kb-stat-icon--chunk">
            <el-icon :size="22"><DocumentCopy /></el-icon>
          </div>
          <div class="kb-stat-body">
            <div class="kb-stat-value">{{ stats.chunkCount }}</div>
            <div class="kb-stat-label">知识分块数</div>
          </div>
        </div>
      </div>

      <div
        class="kb-dropzone"
        :class="{ 'kb-dropzone--dragging': isDragging }"
        @click="triggerFilePick"
        @dragover="handleDragOver"
        @dragleave="handleDragLeave"
        @drop="handleDrop"
      >
        <div class="kb-dropzone-icon">
          <el-icon :size="28"><UploadFilled /></el-icon>
        </div>
        <div class="kb-dropzone-title">点击或拖拽文档到此处上传</div>
        <div class="kb-dropzone-desc">支持 PDF、DOCX、Markdown、TXT，自动分块与异步向量化</div>
        <input
          ref="fileInputRef"
          type="file"
          class="visually-hidden"
          :accept="acceptTypes"
          multiple
          @change="handleFileChange"
        />
      </div>

      <div v-if="error" class="library-error">{{ error }}</div>
      <div v-if="success" class="kb-success">{{ success }}</div>
      <div v-if="list.some(i => (i.status || '').toLowerCase() === 'uploaded')" class="kb-tip">
        当前有文档处于「待索引」状态。你可以去「问答助手」首次提问触发索引，或点击
        <button type="button" class="kb-tip-btn" :disabled="uploadLoading" @click="handleTriggerIndex">开始建立索引</button>
      </div>

      <div v-if="loading" class="library-table-loading">加载中...</div>
      <div v-else class="library-table-wrap">
        <table v-if="list.length > 0" class="library-table">
          <thead>
            <tr>
              <th>文档名称</th>
              <th>格式</th>
              <th>上传时间</th>
              <th>状态</th>
              <th class="library-table-actions"></th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="item in list" :key="item.id" class="library-table-row">
              <td>
                <div class="library-cell-name">
                  <el-icon><Document /></el-icon>
                  {{ item.fileName || item.name || '未命名' }}
                </div>
              </td>
              <td>{{ item.format || '--' }}</td>
              <td>{{ formatDate(item.uploadTime || item.createTime) }}</td>
              <td>
                <span class="kb-status" :class="statusClass(item.status)">
                  <el-icon v-if="item.status === 'processing'" class="kb-status-spin"><Loading /></el-icon>
                  {{ statusText(item.status) }}
                </span>
              </td>
              <td class="library-table-actions">
                <el-icon
                  class="library-action-icon library-action-delete"
                  title="删除"
                  @click="handleDelete(item, $event)"
                >
                  <Delete />
                </el-icon>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-else class="library-empty">暂无知识文档，请先上传</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

/* 与简历库一致的左侧栏布局，固定尺寸不随内容变化 */
.library-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  gap: 28px;
  width: 100%;
  max-width: 1280px;
  height: calc(100vh - 72px);
  max-height: calc(100vh - 72px);
  margin: 0 auto;
  padding: 16px 20px 20px;
  box-sizing: border-box;
  overflow: hidden;
  align-items: stretch;
}

.library-sidebar {
  width: 240px;
  min-width: 240px;
  max-width: 240px;
  height: 100%;
  max-height: 100%;
  padding: 18px 14px;
  background: linear-gradient(180deg, #4f46e5, #6366f1);
  border-radius: 20px;
  color: #e5e7ff;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 0 18px 40px rgba(79, 70, 229, 0.35);
  overflow-y: auto;
  box-sizing: border-box;
}

.library-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
  flex-shrink: 0;
}

.library-logo-icon {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: radial-gradient(circle at 30% 20%, #f9fafb, #a5b4fc);
  color: #1f2933;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.18);
  flex-shrink: 0;
}

.library-logo-title { font-size: 15px; font-weight: 600; }
.library-logo-sub { font-size: 11px; opacity: 0.85; }

.library-menu {
  display: flex;
  flex-direction: column;
  gap: 6px;
  margin-top: 4px;
  flex: 1;
  min-width: 0;
}

.library-menu-section-title {
  font-size: 11px;
  letter-spacing: 0.06em;
  color: rgba(226, 232, 255, 0.9);
  margin: 4px 4px 8px;
  flex-shrink: 0;
}

.library-menu-section-title--spaced { margin-top: 18px; }

.library-menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 16px;
  font-size: 13px;
  cursor: pointer;
  border: none;
  background: transparent;
  color: inherit;
  text-align: left;
  opacity: 0.98;
  width: 100%;
  min-height: 48px;
  flex-shrink: 0;
  box-sizing: border-box;
}

.library-menu-item--active {
  background: rgba(248, 250, 252, 0.2);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.22);
}

.library-menu-left { display: flex; align-items: center; gap: 8px; min-width: 0; }
.library-menu-icon-circle {
  width: 32px;
  height: 32px;
  min-width: 32px;
  min-height: 32px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.library-menu-icon { font-size: 16px; color: rgba(243, 244, 246, 0.95); }
.library-menu-icon--upload { font-size: 18px; color: rgba(243, 244, 246, 0.95); }
.library-menu-text { display: flex; flex-direction: column; gap: 2px; min-width: 0; overflow: hidden; }
.library-menu-title { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.library-menu-sub { font-size: 11px; opacity: 0.85; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.library-menu-arrow { font-size: 18px; opacity: 0.8; flex-shrink: 0; }

.library-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
  padding-right: 8px;
}
.library-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  flex-shrink: 0;
}
.library-header-text { flex: 1; min-width: 0; }
.library-title { font-size: 24px; font-weight: 600; color: #1e293b; margin: 0 0 4px; }
.library-subtitle { font-size: 14px; color: #64748b; margin: 0; }

.library-header-right { display: flex; align-items: center; gap: 12px; flex-shrink: 0; }

.library-error { padding: 16px; color: #ef4444; background: #fef2f2; border-radius: 12px; margin-bottom: 20px; }
.library-table-loading { padding: 40px; text-align: center; color: #64748b; }
.library-table-wrap { background: #fff; border-radius: 20px; padding: 0; box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08); overflow: hidden; }
.library-table { width: 100%; border-collapse: collapse; }
.library-table th { padding: 14px 20px; text-align: left; font-size: 13px; font-weight: 600; color: #64748b; background: #f8fafc; border-bottom: 1px solid #e2e8f0; }
.library-table td { padding: 14px 20px; font-size: 14px; color: #334155; border-bottom: 1px solid #f1f5f9; }
.library-table-row:last-child td { border-bottom: none; }
.library-table-row:hover { background: #f8fafc; }
.library-cell-name { display: flex; align-items: center; gap: 8px; }
.library-cell-name .el-icon { font-size: 18px; color: #64748b; }
.library-table-actions { width: 80px; text-align: right; }
.library-action-icon { font-size: 18px; color: #94a3b8; cursor: pointer; margin-left: 12px; }
.library-action-icon:hover { color: #475569; }
.library-action-delete:hover { color: #ef4444; }
.library-empty { padding: 40px; text-align: center; color: #94a3b8; }

.kb-stats {
  display: flex;
  gap: 16px;
  margin-bottom: 20px;
}

.kb-stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
  min-width: 160px;
}

.kb-stat-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.kb-stat-icon--doc {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
}

.kb-stat-icon--chunk {
  background: linear-gradient(135deg, #0ea5e9, #0284c7);
}

.kb-stat-value {
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

.kb-stat-label {
  font-size: 12px;
  color: #64748b;
  margin-top: 2px;
}

.kb-dropzone {
  border: 2px dashed #cbd5e1;
  border-radius: 16px;
  padding: 28px;
  text-align: center;
  cursor: pointer;
  background: #f8fafc;
  margin-bottom: 20px;
  transition: border-color 0.2s, background 0.2s;
}

.kb-dropzone:hover,
.kb-dropzone--dragging {
  border-color: #6366f1;
  background: #eef2ff;
}

.kb-dropzone-icon {
  color: #94a3b8;
  margin-bottom: 10px;
}

.kb-dropzone--dragging .kb-dropzone-icon {
  color: #6366f1;
}

.kb-dropzone-title {
  font-size: 15px;
  font-weight: 600;
  color: #334155;
  margin-bottom: 4px;
}

.kb-dropzone-desc {
  font-size: 13px;
  color: #64748b;
}

.kb-success {
  padding: 12px 16px;
  background: #dcfce7;
  color: #166534;
  border-radius: 12px;
  margin-bottom: 16px;
  font-size: 14px;
}

.kb-tip {
  padding: 12px 16px;
  background: #f8fafc;
  color: #334155;
  border: 1px solid #e2e8f0;
  border-radius: 12px;
  margin-bottom: 16px;
  font-size: 14px;
  line-height: 1.6;
}

.kb-tip-btn {
  margin-left: 8px;
  padding: 6px 10px;
  border-radius: 10px;
  border: 1px solid #cbd5e1;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
}

.kb-tip-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.kb-status {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.kb-status--done {
  background: #dcfce7;
  color: #166534;
}

.kb-status--pending {
  background: #fef3c7;
  color: #b45309;
}

.kb-status--failed {
  background: #fee2e2;
  color: #b91c1c;
}

.kb-status-spin {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.library-avatar {
  width: 40px;
  height: 40px;
  min-width: 40px;
  min-height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: hidden;
}
.library-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}
.library-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  display: block;
}


@media (max-width: 900px) {
  .library-layout {
    grid-template-columns: 1fr;
    gap: 0;
    padding: 16px;
  }
  .library-sidebar { display: none; }
}
</style>
