import { defineStore } from 'pinia';
import axios from 'axios';
const apiUrl = import.meta.env.VITE_API_SCALA_URL

export const useRoomStore = defineStore('rooms', {
  state: () => ({
    rooms: [],
    selectedRoom: null,
  }),

  actions: {
    async fetchRooms(serverId) {
      try {
        const response = await axios.get(`${apiUrl}/server/${serverId}/rooms`);
        this.rooms = response.data;
      } catch (error) {
        console.error('Erreur lors de la récupération des rooms', error);
      }
    },

    async addRoom(serverId, roomName) {
      try {
        const response = await axios.post(`${apiUrl}/server/${serverId}/rooms`, {
          name: roomName
        });
        if (response.status === 201) {
          await this.fetchRooms(serverId);
        }
      } catch (error) {
        console.error('Erreur lors de l\'ajout de la room', error);
      }
    },

    async updateRoom({ serverId, roomId, newName }) {
      try {
        const response = await axios.put(`${apiUrl}/server/${serverId}/rooms`, {
          id: roomId,
          name: newName
        });
        if (response.status === 200) {
          console.log('Room mise à jour avec succès');
        }
      } catch (error) {
        console.error('Erreur lors de la mise à jour de la room', error);
      }
    },

    async deleteRoom(serverId, roomId) {
      try {
        const response = await axios.delete(`${apiUrl}/server/${serverId}/rooms`, {
          data: {
            id: parseInt(roomId,10),
            name: "",
            id_server: parseInt(serverId,10) 
          }
        });
        if (response.status === 200) {
          console.log('Room supprimée avec succès');
        }
      } catch (error) {
        console.error('Erreur lors de la suppression de la room', error);
      }
    }
  }
});
