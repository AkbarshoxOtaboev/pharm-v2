import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth.store'
import type { Role } from '@/types/api'

declare module 'vue-router' {
  interface RouteMeta {
    public?: boolean
    roles?: Role[]
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/pages/LoginPage.vue'),
      meta: { public: true },
    },
    {
      path: '/',
      redirect: () => {
        const auth = useAuthStore()
        return auth.isAuthenticated ? auth.homePath : '/login'
      },
    },
    {
      path: '/admin',
      component: () => import('@/layouts/AdminLayout.vue'),
      meta: { roles: ['ADMIN'] },
      children: [
        {
          path: 'dashboard',
          name: 'admin-dashboard',
          component: () => import('@/pages/admin/DashboardPage.vue'),
        },
        {
          path: 'orders',
          name: 'admin-orders',
          component: () => import('@/pages/admin/OrdersPage.vue'),
        },
        {
          path: 'orders/create',
          name: 'admin-orders-create',
          component: () => import('@/pages/admin/OrderCreatePage.vue'),
        },
        {
          path: 'orders/:id',
          name: 'admin-orders-detail',
          component: () => import('@/pages/admin/OrderDetailPage.vue'),
        },
        {
          path: 'users',
          name: 'admin-users',
          component: () => import('@/pages/admin/UsersPage.vue'),
        },
        {
          path: 'customers',
          name: 'admin-customers',
          component: () => import('@/pages/admin/CustomersPage.vue'),
        },
        {
          path: 'categories',
          name: 'admin-categories',
          component: () => import('@/pages/admin/CategoriesPage.vue'),
        },
        {
          path: 'products',
          name: 'admin-products',
          component: () => import('@/pages/admin/ProductsPage.vue'),
        },
        {
          path: 'store',
          name: 'admin-store',
          component: () => import('@/pages/admin/StorePage.vue'),
        },
        {
          path: 'courier-stock',
          name: 'admin-courier-stock',
          component: () => import('@/pages/admin/CourierStockPage.vue'),
        },
        {
          path: 'cash-register',
          name: 'admin-cash-register',
          component: () => import('@/pages/admin/CashRegisterPage.vue'),
        },
        {
          path: 'sale-logs',
          name: 'admin-sale-logs',
          component: () => import('@/pages/admin/SaleLogsPage.vue'),
        },
        {
          path: 'profile',
          name: 'admin-profile',
          component: () => import('@/pages/admin/ProfilePage.vue'),
        },
      ],
    },
    {
      path: '/operator',
      component: () => import('@/layouts/SimpleLayout.vue'),
      meta: { roles: ['OPERATOR'] },
      children: [
        {
          path: 'dashboard',
          name: 'operator-dashboard',
          component: () => import('@/pages/PlaceholderPage.vue'),
          props: { titleKey: 'placeholder.operator' },
        },
      ],
    },
    {
      path: '/courier',
      component: () => import('@/layouts/SimpleLayout.vue'),
      meta: { roles: ['COURIER'] },
      children: [
        {
          path: 'dashboard',
          name: 'courier-dashboard',
          component: () => import('@/pages/PlaceholderPage.vue'),
          props: { titleKey: 'placeholder.courier' },
        },
      ],
    },
    {
      path: '/viewer',
      component: () => import('@/layouts/SimpleLayout.vue'),
      meta: { roles: ['VIEWER'] },
      children: [
        {
          path: 'dashboard',
          name: 'viewer-dashboard',
          component: () => import('@/pages/PlaceholderPage.vue'),
          props: { titleKey: 'placeholder.viewer' },
        },
      ],
    },
  ],
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  if (to.meta.public) {
    if (auth.isAuthenticated && to.path === '/login') {
      return auth.homePath
    }
    return true
  }
  if (!auth.isAuthenticated) {
    return '/login'
  }
  const roles = to.matched.flatMap((r) => r.meta.roles || [])
  if (roles.length && auth.role && !roles.includes(auth.role)) {
    return auth.homePath
  }
  return true
})

export default router
