import { http } from './http'
import type { Page, Product } from '../types'

export interface ProductQuery {
  page?: number
  size?: number
  category?: string | null
  keyword?: string | null
  sort?: string
}

export interface ProductPayload {
  name: string
  description: string
  price: number
  category: string
  sku: string
  imageUrl?: string
  active: boolean
}

/**
 * The gateway only forwards reads to the catalog; every mutation is rejected before it reaches
 * product-service unless the caller is an ADMIN, so the UI shows admin affordances only for
 * admins but the server remains the authority.
 */
export const productService = {
  async list(query: ProductQuery = {}): Promise<Page<Product>> {
    const { data } = await http.get<Page<Product>>('/api/products', {
      params: {
        page: query.page ?? 0,
        size: query.size ?? 24,
        ...(query.category ? { category: query.category } : {}),
        ...(query.keyword ? { keyword: query.keyword } : {}),
        ...(query.sort ? { sort: query.sort } : {}),
      },
    })
    return data
  },

  async get(id: number): Promise<Product> {
    const { data } = await http.get<Product>(`/api/products/${id}`)
    return data
  },

  async create(payload: ProductPayload): Promise<Product> {
    const { data } = await http.post<Product>('/api/products', payload)
    return data
  },

  async update(id: number, payload: ProductPayload): Promise<Product> {
    const { data } = await http.put<Product>(`/api/products/${id}`, payload)
    return data
  },

  /** Deactivation is preferred over deletion so order history keeps its product reference. */
  async remove(id: number): Promise<void> {
    await http.delete(`/api/products/${id}`)
  },
}
