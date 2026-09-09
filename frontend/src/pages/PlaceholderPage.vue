<script setup lang="ts">
defineProps<{ title: string }>()
</script>

<template>
  <div class="rounded-2xl border border-gray-200 bg-white p-8 shadow-theme-xs">
    <h1 class="text-2xl font-semibold text-gray-800">{{ title }}</h1>
    <p class="mt-2 text-theme-sm text-gray-500">
      Bu modul keyingi iteratsiyada to‘ldiriladi.
    </p>
  </div>
</template>
