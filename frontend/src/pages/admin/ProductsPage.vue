<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
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

const { t } = useI18n()
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
    error.value = apiError(e, 'errors.loadProducts')
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
    formError.value = t('errors.productRequiredFields')
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
    formError.value = apiError(e, 'errors.saveFailed')
  } finally {
    saving.value = false
  }
}

async function onDelete(p: ProductResponse) {
  if (!confirm(t('common.confirmDeleteNamed', { name: p.name }))) return
  try {
    await deleteProduct(p.id)
    await load()
  } catch (e) {
    error.value = apiError(e, 'errors.deleteFailed')
  }
}

onMounted(async () => {
  try {
    await loadCategories()
  } catch (e) {
    error.value = apiError(e, 'errors.loadCategories')
  }
  await load()
})
</script>

<template>
  <div>
    <PageHeader :title="t('products.title')" :subtitle="t('products.subtitle')">
      <template #actions>
        <AppButton @click="openCreate">{{ t('common.add') }}</AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-4 shadow-theme-xs">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-4">
        <AppInput v-model="q" :label="t('products.search')" :placeholder="t('products.searchPlaceholder')" />
        <AppSelect v-model="filterCategoryId" :label="t('products.category')">
          <option value="">{{ t('common.all') }}</option>
          <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
        </AppSelect>
        <div class="flex items-end gap-2 md:col-span-2">
          <AppButton class="w-full" @click="load">{{ t('common.filter') }}</AppButton>
          <AppButton
            variant="secondary"
            @click="
              q = '';
              filterCategoryId = '';
              load()
            "
          >
            {{ t('common.clear') }}
          </AppButton>
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
      :empty="!loading && !products.length ? t('products.notFound') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.name') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.cost') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('products.category') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.unit') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="p in products" :key="p.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ p.name }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(p.price) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(p.priceCost) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ p.categoryResponse?.name || t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ enumLabel('unitType', p.unitType, t('common.empty')) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppBadge :tone="statusTone(p.status)">{{ enumLabel('status', p.status, t('common.empty')) }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="secondary" @click="openEdit(p)">{{ t('common.edit') }}</AppButton>
            <AppButton size="sm" variant="danger" @click="onDelete(p)">{{ t('common.delete') }}</AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <AppModal
      :open="modalOpen"
      :title="editingId ? t('products.editTitle') : t('products.createTitle')"
      @close="modalOpen = false"
    >
      <div
        v-if="formError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ formError }}
      </div>
      <form class="space-y-3" @submit.prevent="save">
        <AppInput v-model="form.name" :label="t('common.name')" />
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <AppInput v-model="form.price" :label="t('common.price')" type="number" />
          <AppInput v-model="form.priceCost" :label="t('common.cost')" type="number" />
        </div>
        <AppSelect
          v-model="form.categoryId"
          :label="t('products.category')"
          @update:model-value="
            (v) => {
              form.subCategoryId = '';
              loadFormSubs(v)
            }
          "
        >
          <option value="">{{ t('common.select') }}</option>
          <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
        </AppSelect>
        <AppSelect v-model="form.subCategoryId" :label="t('products.subcategory')">
          <option value="">{{ t('common.notSelected') }}</option>
          <option v-for="s in formSubs" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
        </AppSelect>
        <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
          <AppInput v-model="form.sortNumber" :label="t('products.sortNumber')" type="number" />
          <AppSelect v-model="form.unitType" :label="t('common.unit')">
            <option v-for="u in UNIT_TYPES" :key="u" :value="u">{{ enumLabel('unitType', u) }}</option>
          </AppSelect>
        </div>
        <AppTextarea v-model="form.description" :label="t('common.description')" />
        <label class="block space-y-1.5">
          <span class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">{{ t('common.photo') }}</span>
          <input
            type="file"
            accept="image/*"
            class="block w-full text-theme-sm text-gray-700 file:mr-3 file:rounded-lg file:border-0 file:bg-brand-50 file:px-3 file:py-2 file:text-theme-sm file:font-medium file:text-brand-600 dark:text-gray-300 dark:file:bg-brand-500/15 dark:file:text-brand-400"
            @change="onPhotoChange"
          />
        </label>
        <div class="flex justify-end gap-2 pt-2">
          <AppButton type="button" variant="secondary" @click="modalOpen = false">{{ t('common.cancel') }}</AppButton>
          <AppButton type="submit" :loading="saving">{{ t('common.save') }}</AppButton>
        </div>
      </form>
    </AppModal>
  </div>
</template>
