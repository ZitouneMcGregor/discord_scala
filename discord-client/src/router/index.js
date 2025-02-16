// router/index.js
import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../store/auth';

// Import de tes vues
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import PrivateChat from '../views/PrivateChat.vue';
import ServerView from '../views/ServerView.vue';
import ProfileView from '../views/ProfileView.vue';

const routes = [
  { path: '/login',name: 'Login', component: LoginView },
  { path: '/', component: HomeView, meta: { requiresAuth: true }},
  { path: '/home', component: HomeView, meta: { requiresAuth: true }},
  
 
  // Ex pour les MP
  { path: '/dm/:friendId', component: PrivateChat, meta: { requiresAuth: true }},

  {
    path: '/server/:serverId',
    name: 'Server',
    component: ServerView,
    meta: { requiresAuth: true }
  },

  {
    path: '/profile',
    name: 'Profile',
    component: ProfileView,
    meta: { requiresAuth: true } 
  },
  {
	path: '/dm/:userId',
	name: 'DMView',
	component: () => import('../views/DMView.vue'),
	meta: { requiresAuth: true }
  }
  
  
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const authStore = useAuthStore();
  if (to.meta.requiresAuth && !authStore.isAuthenticated) {
    next('/login');
  } else {
    next();
  }
});

export default router;
