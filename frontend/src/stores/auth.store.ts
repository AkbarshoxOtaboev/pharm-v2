import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import * as authApi from '@/api/auth.api'
import type { MeResponse, Role } from '@/types/api'

const ROLE_HOME: Record<Role, string> = {
  ADMIN: '/admin/dashboard',
  OPERATOR: '/operator/dashboard',
  COURIER: '/courier/dashboard',
  VIEWER: '/viewer/dashboard',
}

export const useAuthStore = defineStore('auth', () => {
  const accessToken = ref<string | null>(localStorage.getItem('accessToken'))
  const refreshToken = ref<string | null>(localStorage.getItem('refreshToken'))
  const userId = ref<number | null>(Number(localStorage.getItem('userId')) || null)
  const username = ref(localStorage.getItem('username') || '')
  const fullName = ref(localStorage.getItem('fullName') || '')
  const avatar = ref(localStorage.getItem('avatar') || 'default-1')
  const personalPhone = ref(localStorage.getItem('personalPhone') || '')
  const workPhone = ref(localStorage.getItem('workPhone') || '')
  const role = ref<Role | null>((localStorage.getItem('role') as Role) || null)

  const isAuthenticated = computed(() => !!accessToken.value)
  const homePath = computed(() => (role.value ? ROLE_HOME[role.value] : '/login'))

  function persist() {
    if (accessToken.value) localStorage.setItem('accessToken', accessToken.value)
    else localStorage.removeItem('accessToken')
    if (refreshToken.value) localStorage.setItem('refreshToken', refreshToken.value)
    else localStorage.removeItem('refreshToken')
    if (userId.value != null) localStorage.setItem('userId', String(userId.value))
    localStorage.setItem('username', username.value)
    localStorage.setItem('fullName', fullName.value)
    localStorage.setItem('avatar', avatar.value || 'default-1')
    localStorage.setItem('personalPhone', personalPhone.value || '')
    localStorage.setItem('workPhone', workPhone.value || '')
    if (role.value) localStorage.setItem('role', role.value)
    else localStorage.removeItem('role')
  }

  function applyMe(me: MeResponse) {
    userId.value = me.id
    username.value = me.username
    fullName.value = me.fullName
    avatar.value = me.avatar || 'default-1'
    personalPhone.value = me.personalPhone || ''
    workPhone.value = me.workPhone || ''
    role.value = me.role
    persist()
  }

  async function login(user: string, password: string) {
    const res = await authApi.login(user, password)
    if (!res.success || !res.data) {
      throw new Error(res.error?.message || 'Login xatosi')
    }
    accessToken.value = res.data.accessToken
    refreshToken.value = res.data.refreshToken
    userId.value = res.data.userId
    username.value = res.data.username
    fullName.value = res.data.fullName
    avatar.value = res.data.avatar || 'default-1'
    role.value = res.data.role
    persist()
    try {
      const me = await authApi.fetchMe()
      if (me.success && me.data) applyMe(me.data)
    } catch {
      /* ignore */
    }
    return ROLE_HOME[res.data.role]
  }

  async function loadMe() {
    if (!accessToken.value) return
    const me = await authApi.fetchMe()
    if (me.success && me.data) applyMe(me.data)
  }

  async function updateProfile(payload: {
    fullName?: string
    personalPhone?: string
    workPhone?: string
    avatar?: string
  }) {
    const res = await authApi.updateProfile(payload)
    if (!res.success || !res.data) {
      throw new Error(res.error?.message || 'Profilni saqlab bo‘lmadi')
    }
    applyMe(res.data)
  }

  async function changePassword(currentPassword: string, newPassword: string) {
    const res = await authApi.changePassword({ currentPassword, newPassword })
    if (!res.success) {
      throw new Error(res.error?.message || 'Parolni o‘zgartirib bo‘lmadi')
    }
  }

  async function logout() {
    try {
      await authApi.logout()
    } catch {
      /* ignore */
    }
    accessToken.value = null
    refreshToken.value = null
    userId.value = null
    username.value = ''
    fullName.value = ''
    avatar.value = 'default-1'
    personalPhone.value = ''
    workPhone.value = ''
    role.value = null
    persist()
  }

  return {
    accessToken,
    refreshToken,
    userId,
    username,
    fullName,
    avatar,
    personalPhone,
    workPhone,
    role,
    isAuthenticated,
    homePath,
    login,
    logout,
    loadMe,
    updateProfile,
    changePassword,
  }
})
