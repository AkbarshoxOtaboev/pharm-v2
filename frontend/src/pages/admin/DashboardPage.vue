<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchDailyStats, fetchDashboardStats, fetchMonthlySales } from '@/api/orders.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppModal from '@/components/ui/AppModal.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import DataTable from '@/components/ui/DataTable.vue'
import DailyLineChart from '@/components/charts/DailyLineChart.vue'
import MonthlySalesChart from '@/components/charts/MonthlySalesChart.vue'
import type { DailyStatDTO, OrderStatResponse } from '@/types/api'
import { apiError, dateTime, money } from '@/utils/format'

const STAT_KEYS = ['clients', 'orders', 'deliveredSum', 'cancelledSum', 'dailySum', 'monthlySum'] as const
const CHART_KEYS = ['monthlySales', 'dailyOrdersChart', 'dailySumsChart'] as const
const WIDGET_KEYS = [...STAT_KEYS, ...CHART_KEYS] as const
type StatKey = (typeof STAT_KEYS)[number]
type ChartKey = (typeof CHART_KEYS)[number]
type WidgetKey = (typeof WIDGET_KEYS)[number]
const STORAGE_KEY = 'pharm-dashboard-cards'

type CardTone = 'brand' | 'purple' | 'success' | 'error' | 'warning' | 'cyan'
type CardIcon = 'users' | 'orders' | 'delivered' | 'cancelled' | 'daily' | 'monthly'

const STAT_META: Record<StatKey, { tone: CardTone; icon: CardIcon; minClass: string; labelKey: string }> = {
  clients: { tone: 'brand', icon: 'users', minClass: 'min-w-[160px]', labelKey: 'dashboard.clients' },
  orders: { tone: 'purple', icon: 'orders', minClass: 'min-w-[160px]', labelKey: 'dashboard.orders' },
  deliveredSum: { tone: 'success', icon: 'delivered', minClass: 'min-w-[180px]', labelKey: 'dashboard.deliveredSum' },
  cancelledSum: { tone: 'error', icon: 'cancelled', minClass: 'min-w-[180px]', labelKey: 'dashboard.cancelledSum' },
  dailySum: { tone: 'warning', icon: 'daily', minClass: 'min-w-[180px]', labelKey: 'dashboard.dailySum' },
  monthlySum: { tone: 'cyan', icon: 'monthly', minClass: 'min-w-[180px]', labelKey: 'dashboard.monthlySum' },
}

const CHART_META: Record<ChartKey, { labelKey: string }> = {
  monthlySales: { labelKey: 'dashboard.monthlySales' },
  dailyOrdersChart: { labelKey: 'dashboard.dailyOrdersChart' },
  dailySumsChart: { labelKey: 'dashboard.dailySumsChart' },
}

function defaultVisible(): Record<WidgetKey, boolean> {
  return Object.fromEntries(WIDGET_KEYS.map((k) => [k, true])) as Record<WidgetKey, boolean>
}

function loadVisible(): Record<WidgetKey, boolean> {
  const base = defaultVisible()
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return base
    const parsed = JSON.parse(raw) as Partial<Record<WidgetKey, boolean>>
    for (const key of WIDGET_KEYS) {
      if (typeof parsed[key] === 'boolean') base[key] = parsed[key]!
    }
  } catch {
    /* ignore */
  }
  return base
}

const { t } = useI18n()
const stats = ref<OrderStatResponse | null>(null)
const daily = ref<DailyStatDTO[]>([])
const monthlySales = ref<number[]>(Array(12).fill(0))
const year = new Date().getFullYear()
const loading = ref(true)
const error = ref('')
const settingsOpen = ref(false)
const visibleWidgets = ref<Record<WidgetKey, boolean>>(loadVisible())

watch(
  visibleWidgets,
  (v) => {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(v))
    } catch {
      /* ignore */
    }
  },
  { deep: true },
)

function cardValue(key: StatKey): string | number {
  const s = stats.value
  if (key === 'clients') return s?.clientsCount ?? 0
  if (key === 'orders') return s?.ordersCount ?? 0
  if (key === 'deliveredSum') return money(s?.deliveredOrdersTotalSum)
  if (key === 'cancelledSum') return money(s?.cancelledOrdersTotalSum)
  if (key === 'dailySum') return money(s?.dailyDeliveredOrdersTotalSum)
  return money(s?.monthlyDeliveredOrdersTotalSum)
}

const cardDefs = computed(() =>
  STAT_KEYS.map((key) => ({
    key,
    label: t(STAT_META[key].labelKey),
    value: cardValue(key),
    tone: STAT_META[key].tone,
    icon: STAT_META[key].icon,
    minClass: STAT_META[key].minClass,
  })),
)

const chartDefs = computed(() =>
  CHART_KEYS.map((key) => ({
    key,
    label: t(CHART_META[key].labelKey),
  })),
)

const shownCards = computed(() => cardDefs.value.filter((c) => isOn(c.key)))

function isOn(key: WidgetKey) {
  return visibleWidgets.value[key] !== false
}

function setOn(key: WidgetKey, on: boolean) {
  visibleWidgets.value = { ...visibleWidgets.value, [key]: on }
}

function showAll() {
  visibleWidgets.value = defaultVisible()
}

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
    <PageHeader :title="t('dashboard.title')" :subtitle="t('dashboard.subtitle')">
      <template #actions>
        <AppButton variant="secondary" @click="settingsOpen = true">
          <AppIcon name="settings" class="size-4.5" />
          {{ t('dashboard.cardSettings') }}
        </AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>

    <template v-else>
      <div
        v-if="shownCards.length"
        class="mb-6 flex flex-nowrap gap-3 overflow-x-auto pb-1"
      >
        <StatCard
          v-for="card in shownCards"
          :key="card.key"
          :class="card.minClass"
          :label="card.label"
          :value="card.value"
          :tone="card.tone"
          :icon="card.icon"
        />
      </div>
      <div
        v-else
        class="mb-6 rounded-2xl border border-dashed border-gray-200 px-5 py-6 text-center text-theme-sm text-gray-500 dark:border-gray-800 dark:text-gray-400"
      >
        {{ t('dashboard.noCards') }}
        <button
          type="button"
          class="ml-1 font-medium text-brand-500 hover:underline"
          @click="settingsOpen = true"
        >
          {{ t('dashboard.cardSettings') }}
        </button>
      </div>

      <div v-if="isOn('monthlySales')" class="mb-6">
        <MonthlySalesChart :series-data="monthlySales" :year="year" />
      </div>

      <div class="mb-6 space-y-6">
        <DailyLineChart
          v-if="isOn('dailyOrdersChart')"
          :title="t('dashboard.dailyOrdersChart')"
          :hint="monthHint"
          :categories="dayLabels"
          :series-defs="ordersSeries"
        />
        <DailyLineChart
          v-if="isOn('dailySumsChart')"
          :title="t('dashboard.dailySumsChart')"
          :hint="monthHint"
          :categories="dayLabels"
          :series-defs="sumsSeries"
        />
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

    <AppModal :open="settingsOpen" :title="t('dashboard.cardSettings')" @close="settingsOpen = false">
      <p class="mb-4 text-theme-sm text-gray-500 dark:text-gray-400">{{ t('dashboard.cardSettingsHint') }}</p>

      <h3 class="mb-2 text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ t('dashboard.settingsCards') }}</h3>
      <div class="mb-5 space-y-2">
        <label
          v-for="card in cardDefs"
          :key="card.key"
          class="flex cursor-pointer items-center gap-3 rounded-xl border border-gray-200 px-4 py-3 text-theme-sm text-gray-800 transition hover:bg-gray-50 dark:border-gray-800 dark:text-white/90 dark:hover:bg-white/5"
        >
          <input
            type="checkbox"
            class="size-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500/30 dark:border-gray-600 dark:bg-gray-800"
            :checked="isOn(card.key)"
            @change="setOn(card.key, ($event.target as HTMLInputElement).checked)"
          />
          <span>{{ card.label }}</span>
        </label>
      </div>

      <h3 class="mb-2 text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ t('dashboard.settingsCharts') }}</h3>
      <div class="space-y-2">
        <label
          v-for="chart in chartDefs"
          :key="chart.key"
          class="flex cursor-pointer items-center gap-3 rounded-xl border border-gray-200 px-4 py-3 text-theme-sm text-gray-800 transition hover:bg-gray-50 dark:border-gray-800 dark:text-white/90 dark:hover:bg-white/5"
        >
          <input
            type="checkbox"
            class="size-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500/30 dark:border-gray-600 dark:bg-gray-800"
            :checked="isOn(chart.key)"
            @change="setOn(chart.key, ($event.target as HTMLInputElement).checked)"
          />
          <span>{{ chart.label }}</span>
        </label>
      </div>

      <div class="mt-5 flex flex-wrap justify-end gap-2">
        <AppButton variant="secondary" @click="showAll">{{ t('dashboard.showAllCards') }}</AppButton>
        <AppButton @click="settingsOpen = false">{{ t('common.save') }}</AppButton>
      </div>
    </AppModal>
  </div>
</template>
