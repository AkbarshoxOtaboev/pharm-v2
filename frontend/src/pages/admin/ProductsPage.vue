<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
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
import AppIcon from '@/components/ui/AppIcon.vue'
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
import { apiError, fileUrl, money, statusTone } from '@/utils/format'

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
const photoPreview = ref('')
const existingPhoto = ref('')
const brokenPhotos = reactive(new Set<number>())

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

const shownPhoto = computed(() => photoPreview.value || existingPhoto.value)

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

function setPhoto(file: File | null) {
  if (photoPreview.value) URL.revokeObjectURL(photoPreview.value)
  photoFile.value = file
  photoPreview.value = file ? URL.createObjectURL(file) : ''
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
  setPhoto(null)
  existingPhoto.value = ''
  formSubs.value = []
  editingId.value = null
  formError.value = ''
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

async function openEdit(p: ProductResponse) {
  resetForm()
  editingId.value = p.id
  form.name = p.name
  form.price = String(p.price ?? '')
  form.priceCost = String(p.priceCost ?? '')
  form.categoryId = p.categoryResponse?.id ? String(p.categoryResponse.id) : ''
  form.subCategoryId = p.subCategoryId ? String(p.subCategoryId) : ''
  form.sortNumber = String(p.sortNumber ?? 0)
  form.description = p.description || ''
  form.unitType = (p.unitType as UnitType) || 'PCS'
  existingPhoto.value = fileUrl(p.photo)
  await loadFormSubs(form.categoryId)
  modalOpen.value = true
}

function onPhotoChange(e: Event) {
  const input = e.target as HTMLInputElement
  setPhoto(input.files?.[0] || null)
  input.value = ''
}

function onCategoryChange(v: string) {
  form.categoryId = v
  form.subCategoryId = ''
  loadFormSubs(v)
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

onBeforeUnmount(() => setPhoto(null))
</script>

<template>
  <div>
    <PageHeader :title="t('products.title')" :subtitle="t('products.subtitle')">
      <template #actions>
        <AppButton @click="openCreate">
          <AppIcon name="plus" class="size-4.5" />
          {{ t('products.addProduct') }}
        </AppButton>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-4 shadow-theme-xs">
      <div class="grid grid-cols-1 gap-3 md:grid-cols-4">
        <AppInput v-model="q" icon="search" :label="t('products.search')" :placeholder="t('products.searchPlaceholder')" />
        <AppSelect v-model="filterCategoryId" icon="layers" :label="t('products.category')">
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
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="p in products" :key="p.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <div class="flex items-center gap-3">
            <img
              v-if="fileUrl(p.photo) && !brokenPhotos.has(p.id)"
              :src="fileUrl(p.photo)"
              :alt="p.name"
              class="size-9 shrink-0 rounded-lg object-cover ring-1 ring-gray-200 dark:ring-gray-800"
              @error="brokenPhotos.add(p.id)"
            />
            <span
              v-else
              class="flex size-9 shrink-0 items-center justify-center rounded-lg bg-brand-50 text-brand-500 dark:bg-brand-500/15 dark:text-brand-400"
            >
              <AppIcon name="box" class="size-4.5" />
            </span>
            <span class="font-medium text-gray-800 dark:text-white/90">{{ p.name }}</span>
          </div>
        </td>
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(p.price) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(p.priceCost) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ p.categoryResponse?.name || t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ enumLabel('unitType', p.unitType, t('common.empty')) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppBadge :tone="statusTone(p.status)">{{ enumLabel('status', p.status, t('common.empty')) }}</AppBadge>
        </td>
        <td class="px-5 py-3">
          <div class="flex items-center justify-end gap-2">
            <button
              type="button"
              class="flex size-9 items-center justify-center rounded-lg border border-warning-500 text-warning-500 transition hover:bg-warning-500 hover:text-white"
              :title="t('common.edit')"
              :aria-label="t('common.edit')"
              @click="openEdit(p)"
            >
              <AppIcon name="pencil-alt" class="size-4.5" />
            </button>
            <button
              type="button"
              class="flex size-9 items-center justify-center rounded-lg border border-error-500 text-error-500 transition hover:bg-error-500 hover:text-white"
              :title="t('common.delete')"
              :aria-label="t('common.delete')"
              @click="onDelete(p)"
            >
              <AppIcon name="trash" class="size-4.5" />
            </button>
          </div>
        </td>
      </tr>
    </DataTable>

    <AppModal :open="modalOpen" @close="modalOpen = false">
      <template #title>
        <span
          class="flex size-10 items-center justify-center rounded-xl"
          :class="
            editingId
              ? 'bg-warning-50 text-warning-500 dark:bg-warning-500/15'
              : 'bg-brand-50 text-brand-500 dark:bg-brand-500/15 dark:text-brand-400'
          "
        >
          <AppIcon :name="editingId ? 'pencil-alt' : 'box'" />
        </span>
        <span>
          {{ editingId ? t('products.editTitle') : t('products.createTitle') }}
          <span class="block text-theme-xs font-normal text-gray-500 dark:text-gray-400">{{ t('products.formHint') }}</span>
        </span>
      </template>

      <div
        v-if="formError"
        class="mb-4 flex items-center gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        <AppIcon name="alert" class="size-4.5" />
        {{ formError }}
      </div>

      <form class="space-y-5" @submit.prevent="save">
        <section class="space-y-3">
          <h3 class="flex items-center gap-2 text-theme-sm font-semibold text-gray-800 dark:text-white/90">
            <AppIcon name="tag" class="size-4.5 text-brand-500" />
            {{ t('products.sectionMain') }}
          </h3>
          <AppInput v-model="form.name" icon="box" :label="`${t('common.name')} *`" :placeholder="t('products.namePlaceholder')" />
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <AppSelect
              :model-value="form.categoryId"
              icon="layers"
              :label="`${t('products.category')} *`"
              @update:model-value="onCategoryChange"
            >
              <option value="">{{ t('common.select') }}</option>
              <option v-for="c in categories" :key="c.id" :value="String(c.id)">{{ c.name }}</option>
            </AppSelect>
            <AppSelect
              v-model="form.subCategoryId"
              icon="layers"
              :label="t('products.subcategory')"
              :disabled="!formSubs.length"
            >
              <option value="">{{ t('common.notSelected') }}</option>
              <option v-for="s in formSubs" :key="s.id" :value="String(s.id)">{{ s.name }}</option>
            </AppSelect>
          </div>
        </section>

        <section class="space-y-3 border-t border-gray-100 pt-4 dark:border-gray-800">
          <h3 class="flex items-center gap-2 text-theme-sm font-semibold text-gray-800 dark:text-white/90">
            <AppIcon name="cash" class="size-4.5 text-brand-500" />
            {{ t('products.sectionPrice') }}
          </h3>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <AppInput v-model="form.price" icon="cash" type="number" :label="`${t('common.price')} *`" :suffix="t('common.currency')" placeholder="0" />
            <AppInput v-model="form.priceCost" icon="receipt" type="number" :label="`${t('common.cost')} *`" :suffix="t('common.currency')" placeholder="0" />
          </div>
        </section>

        <section class="space-y-3 border-t border-gray-100 pt-4 dark:border-gray-800">
          <h3 class="flex items-center gap-2 text-theme-sm font-semibold text-gray-800 dark:text-white/90">
            <AppIcon name="ruler" class="size-4.5 text-brand-500" />
            {{ t('products.sectionExtra') }}
          </h3>
          <div class="grid grid-cols-1 gap-3 sm:grid-cols-2">
            <div class="space-y-1.5">
              <span class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">{{ t('common.unit') }}</span>
              <div class="grid grid-cols-3 gap-2">
                <button
                  v-for="u in UNIT_TYPES"
                  :key="u"
                  type="button"
                  class="h-11 rounded-lg border text-theme-sm font-medium transition"
                  :class="
                    form.unitType === u
                      ? 'border-brand-500 bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400'
                      : 'border-gray-300 text-gray-600 hover:border-brand-300 dark:border-gray-700 dark:text-gray-400'
                  "
                  @click="form.unitType = u"
                >
                  {{ enumLabel('unitType', u) }}
                </button>
              </div>
            </div>
            <AppInput v-model="form.sortNumber" icon="hash" type="number" :label="t('products.sortNumber')" />
          </div>
          <AppTextarea v-model="form.description" :label="t('common.description')" :placeholder="t('products.descriptionPlaceholder')" />
        </section>

        <section class="space-y-3 border-t border-gray-100 pt-4 dark:border-gray-800">
          <h3 class="flex items-center gap-2 text-theme-sm font-semibold text-gray-800 dark:text-white/90">
            <AppIcon name="image" class="size-4.5 text-brand-500" />
            {{ t('common.photo') }}
          </h3>
          <div class="flex items-center gap-4">
            <div class="relative size-24 shrink-0 overflow-hidden rounded-xl border border-gray-200 bg-gray-50 dark:border-gray-800 dark:bg-white/[0.03]">
              <img
                v-if="shownPhoto"
                :src="shownPhoto"
                :alt="form.name"
                class="size-full object-cover"
                @error="existingPhoto = ''"
              />
              <span v-else class="flex size-full items-center justify-center text-gray-300 dark:text-gray-600">
                <AppIcon name="image" class="size-8" />
              </span>
              <button
                v-if="photoFile"
                type="button"
                class="absolute right-1 top-1 flex size-6 items-center justify-center rounded-full bg-gray-900/60 text-white hover:bg-gray-900/80"
                :title="t('common.delete')"
                @click="setPhoto(null)"
              >
                <AppIcon name="x" class="size-3.5" />
              </button>
            </div>
            <label
              class="flex flex-1 cursor-pointer flex-col items-center justify-center gap-1.5 rounded-xl border-2 border-dashed border-gray-300 px-4 py-5 text-center transition hover:border-brand-400 hover:bg-brand-50/40 dark:border-gray-700 dark:hover:bg-brand-500/5"
            >
              <AppIcon name="upload" class="size-6 text-brand-500" />
              <span class="text-theme-sm font-medium text-gray-700 dark:text-gray-300">
                {{ shownPhoto ? t('products.changePhoto') : t('products.uploadPhoto') }}
              </span>
              <span class="max-w-full truncate text-theme-xs text-gray-500 dark:text-gray-400">
                {{ photoFile?.name || t('products.photoHint') }}
              </span>
              <input type="file" accept="image/*" class="sr-only" @change="onPhotoChange" />
            </label>
          </div>
        </section>

        <div class="flex justify-end gap-2 border-t border-gray-100 pt-4 dark:border-gray-800">
          <AppButton type="button" variant="secondary" @click="modalOpen = false">
            <AppIcon name="x" class="size-4.5" />
            {{ t('common.cancel') }}
          </AppButton>
          <AppButton type="submit" :loading="saving">
            <AppIcon v-if="!saving" name="check" class="size-4.5" />
            {{ t('common.save') }}
          </AppButton>
        </div>
      </form>
    </AppModal>
  </div>
</template>
