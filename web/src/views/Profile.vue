<template>
  <div class="profile-page">
    <div class="profile-container">
      <button class="back-btn" @click="goBack">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M19 12H5M12 19l-7-7 7-7"/></svg>
        返回首页
      </button>

      <div class="profile-card">
        <div class="card-header">
          <div class="avatar-section" @click="triggerUpload" title="点击更换头像">
            <img v-if="userInfo.avatar" :src="userInfo.avatar" class="avatar-img" />
            <div v-else class="avatar-fallback">{{ userInfo.username?.charAt(0) || 'U' }}</div>
            <div class="avatar-overlay">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2"><path d="M23 19a2 2 0 01-2 2H3a2 2 0 01-2-2V8a2 2 0 012-2h4l2-3h6l2 3h4a2 2 0 012 2z"/><circle cx="12" cy="13" r="4"/></svg>
            </div>
          </div>
          <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="handleFileChange" />
          <div class="user-detail">
            <h2>{{ userInfo.username }}</h2>
            <span class="user-badge">{{ userInfo.studentId }}</span>
          </div>
        </div>
      </div>

      <div class="stats-row">
        <div class="stat-item">
          <div class="stat-num">{{ stats.conversationCount }}</div>
          <div class="stat-label">对话次数</div>
        </div>
        <div class="stat-divider"></div>
        <div class="stat-item">
          <div class="stat-num">{{ stats.messageCount }}</div>
          <div class="stat-label">提问次数</div>
        </div>
      </div>

      <div class="section-card">
        <h3 class="section-title">修改密码</h3>
        <div class="pwd-form">
          <div class="field">
            <label>原密码</label>
            <input v-model="passwordForm.oldPassword" type="password" placeholder="请输入原密码" class="field-input" />
          </div>
          <div class="field">
            <label>新密码</label>
            <input v-model="passwordForm.newPassword" type="password" placeholder="至少6位" class="field-input" />
          </div>
          <div class="field">
            <label>确认密码</label>
            <input v-model="passwordForm.confirmPassword" type="password" placeholder="再次输入新密码" class="field-input" />
          </div>
          <button class="pwd-submit" :disabled="changing" @click="handleChangePassword">
            <span v-if="!changing">修改密码</span>
            <span v-else class="btn-spinner"></span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '@/api/request'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

function goBack() {
  router.push('/chat')
}

const userInfo = ref({ username: '', studentId: '', avatar: '' })
const stats = ref({ conversationCount: 0, messageCount: 0 })
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const changing = ref(false)
const fileInput = ref<HTMLInputElement>()

function triggerUpload() {
  fileInput.value?.click()
}

function handleFileChange(e: Event) {
  const file = (e.target as HTMLInputElement).files?.[0]
  if (!file) return
  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过2MB')
    return
  }
  const reader = new FileReader()
  reader.onload = async () => {
    const img = new Image()
    img.onload = async () => {
      const canvas = document.createElement('canvas')
      const size = 128
      canvas.width = size
      canvas.height = size
      const ctx = canvas.getContext('2d')!
      const minDim = Math.min(img.width, img.height)
      const sx = (img.width - minDim) / 2
      const sy = (img.height - minDim) / 2
      ctx.drawImage(img, sx, sy, minDim, minDim, 0, 0, size, size)
      const compressed = canvas.toDataURL('image/jpeg', 0.7)
      try {
        await request.post('/api/auth/avatar', { avatar: compressed })
        userInfo.value.avatar = compressed
        authStore.setAvatar(compressed)
        ElMessage.success('头像更新成功')
      } catch {
        ElMessage.error('头像上传失败')
      }
    }
    img.src = reader.result as string
  }
  reader.readAsDataURL(file)
}

async function loadProfile() {
  try {
    const meRes = await request.get('/api/auth/me')
    userInfo.value = {
      username: meRes.data.username || '',
      studentId: meRes.data.studentId || '',
      avatar: meRes.data.avatar || ''
    }
  } catch {
    ElMessage.error('加载个人信息失败')
  }
  try {
    const statsRes = await request.get('/api/auth/stats')
    stats.value = statsRes.data
  } catch {
    // 静默失败 - 统计数据非核心功能
  }
}

async function handleChangePassword() {
  if (!passwordForm.value.oldPassword || !passwordForm.value.newPassword) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  if (passwordForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  changing.value = true
  try {
    await request.put('/api/auth/password', {
      oldPassword: passwordForm.value.oldPassword,
      newPassword: passwordForm.value.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
    authStore.logout()
    setTimeout(() => router.push('/login'), 800)
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '修改失败')
  } finally {
    changing.value = false
  }
}

onMounted(loadProfile)
</script>

<style scoped>
.profile-page {
  min-height: 100vh;
  background: var(--color-bg);
  display: flex;
  justify-content: center;
  padding: 40px 20px;
}

.profile-container {
  width: 100%;
  max-width: 480px;
}

.back-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 8px;
  color: var(--color-text-secondary);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 150ms;
  margin-bottom: 20px;
}

.back-btn:hover {
  color: #6366f1;
  border-color: #6366f1;
  background: rgba(99,102,241,0.04);
}

.profile-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  overflow: hidden;
  margin-bottom: 16px;
}

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
}

.avatar-section {
  position: relative;
  width: 60px;
  height: 60px;
  border-radius: 50%;
  cursor: pointer;
  flex-shrink: 0;
  overflow: hidden;
}

.avatar-fallback {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  font-weight: 700;
}

.avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 150ms;
}

.avatar-section:hover .avatar-overlay { opacity: 1; }

.user-detail h2 {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 4px;
}

.user-badge {
  display: inline-block;
  padding: 2px 10px;
  background: rgba(99,102,241,0.08);
  color: #6366f1;
  font-size: 12px;
  font-weight: 600;
  border-radius: 6px;
}

.stats-row {
  display: flex;
  align-items: center;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 20px;
  margin-bottom: 16px;
}

.stat-item {
  flex: 1;
  text-align: center;
}

.stat-num {
  font-size: 24px;
  font-weight: 700;
  color: #6366f1;
}

.stat-label {
  font-size: 12px;
  color: var(--color-text-tertiary);
  margin-top: 2px;
}

.stat-divider {
  width: 1px;
  height: 36px;
  background: var(--color-border);
}

.section-card {
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 14px;
  padding: 24px;
}

.section-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin: 0 0 18px;
}

.pwd-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.field {
  display: flex;
  flex-direction: column;
  gap: 5px;
}

.field label {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-text-secondary);
}

.field-input {
  padding: 10px 14px;
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  outline: none;
  transition: border-color 150ms;
}

.field-input:focus { border-color: #6366f1; }
.field-input::placeholder { color: var(--color-text-tertiary); }

.pwd-submit {
  width: 100%;
  padding: 11px;
  font-size: 14px;
  font-weight: 600;
  font-family: inherit;
  color: #ffffff;
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: all 150ms;
  margin-top: 4px;
}

.pwd-submit:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(99,102,241,0.3);
}

.pwd-submit:disabled { opacity: 0.7; cursor: not-allowed; }

.btn-spinner {
  display: inline-block;
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }
</style>
