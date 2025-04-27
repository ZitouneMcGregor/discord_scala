// stores/privateChats.js
import { defineStore } from 'pinia'
import api       from '../plugins/axios'
import axios from 'axios'
import { wsService } from './wsService'
import { useAuthStore } from '../store/auth'

export const usePrivateChatStore = defineStore('privateChats', {
  state: () => ({
    privateChats: [], 
    watched     : new Set(), 
    messages    : {},
    selectedChat: null,
  }),

  getters: {
    currentMessages (s) {
      return s.selectedChat ? (s.messages[`pc${s.selectedChat.id}`] || []) : []
    },
    watchIds (s) { 
      return s.privateChats.map(c => `pc${c.id}`)
    }
  },

  actions: {
    /* ---------------------- init / synchro ---------------------- */
    async fetchPrivateChats (userId) {
      try {
        const { data } = await api.get(`/privateChat/user/${userId}`)
        this.privateChats = data.privateChats || []
        this._watchAll()
      } catch (e) { console.error('[PC] fetchPrivateChats', e) }
    },

    _watchAll () {
      this.watched = new Set(this.privateChats.map(c => `pc${c.id}`))
      wsService.connect(this.watched)
    },

    /* ---------------------- CRUD chats -------------------------- */
    async createPrivateChat (uid1, uid2) {
      await api.post('/privateChat', { user_id_1: uid1, user_id_2: uid2 })
      await this.fetchPrivateChats(uid1)
    },
    async deletePrivateChat (uid, chatId) {
      await api.delete(`/privateChat/${uid}/${chatId}`)
      this.privateChats = this.privateChats.filter(c => c.id !== chatId)
      this._watchAll()
    },

    /* ---------------------- sélectionner & historique ----------- */
    async selectChat (chat) {
      this.selectedChat = chat
      await this.loadMessages(chat.id)
    },
    async loadMessages (chatId) {
      try {
        const { data } = await axios.get(`http://localhost:8083/privateChat/pc${chatId}/messages`)
        const arr = Array.isArray(data.messages) ? data.messages : []
        this.messages = { ...this.messages, [`pc${chatId}`]: arr }
      } catch (e) { console.error('[PC] loadMessages', e) }
    },
  },
})

/* ----------- pousser les messages entrants via wsService ----------- */
wsService.on('message', (msg) => {
  if (!msg.metadata?.chatId?.startsWith('pc')) return
  const store = usePrivateChatStore()
  if (!store.watched.has(msg.metadata.chatId)) return

  const list = store.messages[msg.metadata.chatId] ||= []
  list.push(msg)
})
