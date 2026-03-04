<script setup>
import { onMounted, ref } from 'vue'
import { adminListUsers, adminUpdateUser, adminDeleteUser } from '../../api/api'

const list = ref([])
const loading = ref(false)
const error = ref('')
const editingId = ref(null)
const editForm = ref({ phone: '', email: '', username: '' })

async function fetchList() {
  loading.value = true
  error.value = ''
  try {
    const data = await adminListUsers()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '加载用户列表失败'
    list.value = []
  } finally {
    loading.value = false
  }
}

function startEdit(user) {
  editingId.value = user.id
  editForm.value = {
    phone: user.phone || '',
    email: user.email || '',
    username: user.username || ''
  }
}

function cancelEdit() {
  editingId.value = null
}

async function saveEdit() {
  if (!editingId.value) return
  try {
    await adminUpdateUser(editingId.value, {
      phone: editForm.value.phone,
      email: editForm.value.email,
      username: editForm.value.username
    })
    cancelEdit()
    await fetchList()
  } catch (e) {
    error.value = e.message || '保存失败'
  }
}

async function doDelete(user) {
  const name = user.phone || user.email || user.username || '该用户'
  if (!confirm(`确定要删除用户「${name}」吗？`)) return
  try {
    await adminDeleteUser(user.id)
    await fetchList()
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

function formatTime(t) {
  if (!t) return '--'
  const d = new Date(t)
  return isNaN(d.getTime()) ? t : d.toLocaleString('zh-CN')
}

onMounted(() => fetchList())
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <h1 class="admin-page-title">用户管理</h1>
      <p class="admin-page-subtitle">管理所有用户信息</p>
    </div>

    <div v-if="error" class="admin-error">{{ error }}</div>

    <div class="admin-card">
      <div v-if="loading" class="admin-loading">加载中…</div>
      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>手机号（账号）</th>
              <th>邮箱</th>
              <th>昵称</th>
              <th>角色</th>
              <th>注册时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="user in list" :key="user.id" class="admin-table-row">
              <td>{{ user.id }}</td>
              <td>
                <template v-if="editingId === user.id">
                  <input v-model="editForm.phone" class="admin-inline-input" type="tel" placeholder="手机号" />
                </template>
                <span v-else>{{ user.phone || '--' }}</span>
              </td>
              <td>
                <template v-if="editingId === user.id">
                  <input v-model="editForm.email" class="admin-inline-input" type="email" />
                </template>
                <span v-else>{{ user.email || '--' }}</span>
              </td>
              <td>
                <template v-if="editingId === user.id">
                  <input v-model="editForm.username" class="admin-inline-input" placeholder="昵称" />
                </template>
                <span v-else>{{ user.username || '--' }}</span>
              </td>
              <td>
                <span class="admin-tag" :class="user.role === 'admin' ? 'admin-tag--admin' : 'admin-tag--user'">
                  {{ user.role === 'admin' ? '管理员' : '普通用户' }}
                </span>
              </td>
              <td>{{ formatTime(user.createTime) }}</td>
              <td>
                <template v-if="editingId === user.id">
                  <button type="button" class="admin-btn admin-btn--primary" @click="saveEdit">保存</button>
                  <button type="button" class="admin-btn" @click="cancelEdit">取消</button>
                </template>
                <template v-else>
                  <button type="button" class="admin-btn" @click="startEdit(user)">编辑</button>
                  <button
                    v-if="user.role !== 'admin'"
                    type="button"
                    class="admin-btn admin-btn--danger"
                    @click="doDelete(user)"
                  >
                    删除
                  </button>
                </template>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!list.length && !loading" class="admin-empty">暂无用户</div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-table-wrap { overflow-x: auto; }
.admin-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.admin-table th {
  padding: 12px 16px; text-align: left; font-weight: 600; color: #64748b;
  background: #f8fafc; border-bottom: 1px solid #e2e8f0;
}
.admin-table td {
  padding: 12px 16px; color: #334155; border-bottom: 1px solid #f1f5f9;
}
.admin-table-row:last-child td { border-bottom: none; }
.admin-inline-input {
  padding: 6px 10px; border: 1px solid #e2e8f0; border-radius: 8px; font-size: 14px; width: 140px;
}
.admin-tag {
  display: inline-block; padding: 2px 8px; border-radius: 6px; font-size: 12px;
}
.admin-tag--admin { background: #ddd6fe; color: #5b21b6; }
.admin-tag--user { background: #e0e7ff; color: #3730a3; }
.admin-btn {
  padding: 6px 12px; margin-right: 8px; border-radius: 8px; border: 1px solid #e2e8f0;
  background: #fff; font-size: 13px; cursor: pointer;
}
.admin-btn--primary { background: #6366f1; color: #fff; border-color: #6366f1; }
.admin-btn--danger { color: #dc2626; border-color: #fecaca; }
.admin-btn--danger:hover { background: #fef2f2; }
.admin-empty { padding: 40px; text-align: center; color: #94a3b8; }
</style>
