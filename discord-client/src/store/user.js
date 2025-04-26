import { defineStore } from 'pinia';
import api from '../plugins/axios';

export const useUserStore = defineStore('users', {
  state: () => ({
    allUsers: [],
    inviteUsers: [] 
  }),
  actions: {
    async fetchAllUsers() {
      try {
        const response = await api.get('http://localhost:8080/users'); 
        this.allUsers = response.data;
      } catch (error) {
        console.error('Erreur fetchAllUsers', error);
      }
    },
    async fetchInviteUsers(serverId) {
      try {
        const response = await api.get(`http://localhost:8080/servers/${serverId}/invite`);
        this.inviteUsers = response.data;
      } catch (error) {
        console.error('Erreur fetchInviteUsers', error);
      }
    }
  }
});
