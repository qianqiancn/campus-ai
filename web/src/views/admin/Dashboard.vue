<template>
  <div class="dashboard">
    <div class="stats-grid">
      <div class="stat-card">
        <div class="stat-icon-wrapper" style="background: rgba(37, 99, 235, 0.1);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#2563eb" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2" /><circle cx="9" cy="7" r="4" /><path d="M23 21v-2a4 4 0 00-3-3.87" /><path d="M16 3.13a4 4 0 010 7.75" />
          </svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.userCount }}</div>
          <div class="stat-label">用户总数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper" style="background: rgba(16, 185, 129, 0.1);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#10b981" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M4 19.5A2.5 2.5 0 016.5 17H20" /><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z" />
          </svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.knowledgeCount }}</div>
          <div class="stat-label">知识条目</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper" style="background: rgba(245, 158, 11, 0.1);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#f59e0b" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z" />
          </svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.conversationCount }}</div>
          <div class="stat-label">对话总数</div>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon-wrapper" style="background: rgba(239, 68, 68, 0.1);">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#ef4444" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="10" /><polyline points="12 6 12 12 16 14" />
          </svg>
        </div>
        <div class="stat-body">
          <div class="stat-value">{{ stats.todayQuestions }}</div>
          <div class="stat-label">今日提问</div>
        </div>
      </div>
    </div>

    <div class="charts-row">
      <div class="chart-card">
        <div class="chart-header">
          <h3>近7天问答统计</h3>
        </div>
        <div ref="barChartRef" class="chart-body"></div>
      </div>
      <div class="chart-card">
        <div class="chart-header">
          <h3>知识分类分布</h3>
        </div>
        <div ref="pieChartRef" class="chart-body"></div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import * as echarts from 'echarts'
import { adminApi } from '@/api/admin'

const stats = ref({
  userCount: 0,
  knowledgeCount: 0,
  conversationCount: 0,
  todayMessages: 0,
  todayQuestions: 0
})

const barChartRef = ref<HTMLElement>()
const pieChartRef = ref<HTMLElement>()
let barChart: echarts.ECharts | null = null
let pieChart: echarts.ECharts | null = null

async function loadStats() {
  try {
    const res = await adminApi.stats()
    stats.value = res.data
  } catch {
  }
}

async function loadCharts() {
  try {
    const [dailyRes, catRes] = await Promise.all([
      adminApi.dailyStats(7),
      adminApi.categoryStats()
    ])

    const dailyData = dailyRes.data as Array<{ date: string; messages: number; questions: number }>
    const dates = dailyData.map(d => d.date.substring(5))
    const msgCounts = dailyData.map(d => d.messages)
    const qCounts = dailyData.map(d => d.questions)

    nextTick(() => {
      if (barChartRef.value) {
        barChart = echarts.init(barChartRef.value)
        barChart.setOption({
          tooltip: { trigger: 'axis' },
          legend: { data: ['消息总数', '提问数'], bottom: 0 },
          xAxis: { type: 'category', data: dates, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisLabel: { color: '#94a3b8' } },
          yAxis: { type: 'value', splitLine: { lineStyle: { color: '#f1f5f9' } }, axisLabel: { color: '#94a3b8' } },
          series: [
            {
              name: '消息总数',
              type: 'bar',
              data: msgCounts,
              itemStyle: { color: '#2563eb', borderRadius: [6, 6, 0, 0] },
              barMaxWidth: 32
            },
            {
              name: '提问数',
              type: 'bar',
              data: qCounts,
              itemStyle: { color: '#06b6d4', borderRadius: [6, 6, 0, 0] },
              barMaxWidth: 32
            }
          ],
          grid: { left: '3%', right: '4%', bottom: '12%', top: '8%', containLabel: true }
        })
      }

      if (pieChartRef.value) {
        pieChart = echarts.init(pieChartRef.value)
        const catData = catRes.data as Array<{ name: string; value: number }>
        const colors = ['#2563eb', '#06b6d4', '#10b981', '#f59e0b', '#ef4444', '#6366f1', '#ec4899', '#14b8a6', '#f97316', '#8b5cf6']

        pieChart.setOption({
          tooltip: { trigger: 'item', formatter: '{b}: {c}条 ({d}%)' },
          series: [{
            type: 'pie',
            radius: ['45%', '72%'],
            center: ['50%', '48%'],
            itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 3 },
            label: { show: true, formatter: '{b}', color: '#475569', fontSize: 12 },
            data: catData.map((item, i) => ({ name: item.name, value: item.value, itemStyle: { color: colors[i % colors.length] } })),
            emphasis: { label: { show: true, fontSize: 14, fontWeight: 'bold' }, scaleSize: 8 }
          }]
        })
      }
    })
  } catch {
  }
}

function handleResize() {
  barChart?.resize()
  pieChart?.resize()
}

onMounted(() => {
  loadStats()
  loadCharts()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  barChart?.dispose()
  pieChart?.dispose()
})
</script>

<style scoped>
.dashboard {
  padding: 0;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
}

.stat-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  padding: 22px 24px;
  display: flex;
  align-items: center;
  gap: 18px;
  transition: all var(--transition-fast);
}

.stat-card:hover {
  border-color: #cbd5e1;
  box-shadow: var(--shadow-md);
  transform: translateY(-1px);
}

.stat-icon-wrapper {
  width: 52px;
  height: 52px;
  border-radius: var(--radius-md);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-body {
  flex: 1;
  min-width: 0;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-text-primary);
  line-height: 1.1;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 13px;
  color: var(--color-text-tertiary);
  margin-top: 4px;
  font-weight: 500;
}

.charts-row {
  display: grid;
  grid-template-columns: 1.2fr 1fr;
  gap: 20px;
}

.chart-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
}

.chart-header {
  padding: 18px 24px 0;
}

.chart-header h3 {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.chart-body {
  height: 350px;
}

@media (max-width: 1024px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .charts-row {
    grid-template-columns: 1fr;
  }
}
</style>
