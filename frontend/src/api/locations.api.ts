import api from './axios'
import type { ApiResponse } from '@/types/api'

export interface RegionItem {
  id: number
  name: string
}

export interface DistrictItem {
  id: number
  name: string
  regionId: number
  regionName?: string
}

export async function fetchRegions() {
  const { data } = await api.get<ApiResponse<RegionItem[]>>('/locations/regions')
  return data
}

export async function createRegion(name: string) {
  const { data } = await api.post<ApiResponse<RegionItem>>('/locations/regions', { name })
  return data
}

export async function deleteRegion(id: number) {
  const { data } = await api.delete<ApiResponse<null>>(`/locations/regions/${id}`)
  return data
}

export async function fetchDistricts(regionId?: number) {
  const { data } = await api.get<ApiResponse<DistrictItem[]>>('/locations/districts', {
    params: regionId ? { regionId } : undefined,
  })
  return data
}

export async function createDistrict(name: string, regionId: number) {
  const { data } = await api.post<ApiResponse<DistrictItem>>('/locations/districts', {
    name,
    parentId: regionId,
  })
  return data
}

export async function deleteDistrict(id: number) {
  const { data } = await api.delete<ApiResponse<null>>(`/locations/districts/${id}`)
  return data
}
