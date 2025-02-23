<template>
  <div class="home-container">
    <header class="home-header">
      <h1>Bienvenue, {{ authStore.user?.username }}</h1>
      <button class="btn-primary" @click="goToProfile">
          Mon profil
        </button>
      <button class="btn-primary" @click="handleLogout">
        Déconnexion
      </button>
    </header>

    <main class="home-content">
      <section class="create-server-section">
        <h2>Créer un serveur</h2>
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
      </section>

      <section class="servers-list-section">
        <h2>Serveurs disponibles (pas encore rejoints)</h2>
        <ul class="servers-list">
          <li
            v-for="srv in serverStore.unjoinedServers"
            :key="srv.id"
            class="servers-list-item"
          >
            <span>{{ srv.name }}</span>
            <button
              class="btn-primary"
              @click.stop="addUserOnServer(srv.id)"
            >
              Rejoindre
            </button>
          </li>
        </ul>
      </section>
    </main>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth';
import { useServerStore } from '../store/servers';

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
      router.push('/profile');
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
.home-container {
  display: flex;
  flex-direction: column;
  flex: 1;
  background-color: #36393f; 
  color: #dcddde;
  min-height: 100vh;
  padding: 20px;
}

.home-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.home-content {
  display: flex;
  flex-direction: column;
  gap: 40px;
}

.create-server-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 300px;
}

.create-server-input {
  padding: 8px;
  border: none;
  border-radius: 4px;
  background-color: #303338;
  color: #dcddde;
  outline: none;
}

.servers-list-section {
  display: flex;
  flex-direction: column;
  gap: 10px;
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
}

.servers-list-item:hover {
  background-color: #393c43;
}

.btn-primary {
  background-color: #5865F2;
  color: white;
  border: none;
  border-radius: 4px;
  padding: 8px 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}
.btn-primary:hover {
  background-color: #4752c4;
}
</style>
