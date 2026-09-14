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
  <div class="relative flex min-h-screen items-center justify-center overflow-hidden p-4">
    <div
      class="absolute inset-0 bg-cover bg-center bg-no-repeat"
      style="background-image: url('/login-bg.png')"
    />
    <div class="absolute inset-0 bg-gray-950/45" />

    <div class="absolute top-4 right-4 z-10 flex items-center gap-2">
      <LocaleSwitcher />
      <ThemeToggle />
    </div>

    <form
      class="relative w-full max-w-md rounded-2xl border border-white/25 bg-white/20 p-8 shadow-theme-md backdrop-blur-xl"
      @submit.prevent="onSubmit"
    >
      <div
        v-if="error"
        class="mb-4 rounded-xl border border-error-200/60 bg-error-50/80 px-4 py-3 text-theme-sm text-error-700"
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

<style scoped>
:deep(label span) {
  color: rgba(255, 255, 255, 0.92);
}
:deep(input) {
  border-color: rgba(255, 255, 255, 0.35);
  background: rgba(255, 255, 255, 0.18);
  color: #fff;
  box-shadow: none;
}
:deep(input::placeholder) {
  color: rgba(255, 255, 255, 0.55);
}
:deep(input:focus) {
  border-color: rgba(255, 255, 255, 0.7);
  background: rgba(255, 255, 255, 0.28);
}
</style>
