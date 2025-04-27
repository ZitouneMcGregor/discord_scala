// store/servers.js
import { defineStore } from 'pinia';
import axios from 'axios';
import api from '../plugins/axios'
import { wsService } from '../store/wsService'   // ← bien importer
import { useRoomStore }  from '../store/room'          // ← bien importer

export const useServerStore = defineStore('servers', {
  state: () => ({
    allServers: [],
    userServers: [],
    unjoinedServers: [],
    serverUsers: { users: [] },
    userMap: {} // new state to store id -> username mapping
  }),

  actions: {
    async fetchAllServers() {
      try {
        const response = await axios.get('http://localhost:8080/servers');
        this.allServers = response.data.servers;
      } catch (error) {
        console.error('Erreur fetchAllServers', error);
      }
    },

    async fetchUserServers(userId) {
      try {
        const { data } = await api.get(`/users/${userId}/servers`)
        this.userServers = data.servers || []
      } catch (err) {
        console.error('[servers] fetchUserServers error', err)
        this.userServers = []
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

    async createServer(serverName, serverImage, userId) {
      try {
        const response = await axios.post('http://localhost:8080/servers', {
          name: serverName,
          img: serverImage
        });

        if (response.status === 201) {
          const serverListResponse = await axios.get("http://localhost:8080/servers");
          const latestServer = serverListResponse.data.servers.slice(-1)[0];
          const newServerId = latestServer?.id;

          if (!newServerId) {
            console.error("Could not determine new server ID");
            return false;
          }

          const addResp = await axios.post(`http://localhost:8080/servers/${newServerId}/users`, {
            user_id: userId,
            server_id: newServerId,
            admin: true
          });
          if (addResp.status === 201) {
            console.log('User ajouté comme admin sur le nouveau serveur.');
          } else {
            console.error('Erreur pour ajouter l’utilisateur comme admin', addResp);
          }
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
          `http://localhost:8080/servers/${serverId}/users`,
          {
            user_id: userId,
            server_id: serverId,
            admin: false
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
        const response = await axios.delete(`http://localhost:8080/servers/${serverId}/users`, {
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
        const response = await axios.put(`http://localhost:8080/servers/${serverData.id}`, {
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

    async fetchServerUsers(serverId) {
      try {
        const response = await axios.get(`http://localhost:8080/servers/${serverId}/users`);
        this.serverUsers = response.data;
        const userIds = this.serverUsers.users.map(u => u.user_id);
        await this.fetchUserMap(userIds);
      } catch (error) {
        console.error('Erreur fetchServerUsers', error);
      }
    },

    async deleteServer(serverId) {
      try {
        const response = await axios.delete(`http://localhost:8080/servers/${serverId}`);
        if (response.status === 200) {
          console.log(`Serveur ${serverId} supprimé avec succès`);
          await this.fetchAllServers();
        }
      } catch (error) {
        console.error('Erreur lors de la suppression du serveur', error);
      }
    },

    async fetchUserMap(userIds) {
      try {
        const entries = await Promise.all(
          userIds.map(async id => {
            const response = await axios.get(`http://localhost:8080/users/id/${id}`);
            return [id, response.data.username];
          })
        );
        this.userMap = Object.fromEntries(entries);
      } catch (error) {
        console.error('Erreur lors du fetchUserMap', error);
      }
    },

    async kickUser(serverId, userId) {
      try {
        const res = await axios.delete(
          `http://localhost:8080/servers/${serverId}/users/${userId}`
        )
        if (res.status === 200) {
          console.log(`Utilisateur ${userId} retiré du serveur ${serverId}`)
  
          await this.fetchServerUsers(serverId)
  
          const toUnsubscribe = new Set(
            useRoomStore.rooms
              .filter(r => r.serverId === serverId)
              .map(r => `r${r.id}`)
          )
  
          wsService.removeWatchIds(toUnsubscribe)
  
          console.log(
            'WS unsubscribed from rooms:',
            Array.from(toUnsubscribe).join(', ')
          )
        }
      } catch (err) {
        console.error('Erreur lors du kick:', err)
      }
    },

    async toggleAdmin(serverId, userId, isAdmin) {
      try {
        const response = await axios.put(`http://localhost:8080/servers/${serverId}/users/${userId}`, {
          admin: isAdmin
        });
        if (response.status === 200) {
          console.log(`Admin ${isAdmin ? 'ajouté à' : 'retiré de'} l'utilisateur ${userId}`);
          await this.fetchServerUsers(serverId);
        }
      } catch (error) {
        console.error(`Erreur lors de la mise à jour du rôle admin de l'utilisateur ${userId}`, error);
      }
    }
  }
});
