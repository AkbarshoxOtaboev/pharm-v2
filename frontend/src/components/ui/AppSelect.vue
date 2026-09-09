<script setup lang="ts">
defineProps<{
  modelValue?: string | number
  label?: string
  disabled?: boolean
}>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <label class="block space-y-1.5">
    <span v-if="label" class="text-theme-sm font-medium text-gray-700">{{ label }}</span>
    <select
      :value="modelValue"
      :disabled="disabled"
      class="h-11 w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-theme-sm text-gray-800 shadow-theme-xs focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10 disabled:bg-gray-50"
      @change="$emit('update:modelValue', ($event.target as HTMLSelectElement).value)"
    >
      <slot />
    </select>
  </label>
</template>
