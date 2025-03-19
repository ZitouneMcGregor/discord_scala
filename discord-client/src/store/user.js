import { defineStore } from 'pinia';
import axios from 'axios';

export const useUserStore = defineStore('users', {
  state: () => ({
    allUsers: [],
    inviteUsers: [] 
  }),
  actions: {
    async fetchAllUsers() {
      try {
        const response = await axios.get('http://localhost:8080/users'); 
        this.allUsers = response.data;
      } catch (error) {
        console.error('Erreur fetchAllUsers', error);
      }
    },
    async fetchInviteUsers(serverId) {
      try {
        const response = await axios.get(`http://localhost:8080/servers/${serverId}/invite`);
        this.inviteUsers = response.data;
      } catch (error) {
        console.error('Erreur fetchInviteUsers', error);
      }
    }
  }
});
