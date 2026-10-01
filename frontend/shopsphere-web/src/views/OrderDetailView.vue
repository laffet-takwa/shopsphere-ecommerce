<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRoute } from 'vue-router'
import { MapPin, CreditCard, Package, ArrowLeft } from '@lucide/vue'
import OrderStatusBadge from '../components/order/OrderStatusBadge.vue'
import OrderTimeline from '../components/order/OrderTimeline.vue'
import BaseButton from '../components/common/BaseButton.vue'
import SkeletonBlock from '../components/common/SkeletonBlock.vue'
import { useOrderStore } from '../stores/order'
import { imageFor } from '../mocks/products'
import type { Order } from '../types'

const route = useRoute()
const orders = useOrderStore()

const order = ref<Order | null>(null)
const loading = ref(true)

const address = computed(() => {
  if (!order.value) return null
  const saved = localStorage.getItem('shopsphere.checkout.address')
  if (saved) {
    try {
      return JSON.parse(saved) as { firstName: string; lastName: string; address: string; city: string; postalCode: string; country: string }
    } catch {
      /* fall through to the placeholder below */
    }
  }
  return null
})

const deliveryEstimate = computed(() => {
  if (!order.value) return ''
  const created = new Date(order.value.createdAt)
  created.setDate(created.getDate() + 5)
  return created.toLocaleDateString('en-US', { month: 'long', day: 'numeric', year: 'numeric' })
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (Number.isFinite(id)) order.value = await orders.fetchOne(id)
  loading.value = false
})

function formatDate(value: string): string {
  return new Date(value).toLocaleString('en-US', {
    month: 'short',
    day: 'numeric',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
</script>

<template>
  <div class="page container">
    <BaseButton to="/orders" variant="ghost" size="sm" class="page__back">
      <ArrowLeft :size="16" aria-hidden="true" /> Back to orders
    </BaseButton>

    <div v-if="loading" class="layout">
      <SkeletonBlock height="320px" radius="var(--radius-lg)" />
      <SkeletonBlock height="320px" radius="var(--radius-lg)" />
    </div>

    <div v-else-if="!order" class="card missing">
      <h1 class="missing__title">Order not found</h1>
      <p class="missing__copy">We could not load this order. It may belong to another account.</p>
      <BaseButton to="/orders" variant="primary">View your orders</BaseButton>
    </div>

    <template v-else>
      <header class="head">
        <div>
          <p class="eyebrow">Order #{{ order.id }}</p>
          <h1 class="head__title">Placed {{ formatDate(order.createdAt) }}</h1>
        </div>
        <OrderStatusBadge :status="order.status" />
      </header>

      <div class="layout">
        <section class="card card--pad">
          <h2 class="section__title">Progress</h2>
          <OrderTimeline :status="order.status" />

          <div class="facts">
            <div class="fact">
              <Package :size="17" aria-hidden="true" />
              <div>
                <p class="fact__label">Items</p>
                <p class="fact__value">{{ order.items.reduce((sum, item) => sum + item.quantity, 0) }}</p>
              </div>
            </div>
            <div class="fact">
              <CreditCard :size="17" aria-hidden="true" />
              <div>
                <p class="fact__label">Payment status</p>
                <p class="fact__value">{{ order.status === 'CANCELLED' ? 'Refunded' : 'Confirmed' }}</p>
              </div>
            </div>
            <div class="fact">
              <MapPin :size="17" aria-hidden="true" />
              <div>
                <p class="fact__label">Estimated delivery</p>
                <p class="fact__value">{{ order.status === 'CANCELLED' ? '—' : deliveryEstimate }}</p>
              </div>
            </div>
          </div>
        </section>

        <div class="side">
          <section class="card card--pad">
            <h2 class="section__title">Shipping address</h2>
            <address v-if="address" class="address">
              {{ address.firstName }} {{ address.lastName }}<br />
              {{ address.address }}<br />
              {{ address.city }}, {{ address.postalCode }}<br />
              {{ address.country }}
            </address>
            <p v-else class="address address--empty">
              Saved addresses are not persisted by this demo. The shipping form is captured at
              checkout and shown here for the current session.
            </p>
          </section>

          <section class="card card--pad">
            <h2 class="section__title">Payment</h2>
            <p class="payment">
              <span class="payment__brand">Card</span>
              <span>Simulated · no real charge</span>
            </p>
            <p class="payment__note">Transaction reference is issued once inventory is reserved.</p>
          </section>
        </div>
      </div>

      <section class="card card--pad items">
        <h2 class="section__title">Items in this order</h2>
        <ul>
          <li v-for="item in order.items" :key="item.productId" class="item">
            <RouterLink
              class="item__media"
              :to="{ name: 'product-detail', params: { id: item.productId } }"
              :aria-label="`View ${item.productName}`"
            >
              <img :src="imageFor({ id: item.productId, category: '' })" :alt="item.productName" width="120" height="120" loading="lazy" />
            </RouterLink>
            <div class="item__body">
              <RouterLink class="item__name" :to="{ name: 'product-detail', params: { id: item.productId } }">
                {{ item.productName }}
              </RouterLink>
              <p class="item__meta">{{ item.quantity }} × {{ item.unitPrice.toFixed(2) }}</p>
            </div>
            <span class="price">{{ item.subtotal.toFixed(2) }}</span>
          </li>
        </ul>
        <div class="totals">
          <div class="totals__row">
            <span>Total</span>
            <span class="price price--lg">{{ order.totalAmount.toFixed(2) }}</span>
          </div>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.page {
  padding-block: var(--space-6) var(--space-16);
}

.page__back {
  margin-bottom: var(--space-5);
}

.head {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.head__title {
  font-size: clamp(1.4rem, 3vw, var(--text-2xl));
  margin-top: var(--space-2);
}

.layout {
  display: grid;
  gap: var(--space-5);
  align-items: start;
}

.section__title {
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-5);
}

.side {
  display: flex;
  flex-direction: column;
  gap: var(--space-5);
}

.facts {
  display: grid;
  gap: var(--space-4);
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border);
}

.fact {
  display: flex;
  gap: var(--space-3);
  align-items: flex-start;
}

.fact svg {
  color: var(--color-muted);
  flex-shrink: 0;
  margin-top: 2px;
}

.fact__label {
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: var(--tracking-wide);
  color: var(--color-muted);
}

.fact__value {
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--color-text);
}

.address {
  font-style: normal;
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  line-height: var(--leading-normal);
}

.address--empty {
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.payment {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-base);
  color: var(--color-text-secondary);
}

.payment__brand {
  padding: 3px var(--space-3);
  border-radius: var(--radius-sm);
  background: var(--color-surface-alt);
  border: 1px solid var(--color-border);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--color-text);
}

.payment__note {
  margin-top: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.items {
  margin-top: var(--space-5);
}

.item {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding-block: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.item__media {
  width: 64px;
  height: 64px;
  border-radius: var(--radius-md);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--color-surface-alt);
}

.item__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.item__body {
  flex: 1;
  min-width: 0;
}

.item__name {
  font-weight: var(--weight-medium);
  font-size: var(--text-base);
}

.item__name:hover {
  color: var(--color-accent);
}

.item__meta {
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.totals {
  display: flex;
  justify-content: flex-end;
  padding-top: var(--space-5);
}

.totals__row {
  display: flex;
  align-items: baseline;
  gap: var(--space-5);
  font-weight: var(--weight-semibold);
}

.missing {
  padding: var(--space-12);
  text-align: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--space-3);
}

.missing__title {
  font-size: var(--text-xl);
}

.missing__copy {
  margin-bottom: var(--space-2);
}

@media (min-width: 900px) {
  .layout {
    grid-template-columns: minmax(0, 1.5fr) minmax(0, 1fr);
    gap: var(--space-6);
  }
  .facts {
    grid-template-columns: repeat(3, 1fr);
  }
}
</style>
