import { defineStore } from 'pinia';
import axios from 'axios';

export const useUserStore = defineStore('users', {
  state: () => ({
    allUsers: []
  }),
  actions: {
    async fetchAllUsers() {
      try {
        const response = await axios.get('http://34.175.236.217:8080/users'); 
        this.allUsers = response.data;
      } catch (error) {
        console.error('Erreur fetchAllUsers', error);
      }
    }
  }
});
