<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { fetchStoreHistoryPage } from '@/api/store.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import PaginationBar from '@/components/ui/PaginationBar.vue'
import type { StoreHistoryResponse } from '@/types/api'
import { apiError, dateTime, isoDate, money } from '@/utils/format'

/** 0 = all rows */
const PAGE_SIZES = [50, 100, 200, 0]

const { t } = useI18n()
const rows = ref<StoreHistoryResponse[]>([])
const loading = ref(false)
const error = ref('')
const page = ref(0)
const size = ref(String(PAGE_SIZES[0]))
const totalPages = ref(0)
const totalElements = ref(0)

const monthStart = () => {
  const now = new Date()
  return isoDate(new Date(now.getFullYear(), now.getMonth(), 1))
}
const from = ref(monthStart())
const to = ref(isoDate(new Date()))
const isCurrentMonth = computed(() => from.value === monthStart() && to.value === isoDate(new Date()))

const pageQty = computed(() => rows.value.reduce((s, r) => s + Number(r.quantity ?? 0), 0))
const pageSum = computed(() => rows.value.reduce((s, r) => s + Number(r.totalAmount ?? 0), 0))

let loadSeq = 0
async function load() {
  if (from.value && to.value && from.value > to.value) {
    error.value = t('errors.dateRangeInvalid')
    rows.value = []
    totalElements.value = 0
    totalPages.value = 0
    return
  }
  const seq = ++loadSeq
  loading.value = true
  error.value = ''
  try {
    const res = await fetchStoreHistoryPage({
      from: from.value || undefined,
      to: to.value || undefined,
      page: page.value,
      size: Number(size.value),
    })
    if (seq !== loadSeq) return
    rows.value = res.data || []
    totalPages.value = res.meta?.totalPages ?? 1
    totalElements.value = res.meta?.totalElements ?? rows.value.length
  } catch (e) {
    if (seq === loadSeq) error.value = apiError(e, 'errors.loadHistory')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

watch([from, to, size], () => {
  page.value = 0
  void load()
})

function goToPage(p: number) {
  page.value = p
  void load()
}

function resetToCurrentMonth() {
  from.value = monthStart()
  to.value = isoDate(new Date())
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="t('storeHistory.title')" :subtitle="t('storeHistory.subtitle')">
      <template #actions>
        <RouterLink
          to="/admin/store"
          class="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2.5 text-theme-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-800 dark:bg-white/5 dark:text-gray-300 dark:hover:bg-white/10"
        >
          <AppIcon name="warehouse" class="size-4.5" />
          {{ t('nav.store') }}
        </RouterLink>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-3">
        <AppInput v-model="from" icon="calendar" :label="t('common.from')" type="date" />
        <AppInput v-model="to" icon="calendar" :label="t('common.to')" type="date" />
        <AppSelect v-model="size" :label="t('common.limit')" class="w-32">
          <option v-for="s in PAGE_SIZES" :key="s" :value="String(s)">{{ s ? s : t('common.all') }}</option>
        </AppSelect>
        <AppButton v-if="!isCurrentMonth" variant="secondary" @click="resetToCurrentMonth">
          <AppIcon name="calendar" class="size-4.5" />
          {{ t('saleLogs.currentMonth') }}
        </AppButton>
        <div class="ml-auto flex flex-wrap gap-2 pb-0.5">
          <div class="rounded-xl bg-brand-50 px-4 py-2 dark:bg-brand-500/10">
            <p class="text-theme-xs text-brand-600 dark:text-brand-400">{{ t('storeHistory.pageQty') }}</p>
            <p class="text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ money(pageQty) }}</p>
          </div>
          <div class="rounded-xl bg-success-50 px-4 py-2 dark:bg-success-500/10">
            <p class="text-theme-xs text-success-600">{{ t('storeHistory.pageSum') }}</p>
            <p class="text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ money(pageSum) }}</p>
          </div>
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
      :empty="!loading && !rows.length ? t('store.historyEmpty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('store.arrivalDate') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.cost') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.comment') }}</th>
      </template>
      <tr v-for="h in rows" :key="h.id">
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <span class="flex items-center gap-2">
            <AppIcon name="calendar" class="size-4 text-gray-400" />
            {{ dateTime(h.dateOfArrival) || t('common.empty') }}
          </span>
        </td>
        <td class="px-5 py-3 text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ h.productName || t('common.empty') }}</td>
        <td class="px-5 py-3 text-right text-theme-sm">
          <span class="inline-flex items-center gap-1 font-medium text-success-600">
            <AppIcon name="plus" class="size-3.5" />{{ money(h.quantity) }}
          </span>
        </td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ money(h.priceCost) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ money(h.totalAmount) }}</td>
        <td class="max-w-64 px-5 py-3 text-theme-sm text-gray-500 dark:text-gray-400">
          <p class="truncate" :title="h.comment">{{ h.comment || t('common.empty') }}</p>
        </td>
      </tr>
    </DataTable>

    <PaginationBar
      :page="page"
      :total-pages="totalPages"
      :total-elements="totalElements"
      :loading="loading"
      @update:page="goToPage"
    />
  </div>
</template>
