import { computed, ref, watch } from 'vue'
import { defineStore } from 'pinia'
import type { CartLine } from '../api'

function loadCart(): CartLine[] {
  try { return JSON.parse(localStorage.getItem('shopsphere-cart') ?? '[]') as CartLine[] }
  catch { return [] }
}

export const useShopStore = defineStore('shop', () => {
  const cart = ref<CartLine[]>(loadCart())
  const token = ref(localStorage.getItem('shopsphere-token'))
  const userName = ref(localStorage.getItem('shopsphere-name') ?? '')
  const cartCount = computed(() => cart.value.reduce((sum, line) => sum + line.quantity, 0))
  const cartTotal = computed(() => cart.value.reduce((sum, line) => sum + line.product.price * line.quantity, 0))

  watch(cart, (value) => localStorage.setItem('shopsphere-cart', JSON.stringify(value)), { deep: true })

  function addToCart(product: CartLine['product']) {
    const existing = cart.value.find((line) => line.product.id === product.id)
    if (existing) existing.quantity += 1
    else cart.value.push({ product, quantity: 1 })
  }
  function changeQuantity(productId: number, change: number) {
    const line = cart.value.find((item) => item.product.id === productId)
    if (!line) return
    line.quantity += change
    if (line.quantity < 1) cart.value = cart.value.filter((item) => item.product.id !== productId)
  }
  function setSession(accessToken: string, name: string) {
    token.value = accessToken
    userName.value = name
    localStorage.setItem('shopsphere-token', accessToken)
    localStorage.setItem('shopsphere-name', name)
  }
  function signOut() {
    token.value = null
    userName.value = ''
    localStorage.removeItem('shopsphere-token')
    localStorage.removeItem('shopsphere-name')
  }

  return { cart, token, userName, cartCount, cartTotal, addToCart, changeQuantity, setSession, signOut }
})