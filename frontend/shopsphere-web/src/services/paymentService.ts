import { http } from './http'
import type { Payment } from '../types'

/**
 * Payment is simulated end to end: payment-service only ever publishes a result after inventory
 * has been reserved, so this endpoint reports an existing decision rather than taking a new one.
 */
export const paymentService = {
  async forOrder(orderId: number): Promise<Payment> {
    const { data } = await http.get<Payment>(`/api/payments/${orderId}`)
    return data
  },

  async status(orderId: number): Promise<Payment> {
    const { data } = await http.post<Payment>('/api/payments/process', { orderId })
    return data
  },
}
