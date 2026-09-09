import api from './axios'
import type { ApiResponse, CashRegisterResponse, DailyCashDTO } from '@/types/api'

export async function fetchCourierCash(courierId: number) {
  const { data } = await api.get<ApiResponse<CashRegisterResponse[]>>(
    `/cash-register/courier/${courierId}`,
  )
  return data
}

export async function fetchCourierCashTotal(courierId: number) {
  const { data } = await api.get<ApiResponse<{ total?: number } | number>>(
    `/cash-register/courier/${courierId}/total`,
  )
  return data
}

export async function returnCash(id: number, comment?: string) {
  const { data } = await api.patch<ApiResponse<null>>(
    `/cash-register/${id}/return`,
    comment ? { comment } : {},
  )
  return data
}

export async function fetchDailyCashReport(params?: {
  from?: string
  to?: string
  date?: string
}) {
  const { data } = await api.get<ApiResponse<DailyCashDTO[]>>('/cash-register/daily-report', {
    params,
  })
  return data
}
