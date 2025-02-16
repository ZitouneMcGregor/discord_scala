<template>
  <div class="home-container">
    <!-- Sidebar avec les serveurs de l'utilisateur -->
    <div class="sidebar">
      <h2>Mes Serveurs</h2>
      <div v-for="server in serverStore.userServers" :key="server.id" class="server">
        <span>{{ server.name }}</span>
        <button @click="toggleServer(server.id)">Quitter</button>
      </div>
    </div>

    <!-- Section principale : liste de tous les serveurs -->
    <div class="main-content">
      <!-- HEADER avec bouton Déconnexion -->
      <div class="header">
        <h2>Serveurs Disponibles</h2>
        <button @click="logout" class="logout-btn">Déconnexion</button>
      </div>

      <div class="server-list">
        <div v-for="server in serverStore.servers" :key="server.id" class="server-card">
          <h3>{{ server.name }}</h3>
          <button @click="toggleServer(server.id)">
            {{ isUserInServer(server.id) ? "Quitter" : "Rejoindre" }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted } from 'vue';
import { useAuthStore } from '../store/auth';
import { useServerStore } from '../store/servers';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const serverStore = useServerStore();
const router = useRouter();

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

const logout = () => {
  authStore.logout();
  router.push('/login'); // Redirige vers la page de connexion
};
</script>

<style scoped>
.home-container {
  display: flex;
  height: 100vh;
  background: #2c2f33;
  color: white;
}

/* Sidebar */
.sidebar {
  width: 250px;
  background: #23272a;
  padding: 20px;
}

.server {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: #40444b;
  padding: 10px;
  margin-top: 10px;
  border-radius: 5px;
}

button {
  background: #5865f2;
  border: none;
  padding: 5px 10px;
  color: white;
  cursor: pointer;
  border-radius: 3px;
}

button:hover {
  background: #4752c4;
}

/* Contenu principal */
.main-content {
  flex: 1;
  padding: 20px;
}

/* Header */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 20px;
  background: #23272a;
  margin-bottom: 20px;
}

.logout-btn {
  background: red;
  border: none;
  padding: 5px 10px;
  color: white;
  cursor: pointer;
  border-radius: 3px;
}

.logout-btn:hover {
  background: darkred;
}

/* Liste des serveurs */
.server-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 15px;
}

.server-card {
  background: #40444b;
  padding: 15px;
  border-radius: 5px;
  text-align: center;
}
</style>
