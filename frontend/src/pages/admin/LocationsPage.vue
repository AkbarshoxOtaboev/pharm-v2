<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import {
  createDistrict,
  createRegion,
  deleteDistrict,
  deleteRegion,
  fetchDistricts,
  fetchRegions,
  type DistrictItem,
  type RegionItem,
} from '@/api/locations.api'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import { apiError } from '@/utils/format'

const { t } = useI18n()
const error = ref('')
const regions = ref<RegionItem[]>([])
const districts = ref<DistrictItem[]>([])
const regionId = ref<number | null>(null)
const regionName = ref('')
const districtName = ref('')
const saving = ref('')

async function loadRegions() {
  const res = await fetchRegions()
  regions.value = res.data || []
  if (regionId.value && !regions.value.some((r) => r.id === regionId.value)) {
    regionId.value = null
  }
}

async function loadDistricts() {
  if (!regionId.value) {
    districts.value = []
    return
  }
  const res = await fetchDistricts(regionId.value)
  districts.value = res.data || []
}

async function refresh() {
  error.value = ''
  try {
    await loadRegions()
    await loadDistricts()
  } catch (e) {
    error.value = apiError(e, 'errors.loadData')
  }
}

watch(regionId, () => {
  void loadDistricts().catch((e) => {
    error.value = apiError(e, 'errors.loadData')
  })
})

async function addRegion() {
  if (!regionName.value.trim()) return
  saving.value = 'region'
  error.value = ''
  try {
    const res = await createRegion(regionName.value.trim())
    regionName.value = ''
    await loadRegions()
    if (res.data) regionId.value = res.data.id
  } catch (e) {
    error.value = apiError(e, 'errors.saveFailed')
  } finally {
    saving.value = ''
  }
}

async function addDistrict() {
  if (!regionId.value || !districtName.value.trim()) return
  saving.value = 'district'
  error.value = ''
  try {
    await createDistrict(districtName.value.trim(), regionId.value)
    districtName.value = ''
    await loadDistricts()
  } catch (e) {
    error.value = apiError(e, 'errors.saveFailed')
  } finally {
    saving.value = ''
  }
}

async function removeRegion(item: RegionItem) {
  if (!confirm(t('common.confirmDeleteNamed', { name: item.name }))) return
  try {
    await deleteRegion(item.id)
    if (regionId.value === item.id) regionId.value = null
    await refresh()
  } catch (e) {
    error.value = apiError(e, 'errors.deleteFailed')
  }
}

async function removeDistrict(item: DistrictItem) {
  if (!confirm(t('common.confirmDeleteNamed', { name: item.name }))) return
  try {
    await deleteDistrict(item.id)
    await loadDistricts()
  } catch (e) {
    error.value = apiError(e, 'errors.deleteFailed')
  }
}

onMounted(refresh)
</script>

<template>
  <div>
    <PageHeader :title="t('locations.title')" :subtitle="t('locations.subtitle')" />

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <div class="grid grid-cols-1 gap-4 xl:grid-cols-2">
      <section class="rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
        <h2 class="mb-3 text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ t('locations.regions') }}</h2>
        <form class="mb-3 flex gap-2" @submit.prevent="addRegion">
          <AppInput v-model="regionName" :placeholder="t('locations.regionName')" class="min-w-0 flex-1" />
          <AppButton type="submit" :loading="saving === 'region'">{{ t('common.add') }}</AppButton>
        </form>
        <ul class="space-y-1">
          <li v-for="item in regions" :key="item.id">
            <div
              class="flex items-center gap-2 rounded-lg px-2 py-1.5"
              :class="regionId === item.id ? 'bg-brand-50 dark:bg-brand-500/10' : 'hover:bg-gray-50 dark:hover:bg-white/5'"
            >
              <button type="button" class="min-w-0 flex-1 truncate text-left text-theme-sm text-gray-800 dark:text-white/90" @click="regionId = item.id">
                {{ item.name }}
              </button>
              <button type="button" class="text-gray-400 hover:text-error-500" :title="t('common.delete')" @click="removeRegion(item)">
                <AppIcon name="trash" class="size-4" />
              </button>
            </div>
          </li>
          <li v-if="!regions.length" class="px-2 py-3 text-theme-sm text-gray-500">{{ t('locations.emptyRegions') }}</li>
        </ul>
      </section>

      <section class="rounded-2xl border border-gray-200 bg-white p-4 shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]">
        <h2 class="mb-3 text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ t('locations.place') }}</h2>
        <form class="mb-3 flex gap-2" @submit.prevent="addDistrict">
          <AppInput v-model="districtName" :placeholder="t('locations.districtName')" :disabled="!regionId" class="min-w-0 flex-1" />
          <AppButton type="submit" :disabled="!regionId" :loading="saving === 'district'">{{ t('common.add') }}</AppButton>
        </form>
        <p v-if="!regionId" class="px-2 py-3 text-theme-sm text-gray-500">{{ t('locations.pickRegion') }}</p>
        <ul v-else class="space-y-1">
          <li v-for="item in districts" :key="item.id" class="flex items-center gap-2 rounded-lg px-2 py-1.5 hover:bg-gray-50 dark:hover:bg-white/5">
            <span class="min-w-0 flex-1 truncate text-theme-sm text-gray-800 dark:text-white/90">{{ item.name }}</span>
            <button type="button" class="text-gray-400 hover:text-error-500" :title="t('common.delete')" @click="removeDistrict(item)">
              <AppIcon name="trash" class="size-4" />
            </button>
          </li>
          <li v-if="!districts.length" class="px-2 py-3 text-theme-sm text-gray-500">{{ t('locations.emptyDistricts') }}</li>
        </ul>
      </section>
    </div>
  </div>
</template>
