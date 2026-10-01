<script setup lang="ts">
import { RouterLink } from 'vue-router'
import { categoryImage } from '../../mocks/products'

defineProps<{
  category: string
  count?: number
  active?: boolean
}>()

const emit = defineEmits<{ select: [category: string] }>()
</script>

<template>
  <button
    class="category-card"
    :class="{ 'is-active': active }"
    type="button"
    :aria-pressed="active"
    @click="emit('select', category)"
  >
    <img :src="categoryImage(category)" :alt="`${category} category`" loading="lazy" width="600" height="600" />
    <span class="category-card__overlay" aria-hidden="true" />
    <span class="category-card__body">
      <span class="category-card__name">{{ category }}</span>
      <span class="category-card__count">{{ count ?? 0 }} products</span>
    </span>
  </button>
</template>

<style scoped>
.category-card {
  position: relative;
  display: block;
  aspect-ratio: 3 / 4;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--color-surface-alt);
  border: 1px solid var(--color-border);
  transition:
    transform var(--duration-base) var(--ease-out),
    box-shadow var(--duration-base) var(--ease-out);
}

.category-card:hover {
  transform: translateY(-4px);
  box-shadow: var(--shadow-lg);
}

.category-card.is-active {
  border-color: var(--color-primary);
  box-shadow: 0 0 0 2px var(--color-primary);
}

.category-card img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform var(--duration-slow) var(--ease-out);
}

.category-card:hover img {
  transform: scale(1.06);
}

.category-card__overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to top, rgba(16, 26, 53, 0.82) 0%, rgba(16, 26, 53, 0.18) 55%, transparent 100%);
}

.category-card__body {
  position: absolute;
  left: var(--space-4);
  right: var(--space-4);
  bottom: var(--space-4);
  display: flex;
  flex-direction: column;
  gap: 2px;
  text-align: left;
}

.category-card__name {
  color: var(--color-inverse);
  font-weight: var(--weight-semibold);
  font-size: var(--text-md);
  letter-spacing: -0.01em;
}

.category-card__count {
  color: rgba(255, 255, 255, 0.82);
  font-size: var(--text-sm);
}
</style>
