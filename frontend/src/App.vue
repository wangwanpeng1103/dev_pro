<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  createPermanentUser,
  createTemporaryUser,
  deleteUser,
  listProjects,
  listUsers,
  login,
  updateUser,
  updateUserPassword,
  type ProjectModule,
  type UserType,
  type UserAccount
} from './api'

type ViewName = 'login' | 'projects' | 'admin' | 'project'
type AdminFeature = 'user-list'
type UserDialogMode = 'create' | 'edit'

const currentView = ref<ViewName>('login')
const username = ref('admin')
const password = ref('admin')
const errorMessage = ref('')
const successMessage = ref('')
const loading = ref(false)
const currentUser = ref<UserAccount | null>(null)
const projects = ref<ProjectModule[]>([])
const users = ref<UserAccount[]>([])
const selectedProject = ref<ProjectModule | null>(null)
const activeAdminFeature = ref<AdminFeature>('user-list')
const userPage = ref(1)
const userPageSize = 10
const userTotal = ref(0)
const userTotalPages = ref(1)
const userDialogMode = ref<UserDialogMode>('create')
const userDialogVisible = ref(false)
const passwordDialogVisible = ref(false)
const deleteDialogVisible = ref(false)
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

const isAdmin = computed(() => currentUser.value?.userType === 'ADMIN')
const visibleProjects = computed(() => projects.value.filter((project) => project.enabled))
const assignableProjects = computed(() => visibleProjects.value.filter((project) => project.code !== 'user-admin'))
const totalUserPages = computed(() => Math.max(1, userTotalPages.value))
const pagedUsers = computed(() => users.value)
const pageTitle = computed(() => {
  if (currentView.value === 'projects') {
    return '项目模块'
  }
  return selectedProject.value?.name ?? '绿云运维控制台'
})

async function handleLogin() {
  errorMessage.value = ''
  loading.value = true
  try {
    const result = await login(username.value, password.value)
    currentUser.value = result.user
    projects.value = result.projects
    currentView.value = 'projects'
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
  }
  currentView.value = project.code === 'user-admin' ? 'admin' : 'project'
}

function logout() {
  currentUser.value = null
  selectedProject.value = null
  currentView.value = 'login'
}

function backToProjects() {
  selectedProject.value = null
  currentView.value = 'projects'
}

function closeErrorDialog() {
  errorMessage.value = ''
}

function closeSuccessDialog() {
  successMessage.value = ''
}

function displayUserType(userType: UserAccount['userType']) {
  const labelMap: Record<UserAccount['userType'], string> = {
    ADMIN: '管理员',
    PERMANENT: '永久用户',
    TEMPORARY: '临时用户'
  }
  return labelMap[userType]
}

function isCurrentUserRow(user: UserAccount) {
  return user.username === currentUser.value?.username
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

async function submitUserForm() {
  formSaving.value = true
  errorMessage.value = ''
  try {
    if (userDialogMode.value === 'create') {
      const payload = {
        username: userForm.value.username,
        displayName: userForm.value.displayName,
        enabled: userForm.value.enabled,
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
        await createPermanentUser(payload)
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

      <section class="login-card">
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
        <button class="primary-button" :disabled="loading" @click="handleLogin">
          {{ loading ? '登录中...' : '登录' }}
        </button>
      </section>

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
          <p v-else class="feature-empty">功能模块待补充</p>
        </aside>

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
                <span>{{ displayUserType(user.userType) }}</span>
                <span>{{ user.enabled ? '启用' : '禁用' }}</span>
                <span>{{ user.expiresAt ?? '-' }}</span>
                <div class="user-actions">
                  <button
                    v-if="!isCurrentUserRow(user)"
                    class="ghost-button action-button"
                    type="button"
                    @click="openEditUserDialog(user)"
                  >
                    修改信息
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

        <section v-else class="workspace-panel">
          <p class="eyebrow">{{ selectedProject?.code }}</p>
          <h3>运维工作区骨架</h3>
          <p>这里后续承载 {{ selectedProject?.name }} 的具体运维工具。功能清单待你下次补充。</p>
        </section>
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
          <label>
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
