<script setup lang="ts">
import { computed } from 'vue'
import VueApexCharts from 'vue3-apexcharts'

const props = defineProps<{
  seriesData: number[]
  year?: number
}>()

const categories = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec']

const series = computed(() => [
  {
    name: 'Sotuv',
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
    categories,
    axisBorder: { show: false },
    axisTicks: { show: false },
    labels: {
      style: {
        colors: '#6B7280',
        fontSize: '12px',
      },
    },
  },
  yaxis: {
    labels: {
      style: {
        colors: '#6B7280',
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
    borderColor: '#E5E7EB',
  },
  fill: { opacity: 1 },
  tooltip: {
    y: {
      formatter: (val: number) =>
        new Intl.NumberFormat('uz-UZ').format(val || 0),
    },
  },
}))
</script>

<template>
  <div class="rounded-2xl border border-gray-200 bg-white px-5 pb-5 pt-5 shadow-theme-xs sm:px-6 sm:pt-6">
    <div class="mb-4">
      <h3 class="text-lg font-semibold text-gray-800">Oylik sotuvlar</h3>
      <p class="mt-1 text-theme-sm text-gray-500">
        {{ year || new Date().getFullYear() }}-yil yetkazilgan buyurtmalar summasi
      </p>
    </div>
    <div class="max-w-full overflow-x-auto custom-scrollbar">
      <div class="min-w-[600px]">
        <VueApexCharts type="bar" height="220" :options="chartOptions" :series="series" />
      </div>
    </div>
  </div>
</template>
