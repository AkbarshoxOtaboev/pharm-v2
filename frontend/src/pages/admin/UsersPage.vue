<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { enumLabel } from '@/i18n'
import {
  blockUser,
  createUser,
  deleteUser,
  fetchUsers,
  unblockUser,
  updateUser,
} from '@/api/users.api'
import { uploadAvatar } from '@/api/auth.api'
import AppBadge from '@/components/ui/AppBadge.vue'
import AppButton from '@/components/ui/AppButton.vue'
import AppInput from '@/components/ui/AppInput.vue'
import AppModal from '@/components/ui/AppModal.vue'
import AppSelect from '@/components/ui/AppSelect.vue'
import AvatarPicker from '@/components/ui/AvatarPicker.vue'
import DataTable from '@/components/ui/DataTable.vue'
import PageHeader from '@/components/ui/PageHeader.vue'
import UserAvatar from '@/components/ui/UserAvatar.vue'
import type { Role, UserDto, UserResponse } from '@/types/api'
import { apiError, statusTone } from '@/utils/format'

const ROLES: Role[] = ['ADMIN', 'OPERATOR', 'COURIER', 'VIEWER']

const { t } = useI18n()
const users = ref<UserResponse[]>([])
const loading = ref(false)
const error = ref('')
const modalOpen = ref(false)
const saving = ref(false)
const editingUsername = ref<string | null>(null)
const formError = ref('')

const form = reactive({
  fullName: '',
  username: '',
  password: '',
  personalPhone: '',
  workPhone: '',
  avatar: 'default-1',
  role: 'OPERATOR' as Role,
})

function resetForm() {
  form.fullName = ''
  form.username = ''
  form.password = ''
  form.personalPhone = ''
  form.workPhone = ''
  form.avatar = 'default-1'
  form.role = 'OPERATOR'
  editingUsername.value = null
  formError.value = ''
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const res = await fetchUsers()
    users.value = res.data || []
  } catch (e) {
    error.value = apiError(e, 'errors.loadUsers')
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  modalOpen.value = true
}

function openEdit(u: UserResponse) {
  editingUsername.value = u.username
  form.fullName = u.fullName
  form.username = u.username
  form.password = ''
  form.personalPhone = u.personalPhone || ''
  form.workPhone = u.workPhone || ''
  form.avatar = u.avatar || 'default-1'
  form.role = u.role
  formError.value = ''
  modalOpen.value = true
}

async function onUpload(file: File) {
  try {
    const res = await uploadAvatar(file)
    if (res.success && res.data?.filename) {
      form.avatar = res.data.filename
    }
  } catch (e) {
    formError.value = apiError(e, 'errors.uploadImage')
  }
}

async function save() {
  formError.value = ''
  if (!form.fullName.trim() || !form.username.trim()) {
    formError.value = t('errors.nameUsernameRequired')
    return
  }
  if (!editingUsername.value && !form.password) {
    formError.value = t('errors.passwordRequired')
    return
  }

  const dto: UserDto = {
    fullName: form.fullName.trim(),
    username: form.username.trim(),
    personalPhone: form.personalPhone.trim() || undefined,
    workPhone: form.workPhone.trim() || undefined,
    avatar: form.avatar || 'default-1',
    role: form.role,
  }
  if (form.password) dto.password = form.password

  saving.value = true
  try {
    if (editingUsername.value) {
      await updateUser(editingUsername.value, dto)
    } else {
      await createUser(dto)
    }
    modalOpen.value = false
    await load()
  } catch (e) {
    formError.value = apiError(e, 'errors.saveFailed')
  } finally {
    saving.value = false
  }
}

async function onDelete(u: UserResponse) {
  if (!confirm(t('common.confirmDeleteNamed', { name: u.fullName }))) return
  try {
    await deleteUser(u.username)
    await load()
  } catch (e) {
    error.value = apiError(e, 'errors.deleteFailed')
  }
}

async function toggleBlock(u: UserResponse) {
  try {
    if (String(u.status).toUpperCase() === 'BLOCKED') {
      await unblockUser(u.username)
    } else {
      await blockUser(u.username)
    }
    await load()
  } catch (e) {
    error.value = apiError(e, 'errors.changeStatus')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <PageHeader :title="t('users.title')" :subtitle="t('users.subtitle')">
      <template #actions>
        <AppButton @click="openCreate">{{ t('common.add') }}</AppButton>
      </template>
    </PageHeader>

    <div
      v-if="error"
      class="mb-4 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
    >
      {{ error }}
    </div>

    <DataTable
      :loading="loading"
      :empty="!loading && !users.length ? t('users.empty') : undefined"
    >
      <template #head>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('users.user') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.login') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.role') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.phone') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.status') }}</th>
        <th class="px-5 py-3 text-left text-theme-xs font-medium text-gray-500 dark:text-gray-400">{{ t('common.actions') }}</th>
      </template>
      <tr v-for="u in users" :key="u.id">
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <div class="flex items-center gap-3">
            <UserAvatar :avatar="u.avatar" :name="u.fullName" size="sm" />
            <span class="font-medium">{{ u.fullName }}</span>
          </div>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ u.username }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">{{ enumLabel('role', u.role) }}</td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          {{ u.personalPhone || u.workPhone || t('common.empty') }}
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <AppBadge :tone="statusTone(String(u.status))">{{ enumLabel('status', String(u.status)) }}</AppBadge>
        </td>
        <td class="px-5 py-3 text-theme-sm text-gray-700 dark:text-gray-300">
          <div class="flex flex-wrap gap-2">
            <AppButton size="sm" variant="secondary" @click="openEdit(u)">{{ t('common.edit') }}</AppButton>
            <AppButton size="sm" variant="ghost" @click="toggleBlock(u)">
              {{ String(u.status).toUpperCase() === 'BLOCKED' ? t('users.unblock') : t('users.block') }}
            </AppButton>
            <AppButton size="sm" variant="danger" @click="onDelete(u)">{{ t('common.delete') }}</AppButton>
          </div>
        </td>
      </tr>
    </DataTable>

    <AppModal
      :open="modalOpen"
      :title="editingUsername ? t('users.editTitle') : t('users.createTitle')"
      @close="modalOpen = false"
    >
      <div
        v-if="formError"
        class="mb-3 rounded-xl border border-error-100 bg-error-50 px-4 py-3 text-theme-sm text-error-600 dark:border-error-500/20 dark:bg-error-500/10"
      >
        {{ formError }}
      </div>
      <form class="space-y-3" @submit.prevent="save">
        <AvatarPicker v-model="form.avatar" allow-upload @upload="onUpload" />
        <AppInput v-model="form.fullName" :label="t('orders.fullName')" />
        <AppInput
          v-model="form.username"
          :label="t('common.login')"
          :disabled="!!editingUsername"
        />
        <AppInput
          v-model="form.password"
          :label="t('common.password')"
          type="password"
          :placeholder="editingUsername ? t('users.passwordKeep') : ''"
        />
        <AppInput v-model="form.personalPhone" :label="t('users.personalPhone')" />
        <AppInput v-model="form.workPhone" :label="t('users.workPhone')" />
        <AppSelect v-model="form.role" :label="t('common.role')">
          <option v-for="r in ROLES" :key="r" :value="r">{{ enumLabel('role', r) }}</option>
        </AppSelect>
        <div class="flex justify-end gap-2 pt-2">
          <AppButton type="button" variant="secondary" @click="modalOpen = false">{{ t('common.cancel') }}</AppButton>
          <AppButton type="submit" :loading="saving">{{ t('common.save') }}</AppButton>
        </div>
      </form>
    </AppModal>
  </div>
</template>
