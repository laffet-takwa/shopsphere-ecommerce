<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { AlertCircle, Check } from '@lucide/vue'
import BaseInput from '../components/common/BaseInput.vue'
import BaseButton from '../components/common/BaseButton.vue'
import { useAuthStore } from '../stores/auth'
import { useUiStore } from '../stores/ui'

const auth = useAuthStore()
const ui = useUiStore()
const router = useRouter()

const form = reactive({
  firstName: '',
  lastName: '',
  email: '',
  password: '',
  confirmPassword: '',
})
const errors = reactive<Record<string, string>>({})
const accepted = ref(false)

/** Password strength is scored locally and never transmitted. */
const strength = computed(() => {
  const value = form.password
  if (!value) return { score: 0, label: '', tone: 'neutral' as const }
  let score = 0
  if (value.length >= 8) score += 1
  if (value.length >= 12) score += 1
  if (/[A-Z]/.test(value) && /[a-z]/.test(value)) score += 1
  if (/\d/.test(value)) score += 1
  if (/[^A-Za-z0-9]/.test(value)) score += 1

  if (score <= 2) return { score, label: 'Weak', tone: 'danger' as const }
  if (score === 3) return { score, label: 'Fair', tone: 'warning' as const }
  if (score === 4) return { score, label: 'Good', tone: 'info' as const }
  return { score, label: 'Strong', tone: 'success' as const }
})

async function submit(): Promise<void> {
  Object.keys(errors).forEach((key) => delete errors[key])

  if (!form.firstName.trim()) errors.firstName = 'First name is required.'
  if (!form.lastName.trim()) errors.lastName = 'Last name is required.'
  if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email)) errors.email = 'Enter a valid email address.'
  if (form.password.length < 8) errors.password = 'Use at least 8 characters.'
  if (form.password !== form.confirmPassword) errors.confirmPassword = 'Passwords do not match.'
  if (!accepted.value) errors.terms = 'Please accept the terms to continue.'

  if (Object.keys(errors).length) return

  const ok = await auth.register({
    firstName: form.firstName.trim(),
    lastName: form.lastName.trim(),
    email: form.email.trim(),
    password: form.password,
  })

  if (!ok) {
    errors.form = auth.error
    return
  }

  ui.success('Welcome to ShopSphere')
  router.push('/account')
}
</script>

<template>
  <div class="page container">
    <div class="register card card--pad">
      <header class="register__head">
        <h1 class="register__title">Create your account</h1>
        <p class="register__copy">Save a wishlist, track orders and check out in seconds.</p>
      </header>

      <form class="register__form" novalidate @submit.prevent="submit">
        <p v-if="errors.form" class="alert alert--error" role="alert">
          <AlertCircle :size="18" aria-hidden="true" /> {{ errors.form }}
        </p>

        <div class="row">
          <BaseInput v-model="form.firstName" label="First name" name="firstName" :error="errors.firstName" required autocomplete="given-name" />
          <BaseInput v-model="form.lastName" label="Last name" name="lastName" :error="errors.lastName" required autocomplete="family-name" />
        </div>

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

        <div>
          <BaseInput
            v-model="form.password"
            label="Password"
            name="password"
            type="password"
            :error="errors.password"
            required
            autocomplete="new-password"
          />
          <div v-if="form.password" class="strength">
            <div class="strength__track" aria-hidden="true">
              <span
                v-for="index in 5"
                :key="index"
                class="strength__seg"
                :class="{ 'is-on': index <= strength.score }"
                :data-tone="strength.tone"
              />
            </div>
            <p class="strength__label" :data-tone="strength.tone" aria-live="polite">
              {{ strength.label }} password
            </p>
          </div>
        </div>

        <BaseInput
          v-model="form.confirmPassword"
          label="Confirm password"
          name="confirmPassword"
          type="password"
          :error="errors.confirmPassword"
          required
          autocomplete="new-password"
        />

        <div>
          <label class="checkbox">
            <input v-model="accepted" type="checkbox" />
            I agree to the terms of service and privacy policy
          </label>
          <p v-if="errors.terms" class="field__error" role="alert">{{ errors.terms }}</p>
        </div>

        <BaseButton type="submit" variant="primary" size="lg" block :loading="auth.busy">
          {{ auth.busy ? 'Creating account…' : 'Create account' }}
        </BaseButton>
      </form>

      <ul class="register__perks">
        <li><Check :size="15" aria-hidden="true" /> Passwords are hashed with BCrypt</li>
        <li><Check :size="15" aria-hidden="true" /> JWT sessions, no card data stored</li>
        <li><Check :size="15" aria-hidden="true" /> Live order status over Kafka</li>
      </ul>

      <p class="register__foot">
        Already have an account?
        <RouterLink class="register__link" to="/login">Sign in</RouterLink>
      </p>
    </div>
  </div>
</template>

<style scoped>
.page {
  padding-block: var(--space-10) var(--space-16);
  display: flex;
  justify-content: center;
}

.register {
  width: 100%;
  max-width: 520px;
}

.register__head {
  margin-bottom: var(--space-6);
}

.register__title {
  font-size: var(--text-2xl);
}

.register__copy {
  margin-top: var(--space-2);
  color: var(--color-muted);
}

.register__form {
  display: flex;
  flex-direction: column;
  gap: var(--space-4);
}

.row {
  display: grid;
  gap: var(--space-4);
}

.strength {
  margin-top: var(--space-2);
}

.strength__track {
  display: flex;
  gap: 4px;
}

.strength__seg {
  flex: 1;
  height: 4px;
  border-radius: var(--radius-full);
  background: var(--color-border);
  transition: background-color var(--duration-base) var(--ease-out);
}

.strength__seg.is-on[data-tone='danger'] {
  background: var(--color-danger);
}
.strength__seg.is-on[data-tone='warning'] {
  background: var(--color-warning);
}
.strength__seg.is-on[data-tone='info'] {
  background: var(--color-accent);
}
.strength__seg.is-on[data-tone='success'] {
  background: var(--color-success);
}

.strength__label {
  margin-top: var(--space-2);
  font-size: var(--text-xs);
  font-weight: var(--weight-semibold);
  color: var(--color-muted);
}

.strength__label[data-tone='danger'] {
  color: var(--color-danger);
}
.strength__label[data-tone='warning'] {
  color: var(--color-warning);
}
.strength__label[data-tone='info'] {
  color: var(--color-accent);
}
.strength__label[data-tone='success'] {
  color: var(--color-success);
}

.register__perks {
  margin-top: var(--space-6);
  padding-top: var(--space-5);
  border-top: 1px solid var(--color-border);
  display: flex;
  flex-direction: column;
  gap: var(--space-2);
}

.register__perks li {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.register__perks svg {
  color: var(--color-success);
}

.register__foot {
  margin-top: var(--space-6);
  text-align: center;
  font-size: var(--text-base);
  color: var(--color-text-secondary);
}

.register__link {
  color: var(--color-accent);
  font-weight: var(--weight-medium);
}

.register__link:hover {
  text-decoration: underline;
}

@media (min-width: 560px) {
  .row {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
