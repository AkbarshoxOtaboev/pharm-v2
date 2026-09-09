<script setup lang="ts">
defineProps<{
  tone?: 'brand' | 'success' | 'warning' | 'error' | 'gray'
}>()
</script>

<template>
  <span
    class="inline-flex rounded-full px-2.5 py-0.5 text-theme-xs font-medium"
    :class="{
      'bg-brand-50 text-brand-600': !tone || tone === 'brand',
      'bg-success-50 text-success-700': tone === 'success',
      'bg-warning-50 text-warning-600': tone === 'warning',
      'bg-error-50 text-error-600': tone === 'error',
      'bg-gray-100 text-gray-700': tone === 'gray',
    }"
  >
    <slot />
  </span>
</template>
