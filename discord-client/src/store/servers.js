// store/servers.js
import { defineStore } from 'pinia';
import axios from 'axios';

export const useServerStore = defineStore('servers', {
  state: () => ({
    allServers: [],
    userServers: [],
    unjoinedServers: [],
  }),

  actions: {
    async fetchAllServers() {
      try {
        const response = await axios.get('http://localhost:8080/server');
        this.allServers = response.data;
      } catch (error) {
        console.error('Erreur fetchAllServers', error);
      }
    },

    async fetchUserServers(userId) {
      try {
        const response = await axios.get(`http://localhost:8080/users/${userId}/servers`);
        this.userServers = response.data;
      } catch (error) {
        console.error('Erreur fetchUserServers', error);
      }
    },

    

    async fetchUnjoinedServers(userId) {
      await this.fetchAllServers();
      await this.fetchUserServers(userId);
      const userServerIds = this.userServers.map(s => s.id);
      this.unjoinedServers = this.allServers.filter(
        srv => !userServerIds.includes(srv.id)
      );
    },

    async createServer(serverName, serverImage) {
      try {
        const response = await axios.post('http://localhost:8080/server', {
          name: serverName,
          img: serverImage
        });
        if (response.status === 201) {
          await this.fetchAllServers();
          return true;
        }
      } catch (error) {
        console.error('Erreur createServer', error);
      }
      return false;
    },

    async addUserOnServer(userId, serverId) {
      try {
        const response = await axios.post(
          `http://localhost:8080/server/${serverId}/userServer`,
          {
            user_id: userId,
            server_id: serverId
          }
        );
        if (response.status === 201) {
          console.log(`User ${userId} a rejoint le serveur ${serverId}`);
          this.unjoinedServers = this.unjoinedServers.filter(
            s => s.id !== serverId
          );
          await this.fetchUserServers(userId);
        }
      } catch (error) {
        if (error.response && error.response.status === 400) {
          console.error(`Limite d'utilisateurs atteinte pour le serveur ${serverId}`);
        } else {
          console.error('Erreur joinServer', error);
        }     
      }
    },

    async leaveServer(userId, serverId) {
      try {
        const response = await axios.delete(`http://localhost:8080/server/${serverId}/userServer`, {
          data: { user_id: userId, server_id: serverId }
        });
        if (response.status === 200) {
          console.log(`Utilisateur ${userId} a quitté le serveur ${serverId}`);
        }
      } catch (error) {
        console.error('Erreur lors du départ du serveur', error);
      }
    },

    async updateServer(serverData) {
      try {
        const response = await axios.put(`http://localhost:8080/server/${serverData.id}`, {
          name: serverData.name,
          img: serverData.image
        });
        if (response.status === 200) {
          console.log('Serveur mis à jour avec succès');
        }
      } catch (error) {
        console.error('Erreur lors de la mise à jour du serveur', error);
      }
    },

    async deleteServer(serverId) {
      try {
        const response = await axios.delete(`http://localhost:8080/server/${serverId}`);
        if (response.status === 200) {
          console.log(`Serveur ${serverId} supprimé avec succès`);
          await this.fetchAllServers();
        }
      } catch (error) {
        console.error('Erreur lors de la suppression du serveur', error);
      }
    }
  }
});
