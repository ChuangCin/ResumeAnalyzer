<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getUserInfo, updateUserProfile, uploadAvatar, avatarUrl } from '../../api/api'

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
  return '管'
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
  <div class="admin-page">
    <div class="admin-page-header">
      <h1 class="admin-page-title">个人中心</h1>
      <p class="admin-page-subtitle">管理您的个人信息</p>
    </div>

    <div class="admin-card admin-profile-card">
      <div v-if="loadError" class="error-text" style="margin-bottom: 16px">{{ loadError }}</div>

      <div class="admin-profile-avatar-row">
        <label class="admin-profile-avatar-wrap">
          <input
            ref="avatarInputRef"
            type="file"
            accept="image/jpeg,image/png,image/gif,image/webp"
            class="admin-profile-avatar-input"
            @change="onAvatarFileChange"
          />
          <div v-if="profileAvatarUrl" class="admin-profile-avatar admin-profile-avatar--img">
            <img :src="profileAvatarUrl" alt="头像" />
          </div>
          <div v-else class="admin-profile-avatar">
            {{ avatarLetter() }}
          </div>
          <span class="admin-profile-avatar-upload-hint">{{ avatarLoading ? '上传中…' : '点击上传头像' }}</span>
        </label>
        <p class="admin-profile-avatar-hint">支持 JPG、PNG、GIF、WEBP</p>
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

      <div class="admin-profile-actions">
        <button class="primary-btn" :disabled="loading" @click="handleSave">
          {{ loading ? '保存中…' : '保存修改' }}
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-profile-card {
  max-width: 560px;
}

.admin-profile-avatar-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.admin-profile-avatar-wrap {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.admin-profile-avatar-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  overflow: hidden;
}

.admin-profile-avatar {
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

.admin-profile-avatar--img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.admin-profile-avatar-upload-hint {
  font-size: 12px;
  color: #6366f1;
}

.admin-profile-avatar-hint {
  font-size: 13px;
  color: #6b7280;
  margin: 0;
}

.admin-profile-actions {
  margin-top: 24px;
}
</style>
