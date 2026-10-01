import { ref } from 'vue'
import { defineStore } from 'pinia'

export type ToastTone = 'success' | 'error' | 'info'

export interface Toast {
  id: number
  message: string
  tone: ToastTone
}

let nextId = 1

/** Transient feedback (add-to-cart, saved, errors). Auto-dismisses so it never piles up. */
export const useUiStore = defineStore('ui', () => {
  const toasts = ref<Toast[]>([])
  const filterDrawerOpen = ref(false)
  const menuOpen = ref(false)
  const searchOpen = ref(false)

  function notify(message: string, tone: ToastTone = 'info', timeout = 3600): void {
    const id = nextId++
    toasts.value.push({ id, message, tone })
    window.setTimeout(() => dismiss(id), timeout)
  }

  const success = (message: string) => notify(message, 'success')
  const error = (message: string) => notify(message, 'error', 5200)

  function dismiss(id: number): void {
    toasts.value = toasts.value.filter((toast) => toast.id !== id)
  }

  function closeOverlays(): void {
    filterDrawerOpen.value = false
    menuOpen.value = false
    searchOpen.value = false
  }

  return {
    toasts,
    filterDrawerOpen,
    menuOpen,
    searchOpen,
    notify,
    success,
    error,
    dismiss,
    closeOverlays,
  }
})
