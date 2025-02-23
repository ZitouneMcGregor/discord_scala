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
        
                if (response.status === 201) {
                    return { success: true, message: "Inscription réussie !" };
                }
        
                return { success: false, message: "Réponse inattendue du serveur." };
        
            } catch (error) {
                if (error.response) {
                    if (error.response.status === 409) {
                        return { success: false, message: "Désolé, ce nom d'utilisateur est déjà pris." };
                    } else if (error.response.status === 500) {
                        return { success: false, message: "Erreur interne du serveur. Réessayez plus tard." };
                    }
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
              const response = await axios.delete(`http://localhost:8080/users/${userName}`);
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
                const response = await axios.get(`http://localhost:8080/users/${username}`); // Assurez-vous que l'URL est correcte ici
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
