<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import {
  ArrowRight,
  Document,
  MagicStick,
  Setting,
  User,
  UserFilled,
  DataAnalysis,
  List,
  Message,
  SwitchButton
} from '@element-plus/icons-vue'
import { avatarUrl } from '../../api/api'

const route = useRoute()
const router = useRouter()

const username = computed(() => localStorage.getItem('username') || '管理员')
const headerAvatarUrl = computed(() => avatarUrl(localStorage.getItem('avatar')))

const navItems = [
  { path: '/admin/profile', label: '个人中心', sub: '管理个人信息', icon: User },
  { path: '/admin/users', label: '用户管理', sub: '管理用户信息', icon: UserFilled },
  { path: '/admin/jobs', label: '岗位管理', sub: '管理岗位信息', icon: List },
  { path: '/admin/analysis', label: '简历分析结果', sub: '查看分析结果', icon: Document },
  { path: '/admin/stats', label: '系统统计', sub: '查看统计数据', icon: DataAnalysis },
  { path: '/admin/maintenance', label: '系统维护', sub: '系统维护', icon: Setting }
]

function isActive(path) {
  return route.path === path
}

function handleCommand(cmd) {
  if (cmd === 'profile') router.push('/admin/profile')
  else if (cmd === 'messages') router.push('/admin/messages')
  else if (cmd === 'logout') {
    localStorage.removeItem('token')
    localStorage.removeItem('userId')
    localStorage.removeItem('username')
    localStorage.removeItem('role')
    localStorage.removeItem('avatar')
    router.push('/login')
  }
}

</script>

<template>
  <div class="admin-layout">
    <aside class="admin-sidebar">
      <div class="admin-logo">
        <div class="admin-logo-icon">
          <el-icon :size="18"><MagicStick /></el-icon>
        </div>
        <div class="admin-logo-text">
          <div class="admin-logo-title">管理后台</div>
          <div class="admin-logo-sub">AI 智能面试助手</div>
        </div>
      </div>

      <nav class="admin-menu">
        <div class="admin-menu-section-title">功能模块</div>
        <button
          v-for="item in navItems"
          :key="item.path"
          type="button"
          class="admin-menu-item"
          :class="{ 'admin-menu-item--active': isActive(item.path) }"
          @click="router.push(item.path)"
        >
          <div class="admin-menu-left">
            <div class="admin-menu-icon-circle">
              <el-icon class="admin-menu-icon"><component :is="item.icon" /></el-icon>
            </div>
            <div class="admin-menu-text">
              <div class="admin-menu-title">{{ item.label }}</div>
              <div class="admin-menu-sub">{{ item.sub }}</div>
            </div>
          </div>
          <el-icon v-if="isActive(item.path)" class="admin-menu-arrow"><ArrowRight /></el-icon>
        </button>
      </nav>
    </aside>

    <main class="admin-main">
      <header class="admin-main-header">
        <div class="admin-main-header-placeholder" />
        <div class="admin-main-header-user">
          <el-dropdown trigger="hover" @command="handleCommand">
            <div class="admin-avatar" aria-haspopup="true" :title="username">
              <img v-if="headerAvatarUrl" class="admin-avatar-img" :src="headerAvatarUrl" alt="头像" />
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
      </header>
      <div class="admin-main-body">
        <router-view />
      </div>
    </main>
  </div>
</template>

<style scoped>
.admin-layout {
  display: grid;
  grid-template-columns: 240px minmax(0, 1fr);
  width: 100%;
  max-width: 1280px;
  min-height: 100vh;
  margin: 0 auto;
}

.admin-sidebar {
  padding: 24px 18px;
  background: linear-gradient(180deg, #4f46e5, #6366f1);
  border-radius: 24px;
  margin: 20px;
  margin-right: 20px;
  color: #e5e7ff;
  display: flex;
  flex-direction: column;
  box-shadow: 0 18px 40px rgba(79, 70, 229, 0.35);
}

.admin-logo {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 28px;
}

.admin-logo-icon {
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

.admin-logo-title { font-size: 15px; font-weight: 600; }
.admin-logo-sub { font-size: 11px; opacity: 0.85; }

.admin-menu {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.admin-menu-section-title {
  font-size: 11px;
  letter-spacing: 0.06em;
  color: rgba(226, 232, 255, 0.9);
  margin: 4px 4px 8px;
}

.admin-menu-item {
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
  transition: background 0.2s;
}

.admin-menu-item:hover {
  background: rgba(248, 250, 252, 0.1);
}

.admin-menu-item--active {
  background: rgba(248, 250, 252, 0.2);
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.22);
}

.admin-menu-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-menu-icon-circle {
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: rgba(248, 250, 252, 0.18);
  display: flex;
  align-items: center;
  justify-content: center;
}

.admin-menu-icon { font-size: 16px; }
.admin-menu-title { font-size: 13px; font-weight: 500; }
.admin-menu-sub { font-size: 11px; opacity: 0.85; }
.admin-menu-arrow { font-size: 18px; opacity: 0.8; }

.admin-main {
  padding: 0 20px 40px;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin-main-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 16px 0 12px;
  flex-shrink: 0;
}

.admin-main-header-placeholder {
  flex: 1;
}

.admin-main-header-user {
  flex-shrink: 0;
}

.admin-avatar {
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

.admin-avatar:hover {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.4);
}

.admin-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.admin-main-body {
  flex: 1;
  min-width: 0;
  padding-top: 0;
}

.admin-page-header { margin-bottom: 24px; }
.admin-page-title { font-size: 24px; font-weight: 600; color: #1e293b; margin: 0 0 8px; }
.admin-page-subtitle { font-size: 14px; color: #64748b; margin: 0; }
.admin-card {
  background: #fff; border-radius: 20px; padding: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
}
.admin-error {
  padding: 16px; color: #dc2626; background: #fef2f2; border-radius: 12px; margin-bottom: 20px;
}
.admin-loading { padding: 40px; text-align: center; color: #64748b; }
</style>
