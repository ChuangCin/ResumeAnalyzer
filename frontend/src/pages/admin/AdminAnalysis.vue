<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { adminListAnalysis } from '../../api/api'

const router = useRouter()
const list = ref([])
const loading = ref(false)
const error = ref('')

async function fetchList() {
  loading.value = true
  error.value = ''
  try {
    const data = await adminListAnalysis()
    list.value = Array.isArray(data) ? data : []
  } catch (e) {
    error.value = e.message || '加载分析结果失败'
    list.value = []
  } finally {
    loading.value = false
  }
}

function formatTime(t) {
  if (!t) return '--'
  const d = new Date(t)
  return isNaN(d.getTime()) ? t : d.toLocaleString('zh-CN')
}

function summaryShort(summary) {
  if (!summary) return '--'
  return summary.length > 80 ? summary.slice(0, 80) + '…' : summary
}

function goDetail(row) {
  if (row && row.id) router.push('/admin/analysis/' + row.id)
}

onMounted(() => fetchList())
</script>

<template>
  <div class="admin-page">
    <div class="admin-page-header">
      <h1 class="admin-page-title">简历分析结果</h1>
      <p class="admin-page-subtitle">点击某行查看详情，可向分析结果好的用户发起面试通知</p>
    </div>

    <div v-if="error" class="admin-error">{{ error }}</div>

    <div class="admin-card">
      <div v-if="loading" class="admin-loading">加载中…</div>
      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>用户名</th>
              <th>综合得分</th>
              <th>核心评价摘要</th>
              <th>分析时间</th>
            </tr>
          </thead>
          <tbody>
            <tr
              v-for="row in list"
              :key="row.id"
              class="admin-table-row admin-table-row--clickable"
              @click="goDetail(row)"
            >
              <td>{{ row.username || '--' }}</td>
              <td>
                <span class="admin-score" :class="row.score >= 80 ? 'admin-score--high' : row.score >= 60 ? 'admin-score--mid' : 'admin-score--low'">
                  {{ row.score ?? '--' }}
                </span>
              </td>
              <td class="admin-cell-summary">{{ summaryShort(row.summary) }}</td>
              <td>{{ formatTime(row.createTime) }}</td>
            </tr>
          </tbody>
        </table>
        <div v-if="!list.length && !loading" class="admin-empty">暂无分析记录</div>
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
.admin-table td { padding: 12px 16px; color: #334155; border-bottom: 1px solid #f1f5f9; }
.admin-table-row:last-child td { border-bottom: none; }
.admin-table-row--clickable { cursor: pointer; }
.admin-table-row--clickable:hover { background: #f8fafc; }
.admin-score { font-weight: 600; }
.admin-score--high { color: #22c55e; }
.admin-score--mid { color: #f59e0b; }
.admin-score--low { color: #ef4444; }
.admin-cell-summary { max-width: 320px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.admin-empty { padding: 40px; text-align: center; color: #94a3b8; }
</style>
