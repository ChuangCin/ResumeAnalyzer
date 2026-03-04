<script setup>
import { onMounted, ref } from 'vue'
import { adminGetStats } from '../../api/api'
import { UserFilled, Document, List, DataAnalysis } from '@element-plus/icons-vue'

const loading = ref(true)
const error = ref('')
const stats = ref({
  userCount: 0,
  jobCount: 0,
  resumeCount: 0,
  analysisCount: 0
})

async function fetchStats() {
  loading.value = true
  error.value = ''
  try {
    const data = await adminGetStats()
    stats.value = { ...stats.value, ...data }
  } catch (e) {
    error.value = e.message || '加载统计失败'
  } finally {
    loading.value = false
  }
}

onMounted(() => fetchStats())
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <h1 class="admin-page-title">系统统计</h1>
      <p class="admin-page-subtitle">查看系统整体数据概览</p>
    </div>

    <div v-if="error" class="admin-error">{{ error }}</div>

    <div v-else-if="loading" class="admin-loading">加载中…</div>

    <div v-else class="admin-stats-grid">
      <div class="admin-stat-card">
        <div class="admin-stat-icon admin-stat-icon--user">
          <el-icon :size="28"><UserFilled /></el-icon>
        </div>
        <div class="admin-stat-content">
          <div class="admin-stat-value">{{ stats.userCount }}</div>
          <div class="admin-stat-label">用户总数</div>
        </div>
      </div>
      <div class="admin-stat-card">
        <div class="admin-stat-icon admin-stat-icon--job">
          <el-icon :size="28"><List /></el-icon>
        </div>
        <div class="admin-stat-content">
          <div class="admin-stat-value">{{ stats.jobCount }}</div>
          <div class="admin-stat-label">岗位数量</div>
        </div>
      </div>
      <div class="admin-stat-card">
        <div class="admin-stat-icon admin-stat-icon--resume">
          <el-icon :size="28"><Document /></el-icon>
        </div>
        <div class="admin-stat-content">
          <div class="admin-stat-value">{{ stats.resumeCount }}</div>
          <div class="admin-stat-label">简历数量</div>
        </div>
      </div>
      <div class="admin-stat-card">
        <div class="admin-stat-icon admin-stat-icon--analysis">
          <el-icon :size="28"><DataAnalysis /></el-icon>
        </div>
        <div class="admin-stat-content">
          <div class="admin-stat-value">{{ stats.analysisCount }}</div>
          <div class="admin-stat-label">分析记录数</div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 20px;
}

.admin-stat-card {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
  display: flex;
  align-items: center;
  gap: 20px;
}

.admin-stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.admin-stat-icon--user { background: linear-gradient(135deg, #6366f1, #4f46e5); }
.admin-stat-icon--job { background: linear-gradient(135deg, #22c55e, #16a34a); }
.admin-stat-icon--resume { background: linear-gradient(135deg, #f59e0b, #d97706); }
.admin-stat-icon--analysis { background: linear-gradient(135deg, #06b6d4, #0891b2); }

.admin-stat-value {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
}

.admin-stat-label {
  font-size: 14px;
  color: #64748b;
  margin-top: 4px;
}
</style>
