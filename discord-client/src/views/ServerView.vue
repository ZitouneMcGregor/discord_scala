<template>
  <div class="server-layout">
    <!-- Left Panel -->
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
        <button v-if="isAdmin" class="btn-danger" @click="deleteServer">Supprimer</button>
      </footer>

      <!-- Invite modal -->
      <div v-if="showInviteModal">
        <div class="modal-overlay" @click="showInviteModal = false"></div>
        <div class="modal-box">
          <h3>Inviter un utilisateur</h3>
          <input v-model="inviteUsername" placeholder="Username" />
          <button class="btn-primary" @click="addUserOnServer">Inviter</button>
          <button class="btn" @click="showInviteModal = false">Annuler</button>
        </div>
      </div>

      <!-- Manage modal -->
      <div v-if="showGererModal">
        <div class="modal-overlay" @click="showGererModal = false"></div>
        <div class="modal-box">
          <h3>Gérer les utilisateurs</h3>
          <ul>
            <li v-for="user in serverStore.serverUsers.users || []" :key="user.user_id" class="manage-user">
              <span>{{ userMap[user.user_id] || user.user_id }}</span>
              <div class="manage-actions">
                <button class="btn-primary" v-if="!user.admin" @click="toggleAdmin(user.user_id, true)">Promouvoir</button>
                <button class="btn-danger" v-if="user.admin" @click="toggleAdmin(user.user_id, false)">Retirer</button>
                <button class="btn-danger" @click="kickUser(user.user_id)">Expulser</button>
              </div>
            </li>
          </ul>
          <button class="btn" @click="showGererModal = false">Fermer</button>
        </div>
      </div>
    </aside>

    <!-- Main Panel -->
    <main class="main-panel">
      <div v-if="!selectedRoom" class="no-selection">
        <p>Sélectionnez un salon à gauche</p>
      </div>
      <div v-else class="room-content">
        <header class="room-header">
          <h2>{{ selectedRoom.name }}</h2>
          <button class="btn" @click="showEditRoomModal = true">✎</button>
        </header>

        <section class="messages-area" ref="messageContainer">
          <div
            v-for="msg in messages"
            :key="msg.ts + msg.content"
            :class="['message', { me: msg.from.id === currentUserId }]"
          >
            <div class="author">{{ msg.from.username }}</div>
            <div class="content">{{ msg.content }}</div>
            <div class="ts">{{ formatTime(msg.ts) }}</div>
          </div>
        </section>

        <footer class="message-input">
          <input v-model="newMessage" placeholder="Message…" @keyup.enter="sendMessage" />
          <button class="btn-primary" @click="sendMessage">Envoyer</button>
        </footer>
      </div>

      <!-- Edit Room modal -->
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

    <!-- Right Panel -->
    <aside class="right-panel">
      <h3>Utilisateurs</h3>
      <ul>
        <li v-for="user in serverStore.serverUsers.users" :key="user.user_id" :class="{ admin: user.admin }">
          {{ userMap[user.user_id] || user.user_id }}
          <span v-if="user.admin" class="badge">Admin</span>
        </li>
      </ul>
    </aside>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, nextTick, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useServerStore } from '../store/servers'
import { useRoomStore } from '../store/room'
import axios from 'axios'

/* —— WS CONFIG */
const WS_URL = 'ws://localhost:8082/subscriptions'
const BASIC_USER = 'foo'
const BASIC_PASS = 'bar'

/* —— STORES & ROUTER */
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const serverStore = useServerStore()
const roomStore = useRoomStore()

/* —— REFS */
const serverId = Number(route.params.serverId)
const selectedRoom = ref<any | null>(null)
const newRoomName = ref('')
const newMessage = ref('')
const inviteUsername = ref('')
const editRoomName = ref('')
const showInviteModal = ref(false)
const showGererModal = ref(false)
const showEditRoomModal = ref(false)
const messages = ref<any[]>([])
const messageContainer = ref<HTMLElement | null>(null)

/* —— COMPUTEDS */
const currentUserId = computed(() => authStore.user.id)
const isAdmin = computed(() => (serverStore.serverUsers.users?.some(u => u.user_id === authStore.user.id && u.admin)) || false)
const userMap = computed(() => serverStore.userMap)

/* —— WEBSOCKET */
/* —— WEBSOCKET */
let ws: WebSocket | null = null
function connectWs() {
  ws = new WebSocket(WS_URL)
  ws.addEventListener('open', () => {
    console.log('WebSocket connected')
    if (selectedRoom.value) subscribeRoom(selectedRoom.value.id)
  })
  ws.addEventListener('message', handleWsMessage)
  ws.addEventListener('error', (err) => {
    console.error('WebSocket error', err)
  })
  ws.addEventListener('close', () => {
    console.log('WebSocket disconnected')
  })
}

function subscribeRoom(roomId: number) {
  if (ws && ws.readyState === WebSocket.OPEN) {
    console.log(`Subscribing to room ${roomId}`)
    ws.send(`r${roomId}`)
  }
}

function handleWsMessage(e: MessageEvent) {
  console.log('Raw message received:', e.data); // Vérifie si ça s'affiche

  if (typeof e.data === 'string' && !e.data.trim().startsWith('{')) {
    console.log('Non-JSON frame, skipping');
    return;
  }

  try {
    const msg = JSON.parse(e.data);
    console.log('Parsed WS message:', msg); // Log l'objet parsé

    // --- AJOUT DE LOGS DE DEBUG ---
    const expectedRoomId = `r${selectedRoom.value?.id}`;
    const receivedRoomId = msg.metadata?.roomId;
    console.log(`[WS Debug] Selected Room: ${selectedRoom.value?.id}, Expected WS ID: ${expectedRoomId}`);
    console.log(`[WS Debug] Received metadata:`, msg.metadata);
    console.log(`[WS Debug] Received WS ID: ${receivedRoomId}`);
    console.log(`[WS Debug] IDs match? : ${receivedRoomId === expectedRoomId}`);
    // --- FIN DES LOGS DE DEBUG ---


    // Vérifie si le message appartient au salon courant
    if (receivedRoomId === expectedRoomId) { // Utilise les variables pour plus de clarté
        console.log('[WS Debug] Condition passed! Adding message.'); // Log si la condition passe
      messages.value.push({
        id: receivedRoomId, // Utilise la variable
        from: {
          id: msg.metadata.fromId,
          username: msg.metadata.fromUsername,
        },
        content: msg.content,
        ts: msg.timestamp ? new Date(msg.timestamp).getTime() : Date.now(),
      });

      nextTick(() => {
        if (messageContainer.value) {
           messageContainer.value.scrollTop = messageContainer.value.scrollHeight;
        }
      });
    } else {
        // Log si la condition échoue
        console.log('[WS Debug] Condition failed. Message not added.');
    }
  } catch (err) {
    console.error('WS parse error', err);
  }
}


/* —— LIFECYCLE */
onMounted(async () => {
  connectWs()
  await roomStore.fetchRooms(serverId)
  await serverStore.fetchServerUsers(serverId)
})

watch(selectedRoom, (room) => {
  if (room) {
    subscribeRoom(room.id)
  }
})

/* —— ROOM ACTIONS */
async function addRoom() {
  if (!newRoomName.value.trim()) return
  await roomStore.addRoom(serverId, newRoomName.value)
  newRoomName.value = ''
}

async function selectRoom(room: any) {
  selectedRoom.value = room
  await loadMessages(room.id)
  subscribeRoom(room.id)
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
  if (!inviteUsername.value.trim()) return
  await serverStore.addUserOnServer(inviteUsername.value, serverId)
  inviteUsername.value = ''
  showInviteModal.value = false
  await serverStore.fetchServerUsers(serverId)
}

async function toggleAdmin(userId: number, makeAdmin: boolean) {
  await serverStore.toggleAdmin(serverId, userId, makeAdmin)
}

async function kickUser(userId: number) {
  await serverStore.kickUser(serverId, userId)
  await serverStore.fetchServerUsers(serverId)
}

/* —— MESSAGES */
async function sendMessage() {
  if (!newMessage.value.trim() || !selectedRoom.value) return;
  const roomIdStr = `r${selectedRoom.value.id}`;
  const payload = {
    id: roomIdStr,
    content: newMessage.value.trim(),
    metadata: {
      roomId: roomIdStr,
      fromId: authStore.user.id.toString(),
      fromUsername: authStore.user.username,
    },
  };
  try {
    // Envoie le message au backend via HTTP
    await axios.post('http://localhost:8081/message', payload, { auth: { username: BASIC_USER, password: BASIC_PASS } });

    // --- SUPPRIMÉ ---
    // messages.value.push({ ... }) n'est plus ici
    // nextTick(...) pour le scroll n'est plus ici

    // Vide le champ de saisie SEULEMENT APRÈS l'envoi réussi
    newMessage.value = '';

  } catch (err) {
    console.error('Erreur envoi message', err);
    // TODO: Peut-être afficher une erreur à l'utilisateur ici ?
    // Par exemple, ne pas vider le champ newMessage.value en cas d'erreur
    // pour qu'il puisse réessayer.
  }
}

async function loadMessages(roomId: number) {
  try {
    const idStr = `r${roomId}`
    const { data } = await axios.get(`http://localhost:8083/server/${serverId}/room/${idStr}/messages`, { auth: { username: BASIC_USER, password: BASIC_PASS } })
    if (Array.isArray(data.messages)) {
      messages.value = data.messages.map((m: any) => ({
        id: idStr,
        from: { id: m.metadata.fromId, username: m.metadata.fromUsername },
        content: m.content,
        ts: new Date(m.timestamp).getTime(),
      }))
    } else {
      messages.value = []
    }
    nextTick(() => {
      if (messageContainer.value) messageContainer.value.scrollTop = messageContainer.value.scrollHeight
    })
  } catch (err) {
    console.error('Erreur chargement messages', err)
  }
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

function formatTime(ts: number) {
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2, '0')}:${d.getMinutes().toString().padStart(2, '0')}`
}
</script>



<style scoped>
/* ---------------------------------------------------------------- Layout */
.server-layout {
  display: grid;
  grid-template-columns: 250px 1fr 200px;
  height: 100vh;
  background: #36393f;              /* même bg global */
  color: #dcddde;
}

/* ---------------------------------------------------------------- Panels */
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
  min-width: 0;                     /* évite le débordement */
}

/* ---------------------------------------------------------------- Header */
.server-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  gap: 12px;
}
.server-header h1 {
  margin: 0;
  font-size: 1.3rem;
  line-height: 1.2;
  flex: 1;
}
.server-actions {
  display: flex;
  gap: 8px;
}

/* ---------------------------------------------------------------- Rooms  */
.rooms-nav h2 {
  margin: 0 0 6px;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #949ba4;
}
.rooms-nav ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.rooms-nav li {
  padding: 8px 10px;
  margin-bottom: 4px;
  border-radius: 4px;
  cursor: pointer;
  transition: background 0.15s;
}
.rooms-nav li:hover       { background: #393c43; }   /* même hover global */
.rooms-nav li.selected    { background: #5865f2; }

.add-room {
  display: flex;
  gap: 6px;
  margin-top: 10px;
}
.add-room input {
  flex: 1;
  padding: 8px;
  border: 1px solid #555;
  border-radius: 4px;
  background: #303338;
  color: #dcddde;
}
.add-room button { width: 38px; }

/* ---------------------------------------------------------------- Footer */
.server-footer {
  margin-top: auto;
  display: flex;
  gap: 8px;
}

/* ------------------------------------------------------ No-selection msg */
.no-selection {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #72767d;
  font-size: 1.1rem;
}

/* ------------------------------------------------ Room content & header */
.room-content { display: flex; flex-direction: column; height: 100%; }
.room-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}
.room-header h2 { margin: 0; font-size: 1.25rem; flex: 1; }

/* ----------------------------------------------------------- Messages    */
.messages-area {
  flex: 1;
  background: #2f3136;
  border-radius: 4px;
  padding: 14px;
  overflow-y: auto;
  margin-bottom: 10px;
}
.message {
  max-width: 65%;
  padding: 10px 14px;
  margin-bottom: 10px;
  border-radius: 6px;
  background: #202225;
  line-height: 1.35;
}
.message.me {
  margin-left: auto;
  background: #5865f2;
  color: #fff;
}
.author       { font-weight: 600; margin-bottom: 2px; }
.ts           { font-size: 0.75rem; margin-top: 4px; color: #8e9297; text-align: right; }

/* -------------------------------------------------------- Input message */
.message-input {
  display: flex;
  gap: 8px;
}
.message-input input {
  flex: 1;
  padding: 10px;
  border: 1px solid #555;
  border-radius: 4px;
  background: #303338;
  color: #dcddde;
}
.message-input button { padding: 10px 18px; }

/* -------------------------------------------------------- Users list    */
.right-panel h3 {
  margin: 0 0 10px;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  color: #949ba4;
}
.right-panel ul {
  list-style: none;
  padding: 0;
  margin: 0;
}
.right-panel li {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 8px;
  border-radius: 4px;
  margin-bottom: 4px;
  transition: background 0.15s;
}
.right-panel li:hover { background: #393c43; }
.badge {
  background: #43b581;
  color: #fff;
  padding: 2px 6px;
  border-radius: 12px;
  font-size: 0.7rem;
}

/* -------------------------------------------------------------- Buttons */
.btn,
.btn-primary,
.btn-danger {
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 0.85rem;
  padding: 7px 14px;
  transition: background 0.15s;
}
.btn-primary  { background: #5865f2; color: #fff; }
.btn-primary:hover { background: #4752c4; }
.btn-danger   { background: #f04747; color: #fff; }
.btn-danger:hover  { background: #ce3c3c; }

/* -------------------------------------------------------------- Modals  */
.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.5);
  z-index: 1000;
}
.modal-box {
  position: fixed;
  top: 50%; left: 50%;
  transform: translate(-50%, -50%);
  width: 320px;
  background: #2f3136;
  padding: 24px;
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0,0,0,0.3);
  z-index: 1001;
}
.modal-box h3 { margin: 0 0 14px; font-size: 1.1rem; }
.modal-box input,
.modal-box select {
  width: 100%;
  padding: 10px;
  margin-bottom: 14px;
  border: 1px solid #555;
  border-radius: 4px;
  background: #303338;
  color: #dcddde;
}
.edit-room-actions { display: flex; gap: 10px; justify-content: center; margin-bottom: 12px; }

/* ----------------------------------------------------- Manage-user list */
.manage-user {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 6px 0;
  border-bottom: 1px solid #444;
}
.manage-actions button { margin-left: 6px; }
</style>
