<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth.store'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { enumLabel } from '@/i18n'

const { t } = useI18n()

const auth = useAuthStore()
const router = useRouter()
const open = ref(false)
const root = ref<HTMLElement | null>(null)

function onDocClick(e: MouseEvent) {
  if (root.value && !root.value.contains(e.target as Node)) open.value = false
}

onMounted(() => document.addEventListener('click', onDocClick))
onUnmounted(() => document.removeEventListener('click', onDocClick))

async function onLogout() {
  open.value = false
  await auth.logout()
  router.push('/login')
}

function goProfile() {
  open.value = false
  router.push('/admin/profile')
}
</script>

<template>
  <div ref="root" class="relative">
    <button
      type="button"
      class="flex items-center gap-2 rounded-full py-1 pr-1 pl-1 hover:bg-gray-50 sm:pr-2 dark:hover:bg-white/5"
      @click="open = !open"
    >
      <UserAvatar :avatar="auth.avatar" :name="auth.fullName" size="sm" />
      <span class="hidden text-theme-sm font-medium text-gray-700 sm:inline dark:text-gray-300">
        {{ auth.fullName || auth.username }}
      </span>
      <svg
        class="hidden size-4 text-gray-500 sm:block"
        :class="open ? 'rotate-180' : ''"
        viewBox="0 0 20 20"
        fill="none"
      >
        <path
          d="M5 7.5L10 12.5L15 7.5"
          stroke="currentColor"
          stroke-width="1.5"
          stroke-linecap="round"
          stroke-linejoin="round"
        />
      </svg>
    </button>

    <div
      v-if="open"
      class="absolute right-0 z-50 mt-3 w-[260px] overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-theme-lg dark:border-gray-800 dark:bg-gray-dark"
    >
      <div class="border-b border-gray-100 px-4 py-3 dark:border-gray-800">
        <p class="text-theme-sm font-semibold text-gray-800 dark:text-white/90">
          {{ auth.fullName || auth.username }}
        </p>
        <p class="mt-0.5 text-theme-xs text-gray-500 dark:text-gray-400">
          {{ auth.username }} · {{ enumLabel('role', auth.role) }}
        </p>
      </div>

      <div class="p-2">
        <button
          type="button"
          class="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-theme-sm text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-white/5"
          @click="goProfile"
        >
          <svg class="size-5 text-gray-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
            <circle cx="12" cy="8" r="4" />
            <path d="M4 21c0-3.87 3.58-7 8-7s8 3.13 8 7" />
          </svg>
          {{ t('auth.editProfile') }}
        </button>
        <button
          type="button"
          class="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-theme-sm text-gray-700 hover:bg-gray-50 dark:text-gray-300 dark:hover:bg-white/5"
          @click="goProfile"
        >
          <svg class="size-5 text-gray-500" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
            <circle cx="12" cy="12" r="3" />
            <path d="M19.4 15a1.7 1.7 0 0 0 .34 1.86l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.7 1.7 0 0 0-1.86-.34 1.7 1.7 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09a1.7 1.7 0 0 0-1-1.51 1.7 1.7 0 0 0-1.86.34l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.7 1.7 0 0 0 .34-1.86 1.7 1.7 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09a1.7 1.7 0 0 0 1.51-1 1.7 1.7 0 0 0-.34-1.86l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.7 1.7 0 0 0 1.86.34H9a1.7 1.7 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.7 1.7 0 0 0 1 1.51 1.7 1.7 0 0 0 1.86-.34l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.7 1.7 0 0 0-.34 1.86V9c0 .69.4 1.31 1.01 1.59.2.1.42.15.64.15H21a2 2 0 1 1 0 4h-.09a1.7 1.7 0 0 0-1.51 1z" />
          </svg>
          {{ t('auth.accountSettings') }}
        </button>
      </div>

      <div class="border-t border-gray-100 p-2 dark:border-gray-800">
        <button
          type="button"
          class="flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left text-theme-sm text-error-600 hover:bg-error-50 dark:hover:bg-error-500/10"
          @click="onLogout"
        >
          <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
            <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4" />
            <path d="M16 17l5-5-5-5" />
            <path d="M21 12H9" />
          </svg>
          {{ t('common.logout') }}
        </button>
      </div>
    </div>
  </div>
</template>
