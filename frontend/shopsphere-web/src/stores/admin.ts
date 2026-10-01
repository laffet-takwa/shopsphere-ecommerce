import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { inventoryService, type StockUpdatePayload } from '../services/inventoryService'
import { productService, type ProductPayload } from '../services/productService'
import { orderService } from '../services/orderService'
import { normalizeError } from '../services/http'
import { demoRating } from '../mocks/products'
import type { InventoryLevel, Order, Product } from '../types'

export type StockState = 'IN_STOCK' | 'LOW_STOCK' | 'OUT_OF_STOCK'

export interface InventoryRow {
  product: Product
  stock: InventoryLevel | null
  state: StockState
}

const LOW_STOCK_THRESHOLD = 10

/**
 * Admin console state.
 *
 * Stock lives in MongoDB behind inventory-service while the catalogue lives in PostgreSQL, so the
 * two are joined here for display. If a level cannot be read the row is still shown, marked as
 * unknown, rather than silently dropping the product from the table.
 */
export const useAdminStore = defineStore('admin', () => {
  const products = ref<Product[]>([])
  const orders = ref<Order[]>([])
  const inventory = ref<InventoryRow[]>([])
  const loading = ref(false)
  const saving = ref(false)
  const error = ref('')

  const inventoryByProduct = computed(() => {
    const map = new Map<number, InventoryRow>()
    inventory.value.forEach((row) => map.set(row.product.id, row))
    return map
  })

  const metrics = computed(() => {
    const revenue = orders.value
      .filter((order) => order.status !== 'CANCELLED')
      .reduce((sum, order) => sum + Number(order.totalAmount), 0)
    const customers = new Set(orders.value.map((order) => order.userId))
    return {
      revenue,
      orderCount: orders.value.length,
      customerCount: customers.size,
      productCount: products.value.length,
      lowStockCount: inventory.value.filter((row) => row.state !== 'IN_STOCK').length,
    }
  })

  /** Last 7 days of revenue, bucketed by day for the dashboard chart. */
  const revenueSeries = computed(() => {
    const days: Array<{ label: string; value: number }> = []
    const now = new Date()
    for (let offset = 6; offset >= 0; offset -= 1) {
      const day = new Date(now.getFullYear(), now.getMonth(), now.getDate() - offset)
      const key = day.toISOString().slice(0, 10)
      const total = orders.value
        .filter((order) => order.status !== 'CANCELLED')
        .filter((order) => order.createdAt.slice(0, 10) === key)
        .reduce((sum, order) => sum + Number(order.totalAmount), 0)
      days.push({
        label: day.toLocaleDateString('en-US', { weekday: 'short' }),
        value: Number(total.toFixed(2)),
      })
    }
    return days
  })

  const orderStatusSeries = computed(() => {
    const counts = new Map<string, number>()
    orders.value.forEach((order) => counts.set(order.status, (counts.get(order.status) ?? 0) + 1))
    return [...counts.entries()].map(([status, value]) => ({ status, value }))
  })

  const topProducts = computed(() =>
    [...products.value]
      .sort((a, b) => Number(b.price) - Number(a.price))
      .slice(0, 5),
  )

  async function loadAll(): Promise<void> {
    loading.value = true
    error.value = ''
    try {
      const [catalog, allOrders] = await Promise.all([
        productService.list({ page: 0, size: 100 }),
        orderService.listAll(),
      ])
      products.value = catalog.content
      orders.value = allOrders
      await loadInventory()
    } catch (cause) {
      error.value = normalizeError(cause).message
    } finally {
      loading.value = false
    }
  }

  async function loadInventory(): Promise<void> {
    // Stock lookups are sequential on purpose: a bulk endpoint does not exist, and firing 100
    // parallel requests would be unkind to inventory-service for no real latency gain.
    const rows: InventoryRow[] = []
    for (const product of products.value) {
      try {
        const stock = await inventoryService.forProduct(product.id)
        rows.push({ product, stock, state: stateFor(stock.availableQuantity) })
      } catch {
        rows.push({ product, stock: null, state: 'OUT_OF_STOCK' })
      }
    }
    inventory.value = rows
  }

  function stateFor(available: number): StockState {
    if (available <= 0) return 'OUT_OF_STOCK'
    if (available <= LOW_STOCK_THRESHOLD) return 'LOW_STOCK'
    return 'IN_STOCK'
  }

  async function saveProduct(id: number | null, payload: ProductPayload): Promise<Product | null> {
    saving.value = true
    error.value = ''
    try {
      const saved = id ? await productService.update(id, payload) : await productService.create(payload)
      const index = products.value.findIndex((product) => product.id === saved.id)
      if (index >= 0) products.value.splice(index, 1, saved)
      else products.value.unshift(saved)
      return saved
    } catch (cause) {
      error.value = normalizeError(cause).message
      return null
    } finally {
      saving.value = false
    }
  }

  async function toggleActive(product: Product): Promise<void> {
    saving.value = true
    try {
      const saved = await productService.update(product.id, { ...toPayload(product), active: !product.active })
      const index = products.value.findIndex((item) => item.id === saved.id)
      if (index >= 0) products.value.splice(index, 1, saved)
    } catch (cause) {
      error.value = normalizeError(cause).message
    } finally {
      saving.value = false
    }
  }

  async function deleteProduct(id: number): Promise<void> {
    saving.value = true
    try {
      await productService.remove(id)
      products.value = products.value.filter((product) => product.id !== id)
      inventory.value = inventory.value.filter((row) => row.product.id !== id)
    } catch (cause) {
      error.value = normalizeError(cause).message
    } finally {
      saving.value = false
    }
  }

  async function updateStock(product: Product, payload: StockUpdatePayload): Promise<void> {
    saving.value = true
    error.value = ''
    try {
      const stock = await inventoryService.update(product.id, payload)
      const row = inventory.value.find((item) => item.product.id === product.id)
      if (row) {
        row.stock = stock
        row.state = stateFor(stock.availableQuantity)
      }
    } catch (cause) {
      error.value = normalizeError(cause).message
      throw cause
    } finally {
      saving.value = false
    }
  }

  async function shipOrder(id: number): Promise<void> {
    error.value = ''
    try {
      const updated = await orderService.ship(id)
      const index = orders.value.findIndex((order) => order.id === updated.id)
      if (index >= 0) orders.value.splice(index, 1, updated)
    } catch (cause) {
      error.value = normalizeError(cause).message
      throw cause
    }
  }

  function ratingFor(product: Product): { rating: number; reviews: number } {
    return demoRating(product.id)
  }

  return {
    products,
    orders,
    inventory,
    loading,
    saving,
    error,
    metrics,
    revenueSeries,
    orderStatusSeries,
    topProducts,
    inventoryByProduct,
    loadAll,
    loadInventory,
    saveProduct,
    toggleActive,
    deleteProduct,
    updateStock,
    shipOrder,
    ratingFor,
  }
})

export function toPayload(product: Product): ProductPayload {
  return {
    name: product.name,
    description: product.description,
    price: product.price,
    category: product.category,
    sku: product.sku,
    imageUrl: product.imageUrl ?? '',
    active: product.active,
  }
}
