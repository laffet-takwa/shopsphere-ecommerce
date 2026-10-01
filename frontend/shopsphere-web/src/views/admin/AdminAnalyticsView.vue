<script setup lang="ts">
import { computed } from 'vue'
import { BarChart3 } from '@lucide/vue'
import AdminShell from '../../components/admin/AdminShell.vue'
import MiniBarChart from '../../components/admin/MiniBarChart.vue'
import StatCard from '../../components/admin/StatCard.vue'
import OrderStatusBadge from '../../components/order/OrderStatusBadge.vue'
import { useAdminStore } from '../../stores/admin'

const admin = useAdminStore()

const avgOrder = computed(() => {
  const paid = admin.orders.filter((order) => order.status !== 'CANCELLED')
  if (paid.length === 0) return 0
  return paid.reduce((sum, order) => sum + Number(order.totalAmount), 0) / paid.length
})

const cancelRate = computed(() => {
  if (admin.orders.length === 0) return 0
  const cancelled = admin.orders.filter((order) => order.status === 'CANCELLED').length
  return Math.round((cancelled / admin.orders.length) * 100)
})

const unitsByCategory = computed(() => {
  const map = new Map<string, number>()
  for (const order of admin.orders) {
    if (order.status === 'CANCELLED') continue
    for (const item of order.items) {
      const category = admin.products.find((product) => product.id === item.productId)?.category ?? 'Other'
      map.set(category, (map.get(category) ?? 0) + item.quantity)
    }
  }
  return [...map.entries()]
    .map(([label, value]) => ({ label, value }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 6)
})

function formatMoney(value: number): string {
  return value.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })
}
</script>

<template>
  <AdminShell title="Analytics">
    <div class="grid">
      <StatCard label="Revenue" :value="formatMoney(admin.metrics.revenue)" tone="success" />
      <StatCard label="Average order" :value="formatMoney(avgOrder)" />
      <StatCard label="Cancellation rate" :value="`${cancelRate}%`" :tone="cancelRate > 10 ? 'warning' : 'default'" />
      <StatCard label="Catalogue size" :value="admin.metrics.productCount" />
    </div>

    <section class="card card--pad">
      <h2 class="panel__title">Daily revenue</h2>
      <MiniBarChart :data="admin.revenueSeries" :height="220" />
    </section>

    <div class="cards">
      <section class="card card--pad">
        <h2 class="panel__title">Units sold by category</h2>
        <MiniBarChart v-if="unitsByCategory.length" :data="unitsByCategory" :height="200" format="number" />
        <p v-else class="muted">No sales recorded yet.</p>
      </section>

      <section class="card card--pad">
        <h2 class="panel__title">Order mix</h2>
        <ul v-if="admin.orderStatusSeries.length" class="mix">
          <li v-for="entry in admin.orderStatusSeries" :key="entry.status" class="mix__row">
            <OrderStatusBadge :status="entry.status as never" />
            <span class="mix__value">{{ entry.value }}</span>
          </li>
        </ul>
        <p v-else class="muted">No orders yet.</p>
      </section>
    </div>
  </AdminShell>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.panel__title {
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-5);
}

.cards {
  display: grid;
  gap: var(--space-5);
  margin-top: var(--space-5);
}

.mix__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding-block: var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.mix__row:last-child {
  border-bottom: none;
}

.mix__value {
  font-weight: var(--weight-bold);
  font-variant-numeric: tabular-nums;
}

.muted {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

@media (min-width: 1000px) {
  .grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .cards {
    grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  }
}
</style>
