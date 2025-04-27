// src/services/wsService.js
import mitt from 'mitt'

class WsService {
  WS_URL = 'ws://localhost:8082/subscriptions'
  socket = null
  bus = mitt()
  watchIds = new Set()

  on  = this.bus.on
  off = this.bus.off

  connect() {
    // n’ouvre la socket qu’une seule fois au montage
    if (this.socket) return this._sendWatchList()
    this.socket = new WebSocket(this.WS_URL)
    this.socket.addEventListener('open', () => {
      this.bus.emit('open')
      this._sendWatchList()
    })
    this.socket.addEventListener('message', e => this._handleMessage(e))
    this.socket.addEventListener('close', () => {
      this.bus.emit('close')
      this.socket = null
      setTimeout(() => this.connect(), 3000)
    })
    this.socket.addEventListener('error', e => {
      this.bus.emit('error', e)
      console.error('[WS] error', e)
    })
  }

  addWatchIds(ids) {
    ids.forEach(id => this.watchIds.add(id))
    console.log('[WS] add watchIds:', Array.from(ids).join(','))
    this._sendWatchList()
  }

  removeWatchIds(ids) {
    ids.forEach(id => this.watchIds.delete(id))
    console.log('[WS] remove watchIds:', Array.from(ids).join(','))
    this._sendWatchList()
    console.log(wsService.getWatchIds())
  }

  getWatchIds() {
    return new Set(this.watchIds)
  }

  // --- privé ---
  _sendWatchList() {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send([...this.watchIds].join(','))
    }
  }
  _handleMessage(e) {
    if (typeof e.data !== 'string' || !e.data.trim().startsWith('{')) return
    try {
      const parsed = JSON.parse(e.data)
      parsed.ts = parsed.timestamp ?? Date.now()
      this.bus.emit('message', parsed)
    } catch (err) {
      console.error('[WS] parse error', err)
    }
  }
}

export const wsService = new WsService()
