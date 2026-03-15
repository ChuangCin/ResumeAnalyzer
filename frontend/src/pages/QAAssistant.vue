<script setup>
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { getKnowledgeDocList, qaStream } from '../api/api'
import { ArrowLeftBold, ChatDotSquare, CirclePlus, Delete, Search, UploadFilled, UserFilled } from '@element-plus/icons-vue'

const router = useRouter()
const userId = computed(() => localStorage.getItem('userId'))

const input = ref('')
const sending = ref(false)
const error = ref('')
const chatListRef = ref(null)

// ---------- 会话（对话历史）----------
const STORAGE_KEY = computed(() => `qa_sessions_${userId.value || 'guest'}`)
const sessions = ref([])
const activeSessionId = ref('')

function nowTs() {
  return Date.now()
}

function newSession(title = '新会话') {
  return {
    id: `s_${nowTs()}_${Math.random().toString(16).slice(2)}`,
    title,
    updatedAt: nowTs(),
    messages: []
  }
}

function persistSessions() {
  try {
    localStorage.setItem(STORAGE_KEY.value, JSON.stringify(sessions.value))
  } catch (_) {}
}

function restoreSessions() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY.value)
    const parsed = raw ? JSON.parse(raw) : null
    sessions.value = Array.isArray(parsed) ? parsed : []
  } catch (_) {
    sessions.value = []
  }
  if (!sessions.value.length) {
    const s = newSession('默认会话')
    sessions.value = [s]
    activeSessionId.value = s.id
    persistSessions()
  } else if (!activeSessionId.value) {
    activeSessionId.value = sessions.value[0].id
  }
}

const activeSession = computed(() => sessions.value.find(s => s.id === activeSessionId.value) || null)
const activeMessages = computed(() => activeSession.value?.messages || [])

function scrollToBottom() {
  nextTick(() => {
    const el = chatListRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function createSession() {
  const s = newSession('新会话')
  sessions.value.unshift(s)
  activeSessionId.value = s.id
  persistSessions()
  scrollToBottom()
}

function selectSession(id) {
  activeSessionId.value = id
  scrollToBottom()
}

function deleteSession(id) {
  const idx = sessions.value.findIndex(s => s.id === id)
  if (idx < 0) return
  sessions.value.splice(idx, 1)
  if (!sessions.value.length) {
    const s = newSession('默认会话')
    sessions.value = [s]
    activeSessionId.value = s.id
  } else if (activeSessionId.value === id) {
    activeSessionId.value = sessions.value[0].id
  }
  persistSessions()
}

watch(sessions, persistSessions, { deep: true })

// ---------- 知识库选择（右侧面板）----------
const kbDocs = ref([])
const kbSearch = ref('')
const kbSort = ref('time_desc') // time_desc | time_asc | name_asc
const selectedDocIds = ref(new Set())

const filteredDocs = computed(() => {
  const q = (kbSearch.value || '').trim().toLowerCase()
  let list = Array.isArray(kbDocs.value) ? [...kbDocs.value] : []
  if (q) list = list.filter(d => (d.fileName || '').toLowerCase().includes(q))
  if (kbSort.value === 'time_asc') {
    list.sort((a, b) => new Date(a.uploadTime || 0).getTime() - new Date(b.uploadTime || 0).getTime())
  } else if (kbSort.value === 'name_asc') {
    list.sort((a, b) => String(a.fileName || '').localeCompare(String(b.fileName || ''), 'zh-CN'))
  } else {
    list.sort((a, b) => new Date(b.uploadTime || 0).getTime() - new Date(a.uploadTime || 0).getTime())
  }
  return list
})

function toggleDoc(id) {
  const set = new Set(selectedDocIds.value)
  if (set.has(id)) set.delete(id)
  else set.add(id)
  selectedDocIds.value = set
}

async function fetchKbDocs() {
  const id = userId.value
  if (!id) return
  try {
    const data = await getKnowledgeDocList(id)
    kbDocs.value = Array.isArray(data) ? data : []
  } catch (_) {
    kbDocs.value = []
  }
}

function sendMessage() {
  const text = (input.value || '').trim()
  if (!text || sending.value) return
  const id = userId.value
  if (!id) {
    error.value = '请先登录'
    return
  }
  if (!activeSession.value) createSession()
  error.value = ''

  activeSession.value.messages.push({ role: 'user', content: text })
  activeSession.value.updatedAt = nowTs()
  if (!activeSession.value.title || activeSession.value.title === '新会话' || activeSession.value.title === '默认会话') {
    activeSession.value.title = text.length > 16 ? text.slice(0, 16) + '…' : text
  }
  input.value = ''

  const assistantMsg = { role: 'assistant', content: '' }
  activeSession.value.messages.push(assistantMsg)
  scrollToBottom()
  sending.value = true

  qaStream(id, text, {
    onChunk(chunk) {
      assistantMsg.content += chunk
      scrollToBottom()
    },
    onDone() {
      sending.value = false
      if (activeSession.value) activeSession.value.updatedAt = nowTs()
      scrollToBottom()
    },
    onError(err) {
      assistantMsg.content = assistantMsg.content || '抱歉，回答生成失败，请稍后重试。'
      if (err?.message) error.value = err.message
      sending.value = false
      scrollToBottom()
    }
  })
}

onMounted(async () => {
  restoreSessions()
  await fetchKbDocs()
})
</script>

<template>
  <div class="qa-shell">
    <div class="qa-container">
      <header class="qa-topbar">
        <div class="qa-topbar-left">
          <div class="qa-title">问答助手</div>
          <div class="qa-subtitle">选择知识库内容，向 AI 提问</div>
        </div>
        <div class="qa-topbar-right">
          <button type="button" class="qa-topbar-btn" @click="router.push('/knowledge')">
            <el-icon><UploadFilled /></el-icon>
            上传知识库
          </button>
          <button type="button" class="qa-topbar-btn qa-topbar-btn--ghost" @click="router.back()">
            <el-icon><ArrowLeftBold /></el-icon>
            返回
          </button>
        </div>
      </header>

      <div class="qa-layout">
      <aside class="qa-panel qa-panel--left">
        <div class="qa-panel-head">
          <div class="qa-panel-title">对话历史</div>
          <button type="button" class="qa-icon-btn" title="新建会话" @click="createSession">
            <el-icon><CirclePlus /></el-icon>
          </button>
        </div>

        <div class="qa-session-list">
          <button
            v-for="s in sessions"
            :key="s.id"
            type="button"
            class="qa-session-item"
            :class="{ 'qa-session-item--active': s.id === activeSessionId }"
            @click="selectSession(s.id)"
          >
            <div class="qa-session-main">
              <div class="qa-session-title">{{ s.title || '未命名会话' }}</div>
              <div class="qa-session-meta">{{ new Date(s.updatedAt || 0).toLocaleString('zh-CN') }}</div>
            </div>
            <div class="qa-session-del" title="删除" @click.stop="deleteSession(s.id)">
              <el-icon><Delete /></el-icon>
            </div>
          </button>
        </div>
      </aside>

      <main class="qa-panel qa-panel--center">
        <div v-if="error" class="qa-error">{{ error }}</div>

        <div class="qa-chat-wrap">
          <div ref="chatListRef" class="qa-chat-list">
            <div v-if="!activeMessages.length" class="qa-welcome">
              <div class="qa-welcome-icon">
                <el-icon :size="40"><ChatDotSquare /></el-icon>
              </div>
              <p class="qa-welcome-title">基于知识库的智能问答</p>
              <p class="qa-welcome-desc">右侧勾选知识库文档后，在下方输入问题开始提问。</p>
            </div>

            <div
              v-for="(msg, idx) in activeMessages"
              :key="idx"
              class="qa-msg"
              :class="{ 'qa-msg--user': msg.role === 'user', 'qa-msg--assistant': msg.role === 'assistant' }"
            >
              <div class="qa-msg-avatar">
                <el-icon v-if="msg.role === 'user'" :size="20"><UserFilled /></el-icon>
                <el-icon v-else :size="20"><ChatDotSquare /></el-icon>
              </div>
              <div class="qa-msg-body">
                <div class="qa-msg-content">{{ msg.content }}</div>
                <div v-if="msg.role === 'assistant' && sending && idx === activeMessages.length - 1 && !msg.content" class="qa-msg-typing">
                  <span></span><span></span><span></span>
                </div>
              </div>
            </div>
          </div>

          <div class="qa-input-wrap">
            <textarea
              v-model="input"
              class="qa-input"
              placeholder="输入您的问题…"
              rows="2"
              :disabled="sending"
              @keydown.enter.exact.prevent="sendMessage()"
            />
            <button type="button" class="qa-send-btn" :disabled="sending || !input.trim()" @click="sendMessage">
              {{ sending ? '生成中…' : '发送' }}
            </button>
          </div>
        </div>
      </main>

      <aside class="qa-panel qa-panel--right">
        <div class="qa-panel-head">
          <div class="qa-panel-title">选择知识库</div>
          <button type="button" class="qa-icon-btn" title="刷新" @click="fetchKbDocs">↻</button>
        </div>

        <div class="qa-kb-search">
          <el-icon class="qa-kb-search-icon"><Search /></el-icon>
          <input v-model="kbSearch" class="qa-kb-search-input" placeholder="搜索…" />
          <button type="button" class="qa-kb-search-btn" @click="fetchKbDocs">搜索</button>
        </div>

        <select v-model="kbSort" class="qa-kb-sort">
          <option value="time_desc">时间排序</option>
          <option value="time_asc">时间正序</option>
          <option value="name_asc">名称排序</option>
        </select>

        <div class="qa-kb-group">
          <div class="qa-kb-group-title">
            知识库文档
            <span class="qa-kb-group-count">{{ filteredDocs.length }}</span>
          </div>
          <div class="qa-kb-list">
            <label v-for="d in filteredDocs" :key="d.id" class="qa-kb-item">
              <input type="checkbox" class="qa-kb-checkbox" :checked="selectedDocIds.has(d.id)" @change="toggleDoc(d.id)" />
              <div class="qa-kb-item-main">
                <div class="qa-kb-item-title">{{ d.fileName }}</div>
                <div class="qa-kb-item-meta">
                  <span class="qa-kb-item-status">{{ d.status }}</span>
                </div>
              </div>
            </label>
          </div>
        </div>
      </aside>
    </div>
    </div>
  </div>
</template>

<style scoped>
.qa-shell {
  min-height: 100vh;
  background: #f6f7fb;
  padding: 18px;
}

.qa-container {
  width: 100%;
  max-width: 1280px;
  margin: 0 auto;
}

.qa-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  backdrop-filter: blur(10px);
}

.qa-title {
  color: #0f172a;
  font-size: 16px;
  font-weight: 800;
}

.qa-subtitle {
  color: rgba(15, 23, 42, 0.62);
  font-size: 12px;
  margin-top: 2px;
}

.qa-topbar-right {
  display: flex;
  gap: 10px;
}

.qa-topbar-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(99, 102, 241, 0.35);
  background: rgba(99, 102, 241, 0.1);
  color: #1e293b;
  cursor: pointer;
}

.qa-topbar-btn--ghost {
  border-color: #e5e7eb;
  background: #fff;
}

.qa-layout {
  display: grid;
  grid-template-columns: 280px 1fr 320px;
  gap: 14px;
  margin-top: 14px;
}

@media (max-width: 1120px) {
  .qa-layout {
    grid-template-columns: 1fr;
  }
}

.qa-panel {
  border-radius: 16px;
  background: #fff;
  border: 1px solid #e5e7eb;
  overflow: hidden;
}

.qa-panel-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 14px;
  border-bottom: 1px solid #eef2f7;
  color: #0f172a;
}

.qa-panel-title {
  font-weight: 800;
}

.qa-icon-btn {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #334155;
  cursor: pointer;
}

.qa-session-list {
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.qa-session-item {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  padding: 10px 12px;
  border-radius: 14px;
  border: 1px solid #eef2f7;
  background: #fff;
  color: #0f172a;
  cursor: pointer;
}

.qa-session-item--active {
  border-color: rgba(99, 102, 241, 0.55);
  background: rgba(99, 102, 241, 0.08);
}

.qa-session-title {
  font-weight: 700;
  font-size: 13px;
}

.qa-session-meta {
  font-size: 11px;
  color: rgba(15, 23, 42, 0.55);
  margin-top: 2px;
}

.qa-session-del {
  width: 28px;
  height: 28px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  border: 1px solid rgba(239, 68, 68, 0.25);
  background: rgba(239, 68, 68, 0.14);
}

.qa-chat-wrap {
  height: calc(100vh - 140px);
  display: flex;
  flex-direction: column;
}

.qa-chat-list {
  flex: 1;
  overflow: auto;
  padding: 14px;
}

.qa-welcome {
  padding: 30px 12px;
  text-align: center;
  color: rgba(15, 23, 42, 0.8);
}

.qa-welcome-title {
  margin: 10px 0 4px;
  font-weight: 900;
  font-size: 16px;
}

.qa-welcome-desc {
  margin: 0;
  color: rgba(15, 23, 42, 0.6);
  font-size: 12px;
}

.qa-msg {
  display: flex;
  gap: 10px;
  margin-bottom: 12px;
}

.qa-msg--user {
  flex-direction: row-reverse;
}

.qa-msg-avatar {
  width: 34px;
  height: 34px;
  border-radius: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #334155;
  flex: 0 0 auto;
}

.qa-msg-body {
  max-width: 72%;
}

.qa-msg-content {
  padding: 12px 16px;
  border-radius: 16px;
  font-size: 14px;
  line-height: 1.65;
  white-space: pre-wrap;
  word-break: break-word;
  background: #f1f5f9;
  color: #0f172a;
}

.qa-msg--user .qa-msg-content {
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
}

.qa-msg-typing {
  display: flex;
  gap: 6px;
  padding: 12px 16px;
}

.qa-msg-typing span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #94a3b8;
  animation: qa-blink 1.2s ease-in-out infinite both;
}

.qa-msg-typing span:nth-child(2) {
  animation-delay: 0.2s;
}
.qa-msg-typing span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes qa-blink {
  0%,
  80%,
  100% {
    opacity: 0.4;
    transform: scale(0.9);
  }
  40% {
    opacity: 1;
    transform: scale(1);
  }
}

.qa-input-wrap {
  display: flex;
  gap: 10px;
  padding: 12px 14px;
  border-top: 1px solid #eef2f7;
}

.qa-input {
  flex: 1;
  resize: none;
  border-radius: 14px;
  padding: 10px 12px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #0f172a;
  outline: none;
}

.qa-send-btn {
  width: 92px;
  border-radius: 14px;
  border: 1px solid rgba(99, 102, 241, 0.45);
  background: rgba(99, 102, 241, 0.12);
  color: #1e293b;
  cursor: pointer;
}

.qa-send-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.qa-error {
  margin: 12px 14px 0;
  padding: 10px 12px;
  border-radius: 12px;
  border: 1px solid rgba(239, 68, 68, 0.35);
  background: rgba(239, 68, 68, 0.12);
  color: #b91c1c;
}

.qa-kb-search {
  padding: 10px 12px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.qa-kb-search-icon {
  color: rgba(15, 23, 42, 0.6);
}

.qa-kb-search-input {
  flex: 1;
  border-radius: 12px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #0f172a;
  outline: none;
}

.qa-kb-search-btn {
  border-radius: 12px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #334155;
  cursor: pointer;
}

.qa-kb-sort {
  width: calc(100% - 24px);
  margin: 0 12px 12px;
  border-radius: 12px;
  padding: 8px 10px;
  border: 1px solid #e5e7eb;
  background: #fff;
  color: #0f172a;
}

.qa-kb-group {
  padding: 0 12px 12px;
}

.qa-kb-group-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: rgba(15, 23, 42, 0.85);
  font-weight: 800;
  padding: 0 2px 8px;
}

.qa-kb-group-count {
  font-size: 12px;
  color: rgba(15, 23, 42, 0.55);
}

.qa-kb-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
  max-height: calc(100vh - 260px);
  overflow: auto;
}

.qa-kb-item {
  display: flex;
  gap: 10px;
  padding: 10px 10px;
  border-radius: 14px;
  border: 1px solid #eef2f7;
  background: #fff;
  cursor: pointer;
}

.qa-kb-checkbox {
  margin-top: 3px;
}

.qa-kb-item-title {
  color: #0f172a;
  font-weight: 700;
  font-size: 13px;
}

.qa-kb-item-meta {
  margin-top: 4px;
  font-size: 11px;
  color: rgba(15, 23, 42, 0.6);
}
</style>

