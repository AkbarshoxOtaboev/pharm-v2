<script setup lang="ts">
defineProps<{
  modelValue?: string
  label?: string
  rows?: number
  placeholder?: string
}>()
defineEmits<{ 'update:modelValue': [value: string] }>()
</script>

<template>
  <label class="block space-y-1.5">
    <span v-if="label" class="text-theme-sm font-medium text-gray-700">{{ label }}</span>
    <textarea
      :value="modelValue"
      :rows="rows || 3"
      :placeholder="placeholder"
      class="w-full rounded-lg border border-gray-300 bg-transparent px-4 py-2.5 text-theme-sm text-gray-800 shadow-theme-xs placeholder:text-gray-400 focus:border-brand-300 focus:outline-hidden focus:ring-3 focus:ring-brand-500/10"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
  </label>
</template>
