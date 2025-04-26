<template>
  <div class="private-home-container">
    <!-- Sidebar : liste de chats -->
    <aside class="card sidebar">
      <h3 class="card-title">Chats Privés</h3>
      <div v-if="loadingChats" class="empty">Chargement…</div>
      <ul v-else class="chat-list">
        <li
          v-for="chat in chats"
          :key="chat.id"
          :class="['chat-entry', { selected: selectedChat && selectedChat.id === chat.id }]"
        >
          <button class="chat-item-content" @click="selectChat(chat)">
            {{ getOtherPseudo(chat) }}
          </button>
          <button class="btn-delete-chat" @click.stop="deleteChat(chat.id)">🗑️</button>
        </li>
      </ul>

      <!-- Auto-complétion pour créer un nouveau chat -->
      <form @submit.prevent="createChat" class="new-chat-form">
        <div class="autocomplete">
          <input
            v-model="searchTerm"
            type="text"
            placeholder="Chercher un utilisateur…"
            class="create-server-input"
            @focus="showSuggestions = true"
            @blur="handleBlur"
            @input="onInput"
            @keydown.enter.prevent="onEnter"
          />
          <ul v-if="showSuggestions && suggestions.length" class="suggestions above">
            <li
              v-for="user in suggestions"
              :key="user.id"
              class="suggestion-item"
              @mousedown.prevent="selectUser(user)"
            >
              {{ user.username }}
            </li>
          </ul>
        </div>
        <button
          type="submit"
          class="btn-primary"
          :disabled="!selectedUser"
        >
          +
        </button>
      </form>
    </aside>

    <!-- Main : conversation -->
    <main class="card chat-window" v-if="selectedChat">
      <header class="chat-header">
        <h4>Chat avec {{ getOtherPseudo(selectedChat) }}</h4>
      </header>

      <div class="messages" ref="messageContainer">
        <div
          v-for="msg in messages"
          :key="msg.id"
          :class="['message', { me: msg.from === currentUserId }]"
        >
          <div class="content">{{ msg.content }}</div>
          <div class="ts">{{ formatTime(msg.ts) }}</div>
        </div>
      </div>

      <form @submit.prevent="sendMessage" class="message-form">
        <input
          v-model="newMessage"
          type="text"
          placeholder="Entrez votre message…"
          class="create-server-input"
        />
        <button type="submit" class="btn-primary">Envoyer</button>
      </form>
    </main>

    <div class="card chat-placeholder" v-else>
      <p>Sélectionne un chat à gauche pour démarrer la conversation.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useAuthStore } from '../store/auth'
import { usePrivateChatStore } from '../store/privateChats'
import axios from 'axios'

const authStore = useAuthStore()
const privateChatStore = usePrivateChatStore()
const currentUserId = computed(() => authStore.user.id)

const loadingChats = ref(true)
const searchTerm = ref('')
const suggestions = ref([])
const showSuggestions = ref(false)
const selectedUser = ref(null)
const selectedChat = ref(null)
const messages = ref([])
const newMessage = ref('')

const chats = computed(() => privateChatStore.privateChats)

// Map des pseudos par ID
const userNames = ref({})

onMounted(async () => {
  await privateChatStore.fetchPrivateChats(currentUserId.value)
  // charger les pseudos des autres users
  const otherIds = [...new Set(privateChatStore.privateChats
    .map(c => c.user_id_1 === currentUserId.value ? c.user_id_2 : c.user_id_1))]
  await Promise.all(otherIds.map(async id => {
    try {
      const res = await axios.get(`http://localhost:8080/users/id/${id}`)
      if (res.data) userNames.value[id] = res.data.username
    } catch {}
  }))
  loadingChats.value = false
})

watch(searchTerm, (val) => {
  selectedUser.value = null
  clearTimeout(debounce)
  if (val.trim().length < 2) {
    suggestions.value = []
    showSuggestions.value = false
    return
  }
  debounce = setTimeout(fetchUserSuggestions, 300)
})

let debounce
async function fetchUserSuggestions() {
  try {
    const { data } = await axios.get(
      `http://localhost:8080/users/search?username=${encodeURIComponent(searchTerm.value)}`
    )
    const list = Array.isArray(data) ? data : data.users
    suggestions.value = list.slice(0, 3)
    showSuggestions.value = suggestions.value.length > 0
  } catch (e) {
    console.error('Erreur fetchUserSuggestions', e)
  }
}

function onInput() {
  showSuggestions.value = true
}

function handleBlur() {
  setTimeout(() => showSuggestions.value = false, 200)
}

function selectUser(user) {
  selectedUser.value = user
  searchTerm.value = user.username
  suggestions.value = []
  showSuggestions.value = false
}

async function onEnter() {
  if (!selectedUser.value) return
  await createChat()
}

async function createChat() {
  if (!selectedUser.value) return
  loadingChats.value = true
  const ok = await privateChatStore.createPrivateChat(
    currentUserId.value,
    selectedUser.value.id
  )
  if (ok) {
    await privateChatStore.fetchPrivateChats(currentUserId.value)
    // mettre à jour les pseudos
    const otherId = selectedUser.value.id
    userNames.value[otherId] = selectedUser.value.username
    searchTerm.value = ''
    selectedUser.value = null
  } else {
    alert('Erreur création chat')
  }
  loadingChats.value = false
}

async function selectChat(chat) {
  selectedChat.value = chat
  await loadMessages(chat.id)
}

async function deleteChat(chatId) {
  const ok = await privateChatStore.deletePrivateChat(currentUserId.value, chatId)
  if (ok) {
    await privateChatStore.fetchPrivateChats(currentUserId.value)
    if (selectedChat.value?.id === chatId) selectedChat.value = null
  } else {
    alert('Erreur suppression chat')
  }
}

async function loadMessages(chatId) {
  messages.value = []
  messages.value = [
    { id: 1, from: currentUserId.value, content: 'Demo message', ts: Date.now() - 60000 }
  ]
  await nextTick()
  messageContainer.value.scrollTop = messageContainer.value.scrollHeight
}

async function sendMessage() {
  if (!newMessage.value.trim() || !selectedChat.value) return
  messages.value.push({
    id: Date.now(),
    from: currentUserId.value,
    content: newMessage.value.trim(),
    ts: Date.now()
  })
  newMessage.value = ''
  await nextTick()
  messageContainer.value.scrollTop = messageContainer.value.scrollHeight
}

function formatTime(ts) {
  const d = new Date(ts)
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`
}

function getOtherPseudo(chat) {
  const otherId = chat.user_id_1 === currentUserId.value ? chat.user_id_2 : chat.user_id_1
  return userNames.value[otherId] || `User#${otherId}`
}

const messageContainer = ref(null)
</script>

<style scoped>
.private-home-container {
  display: flex;
  height: 100vh;
  background-color: #36393f;
  color: #dcddde;
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  padding: 20px;
  gap: 20px;
}
.card { background-color: #2f3136; border-radius: 6px; padding: 20px; box-shadow: 0 2px 5px rgba(0,0,0,0.3); }
.sidebar { width: 300px; display: flex; flex-direction: column; }
.card-title { margin-bottom: 1rem; font-size: 1.25rem; }
.chat-list { flex: 1; overflow-y: auto; list-style: none; margin:0; padding:0; }
.chat-entry { display: flex; align-items: center; justify-content: space-between; padding:10px; border-radius:4px; cursor:pointer; transition:background 0.2s; }
.chat-entry:hover { background:#393c43; }
.chat-entry.selected { background:#202225; }
.chat-item-content { background:none; border:none; color:inherit; text-align:left; flex:1; cursor:pointer; }
.btn-delete-chat { background:transparent; border:none; color:#f04747; cursor:pointer; padding:4px; }
.btn-delete-chat:hover { color:#ce3c3c; }
.new-chat-form { display:flex; margin-top:1rem; }
.create-server-input { flex:1; padding:8px; border:none; border-radius:4px 0 0 4px; background:#303338; color:#dcddde; width:92% }
.create-server-input:focus { outline:2px solid #5865F2; }
.autocomplete { position:relative; width:100%; }
.suggestions.above { position:absolute; bottom:100%; left:0; right:0; background:#2f3136; border:1px solid #393c43; border-radius:4px 4px 0 0; max-height:150px; overflow-y:auto; z-index:10; list-style:none; margin:0; padding:0; }
.suggestion-item { padding:8px; cursor:pointer; }
.suggestion-item:hover { background:#393c43; }
.chat-window { flex:1; display:flex; flex-direction:column; }
.chat-header { border-bottom:1px solid #393c43; margin-bottom:1rem; }
.messages { flex:1; overflow-y:auto; margin-bottom:1rem; }
.message { max-width:60%; margin-bottom:10px; padding:10px; border-radius:6px; background:#202225; }
.message.me { margin-left:auto; background:#5865F2; }
.message .ts { font-size:0.75rem; color:#72767d; margin-top:4px; }
.message-form { display:flex; gap:10px; }
.btn-primary { background:#5865F2; color:#fff; border:none; border-radius:0 4px 4px 0; padding:8px 16px; cursor:pointer; }
.btn-primary:hover { background:#4752c4; }
.chat-placeholder { flex:1; display:flex; align-items:center; justify-content:center; color:#72767d; font-style:italic; }
</style>
