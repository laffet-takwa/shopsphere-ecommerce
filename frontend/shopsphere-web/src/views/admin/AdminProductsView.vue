<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { Plus, Pencil, Trash2, Search, Power, Package } from '@lucide/vue'
import AdminShell from '../../components/admin/AdminShell.vue'
import BaseModal from '../../components/common/BaseModal.vue'
import BaseButton from '../../components/common/BaseButton.vue'
import BaseBadge from '../../components/common/BaseBadge.vue'
import BaseInput from '../../components/common/BaseInput.vue'
import EmptyState from '../../components/common/EmptyState.vue'
import { useAdminStore, toPayload } from '../../stores/admin'
import { useUiStore } from '../../stores/ui'
import { imageFor } from '../../mocks/products'
import type { Product } from '../../types'

const admin = useAdminStore()
const ui = useUiStore()

const keyword = ref('')
const categoryFilter = ref('all')
const sort = ref('name-asc')
const modalOpen = ref(false)
const editing = ref<Product | null>(null)
const form = reactive({ name: '', description: '', price: 0, category: '', sku: '', imageUrl: '', active: true })
const errors = reactive<Record<string, string>>({})

const categories = computed(() => admin.products.map((product) => product.category).filter((value, index, all) => all.indexOf(value) === index))

const filtered = computed(() => {
  const term = keyword.value.trim().toLowerCase()
  let list = admin.products.filter((product) => {
    const matchesTerm = !term || `${product.name} ${product.sku}`.toLowerCase().includes(term)
    const matchesCategory = categoryFilter.value === 'all' || product.category === categoryFilter.value
    return matchesTerm && matchesCategory
  })

  list = [...list]
  switch (sort.value) {
    case 'price-desc':
      list.sort((a, b) => b.price - a.price)
      break
    case 'price-asc':
      list.sort((a, b) => a.price - b.price)
      break
    case 'newest':
      list.sort((a, b) => new Date(b.createdAt ?? 0).getTime() - new Date(a.createdAt ?? 0).getTime())
      break
    default:
      list.sort((a, b) => a.name.localeCompare(b.name))
  }
  return list
})

const stockFor = (product: Product) => admin.inventoryByProduct.get(product.id)?.stock ?? null

onMounted(() => {
  void admin.loadAll()
})

function openCreate(): void {
  editing.value = null
  Object.assign(form, { name: '', description: '', price: 0, category: '', sku: '', imageUrl: '', active: true })
  Object.keys(errors).forEach((key) => delete errors[key])
  modalOpen.value = true
}

function openEdit(product: Product): void {
  editing.value = product
  Object.assign(form, toPayload(product))
  Object.keys(errors).forEach((key) => delete errors[key])
  modalOpen.value = true
}

async function save(): Promise<void> {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!form.name.trim()) errors.name = 'Name is required.'
  if (!form.category.trim()) errors.category = 'Category is required.'
  if (!form.sku.trim()) errors.sku = 'SKU is required.'
  if (!Number.isFinite(form.price) || form.price <= 0) errors.price = 'Enter a price greater than zero.'
  if (Object.keys(errors).length) return

  const saved = await admin.saveProduct(editing.value?.id ?? null, {
    name: form.name.trim(),
    description: form.description.trim(),
    price: Number(form.price),
    category: form.category.trim(),
    sku: form.sku.trim(),
    imageUrl: form.imageUrl.trim(),
    active: form.active,
  })

  if (saved) {
    ui.success(editing.value ? 'Product updated' : 'Product created')
    modalOpen.value = false
  } else {
    ui.error(admin.error || 'Could not save the product')
  }
}

async function remove(product: Product): Promise<void> {
  await admin.deleteProduct(product.id)
  ui.success(`${product.name} removed`)
}

watch(() => form.price, () => delete errors.price)
</script>

<template>
  <AdminShell title="Products">
    <div class="toolbar">
      <div class="toolbar__search">
        <Search :size="17" aria-hidden="true" />
        <label class="visually-hidden" for="product-search">Search products</label>
        <input id="product-search" v-model="keyword" type="search" placeholder="Search name or SKU…" />
      </div>

      <label class="visually-hidden" for="product-category">Filter by category</label>
      <select id="product-category" v-model="categoryFilter" class="select">
        <option value="all">All categories</option>
        <option v-for="category in categories" :key="category" :value="category">{{ category }}</option>
      </select>

      <label class="visually-hidden" for="product-sort">Sort products</label>
      <select id="product-sort" v-model="sort" class="select">
        <option value="name-asc">Name A–Z</option>
        <option value="price-asc">Price low → high</option>
        <option value="price-desc">Price high → low</option>
        <option value="newest">Newest</option>
      </select>

      <BaseButton variant="accent" @click="openCreate">
        <Plus :size="17" aria-hidden="true" /> Add product
      </BaseButton>
    </div>

    <EmptyState
      v-if="filtered.length === 0"
      :icon="Package"
      title="No products match."
      message="Adjust the search or category filter, or add a new product."
      action-label="Add product"
      @action="openCreate"
    />

    <div v-else class="table-wrap">
      <table class="table responsive-table">
        <thead>
          <tr>
            <th>Product</th>
            <th>Category</th>
            <th>Price</th>
            <th>Stock</th>
            <th>Status</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="product in filtered" :key="product.id">
            <td data-label="Product">
              <div class="cell-product">
                <img :src="imageFor(product)" :alt="product.name" width="44" height="44" loading="lazy" />
                <div>
                  <p class="cell-product__name">{{ product.name }}</p>
                  <p class="cell-product__sku">{{ product.sku }}</p>
                </div>
              </div>
            </td>
            <td data-label="Category">{{ product.category }}</td>
            <td data-label="Price" class="table__num">{{ product.price.toFixed(2) }}</td>
            <td data-label="Stock" class="table__num">{{ stockFor(product)?.availableQuantity ?? '—' }}</td>
            <td data-label="Status">
              <BaseBadge :tone="product.active ? 'success' : 'neutral'">
                {{ product.active ? 'Active' : 'Inactive' }}
              </BaseBadge>
            </td>
            <td data-label="Actions">
              <div class="actions">
                <button class="icon-btn" type="button" :aria-label="`Edit ${product.name}`" @click="openEdit(product)">
                  <Pencil :size="17" />
                </button>
                <button
                  class="icon-btn"
                  type="button"
                  :aria-label="product.active ? `Deactivate ${product.name}` : `Activate ${product.name}`"
                  @click="admin.toggleActive(product)"
                >
                  <Power :size="17" />
                </button>
                <button class="icon-btn actions__danger" type="button" :aria-label="`Delete ${product.name}`" @click="remove(product)">
                  <Trash2 :size="17" />
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <BaseModal :open="modalOpen" :title="editing ? 'Edit product' : 'Add product'" wide @close="modalOpen = false">
      <form id="product-form" class="form" novalidate @submit.prevent="save">
        <BaseInput v-model="form.name" label="Name" name="name" :error="errors.name" required />
        <div class="form__row">
          <BaseInput v-model="form.category" label="Category" name="category" :error="errors.category" required />
          <BaseInput v-model="form.sku" label="SKU" name="sku" :error="errors.sku" required />
        </div>
        <BaseInput v-model="form.price" label="Price" name="price" type="number" inputmode="decimal" :error="errors.price" required />
        <BaseInput v-model="form.imageUrl" label="Image URL" name="imageUrl" hint="Optional. A category image is used when empty." />
        <div class="field">
          <label class="field__label" for="description">Description</label>
          <textarea id="description" v-model="form.description" class="textarea" rows="3" />
        </div>
        <label class="checkbox">
          <input v-model="form.active" type="checkbox" />
          Visible in the storefront
        </label>
      </form>

      <template #footer>
        <BaseButton variant="ghost" @click="modalOpen = false">Cancel</BaseButton>
        <BaseButton variant="primary" :loading="admin.saving" @click="save">
          {{ editing ? 'Save changes' : 'Create product' }}
        </BaseButton>
      </template>
    </BaseModal>
  </AdminShell>
</template>

<style scoped>
.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: var(--space-3);
  margin-bottom: var(--space-5);
}

.toolbar__search {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  flex: 1;
  min-width: 200px;
  height: 44px;
  padding: 0 var(--space-4);
  border: 1px solid var(--color-border-strong);
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-muted);
}

.toolbar__search input {
  flex: 1;
  border: none;
  outline: none;
  background: none;
  color: var(--color-text);
  font-size: var(--text-base);
}

.toolbar .select {
  width: auto;
  min-width: 150px;
}

.cell-product {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.cell-product img {
  width: 44px;
  height: 44px;
  border-radius: var(--radius-sm);
  object-fit: cover;
  flex-shrink: 0;
}

.cell-product__name {
  font-weight: var(--weight-medium);
  color: var(--color-text);
}

.cell-product__sku {
  font-size: var(--text-xs);
  color: var(--color-muted);
  font-family: var(--font-mono);
}

.actions {
  display: flex;
  gap: var(--space-1);
}

.actions__danger:hover {
  color: var(--color-danger);
  background: var(--color-danger-soft);
}

.form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.form__row {
  display: grid;
  gap: var(--space-4);
}

@media (min-width: 560px) {
  .form__row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
