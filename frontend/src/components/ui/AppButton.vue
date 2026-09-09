<script setup lang="ts">
defineProps<{
  variant?: 'primary' | 'secondary' | 'danger' | 'ghost' | 'success'
  type?: 'button' | 'submit' | 'reset'
  loading?: boolean
  disabled?: boolean
  size?: 'sm' | 'md'
}>()
</script>

<template>
  <button
    :type="type || 'button'"
    :disabled="disabled || loading"
    class="inline-flex items-center justify-center gap-2 rounded-lg font-medium transition disabled:cursor-not-allowed disabled:opacity-60"
    :class="[
      size === 'sm' ? 'px-3 py-2 text-theme-sm' : 'px-4 py-2.5 text-theme-sm',
      variant === 'secondary' &&
        'border border-gray-200 bg-white text-gray-700 hover:bg-gray-50',
      variant === 'danger' && 'bg-error-500 text-white hover:bg-error-600',
      variant === 'ghost' && 'bg-transparent text-gray-600 hover:bg-gray-100',
      variant === 'success' && 'bg-success-500 text-white hover:bg-success-600',
      (!variant || variant === 'primary') &&
        'bg-brand-500 text-white shadow-theme-xs hover:bg-brand-600',
    ]"
  >
    <span
      v-if="loading"
      class="size-4 animate-spin rounded-full border-2 border-white/40 border-t-white"
    />
    <slot />
  </button>
</template>
