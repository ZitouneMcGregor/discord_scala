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
                const response = await axios.post('http://localhost:8080/users', { username, password });
        
                if (response.data.success === true) {
                    return { success: true, message: "Inscription réussie !" };
                }else if (response.data.success === false) {
                    return { success: false, message: response.data.description };
                }
        
                return { success: false, message: "Réponse inattendue du serveur." };
        
            } catch (error) {
                if (error.response) {
                 return { success: false, message: "Erreur interne du serveur. Réessayez plus tard." }
                }
        
                return { success: false, message: "Problème de connexion. Vérifiez votre réseau." };
            }
        },
        
        async updateUser(userName, { newUsername, newPassword }) {
            try {
              const response = await axios.put(`http://localhost:8080/users/${userName}`, {
                username: newUsername,
                password: newPassword
              });
              if (response.data.success === true) {
                this.user.username = newUsername;
                localStorage.setItem('user', JSON.stringify(this.user));
                return true;
              }else if (response.data.success === false) {
                console.error('Erreur updateUser', response.data.description);
              }
                return false;
            } catch (error) {
              console.error('Erreur updateUser', error);
            }
            return false;
        },
        async deleteAccount(userName) {
            try {
              const response = await axios.delete(`http://localhost:8080/users/${userName}`);
              if (response.data.success === true) {
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
                const response = await axios.get(`http://localhost:8080/users/${username}`); // Assurez-vous que l'URL est correcte ici
                if (response.data.sucess === true) {
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
