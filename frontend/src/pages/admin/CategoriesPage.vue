<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
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
    error.value = apiError(e, 'Kategoriyalarni yuklab bo‘lmadi')
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
    catError.value = 'Nom majburiy'
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
    catError.value = apiError(e, 'Saqlab bo‘lmadi')
  } finally {
    catSaving.value = false
  }
}

async function onDeleteCat(c: CategoryResponse) {
  if (!confirm(`${c.name} o‘chirilsinmi?`)) return
  try {
    await deleteCategory(c.id)
    await load()
  } catch (e) {
    error.value = apiError(e, 'O‘chirib bo‘lmadi')
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
    subError.value = apiError(e, 'Subkategoriyalarni yuklab bo‘lmadi')
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
    subError.value = 'Nom majburiy'
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
    subError.value = apiError(e, 'Saqlab bo‘lmadi')
  } finally {
    subSaving.value = false
  }
}

async function removeSub(s: SubCategoryResponse) {
  if (!confirm(`${s.name} o‘chirilsinmi?`)) return
  try {
    await deleteSubcategory(s.id)
    if (subCategory.value) {
      const res = await fetchSubcategories(subCategory.value.id)
      subs.value = res.data || []
      await load()
    }
  } catch (e) {
    subError.value = apiError(e, 'O‘chirib bo‘lmadi')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader title="Kategoriyalar" subtitle="Kategoriya va subkategoriyalar">
      <template #actions>
        <AppButton @click="openCreateCat">Qo‘shish</AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
    >
      {{ error }}
    </div>

    <DataTable
      :loading="loading"
      :empty="!loading && !categories.length ? 'Kategoriyalar yo‘q' : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Nom</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Tavsif</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Sub</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Status</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500">Amallar</th>
      </template>
      <tr v-for="c in categories" :key="c.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ c.name }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ c.description || '—' }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">{{ c.subcategoryCount ?? 0 }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <AppBadge :tone="statusTone(c.status)">{{ c.status || '—' }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="secondary" @click="openEditCat(c)">Tahrir</AppButton>
            <AppButton size="sm" variant="ghost" @click="openSubs(c)">Subkategoriyalar</AppButton>
            <AppButton size="sm" variant="danger" @click="onDeleteCat(c)">O‘chirish</AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <AppModal
      :open="catModal"
      :title="editingCatId ? 'Kategoriyani tahrirlash' : 'Yangi kategoriya'"
      @close="catModal = false"
    >
      <div
        v-if="catError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
      >
        {{ catError }}
      </div>
      <form class="space-y-3" @submit.prevent="saveCat">
        <AppInput v-model="catForm.name" label="Nom" />
        <AppTextarea v-model="catForm.description" label="Tavsif" />
        <div class="flex justify-end gap-2">
          <AppButton type="button" variant="secondary" @click="catModal = false">Bekor</AppButton>
          <AppButton type="submit" :loading="catSaving">Saqlash</AppButton>
        </div>
      </form>
    </AppModal>

    <AppModal
      :open="subOpen"
      :title="`Subkategoriyalar — ${subCategory?.name || ''}`"
      @close="subOpen = false"
    >
      <div
        v-if="subError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
      >
        {{ subError }}
      </div>

      <div v-if="subLoading" class="mb-4 text-theme-sm text-gray-500">Yuklanmoqda...</div>
      <div v-else class="mb-4 space-y-2">
        <div
          v-for="s in subs"
          :key="s.id"
          class="flex items-start justify-between gap-3 rounded-xl border border-gray-200 px-4 py-3"
        >
          <div>
            <p class="text-theme-sm font-medium text-gray-800">{{ s.name }}</p>
            <p class="text-theme-xs text-gray-500">{{ s.description || '—' }}</p>
          </div>
          <div class="flex gap-2">
            <AppButton size="sm" variant="secondary" @click="editSub(s)">Tahrir</AppButton>
            <AppButton size="sm" variant="danger" @click="removeSub(s)">O‘chirish</AppButton>
          </div>
        </div>
        <p v-if="!subs.length" class="text-theme-sm text-gray-500">Subkategoriyalar yo‘q</p>
      </div>

      <h3 class="mb-2 text-theme-sm font-semibold text-gray-800">
        {{ editingSubId ? 'Tahrirlash' : 'Yangi subkategoriya' }}
      </h3>
      <div class="space-y-3">
        <AppInput v-model="subForm.name" label="Nom" />
        <AppTextarea v-model="subForm.description" label="Tavsif" />
        <div class="flex justify-end gap-2">
          <AppButton v-if="editingSubId" type="button" variant="secondary" @click="cancelSubEdit">
            Bekor
          </AppButton>
          <AppButton :loading="subSaving" @click="saveSub">Saqlash</AppButton>
        </div>
      </div>
    </AppModal>
  </div>
</template>
