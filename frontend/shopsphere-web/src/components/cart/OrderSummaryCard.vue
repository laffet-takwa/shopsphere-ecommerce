<script setup lang="ts">
import { useCartStore } from '../../stores/cart'

withDefaults(defineProps<{ showAction?: boolean; actionLabel?: string }>(), {
  showAction: false,
  actionLabel: 'Proceed to checkout',
})

const cart = useCartStore()
</script>

<template>
  <aside class="summary card" aria-labelledby="summary-title">
    <h2 id="summary-title" class="summary__title">Order summary</h2>

    <dl class="summary__rows">
      <div class="summary__row">
        <dt>Subtotal</dt>
        <dd>{{ cart.subtotal.toFixed(2) }}</dd>
      </div>
      <div v-if="cart.savings > 0" class="summary__row summary__row--saving">
        <dt>You save</dt>
        <dd>−{{ cart.savings.toFixed(2) }}</dd>
      </div>
      <div class="summary__row">
        <dt>Shipping</dt>
        <dd>{{ cart.shipping === 0 ? 'Free' : cart.shipping.toFixed(2) }}</dd>
      </div>
      <div class="summary__row">
        <dt>Estimated tax</dt>
        <dd>{{ cart.tax.toFixed(2) }}</dd>
      </div>
    </dl>

    <div class="summary__total">
      <span>Total</span>
      <span class="price price--lg">{{ cart.total.toFixed(2) }}</span>
    </div>

    <p v-if="cart.subtotal < 75" class="summary__nudge">
      Add {{ (75 - cart.subtotal).toFixed(2) }} more to qualify for free shipping.
    </p>
    <p v-else class="summary__nudge summary__nudge--ok">Free shipping applied.</p>

    <slot name="action">
      <RouterLink
        v-if="showAction"
        class="btn btn--primary btn--block btn--lg"
        :to="{ name: 'checkout' }"
      >
        {{ actionLabel }}
      </RouterLink>
    </slot>

    <p class="summary__secure">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true">
        <rect x="4" y="10" width="16" height="10" rx="2" />
        <path d="M8 10V7a4 4 0 0 1 8 0v3" />
      </svg>
      Encrypted checkout · Payment is simulated
    </p>
  </aside>
</template>

<style scoped>
.summary {
  padding: var(--space-5);
}

.summary__title {
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-4);
}

.summary__rows {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
}

.summary__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  font-size: var(--text-base);
  color: var(--color-text-secondary);
}

.summary__row dd {
  font-variant-numeric: tabular-nums;
  font-weight: var(--weight-medium);
  color: var(--color-text);
}

.summary__row--saving dd {
  color: var(--color-success);
}

.summary__total {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
  padding-block: var(--space-4);
  font-weight: var(--weight-semibold);
}

.summary__nudge {
  font-size: var(--text-sm);
  color: var(--color-muted);
  margin-bottom: var(--space-4);
}

.summary__nudge--ok {
  color: var(--color-success);
}

.summary__secure {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: var(--space-2);
  margin-top: var(--space-4);
  font-size: var(--text-xs);
  color: var(--color-muted);
}
</style>
