<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { RouterLink, useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { fetchCourierPeriodCash } from '@/api/cashRegister.api'
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
import type { CourierPeriodCash, CourierStatsResponse, OrderResponse, OrderStatus } from '@/types/api'
import { apiError, dateTime, isoDate, money, statusTone } from '@/utils/format'

const ORDER_STATUSES: OrderStatus[] = [
  'NEW',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPED',
  'PENDING',
  'DELIVERED',
  'CANCELLED',
  'DELETED',
]

const { t } = useI18n()
const route = useRoute()
const router = useRouter()

const monthStart = () => {
  const now = new Date()
  return isoDate(new Date(now.getFullYear(), now.getMonth(), 1))
}

const yesterday = () => {
  const d = new Date()
  d.setDate(d.getDate() - 1)
  return isoDate(d)
}

function readNavDates() {
  const state = window.history.state as { from?: string; to?: string } | null
  return {
    from: typeof state?.from === 'string' ? state.from : monthStart(),
    to: typeof state?.to === 'string' ? state.to : isoDate(new Date()),
  }
}

const navDates = readNavDates()
const courierId = computed(() => Number(route.params.id))
const courierName = ref('')
const from = ref(navDates.from)
const to = ref(navDates.to)
const stats = ref<CourierStatsResponse | null>(null)
const periodCash = ref<CourierPeriodCash | null>(null)
const loading = ref(false)
const error = ref('')

/** Jadval uchun alohida filtrlar: dan = kecha, gacha = bugun */
const ordersFrom = ref(yesterday())
const ordersTo = ref(isoDate(new Date()))
const ordersStatus = ref('')
const orders = ref<OrderResponse[]>([])
const ordersLoading = ref(false)
const ordersError = ref('')

const rangeInvalid = computed(() => !!from.value && !!to.value && from.value > to.value)
const ordersRangeInvalid = computed(
  () => !!ordersFrom.value && !!ordersTo.value && ordersFrom.value > ordersTo.value,
)
const isCurrentMonth = computed(() => from.value === monthStart() && to.value === isoDate(new Date()))
const isOrdersDefaultRange = computed(
  () => ordersFrom.value === yesterday() && ordersTo.value === isoDate(new Date()),
)
/** Yetkazgan = Kassaga + Topshirishi kerak (bitta davr, bitta manba) */
const deliveredMoney = computed(() => Number(periodCash.value?.collectedSum ?? 0))
const handedMoney = computed(() => Number(periodCash.value?.handedSum ?? 0))
const toHandMoney = computed(() => Number(periodCash.value?.onCourierSum ?? 0))

async function loadName() {
  try {
    const res = await fetchCouriers()
    const found = (res.data || []).find((c) => c.id === courierId.value)
    courierName.value = found?.fullName || `#${courierId.value}`
  } catch {
    courierName.value = `#${courierId.value}`
  }
}

async function load() {
  if (!courierId.value) {
    error.value = t('errors.loadData')
    return
  }
  if (rangeInvalid.value) {
    error.value = t('errors.dateRangeInvalid')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const fromDate = from.value || undefined
    const toDate = to.value || undefined
    const [statsRes, periodRes] = await Promise.all([
      fetchCourierStats(courierId.value, { from: fromDate, to: toDate }),
      fetchCourierPeriodCash(courierId.value, { from: fromDate, to: toDate }),
    ])
    stats.value = statsRes.data
    periodCash.value = periodRes.data ?? null
  } catch (e) {
    error.value = apiError(e, 'errors.loadCourierReport')
  } finally {
    loading.value = false
  }
}

async function loadOrders() {
  if (!courierId.value) return
  if (ordersRangeInvalid.value) {
    ordersError.value = t('errors.dateRangeInvalid')
    return
  }
  ordersLoading.value = true
  ordersError.value = ''
  try {
    const res = await fetchOrders({
      userId: courierId.value,
      fromDate: ordersFrom.value || undefined,
      toDate: ordersTo.value || undefined,
      status: (ordersStatus.value as OrderStatus) || undefined,
      size: -1,
    })
    orders.value = res.data || []
  } catch (e) {
    ordersError.value = apiError(e, 'errors.loadOrders')
  } finally {
    ordersLoading.value = false
  }
}

function resetToCurrentMonth() {
  from.value = monthStart()
  to.value = isoDate(new Date())
}

function resetOrdersRange() {
  ordersFrom.value = yesterday()
  ordersTo.value = isoDate(new Date())
  ordersStatus.value = ''
}

function goBack() {
  router.push({ name: 'admin-couriers' })
}

watch([from, to], () => void load())
watch([ordersFrom, ordersTo, ordersStatus], () => void loadOrders())

onMounted(async () => {
  await loadName()
  await Promise.all([load(), loadOrders()])
})
</script>

<template>
  <div>
    <PageHeader
      :title="courierName || t('couriers.detailTitle')"
      :subtitle="t('couriers.detailSubtitle')"
    >
      <template #actions>
        <AppButton variant="secondary" @click="goBack">
          <AppIcon name="arrow-left" class="size-4.5" />
          {{ t('common.back') }}
        </AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div class="mb-6 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-3 lg:gap-4">
        <AppInput v-model="from" icon="calendar" :label="t('common.from')" type="date" />
        <AppInput v-model="to" icon="calendar" :label="t('common.to')" type="date" />
        <AppButton v-if="!isCurrentMonth" variant="secondary" @click="resetToCurrentMonth">
          <AppIcon name="calendar" class="size-4.5" />
          {{ t('saleLogs.currentMonth') }}
        </AppButton>

        <div class="ml-auto flex min-w-0 flex-wrap items-stretch gap-3 pb-0.5">
          <div class="min-w-[140px] rounded-xl border border-success-100 bg-success-50 px-3.5 py-2 dark:border-success-500/20 dark:bg-success-500/10">
            <p class="text-theme-xs font-medium text-success-700 dark:text-success-500">
              {{ t('couriers.deliveredMoney') }}
            </p>
            <p class="mt-0.5 text-theme-sm font-semibold tabular-nums text-success-700 dark:text-success-400">
              {{ money(deliveredMoney) }}
            </p>
          </div>
          <div class="min-w-[140px] rounded-xl border border-brand-100 bg-brand-50 px-3.5 py-2 dark:border-brand-500/20 dark:bg-brand-500/10">
            <p class="text-theme-xs font-medium text-brand-600 dark:text-brand-400">
              {{ t('couriers.handedMoney') }}
            </p>
            <p class="mt-0.5 text-theme-sm font-semibold tabular-nums text-brand-600 dark:text-brand-400">
              {{ money(handedMoney) }}
            </p>
          </div>
          <div class="min-w-[140px] rounded-xl border border-warning-100 bg-warning-50 px-3.5 py-2 dark:border-warning-500/20 dark:bg-warning-500/10">
            <p class="text-theme-xs font-medium text-warning-700 dark:text-warning-500">
              {{ t('couriers.toHandMoney') }}
            </p>
            <p class="mt-0.5 text-theme-sm font-semibold tabular-nums text-warning-700 dark:text-warning-500">
              {{ money(toHandMoney) }}
            </p>
          </div>
        </div>
      </div>
    </div>

    <div v-if="loading" class="mb-6 text-theme-sm text-gray-500">{{ t('common.loading') }}</div>

    <div v-else class="mb-8 space-y-3">
      <div class="grid grid-cols-4 gap-3">
        <StatCard
          :label="t('couriers.orders')"
          :value="Number(stats?.totalOrders ?? 0)"
          tone="brand"
          icon="orders"
        />
        <StatCard
          :label="t('couriers.delivered')"
          :value="Number(stats?.deliveredCount ?? 0)"
          tone="success"
          icon="delivered"
        />
        <StatCard
          :label="t('couriers.shipped')"
          :value="Number(stats?.shippedCount ?? 0)"
          tone="warning"
          icon="truck"
        />
        <StatCard
          :label="t('couriers.cancelled')"
          :value="Number(stats?.canceledCount ?? 0)"
          tone="error"
          icon="cancelled"
        />
      </div>
      <div class="grid grid-cols-4 gap-3">
        <StatCard
          :label="t('couriers.deliveredSum')"
          :value="money(stats?.totalSum)"
          tone="success"
          icon="cash"
        />
        <StatCard
          :label="t('couriers.shippedSum')"
          :value="money(stats?.shippedSum)"
          tone="warning"
          icon="truck"
        />
        <StatCard
          :label="t('couriers.cancelledSum')"
          :value="money(stats?.cancelledSum)"
          tone="error"
          icon="cancelled"
        />
        <StatCard
          :label="t('couriers.bonusSum')"
          :value="money(stats?.bonusSum)"
          tone="purple"
          icon="gift"
        />
      </div>
    </div>

    <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">
      {{ t('couriers.ordersTable') }}
    </h2>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-3">
        <AppInput v-model="ordersFrom" icon="calendar" :label="t('common.from')" type="date" />
        <AppInput v-model="ordersTo" icon="calendar" :label="t('common.to')" type="date" />
        <AppSelect v-model="ordersStatus" icon="layers" :label="t('common.status')" class="min-w-44">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
        </AppSelect>
        <AppButton v-if="!isOrdersDefaultRange || ordersStatus" variant="secondary" @click="resetOrdersRange">
          <AppIcon name="calendar" class="size-4.5" />
          {{ t('couriers.ordersDefaultRange') }}
        </AppButton>
      </div>
    </div>

    <div
      v-if="ordersError"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ ordersError }}
    </div>

    <DataTable
      :loading="ordersLoading"
      :empty="!ordersLoading && !orders.length ? t('couriers.ordersEmpty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">
          {{ t('common.id') }}
        </th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">
          {{ t('common.number') }}
        </th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">
          {{ t('common.customer') }}
        </th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">
          {{ t('common.amount') }}
        </th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">
          {{ t('orders.paymentType') }}
        </th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">
          {{ t('common.status') }}
        </th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">
          {{ t('common.date') }}
        </th>
      </template>
      <tr v-for="row in orders" :key="row.id" class="border-t border-gray-100 dark:border-gray-800">
        <td class="px-5 py-3 text-theme-sm">
          <RouterLink
            :to="`/admin/orders/${row.id}`"
            class="font-medium text-brand-500 hover:text-brand-600 hover:underline"
          >
            #{{ row.id }}
          </RouterLink>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ row.orderNumber || t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ row.customerName || t('common.empty') }}
        </td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm font-medium text-gray-800 dark:text-white/90">
          {{ money(row.totalSum) }}
        </td>
        <td class="px-5 py-3 text-theme-sm">
          <AppBadge :tone="row.paymentType === 'CARD' ? 'brand' : 'success'">
            {{ enumLabel('paymentType', row.paymentType, t('common.empty')) }}
          </AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm">
          <AppBadge :tone="statusTone(row.orderStatus)">
            {{ enumLabel('orderStatus', row.orderStatus, t('common.empty')) }}
          </AppBadge>
        </td>
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ dateTime(row.createdAt) || t('common.empty') }}
        </td>
      </tr>
    </DataTable>
  </div>
</template>
