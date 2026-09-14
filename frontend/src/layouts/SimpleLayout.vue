<script setup lang="ts">
import { useRouter, RouterView } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth.store'
import ThemeToggle from '@/components/layout/ThemeToggle.vue'
import LocaleSwitcher from '@/components/layout/LocaleSwitcher.vue'
import { enumLabel } from '@/i18n'

const auth = useAuthStore()
const router = useRouter()
const { t } = useI18n()

async function onLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-gray-50 dark:bg-gray-900">
    <header class="border-b border-gray-200 bg-white px-4 py-3 md:px-6 dark:border-gray-800 dark:bg-gray-dark">
      <div class="mx-auto flex max-w-screen-2xl items-center justify-between">
        <div class="flex items-center gap-2">
          <span
            class="flex size-9 items-center justify-center rounded-xl bg-brand-500 text-sm font-bold text-white"
          >
            P
          </span>
          <strong class="text-gray-800 dark:text-white/90">{{ t('app.name') }}</strong>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-theme-sm text-gray-600 dark:text-gray-400">
            {{ auth.fullName || auth.username }} · {{ enumLabel('role', auth.role) }}
          </span>
          <LocaleSwitcher />
          <ThemeToggle />
          <button
            class="rounded-lg border border-gray-200 px-3 py-2 text-theme-sm hover:bg-gray-50 dark:border-gray-800 dark:text-gray-300 dark:hover:bg-white/5"
            type="button"
            @click="onLogout"
          >
            {{ t('common.logout') }}
          </button>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-screen-2xl p-4 md:p-6">
      <RouterView />
    </main>
  </div>
</template>
