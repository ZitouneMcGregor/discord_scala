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
        <button type="submit" class="btn-primary" :disabled="!selectedUser">+</button>
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
          :key="msg.ts + msg.content"
          :class="['message', { me: Number(msg.from.id) === currentUserId }]"
        >
          <div class="author">{{ msg.from.username }}</div>
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

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from 'vue';
import { useAuthStore } from '../store/auth';
import { usePrivateChatStore } from '../store/privateChats';
import api from '../plugins/axios';
import axios from 'axios';

/* —— WS CONFIG -------------------------------------------------------- */
const WS_URL   = 'ws://localhost:8082/subscriptions';
const BASIC_USER = 'foo';
const BASIC_PASS = 'bar';

/* —— STORES ----------------------------------------------------------- */
const authStore         = useAuthStore();
const privateChatStore  = usePrivateChatStore();

/* —— STATE ------------------------------------------------------------ */
const currentUserId   = computed(() => authStore.user.id);
const loadingChats    = ref(true);
const searchTerm      = ref('');
const suggestions     = ref<any[]>([]);
const showSuggestions = ref(false);
const selectedUser    = ref<any | null>(null);
const selectedChat    = ref<any | null>(null);
const messages        = ref<any[]>([]);
const newMessage      = ref('');
const chats           = computed(() => privateChatStore.privateChats);
const userNames       = ref<Record<number,string>>({});
const messageContainer = ref<HTMLElement|null>(null);

/* —— WEBSOCKET -------------------------------------------------------- */
let ws: WebSocket | null = null;
const subscribed = new Set<number>();

function wsConnect() {
  ws = new WebSocket(WS_URL);
  ws.addEventListener('open', () => {
    console.log('[WS] connected');
    if (selectedChat.value) wsSubscribe(selectedChat.value.id);
  });
  ws.addEventListener('message', wsHandleMessage);
  ws.addEventListener('close', () => subscribed.clear());
  ws.addEventListener('error', e => console.error('[WS] error', e));
}

function wsSubscribe(chatId:number) {
  if (!ws || ws.readyState !== WebSocket.OPEN) return;
  if (subscribed.has(chatId)) return;
  ws.send(`pc${chatId}`);
  subscribed.add(chatId);
}

async function wsHandleMessage(e:MessageEvent) {
  let text:string;
  if (typeof e.data === 'string') text = e.data;
  else if (e.data instanceof Blob) text = await e.data.text();
  else return;

  if (!text.trim().startsWith('{')) return;

  try {
    const msg = JSON.parse(text);
    const expected = `pc${selectedChat.value?.id}`;
    if (msg.metadata?.chatId !== expected) return;

    messages.value.push({
      id:    expected,
      from:  { id: msg.metadata.fromId, username: msg.metadata.fromUsername },
      content: msg.content,
      ts:    msg.timestamp ? Date.parse(msg.timestamp) : Date.now()
    });

    await nextTick();
    messageContainer.value?.scrollTo({ top: messageContainer.value.scrollHeight });
  } catch (err) {
    console.error('[WS] parse error', err);
  }
}

/* —— LIFECYCLE -------------------------------------------------------- */
onMounted(async () => {
  wsConnect();
  await privateChatStore.fetchPrivateChats(currentUserId.value);

  const ids = [...new Set(
    privateChatStore.privateChats.map(c =>
      c.user_id_1 === currentUserId.value ? c.user_id_2 : c.user_id_1
    ))];

  await Promise.all(ids.map(async id => {
    try {
      const { data } = await api.get(`/users/id/${id}`);
      if (data) userNames.value[id] = data.username;
    } catch {}
  }));

  loadingChats.value = false;
});

/* —— WATCH ------------------------------------------------------------ */
watch(selectedChat, chat => {
  if (chat) wsSubscribe(chat.id);
});

/* —— AUTOCOMPLETE ----------------------------------------------------- */
let debounce:any;
watch(searchTerm, val => {
  selectedUser.value = null;
  clearTimeout(debounce);
  if (val.trim().length < 2) {
    suggestions.value = [];
    showSuggestions.value = false;
    return;
  }
  debounce = setTimeout(fetchUserSuggestions, 300);
});

async function fetchUserSuggestions() {
  try {
    const { data } = await api.get(`/users/search?username=${encodeURIComponent(searchTerm.value)}`);
    const list = Array.isArray(data) ? data : data.users;
    suggestions.value = list.slice(0,3);
    showSuggestions.value = suggestions.value.length > 0;
  } catch (e) {
    console.error('Erreur fetchUserSuggestions', e);
  }
}
function onInput()             { showSuggestions.value = true; }
function handleBlur()          { setTimeout(()=>showSuggestions.value=false, 200); }
function selectUser(user:any)  {
  selectedUser.value = user;
  searchTerm.value   = user.username;
  suggestions.value  = [];
  showSuggestions.value = false;
}
async function onEnter() {
  if (selectedUser.value) await createChat();
}

/* —— CRUD CHATS ------------------------------------------------------- */
async function createChat() {
  if (!selectedUser.value) return;
  loadingChats.value = true;

  const ok = await privateChatStore.createPrivateChat(currentUserId.value, selectedUser.value.id);
  if (ok) {
    await privateChatStore.fetchPrivateChats(currentUserId.value);
    userNames.value[selectedUser.value.id] = selectedUser.value.username;
    searchTerm.value = '';
    selectedUser.value = null;
  } else {
    alert('Erreur création chat');
  }
  loadingChats.value = false;
}

async function selectChat(chat:any) {
  selectedChat.value = chat;
  await loadMessages(chat.id);
}

async function deleteChat(chatId:number) {
  const ok = await privateChatStore.deletePrivateChat(currentUserId.value, chatId);
  if (ok) {
    await privateChatStore.fetchPrivateChats(currentUserId.value);
    if (selectedChat.value?.id === chatId) selectedChat.value = null;
  } else {
    alert('Erreur suppression chat');
  }
}

/* —— MESSAGES --------------------------------------------------------- */
async function loadMessages(chatId:number) {
  try {
    const idStr = `pc${chatId}`;
    const { data } = await axios.get(`http://localhost:8083/privateChat/${idStr}/messages`, {
      auth: { username: BASIC_USER, password: BASIC_PASS }
    });

    if (Array.isArray(data.messages)) {
      messages.value = data.messages.map((m:any)=>({
        id: idStr,
        from:{ id:m.metadata.fromId, username:m.metadata.fromUsername },
        content:m.content,
        ts: new Date(m.timestamp).getTime()
      }));
    } else {
      messages.value = [];
    }

    await nextTick();
    messageContainer.value?.scrollTo({ top: messageContainer.value.scrollHeight });
  } catch (err) {
    console.error('Erreur chargement messages', err);
  }
}

async function sendMessage() {
  if (!newMessage.value.trim() || !selectedChat.value) return;

  const chatIdStr = `pc${selectedChat.value.id}`;
  const payload = {
    id: chatIdStr,
    content: newMessage.value.trim(),
    metadata:{
      chatId: chatIdStr,
      fromId: currentUserId.value.toString(),
      fromUsername: authStore.user.username
    }
  };

  try {
    await axios.post('http://localhost:8081/message', payload, {
      auth:{ username:BASIC_USER, password:BASIC_PASS }
    });
    newMessage.value = '';      // pas de push optimiste : on attend le WS
  } catch (err) {
    console.error('Erreur envoi message', err);
    alert('Erreur envoi message');
  }
}

/* —— HELPERS ---------------------------------------------------------- */
function formatTime(ts:number) {
  const d = new Date(ts);
  return `${d.getHours().toString().padStart(2,'0')}:${d.getMinutes().toString().padStart(2,'0')}`;
}
function getOtherPseudo(chat:any) {
  const otherId = chat.user_id_1 === currentUserId.value ? chat.user_id_2 : chat.user_id_1;
  return userNames.value[otherId] || `User#${otherId}`;
}
</script>

<style scoped>
/* ------ Layout général ------------------------------------------------ */
.private-home-container {
  display:flex;
  height:100vh;
  background:#36393f;
  color:#dcddde;
  font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;
  padding:20px;
  gap:20px;
}
.card{ background:#2f3136; border-radius:6px; padding:20px; box-shadow:0 2px 5px rgba(0,0,0,0.3); }

/* ------ Sidebar ------------------------------------------------------- */
.sidebar{ width:300px; display:flex; flex-direction:column; }
.card-title{ margin-bottom:1rem; font-size:1.25rem; }

.chat-list{ flex:1; overflow-y:auto; list-style:none; margin:0; padding:0; }
.chat-entry{ display:flex; align-items:center; justify-content:space-between; padding:10px; border-radius:4px; cursor:pointer; transition:background .2s; }
.chat-entry:hover{ background:#393c43; }
.chat-entry.selected{ background:#202225; }

.chat-item-content{ background:none; border:none; color:inherit; text-align:left; flex:1; cursor:pointer; }
.btn-delete-chat{ background:transparent; border:none; color:#f04747; cursor:pointer; padding:4px; }
.btn-delete-chat:hover{ color:#ce3c3c; }

/* ------ Création / auto-complétion ----------------------------------- */
.new-chat-form{ display:flex; margin-top:1rem; }
.create-server-input{ flex:1; padding:8px; border:none; border-radius:4px 0 0 4px; background:#303338; color:#dcddde; width:92%; }
.create-server-input:focus{ outline:2px solid #5865F2; }

.autocomplete{ position:relative; width:100%; }
.suggestions.above{
  position:absolute; bottom:100%; left:0; right:0;
  background:#2f3136; border:1px solid #393c43; border-radius:4px 4px 0 0;
  max-height:150px; overflow-y:auto; z-index:10; list-style:none; margin:0; padding:0;
}
.suggestion-item{ padding:8px; cursor:pointer; }
.suggestion-item:hover{ background:#393c43; }

/* ------ Fenêtre de chat ---------------------------------------------- */
.chat-window{ flex:1; display:flex; flex-direction:column; }
.chat-header{ border-bottom:1px solid #393c43; margin-bottom:1rem; }

.messages{ flex:1; overflow-y:auto; margin-bottom:1rem; }
.message{
  max-width:60%;
  margin-bottom:10px;
  padding:10px;
  border-radius:6px;
  background:#202225;
  text-align:left;
}
.message.me{
  margin-left:auto;
  background:#5865F2;
  color:#fff;
  text-align:right;
}

.author{ font-weight:bold; margin-bottom:4px; }
.message .ts{ font-size:.75rem; color:#ffffff; margin-top:4px; }

.message-form{ display:flex; gap:10px; }
.btn-primary{
  background:#5865F2; color:#fff;
  border:none; border-radius:0 4px 4px 0;
  padding:8px 16px; cursor:pointer;
}
.btn-primary:hover{ background:#4752c4; }

.chat-placeholder{
  flex:1; display:flex; align-items:center; justify-content:center;
  color:#72767d; font-style:italic;
}
</style>
