<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { uploadResume, avatarUrl } from '../api/api'
import {
  ArrowRight,
  ChatDotSquare,
  Collection,
  Document,
  MagicStick,
  UploadFilled,
  UserFilled,
  User,
  Message,
  SwitchButton
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()

const userId = computed(() => localStorage.getItem('userId'))
const username = computed(() => localStorage.getItem('username') || '用户')
const headerAvatarUrl = computed(() => avatarUrl(localStorage.getItem('avatar')))

function handleCommand(cmd) {
  if (cmd === 'profile') router.push('/profile')
  else if (cmd === 'messages') router.push('/messages')
  else if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('avatar')
    router.push('/login')
  }
}

const file = ref(null)
const loading = ref(false)
const error = ref('')
const success = ref('')
const isDragging = ref(false)
const fileInputRef = ref(null)

function setFile(picked) {
  file.value = picked || null
  error.value = ''
  success.value = ''
}

function handleFileChange(e) {
  const [picked] = e.target.files || []
  setFile(picked)
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
  const [picked] = e.dataTransfer?.files || []
  if (picked) {
    setFile(picked)
  }
}

function triggerFilePick() {
  fileInputRef.value?.click()
}

async function handleUpload() {
  error.value = ''
  success.value = ''

  if (!file.value) {
    error.value = '请先选择要上传的简历文件（PDF 或 Word）'
    return
  }

  loading.value = true
  try {
    const data = await uploadResume(file.value)
    if (!data || typeof data.id === 'undefined') {
      throw new Error('后端未返回简历 ID')
    }
    const resumeId = data.id

    success.value = '上传成功，正在为你跳转到分析页…'
    setTimeout(() => {
      router.push(`/analysis/${encodeURIComponent(resumeId)}`)
    }, 800)
  } catch (e) {
    error.value = e.message || '上传失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
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

        <button class="upload-menu-item" type="button">
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon"><UserFilled /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">面试记录</div>
              <div class="upload-menu-sub">查看面试历史</div>
            </div>
          </div>
        </button>

        <div class="upload-menu-section-title upload-menu-section-title--spaced">
          知识库
        </div>

        <button class="upload-menu-item" type="button">
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon"><Collection /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">知识库管理</div>
              <div class="upload-menu-sub">管理知识文档</div>
            </div>
          </div>
        </button>

        <button class="upload-menu-item" type="button">
          <div class="upload-menu-left">
            <div class="upload-menu-icon-circle">
              <el-icon class="upload-menu-icon"><ChatDotSquare /></el-icon>
            </div>
            <div class="upload-menu-text">
              <div class="upload-menu-title">问答助手</div>
              <div class="upload-menu-sub">基于知识库问答</div>
            </div>
          </div>
        </button>
      </nav>
    </aside>

    <div class="upload-main">
      <div class="upload-header upload-header--row">
        <div class="upload-title-block">
          <div class="upload-title">开始您的 AI 模拟面试</div>
          <div class="upload-subtitle">
            上传 PDF 或 Word 简历，AI 将为您定制专属面试方案
          </div>
        </div>
        <div class="upload-user-wrap">
          <el-dropdown trigger="hover" @command="handleCommand">
            <div class="upload-avatar" aria-haspopup="true">
              <img v-if="headerAvatarUrl" class="upload-avatar-img" :src="headerAvatarUrl" alt="头像" />
              <el-icon v-else :size="24"><UserFilled /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>
                  个人中心
                </el-dropdown-item>
                <el-dropdown-item command="messages">
                  <el-icon><Message /></el-icon>
                  我的消息
                </el-dropdown-item>
                <el-dropdown-item command="logout" divided>
                  <el-icon><SwitchButton /></el-icon>
                  退出
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <div class="upload-card">
        <div
          class="upload-dropzone"
          :class="{ 'upload-dropzone--dragging': isDragging }"
          @click="triggerFilePick"
          @dragover="handleDragOver"
          @dragleave="handleDragLeave"
          @dragend="handleDragLeave"
          @drop="handleDrop"
        >
          <div class="upload-dropzone-icon">
            <el-icon :size="22"><UploadFilled /></el-icon>
          </div>
          <div class="upload-dropzone-title">
            点击或拖拽简历文件至此处
          </div>
          <div class="upload-dropzone-desc">
            支持 PDF、DOCX、TXT 文件，单个文件大小不超过 10MB
          </div>
          <button
            class="primary-btn"
            type="button"
            :disabled="loading"
            @click.stop="triggerFilePick"
          >
            {{ file ? '已选择：' + file.name : '选择简历文件' }}
          </button>

          <label for="resume-file-input" class="visually-hidden">选择简历文件</label>
          <input
            id="resume-file-input"
            ref="fileInputRef"
            class="upload-file-input"
            type="file"
            accept=".pdf,.doc,.docx,.txt"
            aria-label="选择简历文件"
            @change="handleFileChange"
          />
        </div>

        <div v-if="error" class="error-text" style="margin-top: 12px">
          {{ error }}
        </div>
        <div v-if="success" class="success-text" style="margin-top: 12px">
          {{ success }}
        </div>

        <div class="upload-actions">
          <button
            class="primary-btn"
            type="button"
            :disabled="loading"
            @click="handleUpload"
          >
            {{ loading ? '上传并分析中…' : '上传并开始分析' }}
          </button>
          <div class="muted-text">
            上传即代表您同意仅将简历用于本次分析，不会对外泄露。
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.upload-header--row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}

.upload-user-wrap {
  flex-shrink: 0;
}

.upload-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.upload-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.upload-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

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
</style>

