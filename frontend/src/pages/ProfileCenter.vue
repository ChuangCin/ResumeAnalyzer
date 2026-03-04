<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, updateUserProfile, uploadAvatar, avatarUrl } from '../api/api'
import {
  ArrowRight,
  Document,
  MagicStick,
  UploadFilled,
  UserFilled
} from '@element-plus/icons-vue'

const router = useRouter()
const userId = computed(() => localStorage.getItem('userId'))

const phone = ref('')
const email = ref('')
const username = ref('')
const avatar = ref('')
const password = ref('')
const passwordConfirm = ref('')
const loading = ref(false)
const avatarLoading = ref(false)
const loadError = ref('')
const saveSuccess = ref('')
const saveError = ref('')
const avatarInputRef = ref(null)

const profileAvatarUrl = computed(() => avatarUrl(avatar.value))

async function fetchUser() {
  const id = userId.value
  if (!id) {
    router.push('/login')
    return
  }
  loadError.value = ''
  try {
    const data = await getUserInfo(id)
    phone.value = data.phone || ''
    email.value = data.email || ''
    username.value = data.username || ''
    avatar.value = data.avatar || ''
    if (data.avatar) localStorage.setItem('avatar', data.avatar)
  } catch (e) {
    const localPhone = localStorage.getItem('phone')
    const localEmail = localStorage.getItem('email')
    if (localPhone) phone.value = localPhone
    if (localEmail) email.value = localEmail
    loadError.value = '未能从服务器加载信息，您仍可修改并保存。'
  }
}

async function handleSave() {
  saveError.value = ''
  saveSuccess.value = ''
  if (!phone.value?.trim()) {
    saveError.value = '请输入手机号（账号）'
    return
  }
  if (!email.value?.trim()) {
    saveError.value = '请输入邮箱'
    return
  }
  if (password.value && password.value !== passwordConfirm.value) {
    saveError.value = '两次输入的密码不一致'
    return
  }
  loading.value = true
  try {
    const payload = {
      phone: phone.value.trim(),
      email: email.value.trim(),
      username: username.value?.trim() || ''
    }
    if (password.value?.trim()) payload.password = password.value.trim()
    await updateUserProfile(userId.value, payload)
    localStorage.setItem('phone', payload.phone)
    localStorage.setItem('email', payload.email)
    if (payload.username) localStorage.setItem('username', payload.username)
    saveSuccess.value = '保存成功'
    password.value = ''
    passwordConfirm.value = ''
  } catch (e) {
    saveError.value = e.message || '保存失败'
  } finally {
    loading.value = false
  }
}

function avatarLetter() {
  const p = phone.value?.trim()
  const u = username.value?.trim()
  const e = email.value?.trim()
  if (u) return u.charAt(0).toUpperCase()
  if (p) return p.slice(-1)
  if (e) return e.charAt(0).toUpperCase()
  return '我'
}

function triggerAvatarInput() {
  avatarInputRef.value?.click()
}

async function onAvatarFileChange(e) {
  const file = e.target?.files?.[0]
  if (!file) return
  const id = userId.value
  if (!id) return
  if (!file.type.startsWith('image/')) {
    saveError.value = '请选择图片文件（JPG、PNG、GIF、WEBP）'
    return
  }
  avatarLoading.value = true
  saveError.value = ''
  try {
    const data = await uploadAvatar(id, file)
    if (data.avatar) {
      avatar.value = data.avatar
      localStorage.setItem('avatar', data.avatar)
    }
    saveSuccess.value = '头像已更新'
  } catch (err) {
    saveError.value = err.message || '头像上传失败'
  } finally {
    avatarLoading.value = false
    e.target.value = ''
  }
}

onMounted(() => {
  fetchUser()
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
          <div class="upload-title">个人中心</div>
          <div class="upload-subtitle">管理您的个人信息</div>
        </div>
      </div>

      <div class="profile-card page-card">
        <div v-if="loadError" class="error-text" style="margin-bottom: 16px">{{ loadError }}</div>

        <div class="profile-avatar-row">
          <label class="profile-avatar-wrap" :aria-busy="avatarLoading">
            <input
              ref="avatarInputRef"
              type="file"
              accept="image/jpeg,image/png,image/gif,image/webp"
              class="profile-avatar-input"
              @change="onAvatarFileChange"
            />
            <div v-if="profileAvatarUrl" class="profile-avatar profile-avatar--img">
              <img :src="profileAvatarUrl" alt="头像" />
            </div>
            <div v-else class="profile-avatar">
              {{ avatarLetter() }}
            </div>
            <span class="profile-avatar-upload-hint">{{ avatarLoading ? '上传中…' : '点击上传头像' }}</span>
          </label>
          <p class="profile-avatar-hint">支持 JPG、PNG、GIF、WEBP</p>
        </div>

        <div class="form-grid">
          <div class="form-row">
            <label class="form-label">手机号（账号）</label>
            <input v-model="phone" class="form-input" type="tel" placeholder="请输入手机号，用于登录" />
          </div>
          <div class="form-row">
            <label class="form-label">邮箱</label>
            <input v-model="email" class="form-input" type="email" placeholder="请输入邮箱，也可用于登录" />
          </div>
          <div class="form-row">
            <label class="form-label">昵称（选填）</label>
            <input v-model="username" class="form-input" type="text" placeholder="设置昵称" />
          </div>
          <div class="form-row">
            <label class="form-label">新密码（留空则不修改）</label>
            <input
              v-model="password"
              class="form-input"
              type="password"
              placeholder="如需修改请输入新密码"
            />
          </div>
          <div class="form-row">
            <label class="form-label">确认新密码</label>
            <input
              v-model="passwordConfirm"
              class="form-input"
              type="password"
              placeholder="再次输入新密码"
            />
          </div>
        </div>

        <p v-if="saveError" class="error-text" style="margin-top: 12px">{{ saveError }}</p>
        <p v-if="saveSuccess" class="success-text" style="margin-top: 12px">{{ saveSuccess }}</p>

        <div class="profile-actions">
          <button class="primary-btn" :disabled="loading" @click="handleSave">
            {{ loading ? '保存中…' : '保存修改' }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.profile-card {
  max-width: 560px;
}

.profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.profile-avatar-wrap {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.profile-avatar-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
}

.profile-avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #4f46e5);
  color: #fff;
  font-size: 28px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.profile-avatar--img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.profile-avatar-upload-hint {
  font-size: 12px;
  color: #6366f1;
}

.profile-avatar-hint {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

.profile-actions {
  margin-top: 24px;
}
</style>
