import { defineStore } from 'pinia';
import api from '../plugins/axios';
import axios from 'axios';
import { wsService } from '../store/wsService'   // chemin vers ton wsService.js
import { useAuthStore } from '../store/auth'        // pour récupérer l’id de l’utilisateur


export const useRoomStore = defineStore('rooms', {
  state: () => ({
    rooms: [],               // Array<{ id:number, name:string, ... }>
    serverId: null,          // id du serveur courant
    selectedRoom: null,      // objet salon courant
    watched: new Set(),      // Set<string> des ids suivis ("r12", ...)
    messages: {},            // { [roomId:string]: Array<Msg> }
  }),

  getters: {
    // retourne les messages du salon sélectionné
    currentMessages(state) {
      return state.selectedRoom ? (state.messages[`r${state.selectedRoom.id}`] || []) : []
    },
  },

  actions: {
    /** Change de serveur (appelée depuis la route /server/:id) */
    async setServer(serverId) {
      this.serverId = serverId
      await this.fetchRooms(serverId)
      this._watchAll()               // maj watch-list → wsService
    },

    /** Récupère les salons d’un serveur */
    async fetchRooms(serverId) {
      try {
        const { data } = await api.get(`/servers/${serverId}/rooms`)
        this.rooms = Array.isArray(data.rooms) ? data.rooms : []
        if (!this.selectedRoom && this.rooms.length) {
          await this.selectRoom(this.rooms[0])
        }
      } catch (err) {
        console.error('[rooms] fetchRooms error', err)
        this.rooms = []
      }
    },

    async addRoom(serverId, name) {
      if (!name.trim()) return
      try {
        await api.post('/rooms', { name, serverId: Number(serverId) })
        await this.fetchRooms(serverId)
        this._watchAll()
      } catch (err) {
        console.error('[rooms] addRoom error', err)
      }
    },

    /** MAJ ou suppression d’un salon */
    async updateRoom(roomId, payload) {
      await api.put(`/rooms/${roomId}`, payload)
      await this.fetchRooms(this.serverId)
      this._watchAll()
    },
    async deleteRoom(roomId) {
      await api.delete(`/rooms/${roomId}`)
      await this.fetchRooms(this.serverId)
      this.selectedRoom = null
      this._watchAll()
    },

    /** Sélection d’un salon & chargement de l’historique */
    async selectRoom(room) {
      this.selectedRoom = room
      await this.loadMessages(room.id)
    },

    /** Récupère l’historique via HTTP */
    async loadMessages(roomId) {
      if (!this.serverId) return
      try {
        const idStr = `r${roomId}`
        const url = `http://localhost:8083/server/${this.serverId}/room/${idStr}/messages`
        const { data } = await axios.get(url, {
          auth: { username: 'foo', password: 'bar' },  // ton BasicAuth
        })
        
        this.messages[idStr] = Array.isArray(data.messages) ? data.messages.map(m => ({
          content: m.content,
          ts: m.timestamp,
          metadata: m.metadata,
        })) : []
      } catch (err) {
        console.error('[rooms] loadMessages error', err)
        this.messages[`r${roomId}`] = []
      }
    },

    /* ------------------------------------------------------------------ */
    /* private helpers                                                    */
    /* ------------------------------------------------------------------ */

    /** Met à jour la watch‑list et (re)connecte le WebSocket service */
    _watchAll() {
      const authStore = useAuthStore()
      const userId = authStore?.user?.id
      this.watched = new Set(this.rooms.map(r => `r${r.id}`))
      if (userId) this.watched.add(`dm${userId}`)
      wsService.connect(this.watched)
    },
  },
})

/* -------------------------------------------------------------------- */
/* WebSocket → on pousse les messages entrants dans le state            */
/* -------------------------------------------------------------------- */
wsService.on('message', (msg) => {
  const store = useRoomStore()
  // on ignore si ce n’est pas un salon qu’on suit
  if (!store.watched.has(msg.metadata.roomId)) return
  if (!store.messages[msg.metadata.roomId]) {
    store.messages[msg.metadata.roomId] = []
  }
  store.messages[msg.metadata.roomId].push(msg)
})
