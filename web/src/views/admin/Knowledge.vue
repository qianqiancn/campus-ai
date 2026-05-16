<template>
  <div class="knowledge-page">
    <div class="page-header">
      <h2>知识库管理</h2>
      <div class="header-actions">
        <el-button
          v-if="selectedIds.length > 0"
          type="danger"
          @click="handleBatchDelete"
          :loading="batchDeleting"
        >
          批量删除 ({{ selectedIds.length }})
        </el-button>
        <el-button type="primary" @click="openDialog()">新增知识</el-button>
        <el-upload
          :show-file-list="false"
          :before-upload="handleImport"
          accept=".xlsx,.xls"
        >
          <el-button>批量导入Excel</el-button>
        </el-upload>
      </div>
    </div>

    <div class="search-bar">
      <el-input
        v-model="keyword"
        placeholder="搜索知识库..."
        clearable
        @input="handleSearch"
        prefix-icon="Search"
        style="width:400px"
      />
    </div>

    <el-table
      :data="knowledgeList"
      v-loading="loading"
      stripe
      border
      style="width:100%"
      @selection-change="handleSelectionChange"
    >
      <el-table-column type="selection" width="50" />
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="question" label="问题" min-width="200">
        <template #default="{ row }">
          <span v-if="!keyword.trim()">{{ row.question }}</span>
          <span v-else v-html="highlightText(row.question, keyword)"></span>
        </template>
      </el-table-column>
      <el-table-column prop="category" label="分类" width="100">
        <template #default="{ row }">
          <el-tag size="small">{{ row.category }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="keywords" label="关键词" width="180" show-overflow-tooltip />
      <el-table-column prop="createdAt" label="创建时间" width="170">
        <template #default="{ row }">
          {{ formatDate(row.createdAt) }}
        </template>
      </el-table-column>
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openDialog(row)">编辑</el-button>
          <el-button text type="danger" size="small" @click="handleDelete(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="pageSize"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="loadData"
      />
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑知识' : '新增知识'"
      width="650px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="问题" prop="question">
          <el-input v-model="form.question" placeholder="请输入常见问题" />
        </el-form-item>
        <el-form-item label="答案" prop="answer">
          <el-input
            v-model="form.answer"
            type="textarea"
            :rows="6"
            placeholder="请输入详细答案"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.category" filterable allow-create placeholder="选择或输入分类">
            <el-option v-for="c in categories" :key="c" :label="c" :value="c" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keywords" placeholder="多个关键词用逗号分隔" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { knowledgeApi } from '@/api/knowledge'
import type { UploadProps } from 'element-plus'

interface KnowledgeItem {
  id: number
  question: string
  answer: string
  category: string
  keywords: string
  createdAt: string
}

const knowledgeList = ref<KnowledgeItem[]>([])
const categories = ref<string[]>([])
const loading = ref(false)
const submitting = ref(false)
const batchDeleting = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const keyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const selectedIds = ref<number[]>([])

const form = reactive({
  question: '',
  answer: '',
  category: '',
  keywords: ''
})

const rules = {
  question: [{ required: true, message: '请输入问题', trigger: 'blur' }],
  answer: [{ required: true, message: '请输入答案', trigger: 'blur' }]
}

const formRef = ref()

function handleSelectionChange(rows: KnowledgeItem[]) {
  selectedIds.value = rows.map(r => r.id)
}

async function loadData() {
  loading.value = true
  try {
    const res = await knowledgeApi.list({ page: page.value - 1, size: pageSize.value })
    knowledgeList.value = res.data.content
    total.value = res.data.totalElements
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

async function loadCategories() {
  try {
    const res = await knowledgeApi.categories()
    categories.value = res.data
  } catch {
    ElMessage.error('加载分类失败')
  }
}

async function handleSearch() {
  if (!keyword.value.trim()) {
    loadData()
    return
  }
  loading.value = true
  try {
    const res = await knowledgeApi.search(keyword.value)
    knowledgeList.value = res.data
    total.value = res.data.length
  } catch {
    ElMessage.error('搜索失败')
  } finally {
    loading.value = false
  }
}

function openDialog(row?: KnowledgeItem) {
  if (row) {
    isEdit.value = true
    editId.value = row.id
    form.question = row.question
    form.answer = row.answer
    form.category = row.category
    form.keywords = row.keywords || ''
  } else {
    isEdit.value = false
    editId.value = null
    form.question = ''
    form.answer = ''
    form.category = ''
    form.keywords = ''
  }
  dialogVisible.value = true
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value && editId.value) {
      await knowledgeApi.update(editId.value, form)
      ElMessage.success('更新成功')
    } else {
      await knowledgeApi.create(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
    loadCategories()
  } catch {
    ElMessage.error('操作失败，可能已存在相同问题')
  } finally {
    submitting.value = false
  }
}

async function handleDelete(id: number) {
  try {
    await ElMessageBox.confirm('确定删除此知识条目？', '提示', { type: 'warning' })
    await knowledgeApi.delete(id)
    ElMessage.success('删除成功')
    loadData()
  } catch {
  }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(
      `确定删除选中的 ${selectedIds.value.length} 条知识条目？`,
      '批量删除确认',
      { type: 'warning' }
    )
    batchDeleting.value = true
    await knowledgeApi.deleteBatch(selectedIds.value)
    ElMessage.success(`成功删除 ${selectedIds.value.length} 条`)
    selectedIds.value = []
    loadData()
    loadCategories()
  } catch {
    // 用户取消删除
  } finally {
    batchDeleting.value = false
  }
}

const handleImport: UploadProps['beforeUpload'] = async (file) => {
  try {
    const res = await knowledgeApi.importExcel(file)
    ElMessage.success(`导入成功，共${res.data.count}条`)
    loadData()
    loadCategories()
  } catch {
    ElMessage.error('导入失败')
  }
  return false
}

function formatDate(dateStr: string) {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

function highlightText(text: string, term: string) {
  if (!term.trim()) return text
  const escaped = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const regex = new RegExp(`(${escaped})`, 'gi')
  return text.replace(regex, '<mark class="search-highlight">$1</mark>')
}

onMounted(() => {
  loadData()
  loadCategories()
})
</script>

<style scoped>
.knowledge-page {
  padding: 0;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h2 {
  font-size: 20px;
  font-weight: 700;
  color: var(--color-text-primary);
}

.header-actions {
  display: flex;
  gap: 12px;
  align-items: center;
}

.search-bar {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
  padding: 4px 0;
}

:deep(.search-highlight) {
  background: #fef08a;
  color: #854d0e;
  padding: 1px 3px;
  border-radius: 3px;
  font-weight: 600;
}
</style>
