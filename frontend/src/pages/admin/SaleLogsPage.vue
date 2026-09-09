<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { fetchSaleLogs } from '@/api/saleLogs.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { SaleLogResponse } from '@/types/api'
import { apiError, money } from '@/utils/format'

const logs = ref<SaleLogResponse[]>([])
const loading = ref(false)
const error = ref('')
const from = ref('')
const to = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchSaleLogs({
      from: from.value || undefined,
      to: to.value || undefined,
    })
    logs.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'Sotuv loglarini yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="Sotuv loglari" subtitle="Mahsulot sotuvlari tarixi" />

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs">
      <div class="flex flex-wrap items-end gap-2">
        <AppInput v-model="from" label="Dan" type="date" />
        <AppInput v-model="to" label="Gacha" type="date" />
        <AppButton @click="load">Filtrlash</AppButton>
        <AppButton
          variant="secondary"
          @click="
            from = '';
            to = '';
            load()
          "
        >
          Tozalash
        </AppButton>
      </div>
    </div>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
    >
      {{ error }}
    </div>

    <DataTable
      :loading="loading"
      :empty="!loading && !logs.length ? 'Loglar topilmadi' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Sana</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Buyurtma</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Mijoz</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Telefon</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Mahsulot</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Miqdor</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Summa</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Bonus</th>
      </template>
      <tr v-for="(row, idx) in logs" :key="row.orderId + '-' + idx">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.createdAt || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.orderId || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.customerName || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.customerPhone || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.productName || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.quantity ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(row.totalSum) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ row.isBonus ? 'Ha' : 'Yo‘q' }}</td>
      </tr>
    </DataTable>
  </div>
</template>
