<template>
  <div class="admin-layout">
    <aside class="sidebar">
      <div class="sidebar-brand">
        <div class="brand-icon">
          <svg viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="40" height="40" rx="10" fill="url(#sbgrad)" />
            <path d="M12 15h16v3H12zM12 20h11v3H12zM12 25h9v3H12z" fill="#fff" opacity="0.85" />
            <defs>
              <linearGradient id="sbgrad" x1="0" y1="0" x2="40" y2="40">
                <stop offset="0%" stop-color="#6366f1" />
                <stop offset="100%" stop-color="#8b5cf6" />
              </linearGradient>
            </defs>
          </svg>
        </div>
        <span class="brand-text">管理后台</span>
      </div>

      <nav class="sidebar-nav">
        <router-link to="/admin/dashboard" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
          </span>
          <span>数据统计</span>
        </router-link>
        <router-link to="/admin/knowledge" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
          </span>
          <span>知识库管理</span>
        </router-link>
        <router-link to="/admin/users" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87"/><path d="M16 3.13a4 4 0 010 7.75"/></svg>
          </span>
          <span>用户管理</span>
        </router-link>
        <router-link to="/admin/feedbacks" class="nav-item" active-class="nav-active">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14zM7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3"/></svg>
          </span>
          <span>反馈管理</span>
        </router-link>
      </nav>

      <div class="sidebar-footer">
        <router-link to="/chat" class="nav-item footer-item">
          <span class="nav-icon">
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          </span>
          <span>返回聊天</span>
        </router-link>
      </div>
    </aside>

    <div class="main-area">
      <header class="topbar">
        <div class="topbar-breadcrumb">
          <span v-if="pageTitle">{{ pageTitle }}</span>
        </div>
        <div class="topbar-actions">
          <span class="user-badge">{{ authStore.username }}</span>
          <button class="logout-btn" @click="handleLogout">退出</button>
        </div>
      </header>
      <main class="main-content">
        <router-view />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const pageTitle = computed(() => {
  const map: Record<string, string> = {
    '/admin/dashboard': '数据统计',
    '/admin/knowledge': '知识库管理',
    '/admin/users': '用户管理',
    '/admin/feedbacks': '反馈管理'
  }
  return map[route.path] || ''
})

function handleLogout() {
  authStore.logout()
  router.push('/login')
}
</script>

<style scoped>
.admin-layout {
  display: flex;
  height: 100vh;
  background: var(--color-bg);
}

.sidebar {
  width: 240px;
  background: #0f0f1a;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

[data-theme="dark"] .sidebar {
  background: #0a0a14;
}

.sidebar-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 20px 20px 24px;
  border-bottom: 1px solid rgba(255,255,255,0.06);
}

.brand-icon { width: 40px; height: 40px; flex-shrink: 0; }
.brand-icon svg { width: 100%; height: 100%; }

.brand-text {
  font-size: 17px;
  font-weight: 700;
  color: #ffffff;
  letter-spacing: 0.5px;
}

.sidebar-nav {
  flex: 1;
  padding: 12px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  overflow-y: auto;
}

.nav-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 14px;
  border-radius: 8px;
  color: rgba(255,255,255,0.5);
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 150ms;
  cursor: pointer;
}

.nav-item:hover { background: rgba(255,255,255,0.06); color: rgba(255,255,255,0.85); }

.nav-active {
  background: rgba(99,102,241,0.18) !important;
  color: #c7d2fe !important;
}

.nav-active .nav-icon { color: #818cf8; }

.nav-icon {
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: rgba(255,255,255,0.35);
}

.sidebar-footer {
  padding: 12px;
  border-top: 1px solid rgba(255,255,255,0.06);
}

.footer-item { font-size: 13px; opacity: 0.8; }

.main-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.topbar {
  height: 56px;
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  flex-shrink: 0;
}

.topbar-breadcrumb {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-text-primary);
}

.topbar-actions { display: flex; align-items: center; gap: 16px; }

.user-badge { font-size: 14px; color: var(--color-text-secondary); }

.logout-btn {
  padding: 6px 14px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: 6px;
  font-size: 13px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
  transition: all 150ms;
}

.logout-btn:hover {
  background: var(--color-bg);
  border-color: var(--color-text-tertiary);
  color: var(--color-text-primary);
}

.main-content {
  flex: 1;
  overflow-y: auto;
  padding: 28px;
}
</style>
