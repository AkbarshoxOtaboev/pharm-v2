<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
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

const { t } = useI18n()
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
const returnableStock = computed(() => stock.value.filter((s) => Number(s.quantity) > 0))

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
    error.value = apiError(e, 'errors.loadHistory')
  } finally {
    histLoading.value = false
  }
}

async function reloadAll() {
  await Promise.all([loadStock(), loadHistory(), loadProductsInStock()])
}

watch(courierId, () => {
  reloadAll()
})

async function doTransfer() {
  message.value = ''
  error.value = ''
  if (!courierId.value || !transferForm.productId || !transferForm.quantity) {
    error.value = t('errors.courierProductQtyRequired')
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
    message.value = t('courierStock.transferOk')
    transferForm.productId = ''
    transferForm.quantity = ''
    await reloadAll()
  } catch (e) {
    error.value = apiError(e, 'errors.transferFailed')
  } finally {
    transferSaving.value = false
  }
}

async function doReturn() {
  message.value = ''
  error.value = ''
  if (!courierId.value || !returnForm.productId || !returnForm.quantity) {
    error.value = t('errors.courierProductQtyRequired')
    return
  }
  returnSaving.value = true
  try {
    await returnStock({
      courierId: Number(courierId.value),
      productId: Number(returnForm.productId),
      quantity: Number(returnForm.quantity),
    })
    message.value = t('courierStock.returnOk')
    returnForm.productId = ''
    returnForm.quantity = ''
    await reloadAll()
  } catch (e) {
    error.value = apiError(e, 'errors.returnFailed')
  } finally {
    returnSaving.value = false
  }
}

onMounted(async () => {
  try {
    await loadCouriersAndProducts()
    await reloadAll()
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  }
})
</script>

<template>
  <div>
    <PageHeader :title="t('courierStock.title')" :subtitle="t('courierStock.subtitle')" />

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-4 shadow-theme-xs">
      <AppSelect v-model="courierId" :label="t('common.courier')">
        <option value="">{{ t('common.select') }}</option>
        <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
      </AppSelect>
    </div>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>
    <div
      v-if="message"
      class="mb-4 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700 dark:border-success-500/20 dark:bg-success-500/10"
    >
      {{ message }}
    </div>

    <div class="mb-6 grid grid-cols-1 gap-4 xl:grid-cols-2">
      <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-5 shadow-theme-xs">
        <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('courierStock.transferFromStore') }}</h2>
        <div class="space-y-3">
          <AppSelect v-model="transferForm.productId" :label="t('common.product')">
            <option value="">{{ t('common.select') }}</option>
            <option v-for="p in products" :key="p.id" :value="String(p.id)">
              {{ p.name }} ({{ t('courierStock.storeQty', { qty: p.storeQuantity ?? t('common.empty') }) }})
            </option>
          </AppSelect>
          <AppInput v-model="transferForm.quantity" :label="t('common.quantity')" type="number" />
          <AppButton :loading="transferSaving" @click="doTransfer">{{ t('courierStock.transfer') }}</AppButton>
        </div>
      </div>

      <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-5 shadow-theme-xs">
        <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('courierStock.returnToStore') }}</h2>
        <div class="space-y-3">
          <AppSelect v-model="returnForm.productId" :label="t('courierStock.productFromStock')">
            <option value="">{{ t('common.select') }}</option>
            <option v-for="s in returnableStock" :key="s.id" :value="String(s.productId)">
              {{ s.productName }} ({{ s.quantity }})
            </option>
          </AppSelect>
          <AppInput v-model="returnForm.quantity" :label="t('common.quantity')" type="number" />
          <AppButton variant="secondary" :loading="returnSaving" @click="doReturn">
            {{ t('courierStock.return') }}
          </AppButton>
        </div>
      </div>
    </div>

    <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('courierStock.currentStock') }}</h2>
    <DataTable
      :loading="loading"
      :empty="!loading && !stock.length ? t('courierStock.empty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.unit') }}</th>
      </template>
      <tr v-for="s in stock" :key="s.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ s.productName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ s.quantity ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(s.productPrice) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(s.totalAmount) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ enumLabel('unitType', s.unitType, t('common.empty')) }}</td>
      </tr>
    </DataTable>

    <div class="mt-8 mb-3 flex flex-wrap items-end justify-between gap-3">
      <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('courierStock.history') }}</h2>
      <div class="flex flex-wrap items-end gap-2">
        <AppInput v-model="histFrom" :label="t('common.from')" type="date" />
        <AppInput v-model="histTo" :label="t('common.to')" type="date" />
        <AppButton @click="loadHistory">{{ t('common.filter') }}</AppButton>
      </div>
    </div>

    <DataTable
      :loading="histLoading"
      :empty="!histLoading && !history.length ? t('courierStock.historyEmpty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.action') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.before') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.change') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.after') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
      </template>
      <tr v-for="(h, idx) in history" :key="idx">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ h.productName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ enumLabel('stockAction', h.actionType as string, t('common.empty')) }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ (h.beforeQuantity as number) ?? t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ (h.changeQuantity as number) ?? h.quantity ?? t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ (h.afterQuantity as number) ?? t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ h.createdAt || t('common.empty') }}</td>
      </tr>
    </DataTable>
  </div>
</template>
