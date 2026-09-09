/// <reference types="vite/client" />

declare module 'vue3-apexcharts' {
  import type { DefineComponent } from 'vue'
  const VueApexCharts: DefineComponent<Record<string, unknown>, Record<string, unknown>, unknown>
  export default VueApexCharts
}
