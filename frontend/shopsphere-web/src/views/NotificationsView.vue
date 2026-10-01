<script setup lang="ts">
import { onMounted } from 'vue'
import { Bell, CheckCheck } from '@lucide/vue'
import EmptyState from '../components/common/EmptyState.vue'
import BaseButton from '../components/common/BaseButton.vue'
import SkeletonBlock from '../components/common/SkeletonBlock.vue'
import { useNotificationStore, type NotificationTone } from '../stores/notification'
import type { Notification } from '../types'

const notifications = useNotificationStore()

const TONE_LABEL: Record<NotificationTone, string> = {
  order: 'Order update',
  payment: 'Payment',
  promotion: 'Promotion',
  system: 'System',
}

function iconFor(type: string): NotificationTone {
  return notifications.toneFor(type)
}

function relative(value: string): string {
  const diff = Date.now() - new Date(value).getTime()
  const minutes = Math.round(diff / 60000)
  if (minutes < 1) return 'just now'
  if (minutes < 60) return `${minutes}m ago`
  const hours = Math.round(minutes / 60)
  if (hours < 24) return `${hours}h ago`
  const days = Math.round(hours / 24)
  return `${days}d ago`
}

onMounted(() => {
  void notifications.fetchAll()
})
</script>

<template>
  <div class="page container">
    <header class="page__head">
      <div>
        <h1 class="page__title">Notifications</h1>
        <p class="page__subtitle">
          Updates published to your account as orders move through the platform.
        </p>
      </div>
      <BaseButton
        v-if="notifications.unreadCount > 0"
        variant="outline"
        size="sm"
        @click="notifications.markAllRead()"
      >
        <CheckCheck :size="16" aria-hidden="true" /> Mark all read
      </BaseButton>
    </header>

    <div v-if="notifications.loading" class="stack">
      <SkeletonBlock v-for="index in 4" :key="index" height="76px" radius="var(--radius-lg)" />
    </div>

    <EmptyState
      v-else-if="notifications.recent.length === 0"
      :icon="Bell"
      title="You're all caught up."
      message="Order and payment updates will appear here as soon as something happens."
      action-label="Start shopping"
      to="/products"
    />

    <ul v-else class="list">
      <li
        v-for="item in notifications.recent"
        :key="item.id"
        class="item"
        :class="{ 'is-unread': !item.read }"
      >
        <span class="item__icon" :data-tone="iconFor(item.type)" aria-hidden="true">
          <Bell :size="16" />
        </span>
        <div class="item__body">
          <p class="item__label">{{ TONE_LABEL[iconFor(item.type)] }}</p>
          <p class="item__message">{{ item.message }}</p>
          <p class="item__time">{{ relative(item.createdAt) }}</p>
        </div>
        <BaseButton
          v-if="!item.read"
          variant="ghost"
          size="sm"
          @click="notifications.markRead(item.id)"
        >
          Mark read
        </BaseButton>
      </li>
    </ul>
  </div>
</template>

<style scoped>
.page {
  padding-block: var(--space-8) var(--space-16);
}

.page__head {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-4);
  margin-bottom: var(--space-6);
}

.page__title {
  font-size: clamp(1.6rem, 3.4vw, var(--text-2xl));
}

.page__subtitle {
  margin-top: var(--space-2);
  color: var(--color-muted);
}

.stack {
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.item {
  display: flex;
  align-items: flex-start;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.item.is-unread {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}

.item__icon {
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-muted);
}

.item__icon[data-tone='order'] {
  color: var(--color-accent);
}
.item__icon[data-tone='payment'] {
  color: var(--color-success);
}

.item__body {
  flex: 1;
  min-width: 0;
}

.item__label {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-muted);
}

.item__message {
  margin-top: 2px;
  font-size: var(--text-base);
  color: var(--color-text);
}

.item__time {
  margin-top: var(--space-1);
  font-size: var(--text-xs);
  color: var(--color-muted);
}
</style>
