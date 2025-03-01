import { defineStore } from 'pinia';
import axios from 'axios';

export const useUserStore = defineStore('users', {
  state: () => ({
    allUsers: [],
    inviteUsers: [] // Nouveau state pour les utilisateurs d'invite
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
    async fetchUsersServer(serverId) {
      try {
        const response = await axios.get(`http://localhost:8080/server/${serverId}/userServer`)
        this.usersServer = response.data;
        
      } catch (error) {
        console.error('Error fetching users from the server');
        
      }
    },

    async fetchInviteUsers(serverId) {
      try {
        const response = await axios.get(`http://localhost:8080/server/${serverId}/invite`);
        this.inviteUsers = response.data;
      } catch (error) {
        console.error('Erreur fetchInviteUsers', error);
      }
    }
  }
});
