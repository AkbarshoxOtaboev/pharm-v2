<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import {
  createCategory,
  createSubcategory,
  deleteCategory,
  deleteSubcategory,
  fetchCategories,
  fetchSubcategories,
  updateCategory,
  updateSubcategory,
} from '@/api/categories.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { CategoryResponse, SubCategoryResponse } from '@/types/api'
import { apiError, statusTone } from '@/utils/format'

const { t } = useI18n()
const categories = ref<CategoryResponse[]>([])
const loading = ref(false)
const error = ref('')

const catModal = ref(false)
const catSaving = ref(false)
const catError = ref('')
const editingCatId = ref<number | null>(null)
const catForm = reactive({ name: '', description: '' })

const subOpen = ref(false)
const subCategory = ref<CategoryResponse | null>(null)
const subs = ref<SubCategoryResponse[]>([])
const subLoading = ref(false)
const subSaving = ref(false)
const subError = ref('')
const editingSubId = ref<number | null>(null)
const subForm = reactive({ name: '', description: '' })

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchCategories()
    categories.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadCategories')
  } finally {
    loading.value = false
  }
}

function openCreateCat() {
  editingCatId.value = null
  catForm.name = ''
  catForm.description = ''
  catError.value = ''
  catModal.value = true
}

function openEditCat(c: CategoryResponse) {
  editingCatId.value = c.id
  catForm.name = c.name
  catForm.description = c.description || ''
  catError.value = ''
  catModal.value = true
}

async function saveCat() {
  if (!catForm.name.trim()) {
    catError.value = t('errors.nameRequired')
    return
  }
  catSaving.value = true
  catError.value = ''
  try {
    const dto = { name: catForm.name.trim(), description: catForm.description.trim() || undefined }
    if (editingCatId.value) {
      await updateCategory(editingCatId.value, dto)
    } else {
      await createCategory(dto)
    }
    catModal.value = false
    await load()
  } catch (e) {
    catError.value = apiError(e, 'errors.saveFailed')
  } finally {
    catSaving.value = false
  }
}

async function onDeleteCat(c: CategoryResponse) {
  if (!confirm(t('common.confirmDeleteNamed', { name: c.name }))) return
  try {
    await deleteCategory(c.id)
    await load()
  } catch (e) {
    error.value = apiError(e, 'errors.deleteFailed')
  }
}

async function openSubs(c: CategoryResponse) {
  subCategory.value = c
  subOpen.value = true
  editingSubId.value = null
  subForm.name = ''
  subForm.description = ''
  subError.value = ''
  subLoading.value = true
  try {
    const res = await fetchSubcategories(c.id)
    subs.value = res.data || []
  } catch (e) {
    subError.value = apiError(e, 'errors.loadSubcategories')
  } finally {
    subLoading.value = false
  }
}

function editSub(s: SubCategoryResponse) {
  editingSubId.value = s.id
  subForm.name = s.name
  subForm.description = s.description || ''
}

function cancelSubEdit() {
  editingSubId.value = null
  subForm.name = ''
  subForm.description = ''
}

async function saveSub() {
  if (!subCategory.value) return
  if (!subForm.name.trim()) {
    subError.value = t('errors.nameRequired')
    return
  }
  subSaving.value = true
  subError.value = ''
  try {
    const dto = {
      name: subForm.name.trim(),
      description: subForm.description.trim() || undefined,
      categoryId: subCategory.value.id,
    }
    if (editingSubId.value) {
      await updateSubcategory(editingSubId.value, dto)
    } else {
      await createSubcategory(subCategory.value.id, dto)
    }
    cancelSubEdit()
    const res = await fetchSubcategories(subCategory.value.id)
    subs.value = res.data || []
    await load()
  } catch (e) {
    subError.value = apiError(e, 'errors.saveFailed')
  } finally {
    subSaving.value = false
  }
}

async function removeSub(s: SubCategoryResponse) {
  if (!confirm(t('common.confirmDeleteNamed', { name: s.name }))) return
  try {
    await deleteSubcategory(s.id)
    if (subCategory.value) {
      const res = await fetchSubcategories(subCategory.value.id)
      subs.value = res.data || []
      await load()
    }
  } catch (e) {
    subError.value = apiError(e, 'errors.deleteFailed')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="t('categories.title')" :subtitle="t('categories.subtitle')">
      <template #actions>
        <AppButton @click="openCreateCat">{{ t('common.add') }}</AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <DataTable
      :loading="loading"
      :empty="!loading && !categories.length ? t('categories.empty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.name') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.description') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('categories.sub') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="c in categories" :key="c.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ c.name }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ c.description || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ c.subcategoryCount ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppBadge :tone="statusTone(c.status)">{{ enumLabel('status', c.status, t('common.empty')) }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="secondary" @click="openEditCat(c)">{{ t('common.edit') }}</AppButton>
            <AppButton size="sm" variant="ghost" @click="openSubs(c)">{{ t('categories.subcategories') }}</AppButton>
            <AppButton size="sm" variant="danger" @click="onDeleteCat(c)">{{ t('common.delete') }}</AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <AppModal
      :open="catModal"
      :title="editingCatId ? t('categories.editTitle') : t('categories.createTitle')"
      @close="catModal = false"
    >
      <div
        v-if="catError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ catError }}
      </div>
      <form class="space-y-3" @submit.prevent="saveCat">
        <AppInput v-model="catForm.name" :label="t('common.name')" />
        <AppTextarea v-model="catForm.description" :label="t('common.description')" />
        <div class="flex justify-end gap-2">
          <AppButton type="button" variant="secondary" @click="catModal = false">{{ t('common.cancel') }}</AppButton>
          <AppButton type="submit" :loading="catSaving">{{ t('common.save') }}</AppButton>
        </div>
      </form>
    </AppModal>

    <AppModal
      :open="subOpen"
      :title="t('categories.subcategoriesTitle', { name: subCategory?.name || '' })"
      @close="subOpen = false"
    >
      <div
        v-if="subError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ subError }}
      </div>

      <div v-if="subLoading" class="mb-4 text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>
      <div v-else class="mb-4 space-y-2">
        <div
          v-for="s in subs"
          :key="s.id"
          class="flex items-start justify-between gap-3 rounded-xl border border-gray-200 dark:border-gray-800 px-4 py-3"
        >
          <div>
            <p class="text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ s.name }}</p>
            <p class="text-theme-xs text-gray-500 dark:text-gray-400">{{ s.description || t('common.empty') }}</p>
          </div>
          <div class="flex gap-2">
            <AppButton size="sm" variant="secondary" @click="editSub(s)">{{ t('common.edit') }}</AppButton>
            <AppButton size="sm" variant="danger" @click="removeSub(s)">{{ t('common.delete') }}</AppButton>
          </div>
        </div>
        <p v-if="!subs.length" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('categories.noSubs') }}</p>
      </div>

      <h3 class="mb-2 text-theme-sm font-semibold text-gray-800 dark:text-white/90">
        {{ editingSubId ? t('categories.editSub') : t('categories.newSub') }}
      </h3>
      <div class="space-y-3">
        <AppInput v-model="subForm.name" :label="t('common.name')" />
        <AppTextarea v-model="subForm.description" :label="t('common.description')" />
        <div class="flex justify-end gap-2">
          <AppButton v-if="editingSubId" type="button" variant="secondary" @click="cancelSubEdit">
            {{ t('common.cancel') }}
          </AppButton>
          <AppButton :loading="subSaving" @click="saveSub">{{ t('common.save') }}</AppButton>
        </div>
      </div>
    </AppModal>
  </div>
</template>
