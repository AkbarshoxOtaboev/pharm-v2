<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'

const props = defineProps<{ title?: string; titleKey?: string }>()
const { t } = useI18n()
const heading = computed(() => (props.titleKey ? t(props.titleKey) : props.title || ''))
</script>

<template>
  <div class="rounded-2xl border border-gray-200 bg-white p-8 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
    <h1 class="text-2xl font-semibold text-gray-800 dark:text-white/90">{{ heading }}</h1>
    <p class="mt-2 text-theme-sm text-gray-500 dark:text-gray-400">
      {{ t('placeholder.comingSoon') }}
    </p>
  </div>
</template>
