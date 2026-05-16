<template>
  <div class="chat-page">
    <aside class="sidebar">
      <div class="sidebar-top">
        <div class="app-brand">
          <div class="brand-icon-wrap">
            <svg viewBox="0 0 32 32" fill="none" class="brand-logo">
              <rect width="32" height="32" rx="8" fill="url(#clogo)" />
              <path d="M9 12h14v2H9zM9 16h10v2H9zM9 20h7v2H9z" fill="#fff" opacity="0.9" />
              <defs><linearGradient id="clogo" x1="0" y1="0" x2="32" y2="32"><stop offset="0%" stop-color="#6366f1"/><stop offset="100%" stop-color="#8b5cf6"/></linearGradient></defs>
            </svg>
          </div>
          <div class="brand-text">
            <span class="brand-name">校园智能客服</span>
            <span class="brand-sub">Campus AI</span>
          </div>
        </div>
        <button class="new-chat-btn" @click="newConversation">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
          新对话
        </button>
        <div class="conv-search">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="search-icon"><circle cx="11" cy="11" r="8"/><line x1="21" y1="21" x2="16.65" y2="16.65"/></svg>
          <input v-model="searchKeyword" placeholder="搜索对话..." class="conv-search-input" />
          <button v-if="searchKeyword" class="conv-search-clear" @click="searchKeyword = ''">
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
      </div>

      <div class="conv-list" v-loading="convLoading">
        <div v-for="conv in filteredConversations" :key="conv.id" class="conv-item" :class="{ active: conversationId === conv.id }" @click="selectConversation(conv.id)">
          <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" class="conv-icon"><path d="M21 15a2 2 0 01-2 2H7l-4 4V5a2 2 0 012-2h14a2 2 0 012 2z"/></svg>
          <span class="conv-title">{{ conv.title }}</span>
          <button class="conv-delete" @click.stop="deleteConv(conv.id)">
            <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
          </button>
        </div>
        <div v-if="!convLoading && filteredConversations.length === 0" class="empty-conv">
          {{ searchKeyword ? '未找到匹配的对话' : '暂无对话记录' }}
        </div>
      </div>

      <div class="sidebar-bottom">
        <div class="user-info">
          <div class="user-avatar">
            <img v-if="authStore.avatar" :src="authStore.avatar" class="user-avatar-img" />
            <span v-else>{{ authStore.username?.charAt(0) }}</span>
          </div>
          <div class="user-meta">
            <span class="user-name">{{ authStore.username }}</span>
            <span class="user-id">{{ authStore.studentId }}</span>
          </div>
        </div>
        <div class="bottom-actions">
          <button class="action-icon" @click="toggleTheme" :title="isDark ? '浅色模式' : '深色模式'">
            <svg v-if="!isDark" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 12.79A9 9 0 1111.21 3 7 7 0 0021 12.79z"/></svg>
            <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/></svg>
          </button>
          <button class="action-icon" @click="$router.push('/profile')" title="个人中心">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 21v-2a4 4 0 00-4-4H8a4 4 0 00-4 4v2"/><circle cx="12" cy="7" r="4"/></svg>
          </button>
          <button v-if="authStore.role === 'ADMIN'" class="action-icon" @click="$router.push('/admin')" title="管理后台">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="3" width="7" height="7"/><rect x="14" y="3" width="7" height="7"/><rect x="3" y="14" width="7" height="7"/><rect x="14" y="14" width="7" height="7"/></svg>
          </button>
          <button class="action-icon" @click="handleLogout" title="退出登录">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4"/><polyline points="16 17 21 12 16 7"/><line x1="21" y1="12" x2="9" y2="12"/></svg>
          </button>
        </div>
      </div>
    </aside>

    <div class="chat-main">
      <div class="chat-topbar" v-if="messages.length > 0">
        <span class="topbar-title">{{ conversations.find(c => c.id === conversationId)?.title || '对话' }}</span>
        <div class="topbar-actions">
          <button class="topbar-btn" @click="exportConversation" title="导出">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg>
          </button>
          <button class="topbar-btn" @click="shareConversation" title="分享">
            <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><circle cx="18" cy="5" r="3"/><circle cx="6" cy="12" r="3"/><circle cx="18" cy="19" r="3"/><line x1="8.59" y1="13.51" x2="15.42" y2="17.49"/><line x1="15.41" y1="6.51" x2="8.59" y2="10.49"/></svg>
          </button>
        </div>
      </div>

      <div class="message-area" ref="messageAreaRef">
        <div v-if="messages.length === 0" class="welcome">
          <div class="welcome-badge">
            <svg width="40" height="40" viewBox="0 0 32 32" fill="none">
              <rect width="32" height="32" rx="8" fill="url(#wgrad)" />
              <path d="M9 12h14v2H9zM9 16h10v2H9zM9 20h7v2H9z" fill="#fff" opacity="0.9" />
              <defs><linearGradient id="wgrad" x1="0" y1="0" x2="32" y2="32"><stop offset="0%" stop-color="#6366f1"/><stop offset="100%" stop-color="#8b5cf6"/></linearGradient></defs>
            </svg>
          </div>
          <h2>你好，有什么可以帮你的？</h2>
          <p>我是四川工商职业技术学院的校园智能助手，可以帮你解答选课、奖学金、宿舍、图书馆等问题</p>
          <div class="quick-grid">
            <button v-for="q in quickQuestions" :key="q" class="quick-card" @click="sendMessage(q)">
              <span class="quick-dot"></span>
              {{ q }}
            </button>
          </div>
        </div>

        <div v-for="(msg, idx) in messages" :key="idx" class="message-row" :class="msg.role === 'USER' ? 'msg-user' : 'msg-ai'">
          <div class="msg-avatar" :class="msg.role === 'USER' ? 'av-user' : 'av-ai'">
            <template v-if="msg.role === 'USER'">
              <img v-if="authStore.avatar" :src="authStore.avatar" class="msg-av-img" />
              <span v-else>{{ authStore.username?.charAt(0) }}</span>
            </template>
            <template v-else>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#6366f1" stroke-width="2"><path d="M12 2a10 10 0 1010 10A10 10 0 0012 2z"/><path d="M8 14s1.5 2 4 2 4-2 4-2"/><circle cx="9" cy="9" r="1" fill="#6366f1"/><circle cx="15" cy="9" r="1" fill="#6366f1"/></svg>
            </template>
          </div>
          <div class="msg-content-wrap">
            <div class="msg-bubble" :class="msg.role === 'USER' ? 'bubble-user' : 'bubble-ai'">
              <div v-if="msg.role === 'AI' && !msg.content && isLoading" class="typing-dots">
                <span></span><span></span><span></span>
              </div>
              <div v-else v-html="formatContent(msg.content)"></div>
            </div>
            <div v-if="idx === 0 || (msg._timestamp && messages[idx-1]._timestamp && (msg._timestamp - messages[idx-1]._timestamp) > 300000)" class="time-divider">
              <span>{{ formatTime(msg._timestamp || Date.now()) }}</span>
            </div>
            <div v-if="msg.role === 'AI' && msg.content && msg.content !== '正在思考...'" class="msg-actions">
              <button class="act-btn" :class="{ liked: msg._feedback === 'LIKE' }" @click="sendFeedback(msg, 'LIKE')" title="有帮助">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M14 9V5a3 3 0 00-3-3l-4 9v11h11.28a2 2 0 002-1.7l1.38-9a2 2 0 00-2-2.3H14zM7 22H4a2 2 0 01-2-2v-7a2 2 0 012-2h3"/></svg>
              </button>
              <button class="act-btn" :class="{ disliked: msg._feedback === 'DISLIKE' }" @click="sendFeedback(msg, 'DISLIKE')" title="无帮助">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M10 15v4a3 3 0 003 3l4-9V2H5.72a2 2 0 00-2 1.7l-1.38 9a2 2 0 002 2.3H10zM17 2h2.67A2.31 2.31 0 0122 4v7a2.31 2.31 0 01-2.33 2H17"/></svg>
              </button>
              <button class="act-btn" :class="{ copied: msg._copied }" @click="copyMessage(msg)" :title="msg._copied ? '已复制' : '复制'">
                <svg v-if="!msg._copied" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="9" y="9" width="13" height="13" rx="2" ry="2"/><path d="M5 15H4a2 2 0 01-2-2V4a2 2 0 012-2h9a2 2 0 012 2v1"/></svg>
                <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="20 6 9 17 4 12"/></svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-wrap">
          <input v-model="inputText" placeholder="输入你的问题..." class="chat-input" @keyup.enter="sendMessage()" :disabled="isLoading" ref="inputRef" />
          <button class="send-btn" :class="{ ready: inputText.trim() && !isLoading }" @click="sendMessage()" :disabled="!inputText.trim() || isLoading">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAuthStore } from '@/store/auth'
import { chatApi } from '@/api/chat'
import { knowledgeApi } from '@/api/knowledge'
import { authApi } from '@/api/auth'
import { useTheme } from '@/composables/useTheme'

interface Message {
  role: string
  content: string
  _feedback?: string
  _messageId?: number
  _timestamp?: number
  _copied?: boolean
}

interface Conversation {
  id: number
  title: string
  updatedAt: string
}

const router = useRouter()
const authStore = useAuthStore()
const { isDark, toggleTheme } = useTheme()

const conversations = ref<Conversation[]>([])
const messages = ref<Message[]>([])
const conversationId = ref<number | null>(null)
const inputText = ref('')
const isLoading = ref(false)
const convLoading = ref(false)
const messageAreaRef = ref<HTMLElement>()
const inputRef = ref<HTMLInputElement>()

const quickQuestions = ref<string[]>([])
const searchKeyword = ref('')

const filteredConversations = computed(() => {
  if (!searchKeyword.value.trim()) return conversations.value
  const kw = searchKeyword.value.trim().toLowerCase()
  return conversations.value.filter(c => c.title.toLowerCase().includes(kw))
})

async function loadQuickQuestions() {
  try {
    const res = await knowledgeApi.list({ page: 0, size: 6, sort: 'createdAt,desc' })
    const questions = res.data.content?.map((k: any) => k.question) || []
    if (questions.length > 0) {
      quickQuestions.value = [...new Set(questions)].slice(0, 6)
    }
  } catch {
    quickQuestions.value = [
      '如何选课？', '奖学金怎么申请？', '图书馆几点开门？',
      '宿舍怎么报修？', '校园卡丢了怎么办？', '学校地址在哪里？'
    ]
  }
}

async function loadConversations() {
  convLoading.value = true
  try {
    const res = await chatApi.getConversations()
    conversations.value = res.data
  } catch {
    ElMessage.error('加载对话列表失败')
  } finally {
    convLoading.value = false
  }
}

async function selectConversation(id: number) {
  conversationId.value = id
  try {
    const res = await chatApi.getConversation(id)
    messages.value = res.data.messages.map((m: any) => ({
      role: m.role,
      content: m.content,
      _messageId: m.id,
      _feedback: m.feedback || undefined,
      _timestamp: m.createdAt ? new Date(m.createdAt).getTime() : undefined
    }))
    scrollToBottom()
  } catch {
    ElMessage.error('加载对话失败')
  }
}

async function deleteConv(id: number) {
  try {
    await ElMessageBox.confirm('确定删除此对话？', '提示', { confirmButtonText: '确定', cancelButtonText: '取消', type: 'warning' })
    await chatApi.deleteConversation(id)
    ElMessage.success('删除成功')
    if (conversationId.value === id) newConversation()
    await loadConversations()
  } catch {}
}

function newConversation() {
  conversationId.value = null
  messages.value = []
  inputRef.value?.focus()
}

async function sendFeedback(msg: Message, type: string) {
  if (!msg._messageId) {
    ElMessage.warning('请等待回答完成后再反馈')
    return
  }
  try {
    await chatApi.feedback(msg._messageId, type)
    msg._feedback = msg._feedback === type ? undefined : type
    ElMessage.success(type === 'LIKE' ? '感谢反馈！' : '已记录，我们会改进')
  } catch {
    ElMessage.error('反馈失败')
  }
}

function copyMessage(msg: Message) {
  navigator.clipboard.writeText(msg.content).then(() => {
    msg._copied = true
    setTimeout(() => { msg._copied = false }, 2000)
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

function exportConversation() {
  const title = conversations.value.find(c => c.id === conversationId.value)?.title || '对话记录'
  let text = `${title}\n${'='.repeat(40)}\n导出时间: ${new Date().toLocaleString()}\n\n`
  messages.value.forEach(m => {
    const role = m.role === 'USER' ? '我' : 'AI助手'
    text += `【${role}】\n${m.content}\n\n`
  })
  const blob = new Blob([text], { type: 'text/plain;charset=utf-8' })
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `${title}.txt`
  a.click()
  URL.revokeObjectURL(url)
  ElMessage.success('导出成功')
}

function shareConversation() {
  const title = conversations.value.find(c => c.id === conversationId.value)?.title || '对话记录'
  let text = `📋 ${title}\n\n`
  messages.value.forEach(m => {
    const role = m.role === 'USER' ? '🙋' : '🤖'
    text += `${role} ${m.content}\n\n`
  })
  if (navigator.share) {
    navigator.share({ title, text }).catch(() => {})
  } else {
    navigator.clipboard.writeText(text).then(() => {
      ElMessage.success('对话内容已复制到剪贴板，可粘贴分享')
    }).catch(() => {
      ElMessage.error('分享失败')
    })
  }
}

async function sendMessage(preset?: string) {
  const text = preset || inputText.value.trim()
  if (!text || isLoading.value) return

  inputText.value = ''
  const now = Date.now()
  messages.value.push({ role: 'USER', content: text, _timestamp: now })
  messages.value.push({ role: 'AI' as const, content: '', _timestamp: now })
  const aiIdx = messages.value.length - 1
  scrollToBottom()
  isLoading.value = true

  try {
    const body: any = { question: text }
    if (conversationId.value) body.conversationId = conversationId.value

    const token = localStorage.getItem('token')
    const controller = new AbortController()
    const timeoutId = setTimeout(() => controller.abort(), 60000)

    const response = await fetch('/api/chat/stream', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Authorization': `Bearer ${token}` },
      body: JSON.stringify(body),
      signal: controller.signal
    })
    clearTimeout(timeoutId)

    if (!response.ok) throw new Error(`请求失败 (${response.status})`)

    const reader = response.body?.getReader()
    if (!reader) throw new Error('无法读取响应')

    const decoder = new TextDecoder()
    let buffer = ''
    let currentEvent = ''

    while (true) {
      const { done, value } = await reader.read()
      if (done) break

      buffer += decoder.decode(value, { stream: true })
      const lines = buffer.split('\n')
      buffer = lines.pop() || ''

      for (const line of lines) {
        if (line.startsWith('event:')) {
          currentEvent = line.substring(6).trim()
          continue
        }
        if (line.startsWith('data:')) {
          const dataStr = line.substring(5).trim()
          if (currentEvent === 'init') {
            try {
              const data = JSON.parse(dataStr)
              if (data.conversationId) conversationId.value = data.conversationId
            } catch {}
            messages.value[aiIdx].content = '正在思考...'
          } else if (currentEvent === 'token') {
            if (messages.value[aiIdx].content === '正在思考...') messages.value[aiIdx].content = ''
            if (dataStr) {
              messages.value[aiIdx].content += dataStr
              scrollToBottom()
            }
          } else if (currentEvent === 'done') {
            try {
              const data = JSON.parse(dataStr)
              if (data.messageId) messages.value[aiIdx]._messageId = data.messageId
              if (data.conversationId && !conversationId.value) conversationId.value = data.conversationId
            } catch {}
            isLoading.value = false
          } else if (currentEvent === 'error') {
            if (dataStr && !messages.value[aiIdx].content) {
              messages.value[aiIdx].content = '抱歉，AI服务响应出错: ' + dataStr
            }
          }
        }
      }
    }
  } catch (e: any) {
    if (messages.value[aiIdx].content && messages.value[aiIdx].content !== '正在思考...') return
    if (e.name === 'AbortError') {
      messages.value[aiIdx].content = 'AI响应超时，请重试'
    } else {
      messages.value[aiIdx].content = '请求失败: ' + (e.message || '网络错误')
    }
  } finally {
    isLoading.value = false
    if (conversationId.value) {
      const conv = conversations.value.find(c => c.id === conversationId.value)
      if (conv) {
        conv.updatedAt = new Date().toISOString()
        conversations.value.sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
      } else {
        await loadConversations()
      }
    }
    if (!messages.value[aiIdx]._messageId && conversationId.value) {
      try {
        const res = await chatApi.getConversation(conversationId.value)
        const msgs = res.data.messages
        if (msgs && msgs.length > 0) {
          const last = msgs[msgs.length - 1]
          if (last.role === 'AI' && last.id) messages.value[aiIdx]._messageId = last.id
        }
      } catch {}
    }
    scrollToBottom()
  }
}

function formatContent(text: string) {
  if (!text) return ''
  return text
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n/g, '<br>')
    .replace(/(https?:\/\/[^\s<]+)/g, '<a href="$1" target="_blank">$1</a>')
}

function formatTime(ts: number) {
  const d = new Date(ts)
  const now = new Date()
  const pad = (n: number) => n.toString().padStart(2, '0')
  const time = `${pad(d.getHours())}:${pad(d.getMinutes())}`
  if (d.toDateString() === now.toDateString()) return time
  return `${d.getMonth()+1}/${d.getDate()} ${time}`
}

function scrollToBottom() {
  nextTick(() => {
    const el = messageAreaRef.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

function handleLogout() {
  authStore.logout()
  router.push('/login')
}

loadConversations()
loadQuickQuestions()

authApi.me().then(res => {
  if (res.data.avatar) authStore.setAvatar(res.data.avatar)
  if (res.data.username) authStore.username = res.data.username
}).catch(() => {})
</script>

<style scoped>
.chat-page {
  display: flex;
  height: 100vh;
  background: var(--color-bg);
}

.sidebar {
  width: 272px;
  background: #0f0f1a;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  border-right: 1px solid rgba(255,255,255,0.04);
}

[data-theme="dark"] .sidebar {
  background: #0a0a14;
}

.sidebar-top {
  padding: 20px 14px 12px;
}

.app-brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 14px;
}

.brand-icon-wrap {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
}

.brand-logo {
  width: 100%;
  height: 100%;
}

.brand-text {
  display: flex;
  flex-direction: column;
}

.brand-name {
  font-size: 15px;
  font-weight: 700;
  color: #f0f0f5;
  line-height: 1.2;
}

.brand-sub {
  font-size: 10px;
  color: rgba(255,255,255,0.3);
  font-weight: 500;
  letter-spacing: 0.8px;
  text-transform: uppercase;
}

.new-chat-btn {
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 7px;
  padding: 9px 14px;
  background: rgba(99, 102, 241, 0.15);
  border: 1px solid rgba(99, 102, 241, 0.25);
  border-radius: 10px;
  color: #a5b4fc;
  font-size: 13px;
  font-weight: 600;
  font-family: inherit;
  cursor: pointer;
  transition: all 150ms;
}

.new-chat-btn:hover {
  background: rgba(99, 102, 241, 0.25);
  border-color: rgba(99, 102, 241, 0.4);
  color: #c7d2fe;
  transform: translateY(-1px);
}

.conv-search {
  position: relative;
  display: flex;
  align-items: center;
  margin-top: 10px;
}

.conv-search .search-icon {
  position: absolute;
  left: 10px;
  color: rgba(255,255,255,0.2);
  pointer-events: none;
}

.conv-search-input {
  width: 100%;
  padding: 7px 28px 7px 30px;
  background: rgba(255,255,255,0.04);
  border: 1px solid rgba(255,255,255,0.06);
  border-radius: 8px;
  color: rgba(255,255,255,0.75);
  font-size: 12px;
  font-family: inherit;
  outline: none;
  transition: all 150ms;
}

.conv-search-input::placeholder { color: rgba(255,255,255,0.2); }
.conv-search-input:focus { background: rgba(255,255,255,0.07); border-color: rgba(99,102,241,0.35); }

.conv-search-clear {
  position: absolute;
  right: 5px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  background: none;
  border: none;
  color: rgba(255,255,255,0.3);
  cursor: pointer;
  border-radius: 50%;
  padding: 0;
}

.conv-search-clear:hover { color: rgba(255,255,255,0.7); background: rgba(255,255,255,0.08); }

.conv-list {
  flex: 1;
  overflow-y: auto;
  padding: 4px 8px;
}

.conv-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 9px 10px;
  border-radius: 8px;
  cursor: pointer;
  transition: all 150ms;
  margin-bottom: 1px;
  color: rgba(255,255,255,0.45);
}

.conv-item:hover { background: rgba(255,255,255,0.05); color: rgba(255,255,255,0.75); }
.conv-item.active { background: rgba(99,102,241,0.15); color: #c7d2fe; }

.conv-icon { flex-shrink: 0; opacity: 0.5; }
.conv-item.active .conv-icon { opacity: 1; color: #818cf8; }

.conv-title {
  flex: 1;
  font-size: 12.5px;
  font-weight: 500;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-delete {
  opacity: 0;
  border: none;
  background: none;
  cursor: pointer;
  padding: 3px;
  border-radius: 5px;
  color: rgba(255,255,255,0.25);
  transition: all 150ms;
  display: flex;
  align-items: center;
}

.conv-item:hover .conv-delete { opacity: 1; }
.conv-delete:hover { color: #f87171; background: rgba(248,113,113,0.12); }

.empty-conv {
  text-align: center;
  padding: 28px 14px;
  font-size: 12px;
  color: rgba(255,255,255,0.2);
}

.sidebar-bottom {
  padding: 12px 14px;
  border-top: 1px solid rgba(255,255,255,0.04);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  overflow: hidden;
}

.user-avatar-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.user-meta {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.user-name {
  font-size: 13px;
  color: rgba(255,255,255,0.7);
  font-weight: 600;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-id {
  font-size: 10px;
  color: rgba(255,255,255,0.25);
}

.bottom-actions {
  display: flex;
  gap: 4px;
}

.action-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: rgba(255,255,255,0.04);
  border-radius: 8px;
  color: rgba(255,255,255,0.35);
  cursor: pointer;
  transition: all 150ms;
}

.action-icon:hover { background: rgba(255,255,255,0.08); color: rgba(255,255,255,0.7); }

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-bg);
}

.chat-topbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface);
  flex-shrink: 0;
}

.topbar-title {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.topbar-actions { display: flex; gap: 6px; }

.topbar-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: 8px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 150ms;
}

.topbar-btn:hover { border-color: var(--color-primary); color: var(--color-primary); background: rgba(99,102,241,0.04); }

.message-area {
  flex: 1;
  overflow-y: auto;
  padding: 28px 20px;
}

.welcome {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px 40px;
}

.welcome-badge {
  width: 56px;
  height: 56px;
  margin-bottom: 20px;
  filter: drop-shadow(0 4px 12px rgba(99,102,241,0.25));
}

.welcome h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--color-text-primary);
  margin-bottom: 8px;
}

.welcome p {
  font-size: 14px;
  color: var(--color-text-tertiary);
  margin-bottom: 28px;
  text-align: center;
  max-width: 420px;
  line-height: 1.6;
}

.quick-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 8px;
  max-width: 480px;
  width: 100%;
}

.quick-card {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  background: var(--color-surface);
  border-radius: 10px;
  font-size: 12.5px;
  font-weight: 500;
  color: var(--color-text-secondary);
  cursor: pointer;
  font-family: inherit;
  transition: all 150ms;
  text-align: left;
}

.quick-card:hover {
  border-color: #818cf8;
  color: #6366f1;
  background: rgba(99,102,241,0.03);
  transform: translateY(-1px);
  box-shadow: 0 2px 8px rgba(99,102,241,0.08);
}

.quick-dot {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: #818cf8;
  flex-shrink: 0;
}

.message-row {
  display: flex;
  gap: 12px;
  margin-bottom: 20px;
  max-width: 780px;
}

.msg-user {
  flex-direction: row-reverse;
  margin-left: auto;
}

.msg-content-wrap {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.msg-user .msg-content-wrap {
  align-items: flex-end;
}

.time-divider {
  text-align: center;
  padding: 8px 0 0;
}

.time-divider span {
  font-size: 11px;
  color: var(--color-text-tertiary);
  background: var(--color-bg);
  padding: 3px 12px;
  border-radius: 10px;
}

.msg-avatar {
  flex-shrink: 0;
  width: 34px;
  height: 34px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
}

.av-user {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  overflow: hidden;
}

.msg-av-img {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
}

.av-ai {
  background: rgba(99,102,241,0.08);
}

.msg-bubble {
  max-width: 62%;
  padding: 12px 16px;
  font-size: 13.5px;
  line-height: 1.75;
}

.bubble-user {
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  color: #ffffff;
  border-radius: 14px 14px 4px 14px;
}

.bubble-ai {
  background: var(--color-surface);
  color: var(--color-text-primary);
  border: 1px solid var(--color-border);
  border-radius: 14px 14px 14px 4px;
}

.bubble-ai :deep(a) { color: #6366f1; text-decoration: underline; }
.bubble-ai :deep(strong) { color: var(--color-text-primary); font-weight: 600; }

.typing-dots {
  display: flex;
  gap: 4px;
  padding: 4px 0;
}

.typing-dots span {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #818cf8;
  animation: dotBounce 1.4s infinite both;
}

.typing-dots span:nth-child(2) { animation-delay: 0.2s; }
.typing-dots span:nth-child(3) { animation-delay: 0.4s; }

@keyframes dotBounce {
  0%, 80%, 100% { transform: scale(0.4); opacity: 0.3; }
  40% { transform: scale(1); opacity: 1; }
}

.msg-actions {
  display: flex;
  gap: 3px;
  margin-top: 4px;
  padding-left: 2px;
}

.act-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border: none;
  background: transparent;
  border-radius: 6px;
  color: var(--color-text-tertiary);
  cursor: pointer;
  transition: all 150ms;
}

.act-btn:hover { background: var(--color-bg); color: var(--color-text-secondary); }
.act-btn.liked { color: #6366f1; background: rgba(99,102,241,0.08); }
.act-btn.disliked { color: #f59e0b; background: rgba(245,158,11,0.08); }
.act-btn.copied { color: #10b981; background: rgba(16,185,129,0.08); }

.input-area {
  padding: 12px 20px 16px;
  border-top: 1px solid var(--color-border);
  background: var(--color-surface);
  flex-shrink: 0;
}

.input-wrap {
  display: flex;
  gap: 10px;
  align-items: center;
  max-width: 780px;
  margin: 0 auto;
}

.chat-input {
  flex: 1;
  padding: 11px 18px;
  font-size: 14px;
  font-family: inherit;
  color: var(--color-text-primary);
  background: var(--color-bg);
  border: 1.5px solid var(--color-border);
  border-radius: 24px;
  outline: none;
  transition: all 150ms;
}

.chat-input:focus { border-color: #6366f1; box-shadow: 0 0 0 3px rgba(99,102,241,0.08); }
.chat-input::placeholder { color: var(--color-text-tertiary); }
.chat-input:disabled { opacity: 0.5; cursor: not-allowed; }

.send-btn {
  width: 42px;
  height: 42px;
  border-radius: 50%;
  border: 1.5px solid var(--color-border);
  background: var(--color-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 150ms;
  flex-shrink: 0;
  color: var(--color-text-tertiary);
}

.send-btn.ready {
  background: linear-gradient(135deg, #6366f1, #7c3aed);
  border-color: transparent;
  color: #ffffff;
  box-shadow: 0 2px 8px rgba(99,102,241,0.3);
}

.send-btn.ready:hover { transform: scale(1.06); box-shadow: 0 4px 14px rgba(99,102,241,0.4); }
.send-btn:disabled { cursor: not-allowed; }

.conv-list :deep(.el-loading-mask) { background: rgba(15,15,26,0.85); }
.conv-list :deep(.el-loading-spinner .circular) { width: 24px; height: 24px; }
.conv-list :deep(.el-loading-spinner .path) { stroke: #818cf8; }
</style>
