<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
    error.value = apiError(e, 'Ombor maʼlumotlarini yuklab bo‘lmadi')
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
    error.value = apiError(e, 'Tarixni yuklab bo‘lmadi')
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
    arrivalError.value = 'Miqdor majburiy'
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
    arrivalError.value = apiError(e, 'Kirim qo‘shilmadi')
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
    <PageHeader title="Ombor" subtitle="Mahsulot zaxiralari va kirim" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
    >
      {{ error }}
    </div>

    <div v-if="stats" class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2 xl:grid-cols-4">
      <StatCard label="Mahsulotlar" :value="stats.totalProducts ?? 0" />
      <StatCard label="Jami miqdor" :value="stats.totalQuantity ?? 0" />
      <StatCard label="Jami tannarx" :value="money(stats.totalCostSum)" />
      <StatCard label="Bugungi kirim" :value="stats.todayAddedQuantity ?? 0" />
    </div>

    <h2 class="mb-3 text-lg font-semibold text-gray-800">Ombor ro‘yxati</h2>
    <DataTable
      :loading="loading"
      :empty="!loading && !items.length ? 'Ombor bo‘sh' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Mahsulot</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Narx</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Tannarx</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Miqdor</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Summa</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Kirim sanasi</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Status</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Amallar</th>
      </template>
      <tr v-for="row in items" :key="row.storeId">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.productName || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(row.productPrice) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(row.productPriceCost) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.storeQuantity ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(row.storeTotalAmount) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.storeDateOfArrival || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <AppBadge :tone="statusTone(row.storeStatus)">{{ row.storeStatus || '—' }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <AppButton size="sm" @click="openArrival(row)">Kirim</AppButton>
        </td>
      </tr>
    </DataTable>

    <div class="mt-8 mb-3 flex flex-wrap items-end justify-between gap-3">
      <h2 class="text-lg font-semibold text-gray-800">Kirim tarixi</h2>
      <div class="flex flex-wrap items-end gap-2">
        <AppInput v-model="histFrom" label="Dan" type="date" />
        <AppInput v-model="histTo" label="Gacha" type="date" />
        <AppButton @click="loadHistory">Filtrlash</AppButton>
      </div>
    </div>

    <DataTable
      :loading="histLoading"
      :empty="!histLoading && !history.length ? 'Tarix bo‘sh' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Mahsulot</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Miqdor</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Tannarx</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Summa</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Sana</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Izoh</th>
      </template>
      <tr v-for="h in history" :key="h.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ h.productName || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ h.quantity ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(h.priceCost as number) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(h.totalAmount as number) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          {{ (h.dateOfArrival as string) || h.createdAt || '—' }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ h.comment || '—' }}</td>
      </tr>
    </DataTable>

    <AppModal
      :open="arrivalOpen"
      :title="`Kirim — ${selected?.productName || ''}`"
      @close="arrivalOpen = false"
    >
      <div
        v-if="arrivalError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
      >
        {{ arrivalError }}
      </div>
      <form class="space-y-3" @submit.prevent="saveArrival">
        <AppInput v-model="arrivalForm.quantity" label="Miqdor" type="number" />
        <AppInput v-model="arrivalForm.dateOfArrival" label="Sana" type="date" />
        <AppTextarea v-model="arrivalForm.comment" label="Izoh" />
        <div class="flex justify-end gap-2">
          <AppButton type="button" variant="secondary" @click="arrivalOpen = false">
            Bekor
          </AppButton>
          <AppButton type="submit" :loading="arrivalSaving">Saqlash</AppButton>
        </div>
      </form>
    </AppModal>
  </div>
</template>
