<script setup lang="ts">
import { Server, GitBranch, Database, Zap, ExternalLink } from '@lucide/vue'
import BaseButton from '../components/common/BaseButton.vue'

const pillars = [
  {
    icon: Server,
    title: 'Microservices, not a monolith',
    copy: 'Eight independently deployable Spring Boot services behind an API gateway, each owning its own data.',
  },
  {
    icon: GitBranch,
    title: 'Event driven end to end',
    copy: 'Six Kafka topics carry an order from placement through stock reservation, payment and notification.',
  },
  {
    icon: Database,
    title: 'Polyglot persistence',
    copy: 'PostgreSQL for transactional records, MongoDB for stock and notifications, database per service.',
  },
  {
    icon: Zap,
    title: 'Built for failure',
    copy: 'Idempotent consumers, retry topics and dead-letter routing stop one bad message stalling an order.',
  },
]

const stack = [
  { label: 'Backend', value: 'Java 17 · Spring Boot 3.3 · Spring Cloud 2023 · Spring Security' },
  { label: 'Messaging', value: 'Apache Kafka 3.9 in KRaft mode, retry topics and DLTs' },
  { label: 'Data', value: 'PostgreSQL 16 · MongoDB 7 · database per service' },
  { label: 'Frontend', value: 'Vue 3 · TypeScript · Vite · Pinia · Vue Router' },
  { label: 'Observability', value: 'Logstash JSON logs · Elasticsearch · Kibana' },
  { label: 'Delivery', value: 'Docker Compose · Kubernetes manifests · GitHub Actions CI' },
]

const journey = [
  {
    title: 'Order placed',
    copy: 'Order service snapshots product names and prices, then publishes order.created.',
  },
  {
    title: 'Stock reserved',
    copy: 'Inventory service reserves every line in MongoDB and emits inventory.reserved.',
  },
  {
    title: 'Payment processed',
    copy: 'A simulated processor authorises the order and publishes payment.completed.',
  },
  {
    title: 'You are notified',
    copy: 'Notification service persists each event once and surfaces it in your account.',
  },
]
</script>

<template>
  <div class="about">
    <section class="hero">
      <div class="container">
        <p class="eyebrow">About ShopSphere</p>
        <h1 class="hero__title">Everything you need. One seamless shopping experience.</h1>
        <p class="hero__lede">
          ShopSphere is a working reference for a cloud-native commerce platform. The storefront you
          are using is served by a single-page app that talks to one API gateway, which fans out to
          eight independent Spring Boot services.
        </p>
        <div class="hero__actions">
          <BaseButton variant="primary" size="lg" to="/products">Shop the catalog</BaseButton>
          <a
            class="btn btn--outline btn--lg"
            href="http://localhost:8761"
            target="_blank"
            rel="noopener noreferrer"
          >
            Service registry <ExternalLink :size="17" aria-hidden="true" />
          </a>
        </div>
      </div>
    </section>

    <section class="page-section">
      <div class="container">
        <div class="pillars">
          <article v-for="pillar in pillars" :key="pillar.title" class="pillar">
            <span class="pillar__icon" aria-hidden="true"><component :is="pillar.icon" :size="20" /></span>
            <h2 class="pillar__title">{{ pillar.title }}</h2>
            <p class="pillar__copy">{{ pillar.copy }}</p>
          </article>
        </div>
      </div>
    </section>

    <section class="page-section section--alt">
      <div class="container">
        <h2 class="section-title">Under the hood</h2>
        <dl class="stack">
          <div v-for="item in stack" :key="item.label">
            <dt>{{ item.label }}</dt>
            <dd>{{ item.value }}</dd>
          </div>
        </dl>
      </div>
    </section>

    <section class="page-section">
      <div class="container">
        <h2 class="section-title">What happens when you place an order</h2>
        <ol class="journey">
          <li v-for="(step, index) in journey" :key="step.title">
            <span class="journey__step" aria-hidden="true">{{ index + 1 }}</span>
            <div>
              <h3>{{ step.title }}</h3>
              <p>{{ step.copy }}</p>
            </div>
          </li>
        </ol>
      </div>
    </section>
  </div>
</template>

<style scoped>
.hero {
  background: linear-gradient(180deg, var(--color-primary-soft) 0%, var(--color-background) 100%);
  padding-block: clamp(var(--space-12), 8vw, var(--space-20));
}

.hero__title {
  margin-top: var(--space-4);
  max-width: 18ch;
}

.hero__lede {
  margin-top: var(--space-5);
  max-width: 58ch;
  font-size: var(--text-md);
}

.hero__actions {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-3);
  margin-top: var(--space-8);
}

.pillars {
  display: grid;
  gap: var(--space-4);
}

.pillar {
  padding: var(--space-6);
  background: var(--color-surface);
  border: 1px solid var(--color-border);
  border-radius: var(--radius-lg);
}

.pillar__icon {
  display: grid;
  place-items: center;
  width: 40px;
  height: 40px;
  border-radius: var(--radius-md);
  background: var(--color-primary-soft);
  color: var(--color-primary);
}

.pillar__title {
  margin-top: var(--space-4);
  font-size: var(--text-md);
  font-weight: var(--weight-semibold);
}

.pillar__copy {
  margin-top: var(--space-2);
  font-size: var(--text-base);
}

.section--alt {
  background: var(--color-surface-alt);
}

.section-title {
  font-size: var(--text-xl);
  margin-bottom: var(--space-6);
}

.stack > div {
  display: grid;
  gap: var(--space-2);
  padding-block: var(--space-3);
  border-bottom: 1px solid var(--color-border);
}

.stack dt {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  letter-spacing: var(--tracking-wide);
  text-transform: uppercase;
  color: var(--color-muted);
}

.stack dd {
  color: var(--color-text-secondary);
}

.journey {
  display: grid;
  gap: var(--space-5);
}

.journey li {
  display: flex;
  gap: var(--space-4);
}

.journey__step {
  display: grid;
  place-items: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: var(--radius-full);
  background: var(--color-primary);
  color: var(--color-inverse);
  font-size: var(--text-sm);
  font-weight: var(--weight-bold);
}

.journey h3 {
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
}

.journey p {
  margin-top: var(--space-1);
  font-size: var(--text-base);
}

@media (min-width: 640px) {
  .pillars {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
  .stack > div {
    grid-template-columns: 160px 1fr;
    align-items: baseline;
  }
}

@media (min-width: 900px) {
  .pillars {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
  .journey {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
