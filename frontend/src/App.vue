<script setup lang="ts">
import { computed, ref } from 'vue'
import {
  listFunctionNodes,
  listProjects,
  listUsers,
  login,
  type FunctionNode,
  type ProjectModule,
  type UserAccount
} from './api'

type ViewName = 'login' | 'projects' | 'admin' | 'project'

const currentView = ref<ViewName>('login')
const username = ref('admin')
const password = ref('admin')
const errorMessage = ref('')
const loading = ref(false)
const currentUser = ref<UserAccount | null>(null)
const projects = ref<ProjectModule[]>([])
const users = ref<UserAccount[]>([])
const selectedProject = ref<ProjectModule | null>(null)
const selectedFunctions = ref<FunctionNode[]>([])
const adminTab = ref<'users' | 'projects' | 'functions'>('users')
const temporaryValidHours = ref(24)

const isAdmin = computed(() => currentUser.value?.userType === 'ADMIN')
const visibleProjects = computed(() => projects.value.filter((project) => project.enabled))
const permanentUsers = computed(() => users.value.filter((user) => user.userType !== 'TEMPORARY'))
const temporaryUsers = computed(() => users.value.filter((user) => user.userType === 'TEMPORARY'))

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
  selectedFunctions.value = await listFunctionNodes(project.code)
  currentView.value = project.code === 'user-admin' ? 'admin' : 'project'
}

function logout() {
  currentUser.value = null
  selectedProject.value = null
  selectedFunctions.value = []
  currentView.value = 'login'
}

function backToProjects() {
  selectedProject.value = null
  selectedFunctions.value = []
  currentView.value = 'projects'
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
          <p class="brand-copy">集中管理项目入口、用户授权和可扩展运维功能树。</p>
        </div>
        <div class="quote-card">
          <p>把入口收拢到一处，把权限留在边界，把运维动作变得清楚。</p>
          <strong>绿云运维平台</strong>
        </div>
        <ul class="feature-list">
          <li>用户直接授权到项目模块</li>
          <li>支持永久用户和可配置有效期临时用户</li>
          <li>项目功能树可配置，预留外部链接与 SSO</li>
        </ul>
      </aside>

      <section class="login-card">
        <p class="eyebrow">登录系统</p>
        <h2>进入绿云运维控制台</h2>
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
        <p v-if="errorMessage" class="error-text">{{ errorMessage }}</p>
      </section>
    </section>

    <section v-else class="console-layout">
      <aside class="console-sidebar">
        <div class="sidebar-brand">
          <span class="brand-mark small">GY</span>
          <div>
            <p>绿云运维</p>
            <strong>控制台</strong>
          </div>
        </div>
        <nav>
          <button :class="{ active: currentView === 'projects' }" @click="backToProjects">项目入口</button>
          <button v-if="isAdmin" :class="{ active: currentView === 'admin' }" @click="currentView = 'admin'">
            用户管理
          </button>
        </nav>
      </aside>

      <section class="console-main">
        <header class="topbar">
          <div>
            <p class="eyebrow">{{ currentView === 'project' ? selectedProject?.code : 'OPS CONSOLE' }}</p>
            <h2>
              {{
                currentView === 'projects'
                  ? '项目模块'
                  : currentView === 'admin'
                    ? '用户与项目配置'
                    : selectedProject?.name
              }}
            </h2>
          </div>
          <div class="user-box">
            <span>{{ currentUser?.displayName }}</span>
            <button class="ghost-button" @click="logout">退出</button>
          </div>
        </header>

        <div v-if="currentView === 'projects'" class="content-area">
          <div class="section-title">
            <h3>可运维项目</h3>
            <p>这里只展示当前用户被授权访问的项目模块。</p>
          </div>
          <div class="project-grid">
            <article v-for="project in visibleProjects" :key="project.code" class="project-card">
              <span class="project-icon">{{ project.iconText }}</span>
              <div>
                <h4>{{ project.name }}</h4>
                <p>{{ project.description }}</p>
              </div>
              <button class="link-button" @click="openProject(project)">进入 →</button>
            </article>
          </div>
        </div>

        <div v-else-if="currentView === 'admin'" class="content-area admin-grid">
          <div class="admin-tabs">
            <button :class="{ active: adminTab === 'users' }" @click="adminTab = 'users'">用户</button>
            <button :class="{ active: adminTab === 'projects' }" @click="adminTab = 'projects'">项目模块</button>
            <button :class="{ active: adminTab === 'functions' }" @click="adminTab = 'functions'">功能树</button>
          </div>

          <section v-if="adminTab === 'users'" class="panel">
            <div class="section-title">
              <h3>用户体系</h3>
              <p>第一期采用用户直接授权项目，不引入角色。</p>
            </div>
            <div class="split-list">
              <div>
                <h4>永久用户</h4>
                <p v-for="user in permanentUsers" :key="user.username" class="list-row">
                  <span>{{ user.displayName }}</span>
                  <small>{{ user.username }} · {{ user.userType }}</small>
                </p>
              </div>
              <div>
                <h4>临时用户</h4>
                <p class="hint">默认有效期参数：{{ temporaryValidHours }} 小时，可在后续表单中自定义。</p>
                <input v-model="temporaryValidHours" min="1" type="number" />
                <p v-for="user in temporaryUsers" :key="user.username" class="list-row">
                  <span>{{ user.displayName }}</span>
                  <small>{{ user.username }} · 到期 {{ user.expiresAt }}</small>
                </p>
              </div>
            </div>
          </section>

          <section v-else-if="adminTab === 'projects'" class="panel">
            <div class="section-title">
              <h3>项目模块配置</h3>
              <p>admin 后续可在这里新增项目，并给用户分配项目权限。</p>
            </div>
            <div class="table-like">
              <div v-for="project in projects" :key="project.code" class="table-row">
                <strong>{{ project.name }}</strong>
                <span>{{ project.code }}</span>
                <small>{{ project.enabled ? '启用' : '禁用' }}</small>
              </div>
            </div>
          </section>

          <section v-else class="panel">
            <div class="section-title">
              <h3>功能树配置</h3>
              <p>功能树只控制项目内菜单结构，第一期不做节点级权限。</p>
            </div>
            <div class="tree-preview">
              <div v-for="project in projects" :key="project.code">
                <strong>{{ project.name }}</strong>
                <p v-for="node in project.functionNodes" :key="node.id">
                  {{ node.name }} · {{ node.nodeType }}
                  <small v-if="node.externalUrl"> · 外部链接</small>
                  <small v-if="node.ssoEnabled"> · SSO</small>
                </p>
              </div>
            </div>
          </section>
        </div>

        <div v-else class="project-workbench">
          <aside class="feature-sidebar">
            <button class="ghost-button" @click="backToProjects">← 返回项目</button>
            <h3>{{ selectedProject?.name }}</h3>
            <button v-for="node in selectedFunctions" :key="node.id" class="feature-item">
              {{ node.name }}
              <small>{{ node.nodeType }}</small>
            </button>
          </aside>
          <section class="workspace-panel">
            <p class="eyebrow">{{ selectedProject?.code }}</p>
            <h3>运维工作区骨架</h3>
            <p>这里后续承载 {{ selectedProject?.name }} 的具体运维工具。功能清单待你下次补充。</p>
          </section>
        </div>
      </section>
    </section>
  </main>
</template>

