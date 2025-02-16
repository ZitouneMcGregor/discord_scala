import { defineStore } from 'pinia';
import axios from 'axios';

export const useRoomStore = defineStore('rooms', {
  state: () => ({
    rooms: [],
    selectedRoom: null,
  }),

  actions: {
    async fetchRooms(serverId) {
      try {
        const response = await axios.get(`http://localhost:8080/server/${serverId}/rooms`);
        this.rooms = response.data;
      } catch (error) {
        console.error('Erreur lors de la récupération des rooms', error);
      }
    },

    async addRoom(serverId, roomName) {
      try {
        const response = await axios.post(`http://localhost:8080/server/${serverId}/rooms`, {
          id: null, // ID auto-généré
          id_server: serverId,
          name: roomName
        });
        if (response.status === 201) {
          this.fetchRooms(serverId);
        }
      } catch (error) {
        console.error('Erreur lors de l\'ajout de la room', error);
      }
    },

    async deleteRoom(serverId, roomId) {
      try {
        const response = await axios.delete(`http://localhost:8080/server/${serverId}/rooms`, {
          data: { id: roomId, id_server: serverId }
        });
        if (response.status === 200) {
          this.fetchRooms(serverId);
        }
      } catch (error) {
        console.error('Erreur lors de la suppression de la room', error);
      }
    },

    selectRoom(room) {
      this.selectedRoom = room;
    }
  }
});
