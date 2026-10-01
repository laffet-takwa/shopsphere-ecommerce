<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { imageFor, fallbackImage } from '../../mocks/products'
import type { Product } from '../../types'

const props = defineProps<{ product: Product }>()

/**
 * Gallery views are derived from the single product image the API exposes plus category-matched
 * variants, so the gallery is never a dead control while the backend still stores one image per
 * product. A real implementation would receive an array of URLs from the API.
 */
const views = computed(() => [
  imageFor(props.product),
  fallbackImage(props.product.category),
  fallbackImage('Accessories'),
  fallbackImage('Home & Living'),
])

const active = ref(0)

watch(
  () => props.product.id,
  () => {
    active.value = 0
  },
)
</script>

<template>
  <div class="gallery">
    <div class="gallery__stage">
      <img :src="views[active]" :alt="product.name" width="800" height="800" />
      <span class="gallery__counter">{{ active + 1 }} / {{ views.length }}</span>
    </div>
    <div class="gallery__thumbs" role="tablist" aria-label="Product images">
      <button
        v-for="(view, index) in views"
        :key="view + index"
        class="gallery__thumb"
        :class="{ 'is-active': index === active }"
        type="button"
        role="tab"
        :aria-selected="index === active"
        :aria-label="`Show image ${index + 1}`"
        @click="active = index"
      >
        <img :src="view" alt="" width="120" height="120" loading="lazy" />
      </button>
    </div>
  </div>
</template>

<style scoped>
.gallery {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.gallery__stage {
  position: relative;
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: var(--color-surface-alt);
  border: 1px solid var(--color-border);
}

.gallery__stage img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery__counter {
  position: absolute;
  bottom: var(--space-4);
  right: var(--space-4);
  padding: 4px var(--space-3);
  border-radius: var(--radius-full);
  background: rgba(16, 26, 53, 0.72);
  color: var(--color-inverse);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  font-variant-numeric: tabular-nums;
}

.gallery__thumbs {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: var(--space-3);
}

.gallery__thumb {
  aspect-ratio: 1 / 1;
  border-radius: var(--radius-md);
  overflow: hidden;
  border: 2px solid var(--color-border);
  background: var(--color-surface-alt);
  transition: border-color var(--duration-fast) var(--ease-out);
}

.gallery__thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.gallery__thumb.is-active {
  border-color: var(--color-primary);
}

.gallery__thumb:hover {
  border-color: var(--color-muted);
}
</style>
