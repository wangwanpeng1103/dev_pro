<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import TmhMockCompanyPanel from './TmhMockCompanyPanel.vue'
import {
  createPermanentUser,
  createTemporaryUser,
  createMihotelSystemParam,
  deleteUser,
  extendTemporaryUserTime,
  clearMihotelCacheTarget,
  listProjects,
  listUsers,
  listMihotelCacheTargets,
  listMihotelSystemParamEnvironments,
  login,
  lookupCloudCheckinGroupAddress,
  lookupCloudCheckinStoreConfig,
  queryMihotelSystemParams,
  updateUser,
  updateMihotelSystemParam,
  updateUserPassword,
  type MihotelCacheClearResult,
  type MihotelCacheEnvironment,
  type MihotelCacheTarget,
  type MihotelSystemParamEnvironment,
  type MihotelSystemParamEnvironmentCode,
  type MihotelSystemParamRecord,
  type ProjectModule,
  type UserType,
  type UserAccount
} from './api'

type ViewName = 'login' | 'projects' | 'admin' | 'project'
type AdminFeature = 'user-list'
type CloudCheckinFeature = 'store-rop-registration' | 'address-validation'
type MihotelFeature = 'system-params' | 'clear-cache'
type IhotelFeature = 'tmh-mock-companies'
type CacheClearStatus = 'idle' | 'running' | 'success' | 'failed'
type UserDialogMode = 'create' | 'edit'
type SystemParamDialogMode = 'create' | 'edit'
type PmsSdkInputMode = 'upload' | 'manual'
type ConsoleSession = {
  currentView: ViewName
  currentUser: UserAccount
  projects: ProjectModule[]
  selectedProject: ProjectModule | null
  activeAdminFeature: AdminFeature
  activeCloudCheckinFeature: CloudCheckinFeature
  activeMihotelFeature: MihotelFeature
}

const consoleSessionStorageKey = 'devpro-console-session'

const currentView = ref<ViewName>('login')
const username = ref('')
const password = ref('')
const errorMessage = ref('')
const successMessage = ref('')
const loading = ref(false)
const currentUser = ref<UserAccount | null>(null)
const projects = ref<ProjectModule[]>([])
const users = ref<UserAccount[]>([])
const selectedProject = ref<ProjectModule | null>(null)
const activeAdminFeature = ref<AdminFeature>('user-list')
const activeCloudCheckinFeature = ref<CloudCheckinFeature>('store-rop-registration')
const activeMihotelFeature = ref<MihotelFeature>('system-params')
const activeIhotelFeature = ref<IhotelFeature>('tmh-mock-companies')
const userPage = ref(1)
const userPageSize = 10
const userTotal = ref(0)
const userTotalPages = ref(1)
const userDialogMode = ref<UserDialogMode>('create')
const userDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
const extendTimeDialogVisible = ref(false)
const selectedUser = ref<UserAccount | null>(null)
const formSaving = ref(false)
const userForm = ref({
  username: '',
  displayName: '',
  userType: 'PERMANENT' as Exclude<UserType, 'ADMIN'>,
  enabled: true,
  validHours: 24,
  projectCodes: [] as string[]
})
const passwordForm = ref({
  password: ''
})
const extendTimeForm = ref({
  extendHours: 24
})
const ropGroupCode = ref('')
const ropGroupAddress = ref('')
const ropGroupLookupLoading = ref(false)
const ropGroupLookupMessage = ref('')
const ropAuthManualDialogVisible = ref(false)
const ropAuthManualContent = ref('')
const ropAuthFileName = ref('')
const ropAuthFileContent = ref('')
const ropRegistrationEncryptedText = ref('')
const ropCopyMessage = ref('')
const addressValidationStoreCode = ref('')
const addressValidationLoading = ref(false)
const addressValidationMessage = ref('')
const addressValidationFound = ref(false)
const addressValidationGroupAddress = ref('')
const addressValidationGroupCode = ref('')
const addressValidationUsername = ref('')
const addressValidationPassword = ref('')
const addressValidationAppKey = ref('')
const addressValidationAppSecret = ref('')
const addressValidationGeneratedText = ref('')
const addressValidationCopyMessage = ref('')
const mihotelCacheTargets = ref<MihotelCacheTarget[]>([])
const mihotelCacheLoading = ref(false)
const mihotelCacheRunning = ref(false)
const mihotelCacheMessage = ref('')
const mihotelCacheStatuses = ref<Record<string, CacheClearStatus>>({})
const mihotelCacheResults = ref<Record<string, MihotelCacheClearResult>>({})
const mihotelSystemParamEnvironments = ref<MihotelSystemParamEnvironment[]>([])
const mihotelSystemParamEnvironment = ref<MihotelSystemParamEnvironmentCode>('TRUNK')
const mihotelSystemParamGroupCode = ref('')
const mihotelSystemParamLoading = ref(false)
const mihotelSystemParamMessage = ref('')
const mihotelSystemParamRecords = ref<MihotelSystemParamRecord[]>([])
const mihotelSystemParamLastQuery = ref('')
const systemParamDialogVisible = ref(false)
const systemParamDialogMode = ref<SystemParamDialogMode>('create')
const systemParamSaving = ref(false)
const systemParamForm = ref({
  id: null as number | null,
  hotelGroupCode: '',
  catalog: '',
  item: '',
  setValue: '',
  defValue: '',
  descript: '',
  descriptEn: '',
  ctrlStr: ''
})
const pmsSdkDialogVisible = ref(false)
const pmsSdkInputMode = ref<PmsSdkInputMode>('upload')
const pmsSdkAuthFileName = ref('')
const pmsSdkAuthFileContent = ref('')
const pmsSdkManualContent = ref('')
const pmsSdkGeneratedText = ref('')
const pmsSdkMessage = ref('')
const pmsSdkCopyMessage = ref('')
const pmsSdkGenerating = ref(false)

const isAdmin = computed(() => currentUser.value?.userType === 'ADMIN')
const visibleProjects = computed(() => projects.value.filter((project) => project.enabled))
const assignableProjects = computed(() => visibleProjects.value.filter((project) => project.code !== 'user-admin'))
const totalUserPages = computed(() => Math.max(1, userTotalPages.value))
const pagedUsers = computed(() => users.value)
const isCloudCheckinProject = computed(() => selectedProject.value?.code === 'cloud-checkin')
const isMihotelProject = computed(() => selectedProject.value?.code === 'mihotel')
const isIhotelProject = computed(() => selectedProject.value?.code === 'ihotel')
const mihotelTrunkCacheTargets = computed(() =>
  mihotelCacheTargets.value.filter((target) => target.environment === 'TRUNK')
)
const mihotelLocalCacheTargets = computed(() =>
  mihotelCacheTargets.value.filter((target) => target.environment === 'LOCAL')
)
const visibleMihotelSystemParamEnvironments = computed(() => {
  if (isAdmin.value) {
    return mihotelSystemParamEnvironments.value
  }
  return mihotelSystemParamEnvironments.value.filter((environment) => environment.code !== 'LOCAL')
})
const canUseMihotelLocalTools = computed(() => isAdmin.value)
const pageTitle = computed(() => {
  if (currentView.value === 'projects') {
    return '项目模块'
  }
  return selectedProject.value?.name ?? '绿云运维控制台'
})

restoreConsoleSession()

watch(
  [
    currentView,
    currentUser,
    projects,
    selectedProject,
    activeAdminFeature,
    activeCloudCheckinFeature,
    activeMihotelFeature
  ],
  saveConsoleSession,
  { deep: true }
)

watch(isAdmin, () => {
  ensureAllowedMihotelSystemParamEnvironment()
})

watch(mihotelSystemParamEnvironment, () => {
  ensureAllowedMihotelSystemParamEnvironment()
})

async function handleLogin() {
  errorMessage.value = ''
  loading.value = true
  try {
    const result = await login(username.value, password.value)
    currentUser.value = result.user
    projects.value = result.projects
    currentView.value = 'projects'
    saveConsoleSession()
    if (isAdmin.value) {
      await refreshAdminData()
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '登录失败'
  } finally {
    loading.value = false
  }
}

async function refreshAdminData() {
  const [userPageResult, projectResult] = await Promise.all([
    listUsers(userPage.value, userPageSize),
    listProjects(currentUser.value?.username)
  ])
  users.value = userPageResult.records
  userTotal.value = userPageResult.total
  userTotalPages.value = Math.max(1, userPageResult.pages)
  projects.value = projectResult
  if (userPage.value > totalUserPages.value) {
    userPage.value = totalUserPages.value
    await refreshAdminData()
  }
}

async function openProject(project: ProjectModule) {
  selectedProject.value = project
  if (project.code === 'user-admin') {
    activeAdminFeature.value = 'user-list'
    userPage.value = 1
    await refreshAdminData()
  } else if (project.code === 'cloud-checkin') {
    activeCloudCheckinFeature.value = 'store-rop-registration'
    resetRopRegistrationForm()
    resetAddressValidationForm()
  } else if (project.code === 'mihotel') {
    activeMihotelFeature.value = 'system-params'
    void loadMihotelSystemParamEnvironments()
    void loadMihotelCacheTargets()
  }
  currentView.value = project.code === 'user-admin' ? 'admin' : 'project'
}

function logout() {
  currentUser.value = null
  selectedProject.value = null
  currentView.value = 'login'
  clearConsoleSession()
}

function backToProjects() {
  resetRopRegistrationForm()
  resetAddressValidationForm()
  if (selectedProject.value?.code === 'mihotel') {
    resetMihotelWorkbenchState()
  }
  selectedProject.value = null
  currentView.value = 'projects'
}

function closeErrorDialog() {
  errorMessage.value = ''
}

function closeSuccessDialog() {
  successMessage.value = ''
}

function restoreConsoleSession() {
  try {
    const sessionText = localStorage.getItem(consoleSessionStorageKey)
    if (!sessionText) {
      return
    }
    const session = JSON.parse(sessionText) as Partial<ConsoleSession>
    if (!session.currentUser || !Array.isArray(session.projects)) {
      clearConsoleSession()
      return
    }
    currentUser.value = session.currentUser
    projects.value = session.projects
    selectedProject.value = session.selectedProject ?? null
    activeAdminFeature.value = session.activeAdminFeature === 'user-list' ? session.activeAdminFeature : 'user-list'
    activeCloudCheckinFeature.value = isCloudCheckinFeature(session.activeCloudCheckinFeature)
      ? session.activeCloudCheckinFeature
      : 'store-rop-registration'
    activeMihotelFeature.value = isMihotelFeature(session.activeMihotelFeature)
      ? session.activeMihotelFeature
      : 'system-params'
    currentView.value = normalizeRestoredView(session.currentView, selectedProject.value)
    if (currentView.value === 'admin') {
      void refreshAdminData()
    }
    if (selectedProject.value?.code === 'mihotel') {
      void loadMihotelSystemParamEnvironments()
      void loadMihotelCacheTargets()
    }
  } catch {
    clearConsoleSession()
  }
}

function saveConsoleSession() {
  if (!currentUser.value) {
    return
  }
  const session: ConsoleSession = {
    currentView: currentView.value,
    currentUser: currentUser.value,
    projects: projects.value,
    selectedProject: selectedProject.value,
    activeAdminFeature: activeAdminFeature.value,
    activeCloudCheckinFeature: activeCloudCheckinFeature.value,
    activeMihotelFeature: activeMihotelFeature.value
  }
  localStorage.setItem(consoleSessionStorageKey, JSON.stringify(session))
}

function clearConsoleSession() {
  localStorage.removeItem(consoleSessionStorageKey)
}

function normalizeRestoredView(viewName: ViewName | undefined, restoredProject: ProjectModule | null) {
  if (viewName === 'admin' && restoredProject?.code === 'user-admin') {
    return 'admin'
  }
  if (viewName === 'project' && restoredProject) {
    return 'project'
  }
  return 'projects'
}

function isCloudCheckinFeature(feature?: string): feature is CloudCheckinFeature {
  return feature === 'store-rop-registration' || feature === 'address-validation'
}

function isMihotelFeature(feature?: string): feature is MihotelFeature {
  return feature === 'system-params' || feature === 'clear-cache'
}

function resetRopRegistrationForm() {
  ropGroupCode.value = ''
  ropGroupAddress.value = ''
  ropGroupLookupMessage.value = ''
  ropAuthManualDialogVisible.value = false
  ropAuthManualContent.value = ''
  ropAuthFileName.value = ''
  ropAuthFileContent.value = ''
  ropRegistrationEncryptedText.value = ''
  ropCopyMessage.value = ''
}

function resetAddressValidationForm() {
  addressValidationStoreCode.value = ''
  addressValidationLoading.value = false
  addressValidationMessage.value = ''
  addressValidationFound.value = false
  addressValidationGroupAddress.value = ''
  addressValidationGroupCode.value = ''
  addressValidationUsername.value = ''
  addressValidationPassword.value = ''
  addressValidationAppKey.value = ''
  addressValidationAppSecret.value = ''
  addressValidationGeneratedText.value = ''
  addressValidationCopyMessage.value = ''
}

function resetMihotelWorkbenchState() {
  activeMihotelFeature.value = 'system-params'
  mihotelSystemParamEnvironment.value = 'TRUNK'
  mihotelSystemParamGroupCode.value = ''
  mihotelSystemParamLoading.value = false
  mihotelSystemParamMessage.value = ''
  mihotelSystemParamRecords.value = []
  mihotelSystemParamLastQuery.value = ''
  systemParamDialogVisible.value = false
  systemParamSaving.value = false
  systemParamDialogMode.value = 'create'
  systemParamForm.value = {
    id: null,
    hotelGroupCode: '',
    catalog: '',
    item: '',
    setValue: '',
    defValue: '',
    descript: '',
    descriptEn: '',
    ctrlStr: ''
  }
  resetPmsSdkDialogState()
  mihotelCacheTargets.value = []
  mihotelCacheLoading.value = false
  mihotelCacheRunning.value = false
  mihotelCacheMessage.value = ''
  mihotelCacheStatuses.value = {}
  mihotelCacheResults.value = {}
}

function resetPmsSdkDialogState() {
  pmsSdkDialogVisible.value = false
  pmsSdkInputMode.value = 'upload'
  pmsSdkAuthFileName.value = ''
  pmsSdkAuthFileContent.value = ''
  pmsSdkManualContent.value = ''
  pmsSdkGeneratedText.value = ''
  pmsSdkMessage.value = ''
  pmsSdkCopyMessage.value = ''
  pmsSdkGenerating.value = false
}

function ensureAllowedMihotelSystemParamEnvironment() {
  if (!isAdmin.value && mihotelSystemParamEnvironment.value === 'LOCAL') {
    mihotelSystemParamEnvironment.value = 'TRUNK'
  }
}

function currentOperatorUsername() {
  return currentUser.value?.username ?? ''
}

function canEditMihotelSystemParam(record: MihotelSystemParamRecord) {
  if (isAdmin.value) {
    return true
  }
  return record.catalog === 'PMS' && record.item === 'INTERFACE_PARAMS'
}

async function loadMihotelSystemParamEnvironments() {
  try {
    const environments = await listMihotelSystemParamEnvironments(currentOperatorUsername())
    mihotelSystemParamEnvironments.value = [...environments].sort((firstEnvironment, secondEnvironment) => {
      return firstEnvironment.sortOrder - secondEnvironment.sortOrder
    })
    ensureAllowedMihotelSystemParamEnvironment()
    if (!mihotelSystemParamEnvironments.value.some((environment) => {
      return environment.code === mihotelSystemParamEnvironment.value
    })) {
      mihotelSystemParamEnvironment.value = 'TRUNK'
    }
  } catch (error) {
    mihotelSystemParamEnvironments.value = [
      { code: 'TRUNK', name: '主干环境', sortOrder: 0 },
      { code: 'LOCAL', name: '本地环境', sortOrder: 1 }
    ]
    ensureAllowedMihotelSystemParamEnvironment()
    mihotelSystemParamMessage.value = error instanceof Error
      ? `系统参数环境加载失败：${error.message}`
      : '系统参数环境加载失败，请稍后重试'
  }
}

async function submitMihotelSystemParamQuery() {
  const hotelGroupCode = mihotelSystemParamGroupCode.value.trim()
  ensureAllowedMihotelSystemParamEnvironment()
  if (!hotelGroupCode) {
    errorMessage.value = '请先输入集团代码'
    return
  }
  mihotelSystemParamGroupCode.value = hotelGroupCode
  mihotelSystemParamLoading.value = true
  mihotelSystemParamMessage.value = ''
  mihotelSystemParamRecords.value = []
  try {
    const result = await queryMihotelSystemParams(
      mihotelSystemParamEnvironment.value,
      hotelGroupCode,
      currentOperatorUsername()
    )
    mihotelSystemParamRecords.value = result.records
    mihotelSystemParamLastQuery.value = `${result.environmentName} · ${result.hotelGroupCode}`
    mihotelSystemParamMessage.value = result.records.length > 0
      ? `已查询到 ${result.records.length} 条系统参数`
      : '未查询到系统参数'
  } catch (error) {
    mihotelSystemParamMessage.value = error instanceof Error ? error.message : '系统参数查询失败'
  } finally {
    mihotelSystemParamLoading.value = false
  }
}

function systemParamText(value: string | null | undefined) {
  return value && value.trim() ? value : '-'
}

function openSystemParamCreatePlaceholder() {
  if (!isAdmin.value) {
    errorMessage.value = '只有 admin 可以新增系统参数'
    return
  }
  systemParamDialogMode.value = 'create'
  systemParamForm.value = {
    id: null,
    hotelGroupCode: mihotelSystemParamGroupCode.value.trim(),
    catalog: '',
    item: '',
    setValue: '',
    defValue: '',
    descript: '',
    descriptEn: '',
    ctrlStr: ''
  }
  systemParamDialogVisible.value = true
}

function openSystemParamEditDialog(record: MihotelSystemParamRecord) {
  if (!canEditMihotelSystemParam(record)) {
    errorMessage.value = '当前用户只能修改 PMS / INTERFACE_PARAMS 配置'
    return
  }
  if (!record.id) {
    errorMessage.value = '该系统参数缺少主键，无法修改'
    return
  }
  systemParamDialogMode.value = 'edit'
  systemParamForm.value = {
    id: record.id,
    hotelGroupCode: record.hotelGroupCode ?? '',
    catalog: record.catalog ?? '',
    item: record.item ?? '',
    setValue: record.setValue ?? '',
    defValue: record.defValue ?? '',
    descript: record.descript ?? '',
    descriptEn: record.descriptEn ?? '',
    ctrlStr: record.ctrlStr ?? ''
  }
  systemParamDialogVisible.value = true
}

function closeSystemParamDialog() {
  if (systemParamSaving.value) {
    return
  }
  systemParamDialogVisible.value = false
}

async function submitSystemParamForm() {
  if (systemParamDialogMode.value === 'create') {
    if (!systemParamForm.value.hotelGroupCode.trim()) {
      errorMessage.value = '请填写集团代码'
      return
    }
    if (!systemParamForm.value.catalog.trim()) {
      errorMessage.value = '请填写参数分类'
      return
    }
    if (!systemParamForm.value.item.trim()) {
      errorMessage.value = '请填写参数项'
      return
    }
  }
  if (systemParamDialogMode.value === 'edit' && !systemParamForm.value.id) {
    errorMessage.value = '系统参数主键不能为空'
    return
  }
  systemParamSaving.value = true
  try {
    if (systemParamDialogMode.value === 'create') {
      await createMihotelSystemParam({
        environment: mihotelSystemParamEnvironment.value,
        operatorUsername: currentOperatorUsername(),
        hotelGroupCode: systemParamForm.value.hotelGroupCode.trim(),
        catalog: systemParamForm.value.catalog.trim(),
        item: systemParamForm.value.item.trim(),
        setValue: systemParamForm.value.setValue,
        defValue: systemParamForm.value.defValue,
        descript: systemParamForm.value.descript,
        descriptEn: systemParamForm.value.descriptEn,
        ctrlStr: ''
      })
      mihotelSystemParamGroupCode.value = systemParamForm.value.hotelGroupCode.trim()
      successMessage.value = '系统参数新增成功'
    } else {
      await updateMihotelSystemParam({
        environment: mihotelSystemParamEnvironment.value,
        operatorUsername: currentOperatorUsername(),
        id: systemParamForm.value.id,
        catalog: systemParamForm.value.catalog,
        item: systemParamForm.value.item,
        setValue: systemParamForm.value.setValue
      })
      successMessage.value = '系统参数修改成功'
    }
    systemParamDialogVisible.value = false
    if (mihotelSystemParamGroupCode.value.trim()) {
      await submitMihotelSystemParamQuery()
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '系统参数保存失败'
  } finally {
    systemParamSaving.value = false
  }
}

function openPmsSdkParamDialog() {
  pmsSdkDialogVisible.value = true
  pmsSdkMessage.value = ''
  pmsSdkCopyMessage.value = ''
}

function closePmsSdkParamDialog() {
  if (pmsSdkGenerating.value) {
    return
  }
  pmsSdkDialogVisible.value = false
}

function switchPmsSdkInputMode(mode: PmsSdkInputMode) {
  pmsSdkInputMode.value = mode
  pmsSdkMessage.value = ''
  pmsSdkCopyMessage.value = ''
}

async function handlePmsSdkAuthFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  try {
    pmsSdkAuthFileName.value = file.name
    pmsSdkAuthFileContent.value = await file.text()
    pmsSdkManualContent.value = ''
    pmsSdkGeneratedText.value = ''
    pmsSdkCopyMessage.value = ''
    pmsSdkMessage.value = '已读取认证文件'
  } catch {
    errorMessage.value = '认证文件读取失败，请重新上传或改为手动输入'
  } finally {
    input.value = ''
  }
}

function pmsSdkAuthContent() {
  return pmsSdkInputMode.value === 'manual'
    ? pmsSdkManualContent.value.trim()
    : pmsSdkAuthFileContent.value.trim()
}

function pmsSdkAuthSourceLabel() {
  if (pmsSdkInputMode.value === 'manual') {
    return pmsSdkManualContent.value.trim() ? '已手动输入认证内容' : '暂未提供'
  }
  return pmsSdkAuthFileName.value || '暂未提供'
}

async function generatePmsSdkParam() {
  const authContent = pmsSdkAuthContent()
  if (!authContent) {
    errorMessage.value = '请上传或手动输入 ROP 认证信息'
    return
  }
  const requiredFields = ['hotelGroupCode', 'hotelGroupDescript', 'appKey', 'appSecret', 'username', 'password']
  const missingFields = requiredFields.filter((fieldName) => !extractRopField(authContent, fieldName))
  if (missingFields.length > 0) {
    errorMessage.value = `ROP 认证信息缺少字段：${missingFields.join('、')}`
    return
  }
  const hotelGroupCode = extractRopField(authContent, 'hotelGroupCode')
  pmsSdkGenerating.value = true
  pmsSdkMessage.value = ''
  pmsSdkCopyMessage.value = ''
  try {
    const result = await lookupCloudCheckinGroupAddress(hotelGroupCode)
    if (!result.found || !result.groupAddress) {
      pmsSdkGeneratedText.value = ''
      pmsSdkMessage.value = `${hotelGroupCode} 未查询到集团地址，请先配置地址服务`
      return
    }
    const pmsGroupAddress = normalizePmsGroupAddress(result.groupAddress)
    pmsSdkGeneratedText.value = [
      pmsGroupAddress,
      extractRopField(authContent, 'appKey'),
      extractRopField(authContent, 'appSecret'),
      extractRopField(authContent, 'username'),
      extractRopField(authContent, 'password')
    ].join('|')
    pmsSdkMessage.value = result.groupName
      ? `已生成 ${result.groupName} 的 PMS SDK 参数`
      : '已生成 PMS SDK 参数'
  } catch (error) {
    pmsSdkGeneratedText.value = ''
    errorMessage.value = error instanceof Error ? error.message : 'PMS SDK 参数生成失败'
  } finally {
    pmsSdkGenerating.value = false
  }
}

function normalizePmsGroupAddress(groupAddress: string) {
  const addressParts = groupAddress.split(';')
  const groupUrl = (addressParts.length > 1 ? addressParts[1] : addressParts[0]).trim()
  const lastSlashIndex = groupUrl.lastIndexOf('/')
  const protocolEndIndex = groupUrl.indexOf('://')
  const minPathSlashIndex = protocolEndIndex >= 0 ? protocolEndIndex + 3 : 0
  if (lastSlashIndex > minPathSlashIndex) {
    return groupUrl.substring(0, lastSlashIndex)
  }
  return groupUrl
}

async function copyTextToClipboard(text: string) {
  if (!text) {
    return false
  }
  if (window.isSecureContext && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text)
      return true
    } catch {
      // 部分浏览器或远程访问环境会拒绝 Clipboard API，继续走兼容复制。
    }
  }
  return fallbackCopyText(text)
}

function fallbackCopyText(text: string) {
  const activeElement = document.activeElement
  const selection = document.getSelection()
  const selectedRange = selection && selection.rangeCount > 0 ? selection.getRangeAt(0) : null
  const textarea = document.createElement('textarea')
  textarea.value = text
  textarea.setAttribute('readonly', 'readonly')
  textarea.style.position = 'fixed'
  textarea.style.top = '0'
  textarea.style.left = '0'
  textarea.style.width = '1px'
  textarea.style.height = '1px'
  textarea.style.opacity = '0'
  textarea.style.pointerEvents = 'none'
  document.body.appendChild(textarea)
  textarea.focus({ preventScroll: true })
  textarea.select()
  textarea.setSelectionRange(0, textarea.value.length)
  try {
    return document.execCommand('copy')
  } finally {
    document.body.removeChild(textarea)
    if (selectedRange && selection) {
      selection.removeAllRanges()
      selection.addRange(selectedRange)
    }
    if (activeElement instanceof HTMLElement) {
      activeElement.focus({ preventScroll: true })
    }
  }
}
async function copyPmsSdkGeneratedText() {
  if (!pmsSdkGeneratedText.value) {
    return
  }
  if (await copyTextToClipboard(pmsSdkGeneratedText.value)) {
    pmsSdkCopyMessage.value = '已复制'
  } else {
    errorMessage.value = '复制失败，请手动选择 PMS SDK 参数复制'
  }
}

async function loadMihotelCacheTargets() {
  mihotelCacheLoading.value = true
  mihotelCacheMessage.value = ''
  try {
    const targets = await listMihotelCacheTargets()
    mihotelCacheTargets.value = [...targets].sort((firstTarget, secondTarget) => {
      return firstTarget.sortOrder - secondTarget.sortOrder
    })
    resetMihotelCacheRunState()
    if (targets.length === 0) {
      mihotelCacheMessage.value = '缓存清理目标尚未配置，请先在后端环境变量中配置 mihotel 清理目标。'
    }
  } catch (error) {
    mihotelCacheTargets.value = []
    resetMihotelCacheRunState()
    mihotelCacheMessage.value = error instanceof Error
      ? `缓存清理目标加载失败：${error.message}`
      : '缓存清理目标加载失败，请稍后刷新节点'
  } finally {
    mihotelCacheLoading.value = false
  }
}

function resetMihotelCacheRunState() {
  mihotelCacheStatuses.value = Object.fromEntries(
    mihotelCacheTargets.value.map((target) => [target.code, 'idle' as CacheClearStatus])
  )
  mihotelCacheResults.value = {}
}

function mihotelCacheStatus(targetCode: string) {
  return mihotelCacheStatuses.value[targetCode] ?? 'idle'
}

function mihotelCacheResult(targetCode: string) {
  return mihotelCacheResults.value[targetCode]
}

function cacheStatusLabel(status: CacheClearStatus) {
  const statusLabels: Record<CacheClearStatus, string> = {
    idle: '等待',
    running: '执行中',
    success: '成功',
    failed: '失败'
  }
  return statusLabels[status]
}

function formatDuration(durationMillis?: number) {
  if (durationMillis === undefined) {
    return ''
  }
  if (durationMillis < 1000) {
    return `${durationMillis} ms`
  }
  return `${(durationMillis / 1000).toFixed(1)} s`
}

async function clearMihotelCache(environment: MihotelCacheEnvironment) {
  if (mihotelCacheRunning.value) {
    return
  }
  if (environment === 'LOCAL' && !canUseMihotelLocalTools.value) {
    errorMessage.value = '只有 admin 可以操作本地环境'
    return
  }
  const targets = environment === 'TRUNK' ? mihotelTrunkCacheTargets.value : mihotelLocalCacheTargets.value
  if (targets.length === 0) {
    errorMessage.value = environment === 'TRUNK' ? '主干缓存清理目标未配置' : '本地缓存清理目标未配置'
    return
  }
  const targetCodes = new Set(targets.map((target) => target.code))
  mihotelCacheStatuses.value = {
    ...mihotelCacheStatuses.value,
    ...Object.fromEntries(targets.map((target) => [target.code, 'idle' as CacheClearStatus]))
  }
  mihotelCacheResults.value = Object.fromEntries(
    Object.entries(mihotelCacheResults.value).filter(([targetCode]) => !targetCodes.has(targetCode))
  )
  mihotelCacheRunning.value = true
  mihotelCacheMessage.value = environment === 'TRUNK' ? '主干缓存清理中，请等待当前节点成功后继续下一个节点。' : '本地缓存清理中。'
  for (const target of targets) {
    mihotelCacheStatuses.value = {
      ...mihotelCacheStatuses.value,
      [target.code]: 'running'
    }
    try {
      const result = await clearMihotelCacheTarget(target.code, currentOperatorUsername())
      mihotelCacheResults.value = {
        ...mihotelCacheResults.value,
        [target.code]: result
      }
      mihotelCacheStatuses.value = {
        ...mihotelCacheStatuses.value,
        [target.code]: result.success ? 'success' : 'failed'
      }
      if (!result.success) {
        mihotelCacheMessage.value = `${target.name} 清理失败，后续节点已停止执行。`
        mihotelCacheRunning.value = false
        return
      }
    } catch (error) {
      mihotelCacheStatuses.value = {
        ...mihotelCacheStatuses.value,
        [target.code]: 'failed'
      }
      mihotelCacheMessage.value = error instanceof Error ? error.message : `${target.name} 清理失败`
      mihotelCacheRunning.value = false
      return
    }
  }
  mihotelCacheMessage.value = environment === 'TRUNK' ? '主干环境缓存已全部清理完成。' : '本地环境缓存已清理完成。'
  mihotelCacheRunning.value = false
}

function openRopAuthManualDialog() {
  ropAuthManualDialogVisible.value = true
}

function closeRopAuthManualDialog() {
  ropAuthManualDialogVisible.value = false
}

async function confirmRopAuthManualInput() {
  if (!ropAuthManualContent.value.trim()) {
    errorMessage.value = '请先输入 ROP 认证信息'
    return
  }
  ropAuthFileName.value = ''
  ropAuthFileContent.value = ''
  ropRegistrationEncryptedText.value = ''
  ropCopyMessage.value = ''
  ropAuthManualDialogVisible.value = false
  await applyHotelGroupCodeFromRopAuthContent(ropAuthManualContent.value)
}

async function handleRopAuthFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  if (!file) {
    return
  }
  try {
    ropAuthFileName.value = file.name
    ropAuthFileContent.value = await file.text()
    ropAuthManualContent.value = ''
    ropRegistrationEncryptedText.value = ''
    ropCopyMessage.value = ''
    await applyHotelGroupCodeFromRopAuthContent(ropAuthFileContent.value)
  } catch {
    errorMessage.value = '认证文件读取失败，请重新上传或改为手动输入'
  }
}

function extractRopField(content: string, fieldName: string) {
  const escapedFieldName = fieldName.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
  const fieldPattern = new RegExp(`(?:^|\\r?\\n)\\s*${escapedFieldName}\\s*[:=]\\s*([^\\r\\n]+)`, 'i')
  return content.match(fieldPattern)?.[1]?.trim() ?? ''
}

function extractHotelGroupCode(content: string) {
  return extractRopField(content, 'hotelGroupCode')
}

async function applyHotelGroupCodeFromRopAuthContent(content: string) {
  const hotelGroupCode = extractHotelGroupCode(content)
  if (!hotelGroupCode) {
    ropGroupCode.value = ''
    ropGroupAddress.value = ''
    ropGroupLookupMessage.value = '认证信息中未识别到 hotelGroupCode，请手动填写集团代码'
    return
  }
  ropGroupCode.value = hotelGroupCode
  await lookupRopGroupAddress(hotelGroupCode)
}

async function lookupRopGroupAddress(groupCodeOverride?: string) {
  const groupCode = (groupCodeOverride ?? ropGroupCode.value).trim()
  if (!groupCode) {
    errorMessage.value = '请先输入集团代码'
    return
  }
  ropGroupCode.value = groupCode
  ropGroupLookupLoading.value = true
  ropGroupLookupMessage.value = ''
  try {
    const result = await lookupCloudCheckinGroupAddress(groupCode)
    if (result.found && result.groupAddress) {
      ropGroupAddress.value = result.groupAddress
      ropGroupLookupMessage.value = result.groupName
        ? `已查询到 ${result.groupName} 的集团地址`
        : '已查询到集团地址'
    } else {
      ropGroupAddress.value = ''
      ropGroupLookupMessage.value = '未查询到集团地址，请手动填写'
    }
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '集团地址查询失败'
  } finally {
    ropGroupLookupLoading.value = false
  }
}

function submitRopRegistration() {
  if (!ropGroupCode.value.trim()) {
    errorMessage.value = '请先输入集团代码'
    return
  }
  if (!ropGroupAddress.value.trim()) {
    errorMessage.value = '请先输入集团地址'
    return
  }
  const authContent = ropAuthManualContent.value.trim() || ropAuthFileContent.value.trim()
  if (!authContent) {
    errorMessage.value = '请上传或手动输入 ROP 认证信息'
    return
  }
  const missingFields = ['username', 'password', 'appKey', 'appSecret'].filter(
    (fieldName) => !extractRopField(authContent, fieldName)
  )
  if (missingFields.length > 0) {
    errorMessage.value = `ROP 认证信息缺少字段：${missingFields.join('、')}`
    return
  }
  ropRegistrationEncryptedText.value = generateRopRegistrationText(authContent)
  ropCopyMessage.value = ''
}

function generateRopRegistrationText(authContent: string) {
  return [
    ropGroupAddress.value.trim(),
    ropGroupCode.value.trim(),
    extractRopField(authContent, 'username'),
    extractRopField(authContent, 'password'),
    extractRopField(authContent, 'appKey'),
    extractRopField(authContent, 'appSecret')
  ].join(';')
}

async function copyRopRegistrationEncryptedText() {
  if (!ropRegistrationEncryptedText.value) {
    return
  }
  if (await copyTextToClipboard(ropRegistrationEncryptedText.value)) {
    ropCopyMessage.value = '已复制'
  } else {
    errorMessage.value = '复制失败，请手动选择门店云入住信息复制'
  }
}

async function lookupAddressValidationStoreConfig() {
  const storeCode = addressValidationStoreCode.value.trim()
  if (!storeCode) {
    errorMessage.value = '请先输入门店代码'
    return
  }
  addressValidationStoreCode.value = storeCode
  addressValidationLoading.value = true
  addressValidationMessage.value = ''
  addressValidationFound.value = false
  addressValidationGeneratedText.value = ''
  addressValidationCopyMessage.value = ''
  try {
    const result = await lookupCloudCheckinStoreConfig(storeCode)
    if (!result.found) {
      clearAddressValidationConfigFields()
      addressValidationMessage.value = '该酒店没有云入住配置'
      return
    }
    addressValidationFound.value = true
    addressValidationGroupAddress.value = result.groupAddress ?? ''
    addressValidationGroupCode.value = result.groupCode ?? ''
    addressValidationUsername.value = result.username ?? ''
    addressValidationPassword.value = result.password ?? ''
    addressValidationAppKey.value = result.appKey ?? ''
    addressValidationAppSecret.value = result.appSecret ?? ''
    addressValidationMessage.value = result.configName
      ? `已查询到 ${result.configName} 的云入住配置`
      : '已查询到门店云入住配置'
  } catch (error) {
    clearAddressValidationConfigFields()
    errorMessage.value = error instanceof Error ? error.message : '门店云入住配置查询失败'
  } finally {
    addressValidationLoading.value = false
  }
}

function clearAddressValidationConfigFields() {
  addressValidationGroupAddress.value = ''
  addressValidationGroupCode.value = ''
  addressValidationUsername.value = ''
  addressValidationPassword.value = ''
  addressValidationAppKey.value = ''
  addressValidationAppSecret.value = ''
}

function generateAddressValidationConfig() {
  if (!addressValidationFound.value) {
    errorMessage.value = '请先查询到门店云入住配置'
    return
  }
  const requiredFields = [
    ['集团地址', addressValidationGroupAddress.value],
    ['user', addressValidationUsername.value],
    ['password', addressValidationPassword.value],
    ['appKey', addressValidationAppKey.value],
    ['appSecret', addressValidationAppSecret.value]
  ]
  const missingFieldNames = requiredFields.filter(([, value]) => !value.trim()).map(([fieldName]) => fieldName)
  if (missingFieldNames.length > 0) {
    errorMessage.value = `请先填写：${missingFieldNames.join('、')}`
    return
  }
  if (!addressValidationGroupCode.value.trim()) {
    errorMessage.value = '原配置缺少集团代码，无法生成完整云入住配置'
    return
  }
  addressValidationGeneratedText.value = [
    addressValidationGroupAddress.value.trim(),
    addressValidationGroupCode.value.trim(),
    addressValidationUsername.value.trim(),
    addressValidationPassword.value.trim(),
    addressValidationAppKey.value.trim(),
    addressValidationAppSecret.value.trim()
  ].join(';')
  addressValidationCopyMessage.value = ''
}

async function copyAddressValidationConfig() {
  if (!addressValidationGeneratedText.value) {
    return
  }
  if (await copyTextToClipboard(addressValidationGeneratedText.value)) {
    addressValidationCopyMessage.value = '已复制'
  } else {
    errorMessage.value = '复制失败，请手动选择云入住配置复制'
  }
}

function displayUserType(userType: UserAccount['userType']) {
  const labelMap: Record<UserAccount['userType'], string> = {
    ADMIN: '管理员',
    PERMANENT: '永久用户',
    TEMPORARY: '临时用户'
  }
  return labelMap[userType]
}

function userTypeBadgeClass(userType: UserAccount['userType']) {
  return {
    ADMIN: 'user-type-badge admin-type',
    PERMANENT: 'user-type-badge permanent-type',
    TEMPORARY: 'user-type-badge temporary-type'
  }[userType]
}

function statusBadgeClass(user: UserAccount) {
  if (user.userType === 'ADMIN') {
    return 'status-badge admin-status'
  }
  if (user.userType === 'TEMPORARY') {
    return 'status-badge temporary-status'
  }
  return user.enabled ? 'status-badge enabled-status' : 'status-badge disabled-status'
}

function displayUserStatus(user: UserAccount) {
  if (user.userType === 'TEMPORARY') {
    return '临时'
  }
  return user.enabled ? '启用' : '禁用'
}

function formatDateTime(value: string | null) {
  if (!value) {
    return '-'
  }
  const normalizedValue = value.includes('T') ? value : value.replace(' ', 'T')
  const date = new Date(normalizedValue)
  if (Number.isNaN(date.getTime())) {
    return value.replace('T', ' ')
  }
  const pad = (part: number) => String(part).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + ' ' + [
    pad(date.getHours()),
    pad(date.getMinutes()),
    pad(date.getSeconds())
  ].join(':')
}

function isCurrentUserRow(user: UserAccount) {
  return user.username === currentUser.value?.username
}

function canEditUserInfo(user: UserAccount) {
  return !isCurrentUserRow(user) && user.userType === 'PERMANENT'
}

function canExtendTemporaryUser(user: UserAccount) {
  return !isCurrentUserRow(user) && user.userType === 'TEMPORARY'
}

function isPermanentAccessUser(user: UserAccount) {
  return user.userType === 'ADMIN' || user.userType === 'PERMANENT'
}

function permanentBadgeClass(user: UserAccount) {
  return user.userType === 'ADMIN' ? 'permanent-badge admin-permanent-badge' : 'permanent-badge'
}

function openCreateUserDialog() {
  userDialogMode.value = 'create'
  selectedUser.value = null
  userForm.value = {
    username: '',
    displayName: '',
    userType: 'PERMANENT',
    enabled: true,
    validHours: 24,
    projectCodes: []
  }
  userDialogVisible.value = true
}

function openEditUserDialog(user: UserAccount) {
  userDialogMode.value = 'edit'
  selectedUser.value = user
  userForm.value = {
    username: user.username,
    displayName: user.displayName,
    userType: user.userType === 'TEMPORARY' ? 'TEMPORARY' : 'PERMANENT',
    enabled: user.enabled,
    validHours: 24,
    projectCodes: [...user.projectCodes]
  }
  userDialogVisible.value = true
}

function closeUserDialog() {
  if (formSaving.value) {
    return
  }
  userDialogVisible.value = false
}

function openPasswordDialog(user: UserAccount) {
  selectedUser.value = user
  passwordForm.value = {
    password: ''
  }
  passwordDialogVisible.value = true
}

function closePasswordDialog() {
  if (formSaving.value) {
    return
  }
  passwordDialogVisible.value = false
}

function openDeleteDialog(user: UserAccount) {
  selectedUser.value = user
  deleteDialogVisible.value = true
}

function closeDeleteDialog() {
  if (formSaving.value) {
    return
  }
  deleteDialogVisible.value = false
}

function openExtendTimeDialog(user: UserAccount) {
  selectedUser.value = user
  extendTimeForm.value = {
    extendHours: 24
  }
  extendTimeDialogVisible.value = true
}

function closeExtendTimeDialog() {
  if (formSaving.value) {
    return
  }
  extendTimeDialogVisible.value = false
}

async function submitUserForm() {
  formSaving.value = true
  errorMessage.value = ''
  try {
    if (userDialogMode.value === 'create') {
      const payload = {
        username: userForm.value.username,
        displayName: userForm.value.displayName,
        projectCodes: userForm.value.projectCodes
      }
      if (userForm.value.userType === 'TEMPORARY') {
        if (!Number.isInteger(userForm.value.validHours) || userForm.value.validHours <= 0) {
          throw new Error('临时用户可用时间必须是正整数小时')
        }
        await createTemporaryUser({
          ...payload,
          validHours: userForm.value.validHours
        })
      } else {
        await createPermanentUser({
          ...payload,
          enabled: userForm.value.enabled
        })
      }
      successMessage.value = '用户新增成功'
    } else if (selectedUser.value) {
      await updateUser(selectedUser.value.username, {
        displayName: userForm.value.displayName,
        enabled: userForm.value.enabled,
        projectCodes: userForm.value.projectCodes
      })
      successMessage.value = '用户信息修改成功'
    }
    userDialogVisible.value = false
    await refreshAdminData()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '保存用户失败'
  } finally {
    formSaving.value = false
  }
}

async function confirmDeleteUser() {
  if (!selectedUser.value) {
    return
  }
  formSaving.value = true
  errorMessage.value = ''
  try {
    await deleteUser(selectedUser.value.username)
    deleteDialogVisible.value = false
    successMessage.value = '用户删除成功'
    await refreshAdminData()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '删除用户失败'
  } finally {
    formSaving.value = false
  }
}

async function submitPasswordForm() {
  if (!selectedUser.value || !currentUser.value) {
    return
  }
  formSaving.value = true
  errorMessage.value = ''
  try {
    await updateUserPassword(selectedUser.value.username, currentUser.value.username, {
      newPassword: passwordForm.value.password
    })
    passwordDialogVisible.value = false
    successMessage.value = '密码修改成功'
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '修改密码失败'
  } finally {
    formSaving.value = false
  }
}

async function submitExtendTimeForm() {
  if (!selectedUser.value) {
    return
  }
  if (!Number.isInteger(extendTimeForm.value.extendHours) || extendTimeForm.value.extendHours <= 0) {
    errorMessage.value = '增加时间必须是正整数小时'
    return
  }
  formSaving.value = true
  errorMessage.value = ''
  try {
    await extendTemporaryUserTime(selectedUser.value.username, {
      extendHours: extendTimeForm.value.extendHours
    })
    extendTimeDialogVisible.value = false
    successMessage.value = '临时时间增加成功'
    await refreshAdminData()
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '增加临时时间失败'
  } finally {
    formSaving.value = false
  }
}

async function previousUserPage() {
  if (userPage.value <= 1) {
    return
  }
  userPage.value -= 1
  await refreshAdminData()
}

async function nextUserPage() {
  if (userPage.value >= totalUserPages.value) {
    return
  }
  userPage.value += 1
  await refreshAdminData()
}
</script>

<template>
  <main class="app">
    <section v-if="currentView === 'login'" class="login-layout">
      <aside class="brand-panel">
        <div class="brand-mark">GY</div>
        <div>
          <p class="brand-kicker">GREEN CLOUD OPS</p>
          <h1>绿云运维控制台</h1>
          <div class="brand-copy-spacer" aria-hidden="true"></div>
        </div>
        <div class="quote-card">
          <p>把入口收拢到一处，把权限留在边界，把运维动作变得清楚。</p>
          <strong>绿云运维平台</strong>
        </div>
        <ul class="feature-list">
          <li>用户直接授权到项目模块</li>
          <li>支持永久用户和可配置有效期临时用户</li>
          <li>项目功能栏按需扩展，预留外部链接与 SSO</li>
        </ul>
      </aside>

      <form class="login-card" @submit.prevent="handleLogin">
        <p class="eyebrow">登录系统</p>
        <h2 class="login-title">绿云运维控制台</h2>
        <label>
          用户名
          <input v-model="username" autocomplete="username" />
        </label>
        <label>
          密码
          <input v-model="password" autocomplete="current-password" type="password" />
        </label>
        <button class="primary-button" type="submit" :disabled="loading">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </form>

      <div v-if="errorMessage" class="dialog-backdrop" role="presentation">
        <section class="error-dialog" role="alertdialog" aria-modal="true" aria-labelledby="error-dialog-title">
          <div class="dialog-mark">!</div>
          <div>
            <h3 id="error-dialog-title">登录失败</h3>
            <p>{{ errorMessage }}</p>
          </div>
          <button class="primary-button dialog-button" type="button" @click="closeErrorDialog">确定</button>
        </section>
      </div>
    </section>

    <section v-else class="console-shell">
      <header class="topbar">
        <div v-if="currentView === 'projects'">
          <p class="eyebrow">{{ currentView === 'projects' ? 'OPS CONSOLE' : selectedProject?.code }}</p>
          <h2>{{ pageTitle }}</h2>
        </div>
        <button v-else class="topbar-back" type="button" @click="backToProjects">← 返回项目首页</button>
        <div class="user-box">
          <span>{{ currentUser?.displayName }}</span>
          <button class="ghost-button" @click="logout">退出</button>
        </div>
      </header>

      <div v-if="currentView === 'projects'" class="content-area project-entry">
        <div class="section-title">
          <h3>可运维项目</h3>
        </div>
        <div class="project-grid">
          <button
            v-for="project in visibleProjects"
            :key="project.code"
            class="project-card"
            type="button"
            @click="openProject(project)"
          >
            <span class="project-icon">{{ project.iconText }}</span>
            <div>
              <h4>{{ project.name }}</h4>
            </div>
          </button>
        </div>
      </div>

      <div v-else class="project-workbench">
        <aside class="feature-sidebar">
          <div class="sidebar-project">
            <span class="project-icon">{{ selectedProject?.iconText }}</span>
            <div>
              <p>{{ selectedProject?.code }}</p>
              <h3>{{ selectedProject?.name }}</h3>
            </div>
          </div>
          <button
            v-if="currentView === 'admin'"
            class="feature-item active"
            type="button"
            @click="activeAdminFeature = 'user-list'"
          >
            用户列表
            <small>USER LIST</small>
          </button>
          <template v-else-if="isCloudCheckinProject">
            <button
              :class="['feature-item', { active: activeCloudCheckinFeature === 'store-rop-registration' }]"
              type="button"
              @click="activeCloudCheckinFeature = 'store-rop-registration'"
            >
              门店 ROP 信息注册
              <small>STORE ROP REGISTER</small>
            </button>
            <button
              :class="['feature-item', { active: activeCloudCheckinFeature === 'address-validation' }]"
              type="button"
              @click="activeCloudCheckinFeature = 'address-validation'"
            >
              地址校验
              <small>ADDRESS VALIDATION</small>
            </button>
          </template>
          <template v-else-if="isMihotelProject">
            <button
              :class="['feature-item', { active: activeMihotelFeature === 'system-params' }]"
              type="button"
              @click="activeMihotelFeature = 'system-params'"
            >
              系统参数管理
              <small>SYSTEM PARAMS</small>
            </button>
            <button
              :class="['feature-item', { active: activeMihotelFeature === 'clear-cache' }]"
              type="button"
              @click="activeMihotelFeature = 'clear-cache'"
            >
              清除缓存
              <small>CLEAR CACHE</small>
            </button>
          </template>
          <template v-else-if="isIhotelProject">
            <button
              :class="['feature-item', { active: activeIhotelFeature === 'tmh-mock-companies' }]"
              type="button"
              @click="activeIhotelFeature = 'tmh-mock-companies'"
            >
              天目湖接口模拟数据
              <small>TMH MOCK DATA</small>
            </button>
          </template>
          <p v-else class="feature-empty">功能模块待补充</p>
        </aside>

        <div class="workbench-content">
          <div v-if="currentView === 'admin'" class="content-area admin-grid project-content">
            <section v-if="activeAdminFeature === 'user-list'" class="panel user-list-panel">
              <div class="section-title user-list-header">
                <h3>用户列表</h3>
                <button class="primary-button add-user-button" type="button" @click="openCreateUserDialog">新增用户</button>
              </div>
              <div class="user-table">
                <div class="user-table-head">
                  <span>用户名</span>
                  <span>账号</span>
                  <span>用户类型</span>
                  <span>状态</span>
                  <span>到期时间</span>
                  <span>操作</span>
                </div>
                <div v-for="user in pagedUsers" :key="user.username" class="user-table-row">
                  <span>{{ user.displayName }}</span>
                  <span>{{ user.username }}</span>
                  <span>
                    <span :class="userTypeBadgeClass(user.userType)">{{ displayUserType(user.userType) }}</span>
                  </span>
                  <span>
                    <span :class="statusBadgeClass(user)">{{ displayUserStatus(user) }}</span>
                  </span>
                  <span class="expires-cell">
                    <span v-if="isPermanentAccessUser(user)" :class="permanentBadgeClass(user)">
                      <span class="permanent-badge-icon">∞</span>
                      永久
                    </span>
                    <span v-else>{{ formatDateTime(user.expiresAt) }}</span>
                  </span>
                  <div class="user-actions">
                    <button
                      v-if="canEditUserInfo(user)"
                      class="ghost-button action-button"
                      type="button"
                      @click="openEditUserDialog(user)"
                    >
                      修改信息
                    </button>
                    <button
                      v-if="canExtendTemporaryUser(user)"
                      class="ghost-button action-button"
                      type="button"
                      @click="openExtendTimeDialog(user)"
                    >
                      增加时间
                    </button>
                    <button class="ghost-button action-button" type="button" @click="openPasswordDialog(user)">修改密码</button>
                    <button
                      v-if="!isCurrentUserRow(user)"
                      class="danger-button action-button"
                      type="button"
                      @click="openDeleteDialog(user)"
                    >
                      删除用户
                    </button>
                  </div>
                </div>
                <div v-if="pagedUsers.length === 0" class="empty-state">暂无用户</div>
              </div>
              <div class="pagination-bar">
                <span>共 {{ userTotal }} 条</span>
                <div>
                  <button class="ghost-button" :disabled="userPage === 1" @click="previousUserPage">上一页</button>
                  <span>{{ userPage }} / {{ totalUserPages }}</span>
                  <button class="ghost-button" :disabled="userPage === totalUserPages" @click="nextUserPage">下一页</button>
                </div>
              </div>
            </section>
          </div>

          <section
            v-else-if="isCloudCheckinProject && activeCloudCheckinFeature === 'store-rop-registration'"
            class="workspace-panel cloud-feature-panel"
          >
            <p class="eyebrow">CLOUD CHECK-IN</p>
            <h3>门店 ROP 信息注册</h3>
            <form class="rop-registration-form" @submit.prevent="submitRopRegistration">
              <section class="rop-form-section">
                <div class="rop-section-heading">
                  <span class="rop-step">01</span>
                  <div>
                    <h4>ROP 认证信息</h4>
                    <p>上传认证信息文件，或直接粘贴完整认证内容；识别到 hotelGroupCode 后会自动带出集团信息。</p>
                  </div>
                </div>
                <div class="rop-auth-actions">
                  <label class="rop-upload-button">
                    上传认证文件
                    <input
                      accept=".sycs,.txt,.json,.properties,.conf"
                      type="file"
                      @change="handleRopAuthFileChange"
                    />
                  </label>
                  <button class="ghost-button rop-manual-button" type="button" @click="openRopAuthManualDialog">
                    手动输入
                  </button>
                </div>
                <div class="rop-auth-preview">
                  <span class="rop-auth-label">当前认证信息</span>
                  <strong v-if="ropAuthFileName">{{ ropAuthFileName }}</strong>
                  <strong v-else-if="ropAuthManualContent.trim()">已手动输入认证内容</strong>
                  <span v-else>暂未提供</span>
                </div>
              </section>

            <section class="rop-form-section">
              <div class="rop-section-heading">
                <span class="rop-step">02</span>
                <div>
                  <h4>集团信息</h4>
                  <p>集团代码可由 ROP 认证信息自动带出，也可以手动填写或修正；集团地址支持查询后继续编辑。</p>
                </div>
              </div>
              <div class="rop-query-row">
                <label>
                  集团代码
                  <input
                    v-model.trim="ropGroupCode"
                    autocomplete="off"
                    placeholder="例如 DHYTJSG"
                    @keyup.enter="lookupRopGroupAddress()"
                  />
                </label>
                <button
                  class="primary-button rop-query-button"
                  type="button"
                  :disabled="ropGroupLookupLoading"
                  @click="lookupRopGroupAddress()"
                >
                  {{ ropGroupLookupLoading ? '查询中...' : '查询' }}
                </button>
              </div>
              <p v-if="ropGroupLookupMessage" class="rop-query-message">{{ ropGroupLookupMessage }}</p>
              <label class="rop-full-field">
                集团地址
                <input
                  v-model.trim="ropGroupAddress"
                  autocomplete="off"
                  placeholder="请输入集团地址，例如 https://group.example.com"
                />
              </label>
            </section>

            <div class="rop-form-footer">
              <button class="primary-button rop-submit-button" type="submit">生成</button>
            </div>
          </form>
          <section v-if="ropRegistrationEncryptedText" class="rop-result-panel">
            <div class="rop-result-header">
              <div>
                <p class="eyebrow">REGISTER RESULT</p>
                <h4>门店云入住信息</h4>
              </div>
              <div class="rop-copy-actions">
                <span v-if="ropCopyMessage">{{ ropCopyMessage }}</span>
                <button class="primary-button rop-copy-button" type="button" @click="copyRopRegistrationEncryptedText">
                  复制注册信息
                </button>
              </div>
            </div>
            <textarea
              class="rop-result-textarea"
              :value="ropRegistrationEncryptedText"
              readonly
              rows="5"
            ></textarea>
          </section>
        </section>

        <section
          v-else-if="isCloudCheckinProject && activeCloudCheckinFeature === 'address-validation'"
          class="workspace-panel cloud-feature-panel"
        >
          <p class="eyebrow">CLOUD CHECK-IN</p>
          <h3>地址校验</h3>
          <form class="rop-registration-form" @submit.prevent="lookupAddressValidationStoreConfig">
            <section class="rop-form-section">
              <div class="rop-section-heading">
                <span class="rop-step">01</span>
                <div>
                  <h4>门店配置查询</h4>
                  <p>输入门店代码查询该酒店是否已有云入住配置，查到后可继续手动修正参数。</p>
                </div>
              </div>
              <div class="rop-query-row">
                <label>
                  门店代码
                  <input
                    v-model.trim="addressValidationStoreCode"
                    autocomplete="off"
                    placeholder="例如 LOHKAH001"
                  />
                </label>
                <button
                  class="primary-button rop-query-button"
                  type="submit"
                  :disabled="addressValidationLoading"
                >
                  {{ addressValidationLoading ? '查询中...' : '查询' }}
                </button>
              </div>
              <p v-if="addressValidationMessage" class="rop-query-message">{{ addressValidationMessage }}</p>
            </section>
          </form>

          <section v-if="addressValidationFound" class="rop-form-section">
            <div class="rop-section-heading">
              <span class="rop-step">02</span>
              <div>
                <h4>配置参数</h4>
                <p>查询到的配置会自动拆分到下方字段，确认或修正后点击生成。</p>
              </div>
            </div>
            <div class="address-config-grid">
              <label class="address-config-full">
                集团地址
                <input v-model.trim="addressValidationGroupAddress" autocomplete="off" />
              </label>
              <label>
                user
                <input v-model.trim="addressValidationUsername" autocomplete="off" />
              </label>
              <label>
                password
                <input v-model.trim="addressValidationPassword" autocomplete="off" />
              </label>
              <label>
                appKey
                <input v-model.trim="addressValidationAppKey" autocomplete="off" />
              </label>
              <label>
                appSecret
                <input v-model.trim="addressValidationAppSecret" autocomplete="off" />
              </label>
            </div>
            <div class="rop-form-footer">
              <button class="primary-button rop-submit-button" type="button" @click="generateAddressValidationConfig">
                生成
              </button>
            </div>
          </section>

          <section v-if="addressValidationGeneratedText" class="rop-result-panel">
            <div class="rop-result-header">
              <div>
                <p class="eyebrow">CONFIG RESULT</p>
                <h4>云入住配置</h4>
              </div>
              <div class="rop-copy-actions">
                <span v-if="addressValidationCopyMessage">{{ addressValidationCopyMessage }}</span>
                <button class="primary-button rop-copy-button" type="button" @click="copyAddressValidationConfig">
                  复制配置
                </button>
              </div>
            </div>
            <textarea
              class="rop-result-textarea"
              :value="addressValidationGeneratedText"
              readonly
              rows="4"
            ></textarea>
          </section>
        </section>

          <section
            v-else-if="isMihotelProject && activeMihotelFeature === 'system-params'"
            class="workspace-panel mihotel-system-param-panel"
          >
            <div class="mihotel-system-param-header">
              <div>
                <p class="eyebrow">SYSTEM PARAMS</p>
                <h3>系统参数管理</h3>
              </div>
              <form class="system-param-query-bar" @submit.prevent="submitMihotelSystemParamQuery">
                <label>
                  查询环境
                  <select v-model="mihotelSystemParamEnvironment">
                    <option
                      v-for="environment in visibleMihotelSystemParamEnvironments"
                      :key="environment.code"
                      :value="environment.code"
                    >
                      {{ environment.name }}
                    </option>
                  </select>
                </label>
                <label>
                  集团代码
                  <input
                    v-model.trim="mihotelSystemParamGroupCode"
                    autocomplete="off"
                    placeholder="请输入 hotelGroupCode"
                  />
                </label>
                <button class="primary-button system-param-query-button" type="submit" :disabled="mihotelSystemParamLoading">
                  {{ mihotelSystemParamLoading ? '查询中...' : '查询参数' }}
                </button>
                <button
                  v-if="isAdmin"
                  class="primary-button system-param-tool-button"
                  type="button"
                  @click="openSystemParamCreatePlaceholder"
                >
                  新增系统参数
                </button>
                <button class="primary-button system-param-tool-button" type="button" @click="openPmsSdkParamDialog">
                  生成PMS SDK 参数
                </button>
              </form>
            </div>

            <div class="system-param-result-meta">
              <span v-if="mihotelSystemParamLastQuery">{{ mihotelSystemParamLastQuery }}</span>
              <span v-if="mihotelSystemParamMessage">{{ mihotelSystemParamMessage }}</span>
            </div>

            <div class="system-param-table">
              <div class="system-param-table-head">
                <span>分类</span>
                <span>参数项</span>
                <span>设置值</span>
                <span>默认值</span>
                <span>可修改</span>
                <span>描述</span>
                <span>操作</span>
              </div>
              <div
                v-for="record in mihotelSystemParamRecords"
                :key="`${record.catalog}-${record.item}-${record.hotelCode}`"
                class="system-param-table-row"
              >
                <span :title="systemParamText(record.catalog)">{{ systemParamText(record.catalog) }}</span>
                <strong :title="systemParamText(record.item)">{{ systemParamText(record.item) }}</strong>
                <span class="system-param-value" :title="systemParamText(record.setValue)">{{ systemParamText(record.setValue) }}</span>
                <span class="system-param-value" :title="systemParamText(record.defValue)">{{ systemParamText(record.defValue) }}</span>
                <span :title="systemParamText(record.isMod)">{{ systemParamText(record.isMod) }}</span>
                <span :title="systemParamText(record.descript)">{{ systemParamText(record.descript) }}</span>
                <span>
                  <button
                    v-if="canEditMihotelSystemParam(record)"
                    class="ghost-button system-param-row-button"
                    type="button"
                    @click="openSystemParamEditDialog(record)"
                  >
                    修改
                  </button>
                </span>
              </div>
              <div v-if="!mihotelSystemParamLoading && mihotelSystemParamRecords.length === 0" class="empty-state">
                输入集团代码后查询系统参数
              </div>
            </div>
          </section>

          <section v-else-if="isMihotelProject && activeMihotelFeature === 'clear-cache'" class="workspace-panel mihotel-cache-panel">
            <div class="mihotel-cache-header">
              <div>
                <p class="eyebrow">MIHOTEL CACHE</p>
                <h3>清除缓存</h3>
              </div>
              <button
                class="ghost-button"
                type="button"
                :disabled="mihotelCacheLoading || mihotelCacheRunning"
                @click="loadMihotelCacheTargets"
              >
                刷新节点
              </button>
            </div>

            <div class="mihotel-cache-summary">
              <div>
                <strong>主干环境</strong>
                <span>{{ mihotelTrunkCacheTargets.length }} 个服务，按顺序逐个清理</span>
              </div>
              <div v-if="canUseMihotelLocalTools">
                <strong>本地环境</strong>
                <span>{{ mihotelLocalCacheTargets.length }} 个服务，单独清理</span>
              </div>
            </div>

            <p v-if="mihotelCacheMessage" class="mihotel-cache-message">{{ mihotelCacheMessage }}</p>

            <div class="cache-environment-grid">
              <section class="cache-group-card">
                <div class="cache-group-heading">
                  <div>
                    <p class="eyebrow">TRUNK</p>
                    <h4>主干环境</h4>
                  </div>
                  <button
                    class="primary-button cache-action-button"
                    type="button"
                    :disabled="mihotelCacheLoading || mihotelCacheRunning || mihotelTrunkCacheTargets.length === 0"
                    @click="clearMihotelCache('TRUNK')"
                  >
                    {{ mihotelCacheRunning ? '执行中...' : '清理主干' }}
                  </button>
                </div>

                <div class="cache-node-list">
                  <div
                    v-for="(target, index) in mihotelTrunkCacheTargets"
                    :key="target.code"
                    :class="['cache-node', mihotelCacheStatus(target.code)]"
                  >
                    <span class="cache-node-index">{{ String(index + 1).padStart(2, '0') }}</span>
                    <div class="cache-node-main">
                      <strong>{{ target.name }}</strong>
                      <small>{{ mihotelCacheResult(target.code)?.message || '等待清理指令' }}</small>
                    </div>
                    <span class="cache-duration">{{ formatDuration(mihotelCacheResult(target.code)?.durationMillis) }}</span>
                    <span class="cache-status-badge">{{ cacheStatusLabel(mihotelCacheStatus(target.code)) }}</span>
                  </div>
                  <div v-if="!mihotelCacheLoading && mihotelTrunkCacheTargets.length === 0" class="cache-empty">
                    主干清理目标未配置
                  </div>
                </div>
              </section>

              <section v-if="canUseMihotelLocalTools" class="cache-group-card">
                <div class="cache-group-heading">
                  <div>
                    <p class="eyebrow">LOCAL</p>
                    <h4>本地环境</h4>
                  </div>
                  <button
                    class="primary-button cache-action-button"
                    type="button"
                    :disabled="mihotelCacheLoading || mihotelCacheRunning || mihotelLocalCacheTargets.length === 0"
                    @click="clearMihotelCache('LOCAL')"
                  >
                    {{ mihotelCacheRunning ? '执行中...' : '清理本地' }}
                  </button>
                </div>

                <div class="cache-node-list">
                  <div
                    v-for="(target, index) in mihotelLocalCacheTargets"
                    :key="target.code"
                    :class="['cache-node', mihotelCacheStatus(target.code)]"
                  >
                    <span class="cache-node-index">{{ String(index + 1).padStart(2, '0') }}</span>
                    <div class="cache-node-main">
                      <strong>{{ target.name }}</strong>
                      <small>{{ mihotelCacheResult(target.code)?.message || '等待清理指令' }}</small>
                    </div>
                    <span class="cache-duration">{{ formatDuration(mihotelCacheResult(target.code)?.durationMillis) }}</span>
                    <span class="cache-status-badge">{{ cacheStatusLabel(mihotelCacheStatus(target.code)) }}</span>
                  </div>
                  <div v-if="!mihotelCacheLoading && mihotelLocalCacheTargets.length === 0" class="cache-empty">
                    本地清理目标未配置
                  </div>
                </div>
              </section>
            </div>
          </section>

          <TmhMockCompanyPanel
            v-else-if="isIhotelProject && activeIhotelFeature === 'tmh-mock-companies'"
          />
          <section v-else class="workspace-panel">
            <p class="eyebrow">{{ selectedProject?.code }}</p>
            <h3>运维工作区骨架</h3>
            <p>这里后续承载 {{ selectedProject?.name }} 的具体运维工具。功能清单待你下次补充。</p>
          </section>
        </div>
      </div>
    </section>

    <div v-if="userDialogVisible" class="dialog-backdrop" role="presentation">
      <form class="form-dialog user-form-dialog" @submit.prevent="submitUserForm">
        <div class="dialog-header">
          <h3>{{ userDialogMode === 'create' ? '新增用户' : '修改用户信息' }}</h3>
          <button class="icon-close-button" type="button" aria-label="关闭" @click="closeUserDialog">×</button>
        </div>

        <div class="form-grid">
          <label>
            账号 code
            <input v-model.trim="userForm.username" :disabled="userDialogMode === 'edit'" autocomplete="off" required />
          </label>
          <label>
            用户名
            <input v-model.trim="userForm.displayName" autocomplete="off" required />
          </label>
          <label>
            用户类型
            <select v-model="userForm.userType" :disabled="userDialogMode === 'edit'">
              <option value="PERMANENT">永久用户</option>
              <option value="TEMPORARY">临时用户</option>
            </select>
          </label>
          <label v-if="userForm.userType !== 'TEMPORARY'">
            状态
            <select v-model="userForm.enabled">
              <option :value="true">启用</option>
              <option :value="false">禁用</option>
            </select>
          </label>
          <label v-if="userDialogMode === 'create' && userForm.userType === 'TEMPORARY'">
            可用时间（小时）
            <input v-model.number="userForm.validHours" min="1" step="1" type="number" required />
          </label>
        </div>

        <section class="project-picker">
          <h4>授权项目</h4>
          <label v-for="project in assignableProjects" :key="project.code" class="check-row">
            <input v-model="userForm.projectCodes" type="checkbox" :value="project.code" />
            <span>{{ project.name }}</span>
            <small>{{ project.code }}</small>
          </label>
        </section>

        <div class="dialog-actions">
          <button class="ghost-button" type="button" @click="closeUserDialog">取消</button>
          <button class="primary-button dialog-submit-button" type="submit" :disabled="formSaving">
            {{ formSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="passwordDialogVisible" class="dialog-backdrop" role="presentation">
      <form class="form-dialog password-form-dialog" @submit.prevent="submitPasswordForm">
        <div class="dialog-header">
          <h3>修改密码</h3>
          <button class="icon-close-button" type="button" aria-label="关闭" @click="closePasswordDialog">×</button>
        </div>
        <label class="inline-password-field">
          密码：
          <input v-model="passwordForm.password" autocomplete="new-password" type="password" required />
        </label>
        <div class="dialog-actions">
          <button class="ghost-button" type="button" @click="closePasswordDialog">取消</button>
          <button class="primary-button dialog-submit-button" type="submit" :disabled="formSaving">
            {{ formSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="extendTimeDialogVisible" class="dialog-backdrop" role="presentation">
      <form class="form-dialog password-form-dialog" @submit.prevent="submitExtendTimeForm">
        <div class="dialog-header">
          <h3>增加临时时间</h3>
          <button class="icon-close-button" type="button" aria-label="关闭" @click="closeExtendTimeDialog">×</button>
        </div>
        <p>给临时用户 {{ selectedUser?.username }} 增加可用时间。</p>
        <label class="inline-password-field">
          小时：
          <input v-model.number="extendTimeForm.extendHours" min="1" step="1" type="number" required />
        </label>
        <div class="dialog-actions">
          <button class="ghost-button" type="button" @click="closeExtendTimeDialog">取消</button>
          <button class="primary-button dialog-submit-button" type="submit" :disabled="formSaving">
            {{ formSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="deleteDialogVisible" class="dialog-backdrop" role="presentation">
      <section class="form-dialog password-form-dialog">
        <div class="dialog-header">
          <h3>删除用户</h3>
          <button class="icon-close-button" type="button" aria-label="关闭" @click="closeDeleteDialog">×</button>
        </div>
        <p>确定删除用户 {{ selectedUser?.username }} 吗？删除后会同步清理该用户的项目授权。</p>
        <div class="dialog-actions">
          <button class="ghost-button" type="button" @click="closeDeleteDialog">取消</button>
          <button class="danger-button dialog-submit-button" type="button" :disabled="formSaving" @click="confirmDeleteUser">
            {{ formSaving ? '删除中...' : '删除' }}
          </button>
        </div>
      </section>
    </div>

    <div v-if="ropAuthManualDialogVisible" class="dialog-backdrop" role="presentation">
      <form class="form-dialog rop-manual-dialog" @submit.prevent="confirmRopAuthManualInput">
        <div class="dialog-header">
          <div>
            <p class="eyebrow">ROP AUTH</p>
            <h3>手动输入 ROP 认证信息</h3>
          </div>
          <button class="icon-close-button" type="button" aria-label="关闭" @click="closeRopAuthManualDialog">×</button>
        </div>
        <label>
          认证信息内容
          <textarea
            v-model="ropAuthManualContent"
            placeholder="请粘贴 ROP 认证信息内容，支持完整配置文本或认证参数片段"
            rows="10"
          ></textarea>
        </label>
        <div class="dialog-actions">
          <button class="ghost-button" type="button" @click="closeRopAuthManualDialog">取消</button>
          <button class="primary-button dialog-submit-button" type="submit">确认使用</button>
        </div>
      </form>
    </div>

    <div v-if="pmsSdkDialogVisible" class="dialog-backdrop" role="presentation">
      <section class="form-dialog pms-sdk-dialog" role="dialog" aria-modal="true">
        <div class="dialog-header">
          <div>
            <p class="eyebrow">PMS SDK PARAM</p>
            <h3>生成 PMS SDK 参数</h3>
          </div>
          <button class="icon-close-button" type="button" aria-label="关闭" @click="closePmsSdkParamDialog">×</button>
        </div>

        <section class="rop-form-section">
          <div class="pms-sdk-mode-tabs">
            <label :class="['pms-sdk-mode-button', 'pms-sdk-upload-mode-button', pmsSdkInputMode === 'upload' ? 'active' : '']">
              上传认证文件
              <input
                accept=".sycs,.txt,.json,.properties,.conf"
                type="file"
                @click="switchPmsSdkInputMode('upload')"
                @change="handlePmsSdkAuthFileChange"
              />
            </label>
            <button
              :class="['pms-sdk-mode-button', pmsSdkInputMode === 'manual' ? 'active' : '']"
              type="button"
              @click="switchPmsSdkInputMode('manual')"
            >
              手动输入
            </button>
          </div>
          <label v-if="pmsSdkInputMode === 'manual'" class="pms-sdk-manual-field">
            认证信息内容
            <textarea
              v-model="pmsSdkManualContent"
              placeholder="请粘贴 ROP 认证信息内容，支持完整配置文本或认证参数片段"
              rows="9"
              @input="pmsSdkGeneratedText = ''; pmsSdkCopyMessage = ''"
            ></textarea>
          </label>
          <div class="rop-auth-preview">
            <span class="rop-auth-label">当前认证信息</span>
            <strong v-if="pmsSdkAuthSourceLabel() !== '暂未提供'">{{ pmsSdkAuthSourceLabel() }}</strong>
            <span v-else>暂未提供</span>
          </div>
        </section>

        <div class="pms-sdk-dialog-actions">
          <span v-if="pmsSdkMessage">{{ pmsSdkMessage }}</span>
          <button
            class="primary-button rop-submit-button"
            type="button"
            :disabled="pmsSdkGenerating"
            @click="generatePmsSdkParam"
          >
            {{ pmsSdkGenerating ? '生成中...' : '生成' }}
          </button>
        </div>

        <section v-if="pmsSdkGeneratedText" class="rop-result-panel">
          <div class="rop-result-header">
            <div>
              <p class="eyebrow">PARAM RESULT</p>
              <h4>PMS SDK 参数</h4>
            </div>
            <div class="rop-copy-actions">
              <span v-if="pmsSdkCopyMessage">{{ pmsSdkCopyMessage }}</span>
              <button class="primary-button rop-copy-button" type="button" @click="copyPmsSdkGeneratedText">
                复制参数
              </button>
            </div>
          </div>
          <textarea
            class="rop-result-textarea"
            :value="pmsSdkGeneratedText"
            readonly
            rows="4"
          ></textarea>
        </section>
      </section>
    </div>

    <div v-if="systemParamDialogVisible" class="dialog-backdrop" role="presentation">
      <form class="form-dialog system-param-dialog" @submit.prevent="submitSystemParamForm">
        <div class="dialog-header">
          <div>
            <p class="eyebrow">SYSTEM PARAM</p>
            <h3>{{ systemParamDialogMode === 'create' ? '新增系统参数' : '修改系统参数' }}</h3>
          </div>
          <button class="icon-close-button" type="button" aria-label="关闭" @click="closeSystemParamDialog">×</button>
        </div>

        <div class="system-param-form-grid">
          <label>
            集团代码
            <input v-model.trim="systemParamForm.hotelGroupCode" :disabled="systemParamDialogMode === 'edit'" autocomplete="off" required />
          </label>
          <label>
            参数分类
            <input v-model.trim="systemParamForm.catalog" :disabled="systemParamDialogMode === 'edit'" autocomplete="off" required />
          </label>
          <label>
            参数项
            <input v-model.trim="systemParamForm.item" :disabled="systemParamDialogMode === 'edit'" autocomplete="off" required />
          </label>
          <label>
            默认值
            <input v-model="systemParamForm.defValue" :disabled="systemParamDialogMode === 'edit'" autocomplete="off" />
          </label>
          <label class="system-param-form-full">
            设置值
            <textarea v-model="systemParamForm.setValue" rows="5"></textarea>
          </label>
          <label>
            中文描述
            <input v-model="systemParamForm.descript" :disabled="systemParamDialogMode === 'edit'" autocomplete="off" />
          </label>
          <label>
            英文描述
            <input v-model="systemParamForm.descriptEn" :disabled="systemParamDialogMode === 'edit'" autocomplete="off" />
          </label>
        </div>

        <div class="dialog-actions">
          <button class="ghost-button" type="button" @click="closeSystemParamDialog">取消</button>
          <button class="primary-button dialog-submit-button" type="submit" :disabled="systemParamSaving">
            {{ systemParamSaving ? '保存中...' : '保存' }}
          </button>
        </div>
      </form>
    </div>

    <div v-if="errorMessage && currentView !== 'login'" class="dialog-backdrop" role="presentation">
      <section class="error-dialog" role="alertdialog" aria-modal="true" aria-labelledby="admin-error-dialog-title">
        <div class="dialog-mark">!</div>
        <div>
          <h3 id="admin-error-dialog-title">操作失败</h3>
          <p>{{ errorMessage }}</p>
        </div>
        <button class="primary-button dialog-button" type="button" @click="closeErrorDialog">确定</button>
      </section>
    </div>

    <div v-if="successMessage" class="dialog-backdrop" role="presentation">
      <section class="error-dialog success-dialog" role="status" aria-live="polite">
        <div class="dialog-mark">✓</div>
        <div>
          <h3>操作成功</h3>
          <p>{{ successMessage }}</p>
        </div>
        <button class="primary-button dialog-button" type="button" @click="closeSuccessDialog">确定</button>
      </section>
    </div>
  </main>
</template>
