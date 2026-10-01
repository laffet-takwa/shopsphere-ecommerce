<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Package, XCircle, ArrowRight } from '@lucide/vue'
import OrderStatusBadge from '../components/order/OrderStatusBadge.vue'
import EmptyState from '../components/common/EmptyState.vue'
import SkeletonBlock from '../components/common/SkeletonBlock.vue'
import BaseButton from '../components/common/BaseButton.vue'
import { useOrderStore } from '../stores/order'
import { useUiStore } from '../stores/ui'
import type { Order } from '../types'

const orders = useOrderStore()
const ui = useUiStore()
const router = useRouter()

const upcoming = computed(() => orders.sorted.filter((order) => !['DELIVERED', 'CANCELLED'].includes(order.status)))
const past = computed(() => orders.sorted.filter((order) => ['DELIVERED', 'CANCELLED'].includes(order.status)))

onMounted(() => {
  void orders.fetchMine()
})

function formatDate(value: string): string {
  return new Date(value).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })
}

function itemCount(order: Order): number {
  return order.items.reduce((sum, item) => sum + item.quantity, 0)
}

async function cancel(order: Order): Promise<void> {
  await orders.cancel(order.id)
  ui.success(`Order #${order.id} cancelled`)
}
</script>

<template>
  <div class="page container">
    <header class="page__head">
      <div>
        <h1 class="page__title">Your orders</h1>
        <p class="page__subtitle">Track deliveries, review history and cancel anything still pending.</p>
      </div>
      <BaseButton to="/products" variant="outline" size="sm">Continue shopping</BaseButton>
    </header>

    <p v-if="orders.error" class="alert alert--error" role="alert">{{ orders.error }}</p>

    <div v-if="orders.loading" class="stack">
      <div v-for="index in 3" :key="index" class="card skeleton-row">
        <SkeletonBlock height="1rem" width="22%" />
        <SkeletonBlock height="2.6rem" />
      </div>
    </div>

    <EmptyState
      v-else-if="orders.sorted.length === 0"
      :icon="Package"
      title="You haven't placed any orders yet."
      message="When you place an order it will appear here with live status updates."
      action-label="Start shopping"
      to="/products"
    />

    <template v-else>
      <section v-if="upcoming.length" class="group">
        <h2 class="group__title">Active orders</h2>
        <div class="group__list">
          <article v-for="order in upcoming" :key="order.id" class="order-card card">
            <header class="order-card__head">
              <div>
                <p class="order-card__id">Order #{{ order.id }}</p>
                <p class="order-card__date">Placed {{ formatDate(order.createdAt) }}</p>
              </div>
              <OrderStatusBadge :status="order.status" />
            </header>

            <ul class="order-card__items">
              <li v-for="item in order.items" :key="item.productId">
                <span class="order-card__name">{{ item.productName }}</span>
                <span class="order-card__qty">×{{ item.quantity }}</span>
                <span class="price order-card__price">{{ item.subtotal.toFixed(2) }}</span>
              </li>
            </ul>

            <footer class="order-card__foot">
              <span class="order-card__meta">{{ itemCount(order) }} items</span>
              <span class="order-card__total">
                Total <strong>{{ order.totalAmount.toFixed(2) }}</strong>
              </span>
              <div class="order-card__actions">
                <BaseButton
                  v-if="order.status === 'PENDING'"
                  variant="ghost"
                  size="sm"
                  @click="cancel(order)"
                >
                  <XCircle :size="16" aria-hidden="true" /> Cancel
                </BaseButton>
                <BaseButton
                  variant="outline"
                  size="sm"
                  :to="{ name: 'order-detail', params: { id: order.id } }"
                >
                  View details <ArrowRight :size="15" aria-hidden="true" />
                </BaseButton>
              </div>
            </footer>
          </article>
        </div>
      </section>

      <section v-if="past.length" class="group">
        <h2 class="group__title">Order history</h2>
        <div class="group__list">
          <article v-for="order in past" :key="order.id" class="order-card card">
            <header class="order-card__head">
              <div>
                <p class="order-card__id">Order #{{ order.id }}</p>
                <p class="order-card__date">Placed {{ formatDate(order.createdAt) }}</p>
              </div>
              <OrderStatusBadge :status="order.status" />
            </header>
            <footer class="order-card__foot">
              <span class="order-card__meta">{{ itemCount(order) }} items</span>
              <span class="order-card__total">
                Total <strong>{{ order.totalAmount.toFixed(2) }}</strong>
              </span>
              <BaseButton
                variant="outline"
                size="sm"
                :to="{ name: 'order-detail', params: { id: order.id } }"
              >
                View details
              </BaseButton>
            </footer>
          </article>
        </div>
      </section>
    </template>
  </div>
</template>

<style scoped>
.page {
  padding-block: var(--space-8) var(--space-16);
}

.page__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-8);
}

.page__title {
  font-size: clamp(1.6rem, 3.4vw, var(--text-2xl));
}

.page__subtitle {
  margin-top: var(--space-2);
  color: var(--color-muted);
}

.stack {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.skeleton-row {
  padding: var(--space-6);
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.group + .group {
  margin-top: var(--space-10);
}

.group__title {
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-4);
}

.group__list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.order-card {
  padding: var(--space-5);
}

.order-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.order-card__id {
  font-weight: var(--weight-semibold);
}

.order-card__date {
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.order-card__items {
  margin-top: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: var(--space-1);
  padding-block: var(--space-3);
  border-block: 1px solid var(--color-border);
}

.order-card__items li {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.order-card__name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-card__qty {
  color: var(--color-muted);
}

.order-card__price {
  font-size: var(--text-sm);
  min-width: 64px;
  text-align: right;
}

.order-card__foot {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-4);
}

.order-card__meta {
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.order-card__total {
  margin-left: auto;
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.order-card__actions {
  display: flex;
  gap: var(--space-2);
}

@media (min-width: 700px) {
  .order-card__foot {
    flex-wrap: nowrap;
  }
}
</style>
