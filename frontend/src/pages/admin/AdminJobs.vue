<script setup>
import { onMounted, ref } from 'vue'
import { adminListJobsPage, adminCreateJob, adminUpdateJob, adminDeleteJob } from '../../api/api'

const list = ref([])
const loading = ref(false)
const error = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const showDialog = ref(false)
const editingId = ref(null)
const form = ref({
  jobName: '',
  jobCategory: '',
  company: '',
  city: '',
  salaryMin: null,
  salaryMax: null,
  education: '',
  experience: '',
  skills: '',
  description: '',
  requirement: ''
})

function openAdd() {
  editingId.value = null
  form.value = {
    jobName: '', jobCategory: '', company: '', city: '',
    salaryMin: null, salaryMax: null, education: '', experience: '',
    skills: '', description: '', requirement: ''
  }
  showDialog.value = true
}

function openEdit(job) {
  editingId.value = job.id
  form.value = {
    jobName: job.jobName || '',
    jobCategory: job.jobCategory || '',
    company: job.company || '',
    city: job.city || '',
    salaryMin: job.salaryMin ?? null,
    salaryMax: job.salaryMax ?? null,
    education: job.education || '',
    experience: job.experience || '',
    skills: job.skills || '',
    description: job.description || '',
    requirement: job.requirement || ''
  }
  showDialog.value = true
}

function closeDialog() {
  showDialog.value = false
}

async function submitJob() {
  error.value = ''
  if (!form.value.jobName?.trim()) {
    error.value = '请填写岗位名称'
    return
  }
  try {
    const payload = { ...form.value }
    if (editingId.value) {
      await adminUpdateJob(editingId.value, payload)
    } else {
      await adminCreateJob(payload)
    }
    closeDialog()
    await fetchList()
  } catch (e) {
    error.value = e.message || '保存失败'
  }
}

async function doDelete(job) {
  if (!confirm(`确定要删除岗位「${job.jobName}」吗？`)) return
  try {
    await adminDeleteJob(job.id)
    await fetchList()
  } catch (e) {
    error.value = e.message || '删除失败'
  }
}

async function fetchList() {
  loading.value = true
  error.value = ''
  try {
    const data = await adminListJobsPage(page.value, pageSize.value)
    list.value = Array.isArray(data?.list) ? data.list : []
    total.value = Number(data?.total) || 0
  } catch (e) {
    error.value = e.message || '加载岗位列表失败'
    list.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function onPageChange(p) {
  page.value = p
  fetchList()
}

function onSizeChange(s) {
  pageSize.value = s
  page.value = 1
  fetchList()
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
    <div class="admin-page-header admin-page-header--row">
      <div>
        <h1 class="admin-page-title">岗位管理</h1>
        <p class="admin-page-subtitle">管理招聘岗位信息</p>
      </div>
      <button type="button" class="admin-primary-btn" @click="openAdd">新增岗位</button>
    </div>

    <div v-if="error" class="admin-error">{{ error }}</div>

    <div class="admin-card">
      <div v-if="loading" class="admin-loading">加载中…</div>
      <div v-else class="admin-table-wrap">
        <table class="admin-table">
          <thead>
            <tr>
              <th>岗位名称</th>
              <th>分类</th>
              <th>公司</th>
              <th>城市</th>
              <th>薪资范围</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="job in list" :key="job.id" class="admin-table-row">
              <td>{{ job.jobName || '--' }}</td>
              <td>{{ job.jobCategory || '--' }}</td>
              <td>{{ job.company || '--' }}</td>
              <td>{{ job.city || '--' }}</td>
              <td>
                <span v-if="job.salaryMin != null || job.salaryMax != null">
                  {{ job.salaryMin ?? '?' }} - {{ job.salaryMax ?? '?' }} K
                </span>
                <span v-else>--</span>
              </td>
              <td>{{ formatTime(job.createTime) }}</td>
              <td>
                <button type="button" class="admin-btn" @click="openEdit(job)">编辑</button>
                <button type="button" class="admin-btn admin-btn--danger" @click="doDelete(job)">删除</button>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!list.length && !loading" class="admin-empty">暂无岗位</div>
        <div v-if="total > 0" class="admin-pagination-wrap">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next"
            @current-change="onPageChange"
            @size-change="onSizeChange"
          />
        </div>
      </div>
    </div>

    <div v-if="showDialog" class="admin-dialog-mask" @click.self="closeDialog">
      <div class="admin-dialog">
        <h3 class="admin-dialog-title">{{ editingId ? '编辑岗位' : '新增岗位' }}</h3>
        <div class="admin-form">
          <div class="admin-form-row">
            <label>岗位名称</label>
            <input v-model="form.jobName" placeholder="请输入岗位名称" />
          </div>
          <div class="admin-form-row">
            <label>分类</label>
            <input v-model="form.jobCategory" placeholder="如：技术" />
          </div>
          <div class="admin-form-row">
            <label>公司</label>
            <input v-model="form.company" placeholder="公司名称" />
          </div>
          <div class="admin-form-row">
            <label>城市</label>
            <input v-model="form.city" placeholder="工作城市" />
          </div>
          <div class="admin-form-row-group">
            <div class="admin-form-row">
              <label>最低薪资(K)</label>
              <input v-model.number="form.salaryMin" type="number" placeholder="如 15" />
            </div>
            <div class="admin-form-row">
              <label>最高薪资(K)</label>
              <input v-model.number="form.salaryMax" type="number" placeholder="如 30" />
            </div>
          </div>
          <div class="admin-form-row">
            <label>学历要求</label>
            <input v-model="form.education" placeholder="如：本科" />
          </div>
          <div class="admin-form-row">
            <label>经验要求</label>
            <input v-model="form.experience" placeholder="如：3-5年" />
          </div>
          <div class="admin-form-row">
            <label>技能要求</label>
            <textarea v-model="form.skills" placeholder="技能关键词，逗号分隔" rows="2" />
          </div>
          <div class="admin-form-row">
            <label>岗位描述</label>
            <textarea v-model="form.description" placeholder="岗位描述" rows="3" />
          </div>
          <div class="admin-form-row">
            <label>任职要求</label>
            <textarea v-model="form.requirement" placeholder="任职要求" rows="3" />
          </div>
        </div>
        <div class="admin-dialog-actions">
          <button type="button" class="admin-btn" @click="closeDialog">取消</button>
          <button type="button" class="admin-btn admin-btn--primary" @click="submitJob">保存</button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-page-header--row { display: flex; align-items: center; justify-content: space-between; gap: 16px; }
.admin-primary-btn {
  padding: 10px 20px; border-radius: 12px; border: none; background: #6366f1; color: #fff;
  font-size: 14px; font-weight: 500; cursor: pointer;
}
.admin-primary-btn:hover { background: #4f46e5; }
.admin-table-wrap { overflow-x: auto; }
.admin-table { width: 100%; border-collapse: collapse; font-size: 14px; }
.admin-table th {
  padding: 12px 16px; text-align: left; font-weight: 600; color: #64748b;
  background: #f8fafc; border-bottom: 1px solid #e2e8f0;
}
.admin-table td { padding: 12px 16px; color: #334155; border-bottom: 1px solid #f1f5f9; }
.admin-table-row:last-child td { border-bottom: none; }
.admin-btn { padding: 6px 12px; margin-right: 8px; border-radius: 8px; border: 1px solid #e2e8f0; background: #fff; font-size: 13px; cursor: pointer; }
.admin-btn--primary { background: #6366f1; color: #fff; border-color: #6366f1; }
.admin-btn--danger { color: #dc2626; border-color: #fecaca; }
.admin-empty { padding: 40px; text-align: center; color: #94a3b8; }

.admin-dialog-mask {
  position: fixed; inset: 0; background: rgba(0,0,0,0.4); z-index: 1000;
  display: flex; align-items: center; justify-content: center; padding: 20px;
}
.admin-dialog {
  background: #fff; border-radius: 20px; padding: 24px; max-width: 520px; width: 100%; max-height: 90vh; overflow-y: auto;
  box-shadow: 0 25px 50px -12px rgba(0,0,0,0.25);
}
.admin-dialog-title { font-size: 18px; font-weight: 600; margin: 0 0 20px; color: #1e293b; }
.admin-form { display: flex; flex-direction: column; gap: 14px; }
.admin-form-row { display: flex; flex-direction: column; gap: 6px; }
.admin-form-row label { font-size: 13px; font-weight: 500; color: #475569; }
.admin-form-row input, .admin-form-row textarea {
  padding: 10px 12px; border: 1px solid #e2e8f0; border-radius: 10px; font-size: 14px;
}
.admin-form-row-group { display: flex; gap: 12px; }
.admin-form-row-group .admin-form-row { flex: 1; }
.admin-dialog-actions { margin-top: 24px; display: flex; justify-content: flex-end; gap: 12px; }
.admin-pagination-wrap { margin-top: 16px; display: flex; justify-content: flex-end; }
</style>
