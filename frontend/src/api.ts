export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export type UserType = 'ADMIN' | 'PERMANENT' | 'TEMPORARY'
export type FunctionNodeType = 'DIRECTORY' | 'MENU' | 'EXTERNAL_LINK' | 'SSO_LINK'

export interface UserAccount {
  id: number
  username: string
  displayName: string
  userType: UserType
  expiresAt: string | null
  enabled: boolean
  projectCodes: string[]
}

export interface FunctionNode {
  id: number
  parentId: number | null
  code: string
  name: string
  nodeType: FunctionNodeType
  routePath: string | null
  externalUrl: string | null
  ssoEnabled: boolean
  sortOrder: number
  enabled: boolean
}

export interface ProjectModule {
  id: number
  code: string
  name: string
  description: string
  iconText: string
  sortOrder: number
  enabled: boolean
  functionNodes: FunctionNode[]
}

export interface LoginResult {
  user: UserAccount
  projects: ProjectModule[]
}

export interface HealthStatus {
  status: string
  time: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

async function parseApiResponse<T>(response: Response): Promise<ApiResponse<T> | null> {
  const contentType = response.headers.get('Content-Type') ?? ''
  if (!contentType.includes('application/json')) {
    return null
  }
  try {
    return (await response.json()) as ApiResponse<T>
  } catch {
    return null
  }
}

async function request<T>(path: string, options?: RequestInit): Promise<T> {
  const response = await fetch(`${apiBaseUrl}${path}`, {
    headers: {
      'Content-Type': 'application/json',
      ...options?.headers
    },
    ...options
  })

  const result = await parseApiResponse<T>(response)
  if (!response.ok) {
    throw new Error(result?.message || `请求失败：${response.status}`)
  }

  if (!result) {
    throw new Error('接口返回格式错误')
  }
  if (!result.success) {
    throw new Error(result.message)
  }
  return result.data
}

export function login(username: string, password: string): Promise<LoginResult> {
  return request<LoginResult>('/api/auth/login', {
    method: 'POST',
    body: JSON.stringify({ username, password })
  })
}

export function listUsers(): Promise<UserAccount[]> {
  return request<UserAccount[]>('/api/ops-console/users')
}

export function listProjects(username?: string): Promise<ProjectModule[]> {
  const query = username ? `?username=${encodeURIComponent(username)}` : ''
  return request<ProjectModule[]>(`/api/ops-console/projects${query}`)
}

export function listFunctionNodes(projectCode: string): Promise<FunctionNode[]> {
  return request<FunctionNode[]>(`/api/ops-console/projects/${projectCode}/functions`)
}

export async function getHealthStatus(): Promise<ApiResponse<HealthStatus>> {
  const response = await fetch(`${apiBaseUrl}/api/health`)
  const result = await parseApiResponse<HealthStatus>(response)

  if (!response.ok) {
    throw new Error(result?.message || `请求失败：${response.status}`)
  }

  if (!result) {
    throw new Error('接口返回格式错误')
  }
  return result
}
