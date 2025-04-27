// stores/privateChats.js
import { defineStore } from 'pinia'
import api   from '../plugins/axios'
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

    async fetchPrivateChats (userId) {
      try {
        const { data } = await api.get(`/privateChat/user/${userId}`)
        this.privateChats = data.privateChats || []
        this._watchAll()
      } catch (e) {
        console.error('[PC] fetchPrivateChats', e)
      }
    },

    _watchAll () {
      this.watched = new Set(this.watchIds)
      wsService.connect()
      wsService.addWatchIds(this.watchIds)
    },

    async createPrivateChat (uid1, uid2) {
      try {
        const { data } = await api.post('/privateChat', { user_id_1: uid1, user_id_2: uid2 })
        await this.fetchPrivateChats(uid1)

        if (data.privateChat?.id) {
          const channel = `pc${data.privateChat.id}`
          wsService.connect()
          wsService.addWatchIds([channel])
        }
      } catch (e) {
        console.error('[PC] createPrivateChat', e)
      }
    },

    async deletePrivateChat (uid, chatId) {
      try {
        await api.delete(`/privateChat/${uid}/${chatId}`)
        this.privateChats = this.privateChats.filter(c => c.id !== chatId)
        wsService.removeWatchIds([`pc${chatId}`])
        if (this.selectedChat?.id === chatId) this.selectedChat = null
      } catch (e) {
        console.error('[PC] deletePrivateChat', e)
      }
    },

    async selectChat (chat) {
      this.selectedChat = chat
      await this.loadMessages(chat.id)
    },

    async loadMessages (chatId) {
      try {
        const { data } = await axios.get(
          `http://localhost:8083/privateChat/pc${chatId}/messages`
        )
        const arr = Array.isArray(data.messages) ? data.messages : []
        const mapped = arr.map(m => ({
          content:  m.content,
          ts:       m.timestamp ?? Date.now(),
          metadata: m.metadata
        }))
        this.messages = {
          ...this.messages,
          [`pc${chatId}`]: mapped
        }
      } catch (e) {
        console.error('[PC] loadMessages', e)
        this.messages[`pc${chatId}`] = []
      }
    },
  },
})

wsService.on('message', (msg) => {
  if (!msg.metadata?.chatId?.startsWith('pc')) return
  const store = usePrivateChatStore()
  if (!store.watched.has(msg.metadata.chatId)) return

  const ts = msg.ts ?? Date.now()
  const list = store.messages[msg.metadata.chatId] ||= []
  list.push({
    content:  msg.content,
    ts,
    metadata: msg.metadata
  })
})
