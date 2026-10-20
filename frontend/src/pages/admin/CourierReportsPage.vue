<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { fetchCashSummary } from '@/api/cashRegister.api'
import { fetchCourierStats, fetchOrders } from '@/api/orders.api'
import { fetchCouriers } from '@/api/users.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type { CashSummaryResponse, CourierStatsResponse, OrderResponse, UserResponse } from '@/types/api'
import { apiError, dateTime, isoDate, money, statusTone } from '@/utils/format'

const REFRESH_MS = 30_000

const { t } = useI18n()

const monthStart = () => {
  const now = new Date()
  return isoDate(new Date(now.getFullYear(), now.getMonth(), 1))
}
const couriers = ref<UserResponse[]>([])
const courierId = ref('')
const fromDate = ref(monthStart())
const toDate = ref(isoDate(new Date()))
const stats = ref<CourierStatsResponse | null>(null)
const cash = ref<CashSummaryResponse | null>(null)
const orders = ref<OrderResponse[]>([])
const loading = ref(false)
const error = ref('')
const updatedAt = ref<Date | null>(null)

const isCurrentMonth = computed(() => fromDate.value === monthStart() && toDate.value === isoDate(new Date()))
const isLive = computed(() => !toDate.value || toDate.value >= isoDate(new Date()))
const updatedLabel = computed(() =>
  updatedAt.value
    ? updatedAt.value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '',
)
const courierCash = computed(() => cash.value?.couriers?.[0])

async function loadCouriers() {
  const res = await fetchCouriers()
  couriers.value = res.data || []
  if (!courierId.value && couriers.value.length) {
    courierId.value = String(couriers.value[0].id)
  }
}

let loadSeq = 0
async function loadReport(silent = false) {
  if (!courierId.value) {
    stats.value = null
    cash.value = null
    orders.value = []
    return
  }
  if (fromDate.value && toDate.value && fromDate.value > toDate.value) {
    error.value = t('errors.dateRangeInvalid')
    return
  }
  const seq = ++loadSeq
  if (!silent) loading.value = true
  error.value = ''
  try {
    const id = Number(courierId.value)
    const from = fromDate.value || undefined
    const to = toDate.value || undefined
    const [statsRes, ordersRes, cashRes] = await Promise.all([
      fetchCourierStats(id, { from, to }),
      fetchOrders({ userId: id, fromDate: from, toDate: to, size: -1 }),
      fetchCashSummary({ courierId: id, from, to }),
    ])
    if (seq !== loadSeq) return
    stats.value = statsRes.data
    orders.value = ordersRes.data || []
    cash.value = cashRes.data
    updatedAt.value = new Date()
  } catch (e) {
    if (seq === loadSeq) error.value = apiError(e, 'errors.loadCourierReport')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

let followCurrentMonth = true
watch([courierId, fromDate, toDate], () => {
  followCurrentMonth = isCurrentMonth.value
  void loadReport()
})

function resetToCurrentMonth() {
  fromDate.value = monthStart()
  toDate.value = isoDate(new Date())
}

let timer: ReturnType<typeof setInterval> | undefined
function tick() {
  if (document.hidden) return
  if (followCurrentMonth && !isCurrentMonth.value) {
    resetToCurrentMonth()
    return
  }
  if (isLive.value) void loadReport(true)
}

onMounted(async () => {
  timer = setInterval(tick, REFRESH_MS)
  try {
    await loadCouriers()
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  }
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div>
    <PageHeader :title="t('courierReports.title')" :subtitle="t('courierReports.subtitle')">
      <template #actions>
        <RouterLink
          to="/admin/cash-register"
          class="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2.5 text-theme-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-800 dark:bg-white/5 dark:text-gray-300 dark:hover:bg-white/10"
        >
          <AppIcon name="cash" class="size-4.5" />
          {{ t('nav.cash') }}
        </RouterLink>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div class="mb-6 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-4 shadow-theme-xs">
      <div class="flex flex-wrap items-end gap-3">
        <AppSelect v-model="courierId" icon="user" :label="t('common.courier')" class="min-w-56">
          <option value="">{{ t('common.select') }}</option>
          <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
        </AppSelect>
        <AppInput v-model="fromDate" icon="calendar" :label="t('common.from')" type="date" />
        <AppInput v-model="toDate" icon="calendar" :label="t('common.to')" type="date" />
        <AppButton v-if="!isCurrentMonth" variant="secondary" @click="resetToCurrentMonth">
          <AppIcon name="calendar" class="size-4.5" />
          {{ t('saleLogs.currentMonth') }}
        </AppButton>
        <div class="ml-auto flex items-center gap-2 pb-3 text-theme-xs text-gray-500 dark:text-gray-400">
          <span class="relative flex size-2.5">
            <span v-if="isLive" class="absolute inline-flex size-full animate-ping rounded-full bg-success-500 opacity-60" />
            <span class="relative inline-flex size-2.5 rounded-full" :class="isLive ? 'bg-success-500' : 'bg-gray-300 dark:bg-gray-600'" />
          </span>
          <span v-if="isLive">{{ t('saleLogs.live') }}</span>
          <span v-if="updatedLabel">· {{ t('saleLogs.updatedAt', { time: updatedLabel }) }}</span>
        </div>
      </div>
    </div>

    <div
      v-if="!courierId"
      class="rounded-2xl border border-gray-200 bg-white px-5 py-8 text-center text-theme-sm text-gray-500 dark:border-gray-800 dark:bg-white/[0.03] dark:text-gray-400"
    >
      {{ t('courierReports.selectCourier') }}
    </div>

    <template v-else>
      <h2 class="mb-3 flex items-center gap-2 text-lg font-semibold text-gray-800 dark:text-white/90">
        <AppIcon name="cash" class="text-brand-500" />
        {{ t('courierReports.cashSection') }}
      </h2>
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
        <StatCard
          :label="t('cash.onCourier')"
          :value="money(courierCash?.onCourierSum)"
          :hint="
            courierCash?.onCourierCount
              ? t('courierReports.onCourierHint', {
                  count: courierCash.onCourierCount,
                  date: dateTime(courierCash.oldestOnCourierAt),
                })
              : t('courierReports.noDebt')
          "
          tone="warning"
          icon="wallet"
        />
        <StatCard
          :label="t('cash.handedInPeriod')"
          :value="money(courierCash?.handedSum)"
          :hint="
            courierCash?.lastHandedAt
              ? t('courierReports.handedHint', {
                  count: courierCash.handedCount,
                  date: dateTime(courierCash.lastHandedAt),
                })
              : t('cash.ordersHint', { count: courierCash?.handedCount ?? 0 })
          "
          tone="success"
          icon="cash"
        />
      </div>

      <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">
        {{ t('courierReports.byPaymentType') }}
      </h2>
      <div class="mb-2 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <StatCard
          :label="t('courierReports.totalSum')"
          :value="money(stats?.totalSum)"
          tone="success"
          icon="monthly"
        />
        <StatCard
          :label="t('courierReports.cashTotal')"
          :value="money(stats?.cashTotal)"
          tone="brand"
          icon="cash"
        />
        <StatCard
          :label="t('courierReports.cardTotal')"
          :value="money(stats?.cardTotal)"
          tone="cyan"
          icon="monthly"
        />
      </div>

      <p class="mb-6 text-theme-xs text-gray-500 dark:text-gray-400">
        {{ t('courierReports.deliveredOnly') }}
      </p>

      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
        <StatCard
          :label="t('courierReports.totalOrders')"
          :value="stats?.totalOrders ?? 0"
          tone="purple"
          icon="orders"
        />
        <StatCard
          :label="t('courierReports.deliveredCount')"
          :value="stats?.deliveredCount ?? 0"
          tone="success"
          icon="delivered"
        />
        <StatCard
          :label="t('courierReports.cancelledCount')"
          :value="stats?.canceledCount ?? 0"
          tone="error"
          icon="cancelled"
        />
        <StatCard
          :label="t('courierReports.pendingCount')"
          :value="stats?.pendingCount ?? 0"
          tone="warning"
          icon="daily"
        />
      </div>

      <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">
        {{ t('courierReports.orders') }}
      </h2>
      <DataTable
        :loading="loading && !orders.length"
        :empty="!loading && !orders.length ? t('courierReports.empty') : undefined"
      >
        <template #head>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.id') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.number') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.customer') }}</th>
          <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('orders.paymentType') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
        </template>
        <tr v-for="row in orders" :key="row.id">
          <td class="px-5 py-3 text-theme-sm">
            <RouterLink :to="`/admin/orders/${row.id}`" class="font-medium text-brand-500 hover:text-brand-600 hover:underline">
              #{{ row.id }}
            </RouterLink>
          </td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.orderNumber || t('common.empty') }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.customerName || t('common.empty') }}</td>
          <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ money(row.totalSum) }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
            <AppBadge :tone="row.paymentType === 'CARD' ? 'brand' : 'success'">
              {{ enumLabel('paymentType', row.paymentType, t('common.empty')) }}
            </AppBadge>
          </td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
            <AppBadge :tone="statusTone(row.orderStatus)">
              {{ enumLabel('orderStatus', row.orderStatus, t('common.empty')) }}
            </AppBadge>
          </td>
          <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ dateTime(row.createdAt) || t('common.empty') }}</td>
        </tr>
      </DataTable>
    </template>
  </div>
</template>
