import request from './request'

export const chatApi = {
  send(data: { question: string; conversationId?: number }) {
    return request.post('/api/chat', data)
  },
  health() {
    return request.get('/api/chat/health')
  },
  getConversations() {
    return request.get('/api/conversations')
  },
  getConversation(id: number) {
    return request.get(`/api/conversations/${id}`)
  },
  deleteConversation(id: number) {
    return request.delete(`/api/conversations/${id}`)
  },
  feedback(messageId: number, type: string) {
    return request.post('/api/feedback', { messageId, type })
  }
}
