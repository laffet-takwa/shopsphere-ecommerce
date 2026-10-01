<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { CheckCircle2, Truck, Package, Info } from '@lucide/vue'
import BaseButton from '../components/common/BaseButton.vue'
import { useOrderStore } from '../stores/order'
import type { Order } from '../types'

const route = useRoute()
const orders = useOrderStore()

const order = ref<Order | null>(null)

const deliveryEstimate = computed(() => {
  if (!order.value) return ''
  const created = new Date(order.value.createdAt)
  created.setDate(created.getDate() + 5)
  return created.toLocaleDateString('en-US', { month: 'long', day: 'numeric' })
})

onMounted(async () => {
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) return
  // Prefer the store copy so the page renders instantly after checkout.
  order.value = orders.byId(id) ?? (await orders.fetchOne(id))
})
</script>

<template>
  <div class="page container">
    <section v-if="order" class="confirm">
      <span class="confirm__icon" aria-hidden="true">
        <CheckCircle2 :size="40" :stroke-width="1.75" />
      </span>
      <h1 class="confirm__title">Order Confirmed!</h1>
      <p class="confirm__copy">Thank you for your purchase.</p>

      <p class="confirm__ref">
        Order number <strong>#{{ order.id }}</strong>
      </p>

      <dl class="facts">
        <div>
          <dt>Placed</dt>
          <dd>{{ new Date(order.createdAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' }) }}</dd>
        </div>
        <div>
          <dt>Total</dt>
          <dd class="price">{{ order.totalAmount.toFixed(2) }}</dd>
        </div>
        <div>
          <dt>Estimated delivery</dt>
          <dd>{{ deliveryEstimate }}</dd>
        </div>
      </dl>

      <div class="confirm__actions">
        <BaseButton variant="primary" size="lg" :to="{ name: 'order-detail', params: { id: order.id } }">
          <Truck :size="18" aria-hidden="true" /> Track order
        </BaseButton>
        <BaseButton variant="outline" size="lg" to="/products">Continue shopping</BaseButton>
      </div>

      <p class="confirm__note">
        <Info :size="15" aria-hidden="true" />
        Stock is reserved and payment is processed asynchronously through Kafka. Watch the status
        change from Pending to Paid in your order timeline.
      </p>
    </section>

    <section v-else class="confirm confirm--empty">
      <span class="confirm__icon" aria-hidden="true"><Package :size="36" /></span>
      <h1 class="confirm__title">We could not find that order</h1>
      <p class="confirm__copy">It may still be processing, or belong to a different account.</p>
      <BaseButton variant="primary" to="/orders">View your orders</BaseButton>
    </section>
  </div>
</template>

<style scoped>
.page {
  padding-block: var(--space-10) var(--space-16);
  display: flex;
  justify-content: center;
}

.confirm {
  max-width: 640px;
  width: 100%;
  text-align: center;
  padding: clamp(var(--space-6), 5vw, var(--space-12));
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-sm);
}

.confirm__icon {
  display: grid;
  place-items: center;
  width: 72px;
  height: 72px;
  margin: 0 auto var(--space-5);
  border-radius: var(--radius-full);
  background: var(--color-success-soft);
  color: var(--color-success);
}

.confirm__title {
  font-size: clamp(1.6rem, 3.6vw, var(--text-3xl));
}

.confirm__copy {
  margin-top: var(--space-3);
  font-size: var(--text-md);
  color: var(--color-text-secondary);
}

.confirm__ref {
  margin-top: var(--space-4);
  font-size: var(--text-base);
  color: var(--color-muted);
}

.confirm__ref strong {
  color: var(--color-text);
}

.facts {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: var(--space-4);
  margin-top: var(--space-8);
  padding-block: var(--space-6);
  border-block: 1px solid var(--color-border);
}

.facts dt {
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: var(--tracking-wide);
  color: var(--color-muted);
}

.facts dd {
  margin-top: var(--space-2);
  font-weight: var(--weight-semibold);
  font-variant-numeric: tabular-nums;
}

.confirm__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-3);
  margin-top: var(--space-8);
}

.confirm__note {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-top: var(--space-8);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
  font-size: var(--text-sm);
  color: var(--color-muted);
  text-align: left;
}

.confirm__note svg {
  flex-shrink: 0;
  margin-top: 2px;
}

.confirm--empty .confirm__icon {
  background: var(--color-surface-alt);
  color: var(--color-muted);
}

@media (max-width: 560px) {
  .facts {
    grid-template-columns: 1fr;
  }
}
</style>
