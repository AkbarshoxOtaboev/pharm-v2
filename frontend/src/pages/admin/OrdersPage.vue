<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import Swal from 'sweetalert2'
import { enumLabel } from '@/i18n'
import {
  downloadOrdersExcel,
  fetchOrder,
  fetchOrderComments,
  fetchOrders,
  updateOrderStatus,
  type OrderDeliveredInfoResponse,
  type OrderListParams,
} from '@/api/orders.api'
import { fetchCouriers } from '@/api/users.api'
import { fetchDistricts, fetchRegions, type DistrictItem, type RegionItem } from '@/api/locations.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { OrderDetailResponse, OrderResponse, OrderStatus, UserResponse } from '@/types/api'
import { apiError, fileUrl, money, statusSelectClass } from '@/utils/format'
import { printOrderReceipts, type ReceiptLabels } from '@/utils/printReceipts'

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
const regionId = ref('')
const districtId = ref('')
const regions = ref<RegionItem[]>([])
const allDistricts = ref<DistrictItem[]>([])
const dateDefaults = defaultDateRange()
const fromDate = ref(dateDefaults.from)
const toDate = ref(dateDefaults.to)

const filteredDistricts = computed(() => {
  if (!regionId.value) return allDistricts.value
  const rid = Number(regionId.value)
  return allDistricts.value.filter((d) => d.regionId === rid)
})

const excelOpen = ref(false)
const excelRegionId = ref('')
const excelDate = ref(toIsoDate(new Date()))
const excelOrders = ref<OrderResponse[]>([])
const excelLoading = ref(false)
const excelError = ref('')
const exporting = ref(false)
let excelLoadSeq = 0
let excelFilterSilent = false

const canDownloadExcel = computed(
  () => Boolean(excelRegionId.value && excelDate.value && excelOrders.value.length && !excelLoading.value),
)

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

const printOpen = ref(false)
const printLoading = ref(false)
const printBusy = ref(false)
const printError = ref('')
const printCourierId = ref('')
const printStatus = ref('')
const printDistrictId = ref('')
const printFromDate = ref(dateDefaults.from)
const printToDate = ref(dateDefaults.to)
const printOrders = ref<OrderResponse[]>([])
const printSelected = ref<number[]>([])
const printingId = ref<number | null>(null)

let filterTimer: ReturnType<typeof setTimeout> | null = null
let printLoadSeq = 0
let printFilterSilent = false

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
    if (regionId.value) params.regionId = Number(regionId.value)
    if (districtId.value) params.districtId = Number(districtId.value)
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
  regionId.value = ''
  districtId.value = ''
  const range = defaultDateRange()
  fromDate.value = range.from
  toDate.value = range.to
  size.value = '50'
  page.value = 0
  load()
}

watch([phone, status, courierId, regionId, districtId, fromDate, toDate], () => scheduleLoad(true))
watch(size, () => scheduleLoad(true))
watch(regionId, () => {
  if (districtId.value && !filteredDistricts.value.some((d) => String(d.id) === districtId.value)) {
    districtId.value = ''
  }
})

async function loadExcelOrders() {
  if (!excelRegionId.value || !excelDate.value) {
    excelOrders.value = []
    return
  }
  const seq = ++excelLoadSeq
  excelLoading.value = true
  excelError.value = ''
  try {
    const res = await fetchOrders({
      page: 0,
      size: -1,
      regionId: Number(excelRegionId.value),
      fromDate: excelDate.value,
      toDate: excelDate.value,
    })
    if (seq !== excelLoadSeq) return
    excelOrders.value = res.data || []
  } catch (e) {
    if (seq !== excelLoadSeq) return
    excelError.value = apiError(e, 'errors.loadOrders')
    excelOrders.value = []
  } finally {
    if (seq === excelLoadSeq) excelLoading.value = false
  }
}

function openExcelModal() {
  excelFilterSilent = true
  excelError.value = ''
  excelOrders.value = []
  excelRegionId.value = regionId.value
  excelDate.value = toDate.value || toIsoDate(new Date())
  excelOpen.value = true
  void loadExcelOrders()
  void nextTick(() => {
    excelFilterSilent = false
  })
}

watch([excelRegionId, excelDate], () => {
  if (excelOpen.value && !excelFilterSilent) void loadExcelOrders()
})

async function exportExcel() {
  if (!canDownloadExcel.value) return
  exporting.value = true
  excelError.value = ''
  try {
    await downloadOrdersExcel({
      regionId: Number(excelRegionId.value),
      date: excelDate.value,
    })
    excelOpen.value = false
  } catch (e) {
    excelError.value = apiError(e, 'errors.exportFailed')
  } finally {
    exporting.value = false
  }
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

function receiptLabels(): ReceiptLabels {
  return {
    title: t('orders.receipt'),
    number: t('common.number'),
    date: t('common.date'),
    customer: t('common.customer'),
    phone: t('common.phone'),
    address: t('common.address'),
    place: t('locations.place'),
    house: t('common.house'),
    entrance: t('common.entrance'),
    apartment: t('common.apartment'),
    floor: t('common.floor'),
    orientation: t('common.orientation'),
    courier: t('common.courier'),
    product: t('common.product'),
    quantity: t('common.quantity'),
    price: t('common.price'),
    amount: t('common.amount'),
    bonus: t('common.bonus'),
    total: t('common.total'),
    empty: t('common.empty'),
  }
}

async function detailsOf(ids: number[]) {
  const details = []
  for (let i = 0; i < ids.length; i += 8) {
    const chunk = await Promise.all(
      ids.slice(i, i + 8).map(async (id) => {
        const res = await fetchOrder(id)
        return res.data
      }),
    )
    for (const row of chunk) {
      if (row) details.push(row)
    }
  }
  return details
}

async function printIds(ids: number[]) {
  if (!ids.length) {
    printError.value = t('orders.printEmpty')
    return false
  }
  const details = await detailsOf(ids)
  const opened = printOrderReceipts(details, receiptLabels())
  if (!opened) printError.value = t('orders.printFailed')
  return opened
}

async function printOne(id: number) {
  printingId.value = id
  error.value = ''
  try {
    const opened = await printIds([id])
    if (!opened) error.value = printError.value
  } catch (e) {
    error.value = apiError(e, 'errors.loadOrderDetail')
  } finally {
    printingId.value = null
  }
}

async function loadPrintOrders() {
  const seq = ++printLoadSeq
  printLoading.value = true
  printError.value = ''
  try {
    const params: OrderListParams = { page: 0, size: -1 }
    if (printCourierId.value) params.userId = Number(printCourierId.value)
    if (printStatus.value) params.status = printStatus.value as OrderStatus
    if (printDistrictId.value) params.districtId = Number(printDistrictId.value)
    if (printFromDate.value) params.fromDate = printFromDate.value
    if (printToDate.value) params.toDate = printToDate.value
    const res = await fetchOrders(params)
    if (seq !== printLoadSeq) return
    printOrders.value = res.data || []
    printSelected.value = printSelected.value.filter((id) => printOrders.value.some((o) => o.id === id))
  } catch (e) {
    if (seq !== printLoadSeq) return
    printError.value = apiError(e, 'errors.loadOrders')
    printOrders.value = []
  } finally {
    if (seq === printLoadSeq) printLoading.value = false
  }
}

function openPrintModal() {
  printSelected.value = []
  printError.value = ''
  printFilterSilent = true
  printCourierId.value = courierId.value
  printStatus.value = status.value
  printDistrictId.value = districtId.value
  printFromDate.value = fromDate.value
  printToDate.value = toDate.value
  printOpen.value = true
  void loadPrintOrders()
  void nextTick(() => {
    printFilterSilent = false
  })
}

function togglePrinted(id: number) {
  printSelected.value = printSelected.value.includes(id)
    ? printSelected.value.filter((item) => item !== id)
    : [...printSelected.value, id]
}

function togglePrintAll() {
  const ids = printOrders.value.map((o) => o.id)
  const allOn = ids.length > 0 && ids.every((id) => printSelected.value.includes(id))
  printSelected.value = allOn ? [] : ids
}

async function printChosen() {
  if (!printSelected.value.length) {
    printError.value = t('orders.printEmpty')
    return
  }
  printBusy.value = true
  printError.value = ''
  try {
    const opened = await printIds(printSelected.value)
    if (opened) printOpen.value = false
  } catch (e) {
    printError.value = apiError(e, 'errors.loadOrderDetail')
  } finally {
    printBusy.value = false
  }
}

watch([printCourierId, printStatus, printDistrictId, printFromDate, printToDate], () => {
  if (printOpen.value && !printFilterSilent) void loadPrintOrders()
})

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
    const [courierRes, regionRes, districtRes] = await Promise.all([
      fetchCouriers(),
      fetchRegions(),
      fetchDistricts(),
    ])
    couriers.value = courierRes.data || []
    regions.value = regionRes.data || []
    allDistricts.value = districtRes.data || []
  } catch {
    couriers.value = []
    regions.value = []
    allDistricts.value = []
  }
  await load()
})
</script>

<template>
  <div>
    <PageHeader :title="t('orders.title')" :subtitle="t('orders.subtitle')">
      <template #actions>
        <AppButton variant="success" @click="openExcelModal">
          <AppIcon name="download" class="size-4.5" />
          {{ t('orders.exportExcel') }}
        </AppButton>
        <AppButton variant="secondary" @click="openPrintModal">{{ t('orders.printChecks') }}</AppButton>
        <AppButton @click="$router.push('/admin/orders/create')">{{ t('orders.newOrder') }}</AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-2 xl:grid-cols-4 2xl:grid-cols-8">
        <AppInput v-model="phone" :label="t('common.phone')" placeholder="+998..." />
        <AppSelect v-model="status" :label="t('common.status')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
        </AppSelect>
        <AppSelect v-model="courierId" :label="t('common.courier')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
        </AppSelect>
        <AppSelect v-model="regionId" :label="t('locations.region')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="r in regions" :key="r.id" :value="String(r.id)">{{ r.name }}</option>
        </AppSelect>
        <AppSelect v-model="districtId" :label="t('locations.place')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="d in filteredDistricts" :key="d.id" :value="String(d.id)">
            {{ d.regionName ? `${d.regionName} — ${d.name}` : d.name }}
          </option>
        </AppSelect>
        <AppInput v-model="fromDate" :label="t('common.from')" type="date" />
        <AppInput v-model="toDate" :label="t('common.to')" type="date" />
        <div class="flex items-end gap-2">
          <AppSelect v-model="size" :label="t('common.limit')" class="min-w-0 flex-1">
            <option v-for="n in PAGE_SIZES" :key="n" :value="String(n)">{{ n }}</option>
          </AppSelect>
          <button
            type="button"
            class="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-lg border border-gray-200 text-gray-600 transition hover:bg-gray-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
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
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('locations.place') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.courier') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="o in orders" :key="o.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.id }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.customerName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.customerPhone || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ o.districtName || t('common.empty') }}</td>
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
              class="inline-flex size-9 items-center justify-center rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-50 dark:border-gray-700 dark:text-gray-300 dark:hover:bg-white/5"
              :title="t('orders.printOne')"
              :disabled="printingId === o.id"
              @click="printOne(o.id)"
            >
              <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8">
                <path d="M6 9V3h12v6" stroke-linejoin="round" />
                <path d="M6 18H4.5A1.5 1.5 0 0 1 3 16.5v-5A1.5 1.5 0 0 1 4.5 10h15a1.5 1.5 0 0 1 1.5 1.5v5a1.5 1.5 0 0 1-1.5 1.5H18" stroke-linejoin="round" />
                <path d="M6 14h12v7H6v-7Z" stroke-linejoin="round" />
              </svg>
            </button>
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
          <p>
            <span class="text-gray-500 dark:text-gray-400">{{ t('locations.place') }}:</span>
            {{ detail.districtName || t('common.empty') }}
          </p>
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
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
                <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.bonus') }}</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-gray-100 dark:divide-gray-800">
              <tr v-for="(item, i) in detail.items || []" :key="i">
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ item.productName || item.productId }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ item.quantity ?? 0 }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(item.price) }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(item.totalSum) }}</td>
                <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ item.isBonus ? t('common.yes') : t('common.no') }}</td>
              </tr>
              <tr v-if="!(detail.items && detail.items.length)">
                <td colspan="5" class="px-5 py-4 text-center text-theme-sm text-gray-500 dark:text-gray-400">
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

    <AppModal :open="excelOpen" :title="t('orders.exportExcel')" @close="excelOpen = false">
      <p class="mb-4 text-theme-sm text-gray-500 dark:text-gray-400">{{ t('orders.exportHint') }}</p>
      <div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
        <AppSelect v-model="excelRegionId" :label="t('locations.region')">
          <option value="">{{ t('orders.exportPickRegion') }}</option>
          <option v-for="r in regions" :key="r.id" :value="String(r.id)">{{ r.name }}</option>
        </AppSelect>
        <AppInput v-model="excelDate" :label="t('orders.exportDate')" type="date" />
      </div>

      <div
        v-if="excelError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ excelError }}
      </div>

      <div v-if="!excelRegionId || !excelDate" class="text-theme-sm text-gray-500 dark:text-gray-400">
        {{ t('orders.exportNeedFilters') }}
      </div>
      <div v-else-if="excelLoading" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>
      <p v-else-if="!excelOrders.length" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('orders.exportNone') }}</p>
      <div v-else class="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-800">
        <div class="flex items-center justify-between border-b border-gray-200 px-4 py-3 text-theme-sm font-medium text-gray-700 dark:border-gray-800 dark:text-gray-300">
          <span>{{ t('orders.exportOrdersTitle') }}</span>
          <span class="text-theme-xs font-normal text-gray-500">{{ t('orders.exportOrdersCount', { count: excelOrders.length }) }}</span>
        </div>
        <ul class="max-h-80 divide-y divide-gray-100 overflow-y-auto dark:divide-gray-800">
          <li
            v-for="o in excelOrders"
            :key="o.id"
            class="flex items-start gap-3 px-4 py-3 text-theme-sm text-gray-700 dark:text-gray-300"
          >
            <span class="min-w-0 flex-1">
              <span class="block font-medium text-gray-800 dark:text-white/90">#{{ o.id }} · {{ o.customerName || t('common.empty') }}</span>
              <span class="block text-theme-xs text-gray-500">
                {{ enumLabel('orderStatus', o.orderStatus) }}
                · {{ o.customerPhone || t('common.empty') }}
                · {{ o.districtName || t('common.empty') }}
                · {{ o.courierName || t('common.empty') }}
              </span>
            </span>
            <span class="shrink-0">{{ money(o.totalSum) }}</span>
          </li>
        </ul>
      </div>

      <div class="mt-4 flex justify-end gap-2">
        <AppButton variant="secondary" @click="excelOpen = false">{{ t('common.cancel') }}</AppButton>
        <AppButton
          variant="success"
          :loading="exporting"
          :disabled="!canDownloadExcel"
          @click="exportExcel"
        >
          <AppIcon v-if="!exporting" name="download" class="size-4.5" />
          {{ t('orders.exportExcel') }}
        </AppButton>
      </div>
    </AppModal>

    <AppModal :open="printOpen" :title="t('orders.printChecks')" @close="printOpen = false">
      <p class="mb-4 text-theme-sm text-gray-500 dark:text-gray-400">{{ t('orders.printHint') }}</p>
      <div class="mb-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
        <AppSelect v-model="printCourierId" :label="t('common.courier')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
        </AppSelect>
        <AppSelect v-model="printStatus" :label="t('common.status')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
        </AppSelect>
        <AppSelect v-model="printDistrictId" :label="t('locations.place')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="d in allDistricts" :key="d.id" :value="String(d.id)">
            {{ d.regionName ? `${d.regionName} — ${d.name}` : d.name }}
          </option>
        </AppSelect>
        <div class="grid grid-cols-2 gap-3">
          <AppInput v-model="printFromDate" :label="t('common.from')" type="date" />
          <AppInput v-model="printToDate" :label="t('common.to')" type="date" />
        </div>
      </div>

      <div
        v-if="printError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ printError }}
      </div>
      <div v-if="printLoading" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>
      <p v-else-if="!printOrders.length" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('orders.printNone') }}</p>
      <div v-else class="overflow-hidden rounded-xl border border-gray-200 dark:border-gray-800">
        <label class="flex items-center gap-3 border-b border-gray-200 px-4 py-3 text-theme-sm font-medium text-gray-700 dark:border-gray-800 dark:text-gray-300">
          <input
            type="checkbox"
            class="size-4 accent-brand-500"
            :checked="printOrders.every((o) => printSelected.includes(o.id))"
            @change="togglePrintAll"
          />
          {{ t('orders.selectAll') }}
          <span class="ml-auto text-theme-xs font-normal text-gray-500">{{ t('orders.selectedCount', { count: printSelected.length }) }}</span>
        </label>
        <ul class="max-h-80 divide-y divide-gray-100 overflow-y-auto dark:divide-gray-800">
          <li v-for="o in printOrders" :key="o.id">
            <label class="flex cursor-pointer items-start gap-3 px-4 py-3 text-theme-sm text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-white/5">
              <input
                type="checkbox"
                class="mt-0.5 size-4 accent-brand-500"
                :checked="printSelected.includes(o.id)"
                @change="togglePrinted(o.id)"
              />
              <span class="min-w-0 flex-1">
                <span class="block font-medium text-gray-800 dark:text-white/90">#{{ o.id }} · {{ o.customerName || t('common.empty') }}</span>
                <span class="block text-theme-xs text-gray-500">
                  {{ enumLabel('orderStatus', o.orderStatus) }}
                  · {{ o.customerPhone || t('common.empty') }}
                  · {{ o.districtName || t('common.empty') }}
                  · {{ o.courierName || t('common.empty') }}
                </span>
              </span>
              <span class="shrink-0">{{ money(o.totalSum) }}</span>
            </label>
          </li>
        </ul>
      </div>

      <div class="mt-4 flex justify-end gap-2">
        <AppButton variant="secondary" @click="printOpen = false">{{ t('common.cancel') }}</AppButton>
        <AppButton :loading="printBusy" :disabled="!printSelected.length" @click="printChosen">
          {{ t('orders.printChecks') }}
        </AppButton>
      </div>
    </AppModal>
  </div>
</template>
