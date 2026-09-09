<script setup lang="ts">
withDefaults(
  defineProps<{
    label: string
    value: string | number
    hint?: string
    tone?: 'brand' | 'success' | 'warning' | 'error' | 'purple' | 'cyan'
    icon?: 'users' | 'orders' | 'delivered' | 'cancelled' | 'daily' | 'monthly'
  }>(),
  { tone: 'brand', icon: 'orders' },
)

const toneClass: Record<string, string> = {
  brand: 'from-[#465fff] to-[#3641f5] shadow-[0_10px_24px_rgba(70,95,255,0.28)]',
  success: 'from-[#12b76a] to-[#039855] shadow-[0_10px_24px_rgba(18,183,106,0.28)]',
  warning: 'from-[#f79009] to-[#dc6803] shadow-[0_10px_24px_rgba(247,144,9,0.28)]',
  error: 'from-[#f04438] to-[#d92d20] shadow-[0_10px_24px_rgba(240,68,56,0.28)]',
  purple: 'from-[#7A5AF8] to-[#6941C6] shadow-[0_10px_24px_rgba(122,90,248,0.28)]',
  cyan: 'from-[#0ba5ec] to-[#0086c9] shadow-[0_10px_24px_rgba(11,165,236,0.28)]',
}
</script>

<template>
  <div
    class="relative min-w-0 flex-1 overflow-hidden rounded-2xl bg-gradient-to-br p-4 text-white shadow-lg md:p-5"
    :class="toneClass[tone]"
  >
    <div class="absolute -top-4 -right-4 size-20 rounded-full bg-white/10" />
    <div class="absolute -bottom-6 -left-2 size-16 rounded-full bg-white/10" />

    <div class="relative flex items-start justify-between gap-3">
      <div class="min-w-0">
        <p class="text-theme-xs font-medium text-white/80 md:text-theme-sm">{{ label }}</p>
        <p class="mt-2 truncate text-xl font-bold tracking-tight md:text-2xl">{{ value }}</p>
        <p v-if="hint" class="mt-1 text-theme-xs text-white/70">{{ hint }}</p>
      </div>

      <div class="flex size-10 shrink-0 items-center justify-center rounded-xl bg-white/20 backdrop-blur-sm md:size-11">
        <svg
          class="size-5 md:size-6"
          viewBox="0 0 24 24"
          fill="none"
          stroke="currentColor"
          stroke-width="1.8"
          stroke-linecap="round"
          stroke-linejoin="round"
          aria-hidden="true"
        >
          <template v-if="icon === 'users'">
            <path d="M16 21V19C16 16.79 14.21 15 12 15H6C3.79 15 2 16.79 2 19V21" />
            <circle cx="9" cy="7" r="4" />
            <path d="M22 21V19C22 17.14 20.84 15.54 19.13 15.04" />
            <path d="M16.13 3.04C17.85 3.54 19.02 5.14 19.02 7C19.02 8.86 17.85 10.46 16.13 10.96" />
          </template>
          <template v-else-if="icon === 'orders'">
            <path d="M21 8.5L12 3.5L3 8.5V15.5L12 20.5L21 15.5V8.5Z" />
            <path d="M3 8.5L12 13.5L21 8.5" />
            <path d="M12 13.5V20.5" />
          </template>
          <template v-else-if="icon === 'delivered'">
            <path d="M20 6L9 17L4 12" />
          </template>
          <template v-else-if="icon === 'cancelled'">
            <circle cx="12" cy="12" r="9" />
            <path d="M15 9L9 15M9 9L15 15" />
          </template>
          <template v-else-if="icon === 'daily'">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M3 10H21M8 3V7M16 3V7" />
          </template>
          <template v-else>
            <path d="M4 19V5M4 19H20" />
            <path d="M8 15V11M12 15V8M16 15V12" />
          </template>
        </svg>
      </div>
    </div>
  </div>
</template>
