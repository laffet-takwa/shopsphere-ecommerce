<script setup lang="ts">
import { computed } from 'vue'
import BaseBadge from '../common/BaseBadge.vue'
import type { OrderStatus } from '../../types'

const props = defineProps<{ status: OrderStatus }>()

const TONE: Record<OrderStatus, 'neutral' | 'success' | 'warning' | 'danger' | 'info' | 'solid'> = {
  PENDING: 'warning',
  CONFIRMED: 'info',
  PAID: 'info',
  PROCESSING: 'info',
  SHIPPED: 'solid',
  DELIVERED: 'success',
  CANCELLED: 'danger',
}

const tone = computed(() => TONE[props.status] ?? 'neutral')
</script>

<template>
  <BaseBadge :tone="tone" dot>{{ status.charAt(0) + status.slice(1).toLowerCase() }}</BaseBadge>
</template>
