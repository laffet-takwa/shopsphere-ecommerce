<script setup lang="ts">
import { Settings, Info, ExternalLink } from '@lucide/vue'
import AdminShell from '../../components/admin/AdminShell.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import BaseBadge from '../../components/common/BaseBadge.vue'
import { useAuthStore } from '../../stores/auth'

const auth = useAuthStore()

const services = [
  { name: 'discovery-server', role: 'Eureka registry', port: 8761 },
  { name: 'api-gateway', role: 'Routing, JWT verification, role checks', port: 8080 },
  { name: 'auth-service', role: 'Users, BCrypt, JWT issuance', port: 8081 },
  { name: 'product-service', role: 'Catalog and search (PostgreSQL)', port: 8082 },
  { name: 'order-service', role: 'Orders, event producers (PostgreSQL)', port: 8083 },
  { name: 'inventory-service', role: 'Stock and reservations (MongoDB)', port: 8084 },
  { name: 'payment-service', role: 'Simulated payments (PostgreSQL)', port: 8085 },
  { name: 'notification-service', role: 'Event notifications (MongoDB)', port: 8086 },
]

const topics = [
  'order.created',
  'inventory.reserved',
  'inventory.insufficient',
  'payment.completed',
  'payment.failed',
  'order.shipped',
]
</script>

<template>
  <AdminShell title="Settings">
    <section class="card card--pad block">
      <h2 class="block__title">
        <Settings :size="18" aria-hidden="true" /> Account
      </h2>
      <dl class="rows">
        <div>
          <dt>Name</dt>
          <dd>{{ auth.displayName || 'Administrator' }}</dd>
        </div>
        <div>
          <dt>Email</dt>
          <dd>{{ auth.user?.email ?? '—' }}</dd>
        </div>
        <div>
          <dt>Role</dt>
          <dd><BaseBadge tone="info">{{ auth.user?.role ?? 'ADMIN' }}</BaseBadge></dd>
        </div>
      </dl>
      <p class="block__note">
        <Info :size="15" aria-hidden="true" />
        The backend exposes register, login and profile read only. There is no settings endpoint to
        write to, so this page is intentionally read-only.
      </p>
    </section>

    <section class="card card--pad block">
      <h2 class="block__title">
        <Info :size="18" aria-hidden="true" /> Registered services
      </h2>
      <div class="table-wrap">
        <table class="table responsive-table">
          <thead>
            <tr>
              <th>Service</th>
              <th>Responsibility</th>
              <th>Port</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="service in services" :key="service.name">
              <td data-label="Service"><code>{{ service.name }}</code></td>
              <td data-label="Responsibility">{{ service.role }}</td>
              <td data-label="Port" class="table__num">{{ service.port }}</td>
            </tr>
          </tbody>
        </table>
      </div>
      <BaseButton to="http://localhost:8761" variant="outline" size="sm">
        Open Eureka dashboard <ExternalLink :size="15" aria-hidden="true" />
      </BaseButton>
    </section>

    <section class="card card--pad block">
      <h2 class="block__title">Kafka topics</h2>
      <div class="chips">
        <span v-for="topic in topics" :key="topic" class="chip">{{ topic }}</span>
      </div>
      <p class="block__note">
        Topics are provisioned by the <code>kafka-init</code> Compose service and the
        <code>kafka-create-topics</code> Kubernetes job, each with three partitions.
      </p>
    </section>
  </AdminShell>
</template>

<style scoped>
.block + .block {
  margin-top: var(--space-5);
}

.block__title {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-4);
}

.block__title + .table-wrap {
  margin-bottom: var(--space-4);
}

.rows > div {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  padding-block: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.rows dt {
  color: var(--color-muted);
}

.rows dd {
  font-weight: var(--weight-medium);
}

.block__note {
  display: flex;
  align-items: flex-start;
  gap: var(--space-2);
  margin-top: var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.block__note svg {
  flex-shrink: 0;
  margin-top: 2px;
}

code {
  font-family: var(--font-mono);
  font-size: 0.85em;
  padding: 1px 4px;
  border-radius: 4px;
  background: var(--color-surface-alt);
}

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
}

.chip {
  padding: 4px var(--space-3);
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  font-family: var(--font-mono);
  font-size: var(--text-xs);
  color: var(--color-text-secondary);
}
</style>
