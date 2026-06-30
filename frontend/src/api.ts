export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export type UserType = 'ADMIN' | 'PERMANENT' | 'TEMPORARY'

export interface UserAccount {
  id: number
  username: string
  displayName: string
  userType: UserType
  expiresAt: string | null
  enabled: boolean
  projectCodes: string[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface ProjectModule {
  id: number
  code: string
  name: string
  description: string
  iconText: string
  sortOrder: number
  enabled: boolean
}

export interface LoginResult {
  user: UserAccount
  projects: ProjectModule[]
}

export interface UserRequest {
  username: string
  displayName: string
  enabled?: boolean
  validHours?: number | null
  projectCodes: string[]
}

export interface UserUpdateRequest {
  displayName: string
  enabled: boolean
  projectCodes: string[]
}

export interface UserPasswordUpdateRequest {
  newPassword: string
}

export interface TemporaryUserTimeExtendRequest {
  extendHours: number
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

export function listUsers(page = 1, pageSize = 10): Promise<PageResult<UserAccount>> {
  const query = new URLSearchParams({
    page: String(page),
    pageSize: String(pageSize)
  })
  return request<PageResult<UserAccount>>(`/api/user-admin/users?${query.toString()}`)
}

export function createPermanentUser(payload: UserRequest): Promise<UserAccount> {
  return request<UserAccount>('/api/user-admin/users/permanent', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function createTemporaryUser(payload: UserRequest): Promise<UserAccount> {
  return request<UserAccount>('/api/user-admin/users/temporary', {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function updateUser(username: string, payload: UserUpdateRequest): Promise<UserAccount> {
  return request<UserAccount>(`/api/user-admin/users/${encodeURIComponent(username)}`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function deleteUser(username: string): Promise<void> {
  return request<void>(`/api/user-admin/users/${encodeURIComponent(username)}/delete`, {
    method: 'POST'
  })
}

export function updateUserPassword(
  username: string,
  operatorUsername: string,
  payload: UserPasswordUpdateRequest
): Promise<void> {
  const query = `?operatorUsername=${encodeURIComponent(operatorUsername)}`
  return request<void>(`/api/user-admin/users/${encodeURIComponent(username)}/password${query}`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function extendTemporaryUserTime(
  username: string,
  payload: TemporaryUserTimeExtendRequest
): Promise<UserAccount> {
  return request<UserAccount>(`/api/user-admin/users/${encodeURIComponent(username)}/temporary-time`, {
    method: 'POST',
    body: JSON.stringify(payload)
  })
}

export function listProjects(username?: string): Promise<ProjectModule[]> {
  const query = username ? `?username=${encodeURIComponent(username)}` : ''
  return request<ProjectModule[]>(`/api/project-modules${query}`)
}
