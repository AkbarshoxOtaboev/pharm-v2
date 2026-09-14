<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchSaleLogs } from '@/api/saleLogs.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { SaleLogResponse } from '@/types/api'
import { apiError, money } from '@/utils/format'

const { t } = useI18n()
const logs = ref<SaleLogResponse[]>([])
const loading = ref(false)
const error = ref('')
const from = ref('')
const to = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchSaleLogs({
      from: from.value || undefined,
      to: to.value || undefined,
    })
    logs.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadSaleLogs')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="t('saleLogs.title')" :subtitle="t('saleLogs.subtitle')" />

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-4 shadow-theme-xs">
      <div class="flex flex-wrap items-end gap-2">
        <AppInput v-model="from" :label="t('common.from')" type="date" />
        <AppInput v-model="to" :label="t('common.to')" type="date" />
        <AppButton @click="load">{{ t('common.filter') }}</AppButton>
        <AppButton
          variant="secondary"
          @click="
            from = '';
            to = '';
            load()
          "
        >
          {{ t('common.clear') }}
        </AppButton>
      </div>
    </div>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <DataTable
      :loading="loading"
      :empty="!loading && !logs.length ? t('saleLogs.notFound') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.order') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.customer') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.phone') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.bonus') }}</th>
      </template>
      <tr v-for="(row, idx) in logs" :key="row.orderId + '-' + idx">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.createdAt || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.orderId || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.customerName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.customerPhone || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.productName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.quantity ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.totalSum) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.isBonus ? t('common.yes') : t('common.no') }}</td>
      </tr>
    </DataTable>
  </div>
</template>
