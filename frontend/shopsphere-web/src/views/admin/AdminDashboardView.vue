<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { DollarSign, ShoppingBag, Users, Package, AlertTriangle, ArrowRight } from '@lucide/vue'
import AdminShell from '../../components/admin/AdminShell.vue'
import StatCard from '../../components/admin/StatCard.vue'
import MiniBarChart from '../../components/admin/MiniBarChart.vue'
import OrderStatusBadge from '../../components/order/OrderStatusBadge.vue'
import SkeletonBlock from '../../components/common/SkeletonBlock.vue'
import BaseBadge from '../../components/common/BaseBadge.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import { useAdminStore } from '../../stores/admin'
import { imageFor } from '../../mocks/products'

const admin = useAdminStore()

const TONE: Record<string, 'success' | 'warning' | 'danger'> = {
  IN_STOCK: 'success',
  LOW_STOCK: 'warning',
  OUT_OF_STOCK: 'danger',
}

const lowStock = computed(() => admin.inventory.filter((row) => row.state !== 'IN_STOCK').slice(0, 5))
const recentOrders = computed(() => admin.orders.slice(0, 6))

const revenue = computed(() =>
  admin.metrics.revenue.toLocaleString('en-US', { style: 'currency', currency: 'USD', maximumFractionDigits: 0 }),
)

onMounted(() => {
  void admin.loadAll()
})
</script>

<template>
  <AdminShell title="Dashboard">
    <div v-if="admin.loading && !admin.metrics.orderCount" class="grid">
      <SkeletonBlock v-for="index in 4" :key="index" height="128px" radius="var(--radius-lg)" />
    </div>

    <div class="grid">
      <StatCard label="Total revenue" :value="revenue" :icon="DollarSign" tone="success" hint="Excludes cancelled orders" />
      <StatCard label="Orders" :value="admin.metrics.orderCount" :icon="ShoppingBag" />
      <StatCard label="Customers" :value="admin.metrics.customerCount" :icon="Users" hint="Distinct placing users" />
      <StatCard label="Products" :value="admin.metrics.productCount" :icon="Package" />
    </div>

    <section class="cards">
      <article class="card card--pad">
        <h2 class="panel__title">Revenue, last 7 days</h2>
        <MiniBarChart :data="admin.revenueSeries" :height="200" />
      </article>

      <article class="card card--pad">
        <h2 class="panel__title">Orders by status</h2>
        <ul v-if="admin.orderStatusSeries.length" class="statuses">
          <li v-for="entry in admin.orderStatusSeries" :key="entry.status" class="statuses__row">
            <OrderStatusBadge :status="entry.status as never" />
            <span class="statuses__value">{{ entry.value }}</span>
          </li>
        </ul>
        <p v-else class="muted">No orders yet.</p>
      </article>
    </section>

    <section class="cards">
      <article class="card">
        <div class="card__head">
          <h2 class="card__title">Recent orders</h2>
          <BaseButton to="/admin/orders" variant="ghost" size="sm">
            View all <ArrowRight :size="15" />
          </BaseButton>
        </div>
        <div class="card__body">
          <p v-if="recentOrders.length === 0" class="muted">No orders yet.</p>
          <ul v-else class="rows">
            <li v-for="order in recentOrders" :key="order.id" class="rows__row">
              <div>
                <p class="rows__id">#{{ order.id }}</p>
                <p class="rows__meta">User {{ order.userId }} · {{ order.items.length }} items</p>
              </div>
              <span class="rows__value">{{ order.totalAmount.toFixed(2) }}</span>
              <OrderStatusBadge :status="order.status" />
            </li>
          </ul>
        </div>
      </article>

      <article class="card">
        <div class="card__head">
          <h2 class="card__title">Top products</h2>
        </div>
        <div class="card__body">
          <ul class="rows">
            <li v-for="product in admin.topProducts" :key="product.id" class="rows__row">
              <img class="rows__img" :src="imageFor(product)" :alt="product.name" width="40" height="40" loading="lazy" />
              <div class="rows__text">
                <p class="rows__name">{{ product.name }}</p>
                <p class="rows__meta">{{ product.category }}</p>
              </div>
              <span class="rows__value">{{ product.price.toFixed(2) }}</span>
            </li>
          </ul>
        </div>
      </article>
    </section>

    <section class="card">
      <div class="card__head">
        <h2 class="card__title">Low stock alerts</h2>
        <BaseButton to="/admin/inventory" variant="ghost" size="sm">
          Manage inventory <ArrowRight :size="15" />
        </BaseButton>
      </div>
      <div class="card__body">
        <p v-if="lowStock.length === 0" class="muted">Every product is comfortably stocked.</p>
        <div v-else class="table-wrap">
          <table class="table responsive-table">
            <thead>
              <tr>
                <th>Product</th>
                <th>SKU</th>
                <th>Available</th>
                <th>Reserved</th>
                <th>Status</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="row in lowStock" :key="row.product.id">
                <td data-label="Product">{{ row.product.name }}</td>
                <td data-label="SKU"><code>{{ row.product.sku }}</code></td>
                <td data-label="Available" class="table__num">{{ row.stock?.availableQuantity ?? '—' }}</td>
                <td data-label="Reserved" class="table__num">{{ row.stock?.reservedQuantity ?? '—' }}</td>
                <td data-label="Status">
                  <BaseBadge :tone="TONE[row.state]">{{ row.state.replace('_', ' ') }}</BaseBadge>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>
  </AdminShell>
</template>

<style scoped>
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-4);
  margin-bottom: var(--space-5);
}

.cards {
  display: grid;
  gap: var(--space-5);
  margin-bottom: var(--space-5);
}

.panel__title {
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
  margin-bottom: var(--space-5);
}

.statuses__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding-block: var(--space-2);
  border-bottom: 1px solid var(--color-border);
}

.statuses__row:last-child {
  border-bottom: none;
}

.statuses__value {
  font-weight: var(--weight-bold);
  font-variant-numeric: tabular-nums;
}

.rows__row {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-block: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.rows__row:last-child {
  border-bottom: none;
}

.rows__id {
  font-weight: var(--weight-semibold);
  font-size: var(--text-sm);
}

.rows__name {
  font-weight: var(--weight-medium);
  font-size: var(--text-sm);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rows__meta {
  font-size: var(--text-xs);
  color: var(--color-muted);
}

.rows__value {
  margin-left: auto;
  font-weight: var(--weight-semibold);
  font-variant-numeric: tabular-nums;
  white-space: nowrap;
}

.rows__img {
  width: 40px;
  height: 40px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.rows__text {
  min-width: 0;
  flex: 1;
}

.muted {
  color: var(--color-muted);
  font-size: var(--text-sm);
}

code {
  font-family: var(--font-mono);
  font-size: 0.85em;
}

@media (min-width: 640px) {
  .grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (min-width: 1100px) {
  .cards {
    grid-template-columns: minmax(0, 1.4fr) minmax(0, 1fr);
  }
}
</style>
