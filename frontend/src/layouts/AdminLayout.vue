<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, RouterLink, RouterView } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { useAuthStore } from '@/stores/auth.store'
import NavIcon from '@/components/ui/NavIcon.vue'
import ThemeToggle from '@/components/layout/ThemeToggle.vue'
import LocaleSwitcher from '@/components/layout/LocaleSwitcher.vue'
import UserMenu from '@/components/layout/UserMenu.vue'

const auth = useAuthStore()
const route = useRoute()
const { t } = useI18n()
const mobileOpen = ref(false)
const collapsed = ref(false)

const nav = computed(() => [
  { to: '/admin/dashboard', label: t('nav.dashboard'), icon: 'dashboard' },
  { to: '/admin/orders', label: t('nav.orders'), icon: 'orders' },
  { to: '/admin/customers', label: t('nav.customers'), icon: 'customers' },
  { to: '/admin/users', label: t('nav.users'), icon: 'users' },
  { to: '/admin/categories', label: t('nav.categories'), icon: 'categories' },
  { to: '/admin/products', label: t('nav.products'), icon: 'products' },
  { to: '/admin/store', label: t('nav.store'), icon: 'store' },
  { to: '/admin/courier-stock', label: t('nav.courierStock'), icon: 'courier' },
  { to: '/admin/cash-register', label: t('nav.cash'), icon: 'cash' },
  { to: '/admin/sale-logs', label: t('nav.saleLogs'), icon: 'sales' },
])

const pageTitle = computed(() => {
  if (route.path.startsWith('/admin/profile')) return t('nav.profile')
  return nav.value.find((n) => route.path.startsWith(n.to))?.label || t('app.adminPanel')
})

onMounted(() => {
  auth.loadMe().catch(() => undefined)
})
</script>

<template>
  <div class="flex h-dvh overflow-hidden bg-gray-50 dark:bg-gray-900">
    <div
      v-if="mobileOpen"
      class="fixed inset-0 z-40 bg-gray-900/50 xl:hidden"
      @click="mobileOpen = false"
    />

    <aside
      class="fixed top-0 left-0 z-50 flex h-dvh flex-col overflow-y-auto border-r border-gray-200 bg-white px-4 transition-all duration-300 xl:static xl:shrink-0 dark:border-gray-800 dark:bg-gray-dark"
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
          <span v-if="!collapsed" class="text-xl font-bold tracking-wide text-gray-800 dark:text-white/90">
            {{ t('app.name') }}
          </span>
        </RouterLink>
        <button
          class="hidden rounded-lg p-2 text-gray-500 hover:bg-gray-100 xl:inline-flex dark:text-gray-400 dark:hover:bg-white/5"
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
          {{ collapsed ? '•' : t('app.menu') }}
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

    <div class="flex min-h-0 min-w-0 flex-1 flex-col overflow-hidden">
      <header
        class="z-30 flex w-full shrink-0 border-b border-gray-200 bg-white px-4 py-3 md:px-6 dark:border-gray-800 dark:bg-gray-dark"
      >
        <div class="flex w-full items-center justify-between gap-3">
          <div class="flex items-center gap-3">
            <button
              class="rounded-lg border border-gray-200 p-2 text-gray-600 xl:hidden dark:border-gray-800 dark:text-gray-400"
              type="button"
              @click="mobileOpen = true"
            >
              ☰
            </button>
            <div>
              <p class="text-theme-xs text-gray-400">{{ t('app.adminPanel') }}</p>
              <h2 class="text-theme-sm font-semibold text-gray-800 dark:text-white/90">{{ pageTitle }}</h2>
            </div>
          </div>

          <div class="flex items-center gap-2 sm:gap-3">
            <LocaleSwitcher />
            <ThemeToggle />
            <UserMenu />
          </div>
        </div>
      </header>

      <main class="min-h-0 flex-1 overflow-y-auto overflow-x-hidden">
        <div class="mx-auto max-w-screen-2xl p-4 md:p-6">
          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>
