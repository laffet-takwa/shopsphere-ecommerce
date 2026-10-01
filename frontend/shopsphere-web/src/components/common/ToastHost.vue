<script setup lang="ts">
import { CheckCircle2, AlertCircle, Info, X } from '@lucide/vue'
import { useUiStore } from '../../stores/ui'

const ui = useUiStore()

const icons = { success: CheckCircle2, error: AlertCircle, info: Info } as const
</script>

<template>
  <Teleport to="body">
    <div class="toast-host" aria-live="polite" aria-atomic="false">
      <TransitionGroup name="toast">
        <div v-for="toast in ui.toasts" :key="toast.id" class="toast" :class="`toast--${toast.tone}`">
          <component :is="icons[toast.tone]" :size="18" aria-hidden="true" />
          <p class="toast__message">{{ toast.message }}</p>
          <button class="toast__close" type="button" aria-label="Dismiss" @click="ui.dismiss(toast.id)">
            <X :size="16" />
          </button>
        </div>
      </TransitionGroup>
    </div>
  </Teleport>
</template>

<style scoped>
.toast-host {
  position: fixed;
  right: var(--space-5);
  bottom: var(--space-5);
  z-index: var(--z-toast);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
  max-width: min(380px, calc(100vw - var(--space-8)));
  pointer-events: none;
}

.toast {
  display: flex;
  align-items: flex-start;
  gap: var(--space-3);
  padding: var(--space-4);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-left: 3px solid var(--color-info);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-lg);
  pointer-events: auto;
}

.toast--success {
  border-left-color: var(--color-success);
  color: var(--color-success);
}
.toast--error {
  border-left-color: var(--color-danger);
  color: var(--color-danger);
}
.toast--info {
  border-left-color: var(--color-info);
  color: var(--color-info);
}

.toast__message {
  flex: 1;
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  line-height: var(--leading-snug);
}

.toast__close {
  color: var(--color-muted);
  display: inline-flex;
  padding: 2px;
  border-radius: var(--radius-sm);
}
.toast__close:hover {
  color: var(--color-text);
}

.toast-enter-active,
.toast-leave-active {
  transition:
    opacity var(--duration-base) var(--ease-out),
    transform var(--duration-base) var(--ease-out);
}
.toast-enter-from,
.toast-leave-to {
  opacity: 0;
  transform: translateY(10px);
}

@media (max-width: 640px) {
  .toast-host {
    left: var(--space-4);
    right: var(--space-4);
    bottom: var(--space-4);
    max-width: none;
  }
}
</style>
