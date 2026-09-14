<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
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

const { t } = useI18n()
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
    error.value = apiError(e, 'errors.loadOrders')
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
    detailError.value = apiError(e, 'errors.loadOrderDetail')
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
    detailError.value = apiError(e, 'errors.updateStatus')
  } finally {
    statusSaving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="t('orders.title')" :subtitle="t('orders.subtitle')">
      <template #actions>
        <AppButton @click="router.push('/admin/orders/create')">{{ t('orders.newOrder') }}</AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-4 shadow-theme-xs">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-5">
        <AppInput v-model="phone" :label="t('common.phone')" placeholder="+998..." />
        <AppSelect v-model="status" :label="t('common.status')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
        </AppSelect>
        <AppInput v-model="fromDate" :label="t('common.from')" type="date" />
        <AppInput v-model="toDate" :label="t('common.to')" type="date" />
        <div class="flex items-end gap-2">
          <AppButton class="w-full" @click="onSearch">{{ t('common.search') }}</AppButton>
          <AppButton variant="secondary" @click="resetFilters">{{ t('common.clear') }}</AppButton>
        </div>
      </div>
    </div>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <DataTable
      :loading="loading"
      :empty="!loading && !orders.length ? t('orders.notFound') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.id') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.number') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.customer') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.phone') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.courier') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="o in orders" :key="o.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.id }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.orderNumber || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.customerName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.customerPhone || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppBadge :tone="statusTone(o.orderStatus)">{{ enumLabel('orderStatus', o.orderStatus, t('common.empty')) }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(o.totalSum) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.courierName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="secondary" @click="openDetail(o.id)">{{ t('common.view') }}</AppButton>
            <AppButton size="sm" variant="ghost" @click="router.push(`/admin/orders/${o.id}`)">
              {{ t('common.page') }}
            </AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <div class="mt-4 flex flex-wrap items-center justify-between gap-3">
      <p class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.total') }}: {{ totalElements }}</p>
      <div class="flex items-center gap-2">
        <AppButton
          size="sm"
          variant="secondary"
          :disabled="page <= 0"
          @click="page--; load()"
        >
          {{ t('common.previous') }}
        </AppButton>
        <span class="text-theme-sm text-gray-600 dark:text-gray-400">{{ page + 1 }} / {{ totalPages || 1 }}</span>
        <AppButton
          size="sm"
          variant="secondary"
          :disabled="page + 1 >= totalPages"
          @click="page++; load()"
        >
          {{ t('common.next') }}
        </AppButton>
      </div>
    </div>

    <AppModal
      :open="detailOpen"
      :title="t('orders.detail')"
      @close="detailOpen = false"
    >
      <div v-if="detailLoading" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>
      <div
        v-else-if="detailError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ detailError }}
      </div>
      <template v-else-if="detail">
        <div class="mb-4 grid grid-cols-1 gap-2 text-theme-sm text-gray-700 sm:grid-cols-2">
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.id') }}:</span> {{ detail.orderId }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.number') }}:</span> {{ detail.orderNumber || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.customer') }}:</span> {{ detail.fullName || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.phone') }}:</span> {{ detail.phone || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.address') }}:</span> {{ detail.address || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.house') }}:</span> {{ detail.house || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.entrance') }}:</span> {{ detail.entrance || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.apartment') }}:</span> {{ detail.apartment || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.floor') }}:</span> {{ detail.floor || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.orientation') }}:</span> {{ detail.orientation || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.courier') }}:</span> {{ detail.courier?.fullName || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.amount') }}:</span> {{ money(detail.totalAmount) }}</p>
        </div>

        <div class="mb-4 flex flex-wrap items-end gap-2">
          <AppSelect v-model="newStatus" :label="t('common.status')" class="min-w-48 flex-1">
            <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
          </AppSelect>
          <AppButton :loading="statusSaving" @click="saveStatus">{{ t('common.save') }}</AppButton>
        </div>

        <h3 class="mb-2 text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ t('orders.products') }}</h3>
        <div class="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-800">
          <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-800">
            <thead class="bg-gray-50 dark:bg-white/[0.02]">
              <tr>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.bonus') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
              <tr v-for="(item, i) in detail.items || []" :key="i">
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ item.productName || item.productId }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ item.quantity ?? 0 }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(item.productCost) }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ item.isBonus ? t('common.yes') : t('common.no') }}</td>
              </tr>
              <tr v-if="!(detail.items && detail.items.length)">
                <td colspan="4" class="px-5 py-4 text-center text-theme-sm text-gray-500 dark:text-gray-400">
                  {{ t('orders.noProducts') }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </template>
    </AppModal>
  </div>
</template>
