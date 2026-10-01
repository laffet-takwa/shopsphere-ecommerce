<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { Truck, ShoppingBag } from '@lucide/vue'
import AdminShell from '../../components/admin/AdminShell.vue'
import OrderStatusBadge from '../../components/order/OrderStatusBadge.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import { useAdminStore } from '../../stores/admin'
import { useUiStore } from '../../stores/ui'
import type { Order } from '../../types'

const admin = useAdminStore()
const ui = useUiStore()

const orders = computed(() =>
  [...admin.orders].sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()),
)

/** Payment is resolved by consuming inventory.reserved, so PAID implies payment succeeded. */
function paymentLabel(order: Order): string {
  if (order.status === 'CANCELLED') return 'Refunded'
  if (order.status === 'PENDING') return 'Awaiting stock'
  return 'Captured'
}

onMounted(() => {
  void admin.loadAll()
})

async function ship(order: Order): Promise<void> {
  try {
    await admin.shipOrder(order.id)
    ui.success(`Order #${order.id} marked as shipped`)
  } catch {
    ui.error(admin.error || 'Only paid orders can be shipped.')
  }
}
</script>

<template>
  <AdminShell title="Orders">
    <EmptyState
      v-if="orders.length === 0"
      :icon="ShoppingBag"
      title="No orders yet."
      message="Orders placed in the storefront appear here, newest first."
      action-label="Open storefront"
      to="/"
    />

    <div v-else class="table-wrap">
      <table class="table responsive-table">
        <thead>
          <tr>
            <th>Order</th>
            <th>Customer</th>
            <th>Date</th>
            <th>Items</th>
            <th>Amount</th>
            <th>Payment</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="order in orders" :key="order.id">
            <td data-label="Order" class="table__num">#{{ order.id }}</td>
            <td data-label="Customer">User {{ order.userId }}</td>
            <td data-label="Date">{{ new Date(order.createdAt).toLocaleDateString() }}</td>
            <td data-label="Items">{{ order.items.reduce((sum, item) => sum + item.quantity, 0) }}</td>
            <td data-label="Amount" class="table__num">{{ order.totalAmount.toFixed(2) }}</td>
            <td data-label="Payment">{{ paymentLabel(order) }}</td>
            <td data-label="Status"><OrderStatusBadge :status="order.status" /></td>
            <td data-label="Actions">
              <BaseButton
                v-if="order.status === 'PAID' || order.status === 'PROCESSING'"
                variant="outline"
                size="sm"
                :loading="admin.saving"
                @click="ship(order)"
              >
                <Truck :size="15" aria-hidden="true" /> Ship
              </BaseButton>
              <BaseButton
                v-else
                variant="ghost"
                size="sm"
                :to="{ name: 'order-detail', params: { id: order.id } }"
              >
                View
              </BaseButton>
            </td>
          </tr>
        </tbody>
      </table>
    </div>
  </AdminShell>
</template>
