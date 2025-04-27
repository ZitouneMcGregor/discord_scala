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
import { useRouter }          from 'vue-router'
import { useAuthStore }       from '../store/auth'
import { useServerStore }     from '../store/servers'
import { useRoomStore }       from '../store/room'
import { usePrivateChatStore } from '../store/privateChats'

const router      = useRouter()
const authStore   = useAuthStore()
const serverStore = useServerStore()
const roomStore   = useRoomStore()
const pcStore     = usePrivateChatStore()

/* ---------------- WebSocket ---------------- */
const WS_URL = 'ws://localhost:8082/subscriptions'
let ws: WebSocket | null = null

function connectWs () {
  if (ws && ws.readyState === WebSocket.OPEN) return
  ws = new WebSocket(WS_URL)

  ws.addEventListener('open', sendSubscriptions)
  ws.addEventListener('close', () => setTimeout(connectWs, 3000))
  ws.addEventListener('error', e => console.error('[WS] error', e))
}

async function sendSubscriptions () {
  if (!ws || ws.readyState !== WebSocket.OPEN) return
  const me = authStore.user
  if (!me) return

  const serverIds = serverStore.userServers.map(s => s.id)

  const allRooms = (
   await Promise.all(serverIds.map(id => roomStore.fetchRooms(id, true)))
  ).flat()
  const rIds = allRooms
    .filter(r => r && r.id != null)
    .map(r => `r${r.id}`)

  await pcStore.fetchPrivateChats(me.id)
  const pcIds = pcStore.privateChats.map(c => `pc${c.id}`)

  const channels = [...rIds, ...pcIds].join(',')
  ws.send(channels)
  console.log('[Sidebar] Sent subscriptions →', channels)
}

onMounted(async () => {
  if (!authStore.user) return router.push('/login')

  await serverStore.fetchUserServers(authStore.user.id)
  connectWs()
})

watch(
  () => serverStore.userServers.map((s) => s.id),
  () => sendSubscriptions()
)

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