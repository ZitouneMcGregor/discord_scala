// src/stores/auth.js
import { defineStore } from 'pinia';
import api from '../plugins/axios';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    user: JSON.parse(localStorage.getItem('user')) || null
  }),

  getters: {
    isAuthenticated: state => !!state.user
  },

  actions: {
    async login(username, password) {
      try {
        const res = await api.get(`/users/${username}`);
        const u = res.data;
        if (u && !u.deleted) {
          this.user = { id: u.id, username: u.username };
          localStorage.setItem('user', JSON.stringify(this.user));
          return { success: true };
        } else if (u && u.deleted) {
          return { success: false, message: 'Compte supprimé.' };
        }
      } catch (err) {
        return { success: false, message: 'Identifiants incorrects.' };
      }
      return { success: false, message: 'Erreur de connexion.' };
    },

    logout() {
      this.user = null;
      localStorage.removeItem('user');
    },

    async register(username, password) {
      try {
        const res = await api.post('/users', { username, password });
        if (res.data.success) {
          return { success: true, message: 'Inscription réussie !' };
        } else {
          return { success: false, message: res.data.description };
        }
      } catch (err) {
        return { success: false, message: 'Erreur réseau ou serveur.' };
      }
    },

    async updateUser(newUsername, newPassword) {
      if (!this.user) return { success: false };

      try {
        const res = await api.put(
          `/users/${this.user.username}`,
          { newUsername, newPassword }
        );

        if (res.data.success) {
          this.user.username = newUsername;
          localStorage.setItem('user', JSON.stringify(this.user));
          return { success: true };
        } else {
          return { success: false, message: res.data.description };
        }
      } catch {
        return { success: false, message: 'Erreur réseau.' };
      }
    },

    async deleteAccount() {
      if (!this.user) return { success: false };
      try {
        const res = await api.delete(`/users/${this.user.username}`);
        if (res.data.success) {
          this.logout();
          return { success: true };
        }
      } catch {
        /* ignore */
      }
      return { success: false, message: 'Impossible de supprimer le compte.' };
    }
  }
});
