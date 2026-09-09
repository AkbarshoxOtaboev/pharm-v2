<script setup lang="ts">
import { useRouter, RouterView } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'

const auth = useAuthStore()
const router = useRouter()

async function onLogout() {
  await auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="min-h-screen bg-gray-50">
    <header class="border-b border-gray-200 bg-white px-4 py-3 md:px-6">
      <div class="mx-auto flex max-w-screen-2xl items-center justify-between">
        <div class="flex items-center gap-2">
          <span
            class="flex size-9 items-center justify-center rounded-xl bg-brand-500 text-sm font-bold text-white"
          >
            P
          </span>
          <strong class="text-gray-800">PHARM</strong>
        </div>
        <div class="flex items-center gap-3">
          <span class="text-theme-sm text-gray-600">
            {{ auth.fullName || auth.username }} · {{ auth.role }}
          </span>
          <button
            class="rounded-lg border border-gray-200 px-3 py-2 text-theme-sm hover:bg-gray-50"
            type="button"
            @click="onLogout"
          >
            Chiqish
          </button>
        </div>
      </div>
    </header>
    <main class="mx-auto max-w-screen-2xl p-4 md:p-6">
      <RouterView />
    </main>
  </div>
</template>
