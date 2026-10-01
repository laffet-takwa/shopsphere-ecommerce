<script setup lang="ts">
import { Minus, Plus, Trash2 } from '@lucide/vue'
import { useCartStore } from '../../stores/cart'
import { useUiStore } from '../../stores/ui'
import { imageFor } from '../../mocks/products'
import type { CartLine } from '../../types'

const props = defineProps<{ line: CartLine }>()

const cart = useCartStore()
const ui = useUiStore()

function remove(): void {
  cart.remove(props.line.product.id)
  ui.notify(`${props.line.product.name} removed from cart`, 'info')
}
</script>

<template>
  <article class="cart-line">
    <RouterLink
      class="cart-line__media"
      :to="{ name: 'product-detail', params: { id: line.product.id } }"
      :aria-label="`View ${line.product.name}`"
    >
      <img :src="imageFor(line.product)" :alt="line.product.name" width="160" height="160" loading="lazy" />
    </RouterLink>

    <div class="cart-line__body">
      <p class="cart-line__category">{{ line.product.category }}</p>
      <h3 class="cart-line__name">
        <RouterLink :to="{ name: 'product-detail', params: { id: line.product.id } }">
          {{ line.product.name }}
        </RouterLink>
      </h3>
      <p class="cart-line__variant">SKU {{ line.product.sku }}</p>

      <div class="cart-line__controls">
        <div class="qty">
          <button
            class="qty__btn"
            type="button"
            :aria-label="`Decrease quantity of ${line.product.name}`"
            @click="cart.changeQuantity(line.product.id, -1)"
          >
            <Minus :size="15" />
          </button>
          <span class="qty__value" :aria-label="`Quantity ${line.quantity}`">{{ line.quantity }}</span>
          <button
            class="qty__btn"
            type="button"
            :aria-label="`Increase quantity of ${line.product.name}`"
            @click="cart.changeQuantity(line.product.id, 1)"
          >
            <Plus :size="15" />
          </button>
        </div>

        <button class="cart-line__remove" type="button" @click="remove">
          <Trash2 :size="15" aria-hidden="true" /> Remove
        </button>
      </div>
    </div>

    <p class="cart-line__subtotal">{{ (line.product.price * line.quantity).toFixed(2) }}</p>
  </article>
</template>

<style scoped>
.cart-line {
  display: grid;
  grid-template-columns: 84px 1fr auto;
  gap: var(--space-4);
  padding: var(--space-4) 0;
  border-bottom: 1px solid var(--color-border);
  align-items: start;
}

.cart-line:last-child {
  border-bottom: none;
}

.cart-line__media {
  border-radius: var(--radius-md);
  overflow: hidden;
  aspect-ratio: 1 / 1;
  background: var(--color-surface-alt);
}

.cart-line__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cart-line__category {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-muted);
}

.cart-line__name {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  margin-top: 2px;
}

.cart-line__name a:hover {
  color: var(--color-accent);
}

.cart-line__variant {
  font-size: var(--text-xs);
  color: var(--color-muted);
  margin-top: 2px;
}

.cart-line__controls {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
  margin-top: var(--space-3);
}

.cart-line__remove {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-muted);
  transition: color var(--duration-fast) var(--ease-out);
}

.cart-line__remove:hover {
  color: var(--color-danger);
}

.cart-line__subtotal {
  font-weight: var(--weight-bold);
  font-variant-numeric: tabular-nums;
  text-align: right;
}

@media (max-width: 560px) {
  .cart-line {
    grid-template-columns: 64px 1fr;
  }
  .cart-line__subtotal {
    grid-column: 2;
    text-align: left;
    font-size: var(--text-md);
  }
}
</style>
