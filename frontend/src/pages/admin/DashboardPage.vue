<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchDailyStats, fetchDashboardStats, fetchMonthlySales } from '@/api/orders.api'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import DataTable from '@/components/ui/DataTable.vue'
import MonthlySalesChart from '@/components/charts/MonthlySalesChart.vue'
import type { DailyStatDTO, OrderStatResponse } from '@/types/api'
import { apiError, money } from '@/utils/format'

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
    error.value = apiError(e, 'Statistikani yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader title="Dashboard" subtitle="Buyurtmalar va sotuvlar umumiy ko‘rinishi" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="text-theme-sm text-gray-500">Yuklanmoqda...</div>

    <template v-else>
      <div class="mb-6 flex flex-nowrap gap-3 overflow-x-auto pb-1">
        <StatCard
          class="min-w-[160px]"
          label="Mijozlar"
          :value="stats?.clientsCount ?? 0"
          tone="brand"
          icon="users"
        />
        <StatCard
          class="min-w-[160px]"
          label="Buyurtmalar"
          :value="stats?.ordersCount ?? 0"
          tone="purple"
          icon="orders"
        />
        <StatCard
          class="min-w-[160px]"
          label="Yetkazilgan"
          :value="stats?.deliveredOrdersCount ?? 0"
          tone="success"
          icon="delivered"
        />
        <StatCard
          class="min-w-[160px]"
          label="Bekor qilingan"
          :value="stats?.cancelledOrdersCount ?? 0"
          tone="error"
          icon="cancelled"
        />
        <StatCard
          class="min-w-[180px]"
          label="Kunlik summa"
          :value="money(stats?.dailyDeliveredOrdersTotalSum)"
          tone="warning"
          icon="daily"
        />
        <StatCard
          class="min-w-[180px]"
          label="Oylik summa"
          :value="money(stats?.monthlyDeliveredOrdersTotalSum)"
          tone="cyan"
          icon="monthly"
        />
      </div>

      <div class="mb-6">
        <MonthlySalesChart :series-data="monthlySales" :year="year" />
      </div>

      <h2 class="mb-3 text-lg font-semibold text-gray-800">Kunlik statistika</h2>
      <DataTable :loading="false" :empty="daily.length ? undefined : 'Maʼlumot yo‘q'">
        <template #head>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Sana</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Buyurtmalar</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Yetkazilgan</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Bekor</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Yetkazilgan summa</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Bekor summa</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Bonus</th>
        </template>
        <tr v-for="(row, idx) in daily" :key="row.date || idx">
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.date || '—' }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.ordersCount ?? 0 }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.deliveredCount ?? 0 }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.cancelledCount ?? 0 }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(row.deliveredSum) }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(row.cancelledSum) }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(row.deliveredBonusSum) }}</td>
        </tr>
      </DataTable>
    </template>
  </div>
</template>
