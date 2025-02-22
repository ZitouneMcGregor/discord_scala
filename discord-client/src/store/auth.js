import { defineStore } from 'pinia';
import axios from 'axios';
const apiUrl = import.meta.env.VITE_API_SCALA_URL
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
                const response = await axios.get(`${apiUrl}/users/${username}`);
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
                const response = await axios.post(`${apiUrl}/users`, {
                    username, password
                });
                return response.status === 201;
            } catch (error) {
                console.error('Register error', error);
                return false;
            }
        },
        async updateUser(userName, { newUsername, newPassword }) {
            try {
              const response = await axios.put(`${apiUrl}/users/${userName}`, {
                username: newUsername,
                password: newPassword
              });
              if (response.status === 200) {
                this.user.username = newUsername;
                localStorage.setItem('user', JSON.stringify(this.user));
                return true;
              }
            } catch (error) {
              console.error('Erreur updateUser', error);
            }
            return false;
        },
        async deleteAccount(userName) {
            try {
              const response = await axios.delete(`${apiUrl}/users/${userName}`);
              if (response.status === 200) {
                this.user = null;
                localStorage.removeItem('user');
                return true;
              }
            } catch (error) {
              console.error('Erreur deleteAccount', error);
            }
            return false;
        },
        async getUser(username) {
            try {
                const response = await axios.get(`${apiUrl}/users/${username}`); // Assurez-vous que l'URL est correcte ici
                if (response.status === 200) {
                    return response.data; // L'utilisateur retourné contient un champ "deleted"
                } else {
                    throw new Error('Utilisateur introuvable');
                }
            } catch (error) {
                console.error(error);
                return null;
            }
        }
              

    }
});
