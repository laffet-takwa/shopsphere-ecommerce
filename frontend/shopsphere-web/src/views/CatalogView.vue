<script setup lang="ts">
import { computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { SlidersHorizontal, X, Star, Check } from '@lucide/vue'
import ProductGrid from '../components/product/ProductGrid.vue'
import EmptyState from '../components/common/EmptyState.vue'
import BaseButton from '../components/common/BaseButton.vue'
import { useProductStore } from '../stores/product'
import { useUiStore } from '../stores/ui'

const products = useProductStore()
const ui = useUiStore()
const route = useRoute()
const router = useRouter()

const SORT_OPTIONS = [
  { value: 'featured', label: 'Featured' },
  { value: 'newest', label: 'Newest' },
  { value: 'price-asc', label: 'Price: Low → High' },
  { value: 'price-desc', label: 'Price: High → Low' },
  { value: 'rating', label: 'Top rated' },
] as const

const RATINGS = [4.5, 4, 3.5, 3]

const minPrice = computed({
  get: () => products.filters.minPrice,
  set: (value: number | null) => {
    products.filters.minPrice = value
  },
})

const maxPrice = computed({
  get: () => products.filters.maxPrice,
  set: (value: number | null) => {
    products.filters.maxPrice = value
  },
})

const sort = computed({
  get: () => products.filters.sort,
  set: (value) => {
    products.filters.sort = value
  },
})

const heading = computed(() => {
  if (products.filters.keyword) return `Results for “${products.filters.keyword}”`
  if (products.filters.category) return products.filters.category
  return 'All products'
})

const subtitle = computed(() => {
  if (products.filters.keyword) return 'Matching items from the ShopSphere catalog.'
  return 'Discover products curated for you.'
})

// Sync URL query params to filters so a filtered view is shareable and survives a reload.
function readQuery(): void {
  const category = route.query.category
  const keyword = route.query.keyword
  products.filters.category = typeof category === 'string' && category ? category : null
  products.filters.keyword = typeof keyword === 'string' ? keyword : ''
  const requestedSort = route.query.sort
  if (typeof requestedSort === 'string' && SORT_OPTIONS.some((option) => option.value === requestedSort)) {
    products.filters.sort = requestedSort as (typeof SORT_OPTIONS)[number]['value']
  }
  void products.fetchProducts()
}

onMounted(readQuery)
watch(() => route.query, readQuery)

function applyCategory(name: string | null): void {
  products.filters.category = name
  router.replace({ query: { ...route.query, category: name ?? undefined } })
  void products.fetchProducts()
}

function applyRating(value: number | null): void {
  products.filters.minRating = products.filters.minRating === value ? null : value
}

function clearAll(): void {
  products.resetFilters()
  ui.filterDrawerOpen = false
  router.replace({ query: {} })
  void products.fetchProducts()
}
</script>

<template>
  <div class="catalog">
    <div class="container catalog__layout">
      <!-- Desktop filter rail -->
      <aside class="filters" aria-label="Product filters">
        <div class="filters__head">
          <h2 class="filters__title">Filters</h2>
          <button v-if="products.activeFilterCount" class="filters__clear" type="button" @click="clearAll">
            Clear all ({{ products.activeFilterCount }})
          </button>
        </div>

        <section class="filters__group">
          <h3 class="filters__legend">Category</h3>
          <ul class="filters__list">
            <li>
              <button
                class="filters__option"
                :class="{ 'is-active': products.filters.category === null }"
                type="button"
                @click="applyCategory(null)"
              >
                All products
              </button>
            </li>
            <li v-for="category in products.categories" :key="category.name">
              <button
                class="filters__option"
                :class="{ 'is-active': products.filters.category === category.name }"
                type="button"
                @click="applyCategory(category.name)"
              >
                <span>{{ category.name }}</span>
                <span class="filters__count">{{ category.count }}</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="filters__group">
          <h3 class="filters__legend">Price</h3>
          <div class="filters__range">
            <label class="visually-hidden" for="min-price">Minimum price</label>
            <input
              id="min-price"
              class="input"
              type="number"
              inputmode="decimal"
              placeholder="Min"
              min="0"
              :value="minPrice ?? ''"
              @input="minPrice = ($event.target as HTMLInputElement).value === '' ? null : Number(($event.target as HTMLInputElement).value)"
            />
            <span class="filters__range-sep" aria-hidden="true">–</span>
            <label class="visually-hidden" for="max-price">Maximum price</label>
            <input
              id="max-price"
              class="input"
              type="number"
              inputmode="decimal"
              placeholder="Max"
              min="0"
              :value="maxPrice ?? ''"
              @input="maxPrice = ($event.target as HTMLInputElement).value === '' ? null : Number(($event.target as HTMLInputElement).value)"
            />
          </div>
          <p class="field__hint">Catalog range {{ products.priceBounds.min }}–{{ products.priceBounds.max }}.</p>
        </section>

        <section class="filters__group">
          <h3 class="filters__legend">Rating</h3>
          <ul class="filters__list">
            <li v-for="value in RATINGS" :key="value">
              <button
                class="filters__option filters__option--rating"
                :class="{ 'is-active': products.filters.minRating === value }"
                type="button"
                @click="applyRating(value)"
              >
                <span class="filters__stars" aria-hidden="true">
                  <Star v-for="index in 5" :key="index" :size="13" :class="{ 'on': index <= Math.round(value) }" />
                </span>
                <span>{{ value }} &amp; up</span>
              </button>
            </li>
          </ul>
        </section>

        <section class="filters__group">
          <h3 class="filters__legend">Availability</h3>
          <label class="checkbox">
            <input v-model="products.filters.inStockOnly" type="checkbox" />
            In stock only
          </label>
        </section>
      </aside>

      <main class="catalog__main">
        <header class="catalog__head">
          <div>
            <h1 class="catalog__title">{{ heading }}</h1>
            <p class="catalog__subtitle">{{ subtitle }}</p>
            <p v-if="products.offline" class="alert alert--warning catalog__offline">
              Showing the offline catalog — the storefront service is not reachable right now.
            </p>
          </div>

          <div class="catalog__toolbar">
            <BaseButton
              class="catalog__filter-btn"
              variant="outline"
              size="sm"
              @click="ui.filterDrawerOpen = true"
            >
              <SlidersHorizontal :size="16" aria-hidden="true" />
              Filters
              <span v-if="products.activeFilterCount" class="catalog__filter-badge">
                {{ products.activeFilterCount }}
              </span>
            </BaseButton>

            <label class="visually-hidden" for="sort">Sort products</label>
            <select id="sort" v-model="sort" class="select catalog__sort">
              <option v-for="option in SORT_OPTIONS" :key="option.value" :value="option.value">
                {{ option.label }}
              </option>
            </select>
          </div>
        </header>

        <p class="catalog__count">
          {{ products.visibleProducts.length }} product{{ products.visibleProducts.length === 1 ? '' : 's' }}
          <template v-if="products.filters.keyword"> matching your search</template>
        </p>

        <ProductGrid
          :products="products.visibleProducts"
          :loading="products.loading"
          empty-title="No products found."
          empty-message="Nothing matched those filters. Try widening the price range or clearing the category."
          @reset="clearAll"
        />
      </main>
    </div>

    <!-- Mobile / tablet filter drawer -->
    <Teleport to="body">
      <Transition name="fade">
        <div v-if="ui.filterDrawerOpen" class="drawer-backdrop" @click="ui.filterDrawerOpen = false" />
      </Transition>
      <Transition name="fade">
        <div v-if="ui.filterDrawerOpen" class="drawer drawer--right" role="dialog" aria-label="Filters">
          <div class="drawer__head">
            <p class="drawer__title">Filters</p>
            <button class="icon-btn" type="button" aria-label="Close filters" @click="ui.filterDrawerOpen = false">
              <X :size="20" />
            </button>
          </div>
          <div class="drawer__body">
            <section class="filters__group">
              <h3 class="filters__legend">Category</h3>
              <ul class="filters__list">
                <li>
                  <button
                    class="filters__option"
                    :class="{ 'is-active': products.filters.category === null }"
                    type="button"
                    @click="applyCategory(null)"
                  >
                    All products
                  </button>
                </li>
                <li v-for="category in products.categories" :key="category.name">
                  <button
                    class="filters__option"
                    :class="{ 'is-active': products.filters.category === category.name }"
                    type="button"
                    @click="applyCategory(category.name)"
                  >
                    <span>{{ category.name }}</span>
                    <span class="filters__count">{{ category.count }}</span>
                  </button>
                </li>
              </ul>
            </section>
            <section class="filters__group">
              <h3 class="filters__legend">Price</h3>
              <div class="filters__range">
                <input
                  class="input"
                  type="number"
                  inputmode="decimal"
                  placeholder="Min"
                  :value="minPrice ?? ''"
                  aria-label="Minimum price"
                  @input="minPrice = ($event.target as HTMLInputElement).value === '' ? null : Number(($event.target as HTMLInputElement).value)"
                />
                <span class="filters__range-sep" aria-hidden="true">–</span>
                <input
                  class="input"
                  type="number"
                  inputmode="decimal"
                  placeholder="Max"
                  :value="maxPrice ?? ''"
                  aria-label="Maximum price"
                  @input="maxPrice = ($event.target as HTMLInputElement).value === '' ? null : Number(($event.target as HTMLInputElement).value)"
                />
              </div>
            </section>
            <section class="filters__group">
              <h3 class="filters__legend">Rating</h3>
              <ul class="filters__list">
                <li v-for="value in RATINGS" :key="value">
                  <button
                    class="filters__option"
                    :class="{ 'is-active': products.filters.minRating === value }"
                    type="button"
                    @click="applyRating(value)"
                  >
                    {{ value }} &amp; up
                  </button>
                </li>
              </ul>
            </section>
            <section class="filters__group">
              <label class="checkbox">
                <input v-model="products.filters.inStockOnly" type="checkbox" />
                In stock only
              </label>
            </section>
          </div>
          <div class="drawer__foot">
            <button class="btn btn--primary btn--block" type="button" @click="ui.filterDrawerOpen = false">
              <Check :size="17" aria-hidden="true" /> Show {{ products.visibleProducts.length }} results
            </button>
            <button class="btn btn--ghost btn--block" type="button" @click="clearAll">Clear all</button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </div>
</template>

<style scoped>
.catalog {
  padding-block: var(--space-8) var(--space-16);
}

.catalog__layout {
  display: grid;
  gap: var(--space-8);
}

.filters {
  display: none;
}

.catalog__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
}

.catalog__title {
  font-size: clamp(1.5rem, 3vw, var(--text-2xl));
}

.catalog__subtitle {
  color: var(--color-muted);
  margin-top: var(--space-2);
}

.catalog__offline {
  margin-top: var(--space-4);
  font-size: var(--text-sm);
}

.catalog__toolbar {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.catalog__sort {
  width: auto;
  min-width: 168px;
}

.catalog__filter-badge {
  display: grid;
  place-items: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  color: var(--color-inverse);
  font-size: 10px;
  font-weight: var(--weight-bold);
}

.catalog__count {
  margin-block: var(--space-4) var(--space-5);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.filters__head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
  padding-bottom: var(--space-4);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--space-5);
}

.filters__title {
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
}

.filters__clear {
  font-size: var(--text-sm);
  color: var(--color-accent);
}

.filters__group + .filters__group {
  margin-top: var(--space-6);
}

.filters__legend {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-muted);
  margin-bottom: var(--space-3);
}

.filters__list {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.filters__option {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  text-align: left;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.filters__option:hover {
  background: var(--color-surface-alt);
  color: var(--color-text);
}

.filters__option.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: var(--weight-semibold);
}

.filters__option--rating {
  justify-content: flex-start;
}

.filters__stars {
  display: inline-flex;
  gap: 1px;
  color: var(--color-border-strong);
}

.filters__stars .on {
  color: #f5a524;
}

.filters__count {
  font-size: var(--text-xs);
  color: var(--color-muted);
  font-variant-numeric: tabular-nums;
}

.filters__range {
  display: flex;
  align-items: center;
  gap: var(--space-2);
}

.filters__range-sep {
  color: var(--color-muted);
}

.drawer__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.drawer__title {
  font-weight: var(--weight-semibold);
  font-size: var(--text-md);
}

.drawer__body {
  flex: 1;
  overflow-y: auto;
  padding: var(--space-5);
}

.drawer__foot {
  padding: var(--space-4) var(--space-5);
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

@media (min-width: 900px) {
  .catalog__layout {
    grid-template-columns: 236px 1fr;
    gap: var(--space-10);
  }
  .filters {
    display: block;
    position: sticky;
    top: calc(var(--navbar-height) + var(--announcement-height) + var(--space-4));
    align-self: start;
  }
  .catalog__filter-btn {
    display: none;
  }
}
</style>
