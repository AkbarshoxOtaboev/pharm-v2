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
  productId: number
  quantity: number
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
const emptyDraft = () => ({ productId: '', quantity: '1', isBonus: false })
const draft = ref(emptyDraft())
const items = ref<LineItem[]>([])

const availableStock = computed(() =>
  stock.value.filter((s) => Number(s.quantity ?? 0) > 0),
)
const stockById = computed(() => new Map(stock.value.map((s) => [s.productId, s])))

const requestedById = computed(() => {
  const map = new Map<number, number>()
  for (const item of items.value) {
    map.set(item.productId, (map.get(item.productId) ?? 0) + item.quantity)
  }
  return map
})

function stockOf(productId: number) {
  return stockById.value.get(productId)
}

function priceOf(productId: number) {
  if (!productId) return 0
  return priceById.value.get(productId) ?? Number(stockOf(productId)?.productPrice ?? 0)
}

function lineSum(item: LineItem) {
  return item.isBonus ? 0 : priceOf(item.productId) * item.quantity
}

function onCourier(productId: number) {
  return Number(stockOf(productId)?.quantity ?? 0)
}

function availableFor(productId: number) {
  return onCourier(productId) - (requestedById.value.get(productId) ?? 0)
}

function unitLabel(productId: number) {
  return enumLabel('unitType', stockOf(productId)?.unitType, '')
}

const draftProductId = computed(() => Number(draft.value.productId) || 0)
const draftQty = computed(() => Number(draft.value.quantity) || 0)
const draftRemaining = computed(() => availableFor(draftProductId.value) - draftQty.value)
const draftSum = computed(() => (draft.value.isBonus ? 0 : priceOf(draftProductId.value) * draftQty.value))
const canAdd = computed(() => draftProductId.value > 0 && draftQty.value > 0 && draftRemaining.value >= 0)

const total = computed(() => items.value.reduce((sum, i) => sum + lineSum(i), 0))
const paidCount = computed(() => items.value.filter((i) => !i.isBonus).length)
const bonusCount = computed(() => items.value.filter((i) => i.isBonus).length)
const overStock = computed(() => items.value.find((i) => availableFor(i.productId) < 0))

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
    items.value = items.value.filter((i) => stockById.value.has(i.productId))
    if (draftProductId.value && !stockById.value.has(draftProductId.value)) draft.value.productId = ''
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
  if (!canAdd.value) return
  const productId = draftProductId.value
  const { isBonus } = draft.value
  const existing = items.value.find((i) => i.productId === productId && i.isBonus === isBonus)
  if (existing) existing.quantity += draftQty.value
  else items.value.push({ key: ++lineKey, productId, quantity: draftQty.value, isBonus })
  draft.value = emptyDraft()
}

function removeItem(key: number) {
  items.value = items.value.filter((i) => i.key !== key)
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
  if (!items.value.length) {
    error.value = t('errors.minOneProduct')
    return
  }
  if (overStock.value) {
    error.value = t('errors.notEnoughOnCourier', {
      product: stockOf(overStock.value.productId)?.productName ?? '',
    })
    return
  }

  const mapped: OrderItemDTO[] = items.value.map((i) => ({
    productId: i.productId,
    quantity: i.quantity,
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
          <div class="mb-4 flex items-center gap-3">
            <span class="flex size-9 items-center justify-center rounded-lg bg-brand-50 text-brand-500 dark:bg-brand-500/15">
              <AppIcon name="basket" />
            </span>
            <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.products') }}</h2>
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

          <div v-else class="space-y-4">
            <div
              class="rounded-xl border p-3 transition"
              :class="
                draftProductId && draftRemaining < 0
                  ? 'border-error-500/40 bg-error-50/40 dark:bg-error-500/5'
                  : 'border-gray-100 dark:border-gray-800'
              "
              @keydown.enter.prevent="addItem"
            >
              <div class="grid grid-cols-2 items-end gap-3 md:grid-cols-12">
                <AppSelect v-model="draft.productId" :label="t('common.product')" class="col-span-2 md:col-span-5">
                  <option value="">{{ t('common.select') }}</option>
                  <option
                    v-for="s in availableStock"
                    :key="s.productId"
                    :value="String(s.productId)"
                    :disabled="availableFor(s.productId) <= 0"
                  >
                    {{ s.productName }} — {{ money(availableFor(s.productId)) }}
                  </option>
                </AppSelect>
                <AppInput
                  v-model="draft.quantity"
                  :label="t('common.quantity')"
                  type="number"
                  class="md:col-span-2"
                />
                <label
                  class="flex h-11 cursor-pointer items-center justify-center gap-2 rounded-lg border text-theme-sm font-medium transition md:col-span-2"
                  :class="draft.isBonus
                    ? 'border-success-500/40 bg-success-50 text-success-700 dark:bg-success-500/10 dark:text-success-500'
                    : 'border-gray-300 text-gray-600 hover:bg-gray-50 dark:border-gray-700 dark:text-gray-400 dark:hover:bg-white/5'"
                >
                  <input v-model="draft.isBonus" type="checkbox" class="sr-only" />
                  <AppIcon name="gift" class="size-4" />
                  {{ t('common.bonus') }}
                </label>
                <AppButton type="button" class="col-span-2 h-11 md:col-span-3" :disabled="!canAdd" @click="addItem">
                  <AppIcon name="plus" class="size-4" />
                  {{ t('common.add') }}
                </AppButton>
              </div>

              <div v-if="draftProductId" class="mt-3 flex flex-wrap items-center gap-x-4 gap-y-2 text-theme-xs">
                <span class="inline-flex items-center gap-1.5 text-gray-500 dark:text-gray-400">
                  <AppIcon name="truck" class="size-4" />
                  {{ t('orders.onCourier', { qty: money(onCourier(draftProductId)), unit: unitLabel(draftProductId) }) }}
                </span>
                <span
                  class="inline-flex items-center gap-1.5"
                  :class="draftRemaining < 0 ? 'font-medium text-error-600 dark:text-error-500' : 'text-gray-500 dark:text-gray-400'"
                >
                  <AppIcon :name="draftRemaining < 0 ? 'alert' : 'box'" class="size-4" />
                  {{
                    draftRemaining < 0
                      ? t('orders.notEnough', { qty: money(-draftRemaining), unit: unitLabel(draftProductId) })
                      : t('orders.remainingAfter', { qty: money(draftRemaining), unit: unitLabel(draftProductId) })
                  }}
                </span>
                <span class="text-gray-500 dark:text-gray-400">
                  {{ t('common.price') }}: <b class="font-medium text-gray-700 dark:text-gray-300">{{ money(priceOf(draftProductId)) }}</b>
                </span>
                <span class="ml-auto text-gray-500 dark:text-gray-400">
                  {{ t('common.amount') }}:
                  <b v-if="draft.isBonus" class="font-semibold text-success-600 dark:text-success-500">{{ t('common.bonus') }}</b>
                  <b v-else class="font-semibold text-gray-800 dark:text-white/90">{{ money(draftSum) }}</b>
                </span>
              </div>
            </div>

            <div
              v-if="!items.length"
              class="rounded-xl border border-dashed border-gray-300 px-4 py-5 text-center text-theme-sm text-gray-500 dark:border-gray-700 dark:text-gray-400"
            >
              {{ t('orders.noProducts') }}
            </div>
            <div v-else class="overflow-hidden rounded-xl border border-gray-100 dark:border-gray-800">
              <div
                class="hidden grid-cols-12 gap-3 border-b border-gray-100 bg-gray-50 px-4 py-2.5 text-theme-xs font-medium text-gray-500 md:grid dark:border-gray-800 dark:bg-white/[0.03] dark:text-gray-400"
              >
                <span class="col-span-5">{{ t('common.product') }}</span>
                <span class="col-span-2 text-right">{{ t('common.quantity') }}</span>
                <span class="col-span-2 text-right">{{ t('common.price') }}</span>
                <span class="col-span-2 text-right">{{ t('common.amount') }}</span>
                <span class="col-span-1" />
              </div>
              <div class="divide-y divide-gray-100 dark:divide-gray-800">
                <div
                  v-for="item in items"
                  :key="item.key"
                  class="grid grid-cols-12 items-center gap-3 px-4 py-3 text-theme-sm"
                  :class="availableFor(item.productId) < 0 && 'bg-error-50/40 dark:bg-error-500/5'"
                >
                  <div class="col-span-7 min-w-0 md:col-span-5">
                    <div class="flex items-center gap-2">
                      <span class="truncate font-medium text-gray-800 dark:text-white/90">
                        {{ stockOf(item.productId)?.productName }}
                      </span>
                      <span
                        v-if="item.isBonus"
                        class="inline-flex shrink-0 items-center gap-1 rounded-full bg-success-50 px-2 py-0.5 text-theme-xs font-medium text-success-700 dark:bg-success-500/10 dark:text-success-500"
                      >
                        <AppIcon name="gift" class="size-3" />
                        {{ t('common.bonus') }}
                      </span>
                    </div>
                    <p class="mt-0.5 text-theme-xs text-gray-500 md:hidden dark:text-gray-400">
                      {{ money(item.quantity) }} {{ unitLabel(item.productId) }} × {{ money(priceOf(item.productId)) }}
                    </p>
                  </div>
                  <span class="hidden text-right text-gray-700 md:col-span-2 md:block dark:text-gray-300">
                    {{ money(item.quantity) }} {{ unitLabel(item.productId) }}
                  </span>
                  <span class="hidden text-right text-gray-500 md:col-span-2 md:block dark:text-gray-400">
                    {{ money(priceOf(item.productId)) }}
                  </span>
                  <span class="col-span-3 text-right font-semibold md:col-span-2">
                    <span v-if="item.isBonus" class="text-success-600 dark:text-success-500">{{ t('common.bonus') }}</span>
                    <span v-else class="text-gray-800 dark:text-white/90">{{ money(lineSum(item)) }}</span>
                  </span>
                  <div class="col-span-2 flex justify-end md:col-span-1">
                    <button
                      type="button"
                      class="flex size-9 items-center justify-center rounded-lg bg-error-500 text-white transition hover:bg-error-600"
                      :title="t('common.delete')"
                      @click="removeItem(item.key)"
                    >
                      <AppIcon name="trash" class="size-4" />
                    </button>
                  </div>
                </div>
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
