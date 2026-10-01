<script setup lang="ts">
import type { RouteLocationRaw } from 'vue-router'

withDefaults(
  defineProps<{
    variant?: 'primary' | 'accent' | 'secondary' | 'outline' | 'ghost' | 'danger'
    size?: 'sm' | 'md' | 'lg'
    type?: 'button' | 'submit' | 'reset'
    disabled?: boolean
    loading?: boolean
    block?: boolean
    to?: RouteLocationRaw
  }>(),
  {
    variant: 'primary',
    size: 'md',
    type: 'button',
    disabled: false,
    loading: false,
    block: false,
    to: undefined,
  },
)

/**
 * Buttons are real <button> elements so keyboard activation and screen-reader semantics work for
 * free. A `to` prop renders a router-link instead, which keeps navigation semantics correct rather
 * than faking a link with a click handler.
 */
</script>

<template>
  <RouterLink v-if="to" :to="to" class="btn" :class="[`btn--${variant}`, `btn--${size}`, { 'btn--block': block }]">
    <span v-if="loading" class="btn__spinner" aria-hidden="true" />
    <slot />
  </RouterLink>
  <button
    v-else
    :type="type"
    class="btn"
    :class="[`btn--${variant}`, `btn--${size}`, { 'btn--block': block }]"
    :disabled="disabled || loading"
    :aria-busy="loading || undefined"
  >
    <span v-if="loading" class="btn__spinner" aria-hidden="true" />
    <slot />
  </button>
</template>
