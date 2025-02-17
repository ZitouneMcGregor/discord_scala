// store/privateChats.js
import { defineStore } from 'pinia'
import axios from 'axios'

export const usePrivateChatStore = defineStore('privateChats', {
  state: () => ({
    privateChats: []
  }),
  actions: {
    async fetchPrivateChats(userId) {
      try {
        const response = await axios.get(`http://34.175.236.217:8080/privateChat/user/${userId}`)
        if (response.status === 200) {
          this.privateChats = response.data
        }
      } catch (error) {
        console.error('Erreur fetchPrivateChats', error)

      }
    },

    async createPrivateChat(userId1, userId2) {
      try {
        const newChat = {
          user_id_1: userId1,
          user_id_2: userId2
        }
        const response = await axios.post('http://34.175.236.217:8080/privateChat', newChat)
        if (response.status === 201) {
          await this.fetchPrivateChats(userId1)
          return true
        }
      } catch (error) {
        console.error('Erreur createPrivateChat', error)
      }
      return false
    },

    async deletePrivateChat(userId, chatId) {
      try {
        const response = await axios.delete(`http://34.175.236.217:8080/privateChat/${userId}/${chatId}`)
        if (response.status === 200) {
          this.privateChats = this.privateChats.filter(c => c.id !== chatId)
        }
      } catch (error) {
        console.error('Erreur deletePrivateChat', error)
      }
    }
  }
})
