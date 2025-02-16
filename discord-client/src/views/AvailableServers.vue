<template>
    <div class="server-list">
      <h2>Serveurs Disponibles</h2>
      <div v-for="server in serverStore.servers" :key="server.id" class="server-card">
        <h3>{{ server.name }}</h3>
        <button @click="toggleServer(server.id)">
          {{ isUserInServer(server.id) ? "Quitter" : "Rejoindre" }}
        </button>
      </div>
    </div>
  </template>
  
  <script setup>
  import { useServerStore } from '../store/servers';
  import { useAuthStore } from '../store/auth';
  import { onMounted } from 'vue';
  
  const serverStore = useServerStore();
  const authStore = useAuthStore();

  onMounted(() => {
  serverStore.fetchServers();
  if (authStore.user) {
    serverStore.fetchUserServers(authStore.user.id);
  }
});
  
  const isUserInServer = (serverId) => {
    return serverStore.userServers.some(server => server.id === serverId);
  };
  
  const toggleServer = (serverId) => {
    if (isUserInServer(serverId)) {
      serverStore.leaveServer(authStore.user.id, serverId);
    } else {
      serverStore.joinServer(authStore.user.id, serverId);
    }
  };
  </script>
  