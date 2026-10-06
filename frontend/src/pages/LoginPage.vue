<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth.store'
import AppButton from '@/components/ui/AppButton.vue'
import AppIcon from '@/components/ui/AppIcon.vue'
import AppInput from '@/components/ui/AppInput.vue'
import ThemeToggle from '@/components/layout/ThemeToggle.vue'
import LocaleSwitcher from '@/components/layout/LocaleSwitcher.vue'
import { apiError } from '@/utils/format'

const auth = useAuthStore()
const router = useRouter()
const { t } = useI18n()

const username = ref('')
const password = ref('')
const showPassword = ref(false)
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
  <div class="flex min-h-screen bg-white dark:bg-gray-900">
    <div class="flex w-full flex-col p-6 lg:w-1/2">
      <div class="flex items-center justify-end gap-2">
        <LocaleSwitcher />
        <ThemeToggle />
      </div>

      <div class="mx-auto flex w-full max-w-md flex-1 flex-col justify-center py-10">
        <div class="mb-5 sm:mb-8">
          <h1 class="mb-2 text-2xl font-semibold text-gray-800 sm:text-3xl dark:text-white/90">
            {{ t('auth.signIn') }}
          </h1>
          <p class="text-theme-sm text-gray-500 dark:text-gray-400">{{ t('auth.signInSubtitle') }}</p>
        </div>

        <div
          v-if="error"
          class="mb-5 flex items-start gap-2 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
        >
          <AppIcon name="alert" class="mt-0.5 size-4" />
          <span>{{ error }}</span>
        </div>

        <form class="space-y-5" @submit.prevent="onSubmit">
          <AppInput
            v-model="username"
            :label="t('common.login')"
            :placeholder="t('auth.loginPlaceholder')"
            autocomplete="username"
          />
          <div class="relative [&_input]:pr-12">
            <AppInput
              v-model="password"
              :label="t('common.password')"
              :placeholder="t('auth.passwordPlaceholder')"
              :type="showPassword ? 'text' : 'password'"
              autocomplete="current-password"
            />
            <button
              type="button"
              class="absolute right-0 bottom-0 flex h-11 w-12 items-center justify-center text-gray-500 transition hover:text-gray-700 dark:text-gray-400 dark:hover:text-gray-200"
              :title="showPassword ? t('auth.hidePassword') : t('auth.showPassword')"
              :aria-label="showPassword ? t('auth.hidePassword') : t('auth.showPassword')"
              @click="showPassword = !showPassword"
            >
              <AppIcon :name="showPassword ? 'eye-off' : 'eye'" />
            </button>
          </div>
          <AppButton class="w-full" type="submit" :loading="loading">{{ t('auth.signIn') }}</AppButton>
        </form>
      </div>
    </div>

    <div class="hidden bg-brand-950 lg:block lg:w-1/2 dark:bg-white/5" />
  </div>
</template>
