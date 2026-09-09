<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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
    const t = totalRes.data
    totalCash.value = typeof t === 'number' ? t : Number(t?.total ?? 0)
  } catch (e) {
    error.value = apiError(e, 'Naqd pul maʼlumotlarini yuklab bo‘lmadi')
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
    error.value = apiError(e, 'Kunlik hisobotni yuklab bo‘lmadi')
  } finally {
    reportLoading.value = false
  }
}

async function onReturn(row: CashRegisterResponse) {
  if (!confirm(`#${row.id} naqd pulni qaytarilsinmi?`)) return
  returningId.value = row.id
  try {
    await returnCash(row.id)
    await loadCash()
    await loadReport()
  } catch (e) {
    error.value = apiError(e, 'Qaytarib bo‘lmadi')
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
    error.value = apiError(e, 'Maʼlumotlarni yuklab bo‘lmadi')
  }
})
</script>

<template>
  <div>
    <PageHeader title="Naqd pul" subtitle="Kuryer kassasi va kunlik hisobot" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
    >
      {{ error }}
    </div>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs">
      <AppSelect v-model="courierId" label="Kuryer">
        <option value="">Tanlang</option>
        <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
      </AppSelect>
    </div>

    <div class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-2">
      <StatCard
        label="Kuryerdagi jami"
        :value="money(totalCash)"
        :hint="selectedCourierName || undefined"
      />
      <StatCard label="Yozuvlar soni" :value="entries.length" />
    </div>

    <h2 class="mb-3 text-lg font-semibold text-gray-800">Kassa yozuvlari</h2>
    <DataTable
      :loading="loading"
      :empty="!loading && !entries.length ? 'Yozuvlar yo‘q' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">ID</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Buyurtma</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Summa</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Status</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Sana</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Izoh</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Amallar</th>
      </template>
      <tr v-for="row in entries" :key="row.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.id }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.orderId || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(row.orderTotalSum) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <AppBadge :tone="statusTone(row.registerStatus)">
            {{ row.registerStatus || '—' }}
          </AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          {{ row.dateTime || row.createdAt || '—' }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.comment || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <AppButton
            v-if="String(row.registerStatus).toUpperCase() === 'ON_COURIER'"
            size="sm"
            :loading="returningId === row.id"
            @click="onReturn(row)"
          >
            Qaytarish
          </AppButton>
          <span v-else class="text-gray-400">—</span>
        </td>
      </tr>
    </DataTable>

    <div class="mt-8 mb-3 flex flex-wrap items-end justify-between gap-3">
      <h2 class="text-lg font-semibold text-gray-800">Kunlik hisobot</h2>
      <div class="flex flex-wrap items-end gap-2">
        <AppInput v-model="reportFrom" label="Dan" type="date" />
        <AppInput v-model="reportTo" label="Gacha" type="date" />
        <AppButton @click="loadReport">Filtrlash</AppButton>
      </div>
    </div>

    <DataTable
      :loading="reportLoading"
      :empty="!reportLoading && !report.length ? 'Hisobot bo‘sh' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Sana</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Jami</th>
      </template>
      <tr v-for="(r, idx) in report" :key="r.date || idx">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ r.date || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          {{ money(r.total ?? r.totalSum) }}
        </td>
      </tr>
    </DataTable>
  </div>
</template>
