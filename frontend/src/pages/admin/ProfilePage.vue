<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { uploadAvatar } from '@/api/auth.api'
import { useAuthStore } from '@/stores/auth.store'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AvatarPicker from '@/components/ui/AvatarPicker.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import { apiError } from '@/utils/format'

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

const savingProfile = ref(false)
const savingPassword = ref(false)
const profileMsg = ref('')
const profileError = ref('')
const passwordMsg = ref('')
const passwordError = ref('')

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
})

async function onUpload(file: File) {
  try {
    const res = await uploadAvatar(file)
    if (res.success && res.data?.filename) {
      profile.avatar = res.data.filename
    }
  } catch (e) {
    profileError.value = apiError(e, 'Rasm yuklanmadi')
  }
}

async function saveProfile() {
  profileMsg.value = ''
  profileError.value = ''
  if (!profile.fullName.trim()) {
    profileError.value = 'Ism majburiy'
    return
  }
  savingProfile.value = true
  try {
    await auth.updateProfile({
      fullName: profile.fullName.trim(),
      personalPhone: profile.personalPhone.trim(),
      workPhone: profile.workPhone.trim(),
      avatar: profile.avatar || 'default-1',
    })
    profileMsg.value = 'Profil saqlandi'
  } catch (e) {
    profileError.value = apiError(e)
  } finally {
    savingProfile.value = false
  }
}

async function savePassword() {
  passwordMsg.value = ''
  passwordError.value = ''
  if (!passwords.currentPassword || !passwords.newPassword) {
    passwordError.value = 'Joriy va yangi parolni kiriting'
    return
  }
  if (passwords.newPassword !== passwords.confirmPassword) {
    passwordError.value = 'Yangi parollar mos kelmadi'
    return
  }
  if (passwords.newPassword.length < 4) {
    passwordError.value = 'Yangi parol kamida 4 belgi'
    return
  }
  savingPassword.value = true
  try {
    await auth.changePassword(passwords.currentPassword, passwords.newPassword)
    passwords.currentPassword = ''
    passwords.newPassword = ''
    passwords.confirmPassword = ''
    passwordMsg.value = 'Parol o‘zgartirildi'
  } catch (e) {
    passwordError.value = apiError(e)
  } finally {
    savingPassword.value = false
  }
}
</script>

<template>
  <div>
    <PageHeader title="Mening profilim" subtitle="Shaxsiy maʼlumotlar va parol" />

    <div class="grid gap-6 xl:grid-cols-2">
      <section class="rounded-2xl border border-gray-200 bg-white p-6 shadow-theme-xs">
        <div class="mb-5 flex items-center gap-4">
          <UserAvatar :avatar="profile.avatar" :name="profile.fullName" size="lg" />
          <div>
            <h2 class="text-lg font-semibold text-gray-800">{{ auth.fullName }}</h2>
            <p class="text-theme-sm text-gray-500">{{ auth.username }} · {{ auth.role }}</p>
          </div>
        </div>

        <div
          v-if="profileError"
          class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
        >
          {{ profileError }}
        </div>
        <div
          v-if="profileMsg"
          class="mb-3 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700"
        >
          {{ profileMsg }}
        </div>

        <form class="space-y-4" @submit.prevent="saveProfile">
          <AvatarPicker v-model="profile.avatar" allow-upload @upload="onUpload" />
          <AppInput v-model="profile.fullName" label="To‘liq ism" />
          <AppInput :model-value="auth.username" label="Login" disabled />
          <AppInput v-model="profile.personalPhone" label="Shaxsiy telefon" />
          <AppInput v-model="profile.workPhone" label="Ish telefoni" />
          <AppButton type="submit" :loading="savingProfile">Saqlash</AppButton>
        </form>
      </section>

      <section class="rounded-2xl border border-gray-200 bg-white p-6 shadow-theme-xs">
        <h2 class="mb-4 text-lg font-semibold text-gray-800">Parolni o‘zgartirish</h2>
        <div
          v-if="passwordError"
          class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600"
        >
          {{ passwordError }}
        </div>
        <div
          v-if="passwordMsg"
          class="mb-3 rounded-xl border border-success-100 bg-success-50 px-4 py-3 text-theme-sm text-success-700"
        >
          {{ passwordMsg }}
        </div>
        <form class="space-y-4" @submit.prevent="savePassword">
          <AppInput
            v-model="passwords.currentPassword"
            label="Joriy parol"
            type="password"
          />
          <AppInput v-model="passwords.newPassword" label="Yangi parol" type="password" />
          <AppInput
            v-model="passwords.confirmPassword"
            label="Yangi parolni tasdiqlash"
            type="password"
          />
          <AppButton type="submit" :loading="savingPassword">Parolni yangilash</AppButton>
        </form>
      </section>
    </div>
  </div>
</template>
