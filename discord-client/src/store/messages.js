// store/messages.js
import { defineStore } from 'pinia';
import axios from 'axios';

export const useMessageStore = defineStore('messages', {
  state: () => ({
    messages: []
  }),

  actions: {
    async fetchMessages(serverId, roomId) {
      try {
        const response = await axios.get(`http://localhost:8080/server/${serverId}/rooms/${roomId}/messages`);
        this.messages = response.data;
      } catch (error) {
        console.error('Erreur lors de la récupération des messages', error);
      }
    },

    async sendMessage(serverId, roomId, userId, content) {
      try {
        const response = await axios.post(`http://localhost:8080/server/${serverId}/rooms/${roomId}/messages`, {
          userId,
          content
        });
        if (response.status === 201) {
          this.fetchMessages(serverId, roomId);
        }
      } catch (error) {
        console.error('Erreur lors de l\'envoi du message', error);
      }
    }
  }
});
