export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface HealthStatus {
  status: string
  time: string
}

const apiBaseUrl = import.meta.env.VITE_API_BASE_URL ?? ''

export async function getHealthStatus(): Promise<ApiResponse<HealthStatus>> {
  const response = await fetch(`${apiBaseUrl}/api/health`)

  if (!response.ok) {
    throw new Error(`请求失败：${response.status}`)
  }

  return response.json()
}

