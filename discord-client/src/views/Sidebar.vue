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
import { onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'
import { useRoomStore } from '../store/room'
import { usePrivateChatStore } from '../store/privateChats'
import { wsService } from '../store/wsService'

const router      = useRouter()
const route       = useRoute()
const authStore   = useAuthStore()
const serverStore = useServerStore()
const roomStore   = useRoomStore()
const pcStore     = usePrivateChatStore()

function goHome() {
  router.push('/home')
}
function goToServer(serverId) {
  router.push(`/server/${serverId}`)
  const roomIds = roomStore.rooms
    .filter(r => r.id != null)
    .map(r => `r${r.id}`)
  wsService.addWatchIds(roomIds)
}

function goToDM() {
  router.push(`/dm/${authStore.user!.id}`)
}

async function computeWatchIds() {
  // 1. all servers the user is in
  const serverIds = serverStore.userServers.map(s => s.id)
  // 2. fetch all rooms for each server (true = force reload)
  const allRooms = (
    await Promise.all(serverIds.map(id => roomStore.fetchRooms(id, true)))
  ).flat()
  const roomChannels = allRooms
    .filter(r => r && r.id != null)
    .map(r => `r${r.id}`)

  // 3. fetch private chats
  await pcStore.fetchPrivateChats(authStore.user!.id)
  const pcChannels = pcStore.privateChats.map(c => `pc${c.id}`)

  return new Set<string>([...roomChannels, ...pcChannels])
}

onMounted(async () => {
  if (!authStore.user) return router.push('/login')
  await serverStore.fetchUserServers(authStore.user.id)


  wsService.connect()
  const allIds = await computeWatchIds()
  wsService.addWatchIds(allIds)
})
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
