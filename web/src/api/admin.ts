import request from './request'

export const adminApi = {
  stats() {
    return request.get('/api/admin/stats')
  },
  dailyStats(days: number = 7) {
    return request.get('/api/admin/stats/daily', { params: { days } })
  },
  categoryStats() {
    return request.get('/api/admin/stats/category')
  },
  listUsers() {
    return request.get('/api/admin/users')
  },
  updateUser(id: number, data: any) {
    return request.put(`/api/admin/users/${id}`, data)
  },
  deleteUser(id: number) {
    return request.delete(`/api/admin/users/${id}`)
  },
  listFeedbacks() {
    return request.get('/api/admin/feedbacks')
  },
  feedbackStats() {
    return request.get('/api/admin/feedbacks/stats')
  }
}
