import mitt from 'mitt'

/**
 * WebSocket service – singleton (JavaScript version)
 *   - gère UNE seule connexion WebSocket pour toute l’application
 *   - envoie la liste des salons/DM à suivre sous forme « id1,id2,dm5 »
 *   - redistribue les messages via un bus mitt (pub/sub)
 */

class WsService {
  WS_URL = 'ws://localhost:8082/subscriptions'
  socket = null
  bus = mitt()

  // alias pratiques pour s’abonner / se désabonner
  on = this.bus.on
  off = this.bus.off

  /**
   * Ouvre ou ré-ouvre la WebSocket et (ré)envoie la watch-list.
   * @param {Set<string>} watchIds  ensemble d’IDs (ex. new Set(["r1","dm4"]))
   */
  connect (watchIds) {
    // socket déjà ouverte → on ne fait qu’actualiser la watch-list
    if (this.socket && this.socket.readyState === WebSocket.OPEN) {
      this.#sendWatchList(watchIds)
      return
    }

    // si elle est en cours d’ouverture on ne fait rien
    if (this.socket && this.socket.readyState === WebSocket.CONNECTING) return

    // sinon on ferme l’ancienne et on en crée une nouvelle
    this.socket?.close()
    this.socket = new WebSocket(this.WS_URL)

    this.socket.addEventListener('open', () => {
      this.bus.emit('open')
      this.#sendWatchList(watchIds)
    })

    this.socket.addEventListener('message', (e) => this.#handleMessage(e))

    this.socket.addEventListener('close', () => {
      this.bus.emit('close')
      // tentative de reconnexion simple après 3 s
      setTimeout(() => this.connect(watchIds), 3000)
    })

    this.socket.addEventListener('error', (e) => {
      this.bus.emit('error', e)
      console.error('[WS] error', e)
    })
  }

  /**
   * Envoie brut (publication) – utile si tu postes via WS.
   * @param {string} raw
   */
  send (raw) {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send(raw)
    }
  }

  // --------------------------- privés --------------------------- //

  /**
   * Concatène et envoie la watch-list (« id1,id2,… »)
   * @param {Set<string>} ids
   */
  #sendWatchList (ids) {
    if (this.socket?.readyState === WebSocket.OPEN) {
      this.socket.send([...ids].join(','))
    }
  }

  /**
   * Gère les messages entrants JSON → redispatch via mitt
   * @param {MessageEvent} e
   */
  #handleMessage (e) {
    if (typeof e.data !== 'string' || !e.data.trim().startsWith('{')) return
    try {
      const parsed = JSON.parse(e.data)
      this.bus.emit('message', parsed)
    } catch (err) {
      console.error('[WS] parse error', err)
    }
  }
}

export const wsService = new WsService()