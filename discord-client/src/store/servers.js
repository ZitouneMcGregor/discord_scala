import { defineStore } from 'pinia';
import axios from 'axios';

export const useServerStore = defineStore('servers', {
  state: () => ({
    servers: [], // Tous les serveurs
    userServers: [], // Serveurs de l'utilisateur
  }),

  actions: {
    async fetchServers() {
      try {
        const response = await axios.get('http://localhost:8080/server');
        this.servers = response.data;
      } catch (error) {
        console.error('Erreur lors de la récupération des serveurs', error);
      }
    },

    async fetchUserServers(userId) {
      try {
        console.log(userId);
        const response = await axios.get(`http://localhost:8080/users/${userId}/servers`);
        this.userServers = response.data;
      } catch (error) {
        console.error('Erreur lors de la récupération des serveurs de l\'utilisateur', error);
      }
    },

    async joinServer(userId, serverId) {
      try {
        const response = await axios.post(`http://localhost:8080/server/${serverId}/userServer`, {
          user_id: userId,
          server_id: serverId
        });
        if (response.status === 201) {
          this.fetchUserServers(userId);
        }
      } catch (error) {
        console.error('Erreur lors de la jonction du serveur', error);
      }
    },

    async leaveServer(userId, serverId) {
      try {
        const response = await axios.delete(`http://localhost:8080/server/${serverId}/userServer`, {
          data: { user_id: userId, server_id: serverId }
        });
        if (response.status === 200) {
          this.fetchUserServers(userId);
        }
      } catch (error) {
        console.error('Erreur lors du départ du serveur', error);
      }
    }
  }
});
