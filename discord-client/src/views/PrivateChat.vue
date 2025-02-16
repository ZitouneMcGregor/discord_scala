<template>
  <div class="private-chat-container">
    <h2>Vos chats privés</h2>

    <!-- Liste des chats existants -->
    <div v-if="privateChatStore.privateChats.length > 0" class="chat-list">
      <div
        v-for="chat in privateChatStore.privateChats"
        :key="chat.id"
        class="chat-item"
      >
        <!-- Par exemple, on affiche l'id ou user2, etc. -->
        <p>Chat #{{ chat.id }} avec user {{ otherUserId(chat) }}</p>
        <!-- Bouton pour “supprimer” ce chat côté user -->
        <button class="btn-danger" @click="deleteChat(chat.id)">
          Supprimer
        </button>
      </div>
    </div>
    <div v-else>
      <p>Aucun chat privé trouvé.</p>
    </div>

    <hr />

    <!-- Formulaire pour créer un nouveau chat privé -->
    <div class="create-chat-section">
      <h3>Créer un nouveau chat privé</h3>
      <div>
        <label>Entrez l'ID d'un utilisateur existant :</label>
        <input v-model="otherUserIdInput" placeholder="ID de l'autre user" />
      </div>
      <button class="btn-primary" @click="createChat">
        Créer le chat
      </button>
    </div>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue'
import { useAuthStore } from '../store/auth';
import { usePrivateChatStore } from '../store/privateChats';

export default {
  setup() {
    const authStore = useAuthStore()
    const privateChatStore = usePrivateChatStore()

    const currentUserId = authStore.user.id

    const otherUserIdInput = ref('')

    onMounted(async () => {
      await privateChatStore.fetchPrivateChats(currentUserId)
    })

    function otherUserId(chat) {
      if (chat.user_id_1 === currentUserId) {
        return chat.user_id_2
      } else {
        return chat.user_id_1
      }
    }

    async function createChat() {
      const otherId = parseInt(otherUserIdInput.value, 10)
      if (!otherId) return

      const success = await privateChatStore.createPrivateChat(
        currentUserId,
        otherId
      )
      if (success) {
        otherUserIdInput.value = ''
      } else {
        alert('Erreur lors de la création du chat privé')
      }
    }

    async function deleteChat(chatId) {
      await privateChatStore.deletePrivateChat(currentUserId, chatId)
    }

    return {
      authStore,
      privateChatStore,
      currentUserId,
      otherUserIdInput,
      otherUserId,
      createChat,
      deleteChat
    }
  }
}
</script>

<style scoped>
.private-chat-container {
  color: #dcddde;
  background-color: #36393f;
  min-height: 100vh;
  padding: 20px;
}

.chat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.chat-item {
  background-color: #2f3136;
  padding: 10px;
  border-radius: 4px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.create-chat-section {
  margin-top: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}

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
</style>
