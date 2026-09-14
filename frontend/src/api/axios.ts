import axios, { type AxiosError, type InternalAxiosRequestConfig } from 'axios'
import { isSessionIdle, touchActivity } from '@/utils/sessionIdle'

const baseURL = import.meta.env.VITE_API_BASE_URL || '/api/v1'

/** Bare client — no auth interceptors (used for refresh to avoid loops). */
export const plainApi = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

const api = axios.create({
  baseURL,
  headers: { 'Content-Type': 'application/json' },
})

type RetriableConfig = InternalAxiosRequestConfig & { _retry?: boolean }

let refreshPromise: Promise<boolean> | null = null

async function tryRefreshTokens(): Promise<boolean> {
  const { useAuthStore } = await import('@/stores/auth.store')
  const auth = useAuthStore()
  return auth.refreshTokens()
}

function redirectToLogin() {
  void import('@/stores/auth.store').then(({ useAuthStore }) => {
    const auth = useAuthStore()
    void auth.clearSession().then(() => {
      if (!window.location.pathname.includes('/login')) {
        window.location.href = '/login'
      }
    })
  })
}

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  // Any API call counts as activity while the tab is used
  if (token) touchActivity()
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const status = error.response?.status
    const original = error.config as RetriableConfig | undefined
    const url = original?.url || ''

    const isAuthEndpoint =
      url.includes('/auth/login') ||
      url.includes('/auth/refresh') ||
      url.includes('/auth/logout')

    if (status !== 401 || !original || original._retry || isAuthEndpoint) {
      if (status === 401 && !isAuthEndpoint) {
        redirectToLogin()
      }
      return Promise.reject(error)
    }

    // Idle + expired access → logout (do not refresh)
    if (isSessionIdle()) {
      redirectToLogin()
      return Promise.reject(error)
    }

    // Active session → refresh once, then retry
    original._retry = true
    try {
      if (!refreshPromise) {
        refreshPromise = tryRefreshTokens().finally(() => {
          refreshPromise = null
        })
      }
      const ok = await refreshPromise
      if (!ok) {
        redirectToLogin()
        return Promise.reject(error)
      }
      const token = localStorage.getItem('accessToken')
      if (token) {
        original.headers.Authorization = `Bearer ${token}`
      }
      return api(original)
    } catch (e) {
      redirectToLogin()
      return Promise.reject(e)
    }
  },
)

export default api
