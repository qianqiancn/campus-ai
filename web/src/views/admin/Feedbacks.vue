<template>
  <div class="feedback-page">
    <div class="stats-row">
      <div class="stat-card">
        <div class="stat-icon total-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M12 20V10M18 20V4M6 20v-4"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-num">{{ stats.total }}</div>
          <div class="stat-label">反馈总数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon like-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14zM7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-num like-num">{{ stats.likes }}</div>
          <div class="stat-label">有帮助</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon dislike-icon">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 15v4a3 3 0 003 3l4-9V2H5.72a2 2 0 00-2 1.7l-1.38 9a2 2 0 002 2.3H10zM17 2h2.67A2.31 2.31 0 0122 4v7a2.31 2.31 0 01-2.33 2H17"/></svg>
        </div>
        <div class="stat-info">
          <div class="stat-num dislike-num">{{ stats.dislikes }}</div>
          <div class="stat-label">无帮助</div>
        </div>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="feedbacks" stripe style="width: 100%" v-loading="loading" empty-text="暂无反馈数据">
        <el-table-column prop="id" label="ID" width="60" />
        <el-table-column prop="messageContent" label="消息内容" min-width="240" show-overflow-tooltip />
        <el-table-column prop="username" label="用户" width="100" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <span class="type-badge" :class="row.type === 'LIKE' ? 'like' : 'dislike'">
              {{ row.type === 'LIKE' ? '👍 有帮助' : '👎 无帮助' }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="时间" width="170">
          <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { adminApi } from '@/api/admin'

const feedbacks = ref<any[]>([])
const stats = ref({ total: 0, likes: 0, dislikes: 0 })
const loading = ref(false)

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

async function loadData() {
  loading.value = true
  try {
    const [fbRes, stRes] = await Promise.all([
      adminApi.listFeedbacks(),
      adminApi.feedbackStats()
    ])
    feedbacks.value = fbRes.data
    stats.value = stRes.data
  } catch {}
  loading.value = false
}

onMounted(loadData)
</script>

<style scoped>
.feedback-page {
  max-width: 960px;
}

.stats-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14px;
  margin-bottom: 20px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 18px 20px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
}

.stat-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.total-icon { background: rgba(99,102,241,0.1); color: #6366f1; }
.like-icon { background: rgba(16,185,129,0.1); color: #10b981; }
.dislike-icon { background: rgba(245,158,11,0.1); color: #f59e0b; }

.stat-num { font-size: 22px; font-weight: 700; color: var(--color-text-primary); }
.like-num { color: #10b981; }
.dislike-num { color: #f59e0b; }
.stat-label { font-size: 12px; color: var(--color-text-tertiary); margin-top: 2px; }

.table-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 16px;
}

.type-badge {
  display: inline-block;
  padding: 2px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 600;
}

.type-badge.like { background: rgba(16,185,129,0.1); color: #10b981; }
.type-badge.dislike { background: rgba(245,158,11,0.1); color: #f59e0b; }
</style>
