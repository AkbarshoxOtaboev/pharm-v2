<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchCustomerSalesPage, fetchCustomerSalesSummary } from '@/api/customers.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import PaginationBar from '@/components/ui/PaginationBar.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type { CustomerSalesRow, CustomerSalesSegment, CustomerSalesSummary } from '@/types/api'
import { apiError, dateTime, isoDate, money } from '@/utils/format'

const PAGE_SIZES = [50, 100, 150, 200, 0]
const REFRESH_MS = 30_000
const SEGMENTS: CustomerSalesSegment[] = ['ORDERED', 'NONE', 'CANCELLED']

const { t } = useI18n()
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
const rangeInvalid = computed(() => !!from.value && !!to.value && from.value > to.value)

const summary = ref<CustomerSalesSummary | null>(null)
const rows = ref<CustomerSalesRow[]>([])
const segment = ref<CustomerSalesSegment>('ORDERED')
const loading = ref(false)
const exporting = ref(false)
const page = ref(0)
const size = ref(String(PAGE_SIZES[0]))
const totalPages = ref(0)
const totalElements = ref(0)

const updatedLabel = computed(() =>
  updatedAt.value
    ? updatedAt.value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '',
)

const segmentLabel = computed(() => t(`salesStats.segment.${segment.value}`))

function avgCheck(row: CustomerSalesRow) {
  const delivered = Number(row.deliveredCount ?? 0)
  if (delivered <= 0) return 0
  return Math.round(Number(row.orderSum ?? 0) / delivered)
}

function periodParams() {
  return { from: from.value || undefined, to: to.value || undefined }
}

let summarySeq = 0
async function loadSummary() {
  const seq = ++summarySeq
  try {
    const summaryRes = await fetchCustomerSalesSummary({ to: isoDate(new Date()) })
    if (seq !== summarySeq) return
    summary.value = summaryRes.data
  } catch (e) {
    if (seq === summarySeq) error.value = apiError(e, 'errors.loadStats')
  }
}

let loadSeq = 0
async function loadTable(silent = false) {
  if (rangeInvalid.value) {
    error.value = t('errors.dateRangeInvalid')
    return
  }
  const seq = ++loadSeq
  if (!silent) loading.value = true
  error.value = ''
  try {
    const pageRes = await fetchCustomerSalesPage({
      ...periodParams(),
      segment: segment.value,
      page: page.value,
      size: Number(size.value),
    })
    if (seq !== loadSeq) return
    rows.value = pageRes.data || []
    totalPages.value = pageRes.meta?.totalPages ?? 1
    totalElements.value = pageRes.meta?.totalElements ?? rows.value.length
    updatedAt.value = new Date()
  } catch (e) {
    if (seq === loadSeq) error.value = apiError(e, 'errors.loadStats')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

let followCurrentMonth = true
watch([from, to, size, segment], () => {
  followCurrentMonth = isCurrentMonth.value
  page.value = 0
  void loadTable()
})

function goToPage(p: number) {
  page.value = p
  void loadTable()
}

function resetToCurrentMonth() {
  from.value = monthStart()
  to.value = isoDate(new Date())
}

async function exportExcel() {
  if (rangeInvalid.value) {
    error.value = t('errors.dateRangeInvalid')
    return
  }
  exporting.value = true
  error.value = ''
  try {
    const [{ default: writeXlsxFile }, res] = await Promise.all([
      import('write-excel-file/browser'),
      fetchCustomerSalesPage({ ...periodParams(), segment: segment.value, page: 0, size: 0 }),
    ])
    const data = res.data || []
    const period = `${dateTime(from.value) || '…'} — ${dateTime(to.value) || '…'}`
    const header = (value: string) => ({
      value,
      fontWeight: 'bold' as const,
      backgroundColor: '#E5E7EB',
      borderStyle: 'thin' as const,
      align: 'center' as const,
    })
    const cell = { borderStyle: 'thin' as const }
    await writeXlsxFile(
      [
        [{ value: t('salesStats.excelTitle'), fontWeight: 'bold', fontSize: 14, columnSpan: 8 }, null, null, null, null, null, null, null],
        [{ value: t('salesStats.excelPeriod', { period, segment: segmentLabel.value }), columnSpan: 8 }, null, null, null, null, null, null, null],
        [],
        [
          header('№'),
          header(t('orders.fullName')),
          header(t('common.phone')),
          header(t('salesStats.orders')),
          header(t('salesStats.delivered')),
          header(t('salesStats.cancelled')),
          header(t('customers.avgCheck')),
          header(t('salesStats.sum')),
        ],
        ...data.map((row, i) => [
          { ...cell, value: i + 1, type: Number, align: 'center' as const },
          { ...cell, value: row.fullName || '' },
          { ...cell, value: row.phone || '' },
          { ...cell, value: Number(row.orderCount ?? 0), type: Number, align: 'center' as const },
          { ...cell, value: Number(row.deliveredCount ?? 0), type: Number, align: 'center' as const },
          { ...cell, value: Number(row.cancelledCount ?? 0), type: Number, align: 'center' as const },
          { ...cell, value: avgCheck(row), type: Number, format: '#,##0' },
          { ...cell, value: Number(row.orderSum ?? 0), type: Number, format: '#,##0' },
        ]),
      ],
      {
        sheet: t('nav.salesStats'),
        columns: [{ width: 6 }, { width: 28 }, { width: 16 }, { width: 14 }, { width: 16 }, { width: 16 }, { width: 16 }, { width: 18 }],
      },
    ).toFile(`sotuv_${segment.value.toLowerCase()}_${from.value || 'all'}_${to.value || 'all'}.xlsx`)
  } catch (e) {
    error.value = apiError(e, 'errors.exportFailed')
  } finally {
    exporting.value = false
  }
}

let timer: ReturnType<typeof setInterval> | undefined
function tick() {
  if (document.hidden) return
  if (followCurrentMonth && !isCurrentMonth.value) {
    resetToCurrentMonth()
    return
  }
  if (isLive.value) {
    void loadSummary()
    void loadTable(true)
  }
}

onMounted(() => {
  void loadSummary()
  void loadTable()
  timer = setInterval(tick, REFRESH_MS)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div>
    <PageHeader :title="t('salesStats.title')" :subtitle="t('salesStats.subtitle')" />

    <div
      v-if="error"
      class="mb-4 flex items-center gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      <AppIcon name="alert" class="size-4.5" />
      {{ error }}
    </div>

    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
      <StatCard
        :label="t('salesStats.totalCustomers')"
        :value="summary?.totalCustomers ?? 0"
        :hint="t('salesStats.untilNow')"
        tone="brand"
        icon="users"
      />
      <StatCard
        :label="t('salesStats.activeBase')"
        :value="summary?.activeBase ?? 0"
        :hint="t('salesStats.activeBaseHint')"
        tone="success"
        icon="orders"
      />
      <StatCard
        :label="t('salesStats.noOrders')"
        :value="summary?.noOrders ?? 0"
        :hint="t('salesStats.untilNow')"
        tone="warning"
        icon="cancelled"
      />
    </div>

    <div class="mb-3 flex flex-wrap items-end gap-3">
      <AppInput v-model="from" icon="calendar" :label="t('common.from')" type="date" class="w-44" />
      <AppInput v-model="to" icon="calendar" :label="t('common.to')" type="date" class="w-44" />
      <AppSelect v-model="segment" :label="t('common.filter')" class="w-56">
        <option v-for="item in SEGMENTS" :key="item" :value="item">{{ t(`salesStats.segment.${item}`) }}</option>
      </AppSelect>
      <AppSelect v-model="size" :label="t('salesStats.limit')" class="w-32">
        <option v-for="s in PAGE_SIZES" :key="s" :value="String(s)">{{ s ? s : t('common.all') }}</option>
      </AppSelect>
      <AppButton variant="success" class="h-11" :loading="exporting" :disabled="rangeInvalid" @click="exportExcel">
        <AppIcon v-if="!exporting" name="download" class="size-4.5" />
        {{ t('cash.exportExcel') }}
      </AppButton>
      <AppButton v-if="!isCurrentMonth" variant="secondary" class="h-11" @click="resetToCurrentMonth">
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

    <DataTable
      :loading="loading && !rows.length"
      :empty="!loading && !rows.length ? t('salesStats.empty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('orders.fullName') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.phone') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('salesStats.orders') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('salesStats.delivered') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('salesStats.cancelled') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('customers.avgCheck') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('salesStats.sum') }}</th>
      </template>
      <tr v-for="row in rows" :key="row.id">
        <td class="px-5 py-3 text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ row.fullName || t('common.empty') }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-600 dark:text-gray-400">{{ row.phone || t('common.empty') }}</td>
        <td class="px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ row.orderCount ?? 0 }}</td>
        <td class="px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ row.deliveredCount ?? 0 }}</td>
        <td class="px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ row.cancelledCount ?? 0 }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ money(avgCheck(row)) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ money(row.orderSum) }}</td>
      </tr>
    </DataTable>

    <PaginationBar
      :page="page"
      :total-pages="totalPages"
      :total-elements="totalElements"
      :loading="loading"
      @update:page="goToPage"
    />
  </div>
</template>
