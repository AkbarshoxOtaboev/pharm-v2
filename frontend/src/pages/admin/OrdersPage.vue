<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import {
  fetchOrder,
  fetchOrders,
  updateOrderStatus,
  type OrderListParams,
} from '@/api/orders.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { OrderDetailResponse, OrderResponse, OrderStatus } from '@/types/api'
import { apiError, money, statusTone } from '@/utils/format'

const ORDER_STATUSES: OrderStatus[] = [
  'NEW',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPED',
  'PENDING',
  'DELIVERED',
  'CANCELLED',
  'DELETED',
]

const router = useRouter()
const orders = ref<OrderResponse[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(20)
const totalPages = ref(0)
const totalElements = ref(0)

const phone = ref('')
const status = ref('')
const fromDate = ref('')
const toDate = ref('')

const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<OrderDetailResponse | null>(null)
const statusSaving = ref(false)
const newStatus = ref('')

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params: OrderListParams = {
      page: page.value,
      size: size.value,
    }
    if (phone.value.trim()) params.phone = phone.value.trim()
    if (status.value) params.status = status.value
    if (fromDate.value) params.fromDate = fromDate.value
    if (toDate.value) params.toDate = toDate.value

    const res = await fetchOrders(params)
    orders.value = res.data || []
    totalPages.value = res.meta?.totalPages ?? 1
    totalElements.value = res.meta?.totalElements ?? orders.value.length
  } catch (e) {
    error.value = apiError(e, 'Buyurtmalarni yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
}

function onSearch() {
  page.value = 0
  load()
}

function resetFilters() {
  phone.value = ''
  status.value = ''
  fromDate.value = ''
  toDate.value = ''
  page.value = 0
  load()
}

async function openDetail(id: number) {
  detailOpen.value = true
  detailLoading.value = true
  detailError.value = ''
  detail.value = null
  try {
    const res = await fetchOrder(id)
    detail.value = res.data
    newStatus.value = res.data?.status || ''
  } catch (e) {
    detailError.value = apiError(e, 'Buyurtma tafsilotini yuklab bo‘lmadi')
  } finally {
    detailLoading.value = false
  }
}

async function saveStatus() {
  if (!detail.value?.orderId || !newStatus.value) return
  statusSaving.value = true
  detailError.value = ''
  try {
    await updateOrderStatus(detail.value.orderId, newStatus.value)
    await openDetail(detail.value.orderId)
    await load()
  } catch (e) {
    detailError.value = apiError(e, 'Statusni yangilab bo‘lmadi')
  } finally {
    statusSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="Buyurtmalar" subtitle="Buyurtmalar ro‘yxati va boshqaruvi">
      <template #actions>
        <AppButton @click="router.push('/admin/orders/create')">Yangi buyurtma</AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-5">
        <AppInput v-model="phone" label="Telefon" placeholder="+998..." />
        <AppSelect v-model="status" label="Status">
          <option value="">Barchasi</option>
          <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ s }}</option>
        </AppSelect>
        <AppInput v-model="fromDate" label="Dan" type="date" />
        <AppInput v-model="toDate" label="Gacha" type="date" />
        <div class="flex items-end gap-2">
          <AppButton class="w-full" @click="onSearch">Qidirish</AppButton>
          <AppButton variant="secondary" @click="resetFilters">Tozalash</AppButton>
        </div>
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
      :empty="!loading && !orders.length ? 'Buyurtmalar topilmadi' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">ID</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Raqam</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Mijoz</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Telefon</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Status</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Summa</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Kuryer</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Amallar</th>
      </template>
      <tr v-for="o in orders" :key="o.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ o.id }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ o.orderNumber || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ o.customerName || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ o.customerPhone || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <AppBadge :tone="statusTone(o.orderStatus)">{{ o.orderStatus || '—' }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(o.totalSum) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ o.courierName || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="secondary" @click="openDetail(o.id)">Ko‘rish</AppButton>
            <AppButton size="sm" variant="ghost" @click="router.push(`/admin/orders/${o.id}`)">
              Sahifa
            </AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
      <p class="text-theme-sm text-gray-500">Jami: {{ totalElements }}</p>
      <div class="flex items-center gap-2">
        <AppButton
          size="sm"
          variant="secondary"
          :disabled="page <= 0"
          @click="page--; load()"
        >
          Oldingi
        </AppButton>
        <span class="text-theme-sm text-gray-600">{{ page + 1 }} / {{ totalPages || 1 }}</span>
        <AppButton
          size="sm"
          variant="secondary"
          :disabled="page + 1 >= totalPages"
          @click="page++; load()"
        >
          Keyingi
        </AppButton>
      </div>
    </div>

    <AppModal
      :open="detailOpen"
      title="Buyurtma tafsiloti"
      @close="detailOpen = false"
    >
      <div v-if="detailLoading" class="text-theme-sm text-gray-500">Yuklanmoqda...</div>
      <div
        v-else-if="detailError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
      >
        {{ detailError }}
      </div>
      <template v-else-if="detail">
        <div class="mb-4 grid grid-cols-1 gap-2 text-theme-sm text-gray-700 sm:grid-cols-2">
          <p><span class="text-gray-500">ID:</span> {{ detail.orderId }}</p>
          <p><span class="text-gray-500">Raqam:</span> {{ detail.orderNumber || '—' }}</p>
          <p><span class="text-gray-500">Mijoz:</span> {{ detail.fullName || '—' }}</p>
          <p><span class="text-gray-500">Telefon:</span> {{ detail.phone || '—' }}</p>
          <p><span class="text-gray-500">Manzil:</span> {{ detail.address || '—' }}</p>
          <p><span class="text-gray-500">Uy:</span> {{ detail.house || '—' }}</p>
          <p><span class="text-gray-500">Podyezd:</span> {{ detail.entrance || '—' }}</p>
          <p><span class="text-gray-500">Xonadon:</span> {{ detail.apartment || '—' }}</p>
          <p><span class="text-gray-500">Qavat:</span> {{ detail.floor || '—' }}</p>
          <p><span class="text-gray-500">Mo‘ljal:</span> {{ detail.orientation || '—' }}</p>
          <p><span class="text-gray-500">Kuryer:</span> {{ detail.courier?.fullName || '—' }}</p>
          <p><span class="text-gray-500">Summa:</span> {{ money(detail.totalAmount) }}</p>
        </div>

        <div class="mb-4 flex flex-wrap items-end gap-2">
          <AppSelect v-model="newStatus" label="Status" class="min-w-48 flex-1">
            <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ s }}</option>
          </AppSelect>
          <AppButton :loading="statusSaving" @click="saveStatus">Saqlash</AppButton>
        </div>

        <h3 class="mb-2 text-theme-sm font-semibold text-gray-800">Mahsulotlar</h3>
        <div class="overflow-hidden rounded-xl border border-gray-200">
          <table class="min-w-full divide-y divide-gray-200">
            <thead class="bg-gray-50">
              <tr>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Mahsulot</th>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Miqdor</th>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Narx</th>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Bonus</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100">
              <tr v-for="(item, i) in detail.items || []" :key="i">
                <td class="px-5 py-3 text-theme-sm text-gray-700">{{ item.productName || item.productId }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700">{{ item.quantity ?? 0 }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(item.productCost) }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700">{{ item.isBonus ? 'Ha' : 'Yo‘q' }}</td>
              </tr>
              <tr v-if="!(detail.items && detail.items.length)">
                <td colspan="4" class="px-5 py-4 text-center text-theme-sm text-gray-500">
                  Mahsulotlar yo‘q
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </AppModal>
  </div>
</template>
