<template>
    <div class="register">
      <h2>Inscription</h2>
      <input v-model="username" placeholder="Nom d'utilisateur" />
      <input v-model="password" type="password" placeholder="Mot de passe" />
      <button @click="handleRegister">S'inscrire</button>
      <p v-if="error" class="error">{{ error }}</p>
    </div>
  </template>
  
  <script setup>
  import { ref } from 'vue';
  import { register } from '../services/userService';
  import { useUserStore } from '../store/userStore';
  import { useRouter } from 'vue-router'; 

  
  const username = ref('');
  const password = ref('');
  const error = ref('');
  const userStore = useUserStore();
  const router = useRouter(); 

  
  const handleRegister = async () => {
    try {
      const userData = await register(username.value, password.value);
      userStore.setUser(userData);
      localStorage.setItem('isUserConnected', 'true');
      alert('Inscription réussie !');
      router.push('/');
    } catch (err) {
      error.value = 'Erreur d\'inscription : Veuillez réessayer plus tard.';
    }
  };
  </script>
      
    <style scoped>
    .register {
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
  