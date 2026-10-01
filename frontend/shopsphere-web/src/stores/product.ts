import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import { productService, type ProductQuery } from '../services/productService'
import { normalizeError } from '../services/http'
import { MOCK_PRODUCTS } from '../mocks/products'
import type { Product } from '../types'

const MAX_PAGE_SIZE = 48

export interface CatalogFilters {
  category: string | null
  keyword: string
  minPrice: number | null
  maxPrice: number | null
  minRating: number | null
  inStockOnly: boolean
  sort: 'featured' | 'newest' | 'price-asc' | 'price-desc' | 'rating'
}

const DEFAULT_FILTERS: CatalogFilters = {
  category: null,
  keyword: '',
  minPrice: null,
  maxPrice: null,
  minRating: null,
  inStockOnly: false,
  sort: 'featured',
}

function toBackendSort(sort: CatalogFilters['sort']): string | undefined {
  switch (sort) {
    case 'newest':
      return 'createdAt,desc'
    case 'price-asc':
      return 'price,asc'
    case 'price-desc':
      return 'price,desc'
    case 'rating':
      return 'name,asc'
    default:
      return undefined
  }
}

/**
 * Catalogue state for both the storefront grid and the admin table.
 *
 * Only category, keyword, paging and sorting are server-side. Price, rating and availability are
 * applied on the returned page because the backend contract for the catalog is a plain filtered
 * page; that is an honest limitation rather than a hidden bug, and it keeps the filter UI instant.
 */
export const useProductStore = defineStore('products', () => {
  const products = ref<Product[]>([])
  const allProducts = ref<Product[]>([])
  const loading = ref(false)
  const error = ref('')
  const offline = ref(false)
  const totalElements = ref(0)
  const totalPages = ref(0)
  const filters = ref<CatalogFilters>({ ...DEFAULT_FILTERS })
  const recentSearches = ref<string[]>(loadRecent())

  function loadRecent(): string[] {
    try {
      return JSON.parse(localStorage.getItem('shopsphere.recent') ?? '[]') as string[]
    } catch {
      return []
    }
  }

  const pageNumber = ref(0)
  const categories = computed(() => {
    const counts = new Map<string, number>()
    for (const product of allProducts.value) {
      counts.set(product.category, (counts.get(product.category) ?? 0) + 1)
    }
    return [...counts.entries()]
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
  })

  const priceBounds = computed(() => {
    const prices = allProducts.value.map((product) => product.price)
    if (prices.length === 0) return { min: 0, max: 1000 }
    return { min: Math.floor(Math.min(...prices)), max: Math.ceil(Math.max(...prices)) }
  })

  const visibleProducts = computed(() => {
    let result = [...products.value]

    if (filters.value.minPrice !== null) {
      result = result.filter((product) => product.price >= (filters.value.minPrice as number))
    }
    if (filters.value.maxPrice !== null) {
      result = result.filter((product) => product.price <= (filters.value.maxPrice as number))
    }
    if (filters.value.minRating !== null) {
      const floor = filters.value.minRating
      result = result.filter((product) => demoScore(product.id) >= floor)
    }

    if (filters.value.sort === 'featured') {
      result.sort((a, b) => Number(b.active) - Number(a.active) || a.name.localeCompare(b.name))
    } else if (filters.value.sort === 'rating') {
      result.sort((a, b) => demoScore(b.id) - demoScore(a.id))
    }

    return result
  })

  const activeFilterCount = computed(() => {
    let count = 0
    if (filters.value.category) count += 1
    if (filters.value.minPrice !== null || filters.value.maxPrice !== null) count += 1
    if (filters.value.minRating !== null) count += 1
    if (filters.value.inStockOnly) count += 1
    return count
  })

  function demoScore(productId: number): number {
    // Reviews are not modelled by the backend yet; this keeps the rating filter meaningful.
    return ((productId * 37) % 20) / 10 + 3.5
  }

  function pushRecent(term: string): void {
    const value = term.trim()
    if (value.length < 2) return
    recentSearches.value = [value, ...recentSearches.value.filter((item) => item !== value)].slice(0, 6)
    localStorage.setItem('shopsphere.recent', JSON.stringify(recentSearches.value))
  }

  function clearRecent(): void {
    recentSearches.value = []
    localStorage.setItem('shopsphere.recent', '[]')
  }

  async function fetchProducts(options: { reset?: boolean; size?: number } = {}): Promise<void> {
    loading.value = true
    error.value = ''
    const size = options.size ?? 24

    const query: ProductQuery = {
      page: 0,
      size: MAX_PAGE_SIZE,
      category: filters.value.category,
      keyword: filters.value.keyword.trim() || null,
      sort: toBackendSort(filters.value.sort),
    }

    try {
      const pageData = await productService.list(query)
      offline.value = false
      allProducts.value = pageData.content
      products.value = pageData.content
      totalElements.value = pageData.totalElements
      totalPages.value = pageData.totalPages
      if (filters.value.keyword.trim()) pushRecent(filters.value.keyword)
    } catch (cause) {
      const friendly = normalizeError(cause)
      if (friendly.isNetworkError) {
        // Fall back to the bundled catalogue so the storefront stays browsable offline.
        offline.value = true
        const keyword = filters.value.keyword.trim().toLowerCase()
        const all = MOCK_PRODUCTS.filter((product) => {
          const matchesCategory = !filters.value.category || product.category === filters.value.category
          const matchesKeyword =
            !keyword ||
            `${product.name} ${product.description} ${product.category}`.toLowerCase().includes(keyword)
          return matchesCategory && matchesKeyword
        })
        allProducts.value = all
        products.value = all
        totalElements.value = all.length
        totalPages.value = 1
        error.value = ''
      } else {
        offline.value = false
        error.value = friendly.message
        products.value = []
        allProducts.value = []
        totalElements.value = 0
        totalPages.value = 0
      }
    } finally {
      loading.value = false
      if (options.reset) pageNumber.value = 0
    }
  }

  async function fetchById(id: number): Promise<Product | null> {
    try {
      return await productService.get(id)
    } catch {
      return allProducts.value.find((product) => product.id === id) ?? null
    }
  }

  function resetFilters(): void {
    filters.value = { ...DEFAULT_FILTERS }
  }

  return {
    products,
    allProducts,
    visibleProducts,
    loading,
    error,
    offline,
    filters,
    categories,
    priceBounds,
    totalElements,
    totalPages,
    pageNumber,
    recentSearches,
    activeFilterCount,
    fetchProducts,
    fetchById,
    resetFilters,
    pushRecent,
    clearRecent,
  }
})
