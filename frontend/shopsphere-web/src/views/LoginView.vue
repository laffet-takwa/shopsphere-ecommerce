<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { AlertCircle, Eye, EyeOff, ArrowRight } from '@lucide/vue'
import BaseInput from '../components/common/BaseInput.vue'
import BaseButton from '../components/common/BaseButton.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()
const route = useRoute()

const form = reactive({ email: '', password: '' })
const errors = reactive<Record<string, string>>({})
const showPassword = ref(false)

async function submit(): Promise<void> {
  Object.keys(errors).forEach((key) => delete errors[key])
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email)) errors.email = 'Enter a valid email address.'
  if (form.password.length < 6) errors.password = 'Password must be at least 6 characters.'
  if (Object.keys(errors).length) return

  const ok = await auth.login({ email: form.email.trim(), password: form.password })
  if (!ok) {
    errors.form = auth.error
    return
  }

  ui.success('Signed in')
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : '/account'
  router.push(redirect)
}
</script>

<template>
  <div class="auth">
    <div class="auth__panel">
      <div class="auth__inner">
        <RouterLink class="auth__brand" to="/">
          <span class="auth__mark" aria-hidden="true" />
          <span>ShopSphere</span>
        </RouterLink>

        <h1 class="auth__title">Welcome back</h1>
        <p class="auth__copy">Sign in to track orders, manage your wishlist and check out faster.</p>

        <form novalidate @submit.prevent="submit">
          <p v-if="errors.form" class="alert alert--error" role="alert">
            <AlertCircle :size="18" aria-hidden="true" /> {{ errors.form }}
          </p>

          <BaseInput
            v-model="form.email"
            label="Email"
            name="email"
            type="email"
            inputmode="email"
            placeholder="you@example.com"
            :error="errors.email"
            required
            autocomplete="email"
          />

          <div class="auth__password">
            <BaseInput
              v-model="form.password"
              label="Password"
              name="password"
              :type="showPassword ? 'text' : 'password'"
              :error="errors.password"
              required
              autocomplete="current-password"
            />
            <button
              class="auth__toggle"
              type="button"
              :aria-label="showPassword ? 'Hide password' : 'Show password'"
              @click="showPassword = !showPassword"
            >
              <EyeOff v-if="showPassword" :size="18" />
              <Eye v-else :size="18" />
            </button>
          </div>

          <div class="auth__row">
            <label class="checkbox">
              <input type="checkbox" checked /> Remember me
            </label>
            <button class="auth__link" type="button" @click="ui.notify('Password reset is not wired up in this demo', 'info')">
              Forgot password?
            </button>
          </div>

          <BaseButton type="submit" variant="primary" size="lg" block :loading="auth.busy">
            {{ auth.busy ? 'Signing in…' : 'Sign in' }}
          </BaseButton>
        </form>

        <p class="auth__foot">
          Don't have an account?
          <RouterLink class="auth__link" to="/register">Create account</RouterLink>
        </p>

        <div class="auth__hint">
          <p class="auth__hint-title">Demo accounts</p>
          <p class="auth__hint-copy">
            Seed the stack with <code>ADMIN_EMAIL</code> / <code>ADMIN_PASSWORD</code> in
            <code>.env</code> to get an administrator, then sign in from
            <code>/admin</code>.
          </p>
        </div>
      </div>
    </div>

    <aside class="auth__aside" aria-hidden="true">
      <div class="auth__aside-inner">
        <p class="auth__quote">“Everything you need. One seamless shopping experience.”</p>
        <ul class="auth__points">
          <li>Track every order from placement to delivery</li>
          <li>Save products to a wishlist that persists</li>
          <li>Secure simulated checkout with live status</li>
        </ul>
        <RouterLink class="auth__aside-link" to="/products">
          Browse the catalog <ArrowRight :size="16" />
        </RouterLink>
      </div>
    </aside>
  </div>
</template>

<style scoped>
.auth {
  display: grid;
  min-height: calc(100vh - var(--navbar-height) - var(--announcement-height));
}

.auth__panel {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: var(--space-8) var(--space-5);
}

.auth__inner {
  width: 100%;
  max-width: 420px;
}

.auth__brand {
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-weight: var(--weight-bold);
  font-size: var(--text-md);
  letter-spacing: -0.02em;
}

.auth__mark {
  width: 18px;
  height: 18px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
  box-shadow: inset 0 0 0 4px var(--color-primary);
}

.auth__title {
  margin-top: var(--space-6);
  font-size: var(--text-2xl);
}

.auth__copy {
  margin-top: var(--space-2);
  color: var(--color-muted);
  font-size: var(--text-base);
  margin-bottom: var(--space-6);
}

form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.auth__password {
  position: relative;
}

.auth__toggle {
  position: absolute;
  right: var(--space-2);
  bottom: 4px;
  display: grid;
  place-items: center;
  width: 36px;
  height: 36px;
  color: var(--color-muted);
  border-radius: var(--radius-sm);
}

.auth__toggle:hover {
  color: var(--color-text);
}

.auth__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-3);
  flex-wrap: wrap;
}

.auth__link {
  color: var(--color-accent);
  font-size: var(--text-sm);
  font-weight: var(--weight-medium);
}

.auth__link:hover {
  text-decoration: underline;
}

.auth__foot {
  margin-top: var(--space-6);
  font-size: var(--text-base);
  color: var(--color-text-secondary);
  text-align: center;
}

.auth__hint {
  margin-top: var(--space-8);
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--color-surface-alt);
}

.auth__hint-title {
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  text-transform: uppercase;
  letter-spacing: var(--tracking-wide);
  color: var(--color-muted);
}

.auth__hint-copy {
  margin-top: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-text-secondary);
}

.auth__hint code {
  font-family: var(--font-mono);
  font-size: 0.85em;
  padding: 1px 4px;
  border-radius: 4px;
  background: var(--color-surface);
  border: 1px solid var(--color-border);
}

.auth__aside {
  display: none;
  position: relative;
  background: var(--color-primary);
  color: var(--color-inverse);
  overflow: hidden;
}

.auth__aside::after {
  content: '';
  position: absolute;
  inset: auto -20% -30% -20%;
  height: 70%;
  background: radial-gradient(closest-side, rgba(43, 107, 255, 0.35), transparent);
}

.auth__aside-inner {
  position: relative;
  z-index: 1;
  padding: var(--space-12);
  display: flex;
  flex-direction: column;
  justify-content: center;
  height: 100%;
}

.auth__quote {
  font-size: clamp(1.4rem, 2.4vw, var(--text-2xl));
  font-weight: var(--weight-semibold);
  letter-spacing: -0.02em;
  line-height: var(--leading-tight);
  max-width: 22ch;
}

.auth__points {
  margin-top: var(--space-8);
  display: flex;
  flex-direction: column;
  gap: var(--space-3);
}

.auth__points li {
  position: relative;
  padding-left: var(--space-6);
  color: rgba(255, 255, 255, 0.76);
  font-size: var(--text-base);
}

.auth__points li::before {
  content: '';
  position: absolute;
  left: 0;
  top: 9px;
  width: 6px;
  height: 6px;
  border-radius: var(--radius-full);
  background: var(--color-accent);
}

.auth__aside-link {
  margin-top: var(--space-8);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-base);
  font-weight: var(--weight-semibold);
  color: var(--color-inverse);
}

.auth__aside-link:hover {
  text-decoration: underline;
}

@media (min-width: 900px) {
  .auth {
    grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  }
  .auth__aside {
    display: block;
  }
  .auth__panel {
    padding: var(--space-12);
  }
}
</style>
