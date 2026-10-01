<script setup lang="ts">
import { ShoppingBag, ArrowRight, Tag } from '@lucide/vue'
import CartItemRow from '../components/cart/CartItemRow.vue'
import OrderSummaryCard from '../components/cart/OrderSummaryCard.vue'
import EmptyState from '../components/common/EmptyState.vue'
import { useCartStore } from '../stores/cart'

const cart = useCartStore()
</script>

<template>
  <div class="page container">
    <header class="page__head">
      <h1 class="page__title">Your cart</h1>
      <p v-if="!cart.isEmpty" class="page__subtitle">
        {{ cart.count }} item{{ cart.count === 1 ? '' : 's' }} ready to check out.
      </p>
    </header>

    <div v-if="cart.isEmpty" class="cart-empty card">
      <EmptyState
        :icon="ShoppingBag"
        title="Your cart is waiting."
        message="Browse the catalog and add something you love — it will show up here."
        action-label="Start shopping"
        to="/products"
      />
    </div>

    <div v-else class="cart">
      <section class="cart__items card" aria-label="Cart items">
        <div class="card__head">
          <h2 class="card__title">Items</h2>
          <button class="cart__clear" type="button" @click="cart.clear()">Clear cart</button>
        </div>
        <div class="card__body">
          <CartItemRow v-for="line in cart.lines" :key="line.product.id" :line="line" />
        </div>
        <div class="cart__foot">
          <RouterLink class="cart__continue" :to="{ name: 'catalog' }">
            Continue shopping
          </RouterLink>
        </div>
      </section>

      <OrderSummaryCard :show-action="true" />
    </div>

    <section v-if="!cart.isEmpty" class="perks">
      <article class="perk">
        <Tag :size="18" aria-hidden="true" />
        <div>
          <h3>Promo codes</h3>
          <p>Apply a discount code at checkout. This demo applies shipping and tax automatically.</p>
        </div>
      </article>
      <article class="perk">
        <ArrowRight :size="18" aria-hidden="true" />
        <div>
          <h3>Free returns</h3>
          <p>Return anything within 30 days for a full refund, no questions asked.</p>
        </div>
      </article>
    </section>
  </div>
</template>

<style scoped>
.page {
  padding-block: var(--space-8) var(--space-16);
}

.page__head {
  margin-bottom: var(--space-8);
}

.page__title {
  font-size: clamp(1.6rem, 3.4vw, var(--text-2xl));
}

.page__subtitle {
  margin-top: var(--space-2);
  color: var(--color-muted);
}

.cart-empty {
  padding: var(--space-6);
}

.cart {
  display: grid;
  gap: var(--space-6);
  align-items: start;
}

.cart__clear {
  font-size: var(--text-sm);
  color: var(--color-muted);
  transition: color var(--duration-fast) var(--ease-out);
}

.cart__clear:hover {
  color: var(--color-danger);
}

.cart__foot {
  padding: var(--space-4) var(--space-6);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface-alt);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}

.cart__continue {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--color-accent);
}

.cart__continue:hover {
  text-decoration: underline;
}

.perks {
  display: grid;
  gap: var(--space-4);
  margin-top: var(--space-8);
}

.perk {
  display: flex;
  gap: var(--space-4);
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  color: var(--color-text-secondary);
}

.perk svg {
  color: var(--color-primary);
  flex-shrink: 0;
  margin-top: 2px;
}

.perk h3 {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
}

.perk p {
  font-size: var(--text-sm);
  margin-top: var(--space-1);
}

@media (min-width: 900px) {
  .cart {
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: var(--space-8);
  }
  .perks {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
