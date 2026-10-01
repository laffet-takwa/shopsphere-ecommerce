<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { ArrowLeft, PackageCheck } from '@lucide/vue'
import { api } from '../api.ts'
import { useShopStore } from '../stores/shop.ts'

interface Order {
  id: number
  status: string
  totalAmount: number
  createdAt: string
  items: { productName: string; quantity: number; subtotal: number }[]
}

const shop = useShopStore()
const orders = ref<Order[]>([])
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  if (!shop.token) {
    error.value = 'Sign in to see your order history.'
    loading.value = false
    return
  }
  try {
    const { data } = await api.get('/api/orders')
    orders.value = data
  } catch {
    error.value = 'Your order history could not be loaded.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <main class="orders-page">
    <RouterLink class="back-link" to="/"><ArrowLeft :size="16" /> Back to the collection</RouterLink>
    <p class="eyebrow">YOUR ACCOUNT</p>
    <h1>Order <em>history.</em></h1>
    <p v-if="loading" class="orders-state">Gathering your orders…</p>
    <section v-else-if="error" class="orders-state"><p>{{ error }}</p><RouterLink class="hero-link" to="/">Return to the store <ArrowLeft :size="16" /></RouterLink></section>
    <section v-else-if="orders.length" class="orders-list">
      <article v-for="order in orders" :key="order.id" class="order-row">
        <div class="order-heading"><div><span class="product-category">ORDER #{{ order.id }}</span><h2>{{ new Date(order.createdAt).toLocaleDateString() }}</h2></div><span :class="['order-status', `status-${order.status.toLowerCase()}`]">{{ order.status }}</span></div>
        <div class="order-items"><p v-for="item in order.items" :key="`${order.id}-${item.productName}`">{{ item.quantity }} × {{ item.productName }} <span>${{ Number(item.subtotal).toFixed(2) }}</span></p></div>
        <div class="order-total"><span>Total</span><strong>${{ Number(order.totalAmount).toFixed(2) }}</strong></div>
      </article>
    </section>
    <section v-else class="orders-state"><PackageCheck :size="27" /><p>Nothing on the way just yet.</p><RouterLink class="hero-link" to="/">Find your everyday <ArrowLeft :size="16" /></RouterLink></section>
  </main>
</template>