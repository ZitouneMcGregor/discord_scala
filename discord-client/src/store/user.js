import { defineStore } from 'pinia';
import axios from 'axios';
const apiUrl = import.meta.env.VITE_API_SCALA_URL

export const useUserStore = defineStore('users', {
  state: () => ({
    allUsers: []
  }),
  actions: {
    async fetchAllUsers() {
      try {
        const response = await axios.get(`${apiUrl}/users`); 
        this.allUsers = response.data;
      } catch (error) {
        console.error('Erreur fetchAllUsers', error);
      }
    }
  }
});
