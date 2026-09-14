<script setup lang="ts">
defineProps<{
  tone?: 'brand' | 'success' | 'warning' | 'error' | 'gray'
}>()
</script>

<template>
  <span
    class="inline-flex rounded-full px-2.5 py-0.5 text-theme-xs font-medium"
    :class="{
      'bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400': !tone || tone === 'brand',
      'bg-success-50 text-success-700 dark:bg-success-500/15 dark:text-success-500': tone === 'success',
      'bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-warning-500': tone === 'warning',
      'bg-error-50 text-error-600 dark:bg-error-500/15 dark:text-error-500': tone === 'error',
      'bg-gray-100 text-gray-700 dark:bg-white/5 dark:text-gray-300': tone === 'gray',
    }"
  >
    <slot />
  </span>
</template>
