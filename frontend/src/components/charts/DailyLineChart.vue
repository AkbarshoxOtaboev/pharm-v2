<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import VueApexCharts from 'vue3-apexcharts'
import { useTheme } from '@/composables/useTheme'
import { intlLocale, type AppLocale } from '@/i18n'

export interface DailySeriesDef {
  key: string
  name: string
  color: string
  data: number[]
}

const props = defineProps<{
  title: string
  hint?: string
  categories: string[]
  seriesDefs: DailySeriesDef[]
}>()

const { locale } = useI18n()
const { theme } = useTheme()
const isDark = computed(() => theme.value === 'dark')
const tickColor = computed(() => (isDark.value ? '#98A2B3' : '#6B7280'))
const gridColor = computed(() => (isDark.value ? '#344054' : '#E5E7EB'))

const visible = ref<Record<string, boolean>>({})

watch(
  () => props.seriesDefs.map((s) => s.key).join('|'),
  () => {
    const next = { ...visible.value }
    for (const s of props.seriesDefs) {
      if (next[s.key] === undefined) next[s.key] = true
    }
    visible.value = next
  },
  { immediate: true },
)

function isOn(key: string) {
  return visible.value[key] !== false
}

function setOn(key: string, on: boolean) {
  visible.value = { ...visible.value, [key]: on }
}

const activeDefs = computed(() => props.seriesDefs.filter((s) => isOn(s.key)))

const series = computed(() =>
  activeDefs.value.map((s) => ({
    name: s.name,
    data: s.data,
  })),
)

function formatValue(val: number) {
  const n = Number(val) || 0
  return new Intl.NumberFormat(intlLocale(locale.value as AppLocale)).format(n)
}

function formatAxis(val: number) {
  const n = Number(val) || 0
  if (Math.abs(n) >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (Math.abs(n) >= 1_000) return `${(n / 1_000).toFixed(0)}k`
  return String(Math.round(n))
}

const chartOptions = computed(() => ({
  colors: activeDefs.value.map((s) => s.color),
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'line',
    height: 280,
    toolbar: { show: false },
    background: 'transparent',
    foreColor: tickColor.value,
    zoom: { enabled: false },
  },
  stroke: {
    curve: 'smooth',
    width: 3,
  },
  markers: {
    size: 3,
    strokeWidth: 0,
    hover: { size: 5 },
  },
  dataLabels: { enabled: false },
  xaxis: {
    categories: props.categories,
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: {
      rotate: -45,
      rotateAlways: props.categories.length > 12,
      style: {
        colors: tickColor.value,
        fontSize: '11px',
      },
    },
  },
  yaxis: {
    labels: {
      style: {
        colors: tickColor.value,
        fontSize: '12px',
      },
      formatter: formatAxis,
    },
  },
  legend: { show: false },
  grid: {
    borderColor: gridColor.value,
    strokeDashArray: 4,
  },
  theme: { mode: isDark.value ? 'dark' : 'light' },
  tooltip: {
    theme: isDark.value ? 'dark' : 'light',
    shared: true,
    intersect: false,
    y: {
      formatter: formatValue,
    },
  },
  noData: {
    text: '—',
    style: { color: tickColor.value },
  },
}))
</script>

<template>
  <div class="rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 shadow-theme-xs sm:px-6 sm:pt-6 dark:border-gray-800 dark:bg-white/[0.03]">
    <div class="mb-4 flex flex-wrap items-start justify-between gap-3">
      <div>
        <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ title }}</h3>
        <p v-if="hint" class="mt-1 text-theme-sm text-gray-500 dark:text-gray-400">{{ hint }}</p>
      </div>
      <div class="flex flex-wrap items-center gap-x-4 gap-y-2">
        <label
          v-for="s in seriesDefs"
          :key="s.key"
          class="inline-flex cursor-pointer items-center gap-2 text-theme-sm text-gray-700 select-none dark:text-gray-300"
        >
          <input
            type="checkbox"
            class="size-4 rounded border-gray-300 text-brand-500 focus:ring-brand-500/30 dark:border-gray-600 dark:bg-gray-800"
            :checked="isOn(s.key)"
            @change="setOn(s.key, ($event.target as HTMLInputElement).checked)"
          />
          <span class="inline-flex items-center gap-1.5">
            <span class="size-2.5 rounded-full" :style="{ backgroundColor: s.color }" />
            {{ s.name }}
          </span>
        </label>
      </div>
    </div>
    <div class="max-w-full overflow-x-auto custom-scrollbar">
      <div class="min-w-[560px]">
        <VueApexCharts
          v-if="activeDefs.length"
          type="line"
          height="280"
          :options="chartOptions"
          :series="series"
        />
        <div
          v-else
          class="flex h-[280px] items-center justify-center text-theme-sm text-gray-400 dark:text-gray-500"
        >
          —
        </div>
      </div>
    </div>
  </div>
</template>
