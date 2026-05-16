import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/store/auth'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/Login.vue')
    },
    {
      path: '/chat',
      name: 'Chat',
      component: () => import('@/views/Chat.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/profile',
      name: 'Profile',
      component: () => import('@/views/Profile.vue'),
      meta: { requiresAuth: true }
    },
    {
      path: '/admin',
      name: 'AdminLayout',
      component: () => import('@/views/admin/Layout.vue'),
      meta: { requiresAuth: true, requiresAdmin: true },
      children: [
        {
          path: '',
          redirect: '/admin/dashboard'
        },
        {
          path: 'dashboard',
          name: 'Dashboard',
          component: () => import('@/views/admin/Dashboard.vue')
        },
        {
          path: 'knowledge',
          name: 'Knowledge',
          component: () => import('@/views/admin/Knowledge.vue')
        },
        {
          path: 'users',
          name: 'Users',
          component: () => import('@/views/admin/Users.vue')
        },
        {
          path: 'feedbacks',
          name: 'Feedbacks',
          component: () => import('@/views/admin/Feedbacks.vue')
        }
      ]
    },
    {
      path: '/:pathMatch(.*)*',
      redirect: '/chat'
    }
  ]
})

let authInitialized = false

router.beforeEach(async (to, _from, next) => {
  const authStore = useAuthStore()

  if (!authInitialized) {
    authInitialized = true
    await authStore.validateAndInit()
  }

  const isLoggedIn = !!authStore.token

  if (to.meta.requiresAuth && !isLoggedIn) {
    return next('/login')
  }

  if (to.meta.requiresAdmin && authStore.role !== 'ADMIN') {
    return next('/chat')
  }

  if (to.path === '/login' && isLoggedIn) {
    return next(authStore.role === 'ADMIN' ? '/admin/dashboard' : '/chat')
  }

  next()
})

export default router
