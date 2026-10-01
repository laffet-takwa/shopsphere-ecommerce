/** Shared domain types. These mirror the backend DTOs exactly. */

export interface Product {
  id: number
  name: string
  description: string
  price: number
  category: string
  sku: string
  imageUrl?: string | null
  active: boolean
  createdAt?: string
  updatedAt?: string
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'PAID'
  | 'PROCESSING'
  | 'SHIPPED'
  | 'DELIVERED'
  | 'CANCELLED'

export interface OrderItem {
  productId: number
  productName: string
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface Order {
  id: number
  userId: number
  status: OrderStatus
  totalAmount: number
  createdAt: string
  updatedAt: string
  items: OrderItem[]
}

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'FAILED'

export interface Payment {
  id: number
  orderId: number
  userId: number
  amount: number
  currency: string
  status: PaymentStatus
  transactionReference: string
  createdAt: string
}

export interface InventoryLevel {
  productId: number
  sku: string
  quantity: number
  reservedQuantity: number
  availableQuantity: number
  updatedAt: string
}

export interface Notification {
  id: string
  userId: number
  type: string
  message: string
  createdAt: string
  read: boolean
}

export type UserRole = 'CUSTOMER' | 'ADMIN'

export interface User {
  id: number
  firstName: string
  lastName: string
  email: string
  role: UserRole
  createdAt: string
}

export interface AuthResponse {
  accessToken: string
  tokenType: string
  user: User
}

export interface CartLine {
  product: Product
  quantity: number
}

/** Catalogue facets used by the filter sidebar. */
export interface ProductFilters {
  category: string | null
  keyword: string | null
  minPrice: number | null
  maxPrice: number | null
  minRating: number | null
  inStockOnly: boolean
  sort: SortOption
}

export type SortOption = 'featured' | 'newest' | 'price-asc' | 'price-desc' | 'rating'

export interface CategorySummary {
  name: string
  slug: string
  productCount: number
  imageUrl: string
  blurb: string
}
