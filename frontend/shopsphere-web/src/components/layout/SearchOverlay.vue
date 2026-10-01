<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, X, Clock, TrendingUp } from '@lucide/vue'
import { useUiStore } from '../../stores/ui'
import { useProductStore } from '../../stores/product'
import { imageFor } from '../../mocks/products'

const ui = useUiStore()
const products = useProductStore()
const router = useRouter()

const term = ref('')
const input = ref<HTMLInputElement | null>(null)

const suggestions = computed(() => {
  const value = term.value.trim().toLowerCase()
  if (value.length < 2) return []
  const seen = new Set<string>()
  return products.allProducts
    .filter((product) => `${product.name} ${product.category}`.toLowerCase().includes(value))
    .filter((product) => {
      const key = product.name.toLowerCase()
      if (seen.has(key)) return false
      seen.add(key)
      return true
    })
    .slice(0, 6)
})

const matchingCategories = computed(() => {
  const value = term.value.trim().toLowerCase()
  if (!value) return []
  return products.categories.filter((category) => category.name.toLowerCase().includes(value)).slice(0, 4)
})

// Focus moves into the dialog as soon as it opens so keyboard users are not stranded behind it.
watch(
  () => ui.searchOpen,
  async (open) => {
    if (!open) {
      term.value = ''
      return
    }
    await nextTick()
    input.value?.focus()
  },
)

function submit(): void {
  const value = term.value.trim()
  if (!value) return
  products.pushRecent(value)
  ui.searchOpen = false
  router.push({ name: 'catalog', query: { keyword: value } })
}

function chooseProduct(id: number): void {
  ui.searchOpen = false
  router.push({ name: 'product-detail', params: { id } })
}

function chooseCategory(name: string): void {
  ui.searchOpen = false
  router.push({ name: 'catalog', query: { category: name } })
}

function onKeydown(event: KeyboardEvent): void {
  if (event.key === 'Escape') ui.searchOpen = false
}
</script>

<template>
  <Teleport to="body">
    <Transition name="fade">
      <div v-if="ui.searchOpen" class="search" role="dialog" aria-modal="true" aria-label="Search products">
        <div class="search__backdrop" @click="ui.searchOpen = false" />
        <div class="search__panel" @keydown="onKeydown">
          <form class="search__field" role="search" @submit.prevent="submit">
            <Search :size="20" aria-hidden="true" />
            <input
              ref="input"
              v-model="term"
              class="search__input"
              type="search"
              placeholder="Search headphones, laptops, cookware…"
              aria-label="Search products"
              autocomplete="off"
            />
            <button class="icon-btn" type="button" aria-label="Close search" @click="ui.searchOpen = false">
              <X :size="20" />
            </button>
          </form>

          <div v-if="term.trim().length < 2" class="search__body">
            <template v-if="products.recentSearches.length">
              <div class="search__section">
                <div class="search__section-head">
                  <p class="search__label"><Clock :size="15" aria-hidden="true" /> Recent searches</p>
                  <button class="search__clear" type="button" @click="products.clearRecent">Clear</button>
                </div>
                <div class="search__chips">
                  <button
                    v-for="item in products.recentSearches"
                    :key="item"
                    class="chip"
                    type="button"
                    @click="term = item"
                  >
                    {{ item }}
                  </button>
                </div>
              </div>
            </template>
            <div class="search__section">
              <p class="search__label"><TrendingUp :size="15" aria-hidden="true" /> Popular categories</p>
              <div class="search__chips">
                <button
                  v-for="category in products.categories.slice(0, 6)"
                  :key="category.name"
                  class="chip"
                  type="button"
                  @click="chooseCategory(category.name)"
                >
                  {{ category.name }}
                </button>
              </div>
            </div>
          </div>

          <div v-else class="search__body">
            <div v-if="suggestions.length" class="search__section">
              <p class="search__label">Products</p>
              <ul class="search__results">
                <li v-for="product in suggestions" :key="product.id">
                  <button class="search__result" type="button" @click="chooseProduct(product.id)">
                    <img :src="imageFor(product)" alt="" width="48" height="48" />
                    <span class="search__result-body">
                      <span class="search__result-name">{{ product.name }}</span>
                      <span class="search__result-meta">{{ product.category }}</span>
                    </span>
                    <span class="price search__result-price">{{ product.price.toFixed(2) }}</span>
                  </button>
                </li>
              </ul>
            </div>

            <div v-if="matchingCategories.length" class="search__section">
              <p class="search__label">Categories</p>
              <div class="search__chips">
                <button
                  v-for="category in matchingCategories"
                  :key="category.name"
                  class="chip"
                  type="button"
                  @click="chooseCategory(category.name)"
                >
                  {{ category.name }}
                </button>
              </div>
            </div>

            <p v-if="!suggestions.length && !matchingCategories.length" class="search__empty">
              No results for “{{ term }}”. Try a shorter or more general term.
            </p>

            <button class="btn btn--primary btn--block search__submit" type="button" @click="submit">
              Search for “{{ term.trim() }}”
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.search {
  position: fixed;
  inset: 0;
  z-index: var(--z-modal);
}

.search__backdrop {
  position: absolute;
  inset: 0;
  background: var(--color-overlay);
}

.search__panel {
  position: relative;
  width: min(680px, calc(100vw - var(--space-8)));
  margin: 10vh auto 0;
  background: var(--color-surface);
  border-radius: var(--radius-xl);
  box-shadow: var(--shadow-lg);
  overflow: hidden;
  animation: modal-in var(--duration-base) var(--ease-out);
  max-height: 76vh;
  display: flex;
  flex-direction: column;
}

.search__field {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border);
  color: var(--color-muted);
}

.search__input {
  flex: 1;
  border: none;
  outline: none;
  background: none;
  font-size: var(--text-md);
  color: var(--color-text);
}

.search__input::-webkit-search-cancel-button {
  display: none;
}

.search__body {
  padding: var(--space-4) var(--space-5) var(--space-5);
  overflow-y: auto;
}

.search__section + .search__section {
  margin-top: var(--space-5);
}

.search__section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.search__label {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-muted);
  margin-bottom: var(--space-3);
}

.search__clear {
  font-size: var(--text-xs);
  color: var(--color-accent);
}

.search__chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.chip {
  padding: 6px var(--space-4);
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  transition:
    border-color var(--duration-fast) var(--ease-out),
    background-color var(--duration-fast) var(--ease-out);
}

.chip:hover {
  border-color: var(--color-primary);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.search__results {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.search__result {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-2);
  border-radius: var(--radius-md);
  text-align: left;
  transition: background-color var(--duration-fast) var(--ease-out);
}

.search__result:hover {
  background: var(--color-surface-alt);
}

.search__result img {
  width: 48px;
  height: 48px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.search__result-body {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.search__result-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.search__result-meta {
  font-size: var(--text-xs);
  color: var(--color-muted);
}

.search__result-price {
  font-size: var(--text-sm);
}

.search__empty {
  font-size: var(--text-sm);
  color: var(--color-muted);
  padding: var(--space-4) 0;
}

.search__submit {
  margin-top: var(--space-5);
}
</style>
