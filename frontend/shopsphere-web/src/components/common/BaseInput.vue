<script setup lang="ts">
withDefaults(
  defineProps<{
    modelValue: string | number
    type?: string
    label?: string
    placeholder?: string
    error?: string
    hint?: string
    required?: boolean
    disabled?: boolean
    autocomplete?: string
    name?: string
    inputmode?: 'text' | 'email' | 'tel' | 'numeric' | 'decimal'
  }>(),
  {
    type: 'text',
    label: '',
    placeholder: '',
    error: '',
    hint: '',
    required: false,
    disabled: false,
    autocomplete: 'off',
    name: undefined,
    inputmode: undefined,
  },
)

const emit = defineEmits<{
  'update:modelValue': [value: string]
  blur: []
}>()

// A native input keeps mobile keyboards, autofill and password managers working correctly.
function onInput(event: Event): void {
  emit('update:modelValue', (event.target as HTMLInputElement).value)
}
</script>

<template>
  <div class="field">
    <label v-if="label" class="field__label" :for="name ?? label">
      {{ label }}
      <span v-if="required" aria-hidden="true">*</span>
    </label>
    <input
      :id="name ?? label"
      :name="name"
      :type="type"
      :value="modelValue"
      :placeholder="placeholder"
      :required="required"
      :disabled="disabled"
      :autocomplete="autocomplete"
      :inputmode="inputmode"
      class="input"
      :aria-invalid="error ? 'true' : undefined"
      :aria-describedby="error ? `${name ?? label}-error` : hint ? `${name ?? label}-hint` : undefined"
      @input="onInput"
      @blur="emit('blur')"
    />
    <p v-if="hint && !error" :id="`${name ?? label}-hint`" class="field__hint">{{ hint }}</p>
    <p v-if="error" :id="`${name ?? label}-error`" class="field__error" role="alert">{{ error }}</p>
  </div>
</template>
