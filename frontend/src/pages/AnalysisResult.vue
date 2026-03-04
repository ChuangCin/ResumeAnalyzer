<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getAnalysisByResumeId, startAnalysis, getMatchByResumeId, runMatchForResume } from '../api/api'
import * as echarts from 'echarts'
import { ArrowLeft, Download, Microphone, Document, Position } from '@element-plus/icons-vue'
import html2canvas from 'html2canvas'
import { jsPDF } from 'jspdf'

const route = useRoute()
const router = useRouter()
const resumeId = computed(() => route.params.id || '1')

const activeTab = ref('analysis')
const loading = ref(false)
const error = ref('')
const analysis = ref(null)
const radarChartRef = ref(null)
let chartInstance = null

const matchLoading = ref(false)
const matchError = ref('')
const matchList = ref([])
const matchBarChartRef = ref(null)
let matchBarChartInstance = null

const scoreItems = computed(() => {
  if (!analysis.value) return []
  const a = analysis.value
  const items = [
    { label: '项目经验', value: a.experienceScore ?? 0, max: 40, color: '#8b5cf6' },
    { label: '技能匹配', value: a.skillScore ?? 0, max: 20, color: '#3b82f6' },
    { label: '内容完整性', value: a.completenessScore ?? 0, max: 15, color: '#22c55e' },
    { label: '结构清晰度', value: a.structureScore ?? 0, max: 15, color: '#06b6d4' },
    { label: '表达专业性', value: a.expressionScore ?? 0, max: 10, color: '#f59e0b' }
  ]
  return items
})

const advantages = computed(() => {
  const h = analysis.value?.highlights
  if (!h || typeof h !== 'string') return []
  return h
    .split(/[,，。、；;]/)
    .map(s => s.trim())
    .filter(Boolean)
})

// 改进建议：高/中/低，兼容 category 字段
const suggestionHigh = computed(() => {
  const arr = analysis.value?.suggestions?.high
  return Array.isArray(arr) ? arr : []
})
const suggestionMedium = computed(() => {
  const arr = analysis.value?.suggestions?.medium
  return Array.isArray(arr) ? arr : []
})
const suggestionLow = computed(() => {
  const arr = analysis.value?.suggestions?.low
  return Array.isArray(arr) ? arr : []
})
const suggestionTotal = computed(() => {
  return suggestionHigh.value.length + suggestionMedium.value.length + suggestionLow.value.length
})

function normalizeAnalysis(raw) {
  if (!raw || typeof raw !== 'object') return raw
  if (typeof raw.suggestions === 'string') {
    try {
      raw = { ...raw, suggestions: JSON.parse(raw.suggestions) }
    } catch (_) {
      raw = { ...raw, suggestions: { high: [], medium: [], low: [] } }
    }
  }
  return raw
}

async function ensureAnalysis(id) {
  let raw = null
  try {
    const data = await getAnalysisByResumeId(id)
    raw = Array.isArray(data) ? data[0] : data
  } catch (e) {
    if (e.message === '没有分析结果') {
      raw = await startAnalysis(id)
      raw = normalizeAnalysis(raw)
    } else {
      throw e
    }
  }
  if (!raw || typeof raw !== 'object') {
    raw = await startAnalysis(id)
    raw = normalizeAnalysis(raw)
  }
  if (raw && typeof raw === 'object') {
    return raw
  }
  throw new Error('无分析数据')
}

async function ensureMatch(id) {
  try {
    const list = await getMatchByResumeId(id)
    if (Array.isArray(list) && list.length > 0) return
    await runMatchForResume(id)
  } catch (e) {
    console.warn('岗位匹配未完成:', e.message)
  }
}

async function fetchAnalysis(id) {
  if (!id) return
  loading.value = true
  error.value = ''
  analysis.value = null

  try {
    const [raw] = await Promise.all([ensureAnalysis(id), ensureMatch(id)])
    if (raw && typeof raw === 'object') {
      analysis.value = raw
    } else {
      throw new Error('无分析数据')
    }
  } catch (e) {
    error.value = e.message || '加载分析结果失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function formatTime(t) {
  if (!t) return '--'
  const d = new Date(t)
  if (isNaN(d.getTime())) return t
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return `${y}/${m}/${day} ${h}:${min}`
}

function percentage(value, max) {
  if (!max || max <= 0) return 0
  return Math.round((value / max) * 100)
}

function initRadarChart() {
  if (!radarChartRef.value || !analysis.value) return
  if (chartInstance) chartInstance.dispose()

  chartInstance = echarts.init(radarChartRef.value)
  const a = analysis.value

  const maxValues = [10, 20, 15, 15, 40]
  const rawValues = [
    a.expressionScore ?? 0,
    a.skillScore ?? 0,
    a.completenessScore ?? 0,
    a.structureScore ?? 0,
    a.experienceScore ?? 0
  ]
  const values = rawValues.map((v, i) => (maxValues[i] > 0 ? (v / maxValues[i]) * 100 : 0))

  const indicators = [
    { name: '表达专业性', max: 100 },
    { name: '技能匹配', max: 100 },
    { name: '内容完整性', max: 100 },
    { name: '结构清晰度', max: 100 },
    { name: '项目经验', max: 100 }
  ]

  chartInstance.setOption({
    tooltip: {
      trigger: 'item',
      formatter: () =>
        indicators
          .map((ind, i) => `${ind.name}: ${rawValues[i]}/${maxValues[i]}`)
          .join('<br/>')
    },
    radar: {
      indicator: indicators,
      shape: 'polygon',
      splitNumber: 4,
      alignTicks: false,
      axisName: {
        color: '#64748b',
        fontSize: 12
      },
      splitArea: {
        areaStyle: { color: ['rgba(148,163,184,0.06)', 'rgba(148,163,184,0.12)'] }
      },
      splitLine: {
        lineStyle: { color: 'rgba(148,163,184,0.3)' }
      },
      axisLine: {
        lineStyle: { color: 'rgba(148,163,184,0.4)' }
      }
    },
    series: [
      {
        type: 'radar',
        data: [
          {
            value: values,
            name: '综合评分',
            areaStyle: { color: 'rgba(139,92,246,0.35)' },
            lineStyle: { color: '#8b5cf6', width: 2 },
            itemStyle: { color: '#8b5cf6' }
          }
        ]
      }
    ]
  })
}

function resizeChart() {
  chartInstance?.resize()
}

const matchAverageScore = computed(() => {
  const list = matchList.value
  if (!list.length) return null
  const sum = list.reduce((s, m) => s + (m.matchScore ?? 0), 0)
  return Math.round(sum / list.length)
})

async function fetchMatch(id) {
  if (!id) return
  matchLoading.value = true
  matchError.value = ''
  matchList.value = []
  try {
    const data = await getMatchByResumeId(id)
    matchList.value = Array.isArray(data) ? data : []
  } catch (e) {
    matchError.value = e.message || '加载岗位匹配失败'
  } finally {
    matchLoading.value = false
  }
}

function initMatchBarChart() {
  if (!matchBarChartRef.value || !matchList.value?.length) return
  if (matchBarChartInstance) matchBarChartInstance.dispose()
  matchBarChartInstance = echarts.init(matchBarChartRef.value)
  const names = matchList.value.map(m => m.jobName || '岗位')
  const scores = matchList.value.map(m => m.matchScore ?? 0)
  matchBarChartInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: '12%', right: '8%', top: '10%', bottom: '15%', containLabel: true },
    xAxis: {
      type: 'category',
      data: names,
      axisLabel: { rotate: names.length > 4 ? 25 : 0 }
    },
    yAxis: {
      type: 'value',
      min: 0,
      max: 100,
      axisLabel: { formatter: '{value}' }
    },
    series: [
      {
        type: 'bar',
        data: scores,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: '#8b5cf6' },
            { offset: 1, color: '#6366f1' }
          ])
        },
        barWidth: '50%'
      }
    ]
  })
}

function resizeMatchBarChart() {
  matchBarChartInstance?.resize()
}

const exportPdfLoading = ref(false)

function escapeHtml(s) {
  if (s == null || s === '') return ''
  const str = String(s)
  return str
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function buildPdfHtml() {
  const a = analysis.value
  if (!a) return ''
  const chartDataUrl = chartInstance ? chartInstance.getDataURL({ type: 'png', pixelRatio: 2 }) : ''
  const scoreRows = scoreItems.value
    .map(
      item =>
        `<tr><td style="padding:6px 12px;border-bottom:1px solid #e2e8f0;">${item.label}</td><td style="padding:6px 12px;border-bottom:1px solid #e2e8f0;">${item.value}/${item.max}</td></tr>`
    )
    .join('')
  const advantageTags = advantages.value
    .map(v => `<span style="display:inline-block;margin:4px 6px 4px 0;padding:4px 10px;font-size:12px;color:#166534;background:#dcfce7;border-radius:999px;">${escapeHtml(v)}</span>`)
    .join('')
  const highList = suggestionHigh.value
    .map(
      item =>
        `<div style="margin-bottom:12px;padding:12px;border-left:3px solid #dc2626;background:#fef2f2;border-radius:8px;">
          ${item.category ? `<span style="font-size:11px;color:#fff;background:#dc2626;padding:2px 6px;border-radius:4px;">${escapeHtml(item.category)}</span><br/>` : ''}
          <strong style="font-size:13px;">${escapeHtml(item.title || '')}</strong><br/>
          <span style="font-size:12px;color:#475569;line-height:1.5;">${escapeHtml(item.content || '').replace(/\n/g, '<br/>')}</span>
        </div>`
    )
    .join('')
  const mediumList = suggestionMedium.value
    .map(
      item =>
        `<div style="margin-bottom:12px;padding:12px;border-left:3px solid #eab308;background:#fffbeb;border-radius:8px;">
          ${item.category ? `<span style="font-size:11px;color:#1e293b;background:#eab308;padding:2px 6px;border-radius:4px;">${escapeHtml(item.category)}</span><br/>` : ''}
          <strong style="font-size:13px;">${escapeHtml(item.title || '')}</strong><br/>
          <span style="font-size:12px;color:#475569;line-height:1.5;">${escapeHtml(item.content || '').replace(/\n/g, '<br/>')}</span>
        </div>`
    )
    .join('')
  const lowList = suggestionLow.value
    .map(
      item =>
        `<div style="margin-bottom:12px;padding:12px;border-left:3px solid #3b82f6;background:#eff6ff;border-radius:8px;">
          ${item.category ? `<span style="font-size:11px;color:#fff;background:#3b82f6;padding:2px 6px;border-radius:4px;">${escapeHtml(item.category)}</span><br/>` : ''}
          <strong style="font-size:13px;">${escapeHtml(item.title || '')}</strong><br/>
          <span style="font-size:12px;color:#475569;line-height:1.5;">${escapeHtml(item.content || '').replace(/\n/g, '<br/>')}</span>
        </div>`
    )
    .join('')

  return `
    <div style="font-family:system-ui,'PingFang SC',sans-serif;padding:24px;color:#1e293b;width:752px;min-height:400px;box-sizing:border-box;">
      <h1 style="font-size:20px;margin:0 0 20px;color:#0f766e;">简历分析报告</h1>
      <p style="font-size:12px;color:#64748b;margin:0 0 16px;">分析时间：${escapeHtml(formatTime(a.createTime))}</p>

      <h2 style="font-size:16px;margin:16px 0 8px;color:#334155;">一、核心评价</h2>
      <p style="font-size:14px;line-height:1.7;color:#475569;margin:0 0 12px;padding:12px;background:#f8fafc;border-radius:8px;">${escapeHtml(a.summary || '').replace(/\n/g, '<br/>')}</p>
      <p style="font-size:18px;font-weight:700;color:#0f766e;margin:0 0 16px;">综合得分：${a.score ?? 0}/100</p>

      <h2 style="font-size:16px;margin:16px 0 8px;color:#334155;">二、优势亮点</h2>
      <div style="margin-bottom:16px;">${advantageTags || '—'}</div>

      <h2 style="font-size:16px;margin:16px 0 8px;color:#334155;">三、多维评分</h2>
      ${chartDataUrl ? `<img src="${chartDataUrl}" style="max-width:100%;height:auto;margin-bottom:12px;" alt="雷达图" />` : ''}
      <table style="width:100%;border-collapse:collapse;font-size:13px;">
        <thead><tr style="background:#f8fafc;"><th style="padding:8px 12px;text-align:left;">维度</th><th style="padding:8px 12px;text-align:left;">得分</th></tr></thead>
        <tbody>${scoreRows}</tbody>
      </table>

      ${suggestionTotal.value > 0 ? `
      <h2 style="font-size:16px;margin:20px 0 8px;color:#334155;">四、改进建议（${suggestionTotal.value}条）</h2>
      ${suggestionHigh.value.length ? `<h3 style="font-size:14px;margin:12px 0 6px;color:#dc2626;">高优先级（${suggestionHigh.value.length}）</h3>${highList}` : ''}
      ${suggestionMedium.value.length ? `<h3 style="font-size:14px;margin:12px 0 6px;color:#eab308;">中优先级（${suggestionMedium.value.length}）</h3>${mediumList}` : ''}
      ${suggestionLow.value.length ? `<h3 style="font-size:14px;margin:12px 0 6px;color:#3b82f6;">低优先级（${suggestionLow.value.length}）</h3>${lowList}` : ''}
      ` : ''}
    </div>
  `
}

async function exportAnalysisPdf() {
  if (!analysis.value) return
  exportPdfLoading.value = true
  let overlayEl = null
  let wrapper = null
  try {
    const content = buildPdfHtml()
    overlayEl = document.createElement('div')
    overlayEl.style.cssText = 'position:fixed;inset:0;z-index:10000;background:rgba(0,0,0,0.5);display:flex;align-items:center;justify-content:center;font-size:16px;color:#fff;'
    overlayEl.textContent = '正在生成 PDF...'
    document.body.appendChild(overlayEl)

    wrapper = document.createElement('div')
    wrapper.style.cssText = 'position:fixed;left:20px;top:20px;width:800px;min-height:400px;z-index:10001;background:#fff;padding:0;box-sizing:border-box;'
    wrapper.innerHTML = content
    document.body.appendChild(wrapper)

    await new Promise(r => requestAnimationFrame(() => setTimeout(r, 150)))
    const imgs = wrapper.querySelectorAll('img')
    await Promise.all(
      Array.from(imgs).map(
        img =>
          new Promise(resolve => {
            if (img.complete) resolve()
            else img.onload = resolve
            img.onerror = resolve
          })
      )
    )

    const canvas = await html2canvas(wrapper, {
      scale: 2,
      useCORS: true,
      allowTaint: true,
      backgroundColor: '#ffffff',
      logging: false
    })

    const pdf = new jsPDF('p', 'mm', 'a4')
    const pageW = pdf.internal.pageSize.getWidth()
    const pageH = pdf.internal.pageSize.getHeight()
    const margin = 10
    const contentW = pageW - 2 * margin
    const contentWpx = canvas.width
    const contentHpx = canvas.height
    const scaleMmPerPx = contentW / contentWpx
    const totalHmm = contentHpx * scaleMmPerPx
    const sliceHmm = pageH - 2 * margin
    const numPages = Math.ceil(totalHmm / sliceHmm) || 1
    const sliceHpx = (sliceHmm / scaleMmPerPx)

    for (let i = 0; i < numPages; i++) {
      if (i > 0) pdf.addPage()
      const srcY = i * sliceHpx
      const srcHeight = Math.min(sliceHpx, contentHpx - srcY)
      const sliceCanvas = document.createElement('canvas')
      sliceCanvas.width = contentWpx
      sliceCanvas.height = Math.round(srcHeight)
      const ctx = sliceCanvas.getContext('2d')
      ctx.fillStyle = '#ffffff'
      ctx.fillRect(0, 0, sliceCanvas.width, sliceCanvas.height)
      ctx.drawImage(canvas, 0, srcY, contentWpx, srcHeight, 0, 0, contentWpx, srcHeight)
      const imgData = sliceCanvas.toDataURL('image/jpeg', 0.92)
      const drawH = (sliceCanvas.height * scaleMmPerPx)
      pdf.addImage(imgData, 'JPEG', margin, margin, contentW, drawH)
    }

    pdf.save(`简历分析报告_${resumeId.value}_${new Date().toISOString().slice(0, 10)}.pdf`)
  } catch (e) {
    console.error('导出 PDF 失败:', e)
  } finally {
    if (wrapper && wrapper.parentNode) document.body.removeChild(wrapper)
    if (overlayEl && overlayEl.parentNode) document.body.removeChild(overlayEl)
    exportPdfLoading.value = false
  }
}

function switchTab(tab) {
  activeTab.value = tab
  if (tab === 'match') {
    fetchMatch(resumeId.value)
  }
}

onMounted(() => {
  fetchAnalysis(resumeId.value)
  window.addEventListener('resize', resizeChart)
  window.addEventListener('resize', resizeMatchBarChart)
})

onUnmounted(() => {
  window.removeEventListener('resize', resizeChart)
  window.removeEventListener('resize', resizeMatchBarChart)
  chartInstance?.dispose()
  matchBarChartInstance?.dispose()
})

watch(
  () => [resumeId.value, analysis.value, activeTab.value],
  async () => {
    if (activeTab.value === 'analysis' && analysis.value) {
      await nextTick()
      initRadarChart()
    }
  },
  { flush: 'post' }
)

watch(
  () => [activeTab.value, matchList.value],
  async () => {
    if (activeTab.value === 'match' && matchList.value?.length) {
      await nextTick()
      initMatchBarChart()
    }
  },
  { flush: 'post' }
)
</script>

<template>
  <div class="analysis-page">
    <!-- Header -->
    <header class="analysis-header">
      <div class="header-left">
        <el-icon class="back-icon" @click="router.push('/upload')"><ArrowLeft /></el-icon>
        <div class="file-info">
          <div class="file-name">社招简历{{ resumeId }}.pdf</div>
          <div class="upload-time">
            <el-icon><Document /></el-icon>
            上传于 {{ analysis ? formatTime(analysis.createTime) : '--' }}
          </div>
        </div>
      </div>
      <button class="btn-interview">
        <el-icon><Microphone /></el-icon>
        开始模拟面试
      </button>
    </header>

    <!-- Tabs -->
    <div class="analysis-tabs">
      <div
        class="tab-item"
        :class="{ 'tab-item--active': activeTab === 'analysis' }"
        @click="switchTab('analysis')"
      >
        <el-icon><Document /></el-icon>
        简历分析
      </div>
      <div
        class="tab-item"
        :class="{ 'tab-item--active': activeTab === 'match' }"
        @click="switchTab('match')"
      >
        <el-icon><Position /></el-icon>
        岗位匹配
      </div>
    </div>

    <!-- Main: 简历分析 -->
    <template v-if="activeTab === 'analysis'">
      <div v-if="error" class="analysis-error">{{ error }}</div>

      <div v-else-if="loading" class="analysis-main analysis-main--loading">
      <div class="card-left">
        <div class="skeleton" style="height: 24px; width: 30%; border-radius: 8px; margin-bottom: 12px" />
        <div class="skeleton" style="height: 60px; width: 100%; border-radius: 12px; margin-bottom: 12px" />
        <div class="skeleton" style="height: 48px; width: 25%; border-radius: 8px; margin-bottom: 12px" />
        <div class="skeleton" style="height: 80px; width: 100%; border-radius: 12px" />
      </div>
      <div class="card-right">
        <div class="skeleton" style="height: 260px; width: 100%; border-radius: 16px; margin-bottom: 16px" />
        <div class="skeleton" style="height: 12px; width: 100%; border-radius: 6px; margin-bottom: 12px" />
        <div class="skeleton" style="height: 12px; width: 90%; border-radius: 6px; margin-bottom: 12px" />
      </div>
    </div>

    <div v-else-if="analysis" class="analysis-main">
      <!-- 左侧：核心评价 -->
      <div class="card-left">
        <div class="card-title-row">
          <h3 class="card-title">
            <el-icon><Document /></el-icon>
            核心评价
          </h3>
          <button class="btn-export" :disabled="exportPdfLoading" @click="exportAnalysisPdf">
            <el-icon v-if="!exportPdfLoading"><Download /></el-icon>
            {{ exportPdfLoading ? '导出中...' : '导出分析结果' }}
          </button>
        </div>

        <p class="core-summary">{{ analysis.summary }}</p>

        <div class="score-row-main">
          <div class="score-big">
            <span class="score-value">{{ analysis.score }}</span>
            <span class="score-max">/100</span>
          </div>
          <div class="score-time">分析时间：{{ formatTime(analysis.createTime) }}</div>
        </div>

        <h4 class="section-label">优势亮点</h4>
        <div class="tag-list">
          <span v-for="(item, i) in advantages" :key="i" class="tag-chip">{{ item }}</span>
        </div>
      </div>

      <!-- 右侧：雷达图 + 进度条 -->
      <div class="card-right">
        <h3 class="card-title">
          <el-icon><Document /></el-icon>
          多维评分
        </h3>

        <div ref="radarChartRef" class="radar-chart" />

        <div class="score-bars">
          <div v-for="(item, i) in scoreItems" :key="i" class="score-bar-row">
            <div class="score-bar-label">
              <span>{{ item.label }}</span>
              <span>{{ item.value }}/{{ item.max }}</span>
            </div>
            <div class="score-bar-track">
              <div
                class="score-bar-fill"
                :style="{
                  width: percentage(item.value, item.max) + '%',
                  background: item.color
                }"
              />
            </div>
          </div>
        </div>
      </div>

      <!-- 改进建议：高 / 中 / 低 -->
      <div v-if="suggestionTotal > 0" class="suggestions-wrap">
        <h3 class="suggestions-main-title">改进建议（{{ suggestionTotal }}条）</h3>

        <section v-if="suggestionHigh.length" class="suggestions-block suggestions-block--high">
          <h4 class="suggestions-block-title">高优先级（{{ suggestionHigh.length }}）</h4>
          <div v-for="(item, i) in suggestionHigh" :key="'high-' + i" class="suggestion-card suggestion-card--high">
            <span v-if="item.category" class="suggestion-tag suggestion-tag--high">{{ item.category }}</span>
            <h5 class="suggestion-title">{{ item.title }}</h5>
            <p class="suggestion-content">{{ item.content }}</p>
          </div>
        </section>

        <section v-if="suggestionMedium.length" class="suggestions-block suggestions-block--medium">
          <h4 class="suggestions-block-title">中优先级（{{ suggestionMedium.length }}）</h4>
          <div v-for="(item, i) in suggestionMedium" :key="'medium-' + i" class="suggestion-card suggestion-card--medium">
            <span v-if="item.category" class="suggestion-tag suggestion-tag--medium">{{ item.category }}</span>
            <h5 class="suggestion-title">{{ item.title }}</h5>
            <p class="suggestion-content">{{ item.content }}</p>
          </div>
        </section>

        <section v-if="suggestionLow.length" class="suggestions-block suggestions-block--low">
          <h4 class="suggestions-block-title">低优先级（{{ suggestionLow.length }}）</h4>
          <div v-for="(item, i) in suggestionLow" :key="'low-' + i" class="suggestion-card suggestion-card--low">
            <span v-if="item.category" class="suggestion-tag suggestion-tag--low">{{ item.category }}</span>
            <h5 class="suggestion-title">{{ item.title }}</h5>
            <p class="suggestion-content">{{ item.content }}</p>
          </div>
        </section>
      </div>
    </div>
    </template>

    <!-- Main: 岗位匹配 -->
    <div v-if="activeTab === 'match'" class="match-panel">
      <div v-if="matchLoading" class="match-loading">加载岗位匹配中...</div>
      <div v-else-if="matchError" class="analysis-error">{{ matchError }}</div>
      <div v-else-if="!matchList.length" class="match-empty">暂无岗位匹配数据</div>
      <div v-else class="match-content">
        <h3 class="card-title match-title">
          <el-icon><Position /></el-icon>
          岗位匹配分数
        </h3>
        <div class="match-score-row">
          <div class="score-big">
            <span class="score-value">{{ matchAverageScore }}</span>
            <span class="score-max">/100</span>
          </div>
          <span class="match-score-desc">平均匹配分（共 {{ matchList.length }} 个岗位）</span>
        </div>

        <div ref="matchBarChartRef" class="match-bar-chart" />

        <h4 class="section-label">匹配明细</h4>
        <div class="match-table-wrap">
          <table class="match-table">
            <thead>
              <tr>
                <th>岗位名称</th>
                <th>匹配分数</th>
                <th>匹配优势</th>
                <th>不足</th>
                <th>建议</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, i) in matchList" :key="row.id || i">
                <td>{{ row.jobName || '--' }}</td>
                <td>{{ row.matchScore ?? '--' }}</td>
                <td>
                  <ul v-if="row.advantages?.length" class="match-list">
                    <li v-for="(a, j) in row.advantages" :key="j">{{ a }}</li>
                  </ul>
                  <span v-else>--</span>
                </td>
                <td>
                  <ul v-if="row.disadvantages?.length" class="match-list">
                    <li v-for="(d, j) in row.disadvantages" :key="j">{{ d }}</li>
                  </ul>
                  <span v-else>--</span>
                </td>
                <td>
                  <ul v-if="row.suggestions?.length" class="match-list">
                    <li v-for="(s, j) in row.suggestions" :key="j">{{ s }}</li>
                  </ul>
                  <span v-else>--</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.analysis-page {
  width: 100%;
  max-width: 1120px;
  margin: 0 auto;
  padding: 24px 16px 40px;
}

.analysis-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  padding: 0 4px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.back-icon {
  font-size: 20px;
  color: #64748b;
  cursor: pointer;
}

.back-icon:hover {
  color: #334155;
}

.file-name {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
}

.upload-time {
  font-size: 12px;
  color: #64748b;
  display: flex;
  align-items: center;
  gap: 4px;
}

.btn-interview {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 20px;
  border-radius: 12px;
  border: none;
  background: linear-gradient(135deg, #8b5cf6, #6366f1);
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  box-shadow: 0 4px 14px rgba(139, 92, 246, 0.4);
}

.btn-interview:hover {
  opacity: 0.95;
  transform: translateY(-1px);
}

.analysis-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
}

.tab-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border-radius: 12px;
  font-size: 14px;
  color: #64748b;
  background: #f1f5f9;
  cursor: pointer;
}

.tab-item--active {
  color: #4f46e5;
  background: rgba(79, 70, 229, 0.1);
  font-weight: 500;
}

.tab-badge {
  background: #ef4444;
  color: #fff;
  font-size: 11px;
  padding: 2px 6px;
  border-radius: 999px;
}

.analysis-error {
  padding: 16px;
  color: #ef4444;
  background: #fef2f2;
  border-radius: 12px;
}

.analysis-main {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 20px;
}

.analysis-main--loading .card-left,
.analysis-main--loading .card-right {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
}

.card-left,
.card-right {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
}

.card-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.card-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px;
}

.card-right .card-title {
  margin-bottom: 16px;
}

.btn-export {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border-radius: 10px;
  border: 1px solid #e2e8f0;
  background: #fff;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
}

.btn-export:hover {
  background: #f8fafc;
  color: #475569;
}

.core-summary {
  font-size: 14px;
  line-height: 1.7;
  color: #475569;
  margin: 0 0 16px;
  padding: 12px 14px;
  background: #f8fafc;
  border-radius: 12px;
}

.score-row-main {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.score-big {
  font-size: 32px;
  font-weight: 700;
  color: #0f766e;
}

.score-max {
  font-size: 18px;
  font-weight: 500;
  color: #94a3b8;
}

.score-time {
  font-size: 12px;
  color: #94a3b8;
}

.section-label {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 12px;
}

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag-chip {
  display: inline-block;
  padding: 6px 12px;
  font-size: 12px;
  color: #166534;
  background: #dcfce7;
  border-radius: 999px;
}

.radar-chart {
  width: 100%;
  height: 280px;
  margin-bottom: 20px;
}

.score-bars {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.score-bar-row {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.score-bar-label {
  display: flex;
  justify-content: space-between;
  font-size: 13px;
  color: #64748b;
}

.score-bar-track {
  height: 8px;
  border-radius: 999px;
  background: #e2e8f0;
  overflow: hidden;
}

.score-bar-fill {
  height: 100%;
  border-radius: inherit;
  transition: width 0.4s ease;
}

.skeleton {
  background: linear-gradient(90deg, #e2e8f0 25%, #f1f5f9 50%, #e2e8f0 75%);
  background-size: 200% 100%;
  animation: shimmer 1.2s infinite;
}

@keyframes shimmer {
  0% { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 改进建议 */
.suggestions-wrap {
  grid-column: 1 / -1;
  margin-top: 8px;
}

.suggestions-main-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 16px;
}

.suggestions-block {
  margin-bottom: 20px;
}

.suggestions-block:last-child {
  margin-bottom: 0;
}

.suggestions-block-title {
  font-size: 14px;
  font-weight: 600;
  color: #334155;
  margin: 0 0 12px;
}

.suggestion-card {
  background: #fff;
  border-radius: 12px;
  padding: 14px 16px;
  margin-bottom: 12px;
  border: 1px solid transparent;
  box-shadow: 0 2px 8px rgba(15, 23, 42, 0.06);
}

.suggestion-card:last-child {
  margin-bottom: 0;
}

.suggestion-card--high {
  border-color: #fecaca;
  border-left: 3px solid #dc2626;
}

.suggestion-card--medium {
  border-color: #fde68a;
  border-left: 3px solid #eab308;
}

.suggestion-card--low {
  border-color: #bfdbfe;
  border-left: 3px solid #3b82f6;
}

.suggestion-tag {
  display: inline-block;
  font-size: 12px;
  padding: 2px 8px;
  border-radius: 4px;
  color: #fff;
  margin-bottom: 8px;
}

.suggestion-tag--high {
  background: #dc2626;
}

.suggestion-tag--medium {
  background: #eab308;
  color: #1e293b;
}

.suggestion-tag--low {
  background: #3b82f6;
}

.suggestion-title {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
  line-height: 1.4;
}

.suggestion-content {
  font-size: 13px;
  color: #475569;
  line-height: 1.6;
  margin: 0;
  white-space: pre-wrap;
}

/* 岗位匹配 */
.match-panel {
  background: #fff;
  border-radius: 20px;
  padding: 24px;
  box-shadow: 0 4px 20px rgba(15, 23, 42, 0.08);
}

.match-loading,
.match-empty {
  text-align: center;
  color: #64748b;
  padding: 48px 16px;
}

.match-content {
  width: 100%;
}

.match-title {
  margin-bottom: 16px;
}

.match-score-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.match-score-desc {
  font-size: 14px;
  color: #64748b;
}

.match-bar-chart {
  width: 100%;
  height: 280px;
  margin-bottom: 24px;
}

.match-table-wrap {
  overflow-x: auto;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
}

.match-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

.match-table th,
.match-table td {
  padding: 12px 16px;
  text-align: left;
  border-bottom: 1px solid #e2e8f0;
}

.match-table th {
  background: #f8fafc;
  font-weight: 600;
  color: #334155;
}

.match-table tbody tr:last-child td {
  border-bottom: none;
}

.match-table td {
  color: #475569;
  vertical-align: top;
}

.match-list {
  margin: 0;
  padding-left: 16px;
  list-style: disc;
}

.match-list li {
  margin-bottom: 4px;
}

.match-list li:last-child {
  margin-bottom: 0;
}

@media (max-width: 900px) {
  .analysis-main {
    grid-template-columns: 1fr;
  }
}
</style>
