import request from './request'

export const knowledgeApi = {
  list(params: any) {
    return request.get('/api/knowledge', { params })
  },
  get(id: number) {
    return request.get(`/api/knowledge/${id}`)
  },
  create(data: any) {
    return request.post('/api/knowledge', data)
  },
  update(id: number, data: any) {
    return request.put(`/api/knowledge/${id}`, data)
  },
  delete(id: number) {
    return request.delete(`/api/knowledge/${id}`)
  },
  deleteBatch(ids: number[]) {
    return request.delete('/api/knowledge/batch', { data: ids })
  },
  search(keyword: string) {
    return request.get('/api/knowledge/search', { params: { keyword } })
  },
  categories() {
    return request.get('/api/knowledge/categories')
  },
  importExcel(file: File) {
    const formData = new FormData()
    formData.append('file', file)
    return request.post('/api/knowledge/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  }
}
