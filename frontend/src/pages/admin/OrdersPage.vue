<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Swal from 'sweetalert2'
import { enumLabel } from '@/i18n'
import {
  fetchOrder,
  fetchOrderComments,
  fetchOrders,
  updateOrderStatus,
  type OrderDeliveredInfoResponse,
  type OrderListParams,
} from '@/api/orders.api'
import { fetchCouriers } from '@/api/users.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { OrderDetailResponse, OrderResponse, OrderStatus, UserResponse } from '@/types/api'
import { apiError, money, statusSelectClass } from '@/utils/format'

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

const LOCKED_STATUSES: OrderStatus[] = ['DELIVERED', 'CANCELLED', 'DELETED']
const PAGE_SIZES = [50, 100, 150, 200] as const

function isStatusLocked(status?: string | null) {
  return LOCKED_STATUSES.includes((status || '').toUpperCase() as OrderStatus)
}

function toIsoDate(d: Date) {
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${y}-${m}-${day}`
}

function defaultDateRange() {
  const now = new Date()
  return {
    from: toIsoDate(new Date(now.getFullYear(), now.getMonth(), 1)),
    to: toIsoDate(now),
  }
}

const { t } = useI18n()
const orders = ref<OrderResponse[]>([])
const couriers = ref<UserResponse[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref('50')
const totalPages = ref(0)
const totalElements = ref(0)

const phone = ref('')
const status = ref('')
const courierId = ref('')
const dateDefaults = defaultDateRange()
const fromDate = ref(dateDefaults.from)
const toDate = ref(dateDefaults.to)

const detailOpen = ref(false)
const detailLoading = ref(false)
const detailError = ref('')
const detail = ref<OrderDetailResponse | null>(null)
const statusSaving = ref(false)
const newStatus = ref('')

const commentsOpen = ref(false)
const commentsLoading = ref(false)
const commentsError = ref('')
const comments = ref<OrderDeliveredInfoResponse[]>([])
const commentsOrderId = ref<number | null>(null)

let filterTimer: ReturnType<typeof setTimeout> | null = null

function fileUrl(name?: string | null) {
  if (!name || name === 'no_image.png') return ''
  if (name.startsWith('http') || name.startsWith('/')) return name
  return `/api/v1/files/${name}`
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const params: OrderListParams = {
      page: page.value,
      size: Number(size.value) || 50,
    }
    if (phone.value.trim()) params.phone = phone.value.trim()
    if (status.value) params.status = status.value
    if (courierId.value) params.userId = Number(courierId.value)
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

function scheduleLoad(resetPage = true) {
  if (filterTimer) clearTimeout(filterTimer)
  filterTimer = setTimeout(() => {
    if (resetPage) page.value = 0
    load()
  }, 350)
}

function resetFilters() {
  phone.value = ''
  status.value = ''
  courierId.value = ''
  const range = defaultDateRange()
  fromDate.value = range.from
  toDate.value = range.to
  size.value = '50'
  page.value = 0
  load()
}

watch([phone, status, courierId, fromDate, toDate], () => scheduleLoad(true))
watch(size, () => scheduleLoad(true))

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

async function openComments(id: number) {
  commentsOpen.value = true
  commentsOrderId.value = id
  commentsLoading.value = true
  commentsError.value = ''
  comments.value = []
  try {
    const res = await fetchOrderComments(id)
    comments.value = res.data || []
  } catch (e) {
    commentsError.value = apiError(e, 'errors.loadOrderDetail')
  } finally {
    commentsLoading.value = false
  }
}

async function saveStatus() {
  if (!detail.value?.orderId || !newStatus.value) return
  if (isStatusLocked(detail.value.status)) return
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

async function onRowStatusChange(order: OrderResponse, event: Event) {
  const select = event.target as HTMLSelectElement
  const next = select.value as OrderStatus
  const prev = (order.orderStatus || 'NEW') as OrderStatus

  if (isStatusLocked(prev)) {
    select.value = prev
    return
  }

  if (!next || next === prev) {
    select.value = prev
    return
  }

  const result = await Swal.fire({
    title: t('orders.confirmStatusTitle'),
    text: t('orders.confirmStatusText', {
      id: order.id,
      from: enumLabel('orderStatus', prev),
      to: enumLabel('orderStatus', next),
    }),
    icon: 'question',
    showCancelButton: true,
    confirmButtonText: t('common.save'),
    cancelButtonText: t('common.cancel'),
    confirmButtonColor: '#465fff',
    cancelButtonColor: '#98a2b3',
    reverseButtons: true,
  })

  if (!result.isConfirmed) {
    select.value = prev
    return
  }

  try {
    await updateOrderStatus(order.id, next)
    order.orderStatus = next
    await Swal.fire({
      icon: 'success',
      title: t('orders.statusUpdated'),
      timer: 1400,
      showConfirmButton: false,
    })
  } catch (e) {
    select.value = prev
    await Swal.fire({
      icon: 'error',
      title: apiError(e, 'errors.updateStatus'),
    })
  }
}

onMounted(async () => {
  try {
    const res = await fetchCouriers()
    couriers.value = res.data || []
  } catch {
    couriers.value = []
  }
  await load()
})
</script>

<template>
  <div>
    <PageHeader :title="t('orders.title')" :subtitle="t('orders.subtitle')">
      <template #actions>
        <AppButton @click="$router.push('/admin/orders/create')">{{ t('orders.newOrder') }}</AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4 2xl:grid-cols-7">
        <AppInput v-model="phone" :label="t('common.phone')" placeholder="+998..." />
        <AppSelect v-model="status" :label="t('common.status')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
        </AppSelect>
        <AppSelect v-model="courierId" :label="t('common.courier')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
        </AppSelect>
        <AppInput v-model="fromDate" :label="t('common.from')" type="date" />
        <AppInput v-model="toDate" :label="t('common.to')" type="date" />
        <AppSelect v-model="size" :label="t('common.limit')">
          <option v-for="n in PAGE_SIZES" :key="n" :value="String(n)">{{ n }}</option>
        </AppSelect>
        <div class="flex items-end">
          <button
            type="button"
            class="inline-flex h-11 w-11 items-center justify-center rounded-lg border border-gray-200 text-gray-600 transition hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
            :title="t('common.clear')"
            @click="resetFilters"
          >
            <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
              <path d="M3 6h18" stroke-linecap="round" />
              <path d="M8 6V4h8v2" stroke-linecap="round" />
              <path d="M19 6l-1 14H6L5 6" stroke-linecap="round" stroke-linejoin="round" />
              <path d="M10 11v6M14 11v6" stroke-linecap="round" />
            </svg>
          </button>
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
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.customer') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.phone') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.courier') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="o in orders" :key="o.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.id }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.customerName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.customerPhone || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <select
            class="h-9 min-w-36 rounded-lg border px-2.5 text-theme-sm font-medium shadow-theme-xs focus:outline-hidden focus:ring-3 disabled:cursor-not-allowed disabled:opacity-80"
            :class="statusSelectClass(o.orderStatus || 'NEW')"
            :value="o.orderStatus || 'NEW'"
            :disabled="isStatusLocked(o.orderStatus)"
            @change="onRowStatusChange(o, $event)"
          >
            <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
          </select>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(o.totalSum) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.courierName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <div class="flex items-center gap-1.5">
            <button
              type="button"
              class="inline-flex size-9 items-center justify-center rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
              :title="t('common.view')"
              @click="openDetail(o.id)"
            >
              <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7S2 12 2 12Z" />
                <circle cx="12" cy="12" r="3" />
              </svg>
            </button>
            <button
              type="button"
              class="inline-flex size-9 items-center justify-center rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
              :title="t('common.comment')"
              @click="openComments(o.id)"
            >
              <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M21 12a8 8 0 0 1-8 8H7l-4 3V12a8 8 0 1 1 18 0Z" stroke-linejoin="round" />
              </svg>
            </button>
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
        <div class="mb-4 grid grid-cols-1 gap-2 text-theme-sm text-gray-700 sm:grid-cols-2 dark:text-gray-300">
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

        <div v-if="!isStatusLocked(detail.status)" class="mb-4 flex flex-wrap items-end gap-2">
          <AppSelect v-model="newStatus" :label="t('common.status')" class="min-w-48 flex-1">
            <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
          </AppSelect>
          <AppButton :loading="statusSaving" @click="saveStatus">{{ t('common.save') }}</AppButton>
        </div>
        <p v-else class="mb-4 text-theme-sm text-gray-500 dark:text-gray-400">
          {{ t('common.status') }}: {{ enumLabel('orderStatus', detail.status) }}
        </p>

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

    <AppModal
      :open="commentsOpen"
      :title="`${t('common.comment')} #${commentsOrderId || ''}`"
      @close="commentsOpen = false"
    >
      <div v-if="commentsLoading" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>
      <div
        v-else-if="commentsError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
      >
        {{ commentsError }}
      </div>
      <div v-else-if="!comments.length" class="text-theme-sm text-gray-500 dark:text-gray-400">
        {{ t('common.empty') }}
      </div>
      <div v-else class="space-y-4">
        <div
          v-for="(c, idx) in comments"
          :key="idx"
          class="rounded-xl border border-gray-200 p-4 dark:border-gray-800"
        >
          <p class="text-theme-xs text-gray-400">{{ c.createdAt || t('common.empty') }}</p>
          <p class="mt-2 text-theme-sm text-gray-800 dark:text-gray-200">
            {{ c.comment || t('common.empty') }}
          </p>
          <img
            v-if="fileUrl(c.image)"
            :src="fileUrl(c.image)"
            :alt="t('common.photo')"
            class="mt-3 max-h-64 w-full rounded-lg object-contain bg-gray-50 dark:bg-white/[0.03]"
          />
        </div>
      </div>
    </AppModal>
  </div>
</template>
