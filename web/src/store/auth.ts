import { defineStore } from 'pinia'
import { ref } from 'vue'
import { authApi } from '@/api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const role = ref(localStorage.getItem('role') || '')
  const username = ref(localStorage.getItem('username') || '')
  const studentId = ref(localStorage.getItem('studentId') || '')
  const avatar = ref(localStorage.getItem('avatar') || '')
  const initialized = ref(false)

  function setAuth(t: string, r: string, u: string, s: string, a?: string) {
    token.value = t
    role.value = r
    username.value = u
    studentId.value = s
    if (a) { avatar.value = a; localStorage.setItem('avatar', a) }
    localStorage.setItem('token', t)
    localStorage.setItem('role', r)
    localStorage.setItem('username', u)
    localStorage.setItem('studentId', s)
  }

  function setAvatar(a: string) {
    avatar.value = a
    localStorage.setItem('avatar', a)
  }

  function logout() {
    token.value = ''
    role.value = ''
    username.value = ''
    studentId.value = ''
    avatar.value = ''
    localStorage.removeItem('token')
    localStorage.removeItem('role')
    localStorage.removeItem('username')
    localStorage.removeItem('studentId')
    localStorage.removeItem('avatar')
  }

  async function validateAndInit(): Promise<boolean> {
    if (!token.value) {
      initialized.value = true
      return false
    }
    try {
      const res = await authApi.me()
      username.value = res.data.username || ''
      studentId.value = res.data.studentId || ''
      role.value = res.data.role || ''
      if (res.data.avatar) { avatar.value = res.data.avatar; localStorage.setItem('avatar', res.data.avatar) }
      initialized.value = true
      return true
    } catch {
      logout()
      initialized.value = true
      return false
    }
  }

  return { token, role, username, studentId, avatar, initialized, setAuth, setAvatar, logout, validateAndInit }
})
