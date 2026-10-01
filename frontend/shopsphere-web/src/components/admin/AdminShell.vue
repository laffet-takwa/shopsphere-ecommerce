<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import {
  LayoutDashboard,
  Package,
  ShoppingBag,
  Users,
  Boxes,
  BarChart3,
  Settings,
  LogOut,
  Search,
  Bell,
  Menu,
  X,
} from '@lucide/vue'
import { useAdminStore } from '../../stores/admin'
import { useAuthStore } from '../../stores/auth'
import { useUiStore } from '../../stores/ui'

const props = defineProps<{ title: string }>()

const admin = useAdminStore()
const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const LINKS = [
  { name: 'admin-dashboard', label: 'Dashboard', icon: LayoutDashboard },
  { name: 'admin-products', label: 'Products', icon: Package },
  { name: 'admin-orders', label: 'Orders', icon: ShoppingBag },
  { name: 'admin-customers', label: 'Customers', icon: Users },
  { name: 'admin-inventory', label: 'Inventory', icon: Boxes },
  { name: 'admin-analytics', label: 'Analytics', icon: BarChart3 },
  { name: 'admin-settings', label: 'Settings', icon: Settings },
]

const lowStock = computed(() =>
  admin.inventory.filter((row) => row.state !== 'IN_STOCK').slice(0, 6),
)

onMounted(() => {
  if (admin.products.length === 0 && !admin.loading) void admin.loadAll()
})

function signOut(): void {
  auth.signOut()
  ui.success('Signed out')
  router.push('/')
}
</script>

<template>
  <div class="admin">
    <aside class="admin__nav" :class="{ 'is-open': ui.menuOpen }">
      <div class="admin__brand">
        <RouterLink class="admin__logo" to="/admin">
          <span class="admin__mark" aria-hidden="true" />
          <span>ShopSphere</span>
        </RouterLink>
        <span class="admin__tag">Admin</span>
        <button class="icon-btn admin__close" type="button" aria-label="Close menu" @click="ui.menuOpen = false">
          <X :size="20" />
        </button>
      </div>

      <nav aria-label="Admin sections">
        <ul>
          <li v-for="link in LINKS" :key="link.name">
            <RouterLink
              class="admin__link"
              :to="{ name: link.name }"
              @click="ui.menuOpen = false"
            >
              <component :is="link.icon" :size="17" aria-hidden="true" />
              <span>{{ link.label }}</span>
            </RouterLink>
          </li>
        </ul>
      </nav>

      <div class="admin__nav-foot">
        <RouterLink class="admin__store" to="/">View storefront</RouterLink>
        <button class="admin__link" type="button" @click="signOut">
          <LogOut :size="17" aria-hidden="true" />
          <span>Logout</span>
        </button>
      </div>
    </aside>

    <div v-if="ui.menuOpen" class="admin__scrim" @click="ui.menuOpen = false" />

    <div class="admin__body">
      <header class="admin__topbar">
        <button class="icon-btn admin__burger" type="button" aria-label="Open admin menu" @click="ui.menuOpen = true">
          <Menu :size="20" />
        </button>
        <h1 class="admin__title">{{ title }}</h1>

        <div class="admin__tools">
          <div class="admin__search">
            <Search :size="17" aria-hidden="true" />
            <label class="visually-hidden" for="admin-search">Search the catalog</label>
            <input
              id="admin-search"
              type="search"
              placeholder="Search products…"
              @keydown.enter="router.push({ name: 'admin-products', query: { keyword: ($event.target as HTMLInputElement).value } })"
            />
          </div>
          <button class="icon-btn" type="button" aria-label="Notifications">
            <Bell :size="19" />
            <span v-if="lowStock.length" class="admin__dot" aria-hidden="true" />
          </button>
          <div class="admin__profile">
            <span class="admin__avatar" aria-hidden="true">{{ auth.initials || 'A' }}</span>
            <span class="admin__profile-text">
              <span class="admin__profile-name">{{ auth.displayName || 'Administrator' }}</span>
              <span class="admin__profile-role">{{ auth.user?.role ?? 'ADMIN' }}</span>
            </span>
          </div>
        </div>
      </header>

      <main class="admin__content">
        <p v-if="admin.error" class="alert alert--error" role="alert">{{ admin.error }}</p>
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.admin {
  display: flex;
  min-height: 100vh;
  background: var(--color-background);
}

.admin__nav {
  display: none;
  flex-direction: column;
  width: 244px;
  flex-shrink: 0;
  background: var(--color-primary);
  color: rgba(255, 255, 255, 0.72);
  padding: var(--space-5) var(--space-4);
}

.admin__brand {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  padding-bottom: var(--space-5);
  margin-bottom: var(--space-4);
  border-bottom: 1px solid rgba(255, 255, 255, 0.12);
}

.admin__logo {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  color: var(--color-inverse);
  font-weight: var(--weight-bold);
  letter-spacing: -0.02em;
}

.admin__mark {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  box-shadow: inset 0 0 0 4px #0b1226;
}

.admin__tag {
  margin-left: auto;
  padding: 2px var(--space-2);
  border-radius: var(--radius-sm);
  background: rgba(43, 107, 255, 0.2);
  color: #9dbcff;
  font-size: 10px;
  font-weight: var(--weight-bold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
}

.admin__link {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  color: rgba(255, 255, 255, 0.72);
  text-align: left;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.admin__link:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-inverse);
}

.admin__link.router-link-active {
  background: rgba(43, 107, 255, 0.18);
  color: var(--color-inverse);
  font-weight: var(--weight-semibold);
}

.admin__nav-foot {
  margin-top: auto;
  padding-top: var(--space-4);
  border-top: 1px solid rgba(255, 255, 255, 0.12);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.admin__store {
  padding: var(--space-2) var(--space-3);
  font-size: var(--text-sm);
  color: rgba(255, 255, 255, 0.56);
}

.admin__store:hover {
  color: var(--color-inverse);
}

.admin__body {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.admin__topbar {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4) var(--space-5);
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  position: sticky;
  top: 0;
  z-index: var(--z-nav);
}

.admin__title {
  font-size: var(--text-lg);
  font-weight: var(--weight-semibold);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.admin__tools {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  margin-left: auto;
}

.admin__search {
  display: none;
  align-items: center;
  gap: var(--space-2);
  height: 40px;
  padding: 0 var(--space-4);
  border-radius: var(--radius-md);
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  color: var(--color-muted);
}

.admin__search input {
  border: none;
  outline: none;
  background: none;
  font-size: var(--text-sm);
  color: var(--color-text);
  width: 190px;
}

.admin__dot {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 7px;
  height: 7px;
  border-radius: var(--radius-full);
  background: var(--color-danger);
}

.admin__tools .icon-btn {
  position: relative;
}

.admin__profile {
  display: none;
  align-items: center;
  gap: var(--space-3);
  padding-left: var(--space-3);
  border-left: 1px solid var(--color-border);
}

.admin__avatar {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-inverse);
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
}

.admin__profile-text {
  display: flex;
  flex-direction: column;
}

.admin__profile-name {
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  line-height: 1.2;
}

.admin__profile-role {
  font-size: 10px;
  color: var(--color-muted);
  letter-spacing: var(--tracking-wide);
}

.admin__burger {
  display: inline-flex;
}

.admin__content {
  padding: var(--space-5);
  flex: 1;
}

.admin__close {
  display: none;
  color: var(--color-inverse);
}

.admin__scrim {
  position: fixed;
  inset: 0;
  background: var(--color-overlay);
  z-index: calc(var(--z-drawer) - 1);
}

@media (max-width: 899px) {
  .admin__nav {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    z-index: var(--z-drawer);
    display: flex;
    animation: slide-in-left var(--duration-base) var(--ease-out);
  }
  .admin__nav:not(.is-open) {
    display: none;
  }
  .admin__close {
    display: inline-flex;
  }
}

@media (min-width: 640px) {
  .admin__search {
    display: flex;
  }
  .admin__content {
    padding: var(--space-6);
  }
}

@media (min-width: 1024px) {
  .admin__nav {
    display: flex;
    position: sticky;
    top: 0;
    height: 100vh;
  }
  .admin__burger {
    display: none;
  }
  .admin__profile {
    display: flex;
  }
}
</style>
