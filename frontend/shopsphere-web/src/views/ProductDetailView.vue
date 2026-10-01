<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Minus, Plus, Heart, ShoppingBag, Truck, ShieldCheck, RotateCcw, Check, Package } from '@lucide/vue'
import ProductGallery from '../components/product/ProductGallery.vue'
import ProductCard from '../components/product/ProductCard.vue'
import StarRating from '../components/common/StarRating.vue'
import BaseBadge from '../components/common/BaseBadge.vue'
import BaseButton from '../components/common/BaseButton.vue'
import SkeletonBlock from '../components/common/SkeletonBlock.vue'
import { useCartStore } from '../stores/cart'
import { useUiStore } from '../stores/ui'
import { useProductStore } from '../stores/product'
import { useAuthStore } from '../stores/auth'
import { demoRating, imageFor } from '../mocks/products'
import type { Product } from '../types'

const route = useRoute()
const router = useRouter()
const cart = useCartStore()
const ui = useUiStore()
const products = useProductStore()
const auth = useAuthStore()

const product = ref<Product | null>(null)
const loading = ref(true)
const notFound = ref(false)
const quantity = ref(1)
const activeTab = ref<'details' | 'specs' | 'reviews'>('details')

const meta = computed(() => (product.value ? demoRating(product.value.id) : { rating: 0, reviews: 0 }))
const wished = computed(() => (product.value ? cart.isWishlisted(product.value.id) : false))
const inCart = computed(() => (product.value ? cart.quantityOf(product.value.id) : 0))

const related = computed(() => {
  if (!product.value) return []
  return products.allProducts
    .filter((item) => item.category === product.value?.category && item.id !== product.value?.id)
    .slice(0, 4)
})

const specs = computed(() => {
  if (!product.value) return []
  return [
    { label: 'SKU', value: product.value.sku },
    { label: 'Category', value: product.value.category },
    { label: 'Price', value: `$${product.value.price.toFixed(2)}` },
    { label: 'Availability', value: product.value.active ? 'In stock' : 'Unavailable' },
  ]
})

async function load(): Promise<void> {
  loading.value = true
  notFound.value = false
  const id = Number(route.params.id)
  if (!Number.isFinite(id)) {
    notFound.value = true
    loading.value = false
    return
  }
  if (products.allProducts.length === 0) {
    await products.fetchProducts()
  }
  const found = await products.fetchById(id)
  if (found) product.value = found
  else notFound.value = true
  loading.value = false
}

onMounted(load)
watch(() => route.params.id, load)

function decrement(): void {
  quantity.value = Math.max(1, quantity.value - 1)
}

function increment(): void {
  quantity.value = Math.min(10, quantity.value + 1)
}

function addToCart(): void {
  if (!product.value) return
  cart.add(product.value, quantity.value)
  ui.success(`${product.value.name} added to cart`)
}

function buyNow(): void {
  if (!product.value) return
  cart.add(product.value, quantity.value)
  if (!auth.isAuthenticated) {
    ui.notify('Sign in to complete checkout', 'info')
    router.push({ name: 'login', query: { redirect: '/checkout' } })
    return
  }
  router.push({ name: 'checkout' })
}

function toggleWishlist(): void {
  if (!product.value) return
  const added = cart.toggleWishlist(product.value.id)
  ui.notify(added ? 'Saved to your wishlist' : 'Removed from your wishlist', added ? 'success' : 'info')
}
</script>

<template>
  <div class="detail">
    <div class="container">
      <nav class="crumbs" aria-label="Breadcrumb">
        <ol>
          <li><RouterLink :to="{ name: 'home' }">Home</RouterLink></li>
          <li aria-hidden="true">/</li>
          <li><RouterLink :to="{ name: 'catalog' }">Shop</RouterLink></li>
          <li v-if="product" aria-hidden="true">/</li>
          <li v-if="product"><RouterLink :to="{ name: 'catalog', query: { category: product.category } }">{{ product.category }}</RouterLink></li>
          <li v-if="product" aria-hidden="true">/</li>
          <li v-if="product" aria-current="page">{{ product.name }}</li>
        </ol>
      </nav>

      <div v-if="loading" class="detail__grid">
        <SkeletonBlock height="480px" radius="var(--radius-xl)" />
        <div class="detail__panel">
          <SkeletonBlock height="1rem" width="30%" />
          <SkeletonBlock height="2.4rem" width="80%" />
          <SkeletonBlock height="1rem" width="40%" />
          <SkeletonBlock height="3rem" width="50%" />
          <SkeletonBlock height="12rem" />
          <SkeletonBlock height="3rem" />
        </div>
      </div>

      <div v-else-if="notFound" class="detail__missing">
        <Package :size="40" aria-hidden="true" />
        <h1>Product not found</h1>
        <p>This product may have been removed or is no longer listed.</p>
        <RouterLink class="btn btn--primary" :to="{ name: 'catalog' }">Back to shop</RouterLink>
      </div>

      <article v-else-if="product" class="detail__grid">
        <ProductGallery :product="product" />

        <div class="detail__panel">
          <div class="detail__badges">
            <BaseBadge tone="neutral">{{ product.category }}</BaseBadge>
            <BaseBadge v-if="product.active" tone="success" dot>In stock</BaseBadge>
            <BaseBadge v-else tone="danger" dot>Unavailable</BaseBadge>
          </div>

          <h1 class="detail__title">{{ product.name }}</h1>

          <div class="detail__rating">
            <StarRating :rating="meta.rating" :reviews="meta.reviews" />
            <span class="detail__sku">SKU {{ product.sku }}</span>
          </div>

          <p class="detail__price price price--lg">{{ product.price.toFixed(2) }}</p>

          <p class="detail__description">{{ product.description }}</p>

          <div class="detail__actions">
            <div class="qty" role="group" aria-label="Quantity">
              <button class="qty__btn" type="button" aria-label="Decrease quantity" :disabled="quantity <= 1" @click="decrement">
                <Minus :size="16" />
              </button>
              <span class="qty__value" aria-live="polite">{{ quantity }}</span>
              <button class="qty__btn" type="button" aria-label="Increase quantity" :disabled="quantity >= 10" @click="increment">
                <Plus :size="16" />
              </button>
            </div>

            <BaseButton class="detail__add" variant="primary" size="lg" @click="addToCart">
              <ShoppingBag :size="18" aria-hidden="true" />
              {{ inCart ? `In cart (${inCart})` : 'Add to cart' }}
            </BaseButton>

            <button
              class="icon-btn detail__wish"
              type="button"
              :aria-pressed="wished"
              :aria-label="wished ? 'Remove from wishlist' : 'Save to wishlist'"
              @click="toggleWishlist"
            >
              <Heart :size="20" :fill="wished ? 'currentColor' : 'none'" :class="{ 'is-wished': wished }" />
            </button>
          </div>

          <BaseButton class="detail__buy" variant="accent" size="lg" block @click="buyNow">
            Buy now
          </BaseButton>

          <ul class="detail__promises">
            <li><Truck :size="17" aria-hidden="true" /> Free shipping over $75</li>
            <li><ShieldCheck :size="17" aria-hidden="true" /> Secure simulated checkout</li>
            <li><RotateCcw :size="17" aria-hidden="true" /> 30-day returns</li>
          </ul>

          <div class="detail__tabs">
            <div class="tabs" role="tablist" aria-label="Product information">
              <button
                v-for="tab in (['details', 'specs', 'reviews'] as const)"
                :key="tab"
                class="tabs__tab"
                :class="{ 'is-active': activeTab === tab }"
                type="button"
                role="tab"
                :aria-selected="activeTab === tab"
                @click="activeTab = tab"
              >
                {{ tab === 'details' ? 'Details' : tab === 'specs' ? 'Specifications' : 'Reviews' }}
              </button>
            </div>

            <div class="tabs__panel" role="tabpanel">
              <p v-if="activeTab === 'details'">{{ product.description }}</p>

              <dl v-else-if="activeTab === 'specs'" class="specs">
                <div v-for="spec in specs" :key="spec.label" class="specs__row">
                  <dt>{{ spec.label }}</dt>
                  <dd>{{ spec.value }}</dd>
                </div>
              </dl>

              <div v-else class="reviews">
                <div class="reviews__summary">
                  <span class="reviews__score">{{ meta.rating.toFixed(1) }}</span>
                  <div>
                    <StarRating :rating="meta.rating" />
                    <p class="reviews__count">Based on {{ meta.reviews }} verified reviews</p>
                  </div>
                </div>
                <p class="reviews__placeholder">
                  Written reviews are not stored by the demo API yet. Rating and review count are
                  derived from the seeded catalogue.
                </p>
              </div>
            </div>
          </div>
        </div>
      </article>
    </div>

    <section v-if="related.length" class="page-section section--alt">
      <div class="container">
        <header class="section-head">
          <div>
            <h2 class="section-head__title">Related products</h2>
            <p class="section-head__subtitle">More from {{ product?.category }}.</p>
          </div>
        </header>
        <div class="grid-products">
          <ProductCard v-for="item in related" :key="item.id" :product="item" />
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.detail {
  padding-block: var(--space-6) var(--space-16);
}

.crumbs {
  padding-block: var(--space-4);
}

.crumbs ol {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.crumbs a:hover {
  color: var(--color-primary);
  text-decoration: underline;
}

.crumbs li[aria-current='page'] {
  color: var(--color-text);
  font-weight: var(--weight-medium);
}

.detail__grid {
  display: grid;
  gap: var(--space-8);
  margin-top: var(--space-4);
}

.detail__panel {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.detail__badges {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.detail__title {
  font-size: clamp(1.6rem, 3.4vw, var(--text-3xl));
}

.detail__rating {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-4);
}

.detail__sku {
  font-size: var(--text-sm);
  color: var(--color-muted);
  font-family: var(--font-mono);
}

.detail__price {
  margin-block: var(--space-1);
}

.detail__description {
  color: var(--color-text-secondary);
  line-height: var(--leading-normal);
}

.detail__actions {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-top: var(--space-2);
}

.detail__add {
  flex: 1;
}

.detail__wish {
  border: 1px solid var(--color-border-strong);
  width: 52px;
  height: 52px;
}

.detail__wish.is-wished {
  color: var(--color-danger);
}

.detail__buy {
  margin-top: var(--space-1);
}

.detail__promises {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
}

.detail__promises li {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.detail__promises svg {
  color: var(--color-primary);
  flex-shrink: 0;
}

.detail__missing {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-3);
  padding-block: var(--space-16);
  color: var(--color-muted);
}

.detail__missing h1 {
  font-size: var(--text-2xl);
}

.detail__missing p {
  margin-bottom: var(--space-3);
}

.tabs {
  display: flex;
  gap: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  overflow-x: auto;
}

.tabs__tab {
  padding: var(--space-3) 0;
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--color-muted);
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  white-space: nowrap;
  transition:
    color var(--duration-fast) var(--ease-out),
    border-color var(--duration-fast) var(--ease-out);
}

.tabs__tab:hover {
  color: var(--color-text);
}

.tabs__tab.is-active {
  color: var(--color-primary);
  border-bottom-color: var(--color-primary);
}

.tabs__panel {
  padding-top: var(--space-4);
  font-size: var(--text-base);
  color: var(--color-text-secondary);
}

.specs__row {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  padding-block: var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.specs__row dt {
  color: var(--color-muted);
}

.specs__row dd {
  font-weight: var(--weight-medium);
  color: var(--color-text);
}

.reviews__summary {
  display: flex;
  align-items: center;
  gap: var(--space-5);
  margin-bottom: var(--space-4);
}

.reviews__score {
  font-size: var(--text-3xl);
  font-weight: var(--weight-bold);
  letter-spacing: -0.03em;
}

.reviews__count,
.reviews__placeholder {
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.section--alt {
  background: var(--color-surface-alt);
}

@media (min-width: 900px) {
  .detail__grid {
    grid-template-columns: minmax(0, 1.05fr) minmax(0, 1fr);
    gap: var(--space-12);
  }
}
</style>
