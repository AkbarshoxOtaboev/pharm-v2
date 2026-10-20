<script setup lang="ts">
import AppIcon from './AppIcon.vue'

defineProps<{
  modelValue?: string | number
  label?: string
  disabled?: boolean
  icon?: string
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
      <select
        :value="modelValue"
        :disabled="disabled"
        class="h-11 w-full rounded-lg border border-gray-300 bg-transparent py-2.5 pr-4 text-theme-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 disabled:bg-gray-50 dark:border-gray-700 dark:text-white/90 dark:disabled:bg-white/5"
        :class="icon ? 'pl-10' : 'pl-4'"
        @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
      >
        <slot />
      </select>
    </span>
  </label>
</template>
