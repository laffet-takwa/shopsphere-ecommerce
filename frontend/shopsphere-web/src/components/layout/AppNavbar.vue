<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Search, ShoppingBag, User, Menu, X, Heart, LogOut, LayoutDashboard, ChevronDown } from '@lucide/vue'
import { useAuthStore } from '../../stores/auth'
import { useCartStore } from '../../stores/cart'
import { useUiStore } from '../../stores/ui'
import SearchOverlay from './SearchOverlay.vue'

const auth = useAuthStore()
const cart = useCartStore()
const ui = useUiStore()
const route = useRoute()
const router = useRouter()

const scrolled = ref(false)
const accountOpen = ref(false)

const links = [
  { label: 'Shop', to: { name: 'catalog' } },
  { label: 'Categories', to: { name: 'catalog' } },
  { label: 'Deals', to: { name: 'catalog', query: { sort: 'price-asc' } } },
  { label: 'About', to: { name: 'about' } },
]

const onScroll = (): void => {
  scrolled.value = window.scrollY > 8
}

onMounted(() => {
  window.addEventListener('scroll', onScroll, { passive: true })
  onScroll()
})

onBeforeUnmount(() => window.removeEventListener('scroll', onScroll))

// Any navigation should dismiss the mobile menu rather than leave it covering the new page.
watch(
  () => route.fullPath,
  () => {
    ui.menuOpen = false
    accountOpen.value = false
  },
)

function goToCatalog(): void {
  router.push({ name: 'catalog' })
}

function signOut(): void {
  auth.signOut()
  ui.success('Signed out')
  router.push({ name: 'home' })
}
</script>

<template>
  <header class="nav-shell">
    <div class="announcement">
      <div class="container announcement__inner">
        <p>Free standard shipping on orders over $75</p>
        <p class="announcement__right">
          <RouterLink to="/products">Shop the edit</RouterLink>
          <span aria-hidden="true">·</span>
          <RouterLink to="/about">Why ShopSphere</RouterLink>
        </p>
      </div>
    </div>

    <nav class="navbar" :class="{ 'navbar--stuck': scrolled }" aria-label="Main">
      <div class="container navbar__inner">
        <RouterLink class="brand" :to="{ name: 'home' }" aria-label="ShopSphere home">
          <span class="brand__mark" aria-hidden="true" />
          <span class="brand__name">ShopSphere</span>
        </RouterLink>

        <ul class="navbar__links">
          <li v-for="link in links" :key="link.label">
            <RouterLink class="navbar__link" :to="link.to">{{ link.label }}</RouterLink>
          </li>
          <li v-if="auth.isAdmin">
            <RouterLink class="navbar__link navbar__link--admin" :to="{ name: 'admin-dashboard' }">
              Admin
            </RouterLink>
          </li>
        </ul>

        <div class="navbar__actions">
          <button class="icon-btn" type="button" aria-label="Search products" @click="ui.searchOpen = true">
            <Search :size="20" />
          </button>

          <div class="navbar__account">
            <button
              class="account-trigger"
              type="button"
              :aria-expanded="accountOpen"
              aria-haspopup="menu"
              @click="accountOpen = !accountOpen"
            >
              <User :size="20" />
              <span class="account-trigger__label">{{ auth.isAuthenticated ? auth.displayName.split(' ')[0] : 'Account' }}</span>
              <ChevronDown :size="15" aria-hidden="true" />
            </button>

            <Transition name="fade">
              <div v-if="accountOpen" class="account-menu" role="menu">
                <template v-if="auth.isAuthenticated">
                  <div class="account-menu__head">
                    <p class="account-menu__name">{{ auth.displayName }}</p>
                    <p class="account-menu__email">{{ auth.user?.email }}</p>
                    <span v-if="auth.isAdmin" class="badge badge--info">Administrator</span>
                  </div>
                  <RouterLink class="account-menu__item" role="menuitem" :to="{ name: 'account' }">
                    <LayoutDashboard :size="17" /> Dashboard
                  </RouterLink>
                  <RouterLink class="account-menu__item" role="menuitem" :to="{ name: 'orders' }">
                    <ShoppingBag :size="17" /> Orders
                  </RouterLink>
                  <RouterLink class="account-menu__item" role="menuitem" :to="{ name: 'notifications' }">
                    <Heart :size="17" /> Notifications
                  </RouterLink>
                  <RouterLink
                    v-if="auth.isAdmin"
                    class="account-menu__item"
                    role="menuitem"
                    :to="{ name: 'admin-dashboard' }"
                  >
                    <LayoutDashboard :size="17" /> Admin console
                  </RouterLink>
                  <button class="account-menu__item" role="menuitem" type="button" @click="signOut">
                    <LogOut :size="17" /> Sign out
                  </button>
                </template>
                <template v-else>
                  <RouterLink class="account-menu__item" role="menuitem" :to="{ name: 'login' }">
                    <User :size="17" /> Sign in
                  </RouterLink>
                  <RouterLink class="account-menu__item" role="menuitem" :to="{ name: 'register' }">
                    Create account
                  </RouterLink>
                </template>
              </div>
            </Transition>
          </div>

          <RouterLink class="icon-btn icon-btn--cart" :to="{ name: 'cart' }" :aria-label="`Cart, ${cart.count} items`">
            <ShoppingBag :size="20" />
            <span v-if="cart.count" class="cart-count">{{ cart.count > 99 ? '99+' : cart.count }}</span>
          </RouterLink>

          <button
            class="icon-btn navbar__burger"
            type="button"
            :aria-expanded="ui.menuOpen"
            aria-controls="mobile-menu"
            aria-label="Open menu"
            @click="ui.menuOpen = true"
          >
            <Menu :size="22" />
          </button>
        </div>
      </div>
    </nav>

    <SearchOverlay />

    <Teleport to="body">
      <Transition name="fade">
        <div v-if="ui.menuOpen" class="drawer-backdrop" @click="ui.menuOpen = false" />
      </Transition>
      <Transition name="fade">
        <div v-if="ui.menuOpen" id="mobile-menu" class="drawer drawer--left" role="dialog" aria-label="Menu">
          <div class="drawer__head">
            <p class="drawer__title">Menu</p>
            <button class="icon-btn" type="button" aria-label="Close menu" @click="ui.menuOpen = false">
              <X :size="20" />
            </button>
          </div>
          <ul class="drawer__nav">
            <li><RouterLink :to="{ name: 'catalog' }" @click="goToCatalog">Shop all</RouterLink></li>
            <li><RouterLink :to="{ name: 'catalog' }" @click="goToCatalog">Categories</RouterLink></li>
            <li><RouterLink :to="{ name: 'about' }">About</RouterLink></li>
            <li><RouterLink :to="{ name: 'cart' }">Cart ({{ cart.count }})</RouterLink></li>
            <li v-if="auth.isAuthenticated">
              <RouterLink :to="{ name: 'account' }">My account</RouterLink>
            </li>
            <li v-else>
              <RouterLink :to="{ name: 'login' }">Sign in</RouterLink>
            </li>
            <li v-if="auth.isAdmin">
              <RouterLink :to="{ name: 'admin-dashboard' }">Admin console</RouterLink>
            </li>
          </ul>
          <div class="drawer__foot">
            <button class="btn btn--outline btn--block" type="button" @click="ui.searchOpen = true">
              <Search :size="17" /> Search products
            </button>
          </div>
        </div>
      </Transition>
    </Teleport>
  </header>
</template>

<style scoped>
.nav-shell {
  position: sticky;
  top: 0;
  z-index: var(--z-nav);
}

.announcement {
  background: var(--color-primary);
  color: var(--color-inverse);
  font-size: var(--text-xs);
}

.announcement__inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-4);
  min-height: var(--announcement-height);
}

.announcement__right {
  display: none;
  align-items: center;
  gap: var(--space-3);
  color: rgba(255, 255, 255, 0.78);
}

.announcement a:hover {
  color: var(--color-inverse);
  text-decoration: underline;
}

.navbar {
  background: var(--color-surface);
  border-bottom: 1px solid var(--color-border);
  transition: box-shadow var(--duration-base) var(--ease-out);
}

.navbar--stuck {
  box-shadow: var(--shadow-md);
}

.navbar__inner {
  display: flex;
  align-items: center;
  gap: var(--space-6);
  min-height: var(--navbar-height);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: var(--weight-bold);
  font-size: var(--text-lg);
  letter-spacing: -0.03em;
  flex-shrink: 0;
}

.brand__mark {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  box-shadow: inset 0 0 0 4px var(--color-primary);
}

.navbar__links {
  display: none;
  align-items: center;
  gap: var(--space-5);
}

.navbar__link {
  position: relative;
  padding: var(--space-2) 0;
  font-size: var(--text-base);
  font-weight: var(--weight-medium);
  color: var(--color-text-secondary);
  transition: color var(--duration-fast) var(--ease-out);
}

.navbar__link::after {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 100%;
  height: 2px;
  background: var(--color-primary);
  transform: scaleX(0);
  transform-origin: left;
  transition: transform var(--duration-base) var(--ease-out);
}

.navbar__link:hover,
.navbar__link.router-link-active {
  color: var(--color-text);
}

.navbar__link:hover::after,
.navbar__link.router-link-active::after {
  transform: scaleX(1);
}

.navbar__link--admin {
  color: var(--color-accent);
}

.navbar__actions {
  display: flex;
  align-items: center;
  gap: var(--space-1);
  margin-left: auto;
}

.icon-btn--cart {
  position: relative;
}

.cart-count {
  position: absolute;
  top: 3px;
  right: 3px;
  min-width: 18px;
  height: 18px;
  padding: 0 4px;
  display: grid;
  place-items: center;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  color: var(--color-inverse);
  font-size: 10px;
  font-weight: var(--weight-bold);
  font-variant-numeric: tabular-nums;
}

.navbar__account {
  position: relative;
}

.account-trigger {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  height: 40px;
  padding: 0 var(--space-3);
  border-radius: var(--radius-md);
  color: var(--color-text-secondary);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
  transition: background-color var(--duration-fast) var(--ease-out);
}

.account-trigger:hover {
  background: var(--color-surface-alt);
  color: var(--color-text);
}

.account-trigger__label {
  display: none;
  max-width: 96px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.account-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 244px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-lg);
  padding: var(--space-2);
  z-index: var(--z-nav);
}

.account-menu__head {
  padding: var(--space-3);
  border-bottom: 1px solid var(--color-border);
  margin-bottom: var(--space-2);
}

.account-menu__name {
  font-weight: var(--weight-semibold);
  font-size: var(--text-sm);
}

.account-menu__email {
  font-size: var(--text-xs);
  color: var(--color-muted);
  margin-bottom: var(--space-2);
  overflow: hidden;
  text-overflow: ellipsis;
}

.account-menu__item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
  width: 100%;
  padding: var(--space-2) var(--space-3);
  border-radius: var(--radius-md);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
  text-align: left;
  transition:
    background-color var(--duration-fast) var(--ease-out),
    color var(--duration-fast) var(--ease-out);
}

.account-menu__item:hover {
  background: var(--color-surface-alt);
  color: var(--color-text);
}

.navbar__burger {
  display: inline-flex;
}

.drawer__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: var(--space-4) var(--space-5);
  border-bottom: 1px solid var(--color-border);
}

.drawer__title {
  font-weight: var(--weight-semibold);
}

.drawer__nav {
  flex: 1;
  padding: var(--space-3);
  display: flex;
  flex-direction: column;
}

.drawer__nav a {
  display: block;
  padding: var(--space-3) var(--space-4);
  border-radius: var(--radius-md);
  font-weight: var(--weight-medium);
  color: var(--color-text-secondary);
}

.drawer__nav a:hover,
.drawer__nav a.router-link-active {
  background: var(--color-surface-alt);
  color: var(--color-text);
}

.drawer__foot {
  padding: var(--space-4);
  border-top: 1px solid var(--color-border);
}

@media (min-width: 900px) {
  .navbar__links {
    display: flex;
  }
  .account-trigger__label {
    display: inline;
  }
  .navbar__burger {
    display: none;
  }
  .announcement__right {
    display: flex;
  }
}

@media (max-width: 899px) {
  .announcement__inner {
    justify-content: center;
  }
  .announcement__inner > p:first-child {
    text-align: center;
  }
}
</style>
