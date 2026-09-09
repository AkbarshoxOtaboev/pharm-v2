<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchOrder, updateOrderStatus } from '@/api/orders.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { OrderDetailResponse, OrderStatus } from '@/types/api'
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

const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.params.id))

const detail = ref<OrderDetailResponse | null>(null)
const loading = ref(true)
const error = ref('')
const newStatus = ref('')
const saving = ref(false)

async function load() {
  if (!orderId.value || Number.isNaN(orderId.value)) {
    error.value = 'Noto‘g‘ri buyurtma ID'
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await fetchOrder(orderId.value)
    detail.value = res.data
    newStatus.value = res.data?.status || ''
  } catch (e) {
    error.value = apiError(e, 'Buyurtmani yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
}

async function saveStatus() {
  if (!detail.value?.orderId || !newStatus.value) return
  saving.value = true
  error.value = ''
  try {
    await updateOrderStatus(detail.value.orderId, newStatus.value)
    await load()
  } catch (e) {
    error.value = apiError(e, 'Statusni yangilab bo‘lmadi')
  } finally {
    saving.value = false
  }
}

watch(orderId, load)
onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      :title="`Buyurtma #${detail?.orderNumber || orderId}`"
      subtitle="Buyurtma tafsiloti va status"
    >
      <template #actions>
        <AppButton variant="secondary" @click="router.push('/admin/orders')">Orqaga</AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="text-theme-sm text-gray-500">Yuklanmoqda...</div>

    <template v-else-if="detail">
      <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs">
        <div class="mb-4 flex flex-wrap items-center gap-3">
          <AppBadge :tone="statusTone(detail.status)">{{ detail.status || '—' }}</AppBadge>
          <span class="text-theme-sm text-gray-500">{{ detail.createdDate || '' }}</span>
        </div>
        <div class="grid grid-cols-1 gap-3 text-theme-sm text-gray-700 md:grid-cols-2 xl:grid-cols-3">
          <p><span class="text-gray-500">Mijoz:</span> {{ detail.fullName || '—' }}</p>
          <p><span class="text-gray-500">Telefon:</span> {{ detail.phone || '—' }}</p>
          <p><span class="text-gray-500">Summa:</span> {{ money(detail.totalAmount) }}</p>
          <p><span class="text-gray-500">Manzil:</span> {{ detail.address || '—' }}</p>
          <p><span class="text-gray-500">Uy:</span> {{ detail.house || '—' }}</p>
          <p><span class="text-gray-500">Podyezd:</span> {{ detail.entrance || '—' }}</p>
          <p><span class="text-gray-500">Xonadon:</span> {{ detail.apartment || '—' }}</p>
          <p><span class="text-gray-500">Qavat:</span> {{ detail.floor || '—' }}</p>
          <p><span class="text-gray-500">Mo‘ljal:</span> {{ detail.orientation || '—' }}</p>
          <p>
            <span class="text-gray-500">Kuryer:</span>
            {{ detail.courier?.fullName || '—' }}
          </p>
        </div>

        <div class="mt-5 flex flex-wrap items-end gap-2">
          <AppSelect v-model="newStatus" label="Statusni o‘zgartirish" class="min-w-56">
            <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ s }}</option>
          </AppSelect>
          <AppButton :loading="saving" @click="saveStatus">Yangilash</AppButton>
        </div>
      </div>

      <h2 class="mb-3 text-lg font-semibold text-gray-800">Mahsulotlar</h2>
      <DataTable
        :loading="false"
        :empty="!(detail.items && detail.items.length) ? 'Mahsulotlar yo‘q' : undefined"
      >
        <template #head>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Mahsulot</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Miqdor</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Narx</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Bonus</th>
        </template>
        <tr v-for="(item, i) in detail.items || []" :key="i">
          <td class="px-5 py-3 text-theme-sm text-gray-700">
            {{ item.productName || item.productId }}
          </td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ item.quantity ?? 0 }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(item.productCost) }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700">{{ item.isBonus ? 'Ha' : 'Yo‘q' }}</td>
        </tr>
      </DataTable>
    </template>
  </div>
</template>
