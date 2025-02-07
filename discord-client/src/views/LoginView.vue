<template>
    <div class="login">
      <h2>Connexion</h2>
      <input v-model="username" placeholder="Nom d'utilisateur" />
      <input v-model="password" type="password" placeholder="Mot de passe" />
      <button @click="handleLogin">Se connecter</button>
    </div>
  </template>
  
  <script setup>
  import { ref } from 'vue'
  import { login } from '../services/userService.js'
  import { useUserStore } from '../store/userStore.js'
  
  const username = ref('')
  const password = ref('')
  const userStore = useUserStore()
  
  const handleLogin = async () => {
    try {
      const userData = await login(username.value, password.value)
      userStore.setUser(userData)
      alert('Connexion réussie !')
    } catch (error) {
      alert('Erreur de connexion')
    }
  }
  </script>
  
  <style scoped>
  .login {
    display: flex;
    flex-direction: column;
    align-items: center;
    margin-top: 50px;
  }
  input {
    margin: 5px;
    padding: 8px;
  }
  button {
    margin-top: 10px;
    padding: 10px;
    background: #5865F2;
    color: white;
    border: none;
    cursor: pointer;
  }
  </style>
  