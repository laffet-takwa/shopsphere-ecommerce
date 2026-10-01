<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    label: string
    value: string | number
    icon?: Component
    tone?: 'default' | 'success' | 'warning' | 'danger'
    hint?: string
  }>(),
  { icon: undefined, tone: 'default', hint: '' },
)
</script>

<template>
  <article class="stat" :class="`stat--${tone}`">
    <div class="stat__head">
      <span class="stat__label">{{ label }}</span>
      <span v-if="icon" class="stat__icon" aria-hidden="true">
        <component :is="icon" :size="18" />
      </span>
    </div>
    <p class="stat__value">{{ value }}</p>
    <p v-if="hint" class="stat__hint">{{ hint }}</p>
  </article>
</template>

<style scoped>
.stat {
  padding: var(--space-5);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xs);
}

.stat__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
}

.stat__label {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-muted);
}

.stat__icon {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
  color: var(--color-text-secondary);
}

.stat__value {
  margin-top: var(--space-3);
  font-size: var(--text-2xl);
  font-weight: var(--weight-bold);
  letter-spacing: -0.02em;
  font-variant-numeric: tabular-nums;
  color: var(--color-text);
}

.stat__hint {
  margin-top: var(--space-1);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.stat--success .stat__value {
  color: var(--color-success);
}
.stat--warning .stat__value {
  color: var(--color-warning);
}
.stat--danger .stat__value {
  color: var(--color-danger);
}
</style>
