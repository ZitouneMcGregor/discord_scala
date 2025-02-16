import { createRouter, createWebHistory } from 'vue-router';
import { useAuthStore } from '../store/auth';
import HomeView from '../views/HomeView.vue';
import LoginView from '../views/LoginView.vue';
import CreateServerView from '../views/CreateServerView.vue'
import UpdatingServer from '../views/UpdatingServer.vue'

const routes = [
	{ path: '/login', component: LoginView },
	{ path: '/', component: HomeView, meta: {requiresAuth: true}},
	{ path: '/home', component: HomeView, meta: {requiresAuth: true}},
	{ path: '/createServer', component: CreateServerView},
	{ path: '/edit-server/:id', component: UpdatingServer, props: true }
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