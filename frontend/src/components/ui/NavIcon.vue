<script setup lang="ts">
defineProps<{ name: string; active?: boolean }>()
</script>

<template>
  <svg
    class="size-5 shrink-0"
    :class="active ? 'text-brand-500' : 'text-gray-500 group-hover:text-gray-700'"
    viewBox="0 0 24 24"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    aria-hidden="true"
  >
    <!-- dashboard -->
    <path
      v-if="name === 'dashboard'"
      fill="currentColor"
      fill-rule="evenodd"
      clip-rule="evenodd"
      d="M5.5 3.25C4.257 3.25 3.25 4.257 3.25 5.5V9C3.25 10.243 4.257 11.25 5.5 11.25H9C10.243 11.25 11.25 10.243 11.25 9V5.5C11.25 4.257 10.243 3.25 9 3.25H5.5ZM4.75 5.5C4.75 5.086 5.086 4.75 5.5 4.75H9C9.414 4.75 9.75 5.086 9.75 5.5V9C9.75 9.414 9.414 9.75 9 9.75H5.5C5.086 9.75 4.75 9.414 4.75 9V5.5ZM5.5 12.75C4.257 12.75 3.25 13.757 3.25 15V18.5C3.25 19.743 4.257 20.75 5.5 20.75H9C10.243 20.75 11.25 19.743 11.25 18.5V15C11.25 13.757 10.243 12.75 9 12.75H5.5ZM4.75 15C4.75 14.586 5.086 14.25 5.5 14.25H9C9.414 14.25 9.75 14.586 9.75 15V18.5C9.75 18.914 9.414 19.25 9 19.25H5.5C5.086 19.25 4.75 18.914 4.75 18.5V15ZM12.75 5.5C12.75 4.257 13.757 3.25 15 3.25H18.5C19.743 3.25 20.75 4.257 20.75 5.5V9C20.75 10.243 19.743 11.25 18.5 11.25H15C13.757 11.25 12.75 10.243 12.75 9V5.5ZM15 4.75C14.586 4.75 14.25 5.086 14.25 5.5V9C14.25 9.414 14.586 9.75 15 9.75H18.5C18.914 9.75 19.25 9.414 19.25 9V5.5C19.25 5.086 18.914 4.75 18.5 4.75H15ZM15 12.75C13.757 12.75 12.75 13.757 12.75 15V18.5C12.75 19.743 13.757 20.75 15 20.75H18.5C19.743 20.75 20.75 19.743 20.75 18.5V15C20.75 13.757 19.743 12.75 18.5 12.75H15ZM14.25 15C14.25 14.586 14.586 14.25 15 14.25H18.5C18.914 14.25 19.25 14.586 19.25 15V18.5C19.25 18.914 18.914 19.25 18.5 19.25H15C14.586 19.25 14.25 18.914 14.25 18.5V15Z"
    />
    <!-- orders / package -->
    <g v-else-if="name === 'orders'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <path d="M21 8.5L12 3.5L3 8.5V15.5L12 20.5L21 15.5V8.5Z" />
      <path d="M3 8.5L12 13.5L21 8.5" />
      <path d="M12 13.5V20.5" />
    </g>
    <!-- customers / users -->
    <g v-else-if="name === 'customers'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <path d="M16 21V19C16 16.79 14.21 15 12 15H6C3.79 15 2 16.79 2 19V21" />
      <circle cx="9" cy="7" r="4" />
      <path d="M22 21V19C22 17.14 20.84 15.54 19.13 15.04" />
      <path d="M16.13 3.04C17.85 3.54 19.02 5.14 19.02 7C19.02 8.86 17.85 10.46 16.13 10.96" />
    </g>
    <!-- staff users -->
    <g v-else-if="name === 'users'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <circle cx="12" cy="8" r="4" />
      <path d="M4 21C4 17.13 7.58 14 12 14C16.42 14 20 17.13 20 21" />
    </g>
    <!-- categories / folder -->
    <g v-else-if="name === 'categories'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 7.5C3 6.12 4.12 5 5.5 5H9.17C9.7 5 10.21 5.21 10.59 5.59L11.41 6.41C11.79 6.79 12.3 7 12.83 7H18.5C19.88 7 21 8.12 21 9.5V16.5C21 17.88 19.88 19 18.5 19H5.5C4.12 19 3 17.88 3 16.5V7.5Z" />
    </g>
    <!-- products / pill -->
    <g v-else-if="name === 'products'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <path d="M10.5 3.5L3.5 10.5C2.17 11.83 2.17 14 3.5 15.33L8.67 20.5C10 21.83 12.17 21.83 13.5 20.5L20.5 13.5C21.83 12.17 21.83 10 20.5 8.67L15.33 3.5C14 2.17 11.83 2.17 10.5 3.5Z" />
      <path d="M8 12L12 16" />
    </g>
    <!-- store / warehouse -->
    <g v-else-if="name === 'store'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 10.5L5 4H19L21 10.5" />
      <path d="M3 10.5H21V20H3V10.5Z" />
      <path d="M9 20V14H15V20" />
    </g>
    <!-- courier -->
    <g v-else-if="name === 'courier'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <path d="M3 7H14V16H3V7Z" />
      <path d="M14 10H18L21 13V16H14V10Z" />
      <circle cx="7" cy="17.5" r="1.5" />
      <circle cx="17.5" cy="17.5" r="1.5" />
    </g>
    <!-- cash -->
    <g v-else-if="name === 'cash'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <rect x="2.5" y="6" width="19" height="12" rx="2" />
      <circle cx="12" cy="12" r="2.5" />
      <path d="M6 10V14M18 10V14" />
    </g>
    <!-- sales chart -->
    <g v-else-if="name === 'sales'" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round">
      <path d="M4 19V5" />
      <path d="M4 19H20" />
      <path d="M8 15V11" />
      <path d="M12 15V8" />
      <path d="M16 15V12" />
    </g>
    <!-- fallback -->
    <circle v-else cx="12" cy="12" r="4" fill="currentColor" />
  </svg>
</template>
