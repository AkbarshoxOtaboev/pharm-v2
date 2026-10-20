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
    <template v-else-if="name === 'eye'">
      <path d="M2.5 12C4.5 7.5 8 5 12 5C16 5 19.5 7.5 21.5 12C19.5 16.5 16 19 12 19C8 19 4.5 16.5 2.5 12Z" />
      <circle cx="12" cy="12" r="3" />
    </template>
    <template v-else-if="name === 'eye-off'">
      <path d="M10.6 5.1C11.06 5.03 11.53 5 12 5C16 5 19.5 7.5 21.5 12C20.93 13.28 20.2 14.4 19.35 15.33M6.6 6.6C4.85 7.72 3.45 9.6 2.5 12C4.5 16.5 8 19 12 19C13.95 19 15.77 18.4 17.35 17.35" />
      <path d="M9.9 9.9C9.34 10.46 9 11.23 9 12C9 13.66 10.34 15 12 15C12.77 15 13.54 14.66 14.1 14.1M3 3L21 21" />
    </template>
    <template v-else-if="name === 'pencil-alt'">
      <path d="M11 4H6C4.9 4 4 4.9 4 6V18C4 19.1 4.9 20 6 20H18C19.1 20 20 19.1 20 18V13" />
      <path d="M18.4 2.6C19.2 1.8 20.4 1.8 21.2 2.6C22 3.4 22 4.6 21.2 5.4L12 14.6L8 15.6L9 11.6L18.4 2.6Z" />
    </template>
    <template v-else-if="name === 'tag'">
      <path d="M3 12V4C3 3.45 3.45 3 4 3H12L21 12L12 21L3 12Z" />
      <circle cx="7.5" cy="7.5" r="1.5" />
    </template>
    <template v-else-if="name === 'layers'">
      <path d="M12 3L21 8L12 13L3 8L12 3Z" />
      <path d="M3 12.5L12 17.5L21 12.5M3 16.5L12 21.5L21 16.5" />
    </template>
    <template v-else-if="name === 'image'">
      <rect x="3" y="4" width="18" height="16" rx="2" />
      <circle cx="9" cy="10" r="2" />
      <path d="M21 16L16 11L5 20" />
    </template>
    <template v-else-if="name === 'upload'">
      <path d="M12 16V4M7 9L12 4L17 9" />
      <path d="M4 16V18C4 19.1 4.9 20 6 20H18C19.1 20 20 19.1 20 18V16" />
    </template>
    <template v-else-if="name === 'download'">
      <path d="M12 4V16M7 11L12 16L17 11" />
      <path d="M4 16V18C4 19.1 4.9 20 6 20H18C19.1 20 20 19.1 20 18V16" />
    </template>
    <template v-else-if="name === 'hash'">
      <path d="M5 9H20M4 15H19M10 3L8 21M16 3L14 21" />
    </template>
    <template v-else-if="name === 'text'">
      <path d="M4 6H20M4 10H20M4 14H14M4 18H11" />
    </template>
    <template v-else-if="name === 'ruler'">
      <path d="M3 17L17 3L21 7L7 21L3 17Z" />
      <path d="M7 13L9 15M10 10L12 12M13 7L15 9" />
    </template>
    <template v-else-if="name === 'calendar'">
      <rect x="3" y="5" width="18" height="16" rx="2" />
      <path d="M3 10H21M8 3V7M16 3V7" />
    </template>
    <template v-else-if="name === 'history'">
      <path d="M3 12C3 16.97 7.03 21 12 21C16.97 21 21 16.97 21 12C21 7.03 16.97 3 12 3C8.5 3 5.5 5 4 8" />
      <path d="M3 4V8H7M12 7V12L15 14" />
    </template>
    <template v-else-if="name === 'arrow-up-right'">
      <path d="M7 17L17 7M9 7H17V15" />
    </template>
    <template v-else-if="name === 'arrow-down-left'">
      <path d="M17 7L7 17M15 17H7V9" />
    </template>
    <template v-else-if="name === 'send'">
      <path d="M21 3L10 14" />
      <path d="M21 3L14.5 21L10 14L3 9.5L21 3Z" />
    </template>
    <template v-else-if="name === 'undo'">
      <path d="M9 14L4 9L9 4" />
      <path d="M4 9H15C18.31 9 21 11.69 21 15C21 18.31 18.31 21 15 21H11" />
    </template>
    <template v-else-if="name === 'chevron-left'">
      <path d="M15 6L9 12L15 18" />
    </template>
    <template v-else-if="name === 'chevron-right'">
      <path d="M9 6L15 12L9 18" />
    </template>
    <template v-else-if="name === 'search'">
      <circle cx="11" cy="11" r="7" />
      <path d="M20 20L16 16" />
    </template>
    <template v-else-if="name === 'x'">
      <path d="M6 6L18 18M18 6L6 18" />
    </template>
    <template v-else-if="name === 'warehouse'">
      <path d="M3 21V9L12 4L21 9V21" />
      <path d="M7 21V13H17V21M7 17H17" />
    </template>
    <template v-else-if="name === 'settings'">
      <circle cx="12" cy="12" r="3" />
      <path d="M12 2.5V5M12 19V21.5M21.5 12H19M5 12H2.5M18.7 5.3L16.9 7.1M7.1 16.9L5.3 18.7M18.7 18.7L16.9 16.9M7.1 7.1L5.3 5.3" />
    </template>
    <circle v-else cx="12" cy="12" r="4" fill="currentColor" />
  </svg>
</template>
