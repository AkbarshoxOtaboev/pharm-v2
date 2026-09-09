export type Role = 'ADMIN' | 'OPERATOR' | 'COURIER' | 'VIEWER'
export type Status = 'ACTIVE' | 'INACTIVE' | 'BLOCKED' | 'DELETED' | string
export type OrderStatus =
  | 'NEW'
  | 'CONFIRMED'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'PENDING'
  | 'DELIVERED'
  | 'CANCELLED'
  | 'DELETED'
  | string
export type PaymentType = 'CASH' | 'CARD' | string
export type Gender = 'MALE' | 'FEMALE' | 'NOT_SELECTED' | string
export type CustomerType = 'B2C' | 'B2B' | string
export type UnitType = 'PCS' | 'BOX' | 'KG' | string

export interface ApiResponse<T> {
  success: boolean
  data: T
  meta?: {
    page: number
    size: number
    totalElements: number
    totalPages: number
  }
  error?: {
    code: string
    message: string
  }
}

export interface AuthResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  userId: number
  username: string
  fullName: string
  avatar?: string
  role: Role
}

export interface MeResponse {
  id: number
  username: string
  fullName: string
  personalPhone?: string
  workPhone?: string
  avatar?: string
  role: Role
  status: string
}

export interface UserResponse {
  id: number
  fullName: string
  username: string
  personalPhone?: string
  workPhone?: string
  avatar?: string
  role: Role
  status: Status
}

export interface UserDto {
  fullName: string
  username: string
  password?: string
  personalPhone?: string
  workPhone?: string
  avatar?: string
  role: Role
}

export interface UserStatResponse {
  adminCount?: number
  operatorCount?: number
  courierCount?: number
}

export interface OrderStatResponse {
  clientsCount?: number
  ordersCount?: number
  deliveredOrdersCount?: number
  cancelledOrdersCount?: number
  dailyDeliveredOrdersTotalSum?: number
  monthlyDeliveredOrdersTotalSum?: number
}

export interface DailyStatDTO {
  date?: string
  ordersCount?: number
  deliveredCount?: number
  cancelledCount?: number
  deliveredSum?: number
  cancelledSum?: number
  deliveredBonusSum?: number
}

export interface OrderResponse {
  id: number
  orderNumber?: string
  customerName?: string
  address?: string
  customerPhone?: string
  totalSum?: number
  totalBonusSum?: number
  orderStatus?: OrderStatus
  createdAt?: string
  updatedAt?: string
  courierName?: string
}

export interface OrderItemResponse {
  productId?: number
  productName?: string
  quantity?: number
  productCost?: number
  isBonus?: boolean
}

export interface OrderDetailResponse {
  orderId: number
  fullName?: string
  phone?: string
  orderNumber?: string
  createdDate?: string
  address?: string
  house?: string
  entrance?: string
  apartment?: string
  floor?: string
  orientation?: string
  status?: OrderStatus
  totalAmount?: number
  courier?: UserResponse
  items?: OrderItemResponse[]
}

export interface OrderItemDTO {
  productId: number
  quantity: number
  productCost?: number
  isBonus?: boolean
}

export interface OrderDTO {
  customerId?: number
  fullName?: string
  phone?: string
  address?: string
  home?: string
  entrance?: string
  apartment?: string
  floor?: string
  orientations?: string
  paymentType?: PaymentType
  courierId?: number
  items: OrderItemDTO[]
}

export interface CustomerResponse {
  id: number
  fullName: string
  phone: string
  birthDate?: string
  description?: string
  gender?: Gender
  type?: CustomerType
  status?: Status
  createdAt?: string
}

export interface CustomerDTO {
  fullName: string
  phone: string
  birthDate?: string
  description?: string
  gender?: Gender
  type?: CustomerType
}

export interface AddressDTO {
  fullAddress: string
  home?: string
  entrance?: string
  apartment?: string
  floor?: string
  orientation?: string
  latitude?: number
  longitude?: number
}

export interface AddressResponse {
  id: number
  fullAddress?: string
  home?: string
  entrance?: string
  apartment?: string
  floor?: string
  orientation?: string
  latitude?: number
  longitude?: number
  status?: Status
}

export interface CategoryResponse {
  id: number
  name: string
  description?: string
  subcategoryCount?: number
  status?: Status
}

export interface CategoryDTO {
  id?: number
  name: string
  description?: string
}

export interface SubCategoryResponse {
  id: number
  name: string
  description?: string
  status?: Status
}

export interface SubCategoryDTO {
  id?: number
  name: string
  description?: string
  categoryId?: number
}

export interface ProductResponse {
  id: number
  name: string
  price?: number
  priceCost?: number
  categoryResponse?: CategoryResponse
  subCategoryId?: number
  sortNumber?: number
  status?: Status
  description?: string
  photo?: string
  unitType?: UnitType
  storeQuantity?: number
}

export interface StoreResponse {
  storeId: number
  productName?: string
  productPrice?: number
  productPriceCost?: number
  productUnitType?: UnitType
  storeQuantity?: number
  storeTotalAmount?: number
  storeDateOfArrival?: string
  storeStatus?: Status
}

export interface StoreDTO {
  quantity: number
  dateOfArrival?: string
  comment?: string
}

export interface StoreStatisticsResponse {
  totalProducts?: number
  totalQuantity?: number
  totalCostSum?: number
  todayAddedQuantity?: number
  [key: string]: unknown
}

export interface StoreHistoryResponse {
  id?: number
  productName?: string
  quantity?: number
  comment?: string
  createdAt?: string
  [key: string]: unknown
}

export interface CourierStockResponse {
  id: number
  productId: number
  productName?: string
  quantity?: number
  productPrice?: number
  totalAmount?: number
  unitType?: UnitType
}

export interface CourierStockDTO {
  courierId: number
  items: { productId: number; quantity: number }[]
}

export interface ReturnDTO {
  courierId: number
  productId: number
  quantity: number
}

export interface CourierStockHistoryResponse {
  id?: number
  productName?: string
  quantity?: number
  createdAt?: string
  [key: string]: unknown
}

export interface CashRegisterResponse {
  id: number
  courierName?: string
  orderId?: number
  orderTotalSum?: number
  registerStatus?: string
  createdAt?: string
  dateTime?: string
  comment?: string
}

export interface DailyCashDTO {
  date?: string
  total?: number
  totalSum?: number
  [key: string]: unknown
}

export interface SaleLogResponse {
  id?: number
  orderId?: number
  categoryId?: number
  categoryName?: string
  customerName?: string
  customerPhone?: string
  customerAddress?: string
  productName?: string
  productPriceCost?: number
  quantity?: number
  totalSum?: number
  isBonus?: boolean
  createdAt?: string
  updatedAt?: string
  [key: string]: unknown
}
