<script setup lang="ts">
import { onMounted, ref } from 'vue'

interface GroupHotelConfig {
  id: number
  hotelCode: string
  hotelName: string | null
  entityType: 'GROUP' | 'HOTEL' | null
  addressConfig: string | null
  databaseUsername: string | null
  databaseHost: string | null
  databasePassword: string | null
  databasePort: number | null
  sshUsername: string | null
  sshHost: string | null
  sshPassword: string | null
  sshPort: number | null
}

interface PageResult<T> {
  records: T[]
  total: number
  current: number
  pages: number
}

interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

const records = ref<GroupHotelConfig[]>([])
const entityType = ref<'' | 'GROUP' | 'HOTEL'>('')
const keyword = ref('')
const page = ref(1)
const total = ref(0)
const pages = ref(1)
const loading = ref(false)
const message = ref('')
const createDialogVisible = ref(false)
const editingId = ref<number | null>(null)
const saving = ref(false)
const deletingId = ref<number | null>(null)
const createMessage = ref('')
const sqlYogFileInput = ref<HTMLInputElement | null>(null)
const createForm = ref({
  hotelCode: '',
  hotelName: '',
  entityType: 'GROUP' as 'GROUP' | 'HOTEL',
  useSsh: false,
  addressConfig: '',
  databaseUsername: '',
  databaseHost: '',
  databasePassword: '',
  databasePort: 3306,
  sshUsername: '',
  sshHost: '',
  sshPassword: '',
  sshPort: 22
})

async function loadRecords() {
  loading.value = true
  message.value = ''
  try {
    const query = new URLSearchParams({ page: String(page.value), pageSize: '10' })
    if (entityType.value) query.set('entityType', entityType.value)
    if (keyword.value.trim()) query.set('keyword', keyword.value.trim())
    const response = await fetch(`/api/group-hotel-management/configs?${query.toString()}`)
    const result = (await response.json()) as ApiResponse<PageResult<GroupHotelConfig>>
    if (!response.ok || !result.success) throw new Error(result.message || '集团酒店配置加载失败')
    records.value = result.data.records
    total.value = result.data.total
    pages.value = Math.max(1, result.data.pages)
  } catch (error) {
    records.value = []
    message.value = error instanceof Error ? error.message : '集团酒店配置加载失败'
  } finally {
    loading.value = false
  }
}

function openSqlYogFilePicker() {
  sqlYogFileInput.value?.click()
}

async function handleSqlYogFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) return
  createMessage.value = ''
  try {
    const content = await file.text()
    const values = parseSqlYogConnectionFile(content)
    const databaseHost = findConnectionValue(values, ['host', 'mysqlhost', 'server', 'hostname'])
    const databaseUsername = findConnectionValue(values, ['user', 'username', 'mysqluser'])
    if (!databaseHost || !databaseUsername) throw new Error('未识别到数据库地址或数据库用户，请确认文件由 SQLyog 连接导出功能生成')
    createForm.value.databaseHost = databaseHost
    createForm.value.databaseUsername = databaseUsername
    createForm.value.databasePassword = decodeSqlYogPassword(
      findConnectionValue(values, ['password', 'pwd', 'mysqlpassword', 'mysqlpwd']),
      '数据库密码'
    )
    createForm.value.databasePort = parseConnectionPort(findConnectionValue(values, ['port', 'mysqlport']), 3306)
    const sshHost = findConnectionValue(values, ['sshhost', 'sshhostname', 'sshserver'])
    const sshUsername = findConnectionValue(values, ['sshuser', 'sshusername'])
    const encryptedSshPassword = findConnectionValue(values, ['sshpassword', 'sshpwd', 'sshpassphrase'])
    const sshPassword = decodeSqlYogPassword(encryptedSshPassword, 'SSH密码')
    const hasSsh = Boolean(sshHost || sshUsername || sshPassword)
    createForm.value.useSsh = hasSsh
    createForm.value.sshHost = hasSsh ? sshHost : ''
    createForm.value.sshUsername = hasSsh ? sshUsername : ''
    createForm.value.sshPassword = hasSsh ? sshPassword : ''
    createForm.value.sshPort = hasSsh
      ? parseConnectionPort(findConnectionValue(values, ['sshport']), 22)
      : 22
  } catch (error) {
    createMessage.value = error instanceof Error ? error.message : 'SQLyog 连接文件解析失败'
  }
}

function decodeSqlYogPassword(encryptedValue: string, fieldName: string) {
  if (!encryptedValue) return ''
  try {
    const decodedData = atob(encryptedValue)
    const decodedBytes = new Uint8Array(decodedData.length)
    for (let index = 0; index < decodedData.length; index += 1) {
      const byte = decodedData.charCodeAt(index)
      decodedBytes[index] = ((byte << 1) & 255) | (byte >> 7)
    }
    return new TextDecoder('utf-8', { fatal: true }).decode(decodedBytes)
  } catch {
    throw new Error(`${fieldName}解密失败，请确认使用的是 SQLyog 导出的连接文件`)
  }
}
function parseSqlYogConnectionFile(content: string) {
  if (!content.trim()) throw new Error('SQLyog 连接文件为空')
  const values = new Map<string, string>()
  const addValue = (key: string, value: string | null | undefined) => {
    const normalizedValue = value?.trim() ?? ''
    if (normalizedValue) values.set(normalizeConnectionKey(key), normalizedValue)
  }
  if (content.trimStart().startsWith('<')) {
    const documentNode = new DOMParser().parseFromString(content, 'application/xml')
    if (documentNode.querySelector('parsererror')) throw new Error('SQLyog XML 连接文件格式无效')
    documentNode.querySelectorAll('*').forEach((element) => {
      if (element.children.length === 0) addValue(element.tagName, element.textContent)
      for (const attribute of Array.from(element.attributes)) addValue(attribute.name, attribute.value)
    })
  }
  for (const line of content.split(/\r?\n/)) {
    const match = line.match(/^\s*([^#;][^=:]{0,80})\s*[=:]\s*(.*?)\s*$/)
    if (match) addValue(match[1], match[2].replace(/^['"]|['"]$/g, ''))
  }
  return values
}

function normalizeConnectionKey(key: string) {
  return key.toLowerCase().replace(/[^a-z0-9]/g, '')
}

function findConnectionValue(values: Map<string, string>, aliases: string[]) {
  for (const alias of aliases) {
    const value = values.get(normalizeConnectionKey(alias))
    if (value) return value
  }
  return ''
}

function parseConnectionPort(value: string, defaultPort: number) {
  if (!value) return defaultPort
  const port = Number(value)
  if (!Number.isInteger(port) || port < 1 || port > 65535) throw new Error(`连接端口 ${value} 无效`)
  return port
}
function openCreateDialog() {
  editingId.value = null
  createForm.value = {
    hotelCode: '', hotelName: '', entityType: 'GROUP', useSsh: false, addressConfig: '',
    databaseUsername: '', databaseHost: '', databasePassword: '', databasePort: 3306,
    sshUsername: '', sshHost: '', sshPassword: '', sshPort: 22
  }
  createMessage.value = ''
  createDialogVisible.value = true
}

function openEditDialog(record: GroupHotelConfig) {
  editingId.value = record.id
  createForm.value = {
    hotelCode: record.hotelCode,
    hotelName: record.hotelName ?? '',
    entityType: record.entityType ?? 'GROUP',
    useSsh: Boolean(record.sshUsername || record.sshHost || record.sshPassword),
    addressConfig: record.addressConfig ?? '',
    databaseUsername: record.databaseUsername ?? '',
    databaseHost: record.databaseHost ?? '',
    databasePassword: record.databasePassword ?? '',
    databasePort: record.databasePort ?? 3306,
    sshUsername: record.sshUsername ?? '',
    sshHost: record.sshHost ?? '',
    sshPassword: record.sshPassword ?? '',
    sshPort: record.sshUsername || record.sshHost || record.sshPassword ? (record.sshPort ?? 22) : 22
  }
  createMessage.value = ''
  createDialogVisible.value = true
}
function closeCreateDialog() {
  if (!saving.value) createDialogVisible.value = false
}

async function submitCreateForm() {
  const normalizedHotelCode = createForm.value.hotelCode.trim().toUpperCase()
  if (!normalizedHotelCode) {
    createMessage.value = '请填写集团酒店代码'
    return
  }
  createForm.value.hotelCode = normalizedHotelCode
  saving.value = true
  createMessage.value = ''
  try {
    const requestUrl = editingId.value === null
      ? '/api/group-hotel-management/configs/create'
      : `/api/group-hotel-management/configs/update?id=${editingId.value}`
    const response = await fetch(requestUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        hotelCode: createForm.value.hotelCode,
        hotelName: createForm.value.hotelName,
        entityType: createForm.value.entityType,
        addressConfig: createForm.value.addressConfig,
        databaseUsername: createForm.value.databaseUsername,
        databaseHost: createForm.value.databaseHost,
        databasePassword: createForm.value.databasePassword,
        databasePort: createForm.value.databasePort,
        ...(createForm.value.useSsh ? {
          sshUsername: createForm.value.sshUsername,
          sshHost: createForm.value.sshHost,
          sshPassword: createForm.value.sshPassword,
          sshPort: createForm.value.sshPort
        } : {})
      })
    })
    const result = (await response.json()) as ApiResponse<GroupHotelConfig>
    if (!response.ok || !result.success) throw new Error(result.message || (editingId.value === null ? '新增酒店集团失败' : '修改酒店集团失败'))
    createDialogVisible.value = false
    if (editingId.value === null) page.value = 1
    await loadRecords()
  } catch (error) {
    createMessage.value = error instanceof Error ? error.message : (editingId.value === null ? '新增酒店集团失败' : '修改酒店集团失败')
  } finally {
    saving.value = false
  }
}
async function deleteRecord(record: GroupHotelConfig) {
  if (!window.confirm(`确认删除集团酒店配置“${record.hotelCode}”吗？`)) return
  deletingId.value = record.id
  message.value = ''
  try {
    const response = await fetch(`/api/group-hotel-management/configs/${record.id}/delete`, { method: 'POST' })
    const result = (await response.json()) as ApiResponse<null>
    if (!response.ok || !result.success) throw new Error(result.message || '删除集团酒店配置失败')
    if (records.value.length === 1 && page.value > 1) page.value -= 1
    await loadRecords()
  } catch (error) {
    message.value = error instanceof Error ? error.message : '删除集团酒店配置失败'
  } finally {
    deletingId.value = null
  }
}
function queryRecords() {
  page.value = 1
  void loadRecords()
}

function changePage(target: number) {
  if (target < 1 || target > pages.value || target === page.value) return
  page.value = target
  void loadRecords()
}

onMounted(loadRecords)
</script>

<template>
  <section class="group-hotel-panel">
    <div class="group-hotel-header">
      <div>
        <p class="eyebrow">GROUP HOTEL MANAGEMENT</p>
        <h3>集团酒店列表</h3>
      </div>
      <form class="group-hotel-query" @submit.prevent="queryRecords">
        <button class="primary-button group-hotel-add-button" type="button" @click="openCreateDialog">新增酒店集团</button>
        <select v-model="entityType" aria-label="类型">
          <option value="">全部类型</option>
          <option value="GROUP">集团</option>
          <option value="HOTEL">酒店</option>
        </select>
        <input v-model="keyword" type="search" placeholder="输入集团酒店代码或名称" />
        <button class="primary-button group-hotel-query-button" type="submit" :disabled="loading">查询</button>
      </form>
    </div>

    <p v-if="message" class="group-hotel-message">{{ message }}</p>
    <div class="group-hotel-table-wrap">
      <table class="group-hotel-table">
        <thead>
          <tr>
            <th>集团酒店代码</th><th>酒店名称</th><th>类型</th><th>地址服务配置</th>
            <th>数据库用户</th><th>数据库地址</th><th>数据库密码</th><th>数据库端口</th>
            <th>SSH 用户</th><th>SSH 地址</th><th>SSH 密码</th><th>SSH 端口</th><th>操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="record in records" :key="record.id">
            <td class="hotel-code-cell">{{ record.hotelCode }}</td><td>{{ record.hotelName }}</td><td>{{ record.entityType === 'GROUP' ? '集团' : '酒店' }}</td><td>{{ record.addressConfig }}</td>
            <td>{{ record.databaseUsername }}</td><td>{{ record.databaseHost }}</td><td>{{ record.databasePassword }}</td><td>{{ record.databasePort }}</td>
            <td>{{ record.sshUsername || '-' }}</td><td>{{ record.sshHost || '-' }}</td><td>{{ record.sshPassword || '-' }}</td><td>{{ record.sshPort || '-' }}</td>
            <td><div class="table-actions"><button class="table-edit-button" type="button" @click="openEditDialog(record)">修改</button><button class="table-delete-button" type="button" :disabled="deletingId === record.id" @click="deleteRecord(record)">{{ deletingId === record.id ? '删除中...' : '删除' }}</button></div></td>
          </tr>
          <tr v-if="!loading && records.length === 0"><td colspan="13" class="group-hotel-empty">暂无集团酒店配置</td></tr>
        </tbody>
      </table>
    </div>
    <div class="pagination-bar">
      <span>共 {{ total }} 条</span>
      <div><button type="button" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button><span>第 {{ page }} / {{ pages }} 页</span><button type="button" :disabled="page >= pages" @click="changePage(page + 1)">下一页</button></div>
    </div>
  </section>

  <div v-if="createDialogVisible" class="group-hotel-dialog-backdrop" role="presentation">
    <form class="group-hotel-dialog" @submit.prevent="submitCreateForm">
      <div class="group-hotel-dialog-header">
        <h3>{{ editingId === null ? '新增酒店集团' : '修改酒店集团' }}</h3>
        <button type="button" aria-label="关闭" @click="closeCreateDialog">×</button>
      </div>
      <div class="group-hotel-form-grid">
        <label>类型<select v-model="createForm.entityType"><option value="GROUP">集团</option><option value="HOTEL">酒店</option></select></label>
        <label>集团酒店代码<input v-model.trim="createForm.hotelCode" class="hotel-code-input" maxlength="32" required @input="createForm.hotelCode = createForm.hotelCode.toUpperCase()" /></label>
        <label>酒店名称<input v-model.trim="createForm.hotelName" maxlength="200" /></label>
        <div class="sqlyog-import-field">
          <input ref="sqlYogFileInput" class="sqlyog-file-input" type="file" accept=".xml,.ini,.txt,.sycs" @change="handleSqlYogFileChange" />
          <button class="sqlyog-import-button" type="button" @click="openSqlYogFilePicker">
            <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M12 16V4m0 0L7.5 8.5M12 4l4.5 4.5M5 14v4a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-4" /></svg>
            <span>导入 SQLyog 连接文件</span>
          </button>
        </div>
        <label class="full-row">地址服务配置<input v-model.trim="createForm.addressConfig" maxlength="500" /></label>
        <label>数据库用户<input v-model.trim="createForm.databaseUsername" maxlength="128" /></label>
        <label>数据库地址<input v-model.trim="createForm.databaseHost" maxlength="255" /></label>
        <label>数据库密码<input v-model="createForm.databasePassword" maxlength="255" /></label>
        <label>数据库端口<input v-model.number="createForm.databasePort" type="number" min="1" max="65535" /></label>
        <label class="full-row">SSH 配置
          <select v-model="createForm.useSsh">
            <option :value="false">不使用 SSH</option>
            <option :value="true">使用 SSH</option>
          </select>
        </label>
        <template v-if="createForm.useSsh">
          <label>SSH 用户<input v-model.trim="createForm.sshUsername" maxlength="128" /></label>
          <label>SSH 地址<input v-model.trim="createForm.sshHost" maxlength="255" /></label>
          <label>SSH 密码<input v-model="createForm.sshPassword" maxlength="255" /></label>
          <label>SSH 端口<input v-model.number="createForm.sshPort" type="number" min="1" max="65535" /></label>
        </template>
      </div>
      <p v-if="createMessage" class="group-hotel-message">{{ createMessage }}</p>
      <div class="group-hotel-dialog-actions">
        <button type="button" :disabled="saving" @click="closeCreateDialog">取消</button>
        <button class="primary-button" type="submit" :disabled="saving">{{ saving ? '保存中...' : '保存' }}</button>
      </div>
    </form>
  </div>
</template>

<style scoped>
.group-hotel-panel { width: 100%; min-width: 0; padding: 24px; background: #fff; box-sizing: border-box; }
.group-hotel-header { display: flex; align-items: end; justify-content: space-between; gap: 20px; margin-bottom: 20px; }
.group-hotel-header h3 { margin: 4px 0 0; font-size: 24px; }
.group-hotel-query { display: flex; gap: 10px; }
.group-hotel-query select,
.group-hotel-query input { min-height: 40px; padding: 0 12px; border: 1px solid #ccd4df; border-radius: 6px; }
.group-hotel-query select { width: 130px; background: #fff; }
.group-hotel-query input { width: 280px; }
.group-hotel-table-wrap { width: 100%; overflow-x: auto; border: 1px solid #e1e6ed; border-radius: 8px; }
.group-hotel-table { width: 100%; min-width: 1800px; table-layout: fixed; border-collapse: collapse; font-size: 13px; }
.group-hotel-table th, .group-hotel-table td { box-sizing: border-box; padding: 13px 12px; border-bottom: 1px solid #e8ecf1; text-align: left; white-space: nowrap; }.group-hotel-table th:nth-child(1), .group-hotel-table td:nth-child(1) { width: 140px; }
.group-hotel-table th:nth-child(2), .group-hotel-table td:nth-child(2) { width: 130px; }
.group-hotel-table th:nth-child(3), .group-hotel-table td:nth-child(3) { width: 70px; }
.group-hotel-table th:nth-child(4), .group-hotel-table td:nth-child(4) { width: 220px; }
.group-hotel-table th:nth-child(5), .group-hotel-table td:nth-child(5) { width: 125px; }
.group-hotel-table th:nth-child(6), .group-hotel-table td:nth-child(6) { width: 145px; }
.group-hotel-table th:nth-child(7), .group-hotel-table td:nth-child(7) { width: 145px; }
.group-hotel-table th:nth-child(8), .group-hotel-table td:nth-child(8) { width: 100px; }
.group-hotel-table th:nth-child(9), .group-hotel-table td:nth-child(9) { width: 110px; }
.group-hotel-table th:nth-child(10), .group-hotel-table td:nth-child(10) { width: 160px; }
.group-hotel-table th:nth-child(11), .group-hotel-table td:nth-child(11) { width: 150px; }
.group-hotel-table th:nth-child(12), .group-hotel-table td:nth-child(12) { width: 100px; }
.group-hotel-table th:nth-child(13), .group-hotel-table td:nth-child(13) { width: 145px; }
.group-hotel-table td { overflow: hidden; text-overflow: ellipsis; }
.group-hotel-table th { color: #506077; background: #f6f8fa; font-weight: 700; }
.group-hotel-table .hotel-code-cell { font-weight: 700; }
.group-hotel-table tbody tr:hover { background: #f8fbff; }
.table-actions { display: flex; gap: 8px; }
.table-delete-button { min-height: 30px; padding: 0 13px; border: 1px solid #e2a09a; border-radius: 5px; color: #b42318; background: #fff7f6; font-weight: 700; cursor: pointer; }
.table-delete-button:hover:not(:disabled) { border-color: #c43228; background: #ffebe9; }
.table-delete-button:disabled { cursor: wait; opacity: 0.65; }
.table-edit-button { min-height: 30px; padding: 0 13px; border: 1px solid #8eabd8; border-radius: 5px; color: #2456a6; background: #f5f8ff; font-weight: 700; cursor: pointer; }
.table-edit-button:hover { border-color: #2f5bb7; background: #e8f0ff; }

.group-hotel-empty { padding: 42px !important; color: #718096; text-align: center !important; }
.group-hotel-message { color: #b42318; }
.group-hotel-add-button { min-height: 42px; padding: 0 18px; white-space: nowrap; }
.group-hotel-query-button { width: 58px; flex: 0 0 58px; }
.group-hotel-dialog-backdrop { position: fixed; z-index: 1000; inset: 0; display: grid; place-items: center; padding: 24px; background: rgba(15, 23, 42, 0.45); }
.group-hotel-dialog { width: min(820px, 100%); max-height: calc(100vh - 48px); overflow: auto; padding: 24px; border-radius: 10px; background: #fff; box-shadow: 0 24px 60px rgba(15, 23, 42, 0.25); }
.group-hotel-dialog-header { display: flex; align-items: center; justify-content: space-between; margin-bottom: 20px; }
.group-hotel-dialog-header h3 { margin: 0; }
.group-hotel-dialog-header button { border: 0; color: #526076; background: transparent; font-size: 26px; cursor: pointer; }
.group-hotel-form-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 16px; }
.group-hotel-form-grid label { display: grid; gap: 7px; color: #334155; font-size: 13px; font-weight: 700; }
.group-hotel-form-grid input, .group-hotel-form-grid select { min-height: 40px; padding: 0 11px; border: 1px solid #ccd4df; border-radius: 6px; }
.group-hotel-form-grid .full-row { grid-column: 1 / -1; }
.hotel-code-input { text-transform: uppercase; }
.sqlyog-import-field { display: flex; align-items: end; justify-content: center; }
.sqlyog-file-input { display: none; }
.sqlyog-import-button { display: inline-flex; width: auto; min-width: 245px; min-height: 42px; padding: 0 20px; align-items: center; justify-content: center; gap: 9px; border: 1px solid #9cb3d9; border-radius: 6px; color: #2456a6; background: linear-gradient(180deg, #f9fbff, #eef4ff); font-weight: 700; cursor: pointer; transition: border-color 0.15s, background 0.15s, transform 0.15s; }
.sqlyog-import-button svg { width: 18px; height: 18px; fill: none; stroke: currentColor; stroke-linecap: round; stroke-linejoin: round; stroke-width: 1.8; }
.sqlyog-import-button:hover { border-color: #2f5bb7; background: #e8f0ff; transform: translateY(-1px); }
.group-hotel-dialog-actions { display: flex; justify-content: flex-end; gap: 10px; margin-top: 22px; }
.group-hotel-dialog-actions button { min-height: 40px; padding: 0 18px; }
@media (max-width: 760px) { .group-hotel-header { align-items: stretch; flex-direction: column; } .group-hotel-query { flex-wrap: wrap; } .group-hotel-query select, .group-hotel-query input { width: 100%; } .group-hotel-form-grid { grid-template-columns: 1fr; } .group-hotel-form-grid .full-row { grid-column: auto; } }
</style>
