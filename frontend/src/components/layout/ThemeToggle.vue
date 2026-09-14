<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useTheme } from '@/composables/useTheme'

const { theme, toggleTheme } = useTheme()
const { t } = useI18n()
const isDark = computed(() => theme.value === 'dark')
const label = computed(() => (isDark.value ? t('theme.light') : t('theme.dark')))
</script>

<template>
  <button
    type="button"
    role="switch"
    class="relative inline-flex h-10 w-[72px] shrink-0 items-center rounded-full border transition-colors"
    :class="
      isDark
        ? 'border-gray-700 bg-gray-800'
        : 'border-gray-200 bg-gray-100'
    "
    :aria-checked="isDark"
    :title="label"
    :aria-label="label"
    @click="toggleTheme"
  >
    <!-- track icons -->
    <span class="pointer-events-none absolute inset-0 flex items-center justify-between px-2.5">
      <svg
        class="size-3.5 transition-opacity"
        :class="isDark ? 'text-gray-500 opacity-40' : 'text-amber-500 opacity-100'"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <circle cx="12" cy="12" r="4" />
        <path d="M12 2v2M12 20v2M4.93 4.93l1.41 1.41M17.66 17.66l1.41 1.41M2 12h2M20 12h2M4.93 19.07l1.41-1.41M17.66 6.34l1.41-1.41" />
      </svg>
      <svg
        class="size-3.5 transition-opacity"
        :class="isDark ? 'text-brand-300 opacity-100' : 'text-gray-400 opacity-40'"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        stroke-width="2"
        stroke-linecap="round"
        stroke-linejoin="round"
        aria-hidden="true"
      >
        <path d="M21 14.5A8.5 8.5 0 0 1 9.5 3 7 7 0 1 0 21 14.5Z" />
      </svg>
    </span>

    <!-- thumb -->
    <span
      class="relative z-10 ml-1 size-8 rounded-full bg-white shadow-md transition-transform duration-200 dark:bg-gray-200"
      :class="isDark ? 'translate-x-8' : 'translate-x-0'"
    />
  </button>
</template>
