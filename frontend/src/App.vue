<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  listProjects,
  listUsers,
  login,
  type ProjectModule,
  type UserAccount
} from './api'

type ViewName = 'login' | 'projects' | 'admin' | 'project'
type AdminFeature = 'user-list'

const currentView = ref<ViewName>('login')
const username = ref('admin')
const password = ref('admin')
const errorMessage = ref('')
const loading = ref(false)
const currentUser = ref<UserAccount | null>(null)
const projects = ref<ProjectModule[]>([])
const users = ref<UserAccount[]>([])
const selectedProject = ref<ProjectModule | null>(null)
const activeAdminFeature = ref<AdminFeature>('user-list')
const userPage = ref(1)
const userPageSize = 8

const isAdmin = computed(() => currentUser.value?.userType === 'ADMIN')
const visibleProjects = computed(() => projects.value.filter((project) => project.enabled))
const managedUsers = computed(() => users.value.filter((user) => user.username !== 'admin'))
const totalUserPages = computed(() => Math.max(1, Math.ceil(managedUsers.value.length / userPageSize)))
const pagedUsers = computed(() => {
  const start = (userPage.value - 1) * userPageSize
  return managedUsers.value.slice(start, start + userPageSize)
})
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
  users.value = await listUsers()
  projects.value = await listProjects(currentUser.value?.username)
}

async function openProject(project: ProjectModule) {
  selectedProject.value = project
  if (project.code === 'user-admin') {
    await refreshAdminData()
    activeAdminFeature.value = 'user-list'
    userPage.value = 1
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

function displayUserType(userType: UserAccount['userType']) {
  const labelMap: Record<UserAccount['userType'], string> = {
    ADMIN: '管理员',
    PERMANENT: '永久用户',
    TEMPORARY: '临时用户'
  }
  return labelMap[userType]
}

function previousUserPage() {
  userPage.value = Math.max(1, userPage.value - 1)
}

function nextUserPage() {
  userPage.value = Math.min(totalUserPages.value, userPage.value + 1)
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
            <div class="section-title">
              <h3>用户列表</h3>
              <p>展示除 admin 外的所有用户，后续新增用户管理动作时在此页扩展。</p>
            </div>
            <div class="user-table">
              <div class="user-table-head">
                <span>显示名称</span>
                <span>账号</span>
                <span>用户类型</span>
                <span>状态</span>
                <span>到期时间</span>
              </div>
              <div v-for="user in pagedUsers" :key="user.username" class="user-table-row">
                <strong>{{ user.displayName }}</strong>
                <span>{{ user.username }}</span>
                <span>{{ displayUserType(user.userType) }}</span>
                <span>{{ user.enabled ? '启用' : '禁用' }}</span>
                <span>{{ user.expiresAt ?? '-' }}</span>
              </div>
              <div v-if="pagedUsers.length === 0" class="empty-state">暂无用户</div>
            </div>
            <div class="pagination-bar">
              <span>共 {{ managedUsers.length }} 条</span>
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
  </main>
</template>
