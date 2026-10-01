<script setup lang="ts">
import { Star } from '@lucide/vue'
import { computed } from 'vue'

const props = withDefaults(
  defineProps<{
    rating: number
    reviews?: number
    size?: number
  }>(),
  { reviews: undefined, size: 14 },
)

const percent = computed(() => Math.max(0, Math.min(100, (props.rating / 5) * 100)))
</script>

<template>
  <span class="rating" :aria-label="`Rated ${rating} out of 5${reviews ? ` from ${reviews} reviews` : ''}`">
    <span class="rating__track" :style="{ width: `${size + 2}px`, height: `${size}px` }" aria-hidden="true">
      <Star class="rating__star" :size="size" />
    </span>
    <span class="rating__track" :style="{ width: `${size + 2}px`, height: `${size}px` }" aria-hidden="true">
      <Star class="rating__star rating__star--on" :size="size" :fill="'currentColor'" :style="{ clipPath: `inset(0 ${100 - percent}% 0 0)` }" />
    </span>
    <strong class="rating__value">{{ rating.toFixed(1) }}</strong>
    <span v-if="reviews" class="rating__count">({{ reviews }})</span>
  </span>
</template>

<style scoped>
.rating__track {
  position: relative;
  display: inline-block;
  flex-shrink: 0;
}
.rating__value {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-text);
  margin-left: var(--space-2);
}
</style>
