<template>
  <div class="login-page">
    <div class="login-card">
      <div class="card-header">
        <div class="logo">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="10" fill="#6366f1" />
            <path d="M12 15h16v3H12zM12 20h11v3H12zM12 25h9v3H12z" fill="#fff" opacity="0.9" />
          </svg>
        </div>
        <h1>{{ activeTab === 'login' ? '登录' : '创建账号' }}</h1>
        <p class="subtitle">{{ activeTab === 'login' ? '使用您的校园账号' : '注册新的校园账号' }}</p>
      </div>

      <transition name="fade" mode="out-in">
        <form v-if="activeTab === 'login'" key="login" @submit.prevent="handleLogin" class="form-body">
          <div class="field">
            <input v-model="loginForm.studentId" type="text" placeholder="学号" class="input" autocomplete="username" required />
          </div>
          <div class="field">
            <div class="pwd-wrap">
              <input v-model="loginForm.password" :type="showPwd ? 'text' : 'password'" placeholder="密码" class="input" autocomplete="current-password" required />
              <button type="button" class="pwd-toggle" @click="showPwd = !showPwd">{{ showPwd ? '隐藏' : '显示' }}</button>
            </div>
          </div>
          <button type="submit" class="btn-primary" :disabled="loading">
            <span v-if="!loading">登录</span>
            <span v-else class="spinner"></span>
          </button>
        </form>

        <form v-else key="register" @submit.prevent="handleRegister" class="form-body">
          <div class="field">
            <input v-model="registerForm.studentId" type="text" placeholder="学号" class="input" required />
          </div>
          <div class="field">
            <input v-model="registerForm.username" type="text" placeholder="姓名" class="input" required />
          </div>
          <div class="field">
            <div class="pwd-wrap">
              <input v-model="registerForm.password" :type="showRegPwd ? 'text' : 'password'" placeholder="密码（至少6位）" class="input" required />
              <button type="button" class="pwd-toggle" @click="showRegPwd = !showRegPwd">{{ showRegPwd ? '隐藏' : '显示' }}</button>
            </div>
          </div>
          <div class="field">
            <input v-model="registerForm.confirmPassword" type="password" placeholder="确认密码" class="input" required />
          </div>
          <p v-if="registerError" class="error-msg">{{ registerError }}</p>
          <button type="submit" class="btn-primary" :disabled="loading">
            <span v-if="!loading">注册</span>
            <span v-else class="spinner"></span>
          </button>
        </form>
      </transition>

      <div class="card-footer">
        <button class="switch-btn" @click="switchTab">
          {{ activeTab === 'login' ? '创建账号' : '已有账号？登录' }}
        </button>
      </div>

      <p class="test-hint">Sichuan Technology and Business College</p>
    </div>

    <p class="page-footer">四川工商职业技术学院 · 校园智能客服系统</p>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { authApi } from '@/api/auth'
import { useAuthStore } from '@/store/auth'

const router = useRouter()
const authStore = useAuthStore()

const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)
const showPwd = ref(false)
const showRegPwd = ref(false)
const registerError = ref('')

const loginForm = ref({ studentId: '', password: '' })
const registerForm = ref({ studentId: '', username: '', password: '', confirmPassword: '' })

function switchTab() {
  activeTab.value = activeTab.value === 'login' ? 'register' : 'login'
  registerError.value = ''
}

async function handleLogin() {
  if (!loginForm.value.studentId || !loginForm.value.password) {
    ElMessage.warning('请填写学号和密码')
    return
  }
  loading.value = true
  try {
    const res = await authApi.login(loginForm.value)
    const { token, role, username, studentId } = res.data
    authStore.setAuth(token, role, username, studentId)
    ElMessage.success('登录成功')
    router.push(role === 'ADMIN' ? '/admin/dashboard' : '/chat')
  } catch (e: any) {
    ElMessage.error(e.response?.data?.message || '登录失败')
  } finally {
    loading.value = false
  }
}

async function handleRegister() {
  registerError.value = ''
  const f = registerForm.value
  if (!f.studentId || !f.username || !f.password || !f.confirmPassword) {
    registerError.value = '请填写完整信息'
    return
  }
  if (f.password.length < 6) {
    registerError.value = '密码至少6位'
    return
  }
  if (f.password !== f.confirmPassword) {
    registerError.value = '两次密码不一致'
    return
  }
  loading.value = true
  try {
    await authApi.register(f)
    ElMessage.success('注册成功，请登录')
    loginForm.value.studentId = f.studentId
    loginForm.value.password = ''
    activeTab.value = 'login'
  } catch (e: any) {
    registerError.value = e.response?.data?.message || '注册失败'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: var(--color-bg);
  padding: 24px;
}

.login-card {
  width: 100%;
  max-width: 400px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 40px 36px 28px;
}

.card-header {
  text-align: center;
  margin-bottom: 28px;
}

.logo {
  width: 48px;
  height: 48px;
  margin: 0 auto 16px;
}

.logo svg {
  width: 100%;
  height: 100%;
}

.card-header h1 {
  font-size: 22px;
  font-weight: 600;
  color: var(--color-text-primary);
  margin-bottom: 6px;
}

.subtitle {
  font-size: 14px;
  color: var(--color-text-tertiary);
}

.form-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field {
  display: flex;
  flex-direction: column;
}

.input {
  width: 100%;
  padding: 12px 16px;
  font-size: 15px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: 8px;
  outline: none;
  transition: border-color 150ms, box-shadow 150ms;
}

.input::placeholder { color: var(--color-text-tertiary); }
.input:focus { border-color: #6366f1; box-shadow: 0 0 0 2px rgba(99,102,241,0.12); }

.pwd-wrap { position: relative; }

.pwd-toggle {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  border: none;
  background: none;
  font-size: 13px;
  color: #6366f1;
  cursor: pointer;
  padding: 4px 6px;
  font-family: inherit;
  font-weight: 500;
}

.pwd-toggle:hover { text-decoration: underline; }

.btn-primary {
  width: 100%;
  padding: 12px;
  font-size: 15px;
  font-weight: 600;
  font-family: inherit;
  color: #ffffff;
  background: #6366f1;
  border: none;
  border-radius: 8px;
  cursor: pointer;
  transition: background 150ms, box-shadow 150ms;
  margin-top: 4px;
}

.btn-primary:hover:not(:disabled) { background: #5558e6; box-shadow: 0 2px 8px rgba(99,102,241,0.3); }
.btn-primary:active:not(:disabled) { background: #4f46e5; }
.btn-primary:disabled { opacity: 0.6; cursor: not-allowed; }

.spinner {
  display: inline-block;
  width: 20px;
  height: 20px;
  border: 2.5px solid rgba(255,255,255,0.3);
  border-top-color: #ffffff;
  border-radius: 50%;
  animation: spin 0.6s linear infinite;
}

@keyframes spin { to { transform: rotate(360deg); } }

.error-msg {
  font-size: 13px;
  color: #ef4444;
  margin: -8px 0 -4px;
}

.card-footer {
  text-align: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid var(--color-border);
}

.switch-btn {
  border: none;
  background: none;
  font-size: 14px;
  font-weight: 500;
  color: #6366f1;
  cursor: pointer;
  font-family: inherit;
  padding: 4px 8px;
}

.switch-btn:hover { text-decoration: underline; }

.test-hint {
  text-align: center;
  font-size: 11px;
  color: var(--color-text-tertiary);
  margin-top: 16px;
  line-height: 1.5;
}

.page-footer {
  margin-top: 28px;
  font-size: 12px;
  color: var(--color-text-tertiary);
}

.fade-enter-active { transition: opacity 150ms; }
.fade-leave-active { transition: opacity 100ms; }
.fade-enter-from, .fade-leave-to { opacity: 0; }

@media (max-width: 480px) {
  .login-card { padding: 28px 20px 20px; }
  .card-header h1 { font-size: 20px; }
}
</style>
