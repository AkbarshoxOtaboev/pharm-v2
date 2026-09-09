<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { fetchCategories, fetchSubcategories } from '@/api/categories.api'
import {
  createProduct,
  deleteProduct,
  fetchProducts,
  searchProducts,
  updateProduct,
} from '@/api/products.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type {
  CategoryResponse,
  ProductResponse,
  SubCategoryResponse,
  UnitType,
} from '@/types/api'
import { apiError, money, statusTone } from '@/utils/format'

const UNIT_TYPES: UnitType[] = ['PCS', 'BOX', 'KG']

const products = ref<ProductResponse[]>([])
const categories = ref<CategoryResponse[]>([])
const subcategories = ref<SubCategoryResponse[]>([])
const formSubs = ref<SubCategoryResponse[]>([])

const loading = ref(false)
const error = ref('')
const q = ref('')
const filterCategoryId = ref('')

const modalOpen = ref(false)
const saving = ref(false)
const formError = ref('')
const editingId = ref<number | null>(null)
const photoFile = ref<File | null>(null)

const form = reactive({
  name: '',
  price: '',
  priceCost: '',
  categoryId: '',
  subCategoryId: '',
  sortNumber: '0',
  description: '',
  unitType: 'PCS' as UnitType,
})

async function loadCategories() {
  const res = await fetchCategories()
  categories.value = res.data || []
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (q.value.trim()) {
      const res = await searchProducts(q.value.trim())
      products.value = res.data || []
    } else {
      const res = await fetchProducts(
        filterCategoryId.value ? { categoryId: Number(filterCategoryId.value) } : undefined,
      )
      products.value = res.data || []
    }
  } catch (e) {
    error.value = apiError(e, 'Mahsulotlarni yuklab bo‘lmadi')
  } finally {
    loading.value = false
  }
}

async function loadFilterSubs() {
  subcategories.value = []
  if (!filterCategoryId.value) return
  try {
    const res = await fetchSubcategories(Number(filterCategoryId.value))
    subcategories.value = res.data || []
  } catch {
    subcategories.value = []
  }
}

watch(filterCategoryId, () => {
  loadFilterSubs()
})

async function loadFormSubs(categoryId: string) {
  formSubs.value = []
  if (!categoryId) return
  try {
    const res = await fetchSubcategories(Number(categoryId))
    formSubs.value = res.data || []
  } catch {
    formSubs.value = []
  }
}

function resetForm() {
  form.name = ''
  form.price = ''
  form.priceCost = ''
  form.categoryId = ''
  form.subCategoryId = ''
  form.sortNumber = '0'
  form.description = ''
  form.unitType = 'PCS'
  photoFile.value = null
  formSubs.value = []
  editingId.value = null
  formError.value = ''
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

async function openEdit(p: ProductResponse) {
  editingId.value = p.id
  form.name = p.name
  form.price = String(p.price ?? '')
  form.priceCost = String(p.priceCost ?? '')
  form.categoryId = p.categoryResponse?.id ? String(p.categoryResponse.id) : ''
  form.subCategoryId = p.subCategoryId ? String(p.subCategoryId) : ''
  form.sortNumber = String(p.sortNumber ?? 0)
  form.description = p.description || ''
  form.unitType = (p.unitType as UnitType) || 'PCS'
  photoFile.value = null
  formError.value = ''
  await loadFormSubs(form.categoryId)
  modalOpen.value = true
}

function onPhotoChange(e: Event) {
  const input = e.target as HTMLInputElement
  photoFile.value = input.files?.[0] || null
}

function buildFormData() {
  const fd = new FormData()
  fd.append('name', form.name.trim())
  fd.append('price', form.price)
  fd.append('priceCost', form.priceCost)
  fd.append('categoryId', form.categoryId)
  if (form.subCategoryId) fd.append('subCategoryId', form.subCategoryId)
  fd.append('sortNumber', form.sortNumber || '0')
  if (form.description.trim()) fd.append('description', form.description.trim())
  fd.append('unitType', form.unitType)
  if (photoFile.value) fd.append('photo', photoFile.value)
  return fd
}

async function save() {
  formError.value = ''
  if (!form.name.trim() || !form.price || !form.priceCost || !form.categoryId) {
    formError.value = 'Nom, narx, tannarx va kategoriya majburiy'
    return
  }
  saving.value = true
  try {
    const fd = buildFormData()
    if (editingId.value) {
      await updateProduct(editingId.value, fd)
    } else {
      await createProduct(fd)
    }
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = apiError(e, 'Saqlab bo‘lmadi')
  } finally {
    saving.value = false
  }
}

async function onDelete(p: ProductResponse) {
  if (!confirm(`${p.name} o‘chirilsinmi?`)) return
  try {
    await deleteProduct(p.id)
    await load()
  } catch (e) {
    error.value = apiError(e, 'O‘chirib bo‘lmadi')
  }
}

onMounted(async () => {
  try {
    await loadCategories()
  } catch (e) {
    error.value = apiError(e, 'Kategoriyalarni yuklab bo‘lmadi')
  }
  await load()
})
</script>

<template>
  <div>
    <PageHeader title="Mahsulotlar" subtitle="Mahsulotlar katalogi">
      <template #actions>
        <AppButton @click="openCreate">Qo‘shish</AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-4">
        <AppInput v-model="q" label="Qidiruv" placeholder="Mahsulot nomi..." />
        <AppSelect v-model="filterCategoryId" label="Kategoriya">
          <option value="">Barchasi</option>
          <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
        </AppSelect>
        <div class="flex items-end gap-2 md:col-span-2">
          <AppButton class="w-full" @click="load">Filtrlash</AppButton>
          <AppButton
            variant="secondary"
            @click="
              q = '';
              filterCategoryId = '';
              load()
            "
          >
            Tozalash
          </AppButton>
        </div>
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
      :empty="!loading && !products.length ? 'Mahsulotlar topilmadi' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Nom</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Narx</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Tannarx</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Kategoriya</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Birlik</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Status</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Amallar</th>
      </template>
      <tr v-for="p in products" :key="p.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ p.name }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(p.price) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ money(p.priceCost) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          {{ p.categoryResponse?.name || '—' }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ p.unitType || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <AppBadge :tone="statusTone(p.status)">{{ p.status || '—' }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="secondary" @click="openEdit(p)">Tahrir</AppButton>
            <AppButton size="sm" variant="danger" @click="onDelete(p)">O‘chirish</AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <AppModal
      :open="modalOpen"
      :title="editingId ? 'Mahsulotni tahrirlash' : 'Yangi mahsulot'"
      @close="modalOpen = false"
    >
      <div
        v-if="formError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
      >
        {{ formError }}
      </div>
      <form class="space-y-3" @submit.prevent="save">
        <AppInput v-model="form.name" label="Nom" />
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <AppInput v-model="form.price" label="Narx" type="number" />
          <AppInput v-model="form.priceCost" label="Tannarx" type="number" />
        </div>
        <AppSelect
          v-model="form.categoryId"
          label="Kategoriya"
          @update:model-value="
            (v) => {
              form.subCategoryId = '';
              loadFormSubs(v)
            }
          "
        >
          <option value="">Tanlang</option>
          <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
        </AppSelect>
        <AppSelect v-model="form.subCategoryId" label="Subkategoriya">
          <option value="">Tanlanmagan</option>
          <option v-for="s in formSubs" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
        </AppSelect>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <AppInput v-model="form.sortNumber" label="Tartib raqami" type="number" />
          <AppSelect v-model="form.unitType" label="Birlik">
            <option v-for="u in UNIT_TYPES" :key="u" :value="u">{{ u }}</option>
          </AppSelect>
        </div>
        <AppTextarea v-model="form.description" label="Tavsif" />
        <label class="block space-y-1.5">
          <span class="text-theme-sm font-medium text-gray-700">Rasm</span>
          <input
            type="file"
            accept="image/*"
            class="block w-full text-theme-sm text-gray-700 file:mr-3 file:rounded-lg file:border-0 file:bg-brand-50 file:px-3 file:py-2 file:text-theme-sm file:font-medium file:text-brand-600"
            @change="onPhotoChange"
          />
        </label>
        <div class="flex justify-end gap-2 pt-2">
          <AppButton type="button" variant="secondary" @click="modalOpen = false">Bekor</AppButton>
          <AppButton type="submit" :loading="saving">Saqlash</AppButton>
        </div>
      </form>
    </AppModal>
  </div>
</template>
