import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import type { CartLine, Product } from '../types'

const CART_KEY = 'shopsphere.cart'
const WISHLIST_KEY = 'shopsphere.wishlist'
const MAX_PER_LINE = 10

function load<T>(key: string, fallback: T): T {
  try {
    const raw = localStorage.getItem(key)
    return raw ? (JSON.parse(raw) as T) : fallback
  } catch {
    return fallback
  }
}

/**
 * Cart and wishlist are client-owned: the backend models orders, not a persistent basket, so both
 * are persisted to localStorage. A stored basket can outlive a product being deactivated, so lines
 * are re-validated against the catalogue before checkout.
 */
export const useCartStore = defineStore('cart', () => {
  const lines = ref<CartLine[]>(load<CartLine[]>(CART_KEY, []))
  const wishlistIds = ref<number[]>(load<number[]>(WISHLIST_KEY, []))

  const count = computed(() => lines.value.reduce((sum, line) => sum + line.quantity, 0))
  const subtotal = computed(() =>
    lines.value.reduce((sum, line) => sum + line.product.price * line.quantity, 0),
  )
  const shipping = computed(() => (subtotal.value > 75 || subtotal.value === 0 ? 0 : 6.95))
  const tax = computed(() => Number((subtotal.value * 0.08).toFixed(2)))
  const total = computed(() => Number((subtotal.value + shipping.value + tax.value).toFixed(2)))
  const isEmpty = computed(() => lines.value.length === 0)
  const savings = computed(() =>
    lines.value.reduce((sum, line) => {
      const was = (line.product as Product & { oldPrice?: number }).oldPrice
      return was ? sum + (was - line.product.price) * line.quantity : sum
    }, 0),
  )

  watch(lines, (value) => localStorage.setItem(CART_KEY, JSON.stringify(value)), { deep: true })
  watch(wishlistIds, (value) => localStorage.setItem(WISHLIST_KEY, JSON.stringify(value)), {
    deep: true,
  })

  function quantityOf(productId: number): number {
    return lines.value.find((line) => line.product.id === productId)?.quantity ?? 0
  }

  function add(product: Product, amount = 1): void {
    const existing = lines.value.find((line) => line.product.id === product.id)
    if (existing) {
      existing.quantity = Math.min(existing.quantity + amount, MAX_PER_LINE)
      return
    }
    lines.value.push({ product, quantity: Math.min(Math.max(amount, 1), MAX_PER_LINE) })
  }

  function setQuantity(productId: number, quantity: number): void {
    const line = lines.value.find((item) => item.product.id === productId)
    if (!line) return
    if (quantity < 1) {
      remove(productId)
      return
    }
    line.quantity = Math.min(quantity, MAX_PER_LINE)
  }

  function changeQuantity(productId: number, delta: number): void {
    const line = lines.value.find((item) => item.product.id === productId)
    if (line) setQuantity(productId, line.quantity + delta)
  }

  function remove(productId: number): void {
    lines.value = lines.value.filter((line) => line.product.id !== productId)
  }

  function clear(): void {
    lines.value = []
  }

  function toggleWishlist(productId: number): boolean {
    const index = wishlistIds.value.indexOf(productId)
    if (index >= 0) {
      wishlistIds.value.splice(index, 1)
      return false
    }
    wishlistIds.value.push(productId)
    return true
  }

  function isWishlisted(productId: number): boolean {
    return wishlistIds.value.includes(productId)
  }

  /** Payload shape expected by order-service; it re-reads prices from the catalog. */
  function toOrderPayload(): Array<{ productId: number; quantity: number }> {
    return lines.value.map((line) => ({ productId: line.product.id, quantity: line.quantity }))
  }

  return {
    lines,
    wishlistIds,
    count,
    subtotal,
    shipping,
    tax,
    total,
    isEmpty,
    savings,
    quantityOf,
    add,
    setQuantity,
    changeQuantity,
    remove,
    clear,
    toggleWishlist,
    isWishlisted,
    toOrderPayload,
  }
})
