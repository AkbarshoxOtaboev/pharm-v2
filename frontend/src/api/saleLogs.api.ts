import api from './axios'
import type { ApiResponse, SaleLogResponse } from '@/types/api'

export async function fetchSaleLogs(params?: { from?: string; to?: string }) {
  const { data } = await api.get<ApiResponse<SaleLogResponse[]>>('/sale-logs', { params })
  return data
}
