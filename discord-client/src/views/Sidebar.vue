<template>
  <div class="sidebar">
    <!-- Home icon -->
    <div class="server-icon home-icon" @click="goHome">
      <i class="fa fa-home"></i>
    </div>

    <!-- User servers -->
    <div
      v-for="server in serverStore.userServers"
      :key="server.id"
      class="server-icon"
      @click="goToServer(server.id)"
      :title="server.name"
    >
      <!-- Show logo if available -->
      <img
        v-if="server.image"
        :src="server.image"
        alt="server-logo"
        class="server-image"
      />
      <!-- Fallback to 2-letter acronym -->
      <span v-else>{{ server.name.slice(0, 2).toUpperCase() }}</span>
    </div>

    <!-- DM icon -->
    <div class="server-icon dm-icon" @click="goToDM">
      DM
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'

/* ---------------------------------------------------------------- WS */
const WS_URL = 'ws://localhost:8082/subscriptions'
let ws: WebSocket | null = null

// Ouvre (ou ré-ouvre) la WebSocket
function connectWs() {
  if (ws && ws.readyState === WebSocket.OPEN) return

  ws = new WebSocket(WS_URL)

  ws.addEventListener('open', async () => {
    console.log('[Sidebar] WS connected')
    await sendSubscriptions() // on envoie la liste des salons + DM
  })

  ws.addEventListener('close', (e) => {
    console.warn('[Sidebar] WS closed', e.reason)
    // tentative de reconnexion après 3 s
    setTimeout(connectWs, 3000)
  })

  ws.addEventListener('error', (e) => console.error('[Sidebar] WS error', e))
}

// Construit puis envoie la "watch-list" au serveur
async function sendSubscriptions() {
  if (!ws || ws.readyState !== WebSocket.OPEN) return
  if (!authStore.user) return

  // ① Servers
  const serverIds = serverStore.userServers.map((s) => `s${s.id}`) // préfixe facultatif
  // ② DM perso (ex : dm42)
  const dmChannel = `dm${authStore.user.id}`

  const channels = [...serverIds, dmChannel].join(',')
  ws.send(channels)
  console.log('[Sidebar] Sent subscriptions →', channels)
}

/* ---------------------------------------------------------------- Stores & router */
const router = useRouter()
const authStore = useAuthStore()
const serverStore = useServerStore()

onMounted(async () => {
  if (!authStore.user) {
    router.push('/login')
    return
  }

  // 1) charge les serveurs puis 2) ouvre la socket
  await serverStore.fetchUserServers(authStore.user.id)
  connectWs()
})

// Si la liste des serveurs change (ex : lʼutilisateur rejoint un nouveau serveur)
watch(
  () => serverStore.userServers.map((s) => s.id),
  () => sendSubscriptions()
)

/* ---------------------------------------------------------------- Navigation helpers */
function goHome() {
  router.push('/home')
}

function goToServer(serverId: number) {
  router.push(`/server/${serverId}`)
}

function goToDM() {
  router.push(`/dm/${authStore.user.id}`)
}
</script>

<style scoped>
.sidebar {
  background-color: #202225;
  width: 72px;
  min-width: 72px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 10px;
  height: 100vh;
}

.server-icon {
  width: 48px;
  height: 48px;
  background-color: #2f3136;
  border-radius: 50%;
  margin-bottom: 10px;
  cursor: pointer;
  overflow: hidden;

  display: flex;
  justify-content: center;
  align-items: center;

  color: #fff;
  font-weight: bold;
  font-size: 14px;

  transition: background-color 0.2s ease;
}
.server-icon:hover {
  background-color: #3e4042;
}

.server-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* Accents */
.home-icon {
  color: #f3c623;
}
.dm-icon {
  color: #00b0f4;
  font-size: 0.9rem;
  line-height: 1;
}
</style>