<template>
  <div class="users-page">
    <div class="page-header">
      <h2>用户管理</h2>
      <span class="user-count">共 {{ users.length }} 位用户</span>
    </div>

    <el-table
      :data="users"
      v-loading="loading"
      stripe
      border
      style="width:100%"
    >
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="studentId" label="学号" width="140" />
      <el-table-column prop="username" label="姓名" width="120" />
      <el-table-column prop="role" label="角色" width="100">
        <template #default="{ row }">
          <span class="role-badge" :class="row.role === 'ADMIN' ? 'role-admin' : 'role-student'">
            {{ row.role === 'ADMIN' ? '管理员' : '学生' }}
          </span>
        </template>
      </el-table-column>
      <el-table-column prop="enabled" label="状态" width="110">
        <template #default="{ row }">
          <button
            class="toggle-status"
            :class="{ active: row.enabled }"
            @click="toggleUser(row)"
          >
            <span class="toggle-dot"></span>
            <span class="toggle-label">{{ row.enabled ? '启用' : '禁用' }}</span>
          </button>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="注册时间" min-width="170">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{ row }">
          <button class="action-btn" @click="openResetDialog(row)">重置密码</button>
          <button
            v-if="row.role !== 'ADMIN'"
            class="action-btn danger"
            @click="handleDelete(row.id)"
          >
            删除
          </button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="resetVisible" title="重置密码" width="400px" :close-on-click-modal="false">
      <div class="reset-body">
        <div class="reset-user-info">
          <span class="reset-label">用户</span>
          <span class="reset-value">{{ resetUser?.username }} ({{ resetUser?.studentId }})</span>
        </div>
        <div class="field-group">
          <label class="field-label">新密码</label>
          <input
            v-model="newPassword"
            type="password"
            placeholder="请输入新密码"
            class="modern-input"
          />
        </div>
      </div>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { adminApi } from '@/api/admin'

interface UserItem {
  id: number
  studentId: string
  username: string
  role: string
  enabled: boolean
  createdAt: string
}

const users = ref<UserItem[]>([])
const loading = ref(false)
const submitting = ref(false)
const resetVisible = ref(false)
const resetUser = ref<UserItem | null>(null)
const newPassword = ref('')

async function loadUsers() {
  loading.value = true
  try {
    const res = await adminApi.listUsers()
    users.value = res.data
  } catch {
  } finally {
    loading.value = false
  }
}

async function toggleUser(user: UserItem) {
  try {
    await adminApi.updateUser(user.id, { enabled: !user.enabled })
    user.enabled = !user.enabled
    ElMessage.success(user.enabled ? '已启用' : '已禁用')
  } catch {
  }
}

function openResetDialog(user: UserItem) {
  resetUser.value = user
  newPassword.value = ''
  resetVisible.value = true
}

async function handleReset() {
  if (!newPassword.value.trim()) {
    ElMessage.warning('请输入新密码')
    return
  }
  submitting.value = true
  try {
    await adminApi.updateUser(resetUser.value!.id, { password: newPassword.value })
    ElMessage.success('密码重置成功')
    resetVisible.value = false
  } catch {
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除此用户？', '提示', { type: 'warning' })
    await adminApi.deleteUser(id)
    ElMessage.success('删除成功')
    loadUsers()
  } catch {
  }
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

onMounted(loadUsers)
</script>

<style scoped>
.users-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.user-count {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.role-badge {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
  letter-spacing: 0.3px;
}

.role-admin {
  background: rgba(239, 68, 68, 0.1);
  color: var(--color-danger);
}

.role-student {
  background: rgba(37, 99, 235, 0.1);
  color: var(--color-primary);
}

.toggle-status {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  border: none;
  background: none;
  cursor: pointer;
  padding: 0;
  font-family: inherit;
}

.toggle-dot {
  width: 36px;
  height: 20px;
  border-radius: 10px;
  background: #cbd5e1;
  position: relative;
  transition: background var(--transition-fast);
}

.toggle-dot::after {
  content: '';
  position: absolute;
  top: 2px;
  left: 2px;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #ffffff;
  box-shadow: var(--shadow-sm);
  transition: transform var(--transition-fast);
}

.toggle-status.active .toggle-dot {
  background: var(--color-success);
}

.toggle-status.active .toggle-dot::after {
  transform: translateX(16px);
}

.toggle-label {
  font-size: 13px;
  color: var(--color-text-secondary);
}

.action-btn {
  padding: 4px 10px;
  border: none;
  background: transparent;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-primary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  font-family: inherit;
  transition: all var(--transition-fast);
}

.action-btn:hover {
  background: rgba(37, 99, 235, 0.08);
}

.action-btn.danger {
  color: var(--color-danger);
}

.action-btn.danger:hover {
  background: rgba(239, 68, 68, 0.08);
}

.reset-body {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.reset-user-info {
  display: flex;
  gap: 12px;
  align-items: center;
}

.reset-label {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.reset-value {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.field-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.field-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.modern-input {
  width: 100%;
  padding: 10px 14px;
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-surface);
  border: 1.5px solid var(--color-border);
  border-radius: var(--radius-md);
  outline: none;
  transition: border-color var(--transition-fast), box-shadow var(--transition-fast);
}

.modern-input:focus {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 3px rgba(37, 99, 235, 0.1);
}
</style>
