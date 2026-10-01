<script setup lang="ts">
import { Truck, ShieldCheck, RotateCcw, Headset } from '@lucide/vue'

const year = new Date().getFullYear()

const promises = [
  { icon: Truck, title: 'Free Shipping', copy: 'On every order over $75' },
  { icon: ShieldCheck, title: 'Secure Payments', copy: 'Protected checkout every time' },
  { icon: RotateCcw, title: 'Easy Returns', copy: '30 days, no questions asked' },
  { icon: Headset, title: '24/7 Support', copy: 'Real people, any hour' },
]

const columns = [
  {
    title: 'Shop',
    links: [
      { label: 'All products', to: { name: 'catalog' } },
      { label: 'Electronics', to: { name: 'catalog', query: { category: 'Tech' } } },
      { label: 'Home & Living', to: { name: 'catalog', query: { category: 'Home' } } },
      { label: 'Wellness', to: { name: 'catalog', query: { category: 'Wellness' } } },
    ],
  },
  {
    title: 'Account',
    links: [
      { label: 'My account', to: { name: 'account' } },
      { label: 'Orders', to: { name: 'orders' } },
      { label: 'Notifications', to: { name: 'notifications' } },
      { label: 'Wishlist', to: { name: 'account', query: { tab: 'wishlist' } } },
    ],
  },
  {
    title: 'Company',
    links: [
      { label: 'About', to: { name: 'about' } },
      { label: 'Careers', to: { name: 'about' } },
      { label: 'Contact', to: { name: 'about' } },
      { label: 'Help centre', to: { name: 'about' } },
    ],
  },
]
</script>

<template>
  <footer class="footer">
    <section class="promises" aria-label="Service promises">
      <div class="container promises__grid">
        <div v-for="item in promises" :key="item.title" class="promise">
          <span class="promise__icon" aria-hidden="true">
            <component :is="item.icon" :size="20" />
          </span>
          <span class="promise__body">
            <span class="promise__title">{{ item.title }}</span>
            <span class="promise__copy">{{ item.copy }}</span>
          </span>
        </div>
      </div>
    </section>

    <div class="container footer__main">
      <div class="footer__brand">
        <RouterLink class="brand" :to="{ name: 'home' }">
          <span class="brand__mark" aria-hidden="true" />
          <span class="brand__name">ShopSphere</span>
        </RouterLink>
        <p class="footer__tagline">
          Everything you need. One seamless shopping experience.
        </p>
        <p class="footer__note">
          A microservices reference implementation — Spring Boot, Spring Cloud, Kafka, PostgreSQL,
          MongoDB and Elasticsearch behind a single API gateway.
        </p>
      </div>

      <nav v-for="column in columns" :key="column.title" class="footer__column" :aria-label="column.title">
        <h2 class="footer__heading">{{ column.title }}</h2>
        <ul>
          <li v-for="link in column.links" :key="link.label">
            <RouterLink class="footer__link" :to="link.to">{{ link.label }}</RouterLink>
          </li>
        </ul>
      </nav>
    </div>

    <div class="container footer__bottom">
      <p>© {{ year }} ShopSphere. Built as a portfolio project.</p>
      <p class="footer__legal">
        <span>Privacy</span><span aria-hidden="true">·</span><span>Terms</span><span aria-hidden="true">·</span
        ><span>Cookies</span>
      </p>
    </div>
  </footer>
</template>

<style scoped>
.footer {
  margin-top: var(--space-20);
  background: var(--color-surface);
  border-top: 1px solid var(--color-border);
}

.promises {
  border-bottom: 1px solid var(--color-border);
  background: var(--color-surface-alt);
}

.promises__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-5);
  padding-block: var(--space-8);
}

.promise {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.promise__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: var(--color-surface);
  color: var(--color-primary);
  box-shadow: var(--shadow-xs);
}

.promise__body {
  display: flex;
  flex-direction: column;
}

.promise__title {
  font-size: var(--text-sm);
  font-weight: var(--weight-semibold);
}

.promise__copy {
  font-size: var(--text-xs);
  color: var(--color-muted);
}

.footer__main {
  display: grid;
  grid-template-columns: 1fr;
  gap: var(--space-8);
  padding-block: var(--space-12);
}

.brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: var(--weight-bold);
  font-size: var(--text-lg);
  letter-spacing: -0.03em;
}

.brand__mark {
  width: 20px;
  height: 20px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  box-shadow: inset 0 0 0 4px var(--color-primary);
}

.footer__tagline {
  margin-top: var(--space-4);
  font-size: var(--text-md);
  font-weight: var(--weight-medium);
  color: var(--color-text);
  max-width: 34ch;
}

.footer__note {
  margin-top: var(--space-3);
  font-size: var(--text-sm);
  color: var(--color-muted);
  max-width: 46ch;
}

.footer__heading {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wider);
  text-transform: uppercase;
  color: var(--color-muted);
  margin-bottom: var(--space-4);
}

.footer__column li + li {
  margin-top: var(--space-2);
}

.footer__link {
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  transition: color var(--duration-fast) var(--ease-out);
}

.footer__link:hover {
  color: var(--color-primary);
}

.footer__bottom {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  padding-block: var(--space-5);
  border-top: 1px solid var(--color-border);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.footer__legal {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

@media (min-width: 700px) {
  .promises__grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}

@media (min-width: 900px) {
  .footer__main {
    grid-template-columns: 1.6fr repeat(3, 1fr);
  }
}
</style>
