<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AppNavbar from './components/layout/AppNavbar.vue'
import AppFooter from './components/layout/AppFooter.vue'
import ToastHost from './components/common/ToastHost.vue'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const auth = useAuthStore()

/**
 * The admin console brings its own navigation and fills the viewport, so the storefront chrome is
 * suppressed there. Route metadata is the single switch both concerns agree on.
 */
const isAdminArea = computed(() => route.path.startsWith('/admin'))

const metaTitle = computed(() => {
  if (typeof route.meta.title === 'string') return route.meta.title
  const name = String(route.name ?? '')
  if (name === 'home') return 'Everything you need. One seamless shopping experience.'
  if (name === 'catalog') return 'All products'
  if (name === 'cart') return 'Your cart'
  if (name === 'checkout') return 'Checkout'
  if (name === 'orders') return 'Your orders'
  if (name === 'account') return 'Your dashboard'
  if (name === 'login') return 'Sign in'
  if (name === 'register') return 'Create account'
  if (name === 'notifications') return 'Notifications'
  return ''
})
</script>

<template>
  <div class="app">
    <a class="skip-link" href="#main">Skip to content</a>

    <template v-if="!isAdminArea">
      <AppNavbar />
    </template>

    <main id="main" class="app__main" :class="{ 'app__main--admin': isAdminArea }">
      <RouterView v-slot="{ Component, route: current }">
        <Transition name="page" mode="out-in">
          <component :is="Component" :key="current.path" />
        </Transition>
      </RouterView>
    </main>

    <AppFooter v-if="!isAdminArea && !route.meta.hideFooter" />

    <ToastHost />
  </div>
</template>

<style scoped>
.app {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
}

.app__main {
  flex: 1;
}

.app__main--admin {
  display: flex;
  flex-direction: column;
}
</style>
