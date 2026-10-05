<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { createOrder } from '@/api/orders.api'
import { searchCustomers } from '@/api/customers.api'
import { fetchProducts } from '@/api/products.api'
import { fetchCourierStock } from '@/api/courierStock.api'
import { fetchCouriers } from '@/api/users.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type {
  CourierStockResponse,
  CustomerResponse,
  OrderDTO,
  OrderItemDTO,
  PaymentType,
  UserResponse,
} from '@/types/api'
import { apiError, money } from '@/utils/format'
import { UZ_PHONE_PLACEHOLDER, formatUzPhone, uzPhoneDigits } from '@/utils/phone'

interface LineItem {
  key: number
  productId: string
  quantity: string
  isBonus: boolean
}

const PAYMENT_TYPES: { value: PaymentType; icon: string }[] = [
  { value: 'CASH', icon: 'cash' },
  { value: 'CARD', icon: 'card' },
]

const { t } = useI18n()
const router = useRouter()
const saving = ref(false)
const error = ref('')
const success = ref('')

const searching = ref(false)
const searched = ref(false)
const selectedCustomer = ref<CustomerResponse | null>(null)
let searchSeq = 0

const phone = ref('')
const fullName = ref('')
const address = ref('')
const phoneDigits = computed(() => uzPhoneDigits(phone.value))
const phoneComplete = computed(() => phoneDigits.value.length === 9)
const paymentType = ref<PaymentType>('CASH')
const courierId = ref('')

const couriers = ref<UserResponse[]>([])
const priceById = ref(new Map<number, number>())
const stock = ref<CourierStockResponse[]>([])
const stockLoading = ref(false)
let stockSeq = 0

let lineKey = 0
const newLine = (): LineItem => ({ key: ++lineKey, productId: '', quantity: '1', isBonus: false })
const items = ref<LineItem[]>([newLine()])

const availableStock = computed(() =>
  stock.value.filter((s) => Number(s.quantity ?? 0) > 0),
)
const stockById = computed(() => new Map(stock.value.map((s) => [s.productId, s])))

const requestedById = computed(() => {
  const map = new Map<number, number>()
  for (const item of items.value) {
    if (!item.productId) continue
    const id = Number(item.productId)
    map.set(id, (map.get(id) ?? 0) + Number(item.quantity || 0))
  }
  return map
})

function stockOf(item: LineItem) {
  return item.productId ? stockById.value.get(Number(item.productId)) : undefined
}

function priceOf(item: LineItem) {
  const id = Number(item.productId)
  if (!id) return 0
  return priceById.value.get(id) ?? Number(stockById.value.get(id)?.productPrice ?? 0)
}

function lineSum(item: LineItem) {
  return item.isBonus ? 0 : priceOf(item) * Number(item.quantity || 0)
}

function onCourier(item: LineItem) {
  return Number(stockOf(item)?.quantity ?? 0)
}

function remainingAfter(item: LineItem) {
  return onCourier(item) - (requestedById.value.get(Number(item.productId)) ?? 0)
}

function unitLabel(item: LineItem) {
  return enumLabel('unitType', stockOf(item)?.unitType, '')
}

const filledItems = computed(() =>
  items.value.filter((i) => i.productId && Number(i.quantity) > 0),
)
const total = computed(() => filledItems.value.reduce((sum, i) => sum + lineSum(i), 0))
const paidCount = computed(() => filledItems.value.filter((i) => !i.isBonus).length)
const bonusCount = computed(() => filledItems.value.filter((i) => i.isBonus).length)
const overStock = computed(() => filledItems.value.find((i) => remainingAfter(i) < 0))

async function loadLookups() {
  try {
    const [cRes, pRes] = await Promise.all([fetchCouriers(), fetchProducts()])
    couriers.value = cRes.data || []
    priceById.value = new Map(
      (pRes.data || [])
        .filter((p) => p.priceCost != null)
        .map((p) => [p.id, Number(p.priceCost)]),
    )
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  }
}

watch(courierId, async (id) => {
  const seq = ++stockSeq
  stock.value = []
  if (!id) {
    stockLoading.value = false
    return
  }
  stockLoading.value = true
  try {
    const res = await fetchCourierStock(Number(id))
    if (seq !== stockSeq) return
    stock.value = res.data || []
    for (const item of items.value) {
      if (item.productId && !stockById.value.has(Number(item.productId))) item.productId = ''
    }
  } catch (e) {
    if (seq === stockSeq) error.value = apiError(e, 'errors.loadData')
  } finally {
    if (seq === stockSeq) stockLoading.value = false
  }
})

function onPhoneInput(value: string) {
  phone.value = value
  void nextTick(() => {
    phone.value = formatUzPhone(value)
  })
}

watch(phoneDigits, (digits, prev) => {
  if (digits === prev) return
  if (selectedCustomer.value) {
    selectedCustomer.value = null
    fullName.value = ''
    address.value = ''
  }
  searched.value = false
  if (digits.length === 9) void searchByPhone(digits)
})

async function searchByPhone(digits: string) {
  const seq = ++searchSeq
  searching.value = true
  error.value = ''
  try {
    const res = await searchCustomers(`998${digits}`)
    if (seq !== searchSeq) return
    const c = res.data
    searched.value = true
    if (c) {
      selectedCustomer.value = c
      fullName.value = c.fullName
      if (c.lastAddress) address.value = c.lastAddress
    }
  } catch (e) {
    if (seq === searchSeq) error.value = apiError(e, 'errors.searchCustomer')
  } finally {
    if (seq === searchSeq) searching.value = false
  }
}

function addItem() {
  items.value.push(newLine())
}

function removeItem(idx: number) {
  items.value.splice(idx, 1)
  if (!items.value.length) addItem()
}

async function submit() {
  error.value = ''
  success.value = ''

  if (!phoneDigits.value || !fullName.value.trim()) {
    error.value = t('errors.customerNamePhoneRequired')
    return
  }
  if (!phoneComplete.value) {
    error.value = t('errors.phoneIncomplete')
    return
  }
  if (!courierId.value) {
    error.value = t('errors.courierRequired')
    return
  }
  if (!filledItems.value.length) {
    error.value = t('errors.minOneProduct')
    return
  }
  if (overStock.value) {
    error.value = t('errors.notEnoughOnCourier', {
      product: stockOf(overStock.value)?.productName ?? '',
    })
    return
  }

  const mapped: OrderItemDTO[] = filledItems.value.map((i) => ({
    productId: Number(i.productId),
    quantity: Number(i.quantity),
    isBonus: i.isBonus,
  }))

  const dto: OrderDTO = {
    customerId: selectedCustomer.value?.id,
    fullName: fullName.value.trim(),
    phone: `998${phoneDigits.value}`,
    address: address.value.trim() || undefined,
    paymentType: paymentType.value,
    courierId: Number(courierId.value),
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
        <AppButton variant="secondary" @click="router.push('/admin/orders')">
          <AppIcon name="arrow-left" class="size-4" />
          {{ t('common.back') }}
        </AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 flex items-start gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      <AppIcon name="alert" class="mt-0.5 size-4" />
      <span>{{ error }}</span>
    </div>
    <div
      v-if="success"
      class="mb-4 flex items-start gap-2 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700 dark:border-success-500/20 dark:bg-success-500/10"
    >
      <AppIcon name="check" class="mt-0.5 size-4" />
      <span>{{ success }}</span>
    </div>

    <form class="grid grid-cols-1 gap-4 lg:grid-cols-3" @submit.prevent="submit">
      <div class="space-y-4 lg:col-span-2">
        <!-- Customer -->
        <section class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
          <div class="mb-4 flex items-center gap-3">
            <span class="flex size-9 items-center justify-center rounded-lg bg-brand-50 text-brand-500 dark:bg-brand-500/15">
              <AppIcon name="user" />
            </span>
            <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.customerAndAddress') }}</h2>
          </div>
          <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
            <div>
              <AppInput
                :model-value="phone"
                type="tel"
                autocomplete="off"
                :label="t('common.phone')"
                :placeholder="UZ_PHONE_PLACEHOLDER"
                @update:model-value="onPhoneInput"
              />
              <p v-if="searching" class="mt-1.5 flex items-center gap-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
                <span class="size-3 animate-spin rounded-full border-2 border-gray-300 border-t-brand-500" />
                {{ t('orders.customerSearching') }}
              </p>
              <p v-else-if="selectedCustomer" class="mt-1.5 flex items-center gap-1.5 text-theme-xs text-success-600 dark:text-success-500">
                <AppIcon name="check" class="size-3.5" />
                {{ t('orders.customerFound', { name: selectedCustomer.fullName }) }}
              </p>
              <p v-else-if="searched && phoneComplete" class="mt-1.5 flex items-center gap-1.5 text-theme-xs text-gray-500 dark:text-gray-400">
                <AppIcon name="plus" class="size-3.5" />
                {{ t('orders.customerNew') }}
              </p>
            </div>
            <AppInput v-model="fullName" :label="t('orders.fullName')" />
            <AppInput v-model="address" :label="t('common.address')" class="md:col-span-2" />
          </div>
        </section>

        <!-- Products -->
        <section class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
          <div class="mb-4 flex items-center justify-between gap-2">
            <div class="flex items-center gap-3">
              <span class="flex size-9 items-center justify-center rounded-lg bg-brand-50 text-brand-500 dark:bg-brand-500/15">
                <AppIcon name="basket" />
              </span>
              <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.products') }}</h2>
            </div>
            <AppButton type="button" size="sm" variant="secondary" :disabled="!availableStock.length" @click="addItem">
              <AppIcon name="plus" class="size-4" />
              {{ t('orders.addProduct') }}
            </AppButton>
          </div>

          <div
            v-if="!courierId"
            class="flex items-center gap-3 rounded-xl border border-dashed border-gray-300 px-4 py-6 text-theme-sm text-gray-500 dark:border-gray-700 dark:text-gray-400"
          >
            <AppIcon name="truck" class="size-6 text-gray-400" />
            {{ t('orders.selectCourierFirst') }}
          </div>
          <div
            v-else-if="stockLoading"
            class="flex items-center gap-3 rounded-xl border border-dashed border-gray-300 px-4 py-6 text-theme-sm text-gray-500 dark:border-gray-700 dark:text-gray-400"
          >
            <span class="size-4 animate-spin rounded-full border-2 border-gray-300 border-t-brand-500" />
            {{ t('common.loading') }}
          </div>
          <div
            v-else-if="!availableStock.length"
            class="flex items-center gap-3 rounded-xl border border-dashed border-warning-500/40 bg-warning-50 px-4 py-6 text-theme-sm text-warning-600 dark:bg-warning-500/10 dark:text-warning-500"
          >
            <AppIcon name="box" class="size-6" />
            {{ t('orders.courierEmpty') }}
          </div>

          <div v-else class="space-y-3">
            <div
              v-for="(item, idx) in items"
              :key="item.key"
              class="rounded-xl border p-3 transition"
              :class="
                item.productId && remainingAfter(item) < 0
                  ? 'border-error-500/40 bg-error-50/40 dark:bg-error-500/5'
                  : 'border-gray-100 dark:border-gray-800'
              "
            >
              <div class="grid grid-cols-2 gap-3 md:grid-cols-12 md:items-end">
                <AppSelect v-model="item.productId" :label="t('common.product')" class="col-span-2 md:col-span-6">
                  <option value="">{{ t('common.select') }}</option>
                  <option v-for="s in availableStock" :key="s.productId" :value="String(s.productId)">
                    {{ s.productName }} — {{ money(s.quantity) }}
                  </option>
                </AppSelect>
                <AppInput
                  v-model="item.quantity"
                  :label="t('common.quantity')"
                  type="number"
                  class="md:col-span-2"
                />
                <div class="md:col-span-3">
                  <span class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">{{ t('common.amount') }}</span>
                  <div class="mt-1.5 flex h-11 items-center rounded-lg bg-gray-50 px-3 text-theme-sm font-semibold text-gray-800 dark:bg-white/5 dark:text-white/90">
                    <span v-if="item.isBonus" class="text-success-600 dark:text-success-500">{{ t('common.bonus') }}</span>
                    <span v-else>{{ money(lineSum(item)) }}</span>
                  </div>
                </div>
                <div class="col-span-2 flex items-end justify-end md:col-span-1">
                  <button
                    type="button"
                    class="flex size-11 items-center justify-center rounded-lg text-gray-400 transition hover:bg-error-50 hover:text-error-500 dark:hover:bg-error-500/10"
                    :title="t('common.delete')"
                    @click="removeItem(idx)"
                  >
                    <AppIcon name="trash" />
                  </button>
                </div>
              </div>

              <div v-if="item.productId" class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-theme-xs">
                <span class="inline-flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
                  <AppIcon name="truck" class="size-4" />
                  {{ t('orders.onCourier', { qty: money(onCourier(item)), unit: unitLabel(item) }) }}
                </span>
                <span
                  class="inline-flex items-center gap-1.5"
                  :class="remainingAfter(item) < 0 ? 'font-medium text-error-600 dark:text-error-500' : 'text-gray-500 dark:text-gray-400'"
                >
                  <AppIcon :name="remainingAfter(item) < 0 ? 'alert' : 'box'" class="size-4" />
                  {{
                    remainingAfter(item) < 0
                      ? t('orders.notEnough', { qty: money(-remainingAfter(item)), unit: unitLabel(item) })
                      : t('orders.remainingAfter', { qty: money(remainingAfter(item)), unit: unitLabel(item) })
                  }}
                </span>
                <span class="text-gray-500 dark:text-gray-400">
                  {{ t('common.price') }}: <b class="font-medium text-gray-700 dark:text-gray-300">{{ money(priceOf(item)) }}</b>
                </span>
                <label class="ml-auto inline-flex cursor-pointer items-center gap-2 rounded-lg border px-2.5 py-1.5 transition"
                  :class="item.isBonus
                    ? 'border-success-500/40 bg-success-50 text-success-700 dark:bg-success-500/10 dark:text-success-500'
                    : 'border-gray-200 text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5'"
                >
                  <input v-model="item.isBonus" type="checkbox" class="sr-only" />
                  <AppIcon name="gift" class="size-4" />
                  {{ t('common.bonus') }}
                </label>
              </div>
            </div>
          </div>
        </section>
      </div>

      <!-- Delivery & summary -->
      <aside class="lg:col-span-1">
        <div class="space-y-4 lg:sticky lg:top-0">
          <section class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
            <div class="mb-4 flex items-center gap-3">
              <span class="flex size-9 items-center justify-center rounded-lg bg-brand-50 text-brand-500 dark:bg-brand-500/15">
                <AppIcon name="truck" />
              </span>
              <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.delivery') }}</h2>
            </div>
            <div class="space-y-4">
              <AppSelect v-model="courierId" :label="t('common.courier')">
                <option value="">{{ t('common.select') }}</option>
                <option v-for="c in couriers" :key="c.id" :value="String(c.id)">
                  {{ c.fullName }}
                </option>
              </AppSelect>
              <div>
                <span class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">{{ t('orders.paymentType') }}</span>
                <div class="mt-1.5 grid grid-cols-2 gap-2">
                  <button
                    v-for="p in PAYMENT_TYPES"
                    :key="p.value"
                    type="button"
                    class="flex h-11 items-center justify-center gap-2 rounded-lg border text-theme-sm font-medium transition"
                    :class="paymentType === p.value
                      ? 'border-brand-500 bg-brand-50 text-brand-600 dark:border-brand-500/60 dark:bg-brand-500/15 dark:text-brand-400'
                      : 'border-gray-300 text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5'"
                    @click="paymentType = p.value"
                  >
                    <AppIcon :name="p.icon" class="size-4" />
                    {{ enumLabel('paymentType', p.value) }}
                  </button>
                </div>
              </div>
            </div>
          </section>

          <section class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
            <div class="mb-4 flex items-center gap-3">
              <span class="flex size-9 items-center justify-center rounded-lg bg-brand-50 text-brand-500 dark:bg-brand-500/15">
                <AppIcon name="receipt" />
              </span>
              <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.summary') }}</h2>
            </div>
            <dl class="space-y-2 text-theme-sm">
              <div class="flex justify-between text-gray-500 dark:text-gray-400">
                <dt>{{ t('orders.products') }}</dt>
                <dd class="font-medium text-gray-800 dark:text-white/90">{{ paidCount }}</dd>
              </div>
              <div class="flex justify-between text-gray-500 dark:text-gray-400">
                <dt>{{ t('common.bonus') }}</dt>
                <dd class="font-medium text-gray-800 dark:text-white/90">{{ bonusCount }}</dd>
              </div>
              <div class="flex items-baseline justify-between border-t border-gray-100 pt-3 dark:border-gray-800">
                <dt class="font-medium text-gray-700 dark:text-gray-300">{{ t('common.total') }}</dt>
                <dd class="text-xl font-bold text-gray-800 dark:text-white/90">{{ money(total) }}</dd>
              </div>
            </dl>
            <div class="mt-5 flex flex-col gap-2">
              <AppButton type="submit" variant="success" :loading="saving" :disabled="!!overStock">
                <AppIcon v-if="!saving" name="check" class="size-4" />
                {{ t('orders.createOrder') }}
              </AppButton>
              <AppButton type="button" variant="secondary" @click="router.push('/admin/orders')">
                {{ t('common.cancel') }}
              </AppButton>
            </div>
          </section>
        </div>
      </aside>
    </form>
  </div>
</template>
