<script setup lang="ts">
withDefaults(
  defineProps<{
    height?: string
    radius?: string
  }>(),
  { height: '1rem', radius: 'var(--radius-md)' },
)
</script>

<template>
  <div class="skeleton" :style="{ height, borderRadius: radius }" aria-hidden="true" />
</template>
