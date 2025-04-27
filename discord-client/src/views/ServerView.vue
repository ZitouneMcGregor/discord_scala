<template>
  <div class="server-layout">
  <!-- ---------- Sidebar ---------- -->
  <aside class="left-panel card">
    <div class="server-header">
      <h1>{{ serverStore.serverUsers?.serverName || 'Serveur' }}</h1>
      <div class="server-actions">
        <button class="btn-primary" @click="showInvite = true">Inviter</button>
        <button v-if="isAdmin" class="btn-primary" @click="showManage = true">Gérer</button>
      </div>
    </div>

    <!-- Liste des salons -->
    <nav class="rooms-nav">
      <h2>Salons</h2>
      <ul class="room-list">
        <li
          v-for="room in roomStore.rooms"
          :key="room.id"
          :class="['room-entry',{selected: roomStore.selectedRoom && room.id===roomStore.selectedRoom.id}]"
          @click="roomStore.selectRoom(room)"
        >
          {{ room.name }}
        </li>
      </ul>
      <div class="add-room">
        <input v-model="newRoomName" placeholder="Nouveau salon" class="create-input" />
        <button class="btn-primary" @click="addRoom">+</button>
      </div>
    </nav>

    <!-- Quitter / supprimer -->
    <footer class="server-footer">
      <button class="btn-danger" @click="leaveServer">Quitter</button>
      <button v-if="isAdmin" class="btn-danger" @click="deleteServer">Supprimer</button>
    </footer>
  </aside>

  <!-- ---------- Zone de chat ---------- -->
  <main class="chat-main card">
    <div v-if="!roomStore.selectedRoom" class="chat-placeholder">
      <p>Sélectionne un salon à gauche pour commencer à discuter.</p>
    </div>

    <div v-else class="chat-window">
      <header class="chat-header">
        <h4>{{ roomStore.selectedRoom.name }}</h4>
        <button class="btn" @click="openEditRoom">✎</button>
      </header>

      <div ref="msgArea" class="messages">
        <div
          v-for="m in roomStore.currentMessages"
          :key="m.ts + m.content"
          :class="['message',{me:Number(m.metadata.fromId)===authStore.user.id}]"
        >
          <div class="author">{{ m.metadata.fromUsername }}</div>
          <div class="content">{{ m.content }}</div>
          <div class="ts">{{ fmt(m.ts) }}</div>
        </div>
      </div>

      <form class="message-form" @submit.prevent="send">
        <input v-model="newMessage" class="create-input" placeholder="Entrez votre message…" />
        <button class="btn-primary">Envoyer</button>
      </form>
    </div>
  </main>

  <!-- ---------- Membres ---------- -->
  <aside class="right-panel card">
    <h3>Membres</h3>
    <ul class="user-list">
      <li
        v-for="u in serverStore.serverUsers.users"
        :key="u.user_id"
        :class="{ admin: u.admin }"
      >
        {{ userMap[u.user_id] || u.user_id }}
        <span v-if="u.admin" class="badge">Admin</span>
      </li>

    </ul>
  </aside>

  <!-- ---------- Invite Modal ---------- -->
  <div v-if="showInvite">
    <div class="modal-overlay" @click="showInvite = false"></div>
    <div class="modal-box">
      <h3>Inviter un utilisateur</h3>
      <input v-model="inviteUsername" placeholder="Nom d'utilisateur" />
      <button class="btn-primary" @click="addUserOnServer">Inviter</button>
      <button class="btn" @click="showInvite = false">Annuler</button>
    </div>
  </div>

  <!-- ---------- Manage Modal ---------- -->
  <div v-if="showManage">
    <div class="modal-overlay" @click="showManage = false"></div>
    <div class="modal-box">
      <h3>Gérer les utilisateurs</h3>
      <ul>
        <li v-for="user in serverStore.serverUsers.users || []" :key="user.user_id" class="manage-user">
          <span>{{ userMap[user.user_id] || user.user_id }}</span>
          <div class="manage-actions">

            <button
              class="btn-primary"
              v-if="!user.admin"
              @click="toggleAdmin(user.user_id, true)"
            >
              Promouvoir
            </button>

            <button
              class="btn-danger"
              @click="kickUser(user.user_id)"
            >
              Retirer
            </button>

          </div>
        </li>
      </ul>
      <button class="btn" @click="showManage = false">Fermer</button>
    </div>
  </div>

  <!-- ---------- Edit Room Modal ---------- -->
  <div v-if="showEditRoom">
    <div class="modal-overlay" @click="showEditRoom=false"></div>
    <div class="modal-box">
      <h3>Modifier salon</h3>
      <input v-model="editRoomName" placeholder="Nouveau nom" />
      <div class="edit-room-actions">
        <button class="btn-primary" @click="updateSelectedRoom">Enregistrer</button>
        <button class="btn-danger" @click="deleteSelectedRoom">Supprimer</button>
      </div>
      <button class="btn" @click="showEditRoom=false">Annuler</button>
    </div>
  </div>
</div>

</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../store/auth'
import { useRoomStore } from '../store/room'
import { useServerStore } from '../store/servers'
import axios from 'axios'
import { wsService } from '../store/wsService'

const route      = useRoute()
const router     = useRouter()
const authStore  = useAuthStore()
const roomStore  = useRoomStore()
const serverStore= useServerStore()

const serverId   = Number(route.params.serverId)
const newRoomName= ref('')
const newMessage = ref('')
const msgArea    = ref<HTMLElement|null>(null)

/* ---------- Modals ---------- */
const showInvite = ref(false)
const showManage = ref(false)
const showEditRoom = ref(false)
const userMap = computed(() => serverStore.userMap)

const inviteUsername = ref('')
const editRoomName   = ref('')

const isAdmin = computed(() =>
  serverStore.serverUsers.users.some(u => u.user_id === authStore.user.id && u.admin)
)

/* ---------- init ---------- */
onMounted(async () => {
  if (!authStore.user) return router.push('/login')
  await roomStore.setServer(serverId)
  await serverStore.fetchServerUsers(serverId)
})

/* ---------- auto-scroll ---------- */
watch(() => roomStore.currentMessages, () => nextTick(scrollBottom), { deep:true })
watch(() => roomStore.selectedRoom,     () => nextTick(scrollBottom))
function scrollBottom() {
  if (msgArea.value) msgArea.value.scrollTop = msgArea.value.scrollHeight
}

/* ---------- actions ---------- */
 async function addRoom() {
  if (!newRoomName.value.trim()) return
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
    await axios.post('http://localhost:8081/message', payload,
      { auth:{ username:'foo', password:'bar' } })
    newMessage.value = ''
  } catch (e) { console.error('send error', e) }
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

 function fmt(ts:number) {
  return new Date(ts)
    .toLocaleTimeString([], { hour:'2-digit', minute:'2-digit' })
}

/* ---------- modal actions ---------- */
 async function addUserOnServer() {
  if (!inviteUsername.value.trim()) return
  await serverStore.addUserOnServer(inviteUsername.value, serverId)
  inviteUsername.value = ''
  showInvite.value = false
  await serverStore.fetchServerUsers(serverId)
}

 async function toggleAdmin(userId:number, makeAdmin:boolean) {
  await serverStore.toggleAdmin(serverId, userId, makeAdmin)
}

async function kickUser(userId) {
  await serverStore.kickUser(serverId, userId)
  await serverStore.fetchServerUsers(serverId)

  // on retire des subscriptions WS uniquement les rooms de CE serveur
  const roomIds = roomStore.rooms
    .map(r => `r${r.id}`)
  wsService.removeWatchIds(roomIds)
}



 function openEditRoom() {
  if (!roomStore.selectedRoom) return
  editRoomName.value = roomStore.selectedRoom.name
  showEditRoom.value = true
}

async function updateSelectedRoom() {
  if (!editRoomName.value.trim() || !roomStore.selectedRoom) return;

  await roomStore.updateRoom(
    roomStore.selectedRoom.id,   // roomId : number
    editRoomName.value,          // newName : string
    serverId                     // serverId : number
  );

  // Mise à jour locale
  roomStore.selectedRoom.name = editRoomName.value;
  showEditRoom.value = false;
}


 async function deleteSelectedRoom() {
  if (!roomStore.selectedRoom) return
  await roomStore.deleteRoom(roomStore.selectedRoom.id, serverId)
  showEditRoom.value = false
}

</script>

<style scoped>
/* ------ Layout global ------ */
.server-layout{
  display:flex;
  height:100vh;
  background:#36393f;
  color:#dcddde;
  font-family:'Segoe UI',Tahoma,Verdana,sans-serif;
  gap:20px;
  padding:20px;
  box-sizing:border-box;
}
.card{
  background:#2f3136;
  border-radius:6px;
  padding:20px;
  box-shadow:0 2px 5px rgba(0,0,0,.3);
  display:flex;
  flex-direction:column;
}
.left-panel{width:280px}
.right-panel{width:220px;overflow-y:auto}
.chat-main{flex:1}

/* ------ Liste des salons ------ */
.room-list{flex:1;overflow-y:auto;list-style:none;margin:0;padding:0}
.room-entry{
  padding:10px;
  border-radius:4px;
  cursor:pointer;
  margin-bottom:4px;
  transition:background .2s;
}
.room-entry:hover{background:#393c43}
.room-entry.selected{background:#202225}
.add-room{display:flex;margin-top:1rem;gap:6px}
.create-input{
  flex:1;padding:8px;border:none;border-radius:4px;
  background:#303338;color:#dcddde;
}

/* ------ Zone de chat ------ */
.chat-window{flex:1;display:flex;flex-direction:column}
.chat-header{
  display:flex;align-items:center;justify-content:space-between;
  border-bottom:1px solid #393c43;margin-bottom:1rem
}
.messages{
  flex:1;overflow-y:auto;margin-bottom:1rem;background:#2f3136;
  border-radius:6px;padding:14px;max-height:720px
}
.message{
  max-width:60%;margin-bottom:10px;padding:10px;border-radius:6px;
  background:#202225;overflow-wrap:anywhere;word-break:break-word;
  white-space:pre-wrap;text-align:left
}
.message.me{margin-left:auto;background:#5865f2;color:#fff;text-align:right}
.author{font-weight:bold;margin-bottom:4px}
.ts{font-size:.75rem;color:#ffffff;margin-top:4px}
.message-form{display:flex;gap:10px}

/* ------ Users ------ */
.user-list{list-style:none;margin:0;padding:0}
.user-list li{
  padding:6px 8px;border-radius:4px;margin-bottom:4px;
  display:flex;justify-content:space-between;align-items:center;
  transition:background .15s
}
.user-list li:hover{background:#393c43}
.badge{
  background:#43b581;color:#fff;padding:2px 6px;border-radius:12px;font-size:.7rem
}

/* ------ Boutons ------ */
.btn-primary{
  background:#5865f2;color:#fff;border:none;border-radius:4px;
  padding:8px 16px;cursor:pointer
}
.btn-primary:hover{background:#4752c4}
.btn-danger{
  background:#f04747;color:#fff;border:none;border-radius:4px;
  padding:7px 14px;cursor:pointer
}
.btn-danger:hover{background:#ce3c3c}
.btn{
  background:#4f545c;color:#fff;border:none;border-radius:4px;
  padding:7px 14px;cursor:pointer
}
.btn:hover{background:#3c4046}

/* ------ Placeholder ------ */
.chat-placeholder{
  flex:1;display:flex;align-items:center;justify-content:center;
  color:#72767d;font-style:italic
}

/* ------ Modals ------ */
.modal-overlay{
  position:fixed;inset:0;background:rgba(0,0,0,0.5);z-index:1000
}
.modal-box{
  position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);
  background:#2f3136;padding:24px;border-radius:8px;
  box-shadow:0 4px 12px rgba(0,0,0,0.3);z-index:1001;width:340px
}
.modal-box h3{margin:0 0 14px;font-size:1.1rem}
.modal-box input{
  width:100%;padding:10px;margin-bottom:14px;border:1px solid #555;
  border-radius:4px;background:#303338;color:#dcddde;
}
.edit-room-actions{display:flex;gap:10px;justify-content:center;margin-bottom:12px}
.manage-user{
  display:flex;justify-content:space-between;align-items:center;
  padding:6px 0;border-bottom:1px solid #444
}
.manage-actions button{margin-left:6px}

</style>
