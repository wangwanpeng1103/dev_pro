<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { copyTextToClipboard } from './clipboard'

interface ApiResponse<T> { success: boolean; message: string; data: T }
interface PageResult<T> { records: T[]; total: number; current: number; size: number; pages: number }
interface MockCompany {
  id: number
  enterpriseCode: string
  enterpriseName: string
  openStatus: number
  createdAt: string
  updatedAt: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''
const records = ref<MockCompany[]>([])
const page = ref(1)
const pageSize = 10
const total = ref(0)
const pages = ref(1)
const keyword = ref('')
const loading = ref(false)
const saving = ref(false)
const message = ref('')
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = ref({ enterpriseCode: '', enterpriseName: '', openStatus: 1 })
const mockApiUrl = computed(() => new URL('/api/ihotel/tmh-mock-companies/mock-api', apiBaseUrl || window.location.origin).toString())

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: { 'Content-Type': 'application/json', ...options?.headers },
    ...options
  })
  const result = await response.json() as ApiResponse<T>
  if (!response.ok || !result.success) {
    throw new Error(result.message || `请求失败：${response.status}`)
  }
  return result.data
}

async function loadRecords() {
  loading.value = true
  message.value = ''
  const query = new URLSearchParams({ page: String(page.value), pageSize: String(pageSize) })
  if (keyword.value.trim()) query.set('keyword', keyword.value.trim())
  try {
    const result = await request<PageResult<MockCompany>>(`/api/ihotel/tmh-mock-companies?${query}`)
    records.value = result.records
    total.value = result.total
    pages.value = Math.max(1, result.pages)
  } catch (error) {
    message.value = error instanceof Error ? error.message : '查询失败'
  } finally {
    loading.value = false
  }
}

function queryRecords() {
  page.value = 1
  void loadRecords()
}

function openCreateDialog() {
  editingId.value = null
  form.value = { enterpriseCode: '', enterpriseName: '', openStatus: 1 }
  dialogVisible.value = true
}

function openEditDialog(record: MockCompany) {
  editingId.value = record.id
  form.value = {
    enterpriseCode: record.enterpriseCode,
    enterpriseName: record.enterpriseName,
    openStatus: record.openStatus
  }
  dialogVisible.value = true
}

async function saveCompany() {
  saving.value = true
  message.value = ''
  try {
    const path = editingId.value === null ? 'create' : 'update'
    const payload = editingId.value === null
      ? { ...form.value }
      : { id: editingId.value, enterpriseName: form.value.enterpriseName, openStatus: form.value.openStatus }
    await request<MockCompany>(`/api/ihotel/tmh-mock-companies/${path}`, {
      method: 'POST',
      body: JSON.stringify(payload)
    })
    dialogVisible.value = false
    message.value = editingId.value === null ? '新增成功' : '修改成功'
    await loadRecords()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '保存失败'
  } finally {
    saving.value = false
  }
}

async function changeStatus(record: MockCompany) {
  message.value = ''
  try {
    await request<MockCompany>('/api/ihotel/tmh-mock-companies/change-status', {
      method: 'POST',
      body: JSON.stringify({ id: record.id, openStatus: record.openStatus === 1 ? 0 : 1 })
    })
    message.value = record.openStatus === 1 ? '已停用' : '已启用'
    await loadRecords()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '状态修改失败'
  }
}

async function deleteCompany(record: MockCompany) {
  if (!window.confirm(`确定删除 ${record.enterpriseCode}（${record.enterpriseName}）吗？`)) return
  message.value = ''
  try {
    await request<void>(`/api/ihotel/tmh-mock-companies/${record.id}/delete`, { method: 'POST' })
    message.value = '删除成功'
    if (records.value.length === 1 && page.value > 1) page.value -= 1
    await loadRecords()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '删除失败'
  }
}
async function copyApiUrl() {
  message.value = await copyTextToClipboard(mockApiUrl.value)
    ? '模拟接口地址已复制'
    : '复制失败，请手动选择接口地址复制'
}

async function changePage(targetPage: number) {
  page.value = Math.min(Math.max(targetPage, 1), pages.value)
  await loadRecords()
}

onMounted(loadRecords)
</script>

<template>
  <section class="tmh-panel">
    <div class="tmh-header">
      <div>
        <p class="eyebrow">TMH MOCK API</p>
        <h3>天目湖接口模拟数据</h3>
      </div>
      <button class="primary-button" type="button" @click="openCreateDialog">新增协议单位</button>
    </div>

    <div class="api-address">
      <div><small>PMS 拉取地址</small><code>{{ mockApiUrl }}</code></div>
      <button class="ghost-button" type="button" @click="copyApiUrl">复制地址</button>
    </div>

    <form class="query-bar" @submit.prevent="queryRecords">
      <input v-model.trim="keyword" autocomplete="off" placeholder="按企业编码或名称查询" />
      <button class="primary-button" type="submit" :disabled="loading">{{ loading ? '查询中...' : '查询' }}</button>
    </form>
    <p v-if="message" class="tmh-message">{{ message }}</p>

    <div class="tmh-table">
      <div class="tmh-row tmh-head"><span>企业编码</span><span>企业名称</span><span>状态</span><span>更新时间</span><span>操作</span></div>
      <div v-for="record in records" :key="record.id" class="tmh-row">
        <strong>{{ record.enterpriseCode }}</strong>
        <span>{{ record.enterpriseName }}</span>
        <span><em :class="record.openStatus === 1 ? 'enabled' : 'disabled'">{{ record.openStatus === 1 ? '启用' : '停用' }}</em></span>
        <span>{{ record.updatedAt?.replace('T', ' ') }}</span>
        <div class="row-actions">
          <button class="ghost-button" type="button" @click="openEditDialog(record)">修改</button>
          <button :class="record.openStatus === 1 ? 'danger-button' : 'primary-button'" type="button" @click="changeStatus(record)">
            {{ record.openStatus === 1 ? '停用' : '启用' }}
          </button>
          <button class="danger-button" type="button" @click="deleteCompany(record)">删除</button>
        </div>
      </div>
      <div v-if="!loading && records.length === 0" class="empty-state">暂无模拟协议单位</div>
    </div>
    <div class="page-bar">
      <span>共 {{ total }} 条</span>
      <div><button class="ghost-button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button><span>{{ page }} / {{ pages }}</span><button class="ghost-button" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button></div>
    </div>

    <div v-if="dialogVisible" class="dialog-backdrop">
      <form class="form-dialog tmh-dialog" @submit.prevent="saveCompany">
        <div class="dialog-header"><h3>{{ editingId === null ? '新增模拟协议单位' : '修改模拟协议单位' }}</h3><button class="icon-close-button" type="button" @click="dialogVisible = false">×</button></div>
        <label>
          企业编码
          <input v-model.trim="form.enterpriseCode" maxlength="64" :readonly="editingId !== null" required />
        </label>
        <label>企业名称<input v-model.trim="form.enterpriseName" maxlength="200" required /></label>
        <label>状态<select v-model.number="form.openStatus"><option :value="1">启用</option><option :value="0">停用</option></select></label>
        <div class="dialog-actions"><button class="ghost-button" type="button" @click="dialogVisible = false">取消</button><button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button></div>
      </form>
    </div>
  </section>
</template>

<style scoped>
.tmh-panel { background: #fff; border: 1px solid #e5e7eb; border-radius: 18px; padding: 24px; }
.tmh-header,.api-address,.query-bar,.page-bar,.page-bar>div,.row-actions { display:flex; align-items:center; gap:12px; }
.tmh-header,.api-address,.page-bar { justify-content:space-between; }
.tmh-header h3 { margin:4px 0 0; }
.api-address { margin:20px 0; padding:14px 16px; background:#f8fafc; border-radius:12px; }
.api-address div { display:grid; gap:5px; min-width:0; }.api-address code { overflow-wrap:anywhere; color:#0f766e; }
.query-bar input { width:min(420px, 70vw); }.tmh-message { color:#0f766e; }
.tmh-table { margin-top:18px; border:1px solid #e5e7eb; border-radius:12px; overflow:hidden; }
.tmh-row { display:grid; grid-template-columns:1.2fr 1.5fr .6fr 1fr 1fr; gap:14px; align-items:center; padding:13px 16px; border-top:1px solid #edf0f3; }
.tmh-head { border-top:0; background:#f8fafc; font-weight:700; }.tmh-row em { padding:4px 9px; border-radius:999px; font-style:normal; }.enabled { color:#047857;background:#d1fae5; }.disabled { color:#b91c1c;background:#fee2e2; }
.row-actions button { padding:7px 12px; }.page-bar { margin-top:16px; }.tmh-dialog { display:grid; gap:14px; }.tmh-dialog label { display:grid; gap:7px; }
.tmh-dialog input[readonly] { color:#64748b; background:#f1f5f9; cursor:not-allowed; }
@media (max-width:900px) { .tmh-row { grid-template-columns:1fr; }.tmh-head { display:none; }.api-address { align-items:flex-start; }.tmh-header { align-items:flex-start; } }
</style>
