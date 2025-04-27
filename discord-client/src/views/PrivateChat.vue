<template>
  <div class="private-home-container">
    <!-- ---------------- Sidebar : listes des DM ---------------- -->
    <aside class="card sidebar">
      <h3 class="card-title">Chats Privés</h3>

      <div v-if="loading" class="empty">Chargement…</div>

      <ul v-else class="chat-list">
        <li
          v-for="chat in chats"
          :key="chat.id"
          :class="['chat-entry', { selected: pcStore.selectedChat && pcStore.selectedChat.id === chat.id }]"
        >
          <button class="chat-item-content" @click="pcStore.selectChat(chat)">
            {{ otherPseudo(chat) }}
          </button>
          <button class="btn-delete-chat" @click.stop="del(chat.id)">🗑️</button>
        </li>
      </ul>

      <!-- ------- Création d'un nouveau DM (autocomplete) ------- -->
      <form @submit.prevent="create" class="new-chat-form">
        <div class="autocomplete">
          <input
            v-model="search"
            type="text"
            placeholder="Chercher un utilisateur…"
            class="create-server-input"
            @focus="showSug = true"
            @blur="() => setTimeout(()=>showSug=false,200)"
            @input="showSug = true"
          />
          <ul v-if="showSug && sug.length" class="suggestions above">
            <li v-for="u in sug" :key="u.id" class="suggestion-item" @mousedown.prevent="pick(u)">
              {{ u.username }}
            </li>
          </ul>
        </div>
        <button class="btn-primary" :disabled="!picked">+</button>
      </form>
    </aside>

    <!-- ---------------- Chat window ---------------- -->
    <main class="card chat-window" v-if="pcStore.selectedChat">
      <header class="chat-header"><h4>Chat avec {{ otherPseudo(pcStore.selectedChat) }}</h4></header>

      <div ref="msgArea" class="messages">
        <div
          v-for="m in msgs"
          :key="m.ts + m.content"
          :class="['message', { me: Number(m.metadata.fromId) === meId }]"
        >
          <div class="author">{{ m.metadata.fromUsername }}</div>
          <div class="content">{{ m.content }}</div>
          <div class="ts">{{ fmt(m.ts) }}</div>
        </div>
      </div>

      <form class="message-form" @submit.prevent="send">
        <input v-model="draft" class="create-server-input" placeholder="Entrez votre message…" />
        <button class="btn-primary">Envoyer</button>
      </form>
    </main>

    <div class="card chat-placeholder" v-else>
      <p>Sélectionne un chat à gauche pour démarrer la conversation.</p>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, nextTick } from 'vue'
import { useAuthStore }        from '../store/auth'
import { usePrivateChatStore } from '../store/privateChats'
import api from '../plugins/axios'
import axios from 'axios'

const auth  = useAuthStore()
const pcStore = usePrivateChatStore()
const meId  = computed(() => auth.user.id)

/* ---------------- local state ---------------- */
const loading = ref(true)
const search  = ref('')
const sug     = ref([])
const showSug = ref(false)
const picked  = ref(null)
const draft   = ref('')
const names   = ref({})
const msgArea = ref(null)

/* ---------------- derived ---------------- */
const chats = computed(() => pcStore.privateChats)
const msgs  = computed(() => pcStore.currentMessages)

/* ---------------- lifecycle ---------------- */
onMounted(async () => {
  await pcStore.fetchPrivateChats(meId.value)

  const ids = [...new Set(chats.value.flatMap(c => [c.user_id_1, c.user_id_2]))]
  await Promise.all(ids.map(async id => {
    try { const { data } = await api.get(`/users/id/${id}`); names.value[id]=data.username }
    catch {}
  }))
  loading.value = false
})

/* auto scroll */
watch(msgs, () => nextTick(()=>{ msgArea.value && (msgArea.value.scrollTop = msgArea.value.scrollHeight) }))

/* ---------------- autocomplete ---------------- */
watch(search, async v => {
  picked.value = null
  if(v.trim().length<2){ sug.value=[]; return }
  const { data } = await api.get(`/users/search?username=${encodeURIComponent(v)}`)
  sug.value = (Array.isArray(data)?data:data.users).slice(0,3)
})
function pick(u){ picked.value=u; search.value=u.username; sug.value=[]; showSug.value=false }

/* ---------------- actions ---------------- */
async function create(){
  if(!picked.value) return
  await pcStore.createPrivateChat(meId.value, picked.value.id)
  names.value[picked.value.id] = picked.value.username
  search.value=''; picked.value=null
}

function del(id){ pcStore.deletePrivateChat(meId.value,id) }

async function send(){
  if(!draft.value.trim()||!pcStore.selectedChat) return
  await axios.post('http://localhost:8081/message', {
    id:`pc${pcStore.selectedChat.id}`,
    content:draft.value.trim(),
    metadata:{ chatId:`pc${pcStore.selectedChat.id}`, fromId:meId.value.toString(), fromUsername:auth.user.username }
  }, { auth:{ username:'foo', password:'bar' } })
  draft.value=''
}

/* ---------------- helpers ---------------- */
function otherPseudo(chat){
  const other = chat.user_id_1===meId.value? chat.user_id_2 : chat.user_id_1
  return names.value[other] || `User#${other}`
}
function fmt(ts){ const d=new Date(ts); return d.toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'}) }
</script>

<style scoped>
.private-home-container{display:flex;height:100vh;background:#36393f;color:#dcddde;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;padding:20px;gap:20px}
.card{background:#2f3136;border-radius:6px;padding:20px;box-shadow:0 2px 5px rgba(0,0,0,.3)}
.sidebar{width:300px;display:flex;flex-direction:column}
.card-title{margin-bottom:1rem;font-size:1.25rem}
.chat-list{flex:1;overflow-y:auto;list-style:none;margin:0;padding:0}
.chat-entry{display:flex;align-items:center;justify-content:space-between;padding:10px;border-radius:4px;cursor:pointer;transition:background .2s}
.chat-entry:hover{background:#393c43}.chat-entry.selected{background:#202225}
.chat-item-content{background:none;border:none;color:inherit;text-align:left;flex:1;cursor:pointer}
.btn-delete-chat{background:transparent;border:none;color:#f04747;cursor:pointer;padding:4px}.btn-delete-chat:hover{color:#ce3c3c}
.new-chat-form{display:flex;margin-top:1rem}.create-server-input{flex:1;padding:8px;border:none;border-radius:4px 0 0 4px;background:#303338;color:#dcddde;width:92%}.create-server-input:focus{outline:2px solid #5865f2}
.autocomplete{position:relative;width:100%}.suggestions.above{position:absolute;bottom:100%;left:0;right:0;background:#2f3136;border:1px solid #393c43;border-radius:4px 4px 0 0;max-height:150px;overflow-y:auto;z-index:10;list-style:none;margin:0;padding:0}.suggestion-item{padding:8px;cursor:pointer}.suggestion-item:hover{background:#393c43}
.chat-window{flex:1;display:flex;flex-direction:column}.chat-header{border-bottom:1px solid #393c43;margin-bottom:1rem}
.messages{flex:1;overflow-y:auto;margin-bottom:1rem}.message{max-width:60%;margin-bottom:10px;padding:10px;border-radius:6px;background:#202225;text-align:left}.message.me{margin-left:auto;background:#5865f2;color:#fff;text-align:right}
.author{font-weight:bold;margin-bottom:4px}.message .ts{font-size:.75rem;color:#ffffff;margin-top:4px}
.message-form{display:flex;gap:10px}.btn-primary{background:#5865f2;color:#fff;border:none;border-radius:0 4px 4px 0;padding:8px 16px;cursor:pointer}.btn-primary:hover{background:#4752c4}
.chat-placeholder{flex:1;display:flex;align-items:center;justify-content:center;color:#72767d;font-style:italic}

/* ---------------- bulle de message ---------------- */
.message {
  max-width: 70%;                  /* un peu plus large (ou 60%)        */
  padding: 10px 14px;
  margin-bottom: 10px;
  border-radius: 10px;
  background: #202225;
  line-height: 1.4;
  font-size: 0.95rem;

  /* --- nouvelles protections pour le long texte --- */
  overflow-wrap: anywhere;         /* force la coupure des mots trop longs */
  word-break: break-word;          /* fallback vieux navigateurs            */
  white-space: pre-wrap;           /* respecte les retours à la ligne \n    */
}

.message.me {
  margin-left: auto;
  background: #4b5ef8;             /* un bleu un peu moins saturé         */
  color: #fff;
  box-shadow: 0 2px 4px rgba(0,0,0,.25);
}

.message .author {
  font-weight: 600;
  margin-bottom: 4px;
}

.message .ts {
  font-size: 0.75rem;
  text-align: right;
  margin-top: 6px;
  opacity: .7;
}

/* ---------------- zone scroll ------------------ */
.messages {
  flex: 1;
  overflow-y: auto;
  margin-bottom: 1rem;
  padding-right: 4px;              /* évite que le texte colle à la barre */
}

</style>
