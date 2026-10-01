<script setup lang="ts">
import type { Component } from 'vue'

withDefaults(
  defineProps<{
    title: string
    message?: string
    icon?: Component
    actionLabel?: string
    to?: string
  }>(),
  { message: '', icon: undefined, actionLabel: '', to: '' },
)

const emit = defineEmits<{ action: [] }>()
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__icon" aria-hidden="true">
      <component :is="icon" v-if="icon" :size="26" />
      <svg v-else width="26" height="26" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75">
        <circle cx="11" cy="11" r="7" />
        <path d="m20 20-3.5-3.5" stroke-linecap="round" />
      </svg>
    </div>
    <h3 class="empty-state__title">{{ title }}</h3>
    <p v-if="message" class="empty-state__message">{{ message }}</p>
    <RouterLink v-if="to && actionLabel" class="btn btn--primary" :to="to">{{ actionLabel }}</RouterLink>
    <button v-else-if="actionLabel" class="btn btn--primary" type="button" @click="emit('action')">
      {{ actionLabel }}
    </button>
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-3);
  padding: var(--space-12) var(--space-6);
}

.empty-state__icon {
  display: grid;
  place-items: center;
  width: 56px;
  height: 56px;
  border-radius: var(--radius-full);
  background: var(--color-surface-alt);
  color: var(--color-muted);
  margin-bottom: var(--space-2);
}

.empty-state__title {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
}

.empty-state__message {
  color: var(--color-muted);
  max-width: 42ch;
}
</style>
