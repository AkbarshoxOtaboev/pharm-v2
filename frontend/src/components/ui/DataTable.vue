<script setup lang="ts">
defineProps<{ loading?: boolean; empty?: string }>()
</script>

<template>
  <div
    class="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-theme-xs"
  >
    <div class="overflow-x-auto">
      <table class="min-w-full divide-y divide-gray-200">
        <thead class="bg-gray-50">
          <tr>
            <slot name="head" />
          </tr>
        </thead>
        <tbody class="divide-y divide-gray-100 bg-white">
          <tr v-if="loading">
            <td colspan="20" class="px-5 py-8 text-center text-theme-sm text-gray-500">
              Yuklanmoqda...
            </td>
          </tr>
          <tr v-else-if="empty">
            <td colspan="20" class="px-5 py-8 text-center text-theme-sm text-gray-500">
              {{ empty }}
            </td>
          </tr>
          <slot v-else />
        </tbody>
      </table>
    </div>
  </div>
</template>
