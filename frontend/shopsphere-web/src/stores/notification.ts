import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { notificationService } from '../services/notificationService'
import { normalizeError } from '../services/http'
import type { Notification } from '../types'

export type NotificationTone = 'order' | 'payment' | 'promotion' | 'system'

const TONE_BY_TOPIC: Record<string, NotificationTone> = {
  'order.created': 'order',
  'order.shipped': 'order',
  'inventory.reserved': 'order',
  'inventory.insufficient': 'order',
  'payment.completed': 'payment',
  'payment.failed': 'payment',
}

/** Notification feed, mirrored from the topics the notification service consumes. */
export const useNotificationStore = defineStore('notifications', () => {
  const items = ref<Notification[]>([])
  const loading = ref(false)
  const error = ref('')

  const unreadCount = computed(() => items.value.filter((item) => !item.read).length)
  const recent = computed(() =>
    [...items.value].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
  )

  function toneFor(type: string): NotificationTone {
    return TONE_BY_TOPIC[type] ?? 'system'
  }

  async function fetchAll(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      items.value = await notificationService.list()
    } catch (cause) {
      error.value = normalizeError(cause).message
      items.value = []
    } finally {
      loading.value = false
    }
  }

  async function markRead(id: string): Promise<void> {
    const target = items.value.find((item) => item.id === id)
    if (!target || target.read) return
    // Optimistic: the badge should respond immediately, and a failure is reverted below.
    target.read = true
    try {
      await notificationService.markRead(id)
    } catch (cause) {
      target.read = false
      error.value = normalizeError(cause).message
    }
  }

  async function markAllRead(): Promise<void> {
    const unread = items.value.filter((item) => !item.read)
    unread.forEach((item) => {
      item.read = true
    })
    try {
      await Promise.all(unread.map((item) => notificationService.markRead(item.id)))
    } catch (cause) {
      unread.forEach((item) => {
        item.read = false
      })
      error.value = normalizeError(cause).message
    }
  }

  return { items, recent, loading, error, unreadCount, toneFor, fetchAll, markRead, markAllRead }
})
