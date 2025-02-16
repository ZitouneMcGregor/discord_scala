// src/store/privateChats.js
import { defineStore } from 'pinia'
import axios from 'axios'

export const usePrivateChatStore = defineStore('privateChats', {
  state: () => ({
    privateChats: []
  }),
  actions: {
    async fetchPrivateChats(userId) {
      try {
        const response = await axios.get(`http://localhost:8080/privateChat/user/${userId}`)
        this.privateChats = response.data
      } catch (error) {
        console.error('Erreur fetchPrivateChats', error)
      }
    },
    async createPrivateChat(userId1, userId2) {
        try {
          const newChat = {
            // Adapte à ta structure de données
            id_user1: userId1,
            id_user2: userId2
            // ...
          }
          const response = await axios.post('http://localhost:8080/privateChat', newChat)
          if (response.status === 201) {
            // Succès => recharger la liste, par ex.
            await this.fetchPrivateChats(userId1)
          }
        } catch (error) {
          console.error('Erreur createPrivateChat', error)
        }
      },
    async deletePrivateChat(userId, chatId) {
      try {
        const response = await axios.delete(`http://localhost:8080/privateChat/${userId}/${chatId}`)
        if (response.status === 200) {
          // Rechargement
          this.fetchPrivateChats(userId)
        }
      } catch (error) {
        console.error('Erreur deletePrivateChat', error)
      }
    }
  }
})
