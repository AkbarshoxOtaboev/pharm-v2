<script setup lang="ts">
import { useI18n } from 'vue-i18n'

defineProps<{ loading?: boolean; empty?: string }>()
const { t } = useI18n()
</script>

<template>
  <div
    class="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]"
  >
    <div class="overflow-x-auto">
      <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-800">
        <thead class="bg-gray-50 dark:bg-white/[0.02]">
          <tr>
            <slot name="head" />
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 bg-white dark:divide-gray-800 dark:bg-transparent">
          <tr v-if="loading">
            <td colspan="20" class="px-5 py-8 text-center text-theme-sm text-gray-500 dark:text-gray-400">
              {{ t('common.loading') }}
            </td>
          </tr>
          <tr v-else-if="empty">
            <td colspan="20" class="px-5 py-8 text-center text-theme-sm text-gray-500 dark:text-gray-400">
              {{ empty }}
            </td>
          </tr>
          <slot v-else />
        </tbody>
      </table>
    </div>
  </div>
</template>
