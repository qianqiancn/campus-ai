import request from './request'

export const authApi = {
  login(data: { studentId: string; password: string }) {
    return request.post('/api/auth/login', data)
  },
  register(data: { studentId: string; username: string; password: string }) {
    return request.post('/api/auth/register', data)
  },
  me() {
    return request.get('/api/auth/me')
  }
}
