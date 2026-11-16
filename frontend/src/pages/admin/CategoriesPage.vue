<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import {
  blockCategory,
  createCategory,
  createSubcategory,
  deleteCategory,
  deleteSubcategory,
  fetchCategories,
  fetchSubcategories,
  unblockCategory,
  updateCategory,
  updateSubcategory,
} from '@/api/categories.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppTextarea from '@/components/ui/AppTextarea.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { CategoryResponse, SubCategoryResponse } from '@/types/api'
import { apiError } from '@/utils/format'

const { t } = useI18n()
const categories = ref<CategoryResponse[]>([])
const loading = ref(false)
const error = ref('')
const pending = ref(new Set<number>())

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
    const res = await fetchCategories(true)
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

function categoryStatus(c: CategoryResponse) {
  return String(c.status || '').toUpperCase()
}

function isDeleted(c: CategoryResponse) {
  return categoryStatus(c) === 'DELETED'
}

function isActive(c: CategoryResponse) {
  return categoryStatus(c) === 'ACTIVE'
}

function isPending(c: CategoryResponse) {
  return pending.value.has(c.id)
}

function statusText(c: CategoryResponse) {
  if (isDeleted(c)) return enumLabel('status', 'DELETED')
  return enumLabel('status', isActive(c) ? 'ACTIVE' : 'INACTIVE')
}

function switchTitle(c: CategoryResponse) {
  if (isDeleted(c)) return t('categories.deletedLocked')
  return isActive(c) ? t('categories.block') : t('categories.unblock')
}

async function toggleStatus(c: CategoryResponse) {
  const status = categoryStatus(c)
  if (status === 'DELETED' || pending.value.has(c.id)) return

  const prev = c.status
  const wasBlocked = status === 'BLOCKED'
  c.status = wasBlocked ? 'ACTIVE' : 'BLOCKED'
  pending.value = new Set(pending.value).add(c.id)

  try {
    if (wasBlocked) await unblockCategory(c.id)
    else await blockCategory(c.id)
  } catch (e) {
    c.status = prev
    error.value = apiError(e, 'errors.changeStatus')
  } finally {
    const next = new Set(pending.value)
    next.delete(c.id)
    pending.value = next
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
      <tr v-for="c in categories" :key="c.id" :class="isDeleted(c) ? 'bg-gray-50/80 dark:bg-white/[0.02]' : ''">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <span class="font-medium" :class="isDeleted(c) ? 'text-gray-400 line-through decoration-gray-300 dark:text-gray-500' : ''">
            {{ c.name }}
          </span>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ c.description || t('common.empty') }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ c.subcategoryCount ?? 0 }}</td>
        <td class="px-5 py-3">
          <div class="flex items-center gap-2.5">
            <button
              type="button"
              role="switch"
              class="relative inline-flex h-6 w-11 shrink-0 rounded-full transition-colors duration-200 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500/40 focus-visible:ring-offset-2 focus-visible:ring-offset-white disabled:cursor-not-allowed dark:focus-visible:ring-offset-gray-900"
              :class="[
                isDeleted(c)
                  ? 'bg-gray-200 opacity-50 dark:bg-gray-700'
                  : isActive(c)
                    ? 'bg-success-500 hover:bg-success-600'
                    : 'bg-gray-300 hover:bg-gray-400 dark:bg-gray-700 dark:hover:bg-gray-600',
                isPending(c) ? 'opacity-60' : '',
              ]"
              :aria-checked="isActive(c)"
              :aria-label="switchTitle(c)"
              :title="switchTitle(c)"
              :disabled="isDeleted(c) || isPending(c)"
              @click="toggleStatus(c)"
            >
              <span
                class="pointer-events-none absolute top-0.5 left-0.5 size-5 rounded-full bg-white shadow-sm transition-transform duration-200"
                :class="isActive(c) ? 'translate-x-5' : 'translate-x-0'"
              />
            </button>
            <span
              class="min-w-16 text-theme-xs font-medium"
              :class="
                isDeleted(c)
                  ? 'text-error-500'
                  : isActive(c)
                    ? 'text-success-600 dark:text-success-500'
                    : 'text-gray-500 dark:text-gray-400'
              "
            >
              {{ statusText(c) }}
            </span>
          </div>
        </td>
        <td class="px-5 py-3">
          <div class="flex items-center gap-1">
            <button
              type="button"
              class="flex size-9 items-center justify-center rounded-lg text-gray-500 transition hover:bg-brand-50 hover:text-brand-500 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-gray-400 dark:text-gray-400 dark:hover:bg-brand-500/10 dark:disabled:hover:bg-transparent dark:disabled:hover:text-gray-500"
              :title="t('common.edit')"
              :aria-label="t('common.edit')"
              :disabled="isDeleted(c)"
              @click="openEditCat(c)"
            >
              <AppIcon name="pencil" class="size-4.5" />
            </button>
            <button
              type="button"
              class="flex size-9 items-center justify-center rounded-lg text-gray-500 transition hover:bg-gray-100 hover:text-gray-800 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-gray-400 dark:text-gray-400 dark:hover:bg-white/5 dark:hover:text-white/90 dark:disabled:hover:bg-transparent dark:disabled:hover:text-gray-500"
              :title="t('categories.subcategories')"
              :aria-label="t('categories.subcategories')"
              :disabled="isDeleted(c)"
              @click="openSubs(c)"
            >
              <AppIcon name="layers" class="size-4.5" />
            </button>
            <button
              type="button"
              class="flex size-9 items-center justify-center rounded-lg text-gray-500 transition hover:bg-error-50 hover:text-error-500 disabled:cursor-not-allowed disabled:opacity-40 disabled:hover:bg-transparent disabled:hover:text-gray-400 dark:text-gray-400 dark:hover:bg-error-500/10 dark:disabled:hover:bg-transparent dark:disabled:hover:text-gray-500"
              :title="t('common.delete')"
              :aria-label="t('common.delete')"
              :disabled="isDeleted(c)"
              @click="onDeleteCat(c)"
            >
              <AppIcon name="trash" class="size-4.5" />
            </button>
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
