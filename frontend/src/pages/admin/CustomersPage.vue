<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createCustomer,
  createCustomerAddress,
  deleteAddress,
  deleteCustomer,
  fetchCustomerAddresses,
  fetchCustomers,
  searchCustomers,
  updateCustomer,
} from '@/api/customers.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type {
  AddressDTO,
  AddressResponse,
  CustomerDTO,
  CustomerResponse,
  CustomerType,
  Gender,
} from '@/types/api'
import { apiError, statusTone } from '@/utils/format'

const allCustomers = ref<CustomerResponse[]>([])
const customers = ref<CustomerResponse[]>([])
const loading = ref(false)
const error = ref('')
const searchPhone = ref('')

const modalOpen = ref(false)
const saving = ref(false)
const editingId = ref<number | null>(null)
const formError = ref('')

const form = reactive({
  fullName: '',
  phone: '',
  birthDate: '',
  description: '',
  gender: 'NOT_SELECTED' as Gender,
  type: 'B2C' as CustomerType,
})

const addrOpen = ref(false)
const addrCustomer = ref<CustomerResponse | null>(null)
const addresses = ref<AddressResponse[]>([])
const addrLoading = ref(false)
const addrSaving = ref(false)
const addrError = ref('')
const addrForm = reactive({
  fullAddress: '',
  home: '',
  entrance: '',
  apartment: '',
  floor: '',
  orientation: '',
})

const filteredHint = computed(() =>
  searchPhone.value.trim() ? `Qidiruv: ${searchPhone.value}` : 'Barcha mijozlar',
)

function resetForm() {
  form.fullName = ''
  form.phone = ''
  form.birthDate = ''
  form.description = ''
  form.gender = 'NOT_SELECTED'
  form.type = 'B2C'
  editingId.value = null
  formError.value = ''
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchCustomers()
    allCustomers.value = res.data || []
    customers.value = allCustomers.value
  } catch (e) {
    error.value = apiError(e, 'Mijozlarni yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
}

async function onSearch() {
  if (!searchPhone.value.trim()) {
    customers.value = allCustomers.value
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await searchCustomers(searchPhone.value.trim())
    customers.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'Qidiruv amalga oshmadi')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(c: CustomerResponse) {
  editingId.value = c.id
  form.fullName = c.fullName
  form.phone = c.phone
  form.birthDate = c.birthDate ? String(c.birthDate).slice(0, 10) : ''
  form.description = c.description || ''
  form.gender = (c.gender as Gender) || 'NOT_SELECTED'
  form.type = (c.type as CustomerType) || 'B2C'
  formError.value = ''
  modalOpen.value = true
}

async function save() {
  formError.value = ''
  if (!form.fullName.trim() || !form.phone.trim()) {
    formError.value = 'Ism va telefon majburiy'
    return
  }
  const dto: CustomerDTO = {
    fullName: form.fullName.trim(),
    phone: form.phone.trim(),
    birthDate: form.birthDate || undefined,
    description: form.description.trim() || undefined,
    gender: form.gender,
    type: form.type,
  }
  saving.value = true
  try {
    if (editingId.value) {
      await updateCustomer(editingId.value, dto)
    } else {
      await createCustomer(dto)
    }
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = apiError(e, 'Saqlab bo‘lmadi')
  } finally {
    saving.value = false
  }
}

async function onDelete(c: CustomerResponse) {
  if (!confirm(`${c.fullName} o‘chirilsinmi?`)) return
  try {
    await deleteCustomer(c.id)
    await load()
  } catch (e) {
    error.value = apiError(e, 'O‘chirib bo‘lmadi')
  }
}

async function openAddresses(c: CustomerResponse) {
  addrCustomer.value = c
  addrOpen.value = true
  addrError.value = ''
  addrForm.fullAddress = ''
  addrForm.home = ''
  addrForm.entrance = ''
  addrForm.apartment = ''
  addrForm.floor = ''
  addrForm.orientation = ''
  addrLoading.value = true
  try {
    const res = await fetchCustomerAddresses(c.id)
    addresses.value = res.data || []
  } catch (e) {
    addrError.value = apiError(e, 'Manzillarni yuklab bo‘lmadi')
  } finally {
    addrLoading.value = false
  }
}

async function addAddress() {
  if (!addrCustomer.value) return
  if (!addrForm.fullAddress.trim()) {
    addrError.value = 'To‘liq manzil majburiy'
    return
  }
  const dto: AddressDTO = {
    fullAddress: addrForm.fullAddress.trim(),
    home: addrForm.home.trim() || undefined,
    entrance: addrForm.entrance.trim() || undefined,
    apartment: addrForm.apartment.trim() || undefined,
    floor: addrForm.floor.trim() || undefined,
    orientation: addrForm.orientation.trim() || undefined,
  }
  addrSaving.value = true
  addrError.value = ''
  try {
    await createCustomerAddress(addrCustomer.value.id, dto)
    const res = await fetchCustomerAddresses(addrCustomer.value.id)
    addresses.value = res.data || []
    addrForm.fullAddress = ''
    addrForm.home = ''
    addrForm.entrance = ''
    addrForm.apartment = ''
    addrForm.floor = ''
    addrForm.orientation = ''
  } catch (e) {
    addrError.value = apiError(e, 'Manzil qo‘shilmadi')
  } finally {
    addrSaving.value = false
  }
}

async function removeAddress(a: AddressResponse) {
  if (!confirm('Manzil o‘chirilsinmi?')) return
  try {
    await deleteAddress(a.id)
    if (addrCustomer.value) {
      const res = await fetchCustomerAddresses(addrCustomer.value.id)
      addresses.value = res.data || []
    }
  } catch (e) {
    addrError.value = apiError(e, 'Manzilni o‘chirib bo‘lmadi')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="Mijozlar" :subtitle="filteredHint">
      <template #actions>
        <AppButton @click="openCreate">Qo‘shish</AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs">
      <div class="flex flex-wrap items-end gap-2">
        <AppInput
          v-model="searchPhone"
          class="min-w-64 flex-1"
          label="Telefon bo‘yicha qidirish"
          placeholder="+998..."
        />
        <AppButton @click="onSearch">Qidirish</AppButton>
        <AppButton
          variant="secondary"
          @click="
            searchPhone = '';
            customers = allCustomers
          "
        >
          Tozalash
        </AppButton>
      </div>
    </div>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
    >
      {{ error }}
    </div>

    <DataTable
      :loading="loading"
      :empty="!loading && !customers.length ? 'Mijozlar topilmadi' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Ism</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Telefon</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Jins</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Tur</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Status</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Amallar</th>
      </template>
      <tr v-for="c in customers" :key="c.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ c.fullName }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ c.phone }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ c.gender || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ c.type || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <AppBadge :tone="statusTone(c.status)">{{ c.status || '—' }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="secondary" @click="openEdit(c)">Tahrir</AppButton>
            <AppButton size="sm" variant="ghost" @click="openAddresses(c)">Manzillar</AppButton>
            <AppButton size="sm" variant="danger" @click="onDelete(c)">O‘chirish</AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <AppModal
      :open="modalOpen"
      :title="editingId ? 'Mijozni tahrirlash' : 'Yangi mijoz'"
      @close="modalOpen = false"
    >
      <div
        v-if="formError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
      >
        {{ formError }}
      </div>
      <form class="space-y-3" @submit.prevent="save">
        <AppInput v-model="form.fullName" label="To‘liq ism" />
        <AppInput v-model="form.phone" label="Telefon" />
        <AppInput v-model="form.birthDate" label="Tug‘ilgan sana" type="date" />
        <AppSelect v-model="form.gender" label="Jins">
          <option value="MALE">Erkak</option>
          <option value="FEMALE">Ayol</option>
          <option value="NOT_SELECTED">Ko‘rsatilmagan</option>
        </AppSelect>
        <AppSelect v-model="form.type" label="Mijoz turi">
          <option value="B2C">B2C</option>
          <option value="B2B">B2B</option>
        </AppSelect>
        <AppTextarea v-model="form.description" label="Izoh" />
        <div class="flex justify-end gap-2 pt-2">
          <AppButton type="button" variant="secondary" @click="modalOpen = false">Bekor</AppButton>
          <AppButton type="submit" :loading="saving">Saqlash</AppButton>
        </div>
      </form>
    </AppModal>

    <AppModal
      :open="addrOpen"
      :title="`Manzillar — ${addrCustomer?.fullName || ''}`"
      @close="addrOpen = false"
    >
      <div
        v-if="addrError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
      >
        {{ addrError }}
      </div>

      <div v-if="addrLoading" class="mb-4 text-theme-sm text-gray-500">Yuklanmoqda...</div>
      <div v-else class="mb-4 space-y-2">
        <div
          v-for="a in addresses"
          :key="a.id"
          class="flex items-start justify-between gap-3 rounded-xl border border-gray-200 px-4 py-3"
        >
          <div class="text-theme-sm text-gray-700">
            <p class="font-medium">{{ a.fullAddress }}</p>
            <p class="mt-1 text-gray-500">
              Uy: {{ a.home || '—' }} · Podyezd: {{ a.entrance || '—' }} · Xonadon:
              {{ a.apartment || '—' }} · Qavat: {{ a.floor || '—' }}
            </p>
            <p v-if="a.orientation" class="text-gray-500">Mo‘ljal: {{ a.orientation }}</p>
          </div>
          <AppButton size="sm" variant="danger" @click="removeAddress(a)">O‘chirish</AppButton>
        </div>
        <p v-if="!addresses.length" class="text-theme-sm text-gray-500">Manzillar yo‘q</p>
      </div>

      <h3 class="mb-2 text-theme-sm font-semibold text-gray-800">Yangi manzil</h3>
      <div class="space-y-3">
        <AppInput v-model="addrForm.fullAddress" label="To‘liq manzil" />
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <AppInput v-model="addrForm.home" label="Uy" />
          <AppInput v-model="addrForm.entrance" label="Podyezd" />
          <AppInput v-model="addrForm.apartment" label="Xonadon" />
          <AppInput v-model="addrForm.floor" label="Qavat" />
        </div>
        <AppInput v-model="addrForm.orientation" label="Mo‘ljal" />
        <div class="flex justify-end">
          <AppButton :loading="addrSaving" @click="addAddress">Qo‘shish</AppButton>
        </div>
      </div>
    </AppModal>
  </div>
</template>
