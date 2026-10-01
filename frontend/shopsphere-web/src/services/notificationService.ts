import { http } from './http'
import type { Notification } from '../types'

export const notificationService = {
  async list(): Promise<Notification[]> {
    const { data } = await http.get<Notification[]>('/api/notifications')
    return data
  },

  async markRead(id: string): Promise<Notification> {
    const { data } = await http.put<Notification>(`/api/notifications/${id}/read`)
    return data
  },
}
