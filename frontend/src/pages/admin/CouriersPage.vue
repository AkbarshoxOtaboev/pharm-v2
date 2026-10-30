<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { fetchCourierMonthStats } from '@/api/orders.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { CourierMonthStat } from '@/types/api'
import { apiError, dateTime, isoDate } from '@/utils/format'

const { t } = useI18n()
const router = useRouter()

const monthStart = () => {
  const now = new Date()
  return isoDate(new Date(now.getFullYear(), now.getMonth(), 1))
}

const from = ref(monthStart())
const to = ref(isoDate(new Date()))
const search = ref('')
const rows = ref<CourierMonthStat[]>([])
const loading = ref(false)
const exporting = ref(false)
const error = ref('')

const rangeInvalid = computed(() => !!from.value && !!to.value && from.value > to.value)
const isCurrentMonth = computed(() => from.value === monthStart() && to.value === isoDate(new Date()))

const filtered = computed(() => {
  const q = search.value.trim().toLowerCase()
  if (!q) return rows.value
  return rows.value.filter((r) => (r.courierName || '').toLowerCase().includes(q))
})

async function load() {
  if (rangeInvalid.value) {
    error.value = t('errors.dateRangeInvalid')
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await fetchCourierMonthStats({
      from: from.value || undefined,
      to: to.value || undefined,
    })
    rows.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  } finally {
    loading.value = false
  }
}

watch([from, to], () => void load())

function resetToCurrentMonth() {
  from.value = monthStart()
  to.value = isoDate(new Date())
}

function openDetail(row: CourierMonthStat) {
  if (!row.courierId) return
  router.push({
    name: 'admin-couriers-detail',
    params: { id: String(row.courierId) },
    state: { from: from.value, to: to.value },
  })
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
      fetchCourierMonthStats({ from: from.value || undefined, to: to.value || undefined }),
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
        [
          { value: t('couriers.excelTitle'), fontWeight: 'bold', fontSize: 14, columnSpan: 6 },
          null,
          null,
          null,
          null,
          null,
        ],
        [
          { value: t('couriers.excelPeriod', { period }), columnSpan: 6 },
          null,
          null,
          null,
          null,
          null,
        ],
        [],
        [
          header('№'),
          header(t('couriers.fio')),
          header(t('couriers.orders')),
          header(t('couriers.delivered')),
          header(t('couriers.shipped')),
          header(t('couriers.cancelled')),
        ],
        ...data.map((row, i) => [
          { ...cell, value: i + 1, type: Number, align: 'center' as const },
          { ...cell, value: row.courierName || '' },
          { ...cell, value: Number(row.allOrders ?? 0), type: Number, align: 'center' as const },
          { ...cell, value: Number(row.deliveredOrders ?? 0), type: Number, align: 'center' as const },
          { ...cell, value: Number(row.shippedOrders ?? 0), type: Number, align: 'center' as const },
          { ...cell, value: Number(row.cancelledOrders ?? 0), type: Number, align: 'center' as const },
        ]),
      ],
      {
        sheet: t('couriers.title'),
        columns: [
          { width: 6 },
          { width: 28 },
          { width: 12 },
          { width: 14 },
          { width: 14 },
          { width: 14 },
        ],
      },
    ).toFile(`kuryerlar_${from.value || 'all'}_${to.value || 'all'}.xlsx`)
  } catch (e) {
    error.value = apiError(e, 'errors.exportFailed')
  } finally {
    exporting.value = false
  }
}

onMounted(() => void load())
</script>

<template>
  <div>
    <PageHeader :title="t('couriers.title')" :subtitle="t('couriers.subtitle')">
      <template #actions>
        <AppButton variant="success" :loading="exporting" :disabled="rangeInvalid" @click="exportExcel">
          <AppIcon name="download" class="size-4.5" />
          {{ t('cash.exportExcel') }}
        </AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 flex items-center gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      <AppIcon name="alert" class="size-4.5" />
      {{ error }}
    </div>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-3">
        <AppInput
          v-model="search"
          icon="search"
          :label="t('common.search')"
          :placeholder="t('couriers.searchPlaceholder')"
          class="min-w-56 flex-1"
        />
        <AppInput v-model="from" icon="calendar" :label="t('common.from')" type="date" />
        <AppInput v-model="to" icon="calendar" :label="t('common.to')" type="date" />
        <AppButton v-if="!isCurrentMonth" variant="secondary" @click="resetToCurrentMonth">
          <AppIcon name="calendar" class="size-4.5" />
          {{ t('saleLogs.currentMonth') }}
        </AppButton>
      </div>
    </div>

    <DataTable :loading="loading" :empty="filtered.length ? undefined : t('couriers.empty')">
      <template #head>
        <th class="whitespace-nowrap px-4 py-3.5 text-left text-theme-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">
          №
        </th>
        <th class="whitespace-nowrap px-4 py-3.5 text-left text-theme-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">
          {{ t('couriers.fio') }}
        </th>
        <th class="whitespace-nowrap px-4 py-3.5 text-right text-theme-xs font-semibold uppercase tracking-wide text-brand-500">
          {{ t('couriers.orders') }}
        </th>
        <th class="whitespace-nowrap px-4 py-3.5 text-right text-theme-xs font-semibold uppercase tracking-wide text-success-600">
          {{ t('couriers.delivered') }}
        </th>
        <th class="whitespace-nowrap px-4 py-3.5 text-right text-theme-xs font-semibold uppercase tracking-wide text-warning-600">
          {{ t('couriers.shipped') }}
        </th>
        <th class="whitespace-nowrap px-4 py-3.5 text-right text-theme-xs font-semibold uppercase tracking-wide text-error-600">
          {{ t('couriers.cancelled') }}
        </th>
        <th class="whitespace-nowrap px-4 py-3.5 text-center text-theme-xs font-semibold uppercase tracking-wide text-gray-500 dark:text-gray-400">
          {{ t('common.actions') }}
        </th>
      </template>
      <tr
        v-for="(row, i) in filtered"
        :key="row.courierId"
        class="group border-t border-gray-100 transition hover:bg-gray-50/80 dark:border-gray-800 dark:hover:bg-white/[0.02]"
      >
        <td class="px-4 py-3.5 text-theme-sm tabular-nums text-gray-500 dark:text-gray-400">
          {{ i + 1 }}
        </td>
        <td class="px-4 py-3.5">
          <button
            type="button"
            class="text-left text-theme-sm font-semibold text-gray-800 transition hover:text-brand-500 hover:underline dark:text-white/90 dark:hover:text-brand-400"
            @click="openDetail(row)"
          >
            {{ row.courierName || '—' }}
          </button>
        </td>
        <td class="px-4 py-3.5 text-right text-theme-sm font-semibold tabular-nums text-brand-500">
          {{ Number(row.allOrders ?? 0) }}
        </td>
        <td class="px-4 py-3.5 text-right text-theme-sm font-semibold tabular-nums text-success-600">
          {{ Number(row.deliveredOrders ?? 0) }}
        </td>
        <td class="px-4 py-3.5 text-right text-theme-sm font-semibold tabular-nums text-warning-600">
          {{ Number(row.shippedOrders ?? 0) }}
        </td>
        <td class="px-4 py-3.5 text-right text-theme-sm font-semibold tabular-nums text-error-600">
          {{ Number(row.cancelledOrders ?? 0) }}
        </td>
        <td class="px-4 py-3.5 text-center">
          <button
            type="button"
            class="inline-flex size-9 items-center justify-center rounded-lg border border-brand-200 bg-brand-50 text-brand-500 transition hover:bg-brand-500 hover:text-white dark:border-brand-500/30 dark:bg-brand-500/10"
            :title="t('couriers.overall')"
            @click="openDetail(row)"
          >
            <AppIcon name="eye" class="size-4.5" />
          </button>
        </td>
      </tr>
    </DataTable>
  </div>
</template>
