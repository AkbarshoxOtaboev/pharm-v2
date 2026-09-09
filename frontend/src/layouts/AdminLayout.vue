<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, RouterLink, RouterView } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import NavIcon from '@/components/ui/NavIcon.vue'
import UserMenu from '@/components/layout/UserMenu.vue'

const auth = useAuthStore()
const route = useRoute()
const mobileOpen = ref(false)
const collapsed = ref(false)

const nav = [
  { to: '/admin/dashboard', label: 'Dashboard', icon: 'dashboard' },
  { to: '/admin/orders', label: 'Buyurtmalar', icon: 'orders' },
  { to: '/admin/customers', label: 'Mijozlar', icon: 'customers' },
  { to: '/admin/users', label: 'Foydalanuvchilar', icon: 'users' },
  { to: '/admin/categories', label: 'Kategoriyalar', icon: 'categories' },
  { to: '/admin/products', label: 'Mahsulotlar', icon: 'products' },
  { to: '/admin/store', label: 'Ombor', icon: 'store' },
  { to: '/admin/courier-stock', label: 'Kuryer zaxira', icon: 'courier' },
  { to: '/admin/cash-register', label: 'Naqd pul', icon: 'cash' },
  { to: '/admin/sale-logs', label: 'Sotuv loglari', icon: 'sales' },
]

const pageTitle = computed(() => {
  if (route.path.startsWith('/admin/profile')) return 'Profil'
  return nav.find((n) => route.path.startsWith(n.to))?.label || 'Admin'
})

onMounted(() => {
  auth.loadMe().catch(() => undefined)
})
</script>

<template>
  <div class="min-h-screen xl:flex">
    <div
      v-if="mobileOpen"
      class="fixed inset-0 z-40 bg-gray-900/50 xl:hidden"
      @click="mobileOpen = false"
    />

    <aside
      class="fixed top-0 left-0 z-50 flex h-screen flex-col overflow-y-auto border-r border-gray-200 bg-white px-4 transition-all duration-300 xl:static"
      :class="[
        mobileOpen ? 'translate-x-0' : '-translate-x-full xl:translate-x-0',
        collapsed ? 'w-[90px]' : 'w-[290px]',
      ]"
    >
      <div class="flex items-center justify-between pt-8 pb-7">
        <RouterLink to="/admin/dashboard" class="flex items-center gap-2">
          <span
            class="flex size-10 items-center justify-center rounded-xl bg-brand-500 text-sm font-bold text-white"
          >
            P
          </span>
          <span v-if="!collapsed" class="text-xl font-bold tracking-wide text-gray-800">
            PHARM
          </span>
        </RouterLink>
        <button
          class="hidden rounded-lg p-2 text-gray-500 hover:bg-gray-100 xl:inline-flex"
          type="button"
          @click="collapsed = !collapsed"
        >
          ☰
        </button>
      </div>

      <nav class="mb-6">
        <h3
          class="mb-4 text-xs uppercase leading-5 text-gray-400"
          :class="collapsed ? 'text-center' : ''"
        >
          {{ collapsed ? '•' : 'MENU' }}
        </h3>
        <ul class="flex flex-col gap-1">
          <li v-for="item in nav" :key="item.to">
            <RouterLink
              :to="item.to"
              class="menu-item group"
              :class="[
                collapsed ? 'justify-center px-2' : '',
                route.path.startsWith(item.to) ? 'menu-item-active' : 'menu-item-inactive',
              ]"
              :title="item.label"
              @click="mobileOpen = false"
            >
              <NavIcon :name="item.icon" :active="route.path.startsWith(item.to)" />
              <span v-if="!collapsed">{{ item.label }}</span>
            </RouterLink>
          </li>
        </ul>
      </nav>
    </aside>

    <div class="flex-1 overflow-x-hidden">
      <header
        class="sticky top-0 z-30 flex w-full border-b border-gray-200 bg-white px-4 py-3 md:px-6"
      >
        <div class="flex w-full items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <button
              class="rounded-lg border border-gray-200 p-2 text-gray-600 xl:hidden"
              type="button"
              @click="mobileOpen = true"
            >
              ☰
            </button>
            <div>
              <p class="text-theme-xs text-gray-400">Admin panel</p>
              <h2 class="text-theme-sm font-semibold text-gray-800">{{ pageTitle }}</h2>
            </div>
          </div>

          <div class="flex items-center gap-2 sm:gap-3">
            <button
              type="button"
              class="relative flex size-10 items-center justify-center rounded-full border border-gray-200 text-gray-500 hover:bg-gray-50"
              title="Bildirishnomalar"
            >
              <svg class="size-5" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7">
                <path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9" />
                <path d="M13.73 21a2 2 0 0 1-3.46 0" />
              </svg>
              <span class="absolute top-2 right-2 size-2 rounded-full bg-warning-500" />
            </button>
            <UserMenu />
          </div>
        </div>
      </header>

      <main class="mx-auto max-w-screen-2xl p-4 md:p-6">
        <RouterView />
      </main>
    </div>
  </div>
</template>
