import api from './axios'
import type { ApiResponse, ProductResponse } from '@/types/api'

export async function fetchProducts(params?: {
  categoryId?: number
  subCategoryId?: number
}) {
  const { data } = await api.get<ApiResponse<ProductResponse[]>>('/products', { params })
  return data
}

export async function fetchProductsInStock() {
  const { data } = await api.get<ApiResponse<ProductResponse[]>>('/products/in-stock')
  return data
}

export async function searchProducts(q: string) {
  const { data } = await api.get<ApiResponse<ProductResponse[]>>('/products/search', {
    params: { q },
  })
  return data
}

export async function fetchProduct(id: number) {
  const { data } = await api.get<ApiResponse<ProductResponse>>(`/products/${id}`)
  return data
}

export async function createProduct(form: FormData) {
  const { data } = await api.post<ApiResponse<null>>('/products', form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function updateProduct(id: number, form: FormData) {
  const { data } = await api.put<ApiResponse<null>>(`/products/${id}`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
  return data
}

export async function deleteProduct(id: number) {
  const { data } = await api.delete<ApiResponse<null>>(`/products/${id}`)
  return data
}
