<template>
  <div class="sidebar">
    <div class="server-icon home-icon" @click="goHome">
      <i class="fa fa-home">H</i>
    </div>

    <div 
      class="server-icon" 
      v-for="server in serverStore.userServers" 
      :key="server.id"
      @click="goToServer(server.id)"
    >
      <img 
        v-if="server.image" 
        :src="server.image" 
        alt="server-logo" 
        class="server-image"
      />
      <span v-else>
        {{ server.name.substring(0, 2).toUpperCase() }}
      </span>
    </div>

    <div class="server-icon dm-icon" @click="goToDM">
      DM
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'

const router = useRouter()
const authStore = useAuthStore()
const serverStore = useServerStore()

onMounted(async () => {
  if (!authStore.user) {
    router.push('/login')
    return
  }
  await serverStore.fetchUserServers(authStore.user.id)
})

function goHome() {
  router.push('/home')
}

function goToServer(serverId) {
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

  transition: background-color 0.2s;
}
.server-icon:hover {
  background-color: #3e4042;
}

.server-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.home-icon, .dm-icon {
  color: yellow;
}
</style>
