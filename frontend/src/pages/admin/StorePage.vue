<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import {
  addStoreArrival,
  fetchStore,
  fetchStoreHistory,
  fetchStoreStatistics,
} from '@/api/store.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type {
  StoreHistoryResponse,
  StoreResponse,
  StoreStatisticsResponse,
} from '@/types/api'
import { apiError, money, statusTone } from '@/utils/format'

const { t } = useI18n()
const items = ref<StoreResponse[]>([])
const history = ref<StoreHistoryResponse[]>([])
const stats = ref<StoreStatisticsResponse | null>(null)
const loading = ref(false)
const histLoading = ref(false)
const error = ref('')

const histFrom = ref('')
const histTo = ref('')

const arrivalOpen = ref(false)
const arrivalSaving = ref(false)
const arrivalError = ref('')
const selected = ref<StoreResponse | null>(null)
const arrivalForm = reactive({
  quantity: '',
  dateOfArrival: '',
  comment: '',
})

async function loadStore() {
  loading.value = true
  error.value = ''
  try {
    const [storeRes, statsRes] = await Promise.all([
      fetchStore(),
      fetchStoreStatistics().catch(() => null),
    ])
    items.value = storeRes.data || []
    stats.value = statsRes?.data || null
  } catch (e) {
    error.value = apiError(e, 'errors.loadStore')
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  histLoading.value = true
  try {
    const res = await fetchStoreHistory({
      from: histFrom.value || undefined,
      to: histTo.value || undefined,
    })
    history.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadHistory')
  } finally {
    histLoading.value = false
  }
}

function openArrival(row: StoreResponse) {
  selected.value = row
  arrivalForm.quantity = ''
  arrivalForm.dateOfArrival = new Date().toISOString().slice(0, 10)
  arrivalForm.comment = ''
  arrivalError.value = ''
  arrivalOpen.value = true
}

async function saveArrival() {
  if (!selected.value) return
  if (!arrivalForm.quantity || Number(arrivalForm.quantity) <= 0) {
    arrivalError.value = t('errors.quantityRequired')
    return
  }
  arrivalSaving.value = true
  arrivalError.value = ''
  try {
    await addStoreArrival(selected.value.storeId, {
      quantity: Number(arrivalForm.quantity),
      dateOfArrival: arrivalForm.dateOfArrival || undefined,
      comment: arrivalForm.comment.trim() || undefined,
    })
    arrivalOpen.value = false
    await Promise.all([loadStore(), loadHistory()])
  } catch (e) {
    arrivalError.value = apiError(e, 'errors.addArrivalFailed')
  } finally {
    arrivalSaving.value = false
  }
}

onMounted(async () => {
  await loadStore()
  await loadHistory()
})
</script>

<template>
  <div>
    <PageHeader :title="t('store.title')" :subtitle="t('store.subtitle')" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div v-if="stats" class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard :label="t('store.totalProducts')" :value="stats.totalProducts ?? 0" />
      <StatCard :label="t('store.totalQuantity')" :value="stats.totalQuantity ?? 0" />
      <StatCard :label="t('store.totalCost')" :value="money(stats.totalCostSum)" />
      <StatCard :label="t('store.todayArrival')" :value="stats.todayAddedQuantity ?? 0" />
    </div>

    <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('store.list') }}</h2>
    <DataTable
      :loading="loading"
      :empty="!loading && !items.length ? t('store.empty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.cost') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('store.arrivalDate') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="row in items" :key="row.storeId">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.productName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.productPrice) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.productPriceCost) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.storeQuantity ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(row.storeTotalAmount) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ row.storeDateOfArrival || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppBadge :tone="statusTone(row.storeStatus)">{{ enumLabel('status', row.storeStatus, t('common.empty')) }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppButton size="sm" @click="openArrival(row)">{{ t('store.arrival') }}</AppButton>
        </td>
      </tr>
    </DataTable>

    <div class="mt-8 mb-3 flex flex-wrap items-end justify-between gap-3">
      <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('store.history') }}</h2>
      <div class="flex flex-wrap items-end gap-2">
        <AppInput v-model="histFrom" :label="t('common.from')" type="date" />
        <AppInput v-model="histTo" :label="t('common.to')" type="date" />
        <AppButton @click="loadHistory">{{ t('common.filter') }}</AppButton>
      </div>
    </div>

    <DataTable
      :loading="histLoading"
      :empty="!histLoading && !history.length ? t('store.historyEmpty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.cost') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.comment') }}</th>
      </template>
      <tr v-for="h in history" :key="h.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ h.productName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ h.quantity ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(h.priceCost as number) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(h.totalAmount as number) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ (h.dateOfArrival as string) || h.createdAt || t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ h.comment || t('common.empty') }}</td>
      </tr>
    </DataTable>

    <AppModal
      :open="arrivalOpen"
      :title="t('store.arrivalTitle', { name: selected?.productName || '' })"
      @close="arrivalOpen = false"
    >
      <div
        v-if="arrivalError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ arrivalError }}
      </div>
      <form class="space-y-3" @submit.prevent="saveArrival">
        <AppInput v-model="arrivalForm.quantity" :label="t('common.quantity')" type="number" />
        <AppInput v-model="arrivalForm.dateOfArrival" :label="t('common.date')" type="date" />
        <AppTextarea v-model="arrivalForm.comment" :label="t('common.comment')" />
        <div class="flex justify-end gap-2">
          <AppButton type="button" variant="secondary" @click="arrivalOpen = false">
            {{ t('common.cancel') }}
          </AppButton>
          <AppButton type="submit" :loading="arrivalSaving">{{ t('common.save') }}</AppButton>
        </div>
      </form>
    </AppModal>
  </div>
</template>
