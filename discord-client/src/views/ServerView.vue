<template>
  <div class="server-layout">
    <!-- Colonne de gauche -->
    <div class="left-panel">
      <h1>Serveur</h1>
      <button class="btn-primary" @click="openInviteModal">Inviter des gens</button>
      <button class="btn-primary" @click="openGererModal">Gerer les users</button>
      <h1 v-if="showGererModal">TEST</h1>
      <!-- Boutons quitter/supprimer le serveur -->
      <button class="btn-danger" style="margin-top: 20px;" @click="leaveCurrentServer">
        Quitter le serveur
      </button>
      <button
  v-if="serverStore.serverUsers.users?.some(u => u.id === authStore.user.id && u.admin)"
  class="btn-danger"
  style="margin-top: 20px;"
  @click="deleteServer"
>
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
      <div class="add-room">
        <input v-model="newRoomName" placeholder="Nom de la room" />
        <button class="btn-primary" @click="addRoom">Ajouter</button>
      </div>

      <!-- Bouton d'ouverture du modal -->
      <button class="btn-primary" @click="openEditServerModal" style="margin-top: 20px;">
        Modifier le serveur
      </button>

      <!-- Modale d'édition du serveur -->
      <div v-if="showEditServerModal">
        <div class="modal-overlay" @click="closeEditServerModal"></div>
        <div class="editserv-modal">
          <h3>Modifier le serveur</h3>
          <input v-model="updatedServerName" placeholder="Nouveau nom du serveur" />
          <input v-model="updatedServerImage" placeholder="Nouvelle URL d'image" />
          <button class="btn-primary modal-btn" @click="updateServerInfo">Mettre à jour</button>
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

      <!-- Modale de gestion -->
      <!-- Modale de gestion -->
      <div v-if="showGererModal">
        <div class="modal-overlay" @click="closeGererModal"></div>
        <div class="invite-modal">
          <button @click="closeGererModal">X</button>
          <h3>Gérer les utilisateurs</h3>

          <ul v-if="serverStore.serverUsers.users?.length">
            <li
              v-for="user in serverStore.serverUsers.users"
              :key="user.id"
              style="display: flex; justify-content: space-between; align-items: center; gap: 5px;"
            >
            <span>{{ serverStore.userMap[user.user_id] || 'Utilisateur inconnu' }}</span>
            <div style="display: flex; gap: 5px;">
              <button
  class="btn-primary"
  v-if="!user.admin"
  @click="serverStore.toggleAdmin(serverId, user.user_id, true)"
>
  Promouvoir
</button>
<button
  class="btn-danger"
  v-if="user.admin"
  @click="serverStore.toggleAdmin(serverId, user.user_id, false)"
>
  Retirer admin
</button>
<button
  class="btn-danger"
  @click="serverStore.kickUser(serverId, user.user_id)"
>
  Kick
</button>

              </div>
            </li>
          </ul>
        </div>
      </div>



    </div>

    <!-- Colonne du milieu -->
    <div class="middle-panel">
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

    <!-- Colonne de droite -->
    <div class="right-panel">
      <h3>Utilisateurs</h3>
      <ul>
        <li
          v-for="user in serverStore.serverUsers"
          :key="user.id"
          :class="{ admin: user.admin }"
        >
          {{ user.username }}
        </li>
      </ul>
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
    const route = useRoute()
    const router = useRouter()
    const authStore = useAuthStore()
    const serverStore = useServerStore()
    const roomStore = useRoomStore()
    const userStore = useUserStore()

    const serverId = route.params.serverId
    const selectedRoom = ref(null)
    const newRoomName = ref('')
    const editRoomName = ref('')
    const updatedServerName = ref('')
    const updatedServerImage = ref('')

    const roomToEdit = ref(null)
    const showRoomModal = ref(false)

    // Modale serveur
    const showEditServerModal = ref(false)
    function openEditServerModal() {
      updatedServerName.value = serverStore.server?.name || ''
      updatedServerImage.value = serverStore.server?.image || ''
      showEditServerModal.value = true
    }

    function closeEditServerModal() {
      showEditServerModal.value = false
    }

    async function updateServerInfo() {
      if (!updatedServerName.value.trim() && !updatedServerImage.value.trim()) return
      await serverStore.updateServer({
        id: Number(serverId),
        name: updatedServerName.value.trim(),
        image: updatedServerImage.value.trim()
      })
      closeEditServerModal()
    }

    // Gestion de la modale d'invitation
    const showInviteModal = ref(false)
    const showGererModal = ref(false)
    const inviteInput = ref('')
    const selectedUser = ref(null)

    // Filtre des utilisateurs pour l'invitation
    const filteredUsers = computed(() => {
      if (!inviteInput.value) return []
      return userStore.inviteUsers.filter(user =>
        user.username.toLowerCase().includes(inviteInput.value.toLowerCase())
      )
    })

    function openInviteModal() {
      showInviteModal.value = true
      userStore.fetchInviteUsers(Number(serverId))
    }

    function openGererModal() {
        showGererModal.value = true;
        const userIds = serverStore.serverUsers.users?.map(u => u.user_id) || [];
        serverStore.fetchUserMap(userIds);
      }


    function closeGererModal() {
      showGererModal.value = false
    }

    function closeInviteModal() {
      showInviteModal.value = false
      inviteInput.value = ''
      selectedUser.value = null
    }

    function selectUser(user) {
      selectedUser.value = user
      inviteInput.value = user.username
    }

    async function addUserOnServer() {
      if (!selectedUser.value) {
        alert("Veuillez sélectionner un utilisateur dans la liste.")
        return
      }
      try {
        await serverStore.addUserOnServer(selectedUser.value.id, Number(serverId))
        alert("Utilisateur ajouté !")
        closeInviteModal()
        // Recharger la liste des users du serveur ?
        serverStore.fetchServerUsers(serverId)
      } catch (error) {
        console.error("Erreur lors de l'ajout de l'utilisateur :", error)
      }
    }

    onMounted(async () => {
      if (serverId && !isNaN(serverId)) {
        roomStore.setServerId(serverId);
        await roomStore.fetchRooms(serverId);
        console.log('Rooms after fetch:', roomStore.rooms); // Debug log
        await serverStore.fetchServerUsers(serverId);
      } else {
        console.error('Invalid serverId on mount:', serverId);
      }
    })

    // Sélection d'une room
    function selectRoom(room) {
      selectedRoom.value = room
    }

    // Ajout d'une room
    async function addRoom() {
      if (!newRoomName.value.trim()) return;
      if (!serverId || isNaN(serverId)) {
        console.error('Invalid serverId:', serverId);
        return;
      }
      await roomStore.addRoom(serverId, newRoomName.value.trim());
      console.log('Rooms after adding:', roomStore.rooms); // Debug log
      newRoomName.value = '';
    }

    // Ouvrir la modale pour éditer la room
    function openRoomModal(room) {
      roomToEdit.value = room
      editRoomName.value = room.name
      showRoomModal.value = true
    }

    function closeRoomModal() {
      showRoomModal.value = false
      editRoomName.value = ''
      roomToEdit.value = null
    }

    async function updateSelectedRoom() {
      if (!selectedRoom.value) return
      await roomStore.updateRoom({
        serverId: selectedRoom.value.id_server, // ou autre ID
        roomId: selectedRoom.value.id,
        newName: editRoomName.value.trim()
      })
      // Mettre à jour localement
      selectedRoom.value.name = editRoomName.value.trim()
      closeRoomModal()
    }

    async function deleteSelectedRoom() {
      if (!selectedRoom.value) return;
      try {
        const serverId = selectedRoom.value.id_server ?? roomStore.serverId;
        if (!serverId) {
          console.error('serverId is missing');
          alert('Impossible de supprimer la room : ID du serveur manquant.');
          return;
        }
        await roomStore.deleteRoom(selectedRoom.value.id, serverId);
        console.log('Rooms after deletion:', roomStore.rooms);
        selectedRoom.value = null;
      } catch (error) {
        console.error('Erreur lors de la suppression de la room', error);
        alert('Une erreur est survenue lors de la suppression de la room.');
      }
    }

    // Quitter le serveur
    async function leaveCurrentServer() {
      await serverStore.leaveServer(authStore.user.id, Number(serverId))
      router.push('/home')
    }

    // Supprimer complètement le serveur
    async function deleteServer() {
      await serverStore.deleteServer(Number(serverId))
      router.push('/home')
    }

    return {
      serverId,
      selectedRoom,
      newRoomName,
      editRoomName,
      updatedServerName,
      updatedServerImage,

      // Modale modifier serveur
      showEditServerModal,
      openEditServerModal,
      closeEditServerModal,

      // Modale d'invitation
      showInviteModal,
      inviteInput,
      selectedUser,
      filteredUsers,

      // Modale de gestion
      showGererModal,
      openGererModal,
      closeGererModal,


      openInviteModal,
      closeInviteModal,
      selectUser,
      addUserOnServer,

      // Méthodes rooms
      selectRoom,
      addRoom,
      updateSelectedRoom,
      deleteSelectedRoom,

      // Méthodes serveur
      leaveCurrentServer,
      deleteServer,
      updateServerInfo,

      // Stores
      authStore,
      serverStore,
      roomStore,
      userStore
    }
  }
}
</script>

<style scoped>

/* Conteneur principal (3 colonnes) */
.server-layout {
  display: flex;
  height: 100vh;
  font-family: Arial, sans-serif;
  background-color: #36393f; /* fond général “Discord” */
  color: #dcddde;           /* texte clair */
}

/* Colonne de gauche */
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
  background-color: #393c43; /* un peu plus clair au survol */
}
.left-panel li.selected {
  background-color: #5865F2; /* sur la room sélectionnée */
}

/* Pour un petit bloc d’ajout de room */
.add-room {
  margin-top: 10px;
  display: flex;
  gap: 5px;
}

/* Section de mise à jour du serveur */
.update-server-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Colonne du milieu */
.middle-panel {
  flex: 1; /* occupe l'espace central */
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

.room-edit-content {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.room-edit-content .buttons {
  display: flex;
  gap: 8px;
}

/* Colonne de droite */
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
  color: yellow; /* si l’utilisateur est admin */
  font-weight: bold;
}

/* Boutons */
.btn-primary,
.btn-danger {
  border: none;
  border-radius: 4px;
  padding: 8px 16px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 14px;
}

/* Bouton principal (bleu Discord) */
.btn-primary {
  background-color: #5865F2;
  color: #fff;
}
.btn-primary:hover {
  background-color: #4752c4;
}

/* Bouton danger (rouge) */
.btn-danger {
  background-color: #f04747;
  color: #fff;
}
.btn-danger:hover {
  background-color: #ce3c3c;
}

/* Overlay de modale (fond gris transparent) */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 900;
}

/* Modale d’invitation (ou autres modales) */
.invite-modal,
.editserv-modal {
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

.invite-modal h3,
.editserv-modal h3 {
  color: #fff;
  margin-bottom: 15px;
  text-align: center;
}

.invite-modal input,
.editserv-modal input {
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

/* Édition de la room dans une modale */
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

/* Optionnel : un petit bouton “edit” discret */
.edit-btn {
  margin-left: 10px;
  background-color: transparent;
  border: none;
  color: #fff;
  cursor: pointer;
  font-size: 16px;
}
.edit-btn:hover {
  color: #aaa;
}

</style>
