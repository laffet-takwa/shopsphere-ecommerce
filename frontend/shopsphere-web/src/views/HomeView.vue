<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowRight, Sparkles, Truck, ShieldCheck, RotateCcw, Headset, Star } from '@lucide/vue'
import ProductGrid from '../components/product/ProductGrid.vue'
import CategoryCard from '../components/product/CategoryCard.vue'
import ProductCard from '../components/product/ProductCard.vue'
import { useProductStore } from '../stores/product'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { categoryImage } from '../mocks/products'

const products = useProductStore()
const auth = useAuthStore()
const cart = useCartStore()
const router = useRouter()
const route = useRoute()

const promises = [
  { icon: Truck, title: 'Free Shipping', copy: 'On every order over $75' },
  { icon: ShieldCheck, title: 'Secure Payments', copy: 'Protected checkout every time' },
  { icon: RotateCcw, title: 'Easy Returns', copy: '30 days, no questions asked' },
  { icon: Headset, title: '24/7 Support', copy: 'Real people, any hour' },
]

const categories = computed(() => products.categories.slice(0, 6))
const trending = computed(() => [...products.visibleProducts].slice(0, 8))
const fresh = computed(() => [...products.visibleProducts].slice(8, 12))

onMounted(() => {
  void products.fetchProducts()
})

watch(
  () => route.fullPath,
  () => {
    void products.fetchProducts()
  },
)

function browseCategory(name: string): void {
  router.push({ name: 'catalog', query: { category: name } })
}
</script>

<template>
  <div>
    <section class="hero">
      <div class="container hero__inner">
        <div class="hero__copy">
          <p class="eyebrow">New season · 2026 collection</p>
          <h1 class="hero__title">
            Everything you need.<br />
            All in one place.
          </h1>
          <p class="hero__sub">
            Discover products you love, enjoy secure checkout, and track every order effortlessly.
          </p>
          <div class="hero__actions">
            <RouterLink class="btn btn--primary btn--lg" :to="{ name: 'catalog' }">
              Shop now <ArrowRight :size="18" aria-hidden="true" />
            </RouterLink>
            <a class="btn btn--outline btn--lg" href="#categories">Explore categories</a>
          </div>
          <dl class="hero__stats">
            <div>
              <dt>Products</dt>
              <dd>{{ products.totalElements }}+</dd>
            </div>
            <div>
              <dt>Categories</dt>
              <dd>{{ products.categories.length }}</dd>
            </div>
            <div>
              <dt>Rating</dt>
              <dd>4.8<Star :size="14" class="hero__star" aria-label="out of 5" /></dd>
            </div>
          </dl>
        </div>

        <!-- Decorative collage: pure CSS composition, no placeholder blocks. -->
        <div class="hero__visual" aria-hidden="true">
          <div class="hero__tile hero__tile--main">
            <img :src="categoryImage('Electronics')" alt="" width="600" height="720" />
          </div>
          <div class="hero__tile hero__tile--sm hero__tile--a">
            <img :src="categoryImage('Home & Living')" alt="" width="320" height="320" />
          </div>
          <div class="hero__tile hero__tile--sm hero__tile--b">
            <img :src="categoryImage('Fashion')" alt="" width="320" height="320" />
          </div>
          <span class="hero__badge">
            <Sparkles :size="15" aria-hidden="true" /> Curated weekly
          </span>
        </div>
      </div>
    </section>

    <section class="promises" aria-label="Service promises">
      <div class="container promises__grid">
        <div v-for="item in promises" :key="item.title" class="promise">
          <span class="promise__icon" aria-hidden="true"><component :is="item.icon" :size="20" /></span>
          <span>
            <span class="promise__title">{{ item.title }}</span>
            <span class="promise__copy">{{ item.copy }}</span>
          </span>
        </div>
      </div>
    </section>

    <section id="categories" class="page-section">
      <div class="container">
        <header class="section-head">
          <div>
            <h2 class="section-head__title">Shop by category</h2>
            <p class="section-head__subtitle">Six departments, one checkout.</p>
          </div>
          <RouterLink class="section-head__link" :to="{ name: 'catalog' }">
            View all <ArrowRight :size="16" aria-hidden="true" />
          </RouterLink>
        </header>

        <div v-if="products.loading && !categories.length" class="category-grid">
          <div v-for="index in 6" :key="index" class="skeleton" style="aspect-ratio: 3 / 4" />
        </div>
        <div v-else class="category-grid">
          <CategoryCard
            v-for="category in categories"
            :key="category.name"
            :category="category.name"
            :count="category.count"
            @select="browseCategory"
          />
        </div>
      </div>
    </section>

    <section class="page-section section--alt">
      <div class="container">
        <header class="section-head">
          <div>
            <h2 class="section-head__title">Trending products</h2>
            <p class="section-head__subtitle">Popular products customers are loving right now.</p>
          </div>
          <RouterLink class="section-head__link" :to="{ name: 'catalog' }">
            Shop all <ArrowRight :size="16" aria-hidden="true" />
          </RouterLink>
        </header>

        <ProductGrid :products="trending" :loading="products.loading" :skeleton-count="8" empty-title="The catalog is warming up." />
      </div>
    </section>

    <section v-if="fresh.length" class="page-section">
      <div class="container">
        <header class="section-head">
          <div>
            <h2 class="section-head__title">Also worth a look</h2>
            <p class="section-head__subtitle">Fresh stock across every department.</p>
          </div>
        </header>
        <div class="grid-products">
          <ProductCard v-for="product in fresh" :key="product.id" :product="product" />
        </div>
      </div>
    </section>

    <section class="page-section">
      <div class="container cta">
        <div class="cta__body">
          <h2 class="cta__title">{{ auth.isAuthenticated ? 'Welcome back, ' + auth.displayName.split(' ')[0] : 'Join ShopSphere today' }}</h2>
          <p class="cta__copy">
            {{
              auth.isAuthenticated
                ? 'Track open orders, manage your wishlist and update your details from one place.'
                : 'Create an account to track orders, save a wishlist and check out in seconds.'
            }}
          </p>
        </div>
        <RouterLink
          class="btn btn--primary btn--lg"
          :to="auth.isAuthenticated ? { name: 'account' } : { name: 'register' }"
        >
          {{ auth.isAuthenticated ? 'Open your dashboard' : 'Create free account' }}
        </RouterLink>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  background: linear-gradient(180deg, var(--color-primary-soft) 0%, var(--color-background) 100%);
  overflow: hidden;
}

.hero__inner {
  display: grid;
  gap: var(--space-10);
  padding-block: clamp(var(--space-10), 7vw, var(--space-20));
  align-items: center;
}

.hero__title {
  margin-top: var(--space-4);
  font-size: clamp(2.1rem, 5.4vw, var(--text-5xl));
  line-height: 1.06;
  letter-spacing: -0.035em;
}

.hero__sub {
  margin-top: var(--space-5);
  font-size: var(--text-md);
  color: var(--color-text-secondary);
  max-width: 46ch;
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-8);
}

.hero__stats {
  display: flex;
  gap: var(--space-8);
  margin-top: var(--space-10);
  padding-top: var(--space-6);
  border-top: 1px solid var(--color-border);
}

.hero__stats dt {
  font-size: var(--text-xs);
  text-transform: uppercase;
  letter-spacing: var(--tracking-wide);
  color: var(--color-muted);
}

.hero__stats dd {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  font-size: var(--text-xl);
  font-weight: var(--weight-bold);
  letter-spacing: -0.02em;
  margin-top: 2px;
}

.hero__star {
  color: #f5a524;
}

/* Collage composition */
.hero__visual {
  position: relative;
  display: grid;
  grid-template-columns: 1.4fr 1fr;
  grid-template-rows: 1fr 1fr;
  gap: var(--space-3);
  height: clamp(320px, 46vw, 460px);
}

.hero__tile {
  border-radius: var(--radius-xl);
  overflow: hidden;
  box-shadow: var(--shadow-lg);
}

.hero__tile img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.hero__tile--main {
  grid-row: 1 / -1;
}

.hero__tile--sm {
  border-radius: var(--radius-lg);
}

.hero__tile--a {
  transform: translateY(-10px);
}

.hero__tile--b {
  transform: translateY(10px);
}

.hero__badge {
  position: absolute;
  bottom: var(--space-4);
  left: var(--space-4);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-full);
  background: rgba(255, 255, 255, 0.94);
  color: var(--color-primary);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  box-shadow: var(--shadow-md);
}

.promises {
  background: var(--color-surface);
  border-block: 1px solid var(--color-border);
}

.promises__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
  padding-block: var(--space-6);
}

.promise {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.promise__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.promise__title {
  display: block;
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
}

.promise__copy {
  display: block;
  font-size: var(--text-xs);
  color: var(--color-muted);
}

.section--alt {
  background: var(--color-surface-alt);
}

.section-head__link {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-accent);
}

.section-head__link:hover {
  text-decoration: underline;
}

.category-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
}

.cta {
  display: flex;
  flex-direction: column;
  gap: var(--space-6);
  padding: clamp(var(--space-6), 5vw, var(--space-12));
  border-radius: var(--radius-xl);
  background: var(--color-primary);
  color: var(--color-inverse);
}

.cta__title {
  color: var(--color-inverse);
  font-size: clamp(1.5rem, 3vw, var(--text-2xl));
}

.cta__copy {
  color: rgba(255, 255, 255, 0.76);
  max-width: 52ch;
  margin-top: var(--space-2);
}

@media (min-width: 700px) {
  .promises__grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .category-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: var(--space-5);
  }
  .cta {
    flex-direction: row;
    align-items: center;
    justify-content: space-between;
  }
}

@media (min-width: 1000px) {
  .hero__inner {
    grid-template-columns: 1.05fr 0.95fr;
    gap: var(--space-16);
  }
  .category-grid {
    grid-template-columns: repeat(6, minmax(0, 1fr));
  }
}
</style>
