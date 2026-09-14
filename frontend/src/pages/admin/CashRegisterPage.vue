<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import {
  fetchCourierCash,
  fetchCourierCashTotal,
  fetchDailyCashReport,
  returnCash,
} from '@/api/cashRegister.api'
import { fetchCouriers } from '@/api/users.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type { CashRegisterResponse, DailyCashDTO, UserResponse } from '@/types/api'
import { apiError, money, statusTone } from '@/utils/format'

const { t } = useI18n()
const couriers = ref<UserResponse[]>([])
const courierId = ref('')
const entries = ref<CashRegisterResponse[]>([])
const totalCash = ref(0)
const loading = ref(false)
const error = ref('')
const returningId = ref<number | null>(null)

const reportFrom = ref('')
const reportTo = ref('')
const reportLoading = ref(false)
const report = ref<DailyCashDTO[]>([])

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

async function loadCash() {
  if (!courierId.value) {
    entries.value = []
    totalCash.value = 0
    return
  }
  loading.value = true
  error.value = ''
  try {
    const [listRes, totalRes] = await Promise.all([
      fetchCourierCash(Number(courierId.value)),
      fetchCourierCashTotal(Number(courierId.value)),
    ])
    entries.value = listRes.data || []
    const rawTotal = totalRes.data
    totalCash.value = typeof rawTotal === 'number' ? rawTotal : Number(rawTotal?.total ?? 0)
  } catch (e) {
    error.value = apiError(e, 'errors.loadCash')
  } finally {
    loading.value = false
  }
}

async function loadReport() {
  reportLoading.value = true
  try {
    const res = await fetchDailyCashReport({
      from: reportFrom.value || undefined,
      to: reportTo.value || undefined,
    })
    report.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadDailyReport')
  } finally {
    reportLoading.value = false
  }
}

async function onReturn(row: CashRegisterResponse) {
  if (!confirm(t('common.confirmReturnCash', { id: row.id }))) return
  returningId.value = row.id
  try {
    await returnCash(row.id)
    await loadCash()
    await loadReport()
  } catch (e) {
    error.value = apiError(e, 'errors.returnCashFailed')
  } finally {
    returningId.value = null
  }
}

watch(courierId, () => {
  loadCash()
})

onMounted(async () => {
  try {
    await loadCouriers()
    await Promise.all([loadCash(), loadReport()])
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  }
})
</script>

<template>
  <div>
    <PageHeader :title="t('cash.title')" :subtitle="t('cash.subtitle')" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-4 shadow-theme-xs">
      <AppSelect v-model="courierId" :label="t('common.courier')">
        <option value="">{{ t('common.select') }}</option>
        <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
      </AppSelect>
    </div>

    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
      <StatCard
        :label="t('cash.totalOnCourier')"
        :value="money(totalCash)"
        :hint="selectedCourierName || undefined"
      />
      <StatCard :label="t('cash.entriesCount')" :value="entries.length" />
    </div>

    <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('cash.entries') }}</h2>
    <DataTable
      :loading="loading"
      :empty="!loading && !entries.length ? t('cash.empty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.id') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.order') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.comment') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="row in entries" :key="row.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.id }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.orderId || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.orderTotalSum) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppBadge :tone="statusTone(row.registerStatus)">
            {{ enumLabel('cashStatus', row.registerStatus, t('common.empty')) }}
          </AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ row.dateTime || row.createdAt || t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.comment || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppButton
            v-if="String(row.registerStatus).toUpperCase() === 'ON_COURIER'"
            size="sm"
            :loading="returningId === row.id"
            @click="onReturn(row)"
          >
            {{ t('cash.return') }}
          </AppButton>
          <span v-else class="text-gray-400 dark:text-gray-500">{{ t('common.empty') }}</span>
        </td>
      </tr>
    </DataTable>

    <div class="mt-8 mb-3 flex flex-wrap items-end justify-between gap-3">
      <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('cash.dailyReport') }}</h2>
      <div class="flex flex-wrap items-end gap-2">
        <AppInput v-model="reportFrom" :label="t('common.from')" type="date" />
        <AppInput v-model="reportTo" :label="t('common.to')" type="date" />
        <AppButton @click="loadReport">{{ t('common.filter') }}</AppButton>
      </div>
    </div>

    <DataTable
      :loading="reportLoading"
      :empty="!reportLoading && !report.length ? t('cash.reportEmpty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.total') }}</th>
      </template>
      <tr v-for="(r, idx) in report" :key="r.date || idx">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ r.date || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ money(r.total ?? r.totalSum) }}
        </td>
      </tr>
    </DataTable>
  </div>
</template>
