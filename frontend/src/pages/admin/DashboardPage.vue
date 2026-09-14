<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchDailyStats, fetchDashboardStats, fetchMonthlySales } from '@/api/orders.api'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import DataTable from '@/components/ui/DataTable.vue'
import MonthlySalesChart from '@/components/charts/MonthlySalesChart.vue'
import type { DailyStatDTO, OrderStatResponse } from '@/types/api'
import { apiError, money } from '@/utils/format'

const { t } = useI18n()
const stats = ref<OrderStatResponse | null>(null)
const daily = ref<DailyStatDTO[]>([])
const monthlySales = ref<number[]>(Array(12).fill(0))
const year = new Date().getFullYear()
const loading = ref(true)
const error = ref('')

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const [dashRes, dailyRes, monthlyRes] = await Promise.all([
      fetchDashboardStats(),
      fetchDailyStats(),
      fetchMonthlySales(year),
    ])
    stats.value = dashRes.data
    daily.value = dailyRes.data || []
    monthlySales.value = (monthlyRes.data || []).map((v) => Number(v) || 0)
    while (monthlySales.value.length < 12) monthlySales.value.push(0)
  } catch (e) {
    error.value = apiError(e, 'errors.loadStats')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader :title="t('dashboard.title')" :subtitle="t('dashboard.subtitle')" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>

    <template v-else>
      <div class="mb-6 flex flex-nowrap gap-3 overflow-x-auto pb-1">
        <StatCard
          class="min-w-[160px]"
          :label="t('dashboard.clients')"
          :value="stats?.clientsCount ?? 0"
          tone="brand"
          icon="users"
        />
        <StatCard
          class="min-w-[160px]"
          :label="t('dashboard.orders')"
          :value="stats?.ordersCount ?? 0"
          tone="purple"
          icon="orders"
        />
        <StatCard
          class="min-w-[180px]"
          :label="t('dashboard.deliveredSum')"
          :value="money(stats?.deliveredOrdersTotalSum)"
          tone="success"
          icon="delivered"
        />
        <StatCard
          class="min-w-[180px]"
          :label="t('dashboard.cancelledSum')"
          :value="money(stats?.cancelledOrdersTotalSum)"
          tone="error"
          icon="cancelled"
        />
        <StatCard
          class="min-w-[180px]"
          :label="t('dashboard.dailySum')"
          :value="money(stats?.dailyDeliveredOrdersTotalSum)"
          tone="warning"
          icon="daily"
        />
        <StatCard
          class="min-w-[180px]"
          :label="t('dashboard.monthlySum')"
          :value="money(stats?.monthlyDeliveredOrdersTotalSum)"
          tone="cyan"
          icon="monthly"
        />
      </div>

      <div class="mb-6">
        <MonthlySalesChart :series-data="monthlySales" :year="year" />
      </div>

      <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('dashboard.dailyStats') }}</h2>
      <DataTable :loading="false" :empty="daily.length ? undefined : t('common.noData')">
        <template #head>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('dashboard.date') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('dashboard.orders') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('dashboard.delivered') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.cancel') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('dashboard.deliveredSum') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('dashboard.cancelledSum') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('dashboard.bonus') }}</th>
        </template>
        <tr v-for="(row, idx) in daily" :key="row.date || idx">
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.date || t('common.empty') }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.ordersCount ?? 0 }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.deliveredCount ?? 0 }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.cancelledCount ?? 0 }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.deliveredSum) }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.cancelledSum) }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.deliveredBonusSum) }}</td>
        </tr>
      </DataTable>
    </template>
  </div>
</template>
