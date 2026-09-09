import api from './axios'
import type {
  AddressDTO,
  AddressResponse,
  ApiResponse,
  CustomerDTO,
  CustomerResponse,
} from '@/types/api'

export async function fetchCustomers() {
  const { data } = await api.get<ApiResponse<CustomerResponse[]>>('/customers')
  return data
}

export async function searchCustomers(phone: string) {
  const { data } = await api.get<ApiResponse<CustomerResponse[]>>('/customers/search', {
    params: { phone },
  })
  return data
}

export async function fetchCustomer(id: number) {
  const { data } = await api.get<ApiResponse<CustomerResponse>>(`/customers/${id}`)
  return data
}

export async function createCustomer(dto: CustomerDTO) {
  const { data } = await api.post<ApiResponse<null>>('/customers', dto)
  return data
}

export async function updateCustomer(id: number, dto: CustomerDTO) {
  const { data } = await api.put<ApiResponse<null>>(`/customers/${id}`, dto)
  return data
}

export async function deleteCustomer(id: number) {
  const { data } = await api.delete<ApiResponse<null>>(`/customers/${id}`)
  return data
}

export async function restoreCustomer(id: number) {
  const { data } = await api.patch<ApiResponse<null>>(`/customers/${id}/restore`)
  return data
}

export async function fetchCustomerAddresses(customerId: number) {
  const { data } = await api.get<ApiResponse<AddressResponse[]>>(
    `/customers/${customerId}/addresses`,
  )
  return data
}

export async function createCustomerAddress(customerId: number, dto: AddressDTO) {
  const { data } = await api.post<ApiResponse<null>>(
    `/customers/${customerId}/addresses`,
    dto,
  )
  return data
}

export async function updateAddress(id: number, dto: AddressDTO) {
  const { data } = await api.put<ApiResponse<null>>(`/addresses/${id}`, dto)
  return data
}

export async function deleteAddress(id: number) {
  const { data } = await api.delete<ApiResponse<null>>(`/addresses/${id}`)
  return data
}
