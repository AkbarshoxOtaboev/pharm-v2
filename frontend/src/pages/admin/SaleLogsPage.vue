<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchSaleLogs } from '@/api/saleLogs.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type { SaleLogResponse } from '@/types/api'
import { apiError, dateTime, isoDate, money } from '@/utils/format'
import { formatUzPhone } from '@/utils/phone'

interface OrderSaleGroup {
  key: string
  orderId: number | null
  createdAt?: string
  customerName?: string
  customerPhone?: string
  customerAddress?: string
  items: SaleLogResponse[]
  /** Bonussiz (to‘lov) miqdori */
  soldQty: number
  /** Bonus miqdori */
  bonusQty: number
  /** Jami mahsulot miqdori */
  totalQty: number
  /** To‘lov summasi (bonussiz) */
  paidSum: number
  /** Bonus tovarlar qiymati */
  bonusSum: number
  /** Buyurtma umumiy summasi (to‘lov + bonus) */
  orderTotal: number
}

function lineValue(row: SaleLogResponse) {
  const qty = Number(row.quantity ?? 0)
  if (row.isBonus) return qty * Number(row.productPriceCost ?? 0)
  const sum = Number(row.totalSum ?? 0)
  return sum || qty * Number(row.productPriceCost ?? 0)
}

const REFRESH_MS = 30_000

const { t } = useI18n()
const logs = ref<SaleLogResponse[]>([])
const loading = ref(false)
const exporting = ref(false)
const error = ref('')
const updatedAt = ref<Date | null>(null)

const monthStart = () => {
  const now = new Date()
  return isoDate(new Date(now.getFullYear(), now.getMonth(), 1))
}
const from = ref(monthStart())
const to = ref(isoDate(new Date()))

const rangeInvalid = computed(() => Boolean(from.value && to.value && from.value > to.value))
const isCurrentMonth = computed(() => from.value === monthStart() && to.value === isoDate(new Date()))
const isLive = computed(() => !to.value || to.value >= isoDate(new Date()))

const orderGroups = computed((): OrderSaleGroup[] => groupSaleLogs(logs.value))
const ordersCount = computed(() => orderGroups.value.length)

const totalQty = computed(() => logs.value.reduce((s, l) => s + Number(l.quantity ?? 0), 0))
const soldQty = computed(() =>
  logs.value.filter((l) => !l.isBonus).reduce((s, l) => s + Number(l.quantity ?? 0), 0),
)
const bonusQty = computed(() =>
  logs.value.filter((l) => l.isBonus).reduce((s, l) => s + Number(l.quantity ?? 0), 0),
)
const paidSum = computed(() =>
  logs.value.filter((l) => !l.isBonus).reduce((s, l) => s + lineValue(l), 0),
)
const bonusSum = computed(() =>
  logs.value.filter((l) => l.isBonus).reduce((s, l) => s + lineValue(l), 0),
)
const grandTotal = computed(() => paidSum.value + bonusSum.value)

let loadSeq = 0
async function load(silent = false) {
  if (rangeInvalid.value) {
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

function groupSaleLogs(rows: SaleLogResponse[]): OrderSaleGroup[] {
  const map = new Map<string, OrderSaleGroup>()
  for (const row of rows) {
    const key = row.orderId != null ? `order-${row.orderId}` : `row-${row.id ?? map.size}`
    let group = map.get(key)
    if (!group) {
      group = {
        key,
        orderId: row.orderId ?? null,
        createdAt: row.createdAt,
        customerName: row.customerName,
        customerPhone: row.customerPhone,
        customerAddress: row.customerAddress,
        items: [],
        soldQty: 0,
        bonusQty: 0,
        totalQty: 0,
        paidSum: 0,
        bonusSum: 0,
        orderTotal: 0,
      }
      map.set(key, group)
    }
    group.items.push(row)
    const qty = Number(row.quantity ?? 0)
    const value = lineValue(row)
    group.totalQty += qty
    if (row.isBonus) {
      group.bonusQty += qty
      group.bonusSum += value
    } else {
      group.soldQty += qty
      group.paidSum += value
    }
    group.orderTotal = group.paidSum + group.bonusSum
    if (row.createdAt && (!group.createdAt || row.createdAt > group.createdAt)) group.createdAt = row.createdAt
    if (!group.customerName && row.customerName) group.customerName = row.customerName
    if (!group.customerPhone && row.customerPhone) group.customerPhone = row.customerPhone
    if (!group.customerAddress && row.customerAddress) group.customerAddress = row.customerAddress
  }
  return Array.from(map.values())
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
      fetchSaleLogs({
        from: from.value || undefined,
        to: to.value || undefined,
      }),
    ])
    const rows = res.data || []
    const groups = groupSaleLogs(rows)
    const period = `${dateTime(from.value) || '…'} — ${dateTime(to.value) || '…'}`
    const excelPaidSum = groups.reduce((s, g) => s + g.paidSum, 0)
    const excelBonusSum = groups.reduce((s, g) => s + g.bonusSum, 0)
    const excelGrandTotal = excelPaidSum + excelBonusSum
    const excelSoldQty = groups.reduce((s, g) => s + g.soldQty, 0)
    const excelBonusQty = groups.reduce((s, g) => s + g.bonusQty, 0)
    const excelTotalQty = excelSoldQty + excelBonusQty

    const header = (value: string) => ({
      value,
      fontWeight: 'bold' as const,
      backgroundColor: '#E5E7EB',
      borderStyle: 'thin' as const,
      align: 'center' as const,
      alignVertical: 'center' as const,
    })
    const cell = { borderStyle: 'thin' as const, alignVertical: 'center' as const }
    const num = (value: number, format = '#,##0') => ({
      ...cell,
      value,
      type: Number,
      format,
    })
    const merged = (value: string | number, span: number, extra: Record<string, unknown> = {}) => ({
      ...cell,
      value,
      rowSpan: span,
      alignVertical: 'center' as const,
      ...extra,
    })

    type ExcelCell = Record<string, unknown> | null
    const dataRows: ExcelCell[][] = []
    groups.forEach((group, orderIndex) => {
      const span = Math.max(group.items.length, 1)
      const phone = group.customerPhone
        ? formatUzPhone(group.customerPhone) || group.customerPhone
        : ''
      group.items.forEach((item, itemIndex) => {
        const productCells: ExcelCell[] = [
          { ...cell, value: item.productName || '' },
          num(Number(item.quantity ?? 0)),
          num(Number(item.productPriceCost ?? 0)),
          item.isBonus ? { ...cell, value: '' } : num(Number(item.totalSum ?? 0)),
          { ...cell, value: item.isBonus ? t('common.yes') : t('common.no'), align: 'center' as const },
        ]
        if (itemIndex === 0) {
          dataRows.push([
            merged(orderIndex + 1, span, { type: Number, align: 'center' as const }),
            merged(dateTime(group.createdAt) || '', span),
            merged(group.orderId != null ? `#${group.orderId}` : '', span),
            merged(group.customerName || '', span),
            merged(phone, span),
            ...productCells,
          ])
        } else {
          dataRows.push([null, null, null, null, null, ...productCells])
        }
      })
    })

    await writeXlsxFile(
      [
        [{ value: t('saleLogs.excelTitle'), fontWeight: 'bold', fontSize: 14, columnSpan: 10 }, ...Array(9).fill(null)],
        [{ value: t('saleLogs.excelPeriod', { period }), columnSpan: 10 }, ...Array(9).fill(null)],
        [],
        [
          header('№'),
          header(t('common.date')),
          header(t('common.order')),
          header(t('common.customer')),
          header(t('common.phone')),
          header(t('common.product')),
          header(t('common.quantity')),
          header(t('common.price')),
          header(t('common.amount')),
          header(t('common.bonus')),
        ],
        ...dataRows,
        [],
        [
          { ...cell, value: t('common.total'), fontWeight: 'bold' },
          { ...cell, value: `${t('saleLogs.ordersCount')}: ${groups.length}`, fontWeight: 'bold' },
          { ...cell, value: `${t('saleLogs.totalQty')}: ${excelTotalQty}`, fontWeight: 'bold' },
          { ...cell, value: `${t('saleLogs.soldQty')}: ${excelSoldQty}`, fontWeight: 'bold' },
          { ...cell, value: `${t('saleLogs.bonusQty')}: ${excelBonusQty}`, fontWeight: 'bold' },
          { ...cell, value: '' },
          { ...cell, value: '' },
          { ...num(excelBonusSum), fontWeight: 'bold' },
          { ...num(excelPaidSum), fontWeight: 'bold' },
          { ...num(excelGrandTotal), fontWeight: 'bold' },
        ],
      ],
      {
        sheet: t('saleLogs.title'),
        columns: [
          { width: 6 },
          { width: 18 },
          { width: 10 },
          { width: 22 },
          { width: 16 },
          { width: 28 },
          { width: 12 },
          { width: 14 },
          { width: 14 },
          { width: 10 },
        ],
      },
    ).toFile(`sotuv_loglari_${from.value || 'all'}_${to.value || 'all'}.xlsx`)
  } catch (e) {
    error.value = apiError(e, 'errors.exportFailed')
  } finally {
    exporting.value = false
  }
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
    <PageHeader :title="t('saleLogs.title')" :subtitle="t('saleLogs.subtitle')">
      <template #actions>
        <AppButton variant="success" :loading="exporting" :disabled="rangeInvalid || !logs.length" @click="exportExcel">
          <AppIcon v-if="!exporting" name="download" class="size-4.5" />
          {{ t('saleLogs.exportExcel') }}
        </AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-3">
        <AppInput v-model="from" :label="t('common.from')" type="date" />
        <AppInput v-model="to" :label="t('common.to')" type="date" />
        <AppButton v-if="!isCurrentMonth" variant="secondary" @click="resetToCurrentMonth">
          {{ t('saleLogs.currentMonth') }}
        </AppButton>
        <div class="pb-2.5">
          <p class="text-theme-xs text-gray-500 dark:text-gray-400">{{ t('saleLogs.ordersCount') }}</p>
          <p class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ ordersCount }}</p>
        </div>
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

    <div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2 xl:grid-cols-3">
      <StatCard :label="t('saleLogs.totalQty')" :value="money(totalQty)" tone="brand" icon="orders" />
      <StatCard :label="t('saleLogs.soldQty')" :value="money(soldQty)" tone="cyan" icon="delivered" />
      <StatCard :label="t('saleLogs.bonusQty')" :value="money(bonusQty)" tone="purple" icon="gift" />
      <StatCard :label="t('saleLogs.grandTotal')" :value="money(grandTotal)" tone="success" icon="monthly" />
      <StatCard :label="t('saleLogs.paidSum')" :value="money(paidSum)" tone="warning" icon="cash" />
      <StatCard :label="t('saleLogs.bonusSum')" :value="money(bonusSum)" tone="error" icon="wallet" />
    </div>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="rounded-2xl border border-gray-200 bg-white px-5 py-10 text-center text-theme-sm text-gray-500 dark:border-gray-800 dark:bg-white/[0.03] dark:text-gray-400">
      {{ t('common.loading') }}
    </div>

    <div
      v-else-if="!orderGroups.length"
      class="rounded-2xl border border-gray-200 bg-white px-5 py-10 text-center text-theme-sm text-gray-500 dark:border-gray-800 dark:bg-white/[0.03] dark:text-gray-400"
    >
      {{ t('saleLogs.notFound') }}
    </div>

    <div v-else class="space-y-4">
      <article
        v-for="group in orderGroups"
        :key="group.key"
        class="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]"
      >
        <header class="flex flex-wrap items-start justify-between gap-3 border-b border-gray-100 px-5 py-4 dark:border-gray-800">
          <div class="min-w-0 space-y-1">
            <div class="flex flex-wrap items-center gap-2">
              <RouterLink
                v-if="group.orderId != null"
                :to="`/admin/orders/${group.orderId}`"
                class="text-theme-sm font-semibold text-brand-500 hover:text-brand-600 hover:underline"
              >
                #{{ group.orderId }}
              </RouterLink>
              <span v-else class="text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ t('common.order') }}</span>
              <span class="text-theme-xs text-gray-500 dark:text-gray-400">{{ dateTime(group.createdAt) || t('common.empty') }}</span>
            </div>
            <p class="text-theme-sm font-medium text-gray-800 dark:text-white/90">
              {{ group.customerName || t('common.empty') }}
            </p>
            <p class="text-theme-xs text-gray-500 dark:text-gray-400">
              {{ group.customerPhone ? formatUzPhone(group.customerPhone) || group.customerPhone : t('common.empty') }}
              <template v-if="group.customerAddress"> · {{ group.customerAddress }}</template>
            </p>
          </div>
          <div class="min-w-0 space-y-2 text-right">
            <div class="flex flex-wrap items-center justify-end gap-1.5">
              <span class="rounded-lg bg-gray-50 px-2.5 py-1 text-theme-xs text-gray-600 dark:bg-white/5 dark:text-gray-300">
                {{ t('saleLogs.totalQty') }}: {{ money(group.totalQty) }}
              </span>
              <span class="rounded-lg bg-cyan-50 px-2.5 py-1 text-theme-xs text-cyan-700 dark:bg-cyan-500/10 dark:text-cyan-300">
                {{ t('saleLogs.paidItems') }}: {{ money(group.soldQty) }}
              </span>
              <span
                v-if="group.bonusQty"
                class="rounded-lg bg-success-50 px-2.5 py-1 text-theme-xs text-success-700 dark:bg-success-500/10 dark:text-success-400"
              >
                {{ t('saleLogs.bonusItems') }}: {{ money(group.bonusQty) }}
              </span>
            </div>
            <div class="flex flex-wrap items-center justify-end gap-1.5">
              <span class="rounded-lg bg-warning-50 px-2.5 py-1 text-theme-xs text-warning-700 dark:bg-warning-500/10 dark:text-warning-400">
                {{ t('saleLogs.paidSum') }}: {{ money(group.paidSum) }}
              </span>
              <span
                v-if="group.bonusSum"
                class="rounded-lg bg-error-50 px-2.5 py-1 text-theme-xs text-error-600 dark:bg-error-500/10 dark:text-error-400"
              >
                {{ t('saleLogs.bonusSum') }}: {{ money(group.bonusSum) }}
              </span>
              <span class="rounded-lg bg-brand-50 px-2.5 py-1 text-theme-sm font-semibold text-brand-600 dark:bg-brand-500/10 dark:text-brand-400">
                {{ t('saleLogs.orderTotal') }}: {{ money(group.orderTotal) }}
              </span>
            </div>
          </div>
        </header>

        <div class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-100 dark:divide-gray-800">
            <thead class="bg-gray-50/80 dark:bg-white/[0.02]">
              <tr>
                <th class="px-5 py-2.5 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
                <th class="px-5 py-2.5 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
                <th class="px-5 py-2.5 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
                <th class="px-5 py-2.5 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
                <th class="px-5 py-2.5 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.bonus') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
              <tr v-for="(item, idx) in group.items" :key="item.id ?? `${group.key}-${idx}`">
                <td class="px-5 py-3 text-theme-sm">
                  <p class="text-gray-800 dark:text-white/90">{{ item.productName || t('common.empty') }}</p>
                  <p v-if="item.categoryName" class="text-theme-xs text-gray-500 dark:text-gray-400">{{ item.categoryName }}</p>
                </td>
                <td class="px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ money(item.quantity) }}</td>
                <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">
                  {{ money(item.productPriceCost) }}
                </td>
                <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm font-medium text-gray-800 dark:text-white/90">
                  {{ item.isBonus ? t('common.empty') : money(item.totalSum) }}
                </td>
                <td class="px-5 py-3 text-theme-sm">
                  <AppBadge :tone="item.isBonus ? 'success' : 'gray'">
                    {{ item.isBonus ? t('common.yes') : t('common.no') }}
                  </AppBadge>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </article>
    </div>
  </div>
</template>
