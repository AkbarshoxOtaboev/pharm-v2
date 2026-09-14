<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { createOrder } from '@/api/orders.api'
import { searchCustomers } from '@/api/customers.api'
import { fetchProductsInStock } from '@/api/products.api'
import { fetchCouriers } from '@/api/users.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type {
  CustomerResponse,
  OrderDTO,
  OrderItemDTO,
  PaymentType,
  ProductResponse,
  UserResponse,
} from '@/types/api'
import { apiError, money } from '@/utils/format'

interface LineItem {
  productId: string
  quantity: string
  productCost: string
  isBonus: boolean
}

const { t } = useI18n()
const router = useRouter()
const saving = ref(false)
const error = ref('')
const success = ref('')

const phoneQuery = ref('')
const searching = ref(false)
const customers = ref<CustomerResponse[]>([])
const selectedCustomer = ref<CustomerResponse | null>(null)

const fullName = ref('')
const phone = ref('')
const address = ref('')
const home = ref('')
const entrance = ref('')
const apartment = ref('')
const floor = ref('')
const orientations = ref('')
const paymentType = ref<PaymentType>('CASH')
const courierId = ref('')

const couriers = ref<UserResponse[]>([])
const products = ref<ProductResponse[]>([])
const items = ref<LineItem[]>([
  { productId: '', quantity: '1', productCost: '', isBonus: false },
])

const totalPreview = computed(() =>
  items.value.reduce((sum, item) => {
    if (item.isBonus) return sum
    return sum + Number(item.productCost || 0) * Number(item.quantity || 0)
  }, 0),
)

async function loadLookups() {
  try {
    const [cRes, pRes] = await Promise.all([fetchCouriers(), fetchProductsInStock()])
    couriers.value = cRes.data || []
    products.value = pRes.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  }
}

async function searchByPhone() {
  if (!phoneQuery.value.trim()) return
  searching.value = true
  error.value = ''
  try {
    const res = await searchCustomers(phoneQuery.value.trim())
    customers.value = res.data || []
    if (customers.value.length === 1) {
      selectCustomer(customers.value[0])
    }
  } catch (e) {
    error.value = apiError(e, 'errors.searchCustomer')
  } finally {
    searching.value = false
  }
}

function selectCustomer(c: CustomerResponse) {
  selectedCustomer.value = c
  fullName.value = c.fullName
  phone.value = c.phone
}

function onProductChange(item: LineItem) {
  const p = products.value.find((x) => String(x.id) === item.productId)
  if (p && !item.productCost) {
    item.productCost = String(p.price ?? p.priceCost ?? '')
  }
}

function addItem() {
  items.value.push({ productId: '', quantity: '1', productCost: '', isBonus: false })
}

function removeItem(idx: number) {
  items.value.splice(idx, 1)
  if (!items.value.length) addItem()
}

async function submit() {
  error.value = ''
  success.value = ''
  const mapped: OrderItemDTO[] = items.value
    .filter((i) => i.productId && Number(i.quantity) > 0)
    .map((i) => ({
      productId: Number(i.productId),
      quantity: Number(i.quantity),
      productCost: i.productCost ? Number(i.productCost) : undefined,
      isBonus: i.isBonus,
    }))

  if (!mapped.length) {
    error.value = t('errors.minOneProduct')
    return
  }
  if (!phone.value.trim() || !fullName.value.trim()) {
    error.value = t('errors.customerNamePhoneRequired')
    return
  }

  const dto: OrderDTO = {
    customerId: selectedCustomer.value?.id,
    fullName: fullName.value.trim(),
    phone: phone.value.trim(),
    address: address.value.trim() || undefined,
    home: home.value.trim() || undefined,
    entrance: entrance.value.trim() || undefined,
    apartment: apartment.value.trim() || undefined,
    floor: floor.value.trim() || undefined,
    orientations: orientations.value.trim() || undefined,
    paymentType: paymentType.value,
    courierId: courierId.value ? Number(courierId.value) : undefined,
    items: mapped,
  }

  saving.value = true
  try {
    await createOrder(dto)
    success.value = t('orders.created')
    setTimeout(() => router.push('/admin/orders'), 600)
  } catch (e) {
    error.value = apiError(e, 'errors.createOrder')
  } finally {
    saving.value = false
  }
}

onMounted(loadLookups)
</script>

<template>
  <div>
    <PageHeader :title="t('orders.newOrder')" :subtitle="t('orders.createSubtitle')">
      <template #actions>
        <AppButton variant="secondary" @click="router.push('/admin/orders')">{{ t('common.back') }}</AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>
    <div
      v-if="success"
      class="mb-4 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700 dark:border-success-500/20 dark:bg-success-500/10"
    >
      {{ success }}
    </div>

    <form class="space-y-4" @submit.prevent="submit">
      <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-5 shadow-theme-xs">
        <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.searchCustomer') }}</h2>
        <div class="flex flex-wrap items-end gap-2">
          <AppInput
            v-model="phoneQuery"
            class="min-w-64 flex-1"
            :label="t('orders.byPhone')"
            placeholder="+998..."
          />
          <AppButton type="button" :loading="searching" @click="searchByPhone">{{ t('common.search') }}</AppButton>
        </div>
        <div v-if="customers.length" class="mt-3 space-y-2">
          <button
            v-for="c in customers"
            :key="c.id"
            type="button"
            class="flex w-full items-center justify-between rounded-xl border border-gray-200 dark:border-gray-800 px-4 py-3 text-left text-theme-sm hover:bg-gray-50 dark:hover:bg-white/5"
            :class="selectedCustomer?.id === c.id ? 'border-brand-300 bg-brand-50 dark:border-brand-500/40 dark:bg-brand-500/10' : ''"
            @click="selectCustomer(c)"
          >
            <span>{{ c.fullName }} — {{ c.phone }}</span>
            <span class="text-gray-400 dark:text-gray-500">#{{ c.id }}</span>
          </button>
        </div>
      </div>

      <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-5 shadow-theme-xs">
        <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.customerAndAddress') }}</h2>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <AppInput v-model="fullName" :label="t('orders.fullName')" />
          <AppInput v-model="phone" :label="t('common.phone')" />
          <AppInput v-model="address" :label="t('common.address')" class="md:col-span-2" />
          <AppInput v-model="home" :label="t('common.house')" />
          <AppInput v-model="entrance" :label="t('common.entrance')" />
          <AppInput v-model="apartment" :label="t('common.apartment')" />
          <AppInput v-model="floor" :label="t('common.floor')" />
          <AppInput v-model="orientations" :label="t('common.orientation')" class="md:col-span-2" />
          <AppSelect v-model="paymentType" :label="t('orders.paymentType')">
            <option value="CASH">{{ enumLabel('paymentType', 'CASH') }}</option>
            <option value="CARD">{{ enumLabel('paymentType', 'CARD') }}</option>
          </AppSelect>
          <AppSelect v-model="courierId" :label="t('common.courier')">
            <option value="">{{ t('common.notSelected') }}</option>
            <option v-for="c in couriers" :key="c.id" :value="String(c.id)">
              {{ c.fullName }}
            </option>
          </AppSelect>
        </div>
      </div>

      <div class="rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-5 shadow-theme-xs">
        <div class="mb-3 flex items-center justify-between gap-2">
          <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.products') }}</h2>
          <AppButton type="button" size="sm" variant="secondary" @click="addItem">
            {{ t('common.add') }}
          </AppButton>
        </div>

        <div class="space-y-3">
          <div
            v-for="(item, idx) in items"
            :key="idx"
            class="grid grid-cols-1 gap-3 rounded-xl border border-gray-100 dark:border-gray-800 p-3 md:grid-cols-12"
          >
            <AppSelect
              v-model="item.productId"
              :label="t('common.product')"
              class="md:col-span-5"
              @update:model-value="onProductChange(item)"
            >
              <option value="">{{ t('common.select') }}</option>
              <option v-for="p in products" :key="p.id" :value="String(p.id)">
                {{ p.name }} ({{ money(p.price) }})
              </option>
            </AppSelect>
            <AppInput v-model="item.quantity" :label="t('common.quantity')" type="number" class="md:col-span-2" />
            <AppInput v-model="item.productCost" :label="t('common.price')" type="number" class="md:col-span-2" />
            <label class="flex items-end gap-2 pb-2 text-theme-sm text-gray-700 dark:text-gray-300 md:col-span-2">
              <input v-model="item.isBonus" type="checkbox" class="size-4 rounded border-gray-300" />
              {{ t('common.bonus') }}
            </label>
            <div class="flex items-end md:col-span-1">
              <AppButton type="button" size="sm" variant="danger" @click="removeItem(idx)">
                ×
              </AppButton>
            </div>
          </div>
        </div>

        <p class="mt-4 text-theme-sm font-medium text-gray-800 dark:text-white/90">
          {{ t('orders.estimatedTotal', { amount: money(totalPreview) }) }}
        </p>
      </div>

      <div class="flex justify-end gap-2">
        <AppButton type="button" variant="secondary" @click="router.push('/admin/orders')">
          {{ t('common.cancel') }}
        </AppButton>
        <AppButton type="submit" :loading="saving">{{ t('common.create') }}</AppButton>
      </div>
    </form>
  </div>
</template>
