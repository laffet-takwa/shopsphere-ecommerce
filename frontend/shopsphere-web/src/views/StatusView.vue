<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { PackageSearch, ShieldAlert, WifiOff, Home, RotateCcw } from '@lucide/vue'
import BaseButton from '../components/common/BaseButton.vue'

const props = withDefaults(
  defineProps<{
    code?: string
    title?: string
    message?: string
    kind?: 'not-found' | 'server' | 'network'
  }>(),
  {
    code: '404',
    title: 'Page not found',
    message: 'The page you are looking for has moved or never existed.',
    kind: 'not-found',
  },
)

const router = useRouter()

const CONFIG = {
  'not-found': { icon: PackageSearch, retry: false },
  server: { icon: ShieldAlert, retry: true },
  network: { icon: WifiOff, retry: true },
} as const

const icon = computed(() => CONFIG[props.kind].icon)
const canRetry = computed(() => CONFIG[props.kind].retry)
</script>

<template>
  <div class="status container">
    <component :is="icon" class="status__icon" :size="44" :stroke-width="1.5" aria-hidden="true" />
    <p v-if="kind === 'not-found'" class="status__code">{{ code }}</p>
    <h1 class="status__title">{{ title }}</h1>
    <p class="status__message">{{ message }}</p>
    <div class="status__actions">
      <BaseButton v-if="canRetry" variant="primary" size="lg" @click="router.go(0)">
        <RotateCcw :size="18" aria-hidden="true" /> Try again
      </BaseButton>
      <BaseButton variant="primary" size="lg" @click="router.push('/')">
        <Home :size="18" aria-hidden="true" /> Go home
      </BaseButton>
      <BaseButton variant="outline" size="lg" to="/products">Browse products</BaseButton>
    </div>
  </div>
</template>

<style scoped>
.status {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  gap: var(--space-3);
  padding-block: clamp(var(--space-16), 14vw, var(--space-20));
}

.status__icon {
  color: var(--color-border-strong);
}

.status__code {
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
  letter-spacing: var(--tracking-wider);
  color: var(--color-accent);
}

.status__title {
  font-size: clamp(1.6rem, 3.6vw, var(--text-3xl));
}

.status__message {
  max-width: 44ch;
  color: var(--color-muted);
}

.status__actions {
  display: flex;
  flex-wrap: wrap;
  justify-content: center;
  gap: var(--space-3);
  margin-top: var(--space-4);
}
</style>
