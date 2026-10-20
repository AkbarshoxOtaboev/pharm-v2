<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { fetchCourierStock, returnStock, transferStock } from '@/api/courierStock.api'
import { fetchProductsInStock } from '@/api/products.api'
import { fetchCouriers } from '@/api/users.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import StatCard from '@/components/ui/StatCard.vue'
import type { CourierStockResponse, ProductResponse, UserResponse } from '@/types/api'
import { apiError, money } from '@/utils/format'

const { t } = useI18n()
const couriers = ref<UserResponse[]>([])
const products = ref<ProductResponse[]>([])
const courierId = ref('')
const stock = ref<CourierStockResponse[]>([])
const loading = ref(false)
const error = ref('')
const message = ref('')

const transferOpen = ref(false)
const transferError = ref('')
const transferSaving = ref(false)
const transferForm = reactive({ productId: '', quantity: '' })

const returnOpen = ref(false)
const returnError = ref('')
const returnSaving = ref(false)
const returnForm = reactive({ productId: '', quantity: '' })

const courierName = computed(
  () => couriers.value.find((c) => String(c.id) === courierId.value)?.fullName || '',
)
const returnableStock = computed(() => stock.value.filter((s) => Number(s.quantity) > 0))

function rowTotal(s: CourierStockResponse) {
  return Number(s.quantity ?? 0) + Number(s.inOrders ?? 0)
}

const totals = computed(() => {
  let types = 0
  let free = 0
  let inOrders = 0
  let sum = 0
  let freeSum = 0
  for (const s of stock.value) {
    const total = rowTotal(s)
    if (total > 0) types++
    free += Number(s.quantity ?? 0)
    inOrders += Number(s.inOrders ?? 0)
    sum += total * Number(s.productPrice ?? 0)
    freeSum += Number(s.quantity ?? 0) * Number(s.productPrice ?? 0)
  }
  return { types, free, inOrders, qty: free + inOrders, sum, freeSum, inOrdersSum: sum - freeSum }
})

const transferProduct = computed(() =>
  products.value.find((p) => String(p.id) === transferForm.productId),
)
const transferOnCourier = computed(() =>
  Number(stock.value.find((s) => String(s.productId) === transferForm.productId)?.quantity ?? 0),
)
const transferExceeds = computed(
  () =>
    !!transferProduct.value &&
    Number(transferForm.quantity) > Number(transferProduct.value.storeQuantity ?? 0),
)

const returnItem = computed(() =>
  returnableStock.value.find((s) => String(s.productId) === returnForm.productId),
)
const returnExceeds = computed(
  () => !!returnItem.value && Number(returnForm.quantity) > Number(returnItem.value.quantity ?? 0),
)

async function loadCouriersAndProducts() {
  const [cRes, pRes] = await Promise.all([fetchCouriers(), fetchProductsInStock()])
  couriers.value = cRes.data || []
  products.value = pRes.data || []
  if (!courierId.value && couriers.value.length) {
    courierId.value = String(couriers.value[0].id)
  }
}

async function loadProductsInStock() {
  try {
    const pRes = await fetchProductsInStock()
    products.value = pRes.data || []
  } catch {
    /* keep current list */
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
    error.value = apiError(e, 'errors.loadStock')
  } finally {
    loading.value = false
  }
}

async function reloadAll() {
  await Promise.all([loadStock(), loadProductsInStock()])
}

watch(courierId, () => {
  message.value = ''
  void loadStock()
})

function openTransfer() {
  transferForm.productId = ''
  transferForm.quantity = ''
  transferError.value = ''
  transferOpen.value = true
}

function openReturn() {
  returnForm.productId = ''
  returnForm.quantity = ''
  returnError.value = ''
  returnOpen.value = true
}

async function doTransfer() {
  transferError.value = ''
  if (!courierId.value || !transferForm.productId || !transferForm.quantity || Number(transferForm.quantity) <= 0) {
    transferError.value = t('errors.courierProductQtyRequired')
    return
  }
  if (transferExceeds.value) {
    transferError.value = t('errors.insufficientStock')
    return
  }
  transferSaving.value = true
  try {
    await transferStock({
      courierId: Number(courierId.value),
      items: [{ productId: Number(transferForm.productId), quantity: Number(transferForm.quantity) }],
    })
    transferOpen.value = false
    message.value = t('courierStock.transferOk')
    await reloadAll()
  } catch (e) {
    transferError.value = apiError(e, 'errors.transferFailed')
  } finally {
    transferSaving.value = false
  }
}

async function doReturn() {
  returnError.value = ''
  if (!courierId.value || !returnForm.productId || !returnForm.quantity || Number(returnForm.quantity) <= 0) {
    returnError.value = t('errors.courierProductQtyRequired')
    return
  }
  if (returnExceeds.value) {
    returnError.value = t('errors.insufficientStock')
    return
  }
  returnSaving.value = true
  try {
    await returnStock({
      courierId: Number(courierId.value),
      productId: Number(returnForm.productId),
      quantity: Number(returnForm.quantity),
    })
    returnOpen.value = false
    message.value = t('courierStock.returnOk')
    await reloadAll()
  } catch (e) {
    returnError.value = apiError(e, 'errors.returnFailed')
  } finally {
    returnSaving.value = false
  }
}

onMounted(async () => {
  try {
    await loadCouriersAndProducts()
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  }
})
</script>

<template>
  <div>
    <PageHeader :title="t('courierStock.title')" :subtitle="t('courierStock.subtitle')">
      <template #actions>
        <AppButton :disabled="!courierId" @click="openTransfer">
          <AppIcon name="send" class="size-4.5" />
          {{ t('courierStock.giveToCourier') }}
        </AppButton>
        <button
          type="button"
          class="inline-flex items-center justify-center gap-2 rounded-lg border border-warning-500 px-4 py-2.5 text-theme-sm font-medium text-warning-500 transition hover:bg-warning-500 hover:text-white disabled:cursor-not-allowed disabled:opacity-60 disabled:hover:bg-transparent disabled:hover:text-warning-500"
          :disabled="!courierId || !returnableStock.length"
          @click="openReturn"
        >
          <AppIcon name="undo" class="size-4.5" />
          {{ t('courierStock.returnToStore') }}
        </button>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-4">
        <AppSelect v-model="courierId" icon="user" :label="t('common.courier')" class="min-w-64 flex-1 sm:flex-none">
          <option value="">{{ t('common.select') }}</option>
          <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
        </AppSelect>
        <RouterLink
          to="/admin/courier-transfers"
          class="ml-auto inline-flex items-center gap-2 rounded-lg px-3 py-2.5 text-theme-sm font-medium text-brand-500 hover:bg-brand-50 dark:hover:bg-brand-500/10"
        >
          <AppIcon name="history" class="size-4.5" />
          {{ t('courierTransfers.title') }}
        </RouterLink>
      </div>
    </div>

    <div
      v-if="error"
      class="mb-4 flex items-center gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      <AppIcon name="alert" class="size-4.5" />
      {{ error }}
    </div>
    <div
      v-if="message"
      class="mb-4 flex items-center gap-2 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700 dark:border-success-500/20 dark:bg-success-500/10"
    >
      <AppIcon name="check" class="size-4.5" />
      {{ message }}
    </div>

    <div v-if="courierId" class="mb-6 grid grid-cols-1 gap-4 sm:grid-cols-3">
      <StatCard
        :label="t('courierStock.productTypes')"
        :value="totals.types"
        :hint="t('courierStock.productTypesHint')"
        tone="purple"
        icon="layers"
      />
      <StatCard
        :label="t('courierStock.productCount')"
        :value="money(totals.qty)"
        :hint="t('courierStock.splitHint', { free: money(totals.free), orders: money(totals.inOrders) })"
        tone="brand"
        icon="orders"
      />
      <StatCard
        :label="t('courierStock.totalSum')"
        :value="money(totals.sum)"
        :hint="t('courierStock.splitHint', { free: money(totals.freeSum), orders: money(totals.inOrdersSum) })"
        tone="success"
        icon="cash"
      />
    </div>

    <h2 class="mb-3 flex items-center gap-2 text-lg font-semibold text-gray-800 dark:text-white/90">
      <AppIcon name="truck" class="text-brand-500" />
      {{ t('courierStock.currentStock') }}
      <span v-if="courierName" class="text-theme-sm font-normal text-gray-500 dark:text-gray-400">— {{ courierName }}</span>
    </h2>
    <DataTable
      :loading="loading"
      :empty="!loading && !stock.length ? t('courierStock.empty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.onCourier') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.inOrders') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.total') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.unit') }}</th>
      </template>
      <tr v-for="s in stock" :key="s.id" :class="{ 'opacity-50': rowTotal(s) <= 0 }">
        <td class="px-5 py-3 text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ s.productName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-right text-theme-sm" :class="Number(s.quantity) > 0 ? 'font-semibold text-gray-800 dark:text-white/90' : 'text-gray-400'">
          {{ money(s.quantity) }}
        </td>
        <td class="px-5 py-3 text-right text-theme-sm">
          <span
            v-if="Number(s.inOrders) > 0"
            class="inline-flex items-center gap-1.5 whitespace-nowrap rounded-full bg-warning-50 px-2.5 py-0.5 text-theme-xs font-semibold text-warning-600 dark:bg-warning-500/15 dark:text-warning-500"
            :title="t('courierStock.inOrdersTitle', { count: s.inOrdersCount ?? 0 })"
          >
            <AppIcon name="receipt" class="size-3.5" />
            {{ money(s.inOrders) }}
            <span class="font-normal opacity-80">· {{ t('courierStock.ordersShort', { count: s.inOrdersCount ?? 0 }) }}</span>
          </span>
          <span v-else class="text-gray-400">0</span>
        </td>
        <td class="px-5 py-3 text-right text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ money(rowTotal(s)) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ money(s.productPrice) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">
          {{ money(rowTotal(s) * Number(s.productPrice ?? 0)) }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ enumLabel('unitType', s.unitType, t('common.empty')) }}</td>
      </tr>
    </DataTable>

    <AppModal :open="transferOpen" @close="transferOpen = false">
      <template #title>
        <span class="flex size-10 items-center justify-center rounded-xl bg-brand-50 text-brand-500 dark:bg-brand-500/15 dark:text-brand-400">
          <AppIcon name="send" />
        </span>
        <span>
          {{ t('courierStock.giveToCourier') }}
          <span class="flex items-center gap-1 text-theme-xs font-normal text-gray-500 dark:text-gray-400">
            <AppIcon name="user" class="size-3.5" />{{ courierName }}
          </span>
        </span>
      </template>

      <div
        v-if="transferError"
        class="mb-4 flex items-center gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        <AppIcon name="alert" class="size-4.5" />
        {{ transferError }}
      </div>

      <form class="space-y-4" @submit.prevent="doTransfer">
        <AppSelect v-model="transferForm.productId" icon="box" :label="t('common.product')">
          <option value="">{{ t('common.select') }}</option>
          <option v-for="p in products" :key="p.id" :value="String(p.id)">
            {{ p.name }} ({{ t('courierStock.storeQty', { qty: p.storeQuantity ?? t('common.empty') }) }})
          </option>
        </AppSelect>

        <div v-if="transferProduct" class="grid grid-cols-2 gap-2">
          <div class="flex items-center gap-2.5 rounded-xl border border-gray-200 px-3 py-2.5 dark:border-gray-800">
            <AppIcon name="warehouse" class="text-gray-400" />
            <div>
              <p class="text-theme-xs text-gray-500 dark:text-gray-400">{{ t('courierStock.inStore') }}</p>
              <p class="text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ money(transferProduct.storeQuantity) }}</p>
            </div>
          </div>
          <div class="flex items-center gap-2.5 rounded-xl border border-gray-200 px-3 py-2.5 dark:border-gray-800">
            <AppIcon name="truck" class="text-gray-400" />
            <div>
              <p class="text-theme-xs text-gray-500 dark:text-gray-400">{{ t('courierStock.onCourier') }}</p>
              <p class="text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ money(transferOnCourier) }}</p>
            </div>
          </div>
        </div>

        <AppInput
          v-model="transferForm.quantity"
          icon="hash"
          type="number"
          :label="t('common.quantity')"
          :suffix="transferProduct ? enumLabel('unitType', transferProduct.unitType, '') : ''"
          :error="transferExceeds ? t('errors.insufficientStock') : undefined"
        />

        <p
          v-if="transferProduct && Number(transferForm.quantity) > 0 && !transferExceeds"
          class="flex items-center gap-2 rounded-xl bg-brand-50 px-3 py-2.5 text-theme-sm text-brand-600 dark:bg-brand-500/10 dark:text-brand-400"
        >
          <AppIcon name="arrow-up-right" class="size-4.5" />
          {{ t('courierStock.afterTransfer', {
            store: money(Number(transferProduct.storeQuantity ?? 0) - Number(transferForm.quantity)),
            courier: money(transferOnCourier + Number(transferForm.quantity)),
          }) }}
        </p>

        <div class="flex justify-end gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
          <AppButton type="button" variant="secondary" @click="transferOpen = false">
            <AppIcon name="x" class="size-4.5" />
            {{ t('common.cancel') }}
          </AppButton>
          <AppButton type="submit" :loading="transferSaving" :disabled="transferExceeds">
            <AppIcon v-if="!transferSaving" name="send" class="size-4.5" />
            {{ t('courierStock.give') }}
          </AppButton>
        </div>
      </form>
    </AppModal>

    <AppModal :open="returnOpen" @close="returnOpen = false">
      <template #title>
        <span class="flex size-10 items-center justify-center rounded-xl bg-warning-50 text-warning-500 dark:bg-warning-500/15">
          <AppIcon name="undo" />
        </span>
        <span>
          {{ t('courierStock.returnToStore') }}
          <span class="flex items-center gap-1 text-theme-xs font-normal text-gray-500 dark:text-gray-400">
            <AppIcon name="user" class="size-3.5" />{{ courierName }}
          </span>
        </span>
      </template>

      <div
        v-if="returnError"
        class="mb-4 flex items-center gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        <AppIcon name="alert" class="size-4.5" />
        {{ returnError }}
      </div>

      <form class="space-y-4" @submit.prevent="doReturn">
        <AppSelect v-model="returnForm.productId" icon="box" :label="t('courierStock.productFromStock')">
          <option value="">{{ t('common.select') }}</option>
          <option v-for="s in returnableStock" :key="s.id" :value="String(s.productId)">
            {{ s.productName }} ({{ money(s.quantity) }})
          </option>
        </AppSelect>

        <div class="space-y-1.5">
          <AppInput
            v-model="returnForm.quantity"
            icon="hash"
            type="number"
            :label="t('common.quantity')"
            :suffix="returnItem ? enumLabel('unitType', returnItem.unitType, '') : ''"
            :error="returnExceeds ? t('errors.insufficientStock') : undefined"
          />
          <div v-if="returnItem" class="flex items-center justify-between text-theme-xs text-gray-500 dark:text-gray-400">
            <span class="flex items-center gap-1">
              <AppIcon name="truck" class="size-3.5" />
              {{ t('courierStock.onCourier') }}: {{ money(returnItem.quantity) }}
            </span>
            <button
              type="button"
              class="font-medium text-warning-500 hover:underline"
              @click="returnForm.quantity = String(returnItem.quantity ?? '')"
            >
              {{ t('courierStock.returnAll') }}
            </button>
          </div>
        </div>

        <p
          v-if="returnItem && Number(returnForm.quantity) > 0 && !returnExceeds"
          class="flex items-center gap-2 rounded-xl bg-warning-50 px-3 py-2.5 text-theme-sm text-warning-600 dark:bg-warning-500/10 dark:text-warning-500"
        >
          <AppIcon name="arrow-down-left" class="size-4.5" />
          {{ t('courierStock.afterReturn', { qty: money(Number(returnItem.quantity ?? 0) - Number(returnForm.quantity)) }) }}
        </p>

        <div class="flex justify-end gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
          <AppButton type="button" variant="secondary" @click="returnOpen = false">
            <AppIcon name="x" class="size-4.5" />
            {{ t('common.cancel') }}
          </AppButton>
          <button
            type="submit"
            class="inline-flex items-center justify-center gap-2 rounded-lg bg-warning-500 px-4 py-2.5 text-theme-sm font-medium text-white transition hover:bg-warning-600 disabled:cursor-not-allowed disabled:opacity-60"
            :disabled="returnSaving || returnExceeds"
          >
            <span v-if="returnSaving" class="size-4 animate-spin rounded-full border-2 border-white/40 border-t-white" />
            <AppIcon v-else name="undo" class="size-4.5" />
            {{ t('courierStock.return') }}
          </button>
        </div>
      </form>
    </AppModal>
  </div>
</template>
