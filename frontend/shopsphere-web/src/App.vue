<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ArrowDown, ArrowRight, Check, ChevronDown, Minus, Plus, Search, ShoppingBag, UserRound, X } from '@lucide/vue'
import { api, imageFor, type Product } from './api.ts'
import { RouterLink } from 'vue-router'
import { useShopStore } from './stores/shop.ts'

const products = ref<Product[]>([])
const shop = useShopStore()
const categories = ['All', 'Home', 'Tech', 'Wellness', 'Accessories']
const selectedCategory = ref('All')
const search = ref('')
const loading = ref(true)
const error = ref('')
const cartOpen = ref(false)
const accountOpen = ref(false)
const isRegister = ref(false)
const authBusy = ref(false)
const notice = ref('')
const form = ref({ firstName: '', lastName: '', email: '', password: '' })

const filteredProducts = computed(() => products.value.filter((product) => {
  const matchesCategory = selectedCategory.value === 'All' || product.category.toLowerCase() === selectedCategory.value.toLowerCase()
  const term = search.value.trim().toLowerCase()
  return matchesCategory && (!term || `${product.name} ${product.description} ${product.category}`.toLowerCase().includes(term))
}))

async function loadProducts() {
  loading.value = true
  error.value = ''
  try {
    const { data } = await api.get('/api/products', { params: { size: 48, sort: 'createdAt,desc' } })
    products.value = data.content ?? data
  } catch {
    error.value = 'The catalog could not be reached. Check that ShopSphere services are running, then try again.'
  } finally {
    loading.value = false
  }
}

function addToCart(product: Product) {
  shop.addToCart(product)
  notice.value = `${product.name} added to your bag`
  window.setTimeout(() => { notice.value = '' }, 2200)
}

function changeQuantity(productId: number, change: number) {
  shop.changeQuantity(productId, change)
}

async function submitAuth() {
  authBusy.value = true
  error.value = ''
  try {
    const path = isRegister.value ? '/api/auth/register' : '/api/auth/login'
    const body = isRegister.value ? form.value : { email: form.value.email, password: form.value.password }
    const { data } = await api.post(path, body)
    shop.setSession(data.accessToken, data.user.firstName)
    accountOpen.value = false
    notice.value = `Welcome${shop.userName ? `, ${shop.userName}` : ''}`
    window.setTimeout(() => { notice.value = '' }, 2200)
  } catch (requestError: unknown) {
    error.value = 'We could not sign you in. Check your details and try again.'
  } finally {
    authBusy.value = false
  }
}

async function checkout() {
  if (!shop.token) { accountOpen.value = true; cartOpen.value = false; return }
  try {
    await api.post('/api/orders', { items: shop.cart.map(({ product, quantity }) => ({ productId: product.id, quantity })) })
    shop.cart = []
    cartOpen.value = false
    notice.value = 'Order placed. We’ll keep you posted.'
  } catch {
    notice.value = 'Checkout could not be completed. Please try again.'
  }
  window.setTimeout(() => { notice.value = '' }, 3200)
}

function signOut() {
  shop.signOut()
}

onMounted(loadProducts)
</script>

<template>
  <div class="storefront">
    <div class="announcement">A little more considered. A little less ordinary. <span>Free shipping over $75</span></div>
    <header class="site-header">
      <a class="wordmark" href="#top" aria-label="ShopSphere home">shop<span>sphere</span><i>.</i></a>
      <nav class="main-nav" aria-label="Main navigation">
        <RouterLink to="/#collection">Shop all</RouterLink><a href="#collection" @click="selectedCategory = 'Home'">Home & living</a><a href="#collection" @click="selectedCategory = 'Tech'">Everyday tech</a>
      </nav>
      <div class="header-actions">
        <label class="search-box"><Search :size="17" /><input v-model="search" aria-label="Search products" placeholder="Search the store" /></label>
        <RouterLink v-if="shop.token" class="orders-shortcut" to="/account/orders">Orders</RouterLink>
        <button class="icon-button account-button" :aria-label="shop.token ? 'Sign out' : 'Account'" @click="shop.token ? signOut() : (accountOpen = true)"><UserRound :size="19" /><span>{{ shop.token ? shop.userName : 'Account' }}</span></button>
        <button class="icon-button bag-button" aria-label="Open shopping bag" @click="cartOpen = true"><ShoppingBag :size="19" /><span>Bag</span><b>{{ shop.cartCount }}</b></button>
      </div>
    </header>

    <main id="top">
      <section class="hero">
        <div class="hero-copy">
          <p class="eyebrow"><span class="eyebrow-dot"></span> THE EVERYDAY EDIT · VOL. 04</p>
          <h1>Good things,<br /><em>well chosen.</em></h1>
          <p class="hero-description">Useful objects with a little more thought in them. Made to be reached for, day after day.</p>
          <a class="hero-link" href="#collection">Explore the collection <ArrowRight :size="17" /></a>
          <div class="hero-meta"><span>01 — 04</span><span class="meta-rule"></span><span>Small rituals, better made</span></div>
        </div>
        <div class="hero-art" role="img" aria-label="Carefully arranged home and lifestyle essentials">
          <div class="art-note"><span>THE NEW<br />EVERYDAY</span><ArrowDown :size="16" /></div>
          <div class="art-stamp">OBJECTS<br />WITH<br />INTENTION</div>
        </div>
        <div class="hero-index">SS / 2026</div>
      </section>

      <section class="collection" id="collection">
        <div class="collection-heading">
          <div><p class="eyebrow">A GOOD PLACE TO START</p><h2>Find your <em>everyday.</em></h2></div>
          <p class="collection-count">{{ filteredProducts.length.toString().padStart(2, '0') }} pieces <span>in the edit</span></p>
        </div>
        <div class="filters-row">
          <div class="category-tabs" role="tablist" aria-label="Product categories">
            <button v-for="category in categories" :key="category" :class="['category-tab', { active: selectedCategory === category }]" @click="selectedCategory = category">{{ category }}</button>
          </div>
          <button class="sort-control" @click="loadProducts">Latest arrivals <ChevronDown :size="15" /></button>
        </div>

        <div v-if="loading" class="state-message"><span class="loader"></span> Finding the good stuff…</div>
        <div v-else-if="error" class="state-message error-state"><p>{{ error }}</p><button class="outline-button" @click="loadProducts">Try again <ArrowRight :size="15" /></button></div>
        <div v-else-if="filteredProducts.length" class="product-grid">
          <article v-for="(product, index) in filteredProducts" :key="product.id" class="product-card" :style="{ '--card-index': index }">
            <div class="product-image-wrap">
              <img :src="imageFor(product)" :alt="product.name" loading="lazy" />
              <span v-if="index < 2" class="product-tag">THE EDIT</span>
              <button class="quick-add" :aria-label="`Add ${product.name} to bag`" @click="addToCart(product)"><Plus :size="19" /></button>
            </div>
            <div class="product-info"><div><p class="product-category">{{ product.category }}</p><h3>{{ product.name }}</h3></div><span class="product-price">${{ Number(product.price).toFixed(2) }}</span></div>
            <p class="product-description">{{ product.description }}</p>
          </article>
        </div>
        <div v-else class="state-message empty-state">No pieces found here just yet. Try another search.</div>
      </section>

      <section class="closing-note"><span class="closing-mark">SS</span><p>Less, but <em>better.</em></p><a href="#top">Back to the top <ArrowRight :size="15" /></a></section>
    </main>
    <footer><a class="wordmark footer-mark" href="#top">shop<span>sphere</span><i>.</i></a><span>Thoughtful goods for everyday living.</span><span>© ShopSphere 2026</span></footer>

    <Transition name="fade"><div v-if="cartOpen || accountOpen" class="overlay" @click.self="cartOpen = false; accountOpen = false"></div></Transition>
    <Transition name="drawer"><aside v-if="cartOpen" class="side-panel" aria-label="Shopping bag">
      <div class="panel-head"><div><p class="eyebrow">YOUR SELECTION</p><h2>Your bag <span>({{ shop.cartCount }})</span></h2></div><button class="close-button" aria-label="Close bag" @click="cartOpen = false"><X /></button></div>
      <div v-if="shop.cart.length" class="bag-lines"><div v-for="line in shop.cart" :key="line.product.id" class="bag-line"><img :src="imageFor(line.product)" :alt="line.product.name" /><div class="bag-line-info"><p class="product-category">{{ line.product.category }}</p><h3>{{ line.product.name }}</h3><span>${{ Number(line.product.price).toFixed(2) }}</span><div class="quantity-control"><button :aria-label="`Decrease ${line.product.name} quantity`" @click="changeQuantity(line.product.id, -1)"><Minus :size="13" /></button><span>{{ line.quantity }}</span><button :aria-label="`Increase ${line.product.name} quantity`" @click="changeQuantity(line.product.id, 1)"><Plus :size="13" /></button></div></div></div></div>
      <div v-else class="empty-bag"><ShoppingBag :size="27" /><p>Your bag is taking a quiet moment.</p><button @click="cartOpen = false">Explore the collection <ArrowRight :size="15" /></button></div>
      <div v-if="shop.cart.length" class="bag-footer"><p><span>Subtotal</span><strong>${{ shop.cartTotal.toFixed(2) }}</strong></p><small>Shipping and taxes calculated at checkout.</small><button class="checkout-button" @click="checkout">Continue to checkout <ArrowRight :size="17" /></button></div>
    </aside></Transition>
    <Transition name="modal"><section v-if="accountOpen" class="account-modal" role="dialog" aria-modal="true" aria-labelledby="account-title">
      <button class="close-button modal-close" aria-label="Close account" @click="accountOpen = false"><X /></button><p class="eyebrow">WELCOME TO THE EVERYDAY</p><h2 id="account-title">{{ isRegister ? 'Make yourself at home.' : 'Good to have you back.' }}</h2>
      <form @submit.prevent="submitAuth">
        <template v-if="isRegister"><label>First name<input v-model="form.firstName" autocomplete="given-name" required /></label><label>Last name<input v-model="form.lastName" autocomplete="family-name" required /></label></template>
        <label>Email address<input v-model="form.email" type="email" autocomplete="email" required /></label><label>Password<input v-model="form.password" type="password" :autocomplete="isRegister ? 'new-password' : 'current-password'" minlength="8" required /></label>
        <p v-if="error" class="form-error">{{ error }}</p><button class="checkout-button" :disabled="authBusy">{{ authBusy ? 'One moment…' : isRegister ? 'Create account' : 'Sign in' }} <ArrowRight :size="17" /></button>
      </form>
      <button class="switch-auth" @click="isRegister = !isRegister; error = ''">{{ isRegister ? 'Already have an account? Sign in' : 'New around here? Create an account' }}</button>
    </section></Transition>
    <Transition name="toast"><div v-if="notice" class="toast"><Check :size="17" />{{ notice }}</div></Transition>
  </div>
</template>