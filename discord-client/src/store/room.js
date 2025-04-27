import { defineStore } from 'pinia';
import api from '../plugins/axios';
import axios from 'axios';
import { wsService } from '../store/wsService'
import { useAuthStore } from '../store/auth' 


export const useRoomStore = defineStore('rooms', {
  state: () => ({
    rooms: [],
    serverId: null,
    selectedRoom: null,
    watched: new Set(),
    messages: {}, 
  }),

  getters: {
    currentMessages(state) {
      return state.selectedRoom ? (state.messages[`r${state.selectedRoom.id}`] || []) : []
    },
  },

  actions: {
    async setServer(serverId) {
      this.serverId = serverId
      await this.fetchRooms(serverId)
      this._watchAll()
    },

    /** Récupère les salons d’un serveur */
    async fetchRooms (serverId, dryRun = false) {
      try {
        const { data } = await api.get(`/servers/${serverId}/rooms`)
        const list = Array.isArray(data.rooms) ? data.rooms : []
    
       if (dryRun) return list       
    
        this.rooms = list 
        return list 
      } catch (err) {
        console.error('[rooms] fetchRooms error', err)
        if (dryRun) return []
        this.rooms = []
        return []
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
    async updateRoom(roomId, newName, serverId) {
      try {
        const roomPayload = {
          id: roomId,        // un entier, pas d’objet
          name: newName,
          serverId,          // si vous devez aussi l’envoyer
        };
        const response = await api.put(`http://localhost:8080/rooms/${roomId}`, roomPayload);
        if (response.status === 200) {
          console.log('Room mise à jour avec succès');
          await this.fetchRooms(serverId);
        }
      } catch (error) {
        console.error('Erreur lors de la mise à jour de la room', error.response?.data || error);
      }
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
          auth: { username: 'foo', password: 'bar' },
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

    _watchAll() {
      const authStore = useAuthStore()
      const userId = authStore?.user?.id
      this.watched = new Set(this.rooms.map(r => `r${r.id}`))
      if (userId) this.watched.add(`dm${userId}`)
      wsService.connect(this.watched)
    },
  },
})

wsService.on('message', (msg) => {
  const store = useRoomStore()
  if (!store.watched.has(msg.metadata.roomId)) return
  if (!store.messages[msg.metadata.roomId]) {
    store.messages[msg.metadata.roomId] = []
  }
  store.messages[msg.metadata.roomId].push(msg)
})
