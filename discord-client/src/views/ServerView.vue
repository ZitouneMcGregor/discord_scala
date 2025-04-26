<template>
  <div class="server-layout">
    <!-- Left Panel: Server Info + Rooms List -->
    <aside class="left-panel">
      <div class="server-header">
        <h1>{{ serverStore.server?.name }}</h1>
        <div class="server-actions">
          <button class="btn-primary" @click="showInviteModal = true">Inviter</button>
          <button
            v-if="isAdmin"
            class="btn-primary"
            @click="showGererModal = true"
          >
            Gérer
          </button>
        </div>
      </div>
      <nav class="rooms-nav">
        <h2>Salons</h2>
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
          <input v-model="newRoomName" placeholder="Nouveau salon" />
          <button class="btn-primary" @click="addRoom">+</button>
        </div>
      </nav>
      <footer class="server-footer">
        <button class="btn-danger" @click="leaveCurrentServer">Quitter</button>
        <button
          v-if="isAdmin"
          class="btn-danger"
          @click="deleteServer"
        >Supprimer</button>
      </footer>

      <!-- Invite Modal Inline -->
      <div v-if="showInviteModal">
        <div class="modal-overlay" @click="showInviteModal = false"></div>
        <div class="modal-box">
          <h3>Inviter un utilisateur</h3>
          <input v-model="inviteUsername" placeholder="Username" />
          <button class="btn-primary" @click="addUserOnServer">Inviter</button>
          <button class="btn" @click="showInviteModal = false">Annuler</button>
        </div>
      </div>

      <!-- Manage Modal Inline -->
      <div v-if="showGererModal">
        <div class="modal-overlay" @click="showGererModal = false"></div>
        <div class="modal-box">
          <h3>Gérer les utilisateurs</h3>
          <ul>
            <li
              v-for="user in serverStore.serverUsers.users || []"
              :key="user.user_id"
              class="manage-user"
            >
              <span>{{ userMap[user.user_id] || user.user_id }}</span>
              <div class="manage-actions">
                <button
                  class="btn-primary"
                  v-if="!user.admin"
                  @click="toggleAdmin(user.user_id, true)"
                >Promouvoir</button>
                <button
                  class="btn-danger"
                  v-if="user.admin"
                  @click="toggleAdmin(user.user_id, false)"
                >Retirer</button>
                <button
                  class="btn-danger"
                  :disabled="user.user_id === authStore.user.id"
                  @click="kickUser(user.user_id)"
                >
                  Expulser
                </button>

              </div>
            </li>
          </ul>
          <button class="btn" @click="showGererModal = false">Fermer</button>
        </div>
      </div>
    </aside>

    <!-- Main Panel: Chat/Room Content -->
    <main class="main-panel">
      <div v-if="!selectedRoom" class="no-selection">
        <p>Sélectionnez un salon à gauche</p>
      </div>
      <div v-else class="room-content">
        <header class="room-header">
          <h2>{{ selectedRoom.name }}</h2>
          <button class="btn" @click="showEditRoomModal = true">✎</button>
        </header>
        <section class="messages-area">
          <!-- Messages component goes here -->
        </section>
        <footer class="message-input">
          <input v-model="newMessage" placeholder="Message…" @keyup.enter="sendMessage" />
          <button class="btn-primary" @click="sendMessage">Envoyer</button>
        </footer>
      </div>

      <!-- Edit Room Modal Inline -->
      <div v-if="showEditRoomModal">
        <div class="modal-overlay" @click="showEditRoomModal = false"></div>
        <div class="modal-box">
          <h3>Modifier salon</h3>
          <input v-model="editRoomName" placeholder="Nouveau nom" />
          <div class="edit-room-actions">
            <button class="btn-primary" @click="updateSelectedRoom">Enregistrer</button>
            <button class="btn-danger" @click="deleteSelectedRoom">Supprimer</button>
          </div>
          <button class="btn" @click="showEditRoomModal = false">Annuler</button>
        </div>
      </div>
    </main>

    <!-- Right Panel: Users List -->
    <aside class="right-panel">
      <h3>Utilisateurs</h3>
      <ul>
        <li
          v-for="user in serverStore.serverUsers.users"
          :key="user.user_id"
          :class="{ admin: user.admin }"
        >
          {{ userMap[user.user_id] || user.user_id }}
          <span v-if="user.admin" class="badge">Admin</span>
        </li>
      </ul>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'
import { useRoomStore } from '../store/room'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const serverStore = useServerStore()
const roomStore = useRoomStore()

const serverId = Number(route.params.serverId)
const selectedRoom = ref(null)
const newRoomName = ref('')
const newMessage = ref('')
const inviteUsername = ref('')
const editRoomName = ref('')

const showInviteModal = ref(false)
const showGererModal = ref(false)
const showEditRoomModal = ref(false)

const isAdmin = computed(
  () => (serverStore.serverUsers.users?.some(u => u.user_id === authStore.user.id && u.admin)) || false
)

onMounted(async () => {
  await roomStore.fetchRooms(serverId)
  await serverStore.fetchServerUsers(serverId)
})

function selectRoom(room) {
  selectedRoom.value = room
}

async function addRoom() {
  if (!newRoomName.value) return
  await roomStore.addRoom(serverId, newRoomName.value)
  newRoomName.value = ''
}

async function leaveCurrentServer() {
  await serverStore.leaveServer(authStore.user.id, serverId)
  router.push('/home')
}

async function deleteServer() {
  if (!isAdmin.value) return
  await serverStore.deleteServer(serverId)
  router.push('/home')
}

async function addUserOnServer() {
  if (!inviteUsername.value) return
  await serverStore.addUserOnServer(inviteUsername.value, serverId)
  inviteUsername.value = ''
  showInviteModal.value = false
  await serverStore.fetchServerUsers(serverId)
}

async function toggleAdmin(userId, makeAdmin) {
  await serverStore.toggleAdmin(serverId, userId, makeAdmin)
}

async function kickUser(userId) {
  await serverStore.kickUser(serverId, userId)
  await serverStore.fetchServerUsers(serverId)
}

async function sendMessage() {
  // Stub: implement message send
}

async function updateSelectedRoom() {
  if (!editRoomName.value.trim()) return
  await roomStore.updateRoom(selectedRoom.value.id, { name: editRoomName.value })
  selectedRoom.value.name = editRoomName.value
  showEditRoomModal.value = false
}

async function deleteSelectedRoom() {
  await roomStore.deleteRoom(selectedRoom.value.id, serverId)
  selectedRoom.value = null
}

const userMap = computed(() => serverStore.userMap)
</script>

<style scoped>
.server-layout {
  display: grid;
  grid-template-columns: 250px 1fr 200px;
  height: 100vh;
  background: #36393f;
  color: #dcddde;
}
.left-panel,
.right-panel {
  background: #2f3136;
  padding: 16px;
  overflow-y: auto;
}
.main-panel {
  display: flex;
  flex-direction: column;
  background: #36393f;
  padding: 16px;
}
/* Header */
.server-header {
  display: flex;
  flex-direction: column;
  margin-bottom: 16px;
}
.server-header h1 {
  margin: 0 0 8px 0;
  font-size: 1.5rem;
}
.server-actions {
  display: flex;
  gap: 8px;
}
/* Rooms navigation */
.rooms-nav h2 {
  margin-bottom: 8px;
  font-size: 1.25rem;
}
.rooms-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.rooms-nav li {
  padding: 10px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.2s;
}
.rooms-nav li:hover {
  background: #42454a;
}
.rooms-nav li.selected {
  background: #5865f2;
}
.add-room {
  display: flex;
  gap: 8px;
  margin-top: 12px;
}
.add-room input {
  flex: 1;
  padding: 8px;
  border-radius: 4px;
  border: 1px solid #555;
  background: #2f3136;
  color: #dcddde;
}
.add-room button {
  width: 40px;
}
/* Footer */
.server-footer {
  margin-top: auto;
  display: flex;
  gap: 8px;
}
/* Main panel: no selection */
.no-selection {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #72767d;
  font-size: 1.2rem;
}
/* Room content */
.room-content {
  display: flex;
  flex-direction: column;
  height: 100%;
}
.room-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.room-header h2 {
  margin: 0;
  font-size: 1.5rem;
}
.messages-area {
  flex: 1;
  background: #2f3136;
  border-radius: 4px;
  padding: 16px;
  overflow-y: auto;
  margin-bottom: 12px;
}
.message-input {
  display: flex;
  gap: 8px;
}
.message-input input {
  flex: 1;
  padding: 10px;
  border-radius: 4px;
  border: 1px solid #555;
  background: #2f3136;
  color: #dcddde;
}
.message-input button {
  padding: 10px 16px;
}
/* Right panel: users */
.right-panel h3 {
  margin: 0 0 12px 0;
  font-size: 1.25rem;
}
.right-panel ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.right-panel li {
  padding: 8px;
  border-radius: 4px;
  margin-bottom: 6px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: background 0.2s;
}
.right-panel li:hover {
  background: #42454a;
}
.badge {
  background: #43b581;
  color: #fff;
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 12px;
}
/* Buttons */
.btn,
.btn-primary,
.btn-danger {
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  padding: 8px 16px;
  transition: background 0.2s;
}
.btn-primary {
  background: #5865F2;
  color: #fff;
}
.btn-primary:hover {
  background: #4752c4;
}
.btn-danger {
  background: #f04747;
  color: #fff;
}
.btn-danger:hover {
  background: #ce3c3c;
}
/* Modals inline */
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
}
.modal-box {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: #36393f;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  z-index: 1001;
  width: 320px;
}
.modal-box h3 {
  margin-bottom: 16px;
  font-size: 1.25rem;
}
.modal-box input {
  width: 100%;
  padding: 10px;
  margin-bottom: 16px;
  border-radius: 4px;
  border: 1px solid #555;
  background: #2f3136;
  color: #dcddde;
}
.edit-room-actions {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 16px;
}
.manage-user {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 8px 0;
  border-bottom: 1px solid #555;
}
.manage-actions button {
  margin-left: 8px;
}
</style>
