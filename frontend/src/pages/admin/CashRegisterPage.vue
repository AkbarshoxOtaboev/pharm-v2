<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchCashSummary, fetchDailyCashPage } from '@/api/cashRegister.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import PaginationBar from '@/components/ui/PaginationBar.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type { CashSummaryResponse, DailyCashDTO } from '@/types/api'
import { apiError, dateTime, isoDate, money } from '@/utils/format'

/** 0 = all rows */
const PAGE_SIZES = [50, 100, 200, 0]
const REFRESH_MS = 30_000

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

const summary = ref<CashSummaryResponse | null>(null)
const days = ref<DailyCashDTO[]>([])
const loading = ref(false)
const exporting = ref(false)
const page = ref(0)
const size = ref(String(PAGE_SIZES[0]))
const totalPages = ref(0)
const totalElements = ref(0)

const debtorsCount = computed(() => (summary.value?.couriers ?? []).filter((c) => c.onCourierSum > 0).length)
const dayMax = computed(() => Math.max(1, ...days.value.map((d) => Number(d.total ?? 0))))

const updatedLabel = computed(() =>
  updatedAt.value
    ? updatedAt.value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '',
)

function periodParams() {
  return { from: from.value || undefined, to: to.value || undefined }
}

let loadSeq = 0
async function load(silent = false) {
  if (rangeInvalid.value) {
    error.value = t('errors.dateRangeInvalid')
    return
  }
  const seq = ++loadSeq
  if (!silent) loading.value = true
  error.value = ''
  try {
    const [summaryRes, daysRes] = await Promise.all([
      fetchCashSummary(periodParams()),
      fetchDailyCashPage({ ...periodParams(), page: page.value, size: Number(size.value) }),
    ])
    if (seq !== loadSeq) return
    summary.value = summaryRes.data
    days.value = daysRes.data || []
    totalPages.value = daysRes.meta?.totalPages ?? 1
    totalElements.value = daysRes.meta?.totalElements ?? days.value.length
    updatedAt.value = new Date()
  } catch (e) {
    if (seq === loadSeq) error.value = apiError(e, 'errors.loadCash')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

let followCurrentMonth = true
watch([from, to, size], () => {
  followCurrentMonth = isCurrentMonth.value
  page.value = 0
  void load()
})

function goToPage(p: number) {
  page.value = p
  void load()
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
      fetchDailyCashPage({ ...periodParams(), page: 0, size: 0 }),
    ])
    const rows = res.data || []
    const totalSum = rows.reduce((s, d) => s + Number(d.total ?? 0), 0)
    const totalCount = rows.reduce((s, d) => s + Number(d.ordersCount ?? 0), 0)
    const period = `${dateTime(from.value) || '…'} — ${dateTime(to.value) || '…'}`
    const header = (value: string) => ({
      value,
      fontWeight: 'bold' as const,
      backgroundColor: '#E5E7EB',
      borderStyle: 'thin' as const,
      align: 'center' as const,
    })
    const cell = { borderStyle: 'thin' as const }
    const sumCell = (value: number) => ({ ...cell, value, type: Number, format: '#,##0' })

    await writeXlsxFile(
      [
        [{ value: t('cash.excelTitle'), fontWeight: 'bold', fontSize: 14, columnSpan: 4 }, null, null, null],
        [{ value: t('cash.excelPeriod', { period }), columnSpan: 4 }, null, null, null],
        [],
        [header('№'), header(t('common.date')), header(t('cash.ordersCount')), header(t('cash.receivedSum'))],
        ...rows.map((d, i) => [
          { ...cell, value: i + 1, type: Number, align: 'center' as const },
          { ...cell, value: dateTime(d.date), align: 'center' as const },
          { ...cell, value: Number(d.ordersCount ?? 0), type: Number, align: 'center' as const },
          sumCell(Number(d.total ?? 0)),
        ]),
        [
          { ...cell, value: t('common.total'), fontWeight: 'bold', columnSpan: 2 },
          null,
          { ...cell, value: totalCount, type: Number, fontWeight: 'bold', align: 'center' },
          { ...sumCell(totalSum), fontWeight: 'bold' },
        ],
      ],
      { sheet: t('cash.title'), columns: [{ width: 6 }, { width: 14 }, { width: 18 }, { width: 20 }] },
    ).toFile(`kassa_${from.value || 'all'}_${to.value || 'all'}.xlsx`)
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
  if (isLive.value) void load(true)
}

onMounted(() => {
  void load()
  timer = setInterval(tick, REFRESH_MS)
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div>
    <PageHeader :title="t('cash.title')" :subtitle="t('cash.subtitle')">
      <template #actions>
        <AppButton variant="success" :loading="exporting" :disabled="rangeInvalid" @click="exportExcel">
          <AppIcon v-if="!exporting" name="download" class="size-4.5" />
          {{ t('cash.exportExcel') }}
        </AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-3">
        <AppInput v-model="from" icon="calendar" :label="t('common.from')" type="date" />
        <AppInput v-model="to" icon="calendar" :label="t('common.to')" type="date" />
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
      v-if="error"
      class="mb-4 flex items-center gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      <AppIcon name="alert" class="size-4.5" />
      {{ error }}
    </div>

    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
      <StatCard
        :label="t('cash.handedInPeriod')"
        :value="money(summary?.handedSum)"
        :hint="t('cash.ordersHint', { count: summary?.handedCount ?? 0 })"
        tone="success"
        icon="cash"
      />
      <StatCard
        :label="t('cash.onCouriers')"
        :value="money(summary?.onCourierSum)"
        :hint="t('cash.ordersHint', { count: summary?.onCourierCount ?? 0 })"
        tone="warning"
        icon="wallet"
      />
      <StatCard
        :label="t('cash.debtors')"
        :value="debtorsCount"
        :hint="t('cash.debtorsHint', { total: summary?.couriers?.length ?? 0 })"
        tone="purple"
        icon="truck"
      />
    </div>

    <div class="mb-3 flex flex-wrap items-center justify-between gap-3">
      <h2 class="flex items-center gap-2 text-lg font-semibold text-gray-800 dark:text-white/90">
        <AppIcon name="calendar" class="text-brand-500" />
        {{ t('cash.dailyReport') }}
      </h2>
      <AppSelect v-model="size" class="w-28">
        <option v-for="s in PAGE_SIZES" :key="s" :value="String(s)">{{ s ? s : t('common.all') }}</option>
      </AppSelect>
    </div>
    <p class="mb-3 text-theme-xs text-gray-500 dark:text-gray-400">{{ t('cash.dailyHint') }}</p>

    <DataTable
      :loading="loading && !days.length"
      :empty="!loading && !days.length ? t('cash.reportEmpty') : undefined"
    >
      <template #head>
        <th class="w-40 px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
        <th class="w-40 px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('cash.ordersCount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400" />
        <th class="w-48 px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('cash.receivedSum') }}</th>
      </template>
      <tr v-for="d in days" :key="d.date">
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ dateTime(d.date) || t('common.empty') }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm text-gray-600 dark:text-gray-400">{{ d.ordersCount ?? 0 }}</td>
        <td class="px-5 py-3">
          <div class="h-2.5 w-full overflow-hidden rounded-full bg-gray-100 dark:bg-white/5">
            <div class="h-full rounded-full bg-success-500" :style="{ width: `${(Number(d.total ?? 0) / dayMax) * 100}%` }" />
          </div>
        </td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ money(d.total) }}</td>
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
