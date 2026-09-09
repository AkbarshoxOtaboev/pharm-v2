import api from './axios'
import type {
  ApiResponse,
  CourierStockDTO,
  CourierStockHistoryResponse,
  CourierStockResponse,
  ProductResponse,
  ReturnDTO,
} from '@/types/api'

export async function transferStock(dto: CourierStockDTO) {
  const { data } = await api.post<ApiResponse<null>>('/courier-stock/transfer', dto)
  return data
}

export async function returnStock(dto: ReturnDTO) {
  const { data } = await api.post<ApiResponse<null>>('/courier-stock/return', dto)
  return data
}

export async function fetchCourierStock(courierId: number) {
  const { data } = await api.get<ApiResponse<CourierStockResponse[]>>(
    `/courier-stock/${courierId}`,
  )
  return data
}

export async function fetchCourierStockHistory(
  courierId: number,
  params?: { from?: string; to?: string },
) {
  const { data } = await api.get<ApiResponse<CourierStockHistoryResponse[]>>(
    `/courier-stock/${courierId}/history`,
    { params },
  )
  return data
}

export async function searchCourierProducts(courierId: number, q: string) {
  const { data } = await api.get<ApiResponse<ProductResponse[]>>(
    '/courier-stock/products/search',
    { params: { courierId, q } },
  )
  return data
}
