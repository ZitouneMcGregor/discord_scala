<template>
  <div class="home-container">
    <!-- En-tête -->
    <header class="home-header">
      <div class="header-left">
        <h1 class="welcome">Bienvenue, {{ authStore.user?.username }}</h1>
      </div>
      <div class="header-right">
        <button class="btn-primary" @click="goToProfile">
          Mon profil
        </button>
        <button class="btn-danger" @click="handleLogout">
          Déconnexion
        </button>
      </div>
    </header>

    <!-- Contenu principal -->
    <main class="home-content">
      <!-- Carte "Créer un serveur" -->
      <div class="card create-server-card">
        <h2>Créer un serveur</h2>
        <div class="create-server-section">
          <input
            v-model="newServerName"
            placeholder="Nom du serveur"
            class="create-server-input"
          />
          <input
            v-model="newServerImage"
            placeholder="URL de l'image"
            class="create-server-input"
          />
          <button class="btn-primary" @click="createServer">
            Créer
          </button>
        </div>
      </div>

      <!-- Carte "Serveurs disponibles" -->
      <div class="card servers-list-card">
        <h2>Serveurs disponibles</h2>
        <p class="list-subtitle">(pas encore rejoints)</p>
        <ul class="servers-list">
          <li
            v-for="srv in serverStore.unjoinedServers"
            :key="srv.id"
            class="servers-list-item"
          >
            <div class="server-info">
              <strong>{{ srv.name }}</strong>
            </div>
            <button
              class="btn-primary"
              @click.stop="addUserOnServer(srv.id)"
            >
              Rejoindre
            </button>
          </li>
        </ul>
      </div>
    </main>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'

export default {
  setup() {
    const router = useRouter()
    const authStore = useAuthStore()
    const serverStore = useServerStore()

    const newServerName = ref('')
    const newServerImage = ref('')

    onMounted(async () => {
      if (!authStore.user) {
        router.push('/login')
        return
      }
      await serverStore.fetchUnjoinedServers(authStore.user.id)
    })

    function goToProfile() {
      router.push('/profile')
    }

    function handleLogout() {
      authStore.logout()
      router.push('/login')
    }

    async function createServer() {
      const name = newServerName.value.trim()
      const image = newServerImage.value.trim()
      if (!name) return

      const success = await serverStore.createServer(name, image, authStore.user.id)
      if (success) {
        await serverStore.fetchUnjoinedServers(authStore.user.id)
        newServerName.value = ''
        newServerImage.value = ''
      }
    }

    async function addUserOnServer(serverId) {
      await serverStore.addUserOnServer(authStore.user.id, serverId)
      await serverStore.fetchUnjoinedServers(authStore.user.id)
    }

    return {
      authStore,
      serverStore,
      newServerName,
      newServerImage,
      handleLogout,
      createServer,
      addUserOnServer,
      goToProfile
    }
  }
}
</script>

<style scoped>
/* Couleurs directes */
.home-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  background-color: #36393f; /* fond principal */
  color: #dcddde;           /* texte clair */
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #202225; /* barre plus sombre */
  padding: 20px;
}

.header-left .welcome {
  margin: 0;
  font-size: 20px;
}

.header-right button {
  margin-left: 10px;
}

/* Section principale */
.home-content {
  display: flex;
  flex-direction: row;
  gap: 20px;
  padding: 20px;
}

/* Carte (aspect) */
.card {
  background-color: #2f3136; /* bloc un peu plus clair */
  border-radius: 6px;
  padding: 20px;
  flex: 1;
  box-shadow: 0 2px 5px rgba(0, 0, 0, 0.3);
}

.create-server-card {
  max-width: 300px; /* limite la taille */
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.create-server-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.create-server-input {
  padding: 8px;
  border: none;
  border-radius: 4px;
  background-color: #303338; /* champ de saisie */
  color: #dcddde;
  outline: none;
  font-size: 14px;
}

/* Carte "Serveurs disponibles" */
.servers-list-card {
  flex: 2;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.list-subtitle {
  margin: 0 0 10px 0;
  font-size: 14px;
  color: #aaa; /* sous-titre plus clair */
}

.servers-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.servers-list-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #2f3136;
  margin-bottom: 8px;
  padding: 8px;
  border-radius: 4px;
  transition: background-color 0.2s;
}

.servers-list-item:hover {
  background-color: #393c43; /* hover plus clair */
}

.server-info {
  display: flex;
  flex-direction: column;
}

/* Boutons */
.btn-primary {
  background-color: #5865F2; /* “Discord purple” */
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 14px;
}
.btn-primary:hover {
  background-color: #4752c4;
}

.btn-danger {
  background-color: #f04747; /* rouge */
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 14px;
}
.btn-danger:hover {
  background-color: #ce3c3c;
}
</style>
