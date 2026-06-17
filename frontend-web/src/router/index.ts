import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '@/stores/auth';

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      name: 'login',
      component: () => import('@/views/LoginView.vue'),
      meta: { public: true },
    },
    { path: '/', name: 'dashboard', component: () => import('@/views/DashboardView.vue') },
    { path: '/lots', name: 'lots', component: () => import('@/views/LotsView.vue') },
    { path: '/alertes', name: 'alertes', component: () => import('@/views/AlertesView.vue') },
    {
      path: '/entrepots/:codePays/:id',
      name: 'entrepot-detail',
      component: () => import('@/views/EntrepotDetailView.vue'),
    },
    { path: '/:pathMatch(.*)*', redirect: '/' },
  ],
});

router.beforeEach((to) => {
  const auth = useAuthStore();
  if (!to.meta.public && !auth.isAuthenticated) {
    return { name: 'login', query: { redirect: to.fullPath } };
  }
  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' };
  }
  return true;
});
