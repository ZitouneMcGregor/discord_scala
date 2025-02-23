<template>
  <div class="server-layout">
    <!-- Colonne de gauche : Rooms et options serveur -->
    <div class="left-panel">
      <h2>Rooms</h2>
      <ul>
        <li
          v-for="room in roomStore.rooms"
          :key="room.id"
          :class="{ selected: selectedRoom && selectedRoom.id === room.id }"
          @click="selectRoom(room)"
        >
          {{ room.name }}
        </li>
      </ul>
      <div class="add-room">
        <input v-model="newRoomName" placeholder="Nom de la room" />
        <button class="btn-primary" @click="addRoom">Ajouter</button>
      </div>
      <button class="btn-danger" @click="leaveCurrentServer">
        Quitter le serveur
      </button>
      <div class="update-server-section">
        <h3>Modifier le serveur</h3>
        <input
          v-model="updatedServerName"
          placeholder="Nouveau nom du serveur"
        />
        <input
          v-model="updatedServerImage"
          placeholder="Nouvelle URL d'image"
        />
        <button class="btn-primary" @click="updateServerInfo">
          Mettre à jour
        </button>
      </div>
    </div>

    <!-- Colonne centrale : Détails de la room sélectionnée -->
    <div class="middle-panel">
      <div v-if="!selectedRoom" class="no-room-selected">
        <p>Sélectionnez une room pour la modifier ou la supprimer</p>
      </div>
      <div v-else class="room-edit-panel">
        <h3>Modifier la room : {{ selectedRoom.name }}</h3>
        <input
          v-model="editRoomName"
          placeholder="Nouveau nom de la room"
        />
        <div class="buttons">
          <button class="btn-primary" @click="updateSelectedRoom">
            Enregistrer
          </button>
          <button class="btn-danger" @click="deleteSelectedRoom">
            Supprimer
          </button>
        </div>
      </div>
    </div>

    <!-- Colonne de droite : Liste des utilisateurs du serveur -->
    <div class="right-panel">
      <h3>Utilisateurs</h3>
      <ul>
        <li
          v-for="user in serverStore.serverUsers"
          :key="user.user.id"
          :class="{ admin: user.admin }"
        >
          {{ user.user.username }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'
import { useRoomStore } from '../store/room'

export default {
  setup() {
    const route = useRoute()
    const router = useRouter()
    const authStore = useAuthStore()
    const serverStore = useServerStore()
    const roomStore = useRoomStore()

    // Récupère l'ID du serveur depuis l'URL
    const serverId = route.params.serverId

    const selectedRoom = ref(null)
    const newRoomName = ref('')
    const editRoomName = ref('')

    const updatedServerName = ref('')
    const updatedServerImage = ref('')

    onMounted(async () => {
      if (serverId) {
        await roomStore.fetchRooms(serverId)
        await serverStore.fetchServerUsers(serverId)
      }
    })

    // Sélectionne une room (sans changer de route)
    function selectRoom(room) {
      selectedRoom.value = room
      editRoomName.value = room.name
    }

    // Ajouter une room
    async function addRoom() {
      if (!newRoomName.value.trim()) return
      await roomStore.addRoom(serverId, newRoomName.value.trim())
      newRoomName.value = ''
    }

    // Mettre à jour la room sélectionnée
    async function updateSelectedRoom() {
      if (!selectedRoom.value) return
      await roomStore.updateRoom({
        serverId,
        roomId: selectedRoom.value.id,
        newName: editRoomName.value.trim()
      })
      selectedRoom.value.name = editRoomName.value.trim()
    }

    // Supprimer la room sélectionnée
    async function deleteSelectedRoom() {
      if (!selectedRoom.value) return
      await roomStore.deleteRoom(serverId, selectedRoom.value.id)
      await roomStore.fetchRooms(serverId)
      selectedRoom.value = null
      editRoomName.value = ''
    }

    // Quitter le serveur
    async function leaveCurrentServer() {
      await serverStore.leaveServer(authStore.user.id, Number(serverId))
      router.push('/home')
    }

    // Mettre à jour les infos du serveur
    async function updateServerInfo() {
      if (!updatedServerName.value.trim() && !updatedServerImage.value.trim()) return
      await serverStore.updateServer({
        id: Number(serverId),
        name: updatedServerName.value.trim(),
        image: updatedServerImage.value.trim()
      })
    }

    return {
      serverId,
      selectedRoom,
      newRoomName,
      editRoomName,
      updatedServerName,
      updatedServerImage,
      selectRoom,
      addRoom,
      updateSelectedRoom,
      deleteSelectedRoom,
      leaveCurrentServer,
      updateServerInfo,
      authStore,
      serverStore,
      roomStore
    }
  }
}
</script>

<style scoped>
/* Layout principal en 3 colonnes */
.server-layout {
  display: flex;
  height: 100vh;
  font-family: Arial, sans-serif;
}

/* Colonne de gauche : Rooms et options serveur */
.left-panel {
  width: 250px;
  background-color: #2f3136;
  padding: 10px;
  overflow-y: auto;
}

.left-panel h2 {
  color: #fff;
  margin-bottom: 10px;
}

.left-panel ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.left-panel li {
  padding: 6px 10px;
  margin-bottom: 5px;
  cursor: pointer;
  border-radius: 4px;
  color: #dcddde;
  transition: background-color 0.2s;
}

.left-panel li:hover {
  background-color: #393c43;
}

.left-panel li.selected {
  background-color: #5865F2;
}

.add-room {
  margin-top: 10px;
  display: flex;
  gap: 5px;
}

.update-server-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* Colonne centrale : Détails de la room */
.middle-panel {
  flex: 1;
  background-color: #36393f;
  padding: 20px;
  color: #dcddde;
}

.no-room-selected {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100%;
  color: #aaa;
}

.room-edit-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.room-edit-panel .buttons {
  display: flex;
  gap: 8px;
}

/* Colonne de droite : Liste des utilisateurs */
.right-panel {
  width: 200px;
  background-color: #2f3136;
  padding: 10px;
  overflow-y: auto;
}

.right-panel h3 {
  color: #fff;
  margin-bottom: 10px;
}

.right-panel ul {
  list-style: none;
  margin: 0;
  padding: 0;
}

.right-panel li {
  margin-bottom: 6px;
  color: #fff;
}

.right-panel li.admin {
  color: yellow;
  font-weight: bold;
}

/* Boutons généraux */
.btn-primary {
  background-color: #5865F2;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-primary:hover {
  background-color: #4752c4;
}

.btn-danger {
  background-color: #f04747;
  color: #fff;
  border: none;
  border-radius: 4px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-danger:hover {
  background-color: #ce3c3c;
}
</style>
