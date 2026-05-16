import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 60000
})

request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  response => response,
  error => {
    if (error.response?.status === 401) {
      const authStore = (window as any).__pinia?.state?.value?.auth
      if (authStore) {
        authStore.token = ''
        authStore.role = ''
        authStore.username = ''
        authStore.studentId = ''
      }
      localStorage.removeItem('token')
      localStorage.removeItem('role')
      localStorage.removeItem('username')
      localStorage.removeItem('studentId')
      if (router.currentRoute.value.path !== '/login') {
        router.push('/login')
        ElMessage.error('登录已过期，请重新登录')
      }
    } else if (error.response?.status === 403) {
      ElMessage.error('无权访问')
    } else {
      ElMessage.error(error.response?.data?.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default request
