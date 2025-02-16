<template>
    <div class="profile-container">
      <h1>Mon profil</h1>
      <p>Utilisateur actuel : {{ authStore.user?.username }}</p>
  
      <div class="profile-form">
        <label>Nouveau pseudo</label>
        <input v-model="newUsername" placeholder="Nouveau username" />
  
        <label>Nouveau mot de passe</label>
        <input v-model="newPassword" type="password" placeholder="Nouveau password" />
  
        <button class="btn-primary" @click="updateProfile">
          Enregistrer
        </button>
      </div>
    </div>
  </template>
  
  <script>
  import { ref } from 'vue'
  import { useAuthStore } from '../store/auth';
  
  export default {
    setup() {
      const authStore = useAuthStore()
      const newUsername = ref('')
      const newPassword = ref('')
  
      async function updateProfile() {
        // on appelle l'action updateUser
        const userName = authStore.user.username
        const success = await authStore.updateUser(userName, {
          newUsername: newUsername.value.trim(),
          newPassword: newPassword.value.trim()
        });
        if (success) {
          alert('Profil mis à jour avec succès !');
          newUsername.value = '';
          newPassword.value = '';
        } else {
          alert('Erreur lors de la mise à jour du profil');
        }
      }
  
      return {
        authStore,
        newUsername,
        newPassword,
        updateProfile
      }
    }
  }
  </script>
  
  <style scoped>
  .profile-container {
    padding: 20px;
    color: #dcddde;
    background-color: #36393f;
    min-height: 100vh;
  }
  
  .profile-form {
    display: flex;
    flex-direction: column;
    gap: 10px;
    max-width: 300px;
    margin-top: 20px;
  }
  
  label {
    font-weight: bold;
    margin-bottom: 4px;
  }
  
  input {
    padding: 8px;
    border: none;
    border-radius: 4px;
    background-color: #2f3136;
    color: #fff;
  }
  
  /* Bouton principal */
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
  </style>
  