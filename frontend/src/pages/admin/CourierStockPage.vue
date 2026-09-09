<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import {
  fetchCourierStock,
  fetchCourierStockHistory,
  returnStock,
  transferStock,
} from '@/api/courierStock.api'
import { fetchProductsInStock } from '@/api/products.api'
import { fetchCouriers } from '@/api/users.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type {
  CourierStockHistoryResponse,
  CourierStockResponse,
  ProductResponse,
  UserResponse,
} from '@/types/api'
import { apiError, money } from '@/utils/format'

const couriers = ref<UserResponse[]>([])
const products = ref<ProductResponse[]>([])
const courierId = ref('')
const stock = ref<CourierStockResponse[]>([])
const history = ref<CourierStockHistoryResponse[]>([])
const loading = ref(false)
const histLoading = ref(false)
const error = ref('')
const message = ref('')

const histFrom = ref('')
const histTo = ref('')

const transferForm = reactive({
  productId: '',
  quantity: '',
})
const transferSaving = ref(false)

const returnForm = reactive({
  productId: '',
  quantity: '',
})
const returnSaving = ref(false)

async function loadCouriersAndProducts() {
  const [cRes, pRes] = await Promise.all([fetchCouriers(), fetchProductsInStock()])
  couriers.value = cRes.data || []
  products.value = pRes.data || []
  if (!courierId.value && couriers.value.length) {
    courierId.value = String(couriers.value[0].id)
  }
}

async function loadStock() {
  if (!courierId.value) {
    stock.value = []
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await fetchCourierStock(Number(courierId.value))
    stock.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'Zaxirani yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
}

async function loadHistory() {
  if (!courierId.value) {
    history.value = []
    return
  }
  histLoading.value = true
  try {
    const res = await fetchCourierStockHistory(Number(courierId.value), {
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

async function reloadAll() {
  await Promise.all([loadStock(), loadHistory()])
}

watch(courierId, () => {
  reloadAll()
})

async function doTransfer() {
  message.value = ''
  error.value = ''
  if (!courierId.value || !transferForm.productId || !transferForm.quantity) {
    error.value = 'Kuryer, mahsulot va miqdor majburiy'
    return
  }
  transferSaving.value = true
  try {
    await transferStock({
      courierId: Number(courierId.value),
      items: [
        {
          productId: Number(transferForm.productId),
          quantity: Number(transferForm.quantity),
        },
      ],
    })
    message.value = 'Transfer muvaffaqiyatli'
    transferForm.productId = ''
    transferForm.quantity = ''
    await reloadAll()
  } catch (e) {
    error.value = apiError(e, 'Transfer amalga oshmadi')
  } finally {
    transferSaving.value = false
  }
}

async function doReturn() {
  message.value = ''
  error.value = ''
  if (!courierId.value || !returnForm.productId || !returnForm.quantity) {
    error.value = 'Kuryer, mahsulot va miqdor majburiy'
    return
  }
  returnSaving.value = true
  try {
    await returnStock({
      courierId: Number(courierId.value),
      productId: Number(returnForm.productId),
      quantity: Number(returnForm.quantity),
    })
    message.value = 'Qaytarish muvaffaqiyatli'
    returnForm.productId = ''
    returnForm.quantity = ''
    await reloadAll()
  } catch (e) {
    error.value = apiError(e, 'Qaytarish amalga oshmadi')
  } finally {
    returnSaving.value = false
  }
}

onMounted(async () => {
  try {
    await loadCouriersAndProducts()
    await reloadAll()
  } catch (e) {
    error.value = apiError(e, 'Maʼlumotlarni yuklab bo‘lmadi')
  }
})
</script>

<template>
  <div>
    <PageHeader title="Kuryer zaxira" subtitle="Transfer, qaytarish va tarix" />

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs">
      <AppSelect v-model="courierId" label="Kuryer">
        <option value="">Tanlang</option>
        <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
      </AppSelect>
    </div>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
    >
      {{ error }}
    </div>
    <div
      v-if="message"
      class="mb-4 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700"
    >
      {{ message }}
    </div>

    <div class="mb-6 grid grid-cols-1 gap-4 xl:grid-cols-2">
      <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs">
        <h2 class="mb-3 text-lg font-semibold text-gray-800">Ombordan transfer</h2>
        <div class="space-y-3">
          <AppSelect v-model="transferForm.productId" label="Mahsulot">
            <option value="">Tanlang</option>
            <option v-for="p in products" :key="p.id" :value="String(p.id)">
              {{ p.name }} (ombor: {{ p.storeQuantity ?? '—' }})
            </option>
          </AppSelect>
          <AppInput v-model="transferForm.quantity" label="Miqdor" type="number" />
          <AppButton :loading="transferSaving" @click="doTransfer">Transfer</AppButton>
        </div>
      </div>

      <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs">
        <h2 class="mb-3 text-lg font-semibold text-gray-800">Omborga qaytarish</h2>
        <div class="space-y-3">
          <AppSelect v-model="returnForm.productId" label="Mahsulot (zaxiradan)">
            <option value="">Tanlang</option>
            <option v-for="s in stock" :key="s.id" :value="String(s.productId)">
              {{ s.productName }} ({{ s.quantity }})
            </option>
          </AppSelect>
          <AppInput v-model="returnForm.quantity" label="Miqdor" type="number" />
          <AppButton variant="secondary" :loading="returnSaving" @click="doReturn">
            Qaytarish
          </AppButton>
        </div>
      </div>
    </div>

    <h2 class="mb-3 text-lg font-semibold text-gray-800">Joriy zaxira</h2>
    <DataTable
      :loading="loading"
      :empty="!loading && !stock.length ? 'Zaxira bo‘sh' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Mahsulot</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Miqdor</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Narx</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Summa</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Birlik</th>
      </template>
      <tr v-for="s in stock" :key="s.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ s.productName || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ s.quantity ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(s.productPrice) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(s.totalAmount) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ s.unitType || '—' }}</td>
      </tr>
    </DataTable>

    <div class="mt-8 mb-3 flex flex-wrap items-end justify-between gap-3">
      <h2 class="text-lg font-semibold text-gray-800">Tarix</h2>
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
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Amal</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Oldin</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">O‘zgarish</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Keyin</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Sana</th>
      </template>
      <tr v-for="(h, idx) in history" :key="idx">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ h.productName || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          {{ (h.actionType as string) || '—' }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          {{ (h.beforeQuantity as number) ?? '—' }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          {{ (h.changeQuantity as number) ?? h.quantity ?? '—' }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          {{ (h.afterQuantity as number) ?? '—' }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ h.createdAt || '—' }}</td>
      </tr>
    </DataTable>
  </div>
</template>
