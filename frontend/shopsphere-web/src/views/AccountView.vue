<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import {
  LayoutDashboard,
  ShoppingBag,
  Heart,
  MapPin,
  Bell,
  User,
  LogOut,
  Package,
  X,
} from '@lucide/vue'
import StatCard from '../components/admin/StatCard.vue'
import OrderStatusBadge from '../components/order/OrderStatusBadge.vue'
import ProductCard from '../components/product/ProductCard.vue'
import EmptyState from '../components/common/EmptyState.vue'
import BaseButton from '../components/common/BaseButton.vue'
import { useAuthStore } from '../stores/auth'
import { useOrderStore } from '../stores/order'
import { useCartStore } from '../stores/cart'
import { useNotificationStore } from '../stores/notification'
import { useProductStore } from '../stores/product'
import { useUiStore } from '../stores/ui'

const auth = useAuthStore()
const orders = useOrderStore()
const cart = useCartStore()
const notifications = useNotificationStore()
const products = useProductStore()
const ui = useUiStore()

type TabKey = 'overview' | 'orders' | 'wishlist' | 'addresses' | 'notifications' | 'profile'

const tab = ref<TabKey>('overview')
const navOpen = ref(false)

const TABS = [
  { key: 'overview', label: 'Overview', icon: LayoutDashboard },
  { key: 'orders', label: 'Orders', icon: ShoppingBag },
  { key: 'wishlist', label: 'Wishlist', icon: Heart },
  { key: 'addresses', label: 'Addresses', icon: MapPin },
  { key: 'notifications', label: 'Notifications', icon: Bell },
  { key: 'profile', label: 'Profile', icon: User },
] as const

const wishlistProducts = computed(() =>
  products.allProducts.filter((product) => cart.wishlistIds.includes(product.id)),
)

const recommended = computed(() => products.allProducts.slice(0, 4))

onMounted(async () => {
  await Promise.all([
    orders.fetchMine(),
    notifications.fetchAll(),
    products.allProducts.length ? Promise.resolve() : products.fetchProducts(),
  ])
})

function signOut(): void {
  auth.signOut()
  ui.success('Signed out')
}
</script>

<template>
  <div class="account">
    <div class="container account__layout">
      <aside class="account__nav" :class="{ 'is-open': navOpen }">
        <div class="account__nav-head">
          <div class="account__avatar" aria-hidden="true">{{ auth.initials || 'S' }}</div>
          <div>
            <p class="account__name">{{ auth.displayName || 'Shopper' }}</p>
            <p class="account__email">{{ auth.user?.email }}</p>
          </div>
          <button class="icon-btn account__close" type="button" aria-label="Close menu" @click="navOpen = false">
            <X :size="20" />
          </button>
        </div>

        <nav aria-label="Account">
          <ul>
            <li v-for="item in TABS" :key="item.key">
              <button
                class="account__tab"
                :class="{ 'is-active': tab === item.key }"
                type="button"
                @click="tab = item.key; navOpen = false"
              >
                <component :is="item.icon" :size="17" aria-hidden="true" />
                <span>{{ item.label }}</span>
                <span
                  v-if="item.key === 'notifications' && notifications.unreadCount"
                  class="account__tab-count"
                >
                  {{ notifications.unreadCount }}
                </span>
              </button>
            </li>
          </ul>
        </nav>

        <button class="account__signout" type="button" @click="signOut">
          <LogOut :size="17" aria-hidden="true" /> Logout
        </button>
      </aside>

      <div v-if="navOpen" class="account__scrim" @click="navOpen = false" />

      <main class="account__main">
        <button class="btn btn--outline btn--sm account__menu-btn" type="button" @click="navOpen = true">
          <User :size="16" aria-hidden="true" /> Menu
        </button>

        <!-- Overview -->
        <template v-if="tab === 'overview'">
          <header class="account__head">
            <h1 class="account__title">Dashboard</h1>
            <p class="account__subtitle">A snapshot of your activity across ShopSphere.</p>
          </header>

          <div class="stats">
            <StatCard label="Total orders" :value="orders.totalOrders" :icon="ShoppingBag" />
            <StatCard label="Pending" :value="orders.pendingOrders" :icon="Package" tone="warning" />
            <StatCard label="Completed" :value="orders.completedOrders" :icon="Package" tone="success" />
            <StatCard label="Wishlist" :value="cart.wishlistIds.length" :icon="Heart" />
          </div>

          <section class="panel card">
            <div class="card__head">
              <h2 class="card__title">Recent orders</h2>
              <button class="panel__link" type="button" @click="tab = 'orders'">View all</button>
            </div>
            <div class="card__body">
              <EmptyState
                v-if="orders.sorted.length === 0"
                :icon="Package"
                title="You haven't placed any orders yet."
                message="Your first order will show up here with live status updates."
                action-label="Start shopping"
                to="/products"
              />
              <ul v-else class="recent">
                <li v-for="order in orders.sorted.slice(0, 4)" :key="order.id" class="recent__row">
                  <div>
                    <p class="recent__id">Order #{{ order.id }}</p>
                    <p class="recent__date">{{ new Date(order.createdAt).toLocaleDateString() }}</p>
                  </div>
                  <span class="recent__total">{{ order.totalAmount.toFixed(2) }}</span>
                  <OrderStatusBadge :status="order.status" />
                </li>
              </ul>
            </div>
          </section>

          <section class="panel">
            <header class="section-head">
              <div>
                <h2 class="section-head__title">Recommended for you</h2>
                <p class="section-head__subtitle">Hand-picked from this week's edit.</p>
              </div>
            </header>
            <div class="grid-products">
              <ProductCard v-for="product in recommended" :key="product.id" :product="product" />
            </div>
          </section>
        </template>

        <!-- Orders -->
        <template v-else-if="tab === 'orders'">
          <header class="account__head">
            <h1 class="account__title">Orders</h1>
            <p class="account__subtitle">Every order you have placed, newest first.</p>
          </header>
          <EmptyState
            v-if="orders.sorted.length === 0"
            :icon="Package"
            title="You haven't placed any orders yet."
            message="Browse the catalog and your orders will appear here."
            action-label="Start shopping"
            to="/products"
          />
          <ul v-else class="order-list">
            <li v-for="order in orders.sorted" :key="order.id" class="card card--pad order-item">
              <header class="order-item__head">
                <div>
                  <p class="order-item__id">Order #{{ order.id }}</p>
                  <p class="order-item__date">{{ new Date(order.createdAt).toLocaleString() }}</p>
                </div>
                <OrderStatusBadge :status="order.status" />
              </header>
              <p class="order-item__items">
                {{ order.items.map((item) => item.productName).join(' · ') }}
              </p>
              <footer class="order-item__foot">
                <span class="price">{{ order.totalAmount.toFixed(2) }}</span>
                <BaseButton
                  variant="outline"
                  size="sm"
                  :to="{ name: 'order-detail', params: { id: order.id } }"
                >
                  View details
                </BaseButton>
              </footer>
            </li>
          </ul>
        </template>

        <!-- Wishlist -->
        <template v-else-if="tab === 'wishlist'">
          <header class="account__head">
            <h1 class="account__title">Wishlist</h1>
            <p class="account__subtitle">Products you saved for later.</p>
          </header>
          <EmptyState
            v-if="wishlistProducts.length === 0"
            :icon="Heart"
            title="Your wishlist is empty."
            message="Tap the heart on any product to save it here."
            action-label="Browse products"
            to="/products"
          />
          <div v-else class="grid-products">
            <ProductCard v-for="product in wishlistProducts" :key="product.id" :product="product" />
          </div>
        </template>

        <!-- Addresses -->
        <template v-else-if="tab === 'addresses'">
          <header class="account__head">
            <h1 class="account__title">Addresses</h1>
            <p class="account__subtitle">Where your orders ship to.</p>
          </header>
          <EmptyState
            :icon="MapPin"
            title="No saved addresses."
            message="Address storage is not part of the backend yet. The address you enter at checkout applies to that order."
          />
        </template>

        <!-- Notifications -->
        <template v-else-if="tab === 'notifications'">
          <header class="account__head">
            <h1 class="account__title">Notifications</h1>
            <p class="account__subtitle">Order and payment updates delivered over Kafka.</p>
          </header>
          <EmptyState
            v-if="notifications.recent.length === 0"
            :icon="Bell"
            title="You're all caught up."
            message="Updates about your orders and payments will appear here."
          />
          <ul v-else class="notif-list">
            <li
              v-for="item in notifications.recent"
              :key="item.id"
              class="notif"
              :class="{ 'is-unread': !item.read }"
            >
              <span class="notif__icon" aria-hidden="true"><Bell :size="16" /></span>
              <div class="notif__body">
                <p class="notif__message">{{ item.message }}</p>
                <p class="notif__time">{{ new Date(item.createdAt).toLocaleString() }}</p>
              </div>
              <BaseButton v-if="!item.read" variant="ghost" size="sm" @click="notifications.markRead(item.id)">
                Mark read
              </BaseButton>
            </li>
          </ul>
        </template>

        <!-- Profile -->
        <template v-else>
          <header class="account__head">
            <h1 class="account__title">Profile</h1>
            <p class="account__subtitle">Your ShopSphere identity.</p>
          </header>
          <section class="card card--pad profile">
            <dl class="profile__rows">
              <div>
                <dt>First name</dt>
                <dd>{{ auth.user?.firstName }}</dd>
              </div>
              <div>
                <dt>Last name</dt>
                <dd>{{ auth.user?.lastName }}</dd>
              </div>
              <div>
                <dt>Email</dt>
                <dd>{{ auth.user?.email }}</dd>
              </div>
              <div>
                <dt>Role</dt>
                <dd>{{ auth.user?.role }}</dd>
              </div>
              <div>
                <dt>Member since</dt>
                <dd>{{ auth.user?.createdAt ? new Date(auth.user.createdAt).toLocaleDateString() : '—' }}</dd>
              </div>
            </dl>
            <p class="profile__note">
              Profile updates are read-only in this build. The backend exposes register, login and
              profile read, with no update endpoint.
            </p>
          </section>
        </template>
      </main>
    </div>
  </div>
</template>

<style scoped>
.account {
  padding-block: var(--space-8) var(--space-16);
}

.account__layout {
  display: grid;
  gap: var(--space-6);
}

.account__nav {
  display: none;
}

.account__nav-head {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  padding-bottom: var(--space-4);
  margin-bottom: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.account__avatar {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-inverse);
  font-weight: var(--weight-bold);
  flex-shrink: 0;
}

.account__name {
  font-weight: var(--weight-semibold);
  font-size: var(--text-base);
}

.account__email {
  font-size: var(--text-xs);
  color: var(--color-muted);
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 180px;
}

.account__tab {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  text-align: left;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.account__tab:hover {
  background: var(--color-surface-alt);
  color: var(--color-text);
}

.account__tab.is-active {
  background: var(--color-primary-soft);
  color: var(--color-primary);
  font-weight: var(--weight-semibold);
}

.account__tab-count {
  margin-left: auto;
  min-width: 20px;
  height: 20px;
  padding: 0 5px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  color: var(--color-inverse);
  font-size: 10px;
  font-weight: var(--weight-bold);
}

.account__signout {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  margin-top: var(--space-4);
  padding: var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-base);
  color: var(--color-muted);
}

.account__signout:hover {
  background: var(--color-danger-soft);
  color: var(--color-danger);
}

.account__menu-btn {
  margin-bottom: var(--space-5);
}

.account__close {
  margin-left: auto;
  display: none;
}

.account__head {
  margin-bottom: var(--space-6);
}

.account__title {
  font-size: clamp(1.5rem, 3vw, var(--text-2xl));
}

.account__subtitle {
  margin-top: var(--space-2);
  color: var(--color-muted);
}

.stats {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3);
  margin-bottom: var(--space-8);
}

.panel {
  margin-top: var(--space-8);
}

.panel__link {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
  color: var(--color-accent);
}

.recent__row {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding-block: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.recent__row:last-child {
  border-bottom: none;
}

.recent__id {
  font-weight: var(--weight-medium);
  font-size: var(--text-base);
}

.recent__date {
  font-size: var(--text-xs);
  color: var(--color-muted);
}

.recent__total {
  margin-left: auto;
  font-weight: var(--weight-semibold);
  font-variant-numeric: tabular-nums;
}

.order-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.order-item__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: var(--space-3);
}

.order-item__id {
  font-weight: var(--weight-semibold);
}

.order-item__date {
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.order-item__items {
  margin-top: var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.order-item__foot {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  margin-top: var(--space-4);
  padding-top: var(--space-4);
  border-top: 1px solid var(--color-border);
}

.notif-list {
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.notif {
  display: flex;
  align-items: center;
  gap: var(--space-4);
  padding: var(--space-4);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  background: var(--color-surface);
}

.notif.is-unread {
  border-color: var(--color-accent);
  background: var(--color-accent-soft);
}

.notif__icon {
  display: grid;
  place-items: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-full);
  background: var(--color-surface);
  color: var(--color-accent);
  flex-shrink: 0;
}

.notif__body {
  flex: 1;
  min-width: 0;
}

.notif__message {
  font-size: var(--text-base);
  color: var(--color-text);
}

.notif__time {
  font-size: var(--text-xs);
  color: var(--color-muted);
  margin-top: 2px;
}

.profile__rows > div {
  display: flex;
  justify-content: space-between;
  gap: var(--space-4);
  padding-block: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.profile__rows dt {
  color: var(--color-muted);
  font-size: var(--text-base);
}

.profile__rows dd {
  font-weight: var(--weight-medium);
  text-align: right;
}

.profile__note {
  margin-top: var(--space-5);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

@media (min-width: 560px) {
  .stats {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (min-width: 900px) {
  .account__layout {
    grid-template-columns: 236px 1fr;
    gap: var(--space-8);
  }
  .account__nav {
    display: block;
    position: sticky;
    top: calc(var(--navbar-height) + var(--announcement-height) + var(--space-4));
    align-self: start;
  }
  .account__menu-btn {
    display: none;
  }
}

@media (max-width: 899px) {
  .account__nav {
    position: fixed;
    top: 0;
    bottom: 0;
    left: 0;
    width: min(280px, 84vw);
    z-index: var(--z-drawer);
    background: var(--color-surface);
    box-shadow: var(--shadow-lg);
    padding: var(--space-4);
    overflow-y: auto;
    animation: slide-in-left var(--duration-base) var(--ease-out);
  }
  .account__nav.is-open {
    display: block;
  }
  .account__close {
    display: inline-flex;
  }
  .account__scrim {
    position: fixed;
    inset: 0;
    z-index: calc(var(--z-drawer) - 1);
    background: var(--color-overlay);
  }
}
</style>
