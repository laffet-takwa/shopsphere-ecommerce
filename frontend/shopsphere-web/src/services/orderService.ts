import { http } from './http'
import type { Order, OrderStatus } from '../types'

export interface CreateOrderPayload {
  items: Array<{ productId: number; quantity: number }>
}

export const orderService = {
  /** The gateway fills X-User-Id from the verified token, so no user id is sent here. */
  async create(payload: CreateOrderPayload): Promise<Order> {
    const { data } = await http.post<Order>('/api/orders', payload)
    return data
  },

  async mine(): Promise<Order[]> {
    const { data } = await http.get<Order[]>('/api/orders')
    return data
  },

  async get(id: number): Promise<Order> {
    const { data } = await http.get<Order>(`/api/orders/${id}`)
    return data
  },

  async cancel(id: number): Promise<Order> {
    const { data } = await http.put<Order>(`/api/orders/${id}/cancel`)
    return data
  },

  /** ADMIN-only in both the gateway and order-service. */
  async ship(id: number): Promise<Order> {
    const { data } = await http.put<Order>(`/api/orders/${id}/ship`)
    return data
  },

  async listAll(): Promise<Order[]> {
    const { data } = await http.get<Order[]>('/api/orders')
    return data
  },
}

export type { OrderStatus }
