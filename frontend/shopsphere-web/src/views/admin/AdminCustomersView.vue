<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Users, Search } from '@lucide/vue'
import AdminShell from '../../components/admin/AdminShell.vue'
import StatCard from '../../components/admin/StatCard.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import BaseBadge from '../../components/common/BaseBadge.vue'
import { useAdminStore } from '../../stores/admin'

const admin = useAdminStore()

interface CustomerRow {
  userId: number
  orders: number
  revenue: number
  lastOrder: string
}

/** Customers are derived from the order stream: the backend has no customer directory endpoint. */
const customers = computed<CustomerRow[]>(() => {
  const map = new Map<number, CustomerRow>()
  for (const order of admin.orders) {
    const existing = map.get(order.userId)
    const value = order.status === 'CANCELLED' ? 0 : Number(order.totalAmount)
    if (existing) {
      existing.orders += 1
      existing.revenue += value
      if (order.createdAt > existing.lastOrder) existing.lastOrder = order.createdAt
    } else {
      map.set(order.userId, {
        userId: order.userId,
        orders: 1,
        revenue: value,
        lastOrder: order.createdAt,
      })
    }
  }
  return [...map.values()].sort((a, b) => b.revenue - a.revenue)
})

const repeatRate = computed(() => {
  if (customers.value.length === 0) return 0
  const repeat = customers.value.filter((customer) => customer.orders > 1).length
  return Math.round((repeat / customers.value.length) * 100)
})

const averageValue = computed(() => {
  if (customers.value.length === 0) return 0
  const total = customers.value.reduce((sum, customer) => sum + customer.revenue, 0)
  return total / customers.value.length
})

function formatMoney(value: number): string {
  return value.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 })
}

onMounted(() => {
  void admin.loadAll()
})
</script>

<template>
  <AdminShell title="Customers">
    <div class="grid">
      <StatCard label="Customers" :value="customers.length" :icon="Users" />
      <StatCard label="Repeat rate" :value="`${repeatRate}%`" hint="Ordered more than once" />
      <StatCard label="Average value" :value="formatMoney(averageValue)" hint="Per customer, all time" />
      <StatCard label="Top spender" :value="customers[0] ? formatMoney(customers[0].revenue) : '—'" />
    </div>

    <EmptyState
      v-if="customers.length === 0"
      :icon="Users"
      title="No customers yet."
      message="Customer records appear here once orders are placed."
    />

    <div v-else class="table-wrap">
      <table class="table responsive-table">
        <thead>
          <tr>
            <th>Customer</th>
            <th>Orders</th>
            <th>Lifetime value</th>
            <th>Last order</th>
            <th>Segment</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="customer in customers" :key="customer.userId">
            <td data-label="Customer">
              <div class="who">
                <span class="who__avatar" aria-hidden="true">U{{ customer.userId }}</span>
                <span class="who__name">User {{ customer.userId }}</span>
              </div>
            </td>
            <td data-label="Orders" class="table__num">{{ customer.orders }}</td>
            <td data-label="Lifetime value" class="table__num">{{ formatMoney(customer.revenue) }}</td>
            <td data-label="Last order">{{ new Date(customer.lastOrder).toLocaleDateString() }}</td>
            <td data-label="Segment">
              <BaseBadge :tone="customer.orders > 1 ? 'success' : 'neutral'">
                {{ customer.orders > 1 ? 'Returning' : 'New' }}
              </BaseBadge>
            </td>
          </tr>
        </tbody>
      </table>
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

.who {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.who__avatar {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  border-radius: var(--radius-full);
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-size: 10px;
  font-weight: var(--weight-bold);
}

.who__name {
  font-weight: var(--weight-medium);
  color: var(--color-text);
}

@media (min-width: 1000px) {
  .grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
</style>
