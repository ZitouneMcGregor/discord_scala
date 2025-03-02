import { createApp } from 'vue'
import './style.css'
import { createPinia } from 'pinia'
import router from './router'
import App from './App.vue'

const app = createApp(App)
const pinia = createPinia()
app.use(router)
app.use(pinia)
app.mount('#app')

import axios from 'axios';

// Fonction pour générer le header `Authorization: Basic`
function getAuthHeader() {
    const username = import.meta.env.VITE_BASIC_AUTH_USER || "foo"; // Remplace par ton username par défaut
    const password = import.meta.env.VITE_BASIC_AUTH_PASSWORD || "bar";
    return `Basic ${btoa(`${username}:${password}`)}`;
}

// Ajout automatique de `Authorization: Basic ...` dans chaque requête Axios
axios.interceptors.request.use(
    (config) => {
        config.headers['Authorization'] = getAuthHeader();
        return config;
    },
    (error) => Promise.reject(error)
);
