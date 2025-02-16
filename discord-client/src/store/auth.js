import { defineStore } from 'pinia';
import axios from 'axios';

export const useAuthStore = defineStore('auth', {
    state: () => ({
        user: JSON.parse(localStorage.getItem('user')) || null
    }),
    
    getters: {
        isAuthenticated: (state) => !!state.user
    },

    actions: {
        async login(username, password) {
            try {
                const response = await axios.get(`http://localhost:8080/users/${username}`);
                if (response.data.password === password) {
                    this.user = response.data;
                    localStorage.setItem('user', JSON.stringify(response.data));
                    return true;
                }
            } catch (error) {
                console.error('Login error', error);
            }
            return false;
        },

        logout() {
            this.user = null;
            localStorage.removeItem('user');
        },

        async register(username, password) {
            try {
                const response = await axios.post('http://localhost:8080/users', {
                    username, password
                });
                return response.status === 201;
            } catch (error) {
                console.error('Register error', error);
                return false;
            }
        },
        async updateUser(userId, { newUsername, newPassword }) {
            try {
              const response = await axios.put(`http://localhost:8080/users/${userId}`, {
                username: newUsername,
                password: newPassword
              });
              if (response.status === 200) {
                // Mettre à jour l'utilisateur localement si besoin
                this.user.username = newUsername;
                // (si le backend renvoie le user complet, tu peux réassigner `this.user = response.data;`)
                localStorage.setItem('user', JSON.stringify(this.user));
                return true;
              }
            } catch (error) {
              console.error('Erreur updateUser', error);
            }
            return false;
          }
    }
});
