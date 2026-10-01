import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from './stores/auth'

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior(_to, _from, saved) {
    return saved ?? { top: 0 }
  },
  routes: [
    { path: '/', name: 'home', component: () => import('./views/HomeView.vue') },
    { path: '/products', name: 'catalog', component: () => import('./views/CatalogView.vue') },
    {
      path: '/products/:id',
      name: 'product-detail',
      component: () => import('./views/ProductDetailView.vue'),
      props: true,
    },
    { path: '/cart', name: 'cart', component: () => import('./views/CartView.vue') },
    { path: '/about', name: 'about', component: () => import('./views/AboutView.vue') },

    // Checkout requires a session because the gateway resolves the order owner from the JWT.
    {
      path: '/checkout',
      name: 'checkout',
      component: () => import('./views/CheckoutView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/order/:id/confirmation',
      name: 'order-confirmation',
      component: () => import('./views/OrderConfirmationView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/orders',
      name: 'orders',
      component: () => import('./views/OrdersView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/orders/:id',
      name: 'order-detail',
      component: () => import('./views/OrderDetailView.vue'),
      props: true,
    },
    {
      path: '/account',
      name: 'account',
      component: () => import('./views/AccountView.vue'),
      meta: { requiresAuth: true },
    },
    {
      path: '/notifications',
      name: 'notifications',
      component: () => import('./views/NotificationsView.vue'),
      meta: { requiresAuth: true },
    },
    { path: '/login', name: 'login', component: () => import('./views/LoginView.vue') },
    { path: '/register', name: 'register', component: () => import('./views/RegisterView.vue') },

    {
      path: '/admin',
      name: 'admin-dashboard',
      component: () => import('./views/admin/AdminDashboardView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/products',
      name: 'admin-products',
      component: () => import('./views/admin/AdminProductsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/orders',
      name: 'admin-orders',
      component: () => import('./views/admin/AdminOrdersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/inventory',
      name: 'admin-inventory',
      component: () => import('./views/admin/AdminInventoryView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/customers',
      name: 'admin-customers',
      component: () => import('./views/admin/AdminCustomersView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/analytics',
      name: 'admin-analytics',
      component: () => import('./views/admin/AdminAnalyticsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },
    {
      path: '/admin/settings',
      name: 'admin-settings',
      component: () => import('./views/admin/AdminSettingsView.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
    },

    // Kept for compatibility with the previous URL shape.
    { path: '/account/orders', redirect: { name: 'orders' } },

    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('./views/StatusView.vue'),
      props: { code: '404', title: 'Page not found', kind: 'not-found' },
    },
  ],
})

/**
 * Route guards mirror the gateway's rules: authenticated routes need a token, admin routes need
 * the ADMIN role. These are a UX affordance only - the gateway still rejects every request that
 * arrives without a valid token or role, so bypassing the guard grants no access.
 */
router.beforeEach((to) => {
  const auth = useAuthStore()

  if (!to.meta.requiresAuth && !to.meta.requiresAdmin) return true
  if (!auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } }
  }
  if (to.meta.requiresAdmin && !auth.isAdmin) {
    return { name: 'account' }
  }
  return true
})

router.afterEach((to) => {
  const title = typeof to.meta.title === 'string' ? to.meta.title : 'ShopSphere'
  document.title = `${title} — ShopSphere`
})

export default router
