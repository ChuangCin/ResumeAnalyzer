<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getResumeListPage, deleteResume, avatarUrl, avatarStorageKey, getUnreadCount } from '../api/api'
import {
  ArrowRight,
  ChatDotSquare,
  CircleCheck,
  Collection,
  Document,
  MagicStick,
  Message,
  UploadFilled,
  User,
  UserFilled,
  Search,
  Delete,
  Right,
  SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

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
const unreadCount = ref(0)
const loading = ref(false)
const error = ref('')
const list = ref([])
const total = ref(0)
const page = ref(1)
const pageSize = ref(5)
const searchKeyword = ref('')

const filteredList = computed(() => {
  const kw = searchKeyword.value?.trim().toLowerCase()
  if (!kw) return list.value
  return list.value.filter(item =>
    (item.fileName || '').toLowerCase().includes(kw)
  )
})

async function fetchList(p = page.value) {
  const uid = localStorage.getItem('userId')
  if (!uid) {
    error.value = '请先登录'
    return
  }
  page.value = p
  loading.value = true
  error.value = ''
  try {
    const data = await getResumeListPage(uid, p, pageSize.value)
    list.value = Array.isArray(data?.list) ? data.list : []
    const rawTotal = data?.total
    total.value = typeof rawTotal === 'number' ? rawTotal : Number(rawTotal) || 0
    if (total.value === 0 && list.value.length > 0) total.value = list.value.length
  } catch (e) {
    error.value = e.message || '加载简历列表失败'
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  fetchList(p)
}

function onPageSizeChange(size) {
  pageSize.value = size
  fetchList(1)
}

function formatDate(t) {
  if (!t) return '--'
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  return `${d.getFullYear()}/${String(d.getMonth() + 1).padStart(2, '0')}/${String(d.getDate()).padStart(2, '0')}`
}

function scoreColor(score) {
  if (score == null) return '#94a3b8'
  if (score >= 80) return '#22c55e'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}

function scorePercent(score) {
  if (score == null) return 0
  return Math.min(100, Math.round(score))
}

function goToAnalysis(id) {
  router.push(`/analysis/${id}`)
}

async function handleDelete(item, e) {
  e.stopPropagation()
  const name = item.fileName || '该简历'
  if (!confirm(`确定要删除「${name}」吗？删除后其分析记录也会一并删除。`)) return
  const id = userId.value
  if (!id) {
    error.value = '请先登录'
    return
  }
  try {
    await deleteResume(item.id, id)
    await fetchList(page.value)
  } catch (err) {
    error.value = err.message || '删除失败'
  }
}

onMounted(async () => {
  await fetchList(1)
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
          class="library-menu-item"
          :class="{ 'library-menu-item--active': route.path === '/knowledge' }"
          type="button"
          @click="router.push('/knowledge')"
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
          <el-icon v-if="route.path === '/knowledge'" class="library-menu-arrow"><ArrowRight /></el-icon>
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
          <h1 class="library-title">简历库</h1>
          <p class="library-subtitle">管理您已分析过的所有简历及面试记录</p>
        </div>
        <div class="library-header-right">
          <div class="library-search">
          <label for="library-search-input" class="visually-hidden">搜索简历</label>
          <el-icon class="library-search-icon"><Search /></el-icon>
          <input
            id="library-search-input"
            v-model="searchKeyword"
            class="library-search-input"
            placeholder="搜索简历..."
            aria-label="搜索简历"
          />
          </div>
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

      <div v-if="error" class="library-error">{{ error }}</div>

      <div v-else-if="loading" class="library-table-loading">加载中...</div>

      <div v-else class="library-table-wrap">
        <div class="library-table-scroll">
          <table v-if="filteredList.length > 0" class="library-table">
            <thead>
              <tr>
                <th>简历名称</th>
                <th>上传日期</th>
                <th>AI 评分</th>
                <th>面试状态</th>
                <th class="library-table-actions"></th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in filteredList" :key="item.id" class="library-table-row">
                <td>
                  <div class="library-cell-name">
                    <el-icon><Document /></el-icon>
                    {{ item.fileName || '未命名' }}
                  </div>
                </td>
                <td>{{ formatDate(item.uploadTime) }}</td>
                <td>
                  <div class="library-cell-score">
                    <div class="library-score-bar">
                      <div
                        class="library-score-fill"
                        :style="{
                          width: scorePercent(item.score) + '%',
                          background: scoreColor(item.score)
                        }"
                      />
                    </div>
                    <span class="library-score-num">{{ item.score ?? '--' }}</span>
                  </div>
                </td>
                <td>
                  <span
                    class="library-status"
                    :class="{
                      'library-status--done': item.interviewStatus === '已完成',
                      'library-status--pending': item.interviewStatus === '待面试'
                    }"
                  >
                    <el-icon v-if="item.interviewStatus === '已完成'" class="library-status-icon">
                      <CircleCheck />
                    </el-icon>
                    {{ item.interviewStatus || '待面试' }}
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
                  <el-icon
                    class="library-action-icon library-action-go"
                    title="查看分析"
                    @click="goToAnalysis(item.id)"
                  >
                    <Right />
                  </el-icon>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-else class="library-empty">暂无简历记录</div>
        </div>
        <div v-show="total > 0" class="library-pagination">
          <el-pagination
            :current-page="page"
            :page-size="pageSize"
            :page-sizes="[5, 10, 20]"
            :total="total"
            :hide-on-single-page="false"
            layout="total, sizes, prev, pager, next"
            @current-change="onPageChange"
            @size-change="onPageSizeChange"
          />
        </div>
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
}

.library-logo-title { font-size: 15px; font-weight: 600; }
.library-logo-sub { font-size: 11px; opacity: 0.85; }

.library-menu { display: flex; flex-direction: column; gap: 6px; margin-top: 4px; flex: 1; min-height: 0; }
.library-menu-section-title {
  font-size: 11px; letter-spacing: 0.06em; color: rgba(226, 232, 255, 0.9);
  margin: 4px 4px 8px;
}
.library-menu-section-title--spaced { margin-top: 18px; }

.library-menu-item {
  display: flex; align-items: center; justify-content: space-between;
  gap: 8px; padding: 10px 12px; border-radius: 16px;
  font-size: 13px; cursor: pointer; border: none; background: transparent;
  color: inherit; text-align: left; opacity: 0.98;
}
.library-menu-item--active {
  background: rgba(248, 250, 252, 0.2);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.22);
}
.library-menu-left { display: flex; align-items: center; gap: 8px; }
.library-menu-icon-circle {
  width: 32px; height: 32px; border-radius: 999px;
  background: rgba(248, 250, 252, 0.18);
  display: flex; align-items: center; justify-content: center;
}
.library-menu-icon { font-size: 16px; color: rgba(243, 244, 246, 0.95); }
.library-menu-icon--upload { font-size: 18px; color: rgba(243, 244, 246, 0.95); }
.library-menu-text { display: flex; flex-direction: column; gap: 2px; }
.library-menu-title { font-size: 13px; font-weight: 500; }
.library-menu-sub { font-size: 11px; opacity: 0.85; }
.library-menu-arrow { font-size: 18px; opacity: 0.8; }

.library-main {
  display: flex;
  flex-direction: column;
  min-width: 0;
  min-height: 0;
  overflow: hidden;
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
.library-header-text { flex: 1; }
.library-header-right {
  display: flex; align-items: center; gap: 12px; flex-shrink: 0;
}
.library-title { font-size: 24px; font-weight: 600; color: #1e293b; margin: 0 0 4px; }
.library-subtitle { font-size: 14px; color: #64748b; margin: 0; }

.library-avatar {
  width: 40px; height: 40px; border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff; display: flex; align-items: center; justify-content: center;
  cursor: pointer; transition: transform 0.2s, box-shadow 0.2s;
}
.library-avatar:hover {
  transform: scale(1.05); box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.library-avatar-img {
  width: 100%; height: 100%; border-radius: 50%; object-fit: cover;
}

.library-search {
  display: flex; align-items: center; gap: 8px;
  padding: 10px 14px; border-radius: 12px; border: 1px solid #e2e8f0;
  background: #fff; min-width: 200px;
}
.library-search-icon { font-size: 18px; color: #94a3b8; }
.library-search-input {
  flex: 1; border: none; outline: none; font-size: 14px;
  background: transparent;
}

.library-error { padding: 16px; color: #ef4444; background: #fef2f2; border-radius: 12px; margin-bottom: 20px; }
.library-table-loading { padding: 40px; text-align: center; color: #64748b; }

.library-table-wrap {
  display: flex;
  flex-direction: column;
  min-height: 0;
  flex: 1;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
  overflow: hidden;
}

.library-table-scroll {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.library-table { width: 100%; border-collapse: collapse; }
.library-table th {
  padding: 14px 20px; text-align: left; font-size: 13px; font-weight: 600; color: #64748b;
  background: #f8fafc; border-bottom: 1px solid #e2e8f0;
}
.library-table td {
  padding: 14px 20px; font-size: 14px; color: #334155;
  border-bottom: 1px solid #f1f5f9;
}
.library-table-row:last-child td { border-bottom: none; }
.library-table-row:hover { background: #f8fafc; }

.library-cell-name {
  display: flex; align-items: center; gap: 8px;
}
.library-cell-name .el-icon { font-size: 18px; color: #64748b; }

.library-cell-score { display: flex; align-items: center; gap: 10px; }
.library-score-bar {
  width: 60px; height: 6px; border-radius: 999px; background: #e2e8f0;
  overflow: hidden;
}
.library-score-fill { height: 100%; border-radius: inherit; transition: width 0.3s; }
.library-score-num { font-weight: 600; min-width: 28px; }

.library-status {
  display: inline-flex; align-items: center; gap: 4px;
  padding: 4px 10px; border-radius: 999px; font-size: 12px;
}
.library-status--done { background: #dcfce7; color: #166534; }
.library-status--pending { background: #f1f5f9; color: #64748b; }
.library-status-icon { font-size: 12px; }

.library-table-actions { width: 80px; text-align: right; }
.library-action-icon {
  font-size: 18px; color: #94a3b8; cursor: pointer; margin-left: 12px;
}
.library-action-icon:hover { color: #475569; }
.library-action-go:hover { color: #4f46e5; }

.library-empty { padding: 40px; text-align: center; color: #94a3b8; }

.library-pagination {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid #f1f5f9;
  background: #fafbfc;
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
