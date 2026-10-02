<script setup lang="ts">
import { computed } from 'vue'
import { Heart, Plus, Check } from '@lucide/vue'
import { useCartStore } from '../../stores/cart'
import { useUiStore } from '../../stores/ui'
import { demoRating, imageFor } from '../../mocks/products'
import StarRating from '../common/StarRating.vue'
import type { Product } from '../../types'

const props = withDefaults(
  defineProps<{
    product: Product
    /** Old price is not in the API yet; the demo dataset supplies it for known products. */
    oldPrice?: number
    badge?: string
    compact?: boolean
  }>(),
  { oldPrice: undefined, badge: '', compact: false },
)

const cart = useCartStore()
const ui = useUiStore()

const meta = computed(() => demoRating(props.product.id))
const listPrice = computed(() => props.oldPrice ?? meta.value.oldPrice)
const discount = computed(() =>
  listPrice.value && listPrice.value > props.product.price
    ? Math.round(((listPrice.value - props.product.price) / listPrice.value) * 100)
    : 0,
)
const wished = computed(() => cart.isWishlisted(props.product.id))
const inCart = computed(() => cart.quantityOf(props.product.id) > 0)

function addToCart(): void {
  cart.add(props.product)
  ui.success(`${props.product.name} added to cart`)
}

function toggleWishlist(): void {
  const added = cart.toggleWishlist(props.product.id)
  ui.notify(added ? 'Saved to your wishlist' : 'Removed from your wishlist', added ? 'success' : 'info')
}
</script>

<template>
  <article class="product-card" :class="{ 'product-card--compact': compact }">
    <RouterLink
      class="product-card__media"
      :to="{ name: 'product-detail', params: { id: product.id } }"
      :aria-label="`View ${product.name}`"
    >
      <img :src="imageFor(product)" :alt="product.name" loading="lazy" width="600" height="600" />
      <span v-if="discount" class="product-card__badge badge badge--solid">-{{ discount }}%</span>
      <span v-else-if="badge" class="product-card__badge badge badge--solid">{{ badge }}</span>
    </RouterLink>

    <button
      class="product-card__wish"
      type="button"
      :aria-pressed="wished"
      :aria-label="wished ? `Remove ${product.name} from wishlist` : `Save ${product.name} to wishlist`"
      @click="toggleWishlist"
    >
      <Heart :size="18" :fill="wished ? 'currentColor' : 'none'" :class="{ 'is-wished': wished }" />
    </button>

    <div class="product-card__body">
      <p class="product-card__category">{{ product.category }}</p>
      <h3 class="product-card__name">
        <RouterLink :to="{ name: 'product-detail', params: { id: product.id } }">{{ product.name }}</RouterLink>
      </h3>
      <StarRating :rating="meta.rating" :reviews="meta.reviews" />

      <div class="product-card__price">
        <span class="price">{{ product.price.toFixed(2) }}</span>
        <span v-if="listPrice" class="price--was">{{ listPrice.toFixed(2) }}</span>
      </div>

      <button class="btn btn--outline btn--sm btn--block product-card__add" type="button" @click="addToCart">
        <Check v-if="inCart" :size="16" aria-hidden="true" />
        <Plus v-else :size="16" aria-hidden="true" />
        {{ inCart ? 'In cart' : 'Add to cart' }}
      </button>
    </div>
  </article>
</template>

<style scoped>
.product-card {
  position: relative;
  display: flex;
  flex-direction: column;
  min-width: 0;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  overflow: hidden;
  transition:
    transform var(--duration-base) var(--ease-out),
    box-shadow var(--duration-base) var(--ease-out),
    border-color var(--duration-base) var(--ease-out);
}

.product-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
  border-color: var(--color-border-strong);
}

.product-card__media {
  position: relative;
  display: block;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background: var(--color-surface-alt);
}

.product-card__media img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-slow) var(--ease-out);
}

.product-card:hover .product-card__media img {
  transform: scale(1.05);
}

.product-card__badge {
  position: absolute;
  top: var(--space-3);
  left: var(--space-3);
}

.product-card__wish {
  position: absolute;
  top: var(--space-3);
  right: var(--space-3);
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.92);
  color: var(--color-text-secondary);
  box-shadow: var(--shadow-sm);
  opacity: 0;
  transform: translateY(-4px);
  transition:
    opacity var(--duration-base) var(--ease-out),
    transform var(--duration-base) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.product-card:hover .product-card__wish,
.product-card__wish:focus-visible,
.product-card__wish[aria-pressed='true'] {
  opacity: 1;
  transform: none;
}

.product-card__wish:hover {
  color: var(--color-danger);
}

.product-card__wish .is-wished {
  color: var(--color-danger);
}

.product-card__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4);
  flex: 1;
}

.product-card__category {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-muted);
}

.product-card__name {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  line-height: var(--leading-snug);
  letter-spacing: -0.01em;
  /* Long product names must wrap rather than widen the card. */
  min-width: 0;
  overflow-wrap: anywhere;
}

.product-card__name a {
  transition: color var(--duration-fast) var(--ease-out);
}

.product-card__name a:hover {
  color: var(--color-accent);
}

.product-card__price {
  display: flex;
  align-items: baseline;
  gap: var(--space-2);
  margin-top: auto;
  padding-top: var(--space-2);
}

.product-card__add {
  margin-top: var(--space-3);
}

.product-card__add:hover {
  border-color: var(--color-primary);
}

/* Touch devices have no hover, so the affordance must always be visible. */
@media (hover: none) {
  .product-card__wish {
    opacity: 1;
    transform: none;
  }
}

@media (max-width: 860px) {
  .product-card__body {
    padding: var(--space-3);
    gap: var(--space-1);
  }
  .product-card__name {
    font-size: var(--text-sm);
  }
  .product-card__add {
    padding: 0 var(--space-2);
  }
}
</style>
