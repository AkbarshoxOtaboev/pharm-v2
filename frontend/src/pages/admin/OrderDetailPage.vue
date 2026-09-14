<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { fetchOrder, updateOrderStatus } from '@/api/orders.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import type { OrderDetailResponse, OrderStatus } from '@/types/api'
import { apiError, money, statusTone } from '@/utils/format'

const ORDER_STATUSES: OrderStatus[] = [
  'NEW',
  'CONFIRMED',
  'PROCESSING',
  'SHIPPED',
  'PENDING',
  'DELIVERED',
  'CANCELLED',
  'DELETED',
]

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const orderId = computed(() => Number(route.params.id))

const detail = ref<OrderDetailResponse | null>(null)
const loading = ref(true)
const error = ref('')
const newStatus = ref('')
const saving = ref(false)

async function load() {
  if (!orderId.value || Number.isNaN(orderId.value)) {
    error.value = t('errors.invalidOrderId')
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  try {
    const res = await fetchOrder(orderId.value)
    detail.value = res.data
    newStatus.value = res.data?.status || ''
  } catch (e) {
    error.value = apiError(e, 'errors.loadOrder')
  } finally {
    loading.value = false
  }
}

async function saveStatus() {
  if (!detail.value?.orderId || !newStatus.value) return
  saving.value = true
  error.value = ''
  try {
    await updateOrderStatus(detail.value.orderId, newStatus.value)
    await load()
  } catch (e) {
    error.value = apiError(e, 'errors.updateStatus')
  } finally {
    saving.value = false
  }
}

watch(orderId, load)
onMounted(load)
</script>

<template>
  <div>
    <PageHeader
      :title="t('orders.titleNumber', { number: detail?.orderNumber || orderId })"
      :subtitle="t('orders.detailSubtitle')"
    >
      <template #actions>
        <AppButton variant="secondary" @click="router.push('/admin/orders')">{{ t('common.back') }}</AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div v-if="loading" class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('common.loading') }}</div>

    <template v-else-if="detail">
      <div class="mb-4 rounded-2xl border border-gray-200 bg-white dark:border-gray-800 dark:bg-white/[0.03] p-5 shadow-theme-xs">
        <div class="mb-4 flex flex-wrap items-center gap-3">
          <AppBadge :tone="statusTone(detail.status)">{{ enumLabel('orderStatus', detail.status, t('common.empty')) }}</AppBadge>
          <span class="text-theme-sm text-gray-500 dark:text-gray-400">{{ detail.createdDate || '' }}</span>
        </div>
        <div class="grid grid-cols-1 gap-3 text-theme-sm text-gray-700 md:grid-cols-2 xl:grid-cols-3">
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.customer') }}:</span> {{ detail.fullName || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.phone') }}:</span> {{ detail.phone || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.amount') }}:</span> {{ money(detail.totalAmount) }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.address') }}:</span> {{ detail.address || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.house') }}:</span> {{ detail.house || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.entrance') }}:</span> {{ detail.entrance || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.apartment') }}:</span> {{ detail.apartment || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.floor') }}:</span> {{ detail.floor || t('common.empty') }}</p>
          <p><span class="text-gray-500 dark:text-gray-400">{{ t('common.orientation') }}:</span> {{ detail.orientation || t('common.empty') }}</p>
          <p>
            <span class="text-gray-500 dark:text-gray-400">{{ t('common.courier') }}:</span>
            {{ detail.courier?.fullName || t('common.empty') }}
          </p>
        </div>

        <div class="mt-5 flex flex-wrap items-end gap-2">
          <AppSelect v-model="newStatus" :label="t('orders.changeStatus')" class="min-w-56">
            <option v-for="s in ORDER_STATUSES" :key="s" :value="s">{{ enumLabel('orderStatus', s) }}</option>
          </AppSelect>
          <AppButton :loading="saving" @click="saveStatus">{{ t('common.update') }}</AppButton>
        </div>
      </div>

      <h2 class="mb-3 text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('orders.products') }}</h2>
      <DataTable
        :loading="false"
        :empty="!(detail.items && detail.items.length) ? t('orders.noProducts') : undefined"
      >
        <template #head>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.product') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.quantity') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.price') }}</th>
          <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.bonus') }}</th>
        </template>
        <tr v-for="(item, i) in detail.items || []" :key="i">
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
            {{ item.productName || item.productId }}
          </td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ item.quantity ?? 0 }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ money(item.productCost) }}</td>
          <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ item.isBonus ? t('common.yes') : t('common.no') }}</td>
        </tr>
      </DataTable>
    </template>
  </div>
</template>
