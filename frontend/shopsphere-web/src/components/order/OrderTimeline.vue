<script setup lang="ts">
import { computed } from 'vue'
import { Check } from '@lucide/vue'
import type { OrderStatus } from '../../types'

const props = defineProps<{ status: OrderStatus }>()

interface Step {
  key: string
  label: string
  caption: string
  statuses: OrderStatus[]
}

const STEPS: Step[] = [
  {
    key: 'placed',
    label: 'Order placed',
    caption: 'We have your order',
    statuses: ['PENDING', 'CONFIRMED', 'PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED'],
  },
  {
    key: 'paid',
    label: 'Payment confirmed',
    caption: 'Payment approved',
    statuses: ['PAID', 'PROCESSING', 'SHIPPED', 'DELIVERED'],
  },
  {
    key: 'preparing',
    label: 'Preparing order',
    caption: 'Picked and packed',
    statuses: ['PROCESSING', 'SHIPPED', 'DELIVERED'],
  },
  {
    key: 'shipped',
    label: 'Shipped',
    caption: 'On the way',
    statuses: ['SHIPPED', 'DELIVERED'],
  },
  {
    key: 'delivered',
    label: 'Delivered',
    caption: 'Enjoy it',
    statuses: ['DELIVERED'],
  },
]

const cancelled = computed(() => props.status === 'CANCELLED')

// Position within the track, derived from the order status rather than a stored counter, so a
// redelivered Kafka event can never leave the timeline in a state the backend does not hold.
const currentIndex = computed(() => {
  if (cancelled.value) return -1
  const index = STEPS.findIndex((step) => step.statuses.includes(props.status))
  return index === -1 ? 0 : index
})
</script>

<template>
  <div class="timeline-wrap">
    <p v-if="cancelled" class="timeline-cancelled">
      This order was cancelled. No further fulfilment steps will occur.
    </p>

    <ol class="timeline" :class="{ 'timeline--cancelled': cancelled }">
      <li
        v-for="(step, index) in STEPS"
        :key="step.key"
        class="timeline__step"
        :class="{
          'is-done': index < currentIndex,
          'is-current': index === currentIndex,
          'is-pending': index > currentIndex,
        }"
      >
        <span class="timeline__marker" aria-hidden="true">
          <Check v-if="index < currentIndex" :size="14" :stroke-width="3" />
          <span v-else class="timeline__dot" />
        </span>
        <span class="timeline__text">
          <span class="timeline__label">{{ step.label }}</span>
          <span class="timeline__caption">
            {{ index <= currentIndex ? step.caption : 'Waiting' }}
          </span>
        </span>
      </li>
    </ol>
  </div>
</template>

<style scoped>
.timeline-cancelled {
  padding: var(--space-3) var(--space-4);
  background: var(--color-danger-soft);
  color: #912018;
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  margin-bottom: var(--space-5);
}

.timeline {
  display: grid;
  gap: 0;
}

.timeline__step {
  position: relative;
  display: flex;
  gap: var(--space-4);
  padding-bottom: var(--space-6);
}

/* Connector line between markers, omitted on the final step. */
.timeline__step:not(:last-child)::before {
  content: '';
  position: absolute;
  left: 13px;
  top: 28px;
  bottom: 0;
  width: 2px;
  background: var(--color-border);
}

.timeline__marker {
  position: relative;
  z-index: 1;
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  border: 2px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-inverse);
}

.timeline__dot {
  width: 8px;
  height: 8px;
  border-radius: var(--radius-full);
  background: var(--color-border-strong);
}

.timeline__text {
  display: flex;
  flex-direction: column;
  padding-top: 2px;
}

.timeline__label {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--color-text-secondary);
}

.timeline__caption {
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.timeline__step.is-done .timeline__marker {
  background: var(--color-success);
  border-color: var(--color-success);
}

.timeline__step.is-done::before {
  background: var(--color-success);
}

.timeline__step.is-done .timeline__label {
  color: var(--color-text);
}

.timeline__step.is-current .timeline__marker {
  border-color: var(--color-accent);
  box-shadow: 0 0 0 4px var(--color-accent-soft);
}

.timeline__step.is-current .timeline__dot {
  background: var(--color-accent);
}

.timeline__step.is-current .timeline__label {
  color: var(--color-text);
}

@media (min-width: 700px) {
  /* Horizontal track reads better on a wide order-detail screen. */
  .timeline {
    grid-auto-flow: column;
    grid-auto-columns: 1fr;
    gap: var(--space-4);
  }
  .timeline__step {
    flex-direction: column;
    padding-bottom: 0;
    gap: var(--space-3);
  }
  .timeline__step::before {
    left: 28px;
    right: calc(-1 * var(--space-4));
    top: 13px;
    bottom: auto;
    width: auto;
    height: 2px;
  }
}
</style>
