<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
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
    error.value = apiError(e, 'Maʼlumotlarni yuklab bo‘lmadi')
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
    error.value = apiError(e, 'Mijozni qidirib bo‘lmadi')
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
    error.value = 'Kamida bitta mahsulot qo‘shing'
    return
  }
  if (!phone.value.trim() || !fullName.value.trim()) {
    error.value = 'Mijoz ismi va telefon majburiy'
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
    success.value = 'Buyurtma yaratildi'
    setTimeout(() => router.push('/admin/orders'), 600)
  } catch (e) {
    error.value = apiError(e, 'Buyurtmani yaratib bo‘lmadi')
  } finally {
    saving.value = false
  }
}

onMounted(loadLookups)
</script>

<template>
  <div>
    <PageHeader title="Yangi buyurtma" subtitle="Mijoz, manzil va mahsulotlar">
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
    <div
      v-if="success"
      class="mb-4 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700"
    >
      {{ success }}
    </div>

    <form class="space-y-4" @submit.prevent="submit">
      <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs">
        <h2 class="mb-3 text-lg font-semibold text-gray-800">Mijoz qidirish</h2>
        <div class="flex flex-wrap items-end gap-2">
          <AppInput
            v-model="phoneQuery"
            class="min-w-64 flex-1"
            label="Telefon bo‘yicha"
            placeholder="+998..."
          />
          <AppButton type="button" :loading="searching" @click="searchByPhone">Qidirish</AppButton>
        </div>
        <div v-if="customers.length" class="mt-3 space-y-2">
          <button
            v-for="c in customers"
            :key="c.id"
            type="button"
            class="flex w-full items-center justify-between rounded-xl border border-gray-200 px-4 py-3 text-left text-theme-sm hover:bg-gray-50"
            :class="selectedCustomer?.id === c.id ? 'border-brand-300 bg-brand-50' : ''"
            @click="selectCustomer(c)"
          >
            <span>{{ c.fullName }} — {{ c.phone }}</span>
            <span class="text-gray-400">#{{ c.id }}</span>
          </button>
        </div>
      </div>

      <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs">
        <h2 class="mb-3 text-lg font-semibold text-gray-800">Mijoz va manzil</h2>
        <div class="grid grid-cols-1 gap-3 md:grid-cols-2">
          <AppInput v-model="fullName" label="To‘liq ism" />
          <AppInput v-model="phone" label="Telefon" />
          <AppInput v-model="address" label="Manzil" class="md:col-span-2" />
          <AppInput v-model="home" label="Uy" />
          <AppInput v-model="entrance" label="Podyezd" />
          <AppInput v-model="apartment" label="Xonadon" />
          <AppInput v-model="floor" label="Qavat" />
          <AppInput v-model="orientations" label="Mo‘ljal" class="md:col-span-2" />
          <AppSelect v-model="paymentType" label="To‘lov turi">
            <option value="CASH">Naqd</option>
            <option value="CARD">Karta</option>
          </AppSelect>
          <AppSelect v-model="courierId" label="Kuryer">
            <option value="">Tanlanmagan</option>
            <option v-for="c in couriers" :key="c.id" :value="String(c.id)">
              {{ c.fullName }}
            </option>
          </AppSelect>
        </div>
      </div>

      <div class="rounded-2xl border border-gray-200 bg-white p-5 shadow-theme-xs">
        <div class="mb-3 flex items-center justify-between gap-2">
          <h2 class="text-lg font-semibold text-gray-800">Mahsulotlar</h2>
          <AppButton type="button" size="sm" variant="secondary" @click="addItem">
            Qo‘shish
          </AppButton>
        </div>

        <div class="space-y-3">
          <div
            v-for="(item, idx) in items"
            :key="idx"
            class="grid grid-cols-1 gap-3 rounded-xl border border-gray-100 p-3 md:grid-cols-12"
          >
            <AppSelect
              v-model="item.productId"
              label="Mahsulot"
              class="md:col-span-5"
              @update:model-value="onProductChange(item)"
            >
              <option value="">Tanlang</option>
              <option v-for="p in products" :key="p.id" :value="String(p.id)">
                {{ p.name }} ({{ money(p.price) }})
              </option>
            </AppSelect>
            <AppInput v-model="item.quantity" label="Miqdor" type="number" class="md:col-span-2" />
            <AppInput v-model="item.productCost" label="Narx" type="number" class="md:col-span-2" />
            <label class="flex items-end gap-2 pb-2 text-theme-sm text-gray-700 md:col-span-2">
              <input v-model="item.isBonus" type="checkbox" class="size-4 rounded border-gray-300" />
              Bonus
            </label>
            <div class="flex items-end md:col-span-1">
              <AppButton type="button" size="sm" variant="danger" @click="removeItem(idx)">
                ×
              </AppButton>
            </div>
          </div>
        </div>

        <p class="mt-4 text-theme-sm font-medium text-gray-800">
          Taxminiy summa: {{ money(totalPreview) }}
        </p>
      </div>

      <div class="flex justify-end gap-2">
        <AppButton type="button" variant="secondary" @click="router.push('/admin/orders')">
          Bekor
        </AppButton>
        <AppButton type="submit" :loading="saving">Yaratish</AppButton>
      </div>
    </form>
  </div>
</template>
