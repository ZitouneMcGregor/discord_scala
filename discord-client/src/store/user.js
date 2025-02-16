// store/users.js
import { defineStore } from 'pinia';
import axios from 'axios';

export const useUserStore = defineStore('users', {
  state: () => ({
    allUsers: []
  }),
  actions: {
    async fetchAllUsers() {
      try {
        const response = await axios.get('http://localhost:8080/users'); 
        // Adapt selon ta route
        this.allUsers = response.data;
      } catch (error) {
        console.error('Erreur fetchAllUsers', error);
      }
    }
  }
});
