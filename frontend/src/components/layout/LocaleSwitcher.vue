<script setup lang="ts">
import { LOCALE_OPTIONS, type AppLocale } from '@/i18n'
import { useLocale } from '@/composables/useLocale'
import { useI18n } from 'vue-i18n'

const { locale, setLocale } = useLocale()
const { t } = useI18n()

function onSelect(value: AppLocale) {
  setLocale(value)
}
</script>

<template>
  <div
    class="inline-flex overflow-hidden rounded-full border border-gray-200 dark:border-gray-800"
    role="group"
    :aria-label="t('lang.switcher')"
  >
    <button
      v-for="opt in LOCALE_OPTIONS"
      :key="opt.value"
      type="button"
      class="px-2.5 py-1.5 text-theme-xs font-semibold transition"
      :class="
        locale === opt.value
          ? 'bg-brand-500 text-white'
          : 'bg-white text-gray-600 hover:bg-gray-50 dark:bg-transparent dark:text-gray-400 dark:hover:bg-white/5'
      "
      :title="t(opt.labelKey)"
      :aria-pressed="locale === opt.value"
      @click="onSelect(opt.value)"
    >
      {{ opt.short }}
    </button>
  </div>
</template>
