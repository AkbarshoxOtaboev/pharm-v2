import api from './axios'
import type {
  ApiResponse,
  StoreDTO,
  StoreHistoryResponse,
  StoreResponse,
  StoreStatisticsResponse,
} from '@/types/api'

export async function fetchStore() {
  const { data } = await api.get<ApiResponse<StoreResponse[]>>('/store')
  return data
}

export async function fetchStoreItem(id: number) {
  const { data } = await api.get<ApiResponse<StoreResponse>>(`/store/${id}`)
  return data
}

export async function fetchStoreHistory(params?: { from?: string; to?: string }) {
  const { data } = await api.get<ApiResponse<StoreHistoryResponse[]>>('/store/history', {
    params,
  })
  return data
}

export async function fetchStoreStatistics() {
  const { data } = await api.get<ApiResponse<StoreStatisticsResponse>>('/store/statistics')
  return data
}

export async function addStoreArrival(storeId: number, dto: StoreDTO) {
  const { data } = await api.post<ApiResponse<null>>('/store/arrival', dto, {
    params: { storeId },
  })
  return data
}

export async function updateStoreArrival(id: number, dto: StoreDTO) {
  const { data } = await api.put<ApiResponse<null>>(`/store/${id}/arrival`, dto)
  return data
}
