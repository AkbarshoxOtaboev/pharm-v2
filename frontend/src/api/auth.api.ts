import api, { plainApi } from './axios'
import type { ApiResponse, AuthResponse, MeResponse } from '@/types/api'

export async function login(username: string, password: string) {
  const { data } = await api.post<ApiResponse<AuthResponse>>('/auth/login', {
    username,
    password,
  })
  return data
}

export async function fetchMe() {
  const { data } = await api.get<ApiResponse<MeResponse>>('/auth/me')
  return data
}

export async function updateProfile(payload: {
  fullName?: string
  personalPhone?: string
  workPhone?: string
  avatar?: string
}) {
  const { data } = await api.put<ApiResponse<MeResponse>>('/auth/me', payload)
  return data
}

export async function changePassword(payload: {
  currentPassword: string
  newPassword: string
}) {
  const { data } = await api.put<ApiResponse<null>>('/auth/me/password', payload)
  return data
}

export async function logout() {
  await api.post('/auth/logout')
}

/** Uses plainApi so 401 interceptor does not recurse. */
export async function refresh(refreshToken: string) {
  const { data } = await plainApi.post<ApiResponse<AuthResponse>>(
    `/auth/refresh?refreshToken=${encodeURIComponent(refreshToken)}`,
  )
  return data
}

export async function uploadAvatar(file: File) {
  const form = new FormData()
  form.append('file', file)
  const { data } = await api.post<ApiResponse<{ filename: string; url: string }>>(
    '/files/upload',
    form,
    { headers: { 'Content-Type': 'multipart/form-data' } },
  )
  return data
}
