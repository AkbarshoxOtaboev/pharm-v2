import api from './axios'
import type {
  ApiResponse,
  UserDto,
  UserResponse,
  UserStatResponse,
} from '@/types/api'

export async function fetchUsers() {
  const { data } = await api.get<ApiResponse<UserResponse[]>>('/users')
  return data
}

export async function fetchCouriers() {
  const { data } = await api.get<ApiResponse<UserResponse[]>>('/users/couriers')
  return data
}

export async function fetchUser(username: string) {
  const { data } = await api.get<ApiResponse<UserResponse>>(`/users/${username}`)
  return data
}

export async function fetchUserStats() {
  const { data } = await api.get<ApiResponse<UserStatResponse>>('/users/statistics')
  return data
}

export async function checkUsername(username: string) {
  const { data } = await api.get<ApiResponse<{ exists: boolean }>>(
    '/users/check-username',
    { params: { username } },
  )
  return data
}

export async function createUser(dto: UserDto) {
  const { data } = await api.post<ApiResponse<null>>('/users', dto)
  return data
}

export async function updateUser(username: string, dto: UserDto) {
  const { data } = await api.put<ApiResponse<null>>(`/users/${username}`, dto)
  return data
}

export async function deleteUser(username: string) {
  const { data } = await api.delete<ApiResponse<null>>(`/users/${username}`)
  return data
}

export async function blockUser(username: string) {
  const { data } = await api.patch<ApiResponse<null>>(`/users/${username}/block`)
  return data
}

export async function unblockUser(username: string) {
  const { data } = await api.patch<ApiResponse<null>>(`/users/${username}/unblock`)
  return data
}
