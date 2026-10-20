<script setup lang="ts">
import AppIcon from './AppIcon.vue'

defineProps<{
  modelValue?: string | number
  label?: string
  type?: string
  placeholder?: string
  error?: string
  disabled?: boolean
  autocomplete?: string
  icon?: string
  suffix?: string
}>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <label class="block space-y-1.5">
    <span v-if="label" class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">{{ label }}</span>
    <span class="relative block">
      <AppIcon
        v-if="icon"
        :name="icon"
        class="pointer-events-none absolute left-3.5 top-1/2 size-[18px]! -translate-y-1/2 text-gray-400"
      />
      <input
        :type="type || 'text'"
        :value="modelValue"
        :placeholder="placeholder"
        :disabled="disabled"
        :autocomplete="autocomplete"
        class="h-11 w-full rounded-lg border bg-transparent py-2.5 text-theme-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 disabled:bg-gray-50 dark:text-white/90 dark:placeholder:text-gray-500 dark:disabled:bg-white/5"
        :class="[
          error ? 'border-error-500' : 'border-gray-300 dark:border-gray-700',
          icon ? 'pl-10' : 'pl-4',
          suffix ? 'pr-14' : 'pr-4',
        ]"
        @input="$emit('update:modelValue', ($event.target as HTMLInputElement).value)"
      />
      <span
        v-if="suffix"
        class="pointer-events-none absolute right-3.5 top-1/2 -translate-y-1/2 text-theme-xs font-medium text-gray-400"
      >
        {{ suffix }}
      </span>
    </span>
    <span v-if="error" class="text-theme-xs text-error-500">{{ error }}</span>
  </label>
</template>
