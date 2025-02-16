<template>
    <div class="chat-container">
      <h2>Conversation avec {{ friendUsername }}</h2>
      <div class="messages">
        <p>📩 Affichage des messages privés ici...</p>
      </div>
  
      <div class="message-input">
        <input v-model="newMessage" placeholder="Écrire un message..." />
        <button @click="sendMessage">Envoyer</button>
      </div>
    </div>
  </template>
  
  <script setup>
  import { ref, computed } from 'vue';
  import { useRoute } from 'vue-router';
  
  const route = useRoute();
  const friendId = computed(() => route.params.friendId);
  const friendUsername = computed(() => `Utilisateur ${friendId.value}`);
  const newMessage = ref("");
  
  const sendMessage = () => {
    if (newMessage.value.trim()) {
      console.log("Message privé envoyé:", newMessage.value);
      newMessage.value = "";
    }
  };
  </script>
  
  <style scoped>
  .chat-container {
    background: #40444b;
    padding: 20px;
    flex: 1;
    color: white;
  }
  
  .messages {
    height: 400px;
    overflow-y: auto;
  }
  
  .message-input {
    display: flex;
    gap: 10px;
    margin-top: 20px;
  }
  
  input {
    flex: 1;
    padding: 10px;
    border-radius: 5px;
    border: none;
  }
  
  button {
    background: #5865f2;
    border: none;
    padding: 10px;
    color: white;
    cursor: pointer;
    border-radius: 5px;
  }
  </style>
  