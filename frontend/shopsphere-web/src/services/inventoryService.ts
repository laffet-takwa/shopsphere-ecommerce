import { http } from './http'
import type { InventoryLevel } from '../types'

export interface StockUpdatePayload {
  sku: string
  quantity: number
}

export const inventoryService = {
  async forProduct(productId: number): Promise<InventoryLevel> {
    const { data } = await http.get<InventoryLevel>(`/api/inventory/${productId}`)
    return data
  },

  async update(productId: number, payload: StockUpdatePayload): Promise<InventoryLevel> {
    const { data } = await http.put<InventoryLevel>(`/api/inventory/${productId}`, payload)
    return data
  },

  async release(productId: number, orderId: number): Promise<void> {
    await http.post(`/api/inventory/release`, { productId, orderId })
  },
}
