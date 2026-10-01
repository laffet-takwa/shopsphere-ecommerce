<script setup lang="ts">
import ProductCard from './ProductCard.vue'
import SkeletonBlock from '../common/SkeletonBlock.vue'
import EmptyState from '../common/EmptyState.vue'
import { PackageSearch } from '@lucide/vue'
import type { Product } from '../../types'

withDefaults(
  defineProps<{
    products: Product[]
    loading?: boolean
    skeletonCount?: number
    emptyTitle?: string
    emptyMessage?: string
  }>(),
  {
    loading: false,
    skeletonCount: 8,
    emptyTitle: 'No products found.',
    emptyMessage: 'Try a different search term or clear some filters.',
  },
)

const emit = defineEmits<{ reset: [] }>()
</script>

<template>
  <div>
    <!-- Skeletons keep the grid height stable so the page does not jump when data arrives. -->
    <div v-if="loading" class="grid-products">
      <div v-for="index in skeletonCount" :key="index" class="card product-skeleton">
        <SkeletonBlock height="0" radius="0" class="product-skeleton__media" />
        <div class="product-skeleton__body">
          <SkeletonBlock height="0.7rem" width="40%" />
          <SkeletonBlock height="1rem" width="85%" />
          <SkeletonBlock height="0.8rem" width="55%" />
          <SkeletonBlock height="2.4rem" />
        </div>
      </div>
    </div>

    <div v-else-if="products.length" class="grid-products">
      <ProductCard v-for="product in products" :key="product.id" :product="product" />
    </div>

    <EmptyState
      v-else
      :icon="PackageSearch"
      :title="emptyTitle"
      :message="emptyMessage"
      action-label="Clear filters"
      @action="emit('reset')"
    />
  </div>
</template>

<style scoped>
.product-skeleton {
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.product-skeleton__media {
  aspect-ratio: 1 / 1;
  border-radius: 0;
}

.product-skeleton__body {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  padding: var(--space-4);
}
</style>
