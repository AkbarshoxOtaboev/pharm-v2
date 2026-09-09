import api from './axios'
import type {
  ApiResponse,
  DailyStatDTO,
  OrderDTO,
  OrderDetailResponse,
  OrderResponse,
  OrderStatResponse,
  OrderStatus,
} from '@/types/api'

export async function fetchDashboardStats() {
  const { data } = await api.get<ApiResponse<OrderStatResponse>>('/statistics/dashboard')
  return data
}

export async function fetchDailyStats() {
  const { data } = await api.get<ApiResponse<DailyStatDTO[]>>('/statistics/daily')
  return data
}

export async function fetchMonthlySales(year?: number) {
  const { data } = await api.get<ApiResponse<number[]>>('/statistics/monthly-sales', {
    params: year ? { year } : undefined,
  })
  return data
}

export interface OrderListParams {
  id?: number
  phone?: string
  status?: OrderStatus
  userId?: number
  fromDate?: string
  toDate?: string
  page?: number
  size?: number
}

export async function fetchOrders(params: OrderListParams = {}) {
  const { data } = await api.get<ApiResponse<OrderResponse[]>>('/orders', { params })
  return data
}

export async function fetchOrder(id: number) {
  const { data } = await api.get<ApiResponse<OrderDetailResponse>>(`/orders/${id}`)
  return data
}

export async function createOrder(dto: OrderDTO) {
  const { data } = await api.post<ApiResponse<null>>('/orders', dto)
  return data
}

export async function updateOrder(id: number, dto: OrderDTO) {
  const { data } = await api.put<ApiResponse<null>>(`/orders/${id}`, dto)
  return data
}

export async function updateOrderStatus(id: number, status: string) {
  const { data } = await api.patch<ApiResponse<null>>(`/orders/${id}/status`, { status })
  return data
}
