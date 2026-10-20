<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { fetchCourierTransfers } from '@/api/courierStock.api'
import { fetchCouriers } from '@/api/users.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import PaginationBar from '@/components/ui/PaginationBar.vue'
import type { CourierStockHistoryResponse, StockActionType, UserResponse } from '@/types/api'
import { apiError, dateTime, isoDate, money } from '@/utils/format'

/** 0 = all rows */
const PAGE_SIZES = [50, 100, 200, 0]
const ACTIONS: StockActionType[] = ['INCOME', 'RETURN']
const REFRESH_MS = 30_000

const { t } = useI18n()
const rows = ref<CourierStockHistoryResponse[]>([])
const couriers = ref<UserResponse[]>([])
const loading = ref(false)
const error = ref('')
const updatedAt = ref<Date | null>(null)
const page = ref(0)
const size = ref(String(PAGE_SIZES[0]))
const totalPages = ref(0)
const totalElements = ref(0)

const courierId = ref('')
const action = ref<StockActionType | ''>('')

const monthStart = () => {
  const now = new Date()
  return isoDate(new Date(now.getFullYear(), now.getMonth(), 1))
}
const from = ref(monthStart())
const to = ref(isoDate(new Date()))
const isCurrentMonth = computed(() => from.value === monthStart() && to.value === isoDate(new Date()))
const isLive = computed(() => !to.value || to.value >= isoDate(new Date()))

const updatedLabel = computed(() =>
  updatedAt.value
    ? updatedAt.value.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })
    : '',
)

function actionClass(a?: StockActionType) {
  if (a === 'INCOME') return 'bg-brand-50 text-brand-600 dark:bg-brand-500/15 dark:text-brand-400'
  if (a === 'RETURN') return 'bg-warning-50 text-warning-600 dark:bg-warning-500/15 dark:text-warning-500'
  return 'bg-gray-100 text-gray-600 dark:bg-white/5 dark:text-gray-400'
}

let loadSeq = 0
async function load(silent = false) {
  if (from.value && to.value && from.value > to.value) {
    error.value = t('errors.dateRangeInvalid')
    rows.value = []
    totalElements.value = 0
    totalPages.value = 0
    return
  }
  const seq = ++loadSeq
  if (!silent) loading.value = true
  error.value = ''
  try {
    const res = await fetchCourierTransfers({
      courierId: courierId.value ? Number(courierId.value) : undefined,
      action: action.value || undefined,
      from: from.value || undefined,
      to: to.value || undefined,
      page: page.value,
      size: Number(size.value),
    })
    if (seq !== loadSeq) return
    rows.value = res.data || []
    totalPages.value = res.meta?.totalPages ?? 1
    totalElements.value = res.meta?.totalElements ?? rows.value.length
    updatedAt.value = new Date()
  } catch (e) {
    if (seq === loadSeq) error.value = apiError(e, 'errors.loadHistory')
  } finally {
    if (seq === loadSeq) loading.value = false
  }
}

let followCurrentMonth = true
watch([from, to, size, courierId, action], () => {
  followCurrentMonth = isCurrentMonth.value
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

let timer: ReturnType<typeof setInterval> | undefined
function tick() {
  if (document.hidden) return
  if (followCurrentMonth && !isCurrentMonth.value) {
    resetToCurrentMonth()
    return
  }
  if (isLive.value) void load(true)
}

onMounted(async () => {
  void load()
  timer = setInterval(tick, REFRESH_MS)
  try {
    const res = await fetchCouriers()
    couriers.value = res.data || []
  } catch {
    couriers.value = []
  }
})
onBeforeUnmount(() => clearInterval(timer))
</script>

<template>
  <div>
    <PageHeader :title="t('courierTransfers.title')" :subtitle="t('courierTransfers.subtitle')">
      <template #actions>
        <RouterLink
          to="/admin/courier-stock"
          class="inline-flex items-center gap-2 rounded-lg border border-gray-200 bg-white px-4 py-2.5 text-theme-sm font-medium text-gray-700 hover:bg-gray-50 dark:border-gray-800 dark:bg-white/5 dark:text-gray-300 dark:hover:bg-white/10"
        >
          <AppIcon name="truck" class="size-4.5" />
          {{ t('nav.courierStock') }}
        </RouterLink>
      </template>
    </PageHeader>

    <div class="mb-4 rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
      <div class="flex flex-wrap items-end gap-3">
        <AppSelect v-model="courierId" icon="user" :label="t('common.courier')" class="min-w-52">
          <option value="">{{ t('courierTransfers.allCouriers') }}</option>
          <option v-for="c in couriers" :key="c.id" :value="String(c.id)">{{ c.fullName }}</option>
        </AppSelect>
        <div class="space-y-1.5">
          <span class="block text-theme-sm font-medium text-gray-700 dark:text-gray-300">{{ t('courierStock.action') }}</span>
          <div class="flex h-11 items-center gap-1 rounded-lg bg-gray-100 p-1 dark:bg-white/5">
            <button
              v-for="a in ['', ...ACTIONS] as const"
              :key="a || 'all'"
              type="button"
              class="flex h-full items-center gap-1.5 rounded-md px-3 text-theme-sm font-medium transition"
              :class="
                action === a
                  ? 'bg-white text-gray-800 shadow-theme-xs dark:bg-gray-800 dark:text-white/90'
                  : 'text-gray-500 hover:text-gray-700 dark:text-gray-400'
              "
              @click="action = a"
            >
              <AppIcon v-if="a === 'INCOME'" name="arrow-up-right" class="size-4 text-brand-500" />
              <AppIcon v-else-if="a === 'RETURN'" name="arrow-down-left" class="size-4 text-warning-500" />
              {{ a ? t(`courierTransfers.action.${a}`) : t('common.all') }}
            </button>
          </div>
        </div>
        <AppInput v-model="from" icon="calendar" :label="t('common.from')" type="date" />
        <AppInput v-model="to" icon="calendar" :label="t('common.to')" type="date" />
        <AppSelect v-model="size" :label="t('common.limit')" class="w-32">
          <option v-for="s in PAGE_SIZES" :key="s" :value="String(s)">{{ s ? s : t('common.all') }}</option>
        </AppSelect>
        <AppButton v-if="!isCurrentMonth" variant="secondary" @click="resetToCurrentMonth">
          <AppIcon name="calendar" class="size-4.5" />
          {{ t('saleLogs.currentMonth') }}
        </AppButton>
        <div class="ml-auto flex items-center gap-2 pb-3 text-theme-xs text-gray-500 dark:text-gray-400">
          <span class="relative flex size-2.5">
            <span v-if="isLive" class="absolute inline-flex size-full animate-ping rounded-full bg-success-500 opacity-60" />
            <span class="relative inline-flex size-2.5 rounded-full" :class="isLive ? 'bg-success-500' : 'bg-gray-300 dark:bg-gray-600'" />
          </span>
          <span v-if="isLive">{{ t('saleLogs.live') }}</span>
          <span v-if="updatedLabel">· {{ t('saleLogs.updatedAt', { time: updatedLabel }) }}</span>
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
      :empty="!loading && !rows.length ? t('courierStock.historyEmpty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.date') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.courier') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.action') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.before') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.change') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('courierStock.after') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
        <th class="px-5 py-3 text-right text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.amount') }}</th>
      </template>
      <tr v-for="h in rows" :key="h.id">
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ dateTime(h.createdAt) || t('common.empty') }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-theme-sm text-gray-800 dark:text-white/90">
          <span class="flex items-center gap-2">
            <AppIcon name="user" class="size-4 text-gray-400" />
            {{ h.courierName || t('common.empty') }}
          </span>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-800 dark:text-white/90">
          <p>{{ h.productName || t('common.empty') }}</p>
          <p v-if="h.comment" class="max-w-56 truncate text-theme-xs text-gray-500 dark:text-gray-400" :title="h.comment">{{ h.comment }}</p>
        </td>
        <td class="px-5 py-3 text-theme-sm">
          <span class="inline-flex items-center gap-1.5 whitespace-nowrap rounded-full px-2.5 py-0.5 text-theme-xs font-medium" :class="actionClass(h.actionType)">
            <AppIcon v-if="h.actionType === 'INCOME'" name="arrow-up-right" class="size-3.5" />
            <AppIcon v-else-if="h.actionType === 'RETURN'" name="arrow-down-left" class="size-3.5" />
            {{ h.actionType === 'INCOME' || h.actionType === 'RETURN'
              ? t(`courierTransfers.action.${h.actionType}`)
              : enumLabel('stockAction', h.actionType, t('common.empty')) }}
          </span>
        </td>
        <td class="px-5 py-3 text-right text-theme-sm text-gray-500 dark:text-gray-400">{{ money(h.beforeQuantity) }}</td>
        <td
          class="px-5 py-3 text-right text-theme-sm font-semibold"
          :class="Number(h.changeQuantity) < 0 ? 'text-warning-600' : 'text-brand-500'"
        >
          {{ Number(h.changeQuantity) > 0 ? '+' : '' }}{{ money(h.changeQuantity) }}
        </td>
        <td class="px-5 py-3 text-right text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ money(h.afterQuantity) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm text-gray-700 dark:text-gray-300">{{ money(h.productPrice) }}</td>
        <td class="whitespace-nowrap px-5 py-3 text-right text-theme-sm font-medium text-gray-800 dark:text-white/90">{{ money(h.totalAmount) }}</td>
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
