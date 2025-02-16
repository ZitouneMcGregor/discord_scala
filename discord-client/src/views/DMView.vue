<template>
    <div class="dm-layout">
      <div class="dm-list-panel">
        <h2>Vos MP</h2>
        <ul>
          <li 
            v-for="chat in privateChatStore.privateChats" 
            :key="chat.chatId"
            @click="selectChat(chat)"
            :class="{ selected: selectedChat && selectedChat.chatId === chat.chatId }"
          >
            Conversation avec {{ chat.otherUserName }}
          </li>
        </ul>
      </div>
  
      <div class="dm-chat-panel">
        <PrivateChatView v-if="selectedChat" :chat="selectedChat" />
        <div v-else class="dm-chat-empty">
          <p>Sélectionnez un chat privé.</p>
        </div>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, onMounted } from 'vue'
  import { useRoute } from 'vue-router'
  import { useAuthStore } from '../store/auth'
  import { usePrivateChatStore } from '../store/privateChats'
  import PrivateChat from './PrivateChat.vue'
  
  const route = useRoute()
  const userId = route.params.userId
  
  const authStore = useAuthStore()
  const privateChatStore = usePrivateChatStore()
  
  const selectedChat = ref(null)
  
  onMounted(async () => {
    await privateChatStore.fetchPrivateChats(userId)
  })
  
  function selectChat(chat) {
    selectedChat.value = chat
  }
  </script>
  
  <style scoped>
  .dm-layout {
    display: flex;
    height: 100%;
  }
  
  .dm-list-panel {
    width: 240px;
    background-color: #2f3136;
    padding: 10px;
    overflow-y: auto;
  }
  
  .dm-list-panel ul {
    list-style: none;
    padding: 0;
  }
  
  .dm-list-panel li {
    padding: 6px;
    margin-bottom: 4px;
    border-radius: 4px;
    cursor: pointer;
  }
  .dm-list-panel li:hover {
    background-color: #393c43;
  }
  .dm-list-panel li.selected {
    background-color: #5865F2;
  }
  
  .dm-chat-panel {
    flex: 1;
    display: flex;
    flex-direction: column;
    background-color: #36393f;
  }
  
  .dm-chat-empty {
    flex: 1;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  </style>
  