import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import InscriptionView from '../views/InscriptionView.vue';

const routes = [
  { path: '/', component: HomeView },
  { path: '/login', component: LoginView },
  { path: '/register', component: InscriptionView }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

router.beforeEach((to, from, next) => {
  const isUserConnected = localStorage.getItem('isUserConnected') === 'true';

  if (isUserConnected && (to.path === '/login' || to.path === '/register')) {
    next({ path: '/' }); 
  } else {
    next();
  }
});

export default router;