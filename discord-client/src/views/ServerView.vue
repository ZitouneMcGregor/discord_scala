<template>
  <div class="server-layout">
    <!-- Colonne de gauche -->
    <div class="left-panel">
      <h1>Serveur</h1>

      <button class="btn-primary" @click="openInviteModal">Inviter des gens</button>

      <!-- Boutons quitter/supprimer le serveur -->
      <button class="btn-danger" style="margin-top: 20px;" @click="leaveCurrentServer">
        Quitter le serveur
      </button>
      <button class="btn-danger" style="margin-top: 20px;" @click="deleteServer">
        Supprimer le serveur
      </button>
      
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

      <!-- Formulaire de mise à jour du serveur -->
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

    <!-- Modale d'invitation -->
    <div v-if="showInviteModal">
      <!-- Overlay -->
      <div class="modal-overlay" @click="closeInviteModal"></div>
      <!-- Contenu de la modale -->
      <div class="invite-modal">
        <h3>Inviter des gens</h3>
        <input 
          v-model="inviteInput" 
          placeholder="Entrez l'username de la personne à inviter"
        />
        <!-- Liste dynamique des utilisateurs filtrés -->
        <ul v-if="filteredUsers.length">
          <li 
            v-for="user in filteredUsers" 
            :key="user.id"
            @click="selectUser(user)"
          >
            {{ user.username }}
          </li>
        </ul>
        <button class="btn-primary modal-btn" @click="addUserOnServer">Inviter</button>
      </div>
    </div>

    <!-- Colonne de droite : afficher la room sélectionnée -->
    <div class="right-panel">
      <div v-if="!selectedRoom" class="no-room-selected">
        <p>Sélectionnez une room pour la modifier ou la supprimer</p>
      </div>
      <div v-else>
        <!-- Overlay pour fermer la modale d'édition -->
        <div class="modal-overlay" @click="selectedRoom = null"></div>
        <div class="room-edit-panel">
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
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'

import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'
import { useRoomStore } from '../store/room'
import { useUserStore } from '../store/user'

export default {
  setup() {
    const route = useRoute();
    const router = useRouter();

    const authStore = useAuthStore();
    const serverStore = useServerStore();
    const roomStore = useRoomStore();
    const userStore = useUserStore();

    const serverId = route.params.serverId;
    const selectedRoom = ref(null);
    const newRoomName = ref('');
    const editRoomName = ref('');
    const updatedServerName = ref('');
    const updatedServerImage = ref('');

    const showInviteModal = ref(false);
    const inviteInput = ref('');
    const selectedUser = ref(null);

    const filteredUsers = computed(() => {
      if (!inviteInput.value) return [];
      return userStore.inviteUsers.filter(user =>
        user.username.toLowerCase().includes(inviteInput.value.toLowerCase())
      );
    });

    function openInviteModal() {
      showInviteModal.value = true;
      userStore.fetchInviteUsers(Number(serverId));
    }

    function closeInviteModal() {
      showInviteModal.value = false;
      inviteInput.value = '';
      selectedUser.value = null;
    }

    function selectUser(user) {
      selectedUser.value = user;
      inviteInput.value = user.username;
    }

    async function addUserOnServer() {
      if (!selectedUser.value) {
        alert("Veuillez sélectionner un utilisateur dans la liste.");
        return;
      }
      try {
        await serverStore.addUserOnServer(selectedUser.value.id, Number(serverId));
        alert("Utilisateur ajouté !");
        closeInviteModal();
      } catch (error) {
        console.error("Erreur lors de l'ajout de l'utilisateur :", error);
      }
    }

    onMounted(async () => {
      if (serverId) {
        await roomStore.fetchRooms(serverId);
      }
    });

    function selectRoom(room) {
      selectedRoom.value = room;
      editRoomName.value = room.name;
    }

    async function addRoom() {
      if (!newRoomName.value.trim()) return;
      await roomStore.addRoom(serverId, newRoomName.value.trim());
      newRoomName.value = '';
    }

    async function updateSelectedRoom() {
      if (!selectedRoom.value) return;
      await roomStore.updateRoom({
        serverId,
        roomId: selectedRoom.value.id,
        newName: editRoomName.value.trim()
      });
      selectedRoom.value.name = editRoomName.value.trim();
    }

    async function deleteSelectedRoom() {
      if (!selectedRoom.value) return;
      await roomStore.deleteRoom(serverId, selectedRoom.value.id);
      await roomStore.fetchRooms(serverId);
      selectedRoom.value = null;
      editRoomName.value = '';
    }

    async function leaveCurrentServer() {
      await serverStore.leaveServer(authStore.user.id, Number(serverId));
      router.push('/home');
    }

    async function updateServerInfo() {
      if (!updatedServerName.value.trim() && !updatedServerImage.value.trim()) {
        return;
      }
      await serverStore.updateServer({
        id: Number(serverId),
        name: updatedServerName.value.trim(),
        image: updatedServerImage.value.trim()
      });
    }

    async function deleteServer() {
      await serverStore.deleteServer(Number(serverId));
      router.push('/home');
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
      deleteServer,
      authStore,
      serverStore,
      roomStore,
      showInviteModal,
      inviteInput,
      filteredUsers,
      openInviteModal,
      closeInviteModal,
      selectUser,
      addUserOnServer,
      userStore
    };
  }
};
</script>

<style scoped>
.server-layout {
  display: flex;
  height: 100%;
}

/* Style pour la colonne de gauche */
.left-panel {
  width: 250px;
  background-color: #2f3136;
  padding: 15px;
  overflow-y: auto;
}

.left-panel h1,
.left-panel h2,
.left-panel h3 {
  color: #fff;
  margin-bottom: 15px;
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
  display: flex;
  margin-top: 15px;
  gap: 5px;
}

/* Boutons généraux */
.btn-primary, .btn-danger {
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 14px;
}

.btn-primary {
  background-color: #5865F2;
  color: #fff;
}

.btn-primary:hover {
  background-color: #4752c4;
}

.btn-danger {
  background-color: #f04747;
  color: #fff;
}

.btn-danger:hover {
  background-color: #ce3c3c;
}

/* Section de mise à jour du serveur */
.update-server-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Style pour la colonne de droite */
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

/* Overlay des modales */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 900;
}

/* Style amélioré pour la modale d'invitation */
.invite-modal {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: #36393f;
  padding: 25px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  width: 320px;
}

.invite-modal h3 {
  color: #fff;
  margin-bottom: 15px;
  text-align: center;
}

.invite-modal input {
  width: 100%;
  padding: 10px;
  margin-bottom: 15px;
  border: 1px solid #555;
  border-radius: 4px;
  background-color: #2f3136;
  color: #dcddde;
  font-size: 14px;
}

.invite-modal ul {
  list-style: none;
  padding: 0;
  margin-bottom: 15px;
  max-height: 150px;
  overflow-y: auto;
  border: 1px solid #555;
  border-radius: 4px;
}

.invite-modal li {
  padding: 8px;
  cursor: pointer;
  transition: background-color 0.2s;
  border-bottom: 1px solid #555;
}

.invite-modal li:last-child {
  border-bottom: none;
}

.invite-modal li:hover {
  background-color: #4752c4;
}

.modal-btn {
  width: 100%;
  margin-top: 5px;
}

/* Style amélioré pour la modale d'édition de room */
.room-edit-panel {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background-color: #36393f;
  padding: 25px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  z-index: 1000;
  width: 320px;
}

.room-edit-panel h3 {
  color: #fff;
  margin-bottom: 15px;
  text-align: center;
}

.room-edit-panel input {
  width: 100%;
  padding: 10px;
  margin-bottom: 15px;
  border: 1px solid #555;
  border-radius: 4px;
  background-color: #2f3136;
  color: #dcddde;
  font-size: 14px;
}

.room-edit-panel .buttons {
  display: flex;
  justify-content: space-between;
  gap: 10px;
}
</style>
