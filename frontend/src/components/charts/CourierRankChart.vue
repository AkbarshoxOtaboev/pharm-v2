<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import VueApexCharts from 'vue3-apexcharts'
import { useTheme } from '@/composables/useTheme'
import { intlLocale, type AppLocale } from '@/i18n'
import { money } from '@/utils/format'

const props = defineProps<{
  title: string
  hint?: string
  categories: string[]
  values: number[]
  color: string
  format?: 'money' | 'count'
}>()

const { locale } = useI18n()
const { theme } = useTheme()
const isDark = computed(() => theme.value === 'dark')
const tickColor = computed(() => (isDark.value ? '#98A2B3' : '#6B7280'))
const gridColor = computed(() => (isDark.value ? '#344054' : '#E5E7EB'))

const height = computed(() => Math.max(220, props.categories.length * 42))

function formatValue(val: number) {
  const n = Number(val) || 0
  if (props.format === 'money') return money(n)
  return new Intl.NumberFormat(intlLocale(locale.value as AppLocale)).format(n)
}

function formatAxis(val: number) {
  const n = Number(val) || 0
  if (Math.abs(n) >= 1_000_000) return `${(n / 1_000_000).toFixed(1)}M`
  if (Math.abs(n) >= 1_000) return `${(n / 1_000).toFixed(0)}k`
  return String(Math.round(n))
}

const series = computed(() => [
  {
    name: props.title,
    data: props.values,
  },
])

const chartOptions = computed(() => ({
  colors: [props.color],
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'bar',
    height: height.value,
    toolbar: { show: false },
    background: 'transparent',
    foreColor: tickColor.value,
  },
  plotOptions: {
    bar: {
      horizontal: true,
      borderRadius: 4,
      barHeight: '62%',
    },
  },
  dataLabels: { enabled: false },
  xaxis: {
    categories: props.categories,
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: {
      style: {
        colors: tickColor.value,
        fontSize: '12px',
      },
      formatter: formatAxis,
    },
  },
  yaxis: {
    labels: {
      maxWidth: 180,
      style: {
        colors: tickColor.value,
        fontSize: '12px',
      },
    },
  },
  grid: {
    borderColor: gridColor.value,
    strokeDashArray: 4,
    xaxis: { lines: { show: true } },
    yaxis: { lines: { show: false } },
  },
  legend: { show: false },
  theme: { mode: isDark.value ? 'dark' : 'light' },
  tooltip: {
    theme: isDark.value ? 'dark' : 'light',
    y: { formatter: formatValue },
  },
}))
</script>

<template>
  <div class="rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 shadow-theme-xs sm:px-6 sm:pt-6 dark:border-gray-800 dark:bg-white/[0.03]">
    <div class="mb-4">
      <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ title }}</h3>
      <p v-if="hint" class="mt-1 text-theme-sm text-gray-500 dark:text-gray-400">{{ hint }}</p>
    </div>
    <VueApexCharts type="bar" :height="height" :options="chartOptions" :series="series" />
  </div>
</template>
