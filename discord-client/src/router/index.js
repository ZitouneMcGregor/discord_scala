import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import CreateServerView from '../views/CreateServerView.vue'

const routes = [
  { path: '/', component: HomeView },
  { path: '/login', component: LoginView },
  { path: '/createServer', component: CreateServerView}

]

const router = createRouter({
  history: createWebHistory(),
  routes
})

export default router
