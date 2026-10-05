<script setup lang="ts">
defineProps<{ name: string }>()
</script>

<template>
  <svg
    class="size-5 shrink-0"
    viewBox="0 0 24 24"
    fill="none"
    stroke="currentColor"
    stroke-width="1.8"
    stroke-linecap="round"
    stroke-linejoin="round"
    aria-hidden="true"
  >
    <template v-if="name === 'user'">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21C4 17.13 7.58 14 12 14C16.42 14 20 17.13 20 21" />
    </template>
    <template v-else-if="name === 'phone'">
      <path d="M5 4H9L11 9L8.5 10.5C9.57 12.67 11.33 14.43 13.5 15.5L15 13L20 15V19C20 20.1 19.1 21 18 21C9.72 20.5 3.5 14.28 3 6C3 4.9 3.9 4 5 4Z" />
    </template>
    <template v-else-if="name === 'map-pin'">
      <path d="M12 21C12 21 19 14.5 19 9.5C19 5.63 15.87 2.5 12 2.5C8.13 2.5 5 5.63 5 9.5C5 14.5 12 21 12 21Z" />
      <circle cx="12" cy="9.5" r="2.5" />
    </template>
    <template v-else-if="name === 'truck'">
      <path d="M3 7H14V16H3V7Z" />
      <path d="M14 10H18L21 13V16H14V10Z" />
      <circle cx="7" cy="17.5" r="1.5" />
      <circle cx="17.5" cy="17.5" r="1.5" />
    </template>
    <template v-else-if="name === 'basket'">
      <path d="M3 9.5H21L19.2 18.6C19.02 19.42 18.29 20 17.45 20H6.55C5.71 20 4.98 19.42 4.8 18.6L3 9.5Z" />
      <path d="M8 9.5L11 4M16 9.5L13 4" />
      <path d="M9.5 13.5V16.5M14.5 13.5V16.5" />
    </template>
    <template v-else-if="name === 'receipt'">
      <path d="M6 3H18V21L15 19L12 21L9 19L6 21V3Z" />
      <path d="M9 8H15M9 12H15M9 16H12" />
    </template>
    <template v-else-if="name === 'cash'">
      <rect x="2.5" y="6" width="19" height="12" rx="2" />
      <circle cx="12" cy="12" r="2.5" />
      <path d="M6 10V14M18 10V14" />
    </template>
    <template v-else-if="name === 'card'">
      <rect x="2.5" y="5" width="19" height="14" rx="2" />
      <path d="M2.5 10H21.5M6 15H10" />
    </template>
    <template v-else-if="name === 'gift'">
      <rect x="3" y="8" width="18" height="4" rx="1" />
      <path d="M5 12V20H19V12M12 8V20" />
      <path d="M12 8C12 8 11 3.5 8.5 4C6.5 4.4 7 8 12 8ZM12 8C12 8 13 3.5 15.5 4C17.5 4.4 17 8 12 8Z" />
    </template>
    <template v-else-if="name === 'plus'">
      <path d="M12 5V19M5 12H19" />
    </template>
    <template v-else-if="name === 'pencil'">
      <path d="M4 20H8L18.5 9.5C19.33 8.67 19.33 7.33 18.5 6.5L17.5 5.5C16.67 4.67 15.33 4.67 14.5 5.5L4 16V20Z" />
      <path d="M13.5 6.5L17.5 10.5" />
    </template>
    <template v-else-if="name === 'trash'">
      <path d="M4 7H20M10 11V17M14 11V17M6 7L7 19C7.05 20.1 7.9 21 9 21H15C16.1 21 16.95 20.1 17 19L18 7M9 7V4H15V7" />
    </template>
    <template v-else-if="name === 'box'">
      <path d="M21 8.5L12 3.5L3 8.5V15.5L12 20.5L21 15.5V8.5Z" />
      <path d="M3 8.5L12 13.5L21 8.5M12 13.5V20.5" />
    </template>
    <template v-else-if="name === 'alert'">
      <path d="M12 3L22 20H2L12 3Z" />
      <path d="M12 10V14M12 17H12.01" />
    </template>
    <template v-else-if="name === 'check'">
      <circle cx="12" cy="12" r="9" />
      <path d="M8 12.5L11 15.5L16 9.5" />
    </template>
    <template v-else-if="name === 'arrow-left'">
      <path d="M19 12H5M11 6L5 12L11 18" />
    </template>
    <circle v-else cx="12" cy="12" r="4" fill="currentColor" />
  </svg>
</template>
