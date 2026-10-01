<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Check, CreditCard, Lock, User, Truck, AlertCircle } from '@lucide/vue'
import OrderSummaryCard from '../components/cart/OrderSummaryCard.vue'
import BaseButton from '../components/common/BaseButton.vue'
import BaseInput from '../components/common/BaseInput.vue'
import { useCartStore } from '../stores/cart'
import { useOrderStore } from '../stores/order'
import { useUiStore } from '../stores/ui'
import { useAuthStore } from '../stores/auth'

type StepKey = 'information' | 'shipping' | 'payment'

const cart = useCartStore()
const orders = useOrderStore()
const ui = useUiStore()
const auth = useAuthStore()
const router = useRouter()

const STEPS: Array<{ key: StepKey; label: string }> = [
  { key: 'information', label: 'Information' },
  { key: 'shipping', label: 'Shipping' },
  { key: 'payment', label: 'Payment' },
]

const stepIndex = ref(0)
const form = reactive({
  firstName: auth.user?.firstName ?? '',
  lastName: auth.user?.lastName ?? '',
  email: auth.user?.email ?? '',
  phone: '',
  address: '',
  city: '',
  postalCode: '',
  country: 'United States',
  cardName: '',
  cardNumber: '',
  expiry: '',
  cvc: '',
})
const errors = reactive<Record<string, string>>({})

const currentStep = computed(() => STEPS[stepIndex.value])

function validate(step: StepKey): boolean {
  Object.keys(errors).forEach((key) => delete errors[key])

  if (step === 'information' || step === 'shipping') {
    if (!form.firstName.trim()) errors.firstName = 'First name is required.'
    if (!form.lastName.trim()) errors.lastName = 'Last name is required.'
    if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(form.email)) errors.email = 'Enter a valid email address.'
  }

  if (step === 'shipping') {
    if (!form.address.trim()) errors.address = 'Street address is required.'
    if (!form.city.trim()) errors.city = 'City is required.'
    if (!/^[a-z0-9][a-z0-9\s-]{2,11}$/i.test(form.postalCode.trim())) {
      errors.postalCode = 'Enter a valid postal code.'
    }
  }

  if (step === 'payment') {
    if (!form.cardName.trim()) errors.cardName = 'Name on card is required.'
    const digits = form.cardNumber.replace(/\s/g, '')
    if (!/^\d{15,16}$/.test(digits)) errors.cardNumber = 'Enter a 15 or 16 digit card number.'
    if (!/^(0[1-9]|1[0-2])\s*\/\s*\d{2}$/.test(form.expiry.trim())) {
      errors.expiry = 'Use MM/YY format.'
    }
    if (!/^\d{3,4}$/.test(form.cvc.trim())) errors.cvc = 'Enter the 3 or 4 digit security code.'
  }

  return Object.keys(errors).length === 0
}

function next(): void {
  if (!validate(currentStep.value.key)) return
  if (stepIndex.value < STEPS.length - 1) {
    stepIndex.value += 1
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

function back(): void {
  if (stepIndex.value > 0) {
    stepIndex.value -= 1
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }
}

/**
 * Card fields are validated for shape only and are never sent anywhere: the request body contains
 * order lines and the gateway resolves the user from the JWT. Payment itself is simulated
 * downstream by payment-service after inventory is reserved.
 */
async function placeOrder(): Promise<void> {
  if (!validate('payment')) return

  const created = await orders.create(cart.toOrderPayload())
  if (!created) {
    ui.error(orders.error || 'We could not place your order. Please try again.')
    return
  }

  cart.clear()
  ui.success(`Order #${created.id} placed`)
  router.push({ name: 'order-confirmation', params: { id: created.id } })
}
</script>

<template>
  <div class="page container">
    <header class="page__head">
      <h1 class="page__title">Checkout</h1>
      <p class="page__secure">
        <Lock :size="14" aria-hidden="true" /> Secure checkout · payment is simulated in this demo
      </p>
    </header>

    <ol class="steps" aria-label="Checkout progress">
      <li
        v-for="(step, index) in STEPS"
        :key="step.key"
        class="steps__item"
        :class="{ 'is-done': index < stepIndex, 'is-current': index === stepIndex }"
        :aria-current="index === stepIndex ? 'step' : undefined"
      >
        <span class="steps__marker" aria-hidden="true">
          <Check v-if="index < stepIndex" :size="14" :stroke-width="3" />
          <span v-else>{{ index + 1 }}</span>
        </span>
        <span class="steps__label">{{ step.label }}</span>
      </li>
    </ol>

    <div class="checkout">
      <section class="checkout__form card" aria-labelledby="step-title">
        <div class="card__head">
          <h2 id="step-title" class="card__title">{{ currentStep.label }}</h2>
          <component
            :is="currentStep.key === 'information' ? User : currentStep.key === 'shipping' ? Truck : CreditCard"
            :size="18"
            aria-hidden="true"
          />
        </div>

        <div class="card__body">
          <p v-if="orders.error" class="alert alert--error" role="alert">
            <AlertCircle :size="18" aria-hidden="true" /> {{ orders.error }}
          </p>

          <div v-if="currentStep.key === 'information'" class="grid2">
            <BaseInput v-model="form.firstName" label="First name" name="firstName" :error="errors.firstName" required autocomplete="given-name" />
            <BaseInput v-model="form.lastName" label="Last name" name="lastName" :error="errors.lastName" required autocomplete="family-name" />
            <BaseInput v-model="form.email" label="Email" name="email" type="email" inputmode="email" :error="errors.email" required autocomplete="email" />
            <BaseInput v-model="form.phone" label="Phone" name="phone" type="tel" inputmode="tel" hint="Optional — used for delivery updates." autocomplete="tel" />
          </div>

          <div v-else-if="currentStep.key === 'shipping'" class="grid2">
            <BaseInput v-model="form.address" label="Street address" name="address" :error="errors.address" required autocomplete="street-address" />
            <BaseInput v-model="form.city" label="City" name="city" :error="errors.city" required autocomplete="address-level2" />
            <BaseInput v-model="form.postalCode" label="Postal code" name="postalCode" :error="errors.postalCode" required autocomplete="postal-code" />
            <div class="field">
              <label class="field__label" for="country">Country</label>
              <select id="country" v-model="form.country" class="select">
                <option>United States</option>
                <option>United Kingdom</option>
                <option>Germany</option>
                <option>France</option>
                <option>Netherlands</option>
                <option>Canada</option>
                <option>Australia</option>
              </select>
            </div>
          </div>

          <div v-else class="grid2">
            <div class="sim-note">
              <strong>Simulated payment</strong>
              <p>
                This project never contacts a payment provider. Card details are validated for shape
                only, never stored, and never leave your browser.
              </p>
            </div>
            <BaseInput v-model="form.cardName" label="Name on card" name="cardName" :error="errors.cardName" required autocomplete="cc-name" />
            <BaseInput v-model="form.cardNumber" label="Card number" name="cardNumber" inputmode="numeric" :error="errors.cardNumber" required autocomplete="cc-number" hint="Use 4242 4242 4242 4242 for any test order." />
            <BaseInput v-model="form.expiry" label="Expiry" name="expiry" placeholder="MM/YY" inputmode="numeric" :error="errors.expiry" required autocomplete="cc-exp" />
            <BaseInput v-model="form.cvc" label="CVC" name="cvc" inputmode="numeric" :error="errors.cvc" required autocomplete="cc-csc" />
          </div>
        </div>

        <div class="card__foot">
          <BaseButton v-if="stepIndex > 0" variant="ghost" @click="back">Back</BaseButton>
          <BaseButton
            v-if="stepIndex < STEPS.length - 1"
            variant="primary"
            @click="next"
          >
            Continue
          </BaseButton>
          <BaseButton
            v-else
            variant="accent"
            size="lg"
            :loading="orders.submitting"
            @click="placeOrder"
          >
            {{ orders.submitting ? 'Placing order…' : 'Place order' }}
          </BaseButton>
        </div>
      </section>

      <OrderSummaryCard :show-action="false" />
    </div>
  </div>
</template>

<style scoped>
.page {
  padding-block: var(--space-8) var(--space-16);
}

.page__head {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  justify-content: space-between;
  gap: var(--space-3);
  margin-bottom: var(--space-6);
}

.page__title {
  font-size: clamp(1.6rem, 3.4vw, var(--text-2xl));
}

.page__secure {
  display: flex;
  align-items: center;
  gap: var(--space-2);
  font-size: var(--text-sm);
  color: var(--color-muted);
}

.steps {
  display: flex;
  gap: var(--space-5);
  margin-bottom: var(--space-8);
  flex-wrap: wrap;
}

.steps__item {
  display: flex;
  align-items: center;
  gap: var(--space-3);
}

.steps__marker {
  display: grid;
  place-items: center;
  width: 28px;
  height: 28px;
  border-radius: var(--radius-full);
  border: 1px solid var(--color-border-strong);
  background: var(--color-surface);
  font-size: var(--text-xs);
  font-weight: var(--weight-bold);
  color: var(--color-muted);
}

.steps__label {
  font-size: var(--text-base);
  color: var(--color-muted);
}

.steps__item.is-done .steps__marker {
  background: var(--color-success);
  border-color: var(--color-success);
  color: var(--color-inverse);
}

.steps__item.is-done .steps__label {
  color: var(--color-text-secondary);
}

.steps__item.is-current .steps__marker {
  border-color: var(--color-primary);
  color: var(--color-primary);
  box-shadow: 0 0 0 3px var(--color-primary-soft);
}

.steps__item.is-current .steps__label {
  color: var(--color-text);
  font-weight: var(--weight-semibold);
}

.checkout {
  display: grid;
  gap: var(--space-6);
  align-items: start;
}

.grid2 {
  display: grid;
  gap: var(--space-4);
}

.card__foot {
  display: flex;
  justify-content: flex-end;
  gap: var(--space-3);
  padding: var(--space-5) var(--space-6);
  border-top: 1px solid var(--color-border);
  background: var(--color-surface-alt);
  border-radius: 0 0 var(--radius-lg) var(--radius-lg);
}

.sim-note {
  grid-column: 1 / -1;
  padding: var(--space-4);
  border-radius: var(--radius-md);
  background: var(--color-info-soft);
  color: #17428f;
  font-size: var(--text-sm);
}

.sim-note p {
  color: inherit;
  margin-top: var(--space-1);
}

@media (min-width: 700px) {
  .grid2 {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (min-width: 900px) {
  .checkout {
    grid-template-columns: minmax(0, 1fr) 340px;
    gap: var(--space-8);
  }
}
</style>
