import api from './axios'
import type {
  ApiResponse,
  CategoryDTO,
  CategoryResponse,
  SubCategoryDTO,
  SubCategoryResponse,
} from '@/types/api'

export async function fetchCategories() {
  const { data } = await api.get<ApiResponse<CategoryResponse[]>>('/categories')
  return data
}

export async function fetchCategory(id: number) {
  const { data } = await api.get<ApiResponse<CategoryResponse>>(`/categories/${id}`)
  return data
}

export async function createCategory(dto: CategoryDTO) {
  const { data } = await api.post<ApiResponse<null>>('/categories', dto)
  return data
}

export async function updateCategory(id: number, dto: CategoryDTO) {
  const { data } = await api.put<ApiResponse<null>>(`/categories/${id}`, dto)
  return data
}

export async function deleteCategory(id: number) {
  const { data } = await api.delete<ApiResponse<null>>(`/categories/${id}`)
  return data
}

export async function fetchSubcategories(categoryId: number) {
  const { data } = await api.get<ApiResponse<SubCategoryResponse[]>>(
    `/categories/${categoryId}/subcategories`,
  )
  return data
}

export async function createSubcategory(categoryId: number, dto: SubCategoryDTO) {
  const { data } = await api.post<ApiResponse<null>>(
    `/categories/${categoryId}/subcategories`,
    dto,
  )
  return data
}

export async function updateSubcategory(id: number, dto: SubCategoryDTO) {
  const { data } = await api.put<ApiResponse<null>>(`/subcategories/${id}`, dto)
  return data
}

export async function deleteSubcategory(id: number) {
  const { data } = await api.delete<ApiResponse<null>>(`/subcategories/${id}`)
  return data
}
