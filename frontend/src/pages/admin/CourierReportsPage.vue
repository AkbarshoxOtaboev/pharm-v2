<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { fetchCourierStats, fetchOrders } from '@/api/orders.api'
import { fetchCouriers } from '@/api/users.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type { CourierStatsResponse, OrderResponse, UserResponse } from '@/types/api'
import { apiError, money, statusTone } from '@/utils/format'

const { t } = useI18n()

function isoDate(d: Date) {
  const month = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${month}-${day}`
}

const today = new Date()
const couriers = ref<UserResponse[]>([])
const courierId = ref('')
const fromDate = ref(isoDate(new Date(today.getFullYear(), today.getMonth(), 1)))
const toDate = ref(isoDate(today))
const stats = ref<CourierStatsResponse | null>(null)
const orders = ref<OrderResponse[]>([])
const loading = ref(false)
const error = ref('')

const selectedCourierName = computed(
  () => couriers.value.find((c) => String(c.id) === courierId.value)?.fullName || '',
)

async function loadCouriers() {
  const res = await fetchCouriers()
  couriers.value = res.data || []
  if (!courierId.value && couriers.value.length) {
    courierId.value = String(couriers.value[0].id)
  }
}

async function loadReport() {
  if (!courierId.value) {
    stats.value = null
    orders.value = []
    return
  }
  loading.value = true
  error.value = ''
  try {
    const from = fromDate.value || undefined
    const to = toDate.value || undefined
    const [statsRes, ordersRes] = await Promise.all([
      fetchCourierStats(Number(courierId.value), { from, to }),
      fetchOrders({ userId: Number(courierId.value), fromDate: from, toDate: to, size: -1 }),
    ])
    stats.value = statsRes.data
    orders.value = ordersRes.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadCourierReport')
  } finally {
    loading.value = false
  }
}

watch(courierId, () => {
  loadReport()
})

onMounted(async () => {
  try {
    await loadCouriers()
    await loadReport()
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  }
})
</script>

<template>
  <div>
    <PageHeader :title="t('courierReports.title')" :subtitle="t('courierReports.subtitle')" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div class="mb-6 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-4 shadow-theme-xs">
      <div class="flex flex-wrap items-end gap-3">
        <AppSelect v-model="courierId" :label="t('common.courier')">
          <option value="">{{ t('common.select') }}</option>
          <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
        </AppSelect>
        <AppInput v-model="fromDate" :label="t('common.from')" type="date" />
        <AppInput v-model="toDate" :label="t('common.to')" type="date" />
        <AppButton :loading="loading" @click="loadReport">{{ t('common.filter') }}</AppButton>
      </div>
    </div>

    <div
      v-if="!courierId"
      class="rounded-2xl border border-gray-200 bg-white px-5 py-8 text-center text-theme-sm text-gray-500 dark:border-gray-800 dark:bg-white/[0.03] dark:text-gray-400"
    >
      {{ t('courierReports.selectCourier') }}
    </div>

    <template v-else>
      <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">
        {{ t('courierReports.byPaymentType') }}
      </h2>
      <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-3">
        <StatCard
          :label="t('courierReports.totalSum')"
          :value="money(stats?.totalSum)"
          :hint="selectedCourierName || undefined"
          tone="success"
          icon="monthly"
        />
        <StatCard
          :label="t('courierReports.cashTotal')"
          :value="money(stats?.cashTotal)"
          tone="brand"
          icon="daily"
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
        :loading="loading"
        :empty="!loading && !orders.length ? t('courierReports.empty') : undefined"
      >
        <template #head>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.id') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.number') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.customer') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('orders.paymentType') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
        </template>
        <tr v-for="row in orders" :key="row.id">
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.id }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.orderNumber || t('common.empty') }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.customerName || t('common.empty') }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.totalSum) }}</td>
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
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.createdAt || t('common.empty') }}</td>
        </tr>
      </DataTable>
    </template>
  </div>
</template>
