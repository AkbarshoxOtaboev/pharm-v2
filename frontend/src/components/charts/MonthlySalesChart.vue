<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import VueApexCharts from 'vue3-apexcharts'
import { useTheme } from '@/composables/useTheme'
import { intlLocale, type AppLocale } from '@/i18n'

const props = defineProps<{
  seriesData: number[]
  year?: number
}>()

const { t, locale } = useI18n()
const { theme } = useTheme()
const isDark = computed(() => theme.value === 'dark')
const tickColor = computed(() => (isDark.value ? '#98A2B3' : '#6B7280'))
const gridColor = computed(() => (isDark.value ? '#344054' : '#E5E7EB'))

const categories = computed(() =>
  Array.from({ length: 12 }, (_, i) => t(`months.${i}`)),
)

const series = computed(() => [
  {
    name: t('dashboard.sales'),
    data: props.seriesData.length === 12 ? props.seriesData : Array(12).fill(0),
  },
])

const chartOptions = computed(() => ({
  colors: ['#465fff'],
  chart: {
    fontFamily: 'Outfit, sans-serif',
    type: 'bar',
    height: 220,
    toolbar: { show: false },
    background: 'transparent',
    foreColor: tickColor.value,
  },
  plotOptions: {
    bar: {
      horizontal: false,
      columnWidth: '39%',
      borderRadius: 5,
      borderRadiusApplication: 'end',
    },
  },
  dataLabels: { enabled: false },
  stroke: {
    show: true,
    width: 4,
    colors: ['transparent'],
  },
  xaxis: {
    categories: categories.value,
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: {
      style: {
        colors: tickColor.value,
        fontSize: '12px',
      },
    },
  },
  yaxis: {
    labels: {
      style: {
        colors: tickColor.value,
        fontSize: '12px',
      },
      formatter: (val: number) => {
        if (val >= 1_000_000) return `${(val / 1_000_000).toFixed(1)}M`
        if (val >= 1_000) return `${(val / 1_000).toFixed(0)}k`
        return String(Math.round(val))
      },
    },
  },
  legend: { show: false },
  grid: {
    yaxis: { lines: { show: true } },
    borderColor: gridColor.value,
  },
  fill: { opacity: 1 },
  theme: { mode: isDark.value ? 'dark' : 'light' },
  tooltip: {
    theme: isDark.value ? 'dark' : 'light',
    y: {
      formatter: (val: number) =>
        new Intl.NumberFormat(intlLocale(locale.value as AppLocale)).format(val || 0),
    },
  },
}))
</script>

<template>
  <div class="rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 shadow-theme-xs sm:px-6 sm:pt-6 dark:border-gray-800 dark:bg-white/[0.03]">
    <div class="mb-4">
      <h3 class="text-lg font-semibold text-gray-800 dark:text-white/90">{{ t('dashboard.monthlySales') }}</h3>
      <p class="mt-1 text-theme-sm text-gray-500 dark:text-gray-400">
        {{ t('dashboard.monthlySalesHint', { year: year || new Date().getFullYear() }) }}
      </p>
    </div>
    <div class="max-w-full overflow-x-auto custom-scrollbar">
      <div class="min-w-[600px]">
        <VueApexCharts type="bar" height="220" :options="chartOptions" :series="series" />
      </div>
    </div>
  </div>
</template>
