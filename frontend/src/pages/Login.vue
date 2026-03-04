<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../api/api'
import {
  MagicStick,
  Document,
  VideoCamera,
  DataBoard,
  User,
  Lock,
  Key
} from '@element-plus/icons-vue'

const router = useRouter()

const account = ref('')
const password = ref('')
const remember = ref(false)
const role = ref('user') // 'user' | 'admin'
const loading = ref(false)
const error = ref('')

function switchRole(r) {
  role.value = r
  error.value = ''
}

async function handleLogin() {
  error.value = ''
  if (!account.value?.trim() || !password.value) {
    error.value = '请输入邮箱或手机号及密码'
    return
  }

  loading.value = true
  try {
    const data = await login({
      account: account.value.trim(),
      password: password.value,
      role: role.value
    })
    if (data.token) localStorage.setItem('token', data.token)
    if (data.id != null) localStorage.setItem('userId', String(data.id))
    if (data.username) localStorage.setItem('username', data.username)
    if (data.phone) localStorage.setItem('phone', data.phone)
    if (data.email) localStorage.setItem('email', data.email)
    if (data.role) localStorage.setItem('role', data.role)
    if (data.avatar) localStorage.setItem('avatar', data.avatar)
    if (data.role === 'admin') router.push('/admin')
    else router.push('/upload')
  } catch (e) {
    error.value = e.message || '登录失败，请稍后重试'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-page">
    <div class="auth-card">
      <!-- 左侧：品牌展示区 -->
      <div class="auth-brand">
        <div class="auth-brand-deco auth-brand-deco--1" />
        <div class="auth-brand-deco auth-brand-deco--2" />
        <div class="auth-brand-inner">
          <div class="auth-brand-logo">
            <div class="auth-brand-logo-icon">
              <el-icon :size="24"><MagicStick /></el-icon>
            </div>
            <div>
              <h1 class="auth-brand-title">AI Interview</h1>
              <p class="auth-brand-sub">智能面试助手</p>
            </div>
          </div>
          <div class="auth-brand-content">
            <h2 class="auth-brand-heading">开启您的<br />AI 职场加速引擎</h2>
            <div class="auth-brand-features">
              <div class="auth-feature">
                <div class="auth-feature-icon">
                  <el-icon :size="20"><Document /></el-icon>
                </div>
                <div>
                  <h4 class="auth-feature-title">智能简历分析</h4>
                  <p class="auth-feature-desc">深度解析简历优劣势，精准匹配目标岗位需求。</p>
                </div>
              </div>
              <div class="auth-feature">
                <div class="auth-feature-icon">
                  <el-icon :size="20"><VideoCamera /></el-icon>
                </div>
                <div>
                  <h4 class="auth-feature-title">AI 模拟面试</h4>
                  <p class="auth-feature-desc">多场景、多维度实时对话，告别面试紧张。</p>
                </div>
              </div>
              <div class="auth-feature">
                <div class="auth-feature-icon">
                  <el-icon :size="20"><DataBoard /></el-icon>
                </div>
                <div>
                  <h4 class="auth-feature-title">面试知识库</h4>
                  <p class="auth-feature-desc">覆盖全行业面试题库，为您定制专属应答方案。</p>
                </div>
              </div>
            </div>
          </div>
          <div class="auth-brand-footer">
            <p class="auth-brand-footer-text">
              <el-icon class="auth-brand-footer-icon"><Key /></el-icon>
              2026 智能招聘前沿技术支持
            </p>
          </div>
        </div>
      </div>

      <!-- 右侧：登录表单 -->
      <div class="auth-form-wrap">
        <div class="auth-form-main">
          <div class="auth-form-header">
            <h3 class="auth-form-title">欢迎回来 ✨</h3>
            <p class="auth-form-subtitle">
              {{ role === 'admin' ? '管理员请使用邮箱或手机号登录' : '使用邮箱或手机号登录' }}
            </p>
          </div>

          <!-- 身份切换：普通用户 / 管理员 -->
          <div class="auth-role-tabs">
            <button
              type="button"
              class="auth-role-tab"
              :class="{ 'auth-role-tab--active': role === 'user' }"
              @click="switchRole('user')"
            >
              普通用户
            </button>
            <button
              type="button"
              class="auth-role-tab"
              :class="{ 'auth-role-tab--active': role === 'admin' }"
              @click="switchRole('admin')"
            >
              管理员
            </button>
          </div>

          <form class="auth-form" @submit.prevent="handleLogin">
            <div class="auth-field">
              <label class="auth-label">邮箱或手机号</label>
              <div class="auth-input-wrap">
                <el-icon class="auth-input-icon"><User /></el-icon>
                <input
                  v-model="account"
                  type="text"
                  class="auth-input"
                  placeholder="请输入邮箱或手机号（账号）"
                  autocomplete="username"
                />
              </div>
            </div>
            <div class="auth-field">
              <label class="auth-label auth-label--row">
                密码
                <a href="#" class="auth-link" @click.prevent>忘记密码？</a>
              </label>
              <div class="auth-input-wrap">
                <el-icon class="auth-input-icon"><Lock /></el-icon>
                <input
                  v-model="password"
                  type="password"
                  class="auth-input"
                  placeholder="请输入密码"
                  autocomplete="current-password"
                />
              </div>
            </div>
            <div class="auth-options">
              <input
                id="remember"
                v-model="remember"
                type="checkbox"
                class="auth-checkbox"
              />
              <label for="remember" class="auth-checkbox-label">30天内自动登录</label>
            </div>
            <p v-if="error" class="auth-error">{{ error }}</p>
            <button type="submit" class="auth-submit" :disabled="loading">
              {{ loading ? '登录中…' : '立即登录' }}
            </button>
          </form>

          <div class="auth-switch">
            <p class="auth-switch-text">
              还没有账号？
              <router-link to="/register" class="auth-switch-link">免费注册</router-link>
            </p>
          </div>
        </div>

        <!-- 社交登录 -->
        <div class="auth-social">
          <div class="auth-social-divider">
            <span class="auth-social-label">第三方快捷通道</span>
          </div>
          <div class="auth-social-btns">
            <button type="button" class="auth-social-btn">
              <span class="auth-social-icon auth-social-icon--wechat">微</span>
              <span class="auth-social-name">微信</span>
            </button>
            <button type="button" class="auth-social-btn">
              <span class="auth-social-icon auth-social-icon--alipay">支</span>
              <span class="auth-social-name">支付宝</span>
            </button>
          </div>
        </div>

        <p class="auth-copyright">
          © 2026 AI Interview 智能面试助手. All Rights Reserved.<br />
          当前日期: {{ new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric' }) }}
        </p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.auth-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px 24px;
  background-color: #f8fafc;
}

.auth-card {
  width: 100%;
  max-width: 1600px;
  aspect-ratio: 16 / 10;
  min-height: 720px;
  max-height: 96vh;
  background: #fff;
  border-radius: 28px;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.12), 0 12px 24px -8px rgba(0, 0, 0, 0.08);
}

@media (min-width: 768px) {
  .auth-card {
    flex-direction: row;
  }
}

.auth-brand {
  flex: 0 0 42%;
  background: linear-gradient(135deg, #6366f1 0%, #4338ca 100%);
  padding: 36px 40px;
  color: #fff;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
  overflow: hidden;
}

.auth-brand-deco {
  position: absolute;
  border-radius: 50%;
}
.auth-brand-deco--1 {
  width: 256px;
  height: 256px;
  background: #fff;
  opacity: 0.05;
  top: -80px;
  left: -80px;
}
.auth-brand-deco--2 {
  width: 320px;
  height: 320px;
  border: 40px solid rgba(255, 255, 255, 0.05);
  bottom: 80px;
  right: -80px;
}

.auth-brand-inner {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  flex: 1;
}

.auth-brand-logo {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 36px;
}

.auth-brand-logo-icon {
  width: 48px;
  height: 48px;
  background: #fff;
  color: #6366f1;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);
}

.auth-brand-title {
  font-size: 20px;
  font-weight: 700;
  letter-spacing: -0.02em;
  margin: 0;
  line-height: 1.2;
}

.auth-brand-sub {
  font-size: 12px;
  color: rgba(199, 210, 254, 1);
  margin: 4px 0 0;
  letter-spacing: 0.15em;
  text-transform: uppercase;
}

.auth-brand-content {
  flex: 1;
}

.auth-brand-heading {
  font-size: 26px;
  font-weight: 700;
  line-height: 1.3;
  margin: 0 0 20px;
}

.auth-brand-features {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.auth-feature {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.auth-feature-icon {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.auth-feature-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 4px;
}

.auth-feature-desc {
  font-size: 13px;
  color: rgba(199, 210, 254, 0.9);
  margin: 0;
  line-height: 1.4;
}

.auth-brand-footer {
  padding-top: 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  margin-top: auto;
}

.auth-brand-footer-text {
  font-size: 14px;
  color: rgba(199, 210, 254, 1);
  margin: 0;
  display: flex;
  align-items: center;
  gap: 8px;
}

.auth-brand-footer-icon {
  font-size: 16px;
}

.auth-form-wrap {
  flex: 1;
  padding: 28px 28px 24px;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
  background: #fff;
  min-width: 0;
  overflow-y: auto;
}

@media (min-width: 768px) {
  .auth-form-wrap {
    padding: 40px 56px 32px;
  }
}

.auth-form-main {
  margin-bottom: 28px;
}

.auth-form-header {
  margin-bottom: 24px;
}

.auth-form-title {
  font-size: 24px;
  font-weight: 700;
  color: #111827;
  margin: 0 0 8px;
}

.auth-form-subtitle {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

.auth-role-tabs {
  display: inline-flex;
  padding: 5px;
  background: #f3f4f6;
  border-radius: 12px;
  margin-bottom: 24px;
  gap: 0;
}

.auth-role-tab {
  padding: 10px 28px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 500;
  color: #6b7280;
  background: transparent;
  border: none;
  cursor: pointer;
  transition: all 0.2s;
}

.auth-role-tab--active {
  background: #fff;
  color: #6366f1;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.auth-role-tab:hover:not(.auth-role-tab--active) {
  color: #374151;
}

.auth-form {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.auth-field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.auth-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.auth-label--row {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.auth-link {
  font-size: 12px;
  font-weight: 500;
  color: #6366f1;
  text-decoration: none;
}

.auth-link:hover {
  color: #4f46e5;
}

.auth-input-wrap {
  position: relative;
}

.auth-input-icon {
  position: absolute;
  left: 12px;
  top: 50%;
  transform: translateY(-50%);
  font-size: 18px;
  color: #9ca3af;
}

.auth-input {
  width: 100%;
  padding: 13px 18px 13px 44px;
  font-size: 15px;
  color: #1f2937;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.auth-input:focus {
  border-color: #6366f1;
  box-shadow: 0 0 0 4px rgba(99, 102, 241, 0.1);
}

.auth-options {
  display: flex;
  align-items: center;
  gap: 8px;
}

.auth-checkbox {
  width: 16px;
  height: 16px;
  border-radius: 4px;
  border: 1px solid #d1d5db;
  accent-color: #6366f1;
  cursor: pointer;
}

.auth-checkbox-label {
  font-size: 14px;
  color: #4b5563;
  cursor: pointer;
  user-select: none;
}

.auth-error {
  font-size: 13px;
  color: #ef4444;
  margin: 0;
}

.auth-submit {
  width: 100%;
  padding: 15px 18px;
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  background: #6366f1;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  box-shadow: 0 10px 15px -3px rgba(99, 102, 241, 0.2);
  transition: background 0.2s, transform 0.05s;
}

.auth-submit:hover:not(:disabled) {
  background: #4f46e5;
}

.auth-submit:active:not(:disabled) {
  transform: scale(0.98);
}

.auth-submit:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.auth-switch {
  margin-top: 24px;
  text-align: center;
}

.auth-switch-text {
  font-size: 15px;
  color: #6b7280;
  margin: 0;
}

.auth-switch-link {
  color: #6366f1;
  font-weight: 700;
  text-decoration: none;
}

.auth-switch-link:hover {
  text-decoration: underline;
}

.auth-social {
  margin-top: 28px;
}

.auth-social-divider {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 24px;
}

.auth-social-divider::before {
  content: '';
  position: absolute;
  left: 0;
  right: 0;
  top: 50%;
  height: 1px;
  background: #e5e7eb;
}

.auth-social-label {
  font-size: 12px;
  color: #9ca3af;
  background: #fff;
  padding: 0 16px;
  position: relative;
  z-index: 1;
  letter-spacing: 0.1em;
  text-transform: uppercase;
}

.auth-social-btns {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.auth-social-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 10px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  background: #fff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  color: #4b5563;
  transition: background 0.2s;
}

.auth-social-btn:hover {
  background: #f9fafb;
}

.auth-social-icon {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 600;
}
.auth-social-icon--wechat {
  background: #07c160;
  color: #fff;
}
.auth-social-icon--alipay {
  background: #1677ff;
  color: #fff;
}

.auth-social-name {
  font-weight: 500;
}

.auth-copyright {
  margin-top: auto;
  padding-top: 20px;
  font-size: 12px;
  color: #9ca3af;
  text-align: center;
  line-height: 1.6;
}
</style>
