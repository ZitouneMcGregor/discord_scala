// store/servers.js
import { defineStore } from 'pinia';
import axios from 'axios';

export const useServerStore = defineStore('servers', {
  state: () => ({
    allServers: [],      // Tous les serveurs
    userServers: [],     // Serveurs de l'user
    unjoinedServers: [], // Serveurs pas encore rejoints (calculés)
  }),

  actions: {
    // 1. Récupère la liste de TOUS les serveurs
    async fetchAllServers() {
      try {
        const response = await axios.get('http://localhost:8080/server');
        this.allServers = response.data;
      } catch (error) {
        console.error('Erreur fetchAllServers', error);
      }
    },

    // 2. Récupère la liste des serveurs auxquels l'user est déjà
    async fetchUserServers(userId) {
      try {
        const response = await axios.get(`http://localhost:8080/users/${userId}/servers`);
        this.userServers = response.data;
      } catch (error) {
        console.error('Erreur fetchUserServers', error);
      }
    },

    

    // 3. Récupère tout + filtre localement (pas “propre” mais ça marche)
    async fetchUnjoinedServers(userId) {
      await this.fetchAllServers();
      await this.fetchUserServers(userId);
      const userServerIds = this.userServers.map(s => s.id);
      this.unjoinedServers = this.allServers.filter(
        srv => !userServerIds.includes(srv.id)
      );
    },

    // 4. Crée un serveur (name, img)
    async createServer(serverName, serverImage) {
      try {
        const response = await axios.post('http://localhost:8080/server', {
          name: serverName,
          img: serverImage
        });
        if (response.status === 201) {
          // On recharge la liste globale
          await this.fetchAllServers();
          return true;
        }
      } catch (error) {
        console.error('Erreur createServer', error);
      }
      return false;
    },

    // 5. Permet à l'utilisateur de rejoindre un serveur
    async joinServer(userId, serverId) {
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
          // Mise à jour localement : on enlève ce serveur de unjoinedServers
          this.unjoinedServers = this.unjoinedServers.filter(
            s => s.id !== serverId
          );
          // Re-fetch userServers pour la sidebar
          await this.fetchUserServers(userId);
        }
      } catch (error) {
        console.error('Erreur joinServer', error);
      }
    },

    // 6. Quitter un serveur
    async leaveServer(userId, serverId) {
      try {
        const response = await axios.delete(`http://localhost:8080/server/${serverId}/userServer`, {
          data: { user_id: userId, server_id: serverId }
        });
        if (response.status === 200) {
          console.log(`Utilisateur ${userId} a quitté le serveur ${serverId}`);
          // Optionnel : recharger la liste userServers
          // await this.fetchUserServers(userId);
        }
      } catch (error) {
        console.error('Erreur lors du départ du serveur', error);
      }
    },

    // 7. Mettre à jour un serveur
    async updateServer(serverData) {
      // serverData = { id, name, image }
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
    }
  }
});
