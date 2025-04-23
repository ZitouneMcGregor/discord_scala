import { defineStore } from 'pinia';
import axios from 'axios';

export const useRoomStore = defineStore('rooms', {
  state: () => ({
    rooms: [],
    selectedRoom: null,
    serverId: null,
  }),

  actions: {
    setServerId(serverId) {
      this.serverId = serverId;
    },

    async fetchRooms(serverId) {
      try {
        if (!serverId) throw new Error('serverId is required');
        const response = await axios.get(`http://localhost:8080/servers/${serverId}/rooms`);
        this.rooms = Array.isArray(response.data.rooms) ? response.data.rooms : [];
      } catch (error) {
        console.error('Erreur lors de la récupération des rooms', error);
        this.rooms = [];
      }
    },

    async addRoom(serverId, roomName) {
      try {
        const roomPayload = {
          name: roomName,
          serverId: parseInt(serverId),
        };
        const response = await axios.post('http://localhost:8080/rooms', roomPayload);
        if (response.status === 201) {
          await this.fetchRooms(serverId);
        }
      } catch (error) {
        console.error("Erreur lors de l'ajout de la room", error);
      }
    },

    async updateRoom(roomId, newName, serverId) {
      try {
        const roomPayload = {
          id: parseInt(roomId),
          name: newName,
          serverId: serverId,
        };
        const response = await axios.put(`http://localhost:8080/rooms/${roomId}`, roomPayload);
        if (response.status === 200) {
          console.log('Room mise à jour avec succès');
          await this.fetchRooms(serverId);
        }
      } catch (error) {
        console.error('Erreur lors de la mise à jour de la room', error);
      }
    },

    async deleteRoom(roomId, serverId) {
      try {
        const response = await axios.delete(`http://localhost:8080/rooms/${roomId}`);
        if (response.status === 200) {
          console.log('Room supprimée avec succès');
          await this.fetchRooms(serverId);
        }
      } catch (error) {
        console.error('Erreur lors de la suppression de la room', error);
      }
    },
  },
});