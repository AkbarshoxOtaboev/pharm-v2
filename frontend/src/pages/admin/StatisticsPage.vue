<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchCourierMonthStats, fetchDailyStats } from '@/api/orders.api'
import DailyLineChart from '@/components/charts/DailyLineChart.vue'
import CourierRankChart from '@/components/charts/CourierRankChart.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { CourierMonthStat, DailyStatDTO } from '@/types/api'
import { apiError, dateTime } from '@/utils/format'

const { t } = useI18n()
const daily = ref<DailyStatDTO[]>([])
const couriers = ref<CourierMonthStat[]>([])
const loading = ref(true)
const error = ref('')

const dayLabels = computed(() =>
  daily.value.map((row) => {
    const formatted = dateTime(row.date)
    return formatted.length >= 5 ? formatted.slice(0, 5) : formatted || String(row.date ?? '')
  }),
)

const monthHint = computed(() => {
  const now = new Date()
  return t('dashboard.currentMonthHint', { month: t(`months.${now.getMonth()}`), year: now.getFullYear() })
})

const courierHint = computed(() => {
  const now = new Date()
  return t('dashboard.courierMonthHint', { month: t(`months.${now.getMonth()}`), year: now.getFullYear() })
})

const ordersSeries = computed(() => [
  {
    key: 'orders',
    name: t('dashboard.orders'),
    color: '#465fff',
    data: daily.value.map((r) => Number(r.ordersCount ?? 0)),
  },
  {
    key: 'delivered',
    name: t('dashboard.delivered'),
    color: '#12b76a',
    data: daily.value.map((r) => Number(r.deliveredCount ?? 0)),
  },
  {
    key: 'cancelled',
    name: t('dashboard.cancelled'),
    color: '#f04438',
    data: daily.value.map((r) => Number(r.cancelledCount ?? 0)),
  },
])

const sumsSeries = computed(() => [
  {
    key: 'deliveredSum',
    name: t('dashboard.deliveredSum'),
    color: '#12b76a',
    data: daily.value.map((r) => Number(r.deliveredSum ?? 0)),
  },
  {
    key: 'cancelledSum',
    name: t('dashboard.cancelledSum'),
    color: '#f04438',
    data: daily.value.map((r) => Number(r.cancelledSum ?? 0)),
  },
  {
    key: 'bonus',
    name: t('dashboard.bonus'),
    color: '#f79009',
    data: daily.value.map((r) => Number(r.deliveredBonusSum ?? 0)),
  },
])

function ranked(pick: (row: CourierMonthStat) => number) {
  return [...couriers.value]
    .map((row) => ({
      name: row.courierName?.trim() || '—',
      value: Number(pick(row) ?? 0),
    }))
    .sort((a, b) => b.value - a.value || a.name.localeCompare(b.name))
}

const salesRank = computed(() => ranked((r) => Number(r.deliveredTotalSum ?? 0)))
const ordersRank = computed(() => ranked((r) => Number(r.allOrders ?? 0)))
const deliveredRank = computed(() => ranked((r) => Number(r.deliveredOrders ?? 0)))
const cancelledRank = computed(() => ranked((r) => Number(r.cancelledOrders ?? 0)))
const bonusRank = computed(() => ranked((r) => Number(r.bonusQuantity ?? 0)))

onMounted(async () => {
  loading.value = true
  error.value = ''
  try {
    const [dailyRes, courierRes] = await Promise.all([fetchDailyStats(), fetchCourierMonthStats()])
    daily.value = dailyRes.data || []
    couriers.value = courierRes.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadStats')
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <PageHeader :title="t('dashboard.statisticsTitle')" :subtitle="t('dashboard.statisticsSubtitle')" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>

    <div v-else class="space-y-6">
      <DailyLineChart
        :title="t('dashboard.dailyOrdersChart')"
        :hint="monthHint"
        :categories="dayLabels"
        :series-defs="ordersSeries"
      />
      <DailyLineChart
        :title="t('dashboard.dailySumsChart')"
        :hint="monthHint"
        :categories="dayLabels"
        :series-defs="sumsSeries"
      />

      <CourierRankChart
        :title="t('dashboard.courierSales')"
        :hint="courierHint"
        :categories="salesRank.map((r) => r.name)"
        :values="salesRank.map((r) => r.value)"
        color="#465fff"
        format="money"
      />
      <CourierRankChart
        :title="t('dashboard.courierOrders')"
        :hint="courierHint"
        :categories="ordersRank.map((r) => r.name)"
        :values="ordersRank.map((r) => r.value)"
        color="#465fff"
        format="count"
      />
      <CourierRankChart
        :title="t('dashboard.courierDelivered')"
        :hint="courierHint"
        :categories="deliveredRank.map((r) => r.name)"
        :values="deliveredRank.map((r) => r.value)"
        color="#12b76a"
        format="count"
      />
      <CourierRankChart
        :title="t('dashboard.courierCancelled')"
        :hint="courierHint"
        :categories="cancelledRank.map((r) => r.name)"
        :values="cancelledRank.map((r) => r.value)"
        color="#f04438"
        format="count"
      />
      <CourierRankChart
        :title="t('dashboard.courierBonus')"
        :hint="courierHint"
        :categories="bonusRank.map((r) => r.name)"
        :values="bonusRank.map((r) => r.value)"
        color="#f79009"
        format="count"
      />
    </div>
  </div>
</template>
