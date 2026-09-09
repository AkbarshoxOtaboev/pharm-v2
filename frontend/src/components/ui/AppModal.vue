<script setup lang="ts">
defineProps<{ title?: string; open: boolean }>()
defineEmits<{ close: [] }>()
</script>

<template>
  <Teleport to="body">
    <div
      v-if="open"
      class="fixed inset-0 z-99999 flex items-center justify-center p-4"
    >
      <div class="absolute inset-0 bg-gray-900/40" @click="$emit('close')" />
      <div
        class="relative z-10 max-h-[90vh] w-full max-w-2xl overflow-y-auto rounded-2xl border border-gray-200 bg-white p-6 shadow-theme-md"
      >
        <div class="mb-4 flex items-center justify-between gap-3">
          <h2 class="text-lg font-semibold text-gray-800">{{ title }}</h2>
          <button
            class="rounded-lg p-2 text-gray-500 hover:bg-gray-100"
            type="button"
            @click="$emit('close')"
          >
            ✕
          </button>
        </div>
        <slot />
      </div>
    </div>
  </Teleport>
</template>
