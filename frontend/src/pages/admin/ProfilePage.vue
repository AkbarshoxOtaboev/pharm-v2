<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { RouterLink } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import { uploadAvatar } from '@/api/auth.api'
import { fetchCashSummary } from '@/api/cashRegister.api'
import { fetchDashboardStats } from '@/api/orders.api'
import { useAuthStore } from '@/stores/auth.store'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AvatarPicker from '@/components/ui/AvatarPicker.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { apiError, money } from '@/utils/format'

const { t } = useI18n()
const auth = useAuthStore()

const profile = reactive({
  fullName: '',
  personalPhone: '',
  workPhone: '',
  avatar: 'default-1',
})

const passwords = reactive({
  currentPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const showPassword = ref(false)
const savingAvatar = ref(false)
const savingPassword = ref(false)
const profileMsg = ref('')
const profileError = ref('')
const passwordMsg = ref('')
const passwordError = ref('')

const salesCount = ref(0)
const paymentsSum = ref(0)
const debtSum = ref(0)

const phone = computed(() => profile.personalPhone || profile.workPhone || '—')
const handle = computed(() => (auth.username ? `@${auth.username}` : ''))

onMounted(async () => {
  try {
    await auth.loadMe()
  } catch {
    /* keep local */
  }
  profile.fullName = auth.fullName
  profile.personalPhone = auth.personalPhone
  profile.workPhone = auth.workPhone
  profile.avatar = auth.avatar || 'default-1'
  void loadStats()
})

async function loadStats() {
  try {
    const [dash, cash] = await Promise.all([fetchDashboardStats(), fetchCashSummary()])
    salesCount.value = Number(dash.data?.deliveredOrdersCount ?? dash.data?.ordersCount ?? 0)
    paymentsSum.value = Number(cash.data?.handedSum ?? 0)
    debtSum.value = Number(cash.data?.onCourierSum ?? 0)
  } catch {
    /* optional cards */
  }
}

async function persistAvatar(next: string) {
  if (!next || next === auth.avatar) return
  savingAvatar.value = true
  profileError.value = ''
  profileMsg.value = ''
  try {
    await auth.updateProfile({
      fullName: profile.fullName.trim() || auth.fullName,
      personalPhone: profile.personalPhone.trim(),
      workPhone: profile.workPhone.trim(),
      avatar: next,
    })
    profile.avatar = next
    profileMsg.value = t('profile.saved')
  } catch (e) {
    profileError.value = apiError(e, 'errors.saveProfile')
  } finally {
    savingAvatar.value = false
  }
}

async function onUpload(file: File) {
  try {
    const res = await uploadAvatar(file)
    if (res.success && res.data?.filename) {
      profile.avatar = res.data.filename
      await persistAvatar(res.data.filename)
    }
  } catch (e) {
    profileError.value = apiError(e, 'errors.uploadImage')
  }
}

function generatePassword() {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789!@#$%'
  let out = ''
  const bytes = new Uint32Array(12)
  crypto.getRandomValues(bytes)
  for (let i = 0; i < bytes.length; i++) out += chars[bytes[i]! % chars.length]
  passwords.newPassword = out
  passwords.confirmPassword = out
  showPassword.value = true
}

async function savePassword() {
  passwordMsg.value = ''
  passwordError.value = ''
  if (!passwords.currentPassword || !passwords.newPassword) {
    passwordError.value = t('errors.enterPasswords')
    return
  }
  if (passwords.newPassword !== passwords.confirmPassword) {
    passwordError.value = t('errors.passwordMismatch')
    return
  }
  if (passwords.newPassword.length < 4) {
    passwordError.value = t('errors.passwordMinLength')
    return
  }
  savingPassword.value = true
  try {
    await auth.changePassword(passwords.currentPassword, passwords.newPassword)
    passwords.currentPassword = ''
    passwords.newPassword = ''
    passwords.confirmPassword = ''
    passwordMsg.value = t('profile.passwordChanged')
  } catch (e) {
    passwordError.value = apiError(e, 'errors.changePassword')
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <div>
    <div class="mb-6 flex flex-wrap items-end justify-between gap-3">
      <h1 class="text-2xl font-semibold text-gray-800 dark:text-white/90">{{ t('profile.title') }}</h1>
      <nav class="text-theme-sm text-gray-500 dark:text-gray-400">
        <RouterLink to="/admin/dashboard" class="hover:text-brand-500">{{ t('common.home') }}</RouterLink>
        <span class="mx-1.5">›</span>
        <span class="text-gray-700 dark:text-gray-300">{{ t('nav.profile') }}</span>
      </nav>
    </div>

    <div
      v-if="profileError"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ profileError }}
    </div>
    <div
      v-if="profileMsg"
      class="mb-4 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700 dark:border-success-500/20 dark:bg-success-500/10"
    >
      {{ profileMsg }}
    </div>

    <div
      class="overflow-hidden rounded-2xl border border-gray-200 bg-white shadow-theme-xs dark:border-gray-800 dark:bg-white/[0.03]"
    >
      <div class="grid lg:grid-cols-[280px_1fr]">
        <aside class="border-b border-gray-200 p-6 lg:border-r lg:border-b-0 dark:border-gray-800">
          <div class="flex flex-col items-center text-center">
            <UserAvatar :avatar="profile.avatar" :name="profile.fullName" size="xl" />
            <h2 class="mt-4 text-lg font-semibold text-gray-800 dark:text-white/90">
              {{ profile.fullName || auth.fullName }}
            </h2>
            <p class="text-theme-sm text-gray-500 dark:text-gray-400">{{ handle }}</p>
          </div>

          <div class="mt-6">
            <AvatarPicker
              v-model="profile.avatar"
              allow-upload
              upload-button
              @update:model-value="persistAvatar"
              @upload="onUpload"
            />
            <p v-if="savingAvatar" class="mt-2 text-center text-theme-xs text-gray-400">
              {{ t('common.loading') }}
            </p>
          </div>
        </aside>

        <div class="space-y-8 p-6 md:p-8">
          <section>
            <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">
              {{ t('profile.personalInfo') }}
            </h2>
            <p class="mt-1 text-theme-sm text-gray-500 dark:text-gray-400">
              {{ t('profile.personalInfoHint') }}
            </p>

            <div class="mt-5 grid gap-4 sm:grid-cols-2">
              <div>
                <p class="mb-1.5 text-theme-xs font-medium text-gray-500">{{ t('common.login') }}</p>
                <div
                  class="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-theme-sm font-medium text-gray-800 dark:border-gray-700 dark:bg-white/5 dark:text-white/90"
                >
                  {{ auth.username || '—' }}
                </div>
              </div>
              <div>
                <p class="mb-1.5 text-theme-xs font-medium text-gray-500">{{ t('profile.fullNameShort') }}</p>
                <div
                  class="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-theme-sm font-medium text-gray-800 dark:border-gray-700 dark:bg-white/5 dark:text-white/90"
                >
                  {{ profile.fullName || '—' }}
                </div>
              </div>
              <div>
                <p class="mb-1.5 text-theme-xs font-medium text-gray-500">{{ t('common.phone') }}</p>
                <div
                  class="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-theme-sm font-medium text-gray-800 dark:border-gray-700 dark:bg-white/5 dark:text-white/90"
                >
                  {{ phone }}
                </div>
              </div>
              <div>
                <p class="mb-1.5 text-theme-xs font-medium text-gray-500">{{ t('common.status') }}</p>
                <div
                  class="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-theme-sm font-semibold text-gray-800 dark:border-gray-700 dark:bg-white/5 dark:text-white/90"
                >
                  {{ auth.status || '—' }}
                </div>
              </div>
              <div class="sm:col-span-2">
                <p class="mb-1.5 text-theme-xs font-medium text-gray-500">{{ t('profile.roles') }}</p>
                <div
                  class="rounded-lg border border-gray-200 bg-gray-50 px-3 py-2.5 text-theme-sm font-medium text-gray-800 dark:border-gray-700 dark:bg-white/5 dark:text-white/90"
                >
                  {{ auth.role ? enumLabel('role', auth.role) : '—' }}
                  <span v-if="auth.role" class="ml-2 text-gray-400">({{ auth.role }})</span>
                </div>
              </div>
            </div>

            <div class="mt-5 grid gap-4 sm:grid-cols-3">
              <div class="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-white/[0.02]">
                <p class="text-theme-xs font-medium text-gray-500">{{ t('profile.sales') }}</p>
                <p class="mt-2 text-xl font-semibold text-gray-800 dark:text-white/90">{{ salesCount }}</p>
              </div>
              <div class="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-white/[0.02]">
                <p class="text-theme-xs font-medium text-gray-500">{{ t('profile.payments') }}</p>
                <p class="mt-2 text-xl font-semibold text-gray-800 dark:text-white/90">{{ money(paymentsSum) }}</p>
              </div>
              <div class="rounded-xl border border-gray-200 bg-white p-4 dark:border-gray-700 dark:bg-white/[0.02]">
                <p class="text-theme-xs font-medium text-gray-500">{{ t('profile.debt') }}</p>
                <p class="mt-2 text-xl font-semibold text-gray-800 dark:text-white/90">{{ money(debtSum) }}</p>
              </div>
            </div>
          </section>

          <section class="border-t border-gray-100 pt-8 dark:border-gray-800">
            <h2 class="text-lg font-semibold text-gray-800 dark:text-white/90">
              {{ t('profile.changePassword') }}
            </h2>
            <p class="mt-1 text-theme-sm text-gray-500 dark:text-gray-400">
              {{ t('profile.passwordHint') }}
            </p>

            <div
              v-if="passwordError"
              class="mt-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
            >
              {{ passwordError }}
            </div>
            <div
              v-if="passwordMsg"
              class="mt-4 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700 dark:border-success-500/20 dark:bg-success-500/10"
            >
              {{ passwordMsg }}
            </div>

            <form class="mt-5 space-y-4" @submit.prevent="savePassword">
              <AppInput
                v-model="passwords.currentPassword"
                :label="t('profile.currentPassword')"
                type="password"
              />
              <div>
                <label class="mb-1.5 block text-theme-sm font-medium text-gray-700 dark:text-gray-300">
                  {{ t('profile.newPassword') }}
                </label>
                <div class="flex flex-wrap gap-2">
                  <input
                    v-model="passwords.newPassword"
                    :type="showPassword ? 'text' : 'password'"
                    class="h-11 min-w-0 flex-1 rounded-lg border border-gray-200 bg-transparent px-3 text-theme-sm text-gray-800 outline-none focus:border-brand-300 focus:ring-3 focus:ring-brand-500/10 dark:border-gray-700 dark:text-white/90"
                  />
                  <AppButton type="button" variant="secondary" size="sm" @click="showPassword = !showPassword">
                    {{ showPassword ? t('profile.hide') : t('profile.show') }}
                  </AppButton>
                  <AppButton type="button" variant="secondary" size="sm" @click="generatePassword">
                    {{ t('profile.generate') }}
                  </AppButton>
                </div>
              </div>
              <AppInput
                v-model="passwords.confirmPassword"
                :label="t('profile.confirmPassword')"
                :type="showPassword ? 'text' : 'password'"
              />
              <div class="flex justify-end pt-2">
                <AppButton type="submit" :loading="savingPassword">{{ t('common.save') }}</AppButton>
              </div>
            </form>
          </section>
        </div>
      </div>
    </div>
  </div>
</template>
