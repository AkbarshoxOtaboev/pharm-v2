<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth.store'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import ThemeToggle from '@/components/layout/ThemeToggle.vue'
import LocaleSwitcher from '@/components/layout/LocaleSwitcher.vue'
import { apiError } from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()
const { t } = useI18n()

const username = ref('')
const password = ref('')
const loading = ref(false)
const error = ref('')

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    const path = await auth.login(username.value.trim(), password.value)
    await router.push(path)
  } catch (e) {
    error.value = apiError(e, 'errors.loginFailed')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div
    class="relative flex min-h-screen items-center justify-center overflow-hidden bg-gray-50 p-4 dark:bg-gray-900"
  >
    <div
      class="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_20%_20%,rgba(70,95,255,0.15),transparent_40%),radial-gradient(circle_at_80%_0%,rgba(18,183,106,0.12),transparent_35%)] dark:bg-[radial-gradient(circle_at_20%_20%,rgba(70,95,255,0.22),transparent_40%),radial-gradient(circle_at_80%_0%,rgba(18,183,106,0.12),transparent_35%)]"
    />
    <div class="absolute top-4 right-4 z-10 flex items-center gap-2">
      <LocaleSwitcher />
      <ThemeToggle />
    </div>
    <form
      class="relative w-full max-w-md rounded-2xl border border-gray-200 bg-white p-8 shadow-theme-md dark:border-gray-800 dark:bg-gray-dark"
      @submit.prevent="onSubmit"
    >
      <div class="mb-8 text-center">
        <div
          class="mx-auto mb-4 flex size-14 items-center justify-center rounded-2xl bg-brand-500 text-xl font-bold text-white"
        >
          P
        </div>
        <h1 class="text-2xl font-bold text-gray-800 dark:text-white/90">{{ t('app.name') }}</h1>
        <p class="mt-1 text-theme-sm text-gray-500 dark:text-gray-400">{{ t('app.tagline') }}</p>
      </div>

      <div
        v-if="error"
        class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ error }}
      </div>

      <div class="space-y-4">
        <AppInput v-model="username" :label="t('common.login')" autocomplete="username" />
        <AppInput
          v-model="password"
          :label="t('common.password')"
          type="password"
          autocomplete="current-password"
        />
        <AppButton class="w-full" type="submit" :loading="loading">{{ t('auth.signIn') }}</AppButton>
      </div>
    </form>
  </div>
</template>
