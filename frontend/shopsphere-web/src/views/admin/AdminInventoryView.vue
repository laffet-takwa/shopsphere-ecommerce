<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { Boxes, Save, AlertTriangle } from '@lucide/vue'
import AdminShell from '../../components/admin/AdminShell.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import BaseBadge from '../../components/common/BaseBadge.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import { useAdminStore, type StockState } from '../../stores/admin'
import { useUiStore } from '../../stores/ui'
import type { Product } from '../../types'

const admin = useAdminStore()
const ui = useUiStore()

const TONE: Record<StockState, 'success' | 'warning' | 'danger'> = {
  IN_STOCK: 'success',
  LOW_STOCK: 'warning',
  OUT_OF_STOCK: 'danger',
}

const drafts = reactive<Record<number, number>>({})
const filter = ref('all')

const rows = computed(() =>
  admin.inventory.filter((row) => filter.value === 'all' || row.state === filter.value),
)

const counts = computed(() => ({
  all: admin.inventory.length,
  IN_STOCK: admin.inventory.filter((r) => r.state === 'IN_STOCK').length,
  LOW_STOCK: admin.inventory.filter((r) => r.state === 'LOW_STOCK').length,
  OUT_OF_STOCK: admin.inventory.filter((r) => r.state === 'OUT_OF_STOCK').length,
}))

onMounted(() => {
  void admin.loadAll()
})

function draftFor(product: Product): number {
  return drafts[product.id] ?? admin.inventoryByProduct.get(product.id)?.stock?.quantity ?? 0
}

async function save(product: Product): Promise<void> {
  const quantity = draftFor(product)
  const reserved = admin.inventoryByProduct.get(product.id)?.stock?.reservedQuantity ?? 0

  if (quantity < reserved) {
    ui.error(`Quantity cannot be below the ${reserved} units already reserved.`)
    return
  }

  try {
    await admin.updateStock(product, { sku: product.sku, quantity: Number(quantity) })
    delete drafts[product.id]
    ui.success(`${product.name} stock updated`)
  } catch {
    ui.error(admin.error || 'Could not update stock')
  }
}

const FILTERS = [
  { value: 'all', label: 'All' },
  { value: 'IN_STOCK', label: 'In stock' },
  { value: 'LOW_STOCK', label: 'Low stock' },
  { value: 'OUT_OF_STOCK', label: 'Out of stock' },
] as const
</script>

<template>
  <AdminShell title="Inventory">
    <div class="tabs" role="tablist" aria-label="Stock filter">
      <button
        v-for="option in FILTERS"
        :key="option.value"
        class="tabs__tab"
        :class="{ 'is-active': filter === option.value }"
        type="button"
        role="tab"
        :aria-selected="filter === option.value"
        @click="filter = option.value"
      >
        {{ option.label }}
        <span class="tabs__count">{{ counts[option.value] }}</span>
      </button>
    </div>

    <EmptyState
      v-if="rows.length === 0"
      :icon="Boxes"
      title="No stock records."
      message="Inventory rows are created in MongoDB when a product is first reserved."
    />

    <div v-else class="table-wrap">
      <table class="table responsive-table">
        <thead>
          <tr>
            <th>Product</th>
            <th>SKU</th>
            <th>Current stock</th>
            <th>Reserved</th>
            <th>Available</th>
            <th>Status</th>
            <th>Last updated</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.product.id">
            <td data-label="Product">
              <RouterLink class="link" :to="{ name: 'product-detail', params: { id: row.product.id } }">
                {{ row.product.name }}
              </RouterLink>
            </td>
            <td data-label="SKU"><code>{{ row.product.sku }}</code></td>
            <td data-label="Current stock">
              <div class="stock-input">
                <label class="visually-hidden" :for="`stock-${row.product.id}`">
                  Stock for {{ row.product.name }}
                </label>
                <input
                  :id="`stock-${row.product.id}`"
                  class="input"
                  type="number"
                  inputmode="numeric"
                  min="0"
                  :value="draftFor(row.product)"
                  @input="drafts[row.product.id] = Number(($event.target as HTMLInputElement).value)"
                />
              </div>
            </td>
            <td data-label="Reserved" class="table__num">{{ row.stock?.reservedQuantity ?? 0 }}</td>
            <td data-label="Available" class="table__num">{{ row.stock?.availableQuantity ?? 0 }}</td>
            <td data-label="Status">
              <BaseBadge :tone="TONE[row.state]">
                <AlertTriangle v-if="row.state === 'LOW_STOCK'" :size="12" aria-hidden="true" />
                {{ row.state.replace('_', ' ') }}
              </BaseBadge>
            </td>
            <td data-label="Last updated">
              {{ row.stock?.updatedAt ? new Date(row.stock.updatedAt).toLocaleDateString() : '—' }}
            </td>
            <td data-label="Actions">
              <BaseButton
                variant="outline"
                size="sm"
                :loading="admin.saving"
                @click="save(row.product)"
              >
                <Save :size="15" aria-hidden="true" /> Save
              </BaseButton>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="note">
      Reserved units are held by <code>order.created</code> events and released when a payment
      fails, so stock can never be set below what is currently reserved.
    </p>
  </AdminShell>
</template>

<style scoped>
.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2);
  margin-bottom: var(--space-5);
}

.tabs__tab {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  padding: var(--space-2) var(--space-4);
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  color: var(--color-text-secondary);
}

.tabs__tab.is-active {
  background: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-inverse);
}

.tabs__count {
  padding: 1px var(--space-2);
  border-radius: var(--radius-full);
  background: var(--color-surface-alt);
  color: var(--color-text-secondary);
  font-size: 11px;
  font-weight: var(--weight-bold);
}

.tabs__tab.is-active .tabs__count {
  background: rgba(255, 255, 255, 0.2);
  color: var(--color-inverse);
}

.stock-input {
  width: 96px;
}

.link {
  font-weight: var(--weight-medium);
  color: var(--color-text);
}

.link:hover {
  color: var(--color-accent);
}

code {
  font-family: var(--font-mono);
  font-size: 0.85em;
}

.note {
  margin-top: var(--space-4);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.note code {
  padding: 1px 4px;
  border-radius: 4px;
  background: var(--color-surface-alt);
}
</style>
