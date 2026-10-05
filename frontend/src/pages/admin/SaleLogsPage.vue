<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchSaleLogs } from '@/api/saleLogs.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type { SaleLogResponse } from '@/types/api'
import { apiError, dateTime, isoDate, money } from '@/utils/format'
import { formatUzPhone } from '@/utils/phone'

const REFRESH_MS = 30_000

const { t } = useI18n()
const logs = ref<SaleLogResponse[]>([])
const loading = ref(false)
const error = ref('')
const updatedAt = ref<Date | null>(null)

const monthStart = () => {
  const now = new Date()
  return isoDate(new Date(now.getFullYear(), now.getMonth(), 1))
}
const from = ref(monthStart())
const to = ref(isoDate(new Date()))

const isCurrentMonth = computed(() => from.value === monthStart() && to.value === isoDate(new Date()))
const isLive = computed(() => !to.value || to.value >= isoDate(new Date()))

const paidLogs = computed(() => logs.value.filter((l) => !l.isBonus))
const totalSum = computed(() => paidLogs.value.reduce((s, l) => s + Number(l.totalSum ?? 0), 0))
const ordersCount = computed(() => new Set(logs.value.map((l) => l.orderId)).size)
const soldQty = computed(() => paidLogs.value.reduce((s, l) => s + Number(l.quantity ?? 0), 0))
const bonusQty = computed(() =>
  logs.value.filter((l) => l.isBonus).reduce((s, l) => s + Number(l.quantity ?? 0), 0),
)

let loadSeq = 0
async function load(silent = false) {
  if (from.value && to.value && from.value > to.value) {
    error.value = t('errors.dateRangeInvalid')
    logs.value = []
    return
  }
  const seq = ++loadSeq
  if (!silent) loading.value = true
  error.value = ''
  try {
    const res = await fetchSaleLogs({
      from: from.value || undefined,
      to: to.value || undefined,
    })
    if (seq !== loadSeq) return
    logs.value = res.data || []
    updatedAt.value = new Date()
  } catch (e) {
    if (seq === loadSeq) error.value = apiError(e, 'errors.loadSaleLogs')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

function resetToCurrentMonth() {
  from.value = monthStart()
  to.value = isoDate(new Date())
}

let followCurrentMonth = true
watch([from, to], () => {
  followCurrentMonth = from.value === monthStart() && to.value === isoDate(new Date())
  void load()
})

let timer: ReturnType<typeof setInterval> | undefined
function tick() {
  if (document.hidden) return
  if (followCurrentMonth && (from.value !== monthStart() || to.value !== isoDate(new Date()))) {
    resetToCurrentMonth()
    return
  }
  if (!to.value || to.value >= isoDate(new Date())) void load(true)
}

onMounted(() => {
  void load()
  timer = setInterval(tick, REFRESH_MS)
})
onBeforeUnmount(() => clearInterval(timer))

const updatedLabel = computed(() =>
  updatedAt.value
    ? updatedAt.value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '',
)
</script>

<template>
  <div>
    <PageHeader :title="t('saleLogs.title')" :subtitle="t('saleLogs.subtitle')" />

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-3">
        <AppInput v-model="from" :label="t('common.from')" type="date" />
        <AppInput v-model="to" :label="t('common.to')" type="date" />
        <AppButton v-if="!isCurrentMonth" variant="secondary" @click="resetToCurrentMonth">
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

    <div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2 lg:grid-cols-5">
      <StatCard class="sm:col-span-2" :label="t('saleLogs.totalSum')" :value="money(totalSum)" tone="success" icon="monthly" />
      <StatCard :label="t('saleLogs.ordersCount')" :value="ordersCount" tone="brand" icon="orders" />
      <StatCard :label="t('saleLogs.soldQty')" :value="money(soldQty)" tone="cyan" icon="delivered" />
      <StatCard :label="t('saleLogs.bonusQty')" :value="money(bonusQty)" tone="purple" icon="daily" />
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
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.bonus') }}</th>
      </template>
      <tr v-for="(row, idx) in logs" :key="row.id ?? `${row.orderId}-${idx}`">
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ dateTime(row.createdAt) || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm">
          <RouterLink
            v-if="row.orderId"
            :to="`/admin/orders/${row.orderId}`"
            class="font-medium text-brand-500 hover:text-brand-600 hover:underline"
          >
            #{{ row.orderId }}
          </RouterLink>
          <span v-else class="text-gray-400">{{ t('common.empty') }}</span>
        </td>
        <td class="max-w-64 px-5 py-3 text-theme-sm">
          <p class="truncate text-gray-800 dark:text-white/90" :title="row.customerName">{{ row.customerName || t('common.empty') }}</p>
          <p
            v-if="row.customerAddress && row.customerAddress !== row.customerName"
            class="truncate text-theme-xs text-gray-500 dark:text-gray-400"
            :title="row.customerAddress"
          >
            {{ row.customerAddress }}
          </p>
        </td>
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ row.customerPhone ? formatUzPhone(row.customerPhone) || row.customerPhone : t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm">
          <p class="text-gray-800 dark:text-white/90">{{ row.productName || t('common.empty') }}</p>
          <p v-if="row.categoryName" class="text-theme-xs text-gray-500 dark:text-gray-400">{{ row.categoryName }}</p>
        </td>
        <td class="px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.quantity) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.productPriceCost) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm font-medium text-gray-800 dark:text-white/90">
          {{ row.isBonus ? t('common.empty') : money(row.totalSum) }}
        </td>
        <td class="px-5 py-3 text-theme-sm">
          <AppBadge :tone="row.isBonus ? 'success' : 'gray'">
            {{ row.isBonus ? t('common.yes') : t('common.no') }}
          </AppBadge>
        </td>
      </tr>
    </DataTable>
  </div>
</template>
