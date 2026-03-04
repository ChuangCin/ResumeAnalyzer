<script setup>
import { ref } from 'vue'
import { adminMaintenance } from '../../api/api'

const loading = ref(false)
const message = ref('')
const error = ref('')

async function runMaintenance() {
  loading.value = true
  message.value = ''
  error.value = ''
  try {
    const data = await adminMaintenance()
    message.value = data || '维护操作已执行'
  } catch (e) {
    error.value = e.message || '执行失败'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <h1 class="admin-page-title">系统维护</h1>
      <p class="admin-page-subtitle">执行系统维护与运维操作</p>
    </div>

    <div class="admin-card admin-maintenance-card">
      <p class="admin-maintenance-desc">
        系统维护模块用于执行数据清理、缓存刷新等运维操作。当前为占位实现，后续可接入实际维护接口。
      </p>
      <div v-if="message" class="admin-success">{{ message }}</div>
      <div v-if="error" class="admin-error">{{ error }}</div>
      <button
        type="button"
        class="admin-primary-btn"
        :disabled="loading"
        @click="runMaintenance"
      >
        {{ loading ? '执行中…' : '执行维护' }}
      </button>
    </div>
  </div>
</template>

<style scoped>
.admin-maintenance-card { max-width: 560px; }
.admin-maintenance-desc {
  font-size: 14px; color: #64748b; line-height: 1.6; margin: 0 0 20px;
}
.admin-success {
  padding: 12px 16px; background: #f0fdf4; color: #16a34a; border-radius: 10px; margin-bottom: 16px; font-size: 14px;
}
.admin-primary-btn {
  padding: 10px 20px; border-radius: 12px; border: none; background: #6366f1; color: #fff;
  font-size: 14px; font-weight: 500; cursor: pointer;
}
.admin-primary-btn:hover:not(:disabled) { background: #4f46e5; }
.admin-primary-btn:disabled { opacity: 0.7; cursor: not-allowed; }
</style>
