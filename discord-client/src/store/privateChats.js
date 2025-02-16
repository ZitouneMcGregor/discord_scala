// store/privateChats.js
import { defineStore } from 'pinia'
import axios from 'axios'

export const usePrivateChatStore = defineStore('privateChats', {
  state: () => ({
    privateChats: []
  }),
  actions: {
    // Récupère la liste des chats privés pour un userId
    async fetchPrivateChats(userId) {
      try {
        const response = await axios.get(`http://localhost:8080/privateChat/user/${userId}`)
        // Si le backend renvoie 404 quand pas de chat, c’est capté => le catch, 
        // ou tu peux gérer autrement
        if (response.status === 200) {
          this.privateChats = response.data
        }
      } catch (error) {
        console.error('Erreur fetchPrivateChats', error)
        // Selon ta logique, tu peux vider la liste ou ignorer
        // this.privateChats = []
      }
    },

    // Créer un private chat
    // suppose qu’on envoie un objet : { user_id_1, user_id_2 }
    async createPrivateChat(userId1, userId2) {
      try {
        const newChat = {
          user_id_1: userId1,
          user_id_2: userId2
        }
        // L’endpoint : POST /privateChat
        const response = await axios.post('http://localhost:8080/privateChat', newChat)
        if (response.status === 201) {
          // On recharge la liste après création
          await this.fetchPrivateChats(userId1)
          return true
        }
      } catch (error) {
        console.error('Erreur createPrivateChat', error)
      }
      return false
    },

    // Supprimer (ou “marquer supprimé”) un chat pour un user
    async deletePrivateChat(userId, chatId) {
      try {
        const response = await axios.delete(`http://localhost:8080/privateChat/${userId}/${chatId}`)
        if (response.status === 200) {
          // On met à jour la liste localement 
          this.privateChats = this.privateChats.filter(c => c.id !== chatId)
        }
      } catch (error) {
        console.error('Erreur deletePrivateChat', error)
      }
    }
  }
})
