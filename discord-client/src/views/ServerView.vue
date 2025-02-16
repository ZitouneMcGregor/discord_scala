<template>
  <div class="server-layout">
    <!-- Colonne de gauche -->
    <div class="left-panel">
      <h2>Rooms du serveur {{ serverId }}</h2>
      <ul>
        <li
          v-for="room in roomStore.rooms"
          :key="room.id"
          :class="{ selected: selectedRoom?.id === room.id }"
          @click="selectRoom(room)"
        >
          {{ room.name }}
        </li>
      </ul>

      <!-- Ajouter une room -->
      <div class="add-room">
        <input v-model="newRoomName" placeholder="Nom de la room" />
        <button class="btn-primary" @click="addRoom">Ajouter</button>
      </div>

      <!-- Quitter le serveur -->
      <button class="btn-danger" style="margin-top: 20px;" @click="leaveCurrentServer">
        Quitter le serveur
      </button>

      <!-- Formulaire pour mettre à jour le serveur -->
      <div class="update-server-section" style="margin-top: 20px;">
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

    <!-- Colonne de droite : afficher la room sélectionnée -->
    <div class="right-panel">
      <!-- Si aucune room n'est sélectionnée -->
      <div v-if="!selectedRoom" class="no-room-selected">
        <p>Sélectionnez une room pour la modifier ou la supprimer</p>
      </div>
      
      <!-- Formulaire d'édition / suppression de la room sélectionnée -->
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
  </div>
</template>

<script>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

// Stores
import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'
import { useRoomStore } from '../store/room';

export default {
  setup() {
    const route = useRoute();
    const router = useRouter();

    const authStore = useAuthStore();
    const serverStore = useServerStore();
    const roomStore = useRoomStore();

    // Récupère l'ID du serveur dans l'URL
    const serverId = route.params.serverId;

    // Room actuellement sélectionnée
    const selectedRoom = ref(null);

    // Champ pour ajouter une room
    const newRoomName = ref('');

    // Champs pour modifier la room sélectionnée
    const editRoomName = ref('');

    // Champs pour modifier le serveur
    const updatedServerName = ref('');
    const updatedServerImage = ref('');

    onMounted(async () => {
      // Charger la liste des rooms du serveur
      if (serverId) {
        await roomStore.fetchRooms(serverId);
      }
      // Optionnel : charger les infos du serveur pour pré-remplir updatedServerName / updatedServerImage
      // ex: let srv = await serverStore.fetchServerById(serverId);
      // updatedServerName.value = srv.name;
      // updatedServerImage.value = srv.image;
    });

    // Sélection d'une room : pas de navigation, on reste sur la même page
    function selectRoom(room) {
      selectedRoom.value = room;
      editRoomName.value = room.name; // on pré-remplit le champ
    }

    // Ajouter une room
    async function addRoom() {
      if (!newRoomName.value.trim()) return;
      await roomStore.addRoom(serverId, newRoomName.value.trim());
      newRoomName.value = '';
    }

    // Mettre à jour la room sélectionnée
    async function updateSelectedRoom() {
      if (!selectedRoom.value) return;
      await roomStore.updateRoom({
        serverId,
        roomId: selectedRoom.value.id,
        newName: editRoomName.value.trim()
      });
      // On met à jour localement
      selectedRoom.value.name = editRoomName.value.trim();
    }

    // Supprimer la room sélectionnée
    async function deleteSelectedRoom() {
      if (!selectedRoom.value) return;
      await roomStore.deleteRoom(serverId, selectedRoom.value.id);
      // On re-fetch la liste pour la retirer
      await roomStore.fetchRooms(serverId);
      // On désélectionne
      selectedRoom.value = null;
      editRoomName.value = '';
    }

    // Quitter le serveur
    async function leaveCurrentServer() {
      await serverStore.leaveServer(authStore.user.id, Number(serverId));
      // redirection par ex.
      router.push('/home');
    }

    // Mettre à jour le serveur (nom, image)
    async function updateServerInfo() {
      if (!updatedServerName.value.trim() && !updatedServerImage.value.trim()) {
        return;
      }
      await serverStore.updateServer({
        id: Number(serverId),
        name: updatedServerName.value.trim(),
        image: updatedServerImage.value.trim()
      });
      // Optionnel : rafraîchir, vider les champs, etc.
    }

    return {
      serverId,
      // Room
      selectedRoom,
      newRoomName,
      editRoomName,

      // Server
      updatedServerName,
      updatedServerImage,

      // Méthodes
      selectRoom,
      addRoom,
      updateSelectedRoom,
      deleteSelectedRoom,
      leaveCurrentServer,
      updateServerInfo,

      // Stores
      authStore,
      serverStore,
      roomStore
    };
  }
}
</script>

<style scoped>
.server-layout {
  display: flex;
  height: 100%;
}

/* Colonne de gauche */
.left-panel {
  width: 250px;
  background-color: #2f3136;
  padding: 10px;
  overflow-y: auto;
}

/* Liste des rooms */
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

/* Ajouter une room */
.add-room {
  display: flex;
  margin-top: 10px;
  gap: 5px;
}

/* Boutons */
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

/* Formulaire update server */
.update-server-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.right-panel {
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
</style>
