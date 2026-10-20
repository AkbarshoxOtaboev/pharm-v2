<script setup lang="ts">
import { useI18n } from 'vue-i18n'
import AppButton from './AppButton.vue'
import AppIcon from './AppIcon.vue'

defineProps<{
  page: number
  totalPages: number
  totalElements: number
  loading?: boolean
}>()
defineEmits<{ 'update:page': [page: number] }>()

const { t } = useI18n()
</script>

<template>
  <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
    <p class="text-theme-sm text-gray-500 dark:text-gray-400">
      {{ t('common.total') }}: <span class="font-medium text-gray-800 dark:text-white/90">{{ totalElements }}</span>
    </p>
    <div v-if="totalPages > 1" class="flex items-center gap-2">
      <AppButton
        size="sm"
        variant="secondary"
        :disabled="loading || page <= 0"
        :aria-label="t('common.previous')"
        @click="$emit('update:page', page - 1)"
      >
        <AppIcon name="chevron-left" class="size-4" />
        {{ t('common.previous') }}
      </AppButton>
      <span class="text-theme-sm text-gray-600 dark:text-gray-400">{{ page + 1 }} / {{ totalPages }}</span>
      <AppButton
        size="sm"
        variant="secondary"
        :disabled="loading || page + 1 >= totalPages"
        :aria-label="t('common.next')"
        @click="$emit('update:page', page + 1)"
      >
        {{ t('common.next') }}
        <AppIcon name="chevron-right" class="size-4" />
      </AppButton>
    </div>
  </div>
</template>
