import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { orderService } from '../services/orderService'
import { normalizeError } from '../services/http'
import type { Order, OrderStatus } from '../types'

export const useOrderStore = defineStore('orders', () => {
  const orders = ref<Order[]>([])
  const loading = ref(false)
  const submitting = ref(false)
  const error = ref('')
  const lastCreatedId = ref<number | null>(null)

  const sorted = computed(() =>
    [...orders.value].sort(
      (a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime(),
    ),
  )

  const counts = computed(() => {
    const base: Record<OrderStatus, number> = {
      PENDING: 0,
      CONFIRMED: 0,
      PAID: 0,
      PROCESSING: 0,
      SHIPPED: 0,
      DELIVERED: 0,
      CANCELLED: 0,
    }
    for (const order of orders.value) base[order.status] += 1
    return base
  })

  const totalOrders = computed(() => orders.value.length)
  const pendingOrders = computed(
    () => counts.value.PENDING + counts.value.CONFIRMED + counts.value.PROCESSING,
  )
  const completedOrders = computed(() => counts.value.DELIVERED)
  const lifetimeValue = computed(() =>
    orders.value
      .filter((order) => order.status !== 'CANCELLED')
      .reduce((sum, order) => sum + Number(order.totalAmount), 0),
  )

  async function fetchMine(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      orders.value = await orderService.mine()
    } catch (cause) {
      error.value = normalizeError(cause).message
      orders.value = []
    } finally {
      loading.value = false
    }
  }

  async function create(items: Array<{ productId: number; quantity: number }>): Promise<Order | null> {
    submitting.value = true
    error.value = ''
    try {
      const order = await orderService.create({ items })
      orders.value = [order, ...orders.value]
      lastCreatedId.value = order.id
      return order
    } catch (cause) {
      error.value = normalizeError(cause).message
      return null
    } finally {
      submitting.value = false
    }
  }

  async function fetchOne(id: number): Promise<Order | null> {
    const cached = orders.value.find((order) => order.id === id)
    if (cached) return cached
    loading.value = true
    try {
      return await orderService.get(id)
    } catch (cause) {
      error.value = normalizeError(cause).message
      return null
    } finally {
      loading.value = false
    }
  }

  async function cancel(id: number): Promise<void> {
    try {
      const updated = await orderService.cancel(id)
      replace(updated)
    } catch (cause) {
      error.value = normalizeError(cause).message
    }
  }

  async function ship(id: number): Promise<void> {
    try {
      const updated = await orderService.ship(id)
      replace(updated)
    } catch (cause) {
      error.value = normalizeError(cause).message
      throw cause
    }
  }

  function replace(order: Order): void {
    const index = orders.value.findIndex((item) => item.id === order.id)
    if (index >= 0) orders.value.splice(index, 1, order)
    else orders.value.unshift(order)
  }

  function byId(id: number): Order | undefined {
    return orders.value.find((order) => order.id === id)
  }

  return {
    orders,
    sorted,
    loading,
    submitting,
    error,
    counts,
    totalOrders,
    pendingOrders,
    completedOrders,
    lifetimeValue,
    lastCreatedId,
    fetchMine,
    create,
    fetchOne,
    cancel,
    ship,
    byId,
  }
})
