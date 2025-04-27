<template>
  <div class="server-layout">
    <!-- ------------------ Left panel ------------------ -->
    <aside class="left-panel">
      <div class="server-header">
        <h1>{{ serverStore.serverUsers?.serverName || 'Server' }}</h1>
        <div class="server-actions">
          <button class="btn-primary" @click="showInvite = true">Inviter</button>
          <button v-if="isAdmin" class="btn-primary" @click="showManage = true">Gérer</button>
        </div>
      </div>

      <!-- salons -------------------------------------------------- -->
      <nav class="rooms-nav">
        <h2>Salons</h2>
        <ul>
          <li
            v-for="room in roomStore.rooms"
            :key="room.id"
            :class="{ selected: roomStore.selectedRoom && room.id === roomStore.selectedRoom.id }"
            @click="roomStore.selectRoom(room);console.log('clicked', room.id)"
          >
            {{ room.name }}
          </li>
        </ul>
        <div class="add-room">
          <input v-model="newRoomName" placeholder="Nouveau salon" />
          <button class="btn-primary" @click="addRoom">+</button>
        </div>
      </nav>

      <!-- footer -->
      <footer class="server-footer">
        <button class="btn-danger" @click="leaveServer">Quitter</button>
        <button v-if="isAdmin" class="btn-danger" @click="deleteServer">Supprimer</button>
      </footer>
    </aside>

    <!-- ------------------ Main panel ------------------- -->
    <main class="main-panel">
      <div v-if="!roomStore.selectedRoom" class="no-selection">
        Sélectionnez un salon à gauche
      </div>

      <div v-else class="room-content">
        <header class="room-header">
          <h2>{{ roomStore.selectedRoom.name }}</h2>
        </header>

        <!-- messages -->
        <section ref="msgArea" class="messages-area">
          <div
            v-for="msg in roomStore.currentMessages"
            :key="msg.ts + msg.content"
            :class="['message', { me: msg.metadata.fromId == authStore.user.id }]"
          >
            <div class="author">{{ msg.metadata.fromUsername }}</div>
            <div class="content">{{ msg.content }}</div>
            <div class="ts">{{ formatTime(msg.ts) }}</div>
          </div>
        </section>

        <!-- input -->
        <footer class="message-input">
          <input v-model="newMessage" placeholder="Message…" @keyup.enter="send" />
          <button class="btn-primary" @click="send">Envoyer</button>
        </footer>
      </div>
    </main>

    <!-- ------------------ Right panel ------------------ -->
    <aside class="right-panel">
      <h3>Membres</h3>
      <ul>
        <li v-for="u in serverStore.serverUsers.users" :key="u.user_id" :class="{ admin: u.admin }">
          {{ u.username }}
          <span v-if="u.admin" class="badge">Admin</span>
        </li>
      </ul>
    </aside>
  </div>
</template>

<script setup>
import { ref, onMounted, watch, nextTick, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useRoomStore } from '../store/room'
import { useServerStore } from '../store/servers'
import { wsService } from '../store/wsService'
import axios from 'axios'

/* ----------------------------------------------------------------- */
const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const roomStore = useRoomStore()
const serverStore = useServerStore()

const serverId = Number(route.params.serverId)
const newRoomName = ref('')
const newMessage = ref('')
const msgArea = ref(null)

const isAdmin = computed(() => serverStore.serverUsers.users.some(u => u.user_id === authStore.user.id && u.admin))

/* --------------------------- lifecycle --------------------------- */
onMounted(async () => {
  if (!authStore.user) {
    router.push('/login')
    return
  }
  // récupère rooms + users + ws watch-list
  await roomStore.setServer(serverId)
  await serverStore.fetchServerUsers(serverId)
})

// scroll auto quand nouveaux messages
watch(() => roomStore.currentMessages.length, () => nextTick(() => {
  msgArea.value && (msgArea.value.scrollTop = msgArea.value.scrollHeight)
}))

/* --------------------------- actions ----------------------------- */
async function addRoom() {
  await roomStore.addRoom(serverId, newRoomName.value)
  newRoomName.value = ''
}

async function send() {
  if (!newMessage.value.trim() || !roomStore.selectedRoom) return
  const payload = {
    id: `r${roomStore.selectedRoom.id}`,
    content: newMessage.value.trim(),
    metadata: {
      roomId: `r${roomStore.selectedRoom.id}`,
      fromId: authStore.user.id.toString(),
      fromUsername: authStore.user.username,
    },
  }
  try {
    await axios.post('http://localhost:8081/message', payload, {
      auth: { username: 'foo', password: 'bar' },
    })
    newMessage.value = ''
  } catch (err) {
    console.error('send error', err)
  }
}

async function leaveServer() {
  await serverStore.leaveServer(authStore.user.id, serverId)
  router.push('/home')
}
async function deleteServer() {
  if (!isAdmin.value) return
  await serverStore.deleteServer(serverId)
  router.push('/home')
}

function formatTime(ts) {
  const d = new Date(ts)
  return d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })
}
</script>
<style scoped>
/* ---------------------------------------------------------------- Layout */
.server-layout {
  display: grid;
  grid-template-columns: 250px 1fr 200px;   /* sidebar  ┆  main  ┆ users */
  height: 100vh;
  background: #36393f;                      /* fond Discord-like */
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
  min-width: 0;                             /* évite débordement */
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
.rooms-nav li:hover    { background: #393c43; }
.rooms-nav li.selected { background: #5865f2; }

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
.author { font-weight: 600; margin-bottom: 2px; }
.ts     { font-size: 0.75rem; margin-top: 4px; color: #8e9297; text-align: right; }

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
</style>
