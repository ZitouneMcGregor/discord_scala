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

      <div class="supp-user">
        <button class="btn-danger" @click="deleteAccount">
          Supprimer mon compte
      </button>
      </div>

    </div>
  </template>
  
  <script>
  import { ref } from 'vue'
  import { useAuthStore } from '../store/auth';
  import { useRouter } from 'vue-router';

  
  export default {
    setup() {
      const authStore = useAuthStore()
      const newUsername = ref('')
      const newPassword = ref('')
      const router = useRouter()
  
      async function updateProfile() {
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

      async function deleteAccount() {
        if (!confirm("Voulez-vous vraiment supprimer votre compte ? Cette action est irréversible.")) {
          return;
        }

        const userName = authStore.user.username;
        try {
          const success = await authStore.deleteAccount(userName);
          if (success) {
            alert("Compte supprimé avec succès !");
            authStore.logout();
            router.push('/home');
          } else {
            alert("Erreur lors de la suppression du compte.");
          }
        } catch (error) {
          alert("Erreur lors de la suppression du compte.");
        }
      }



    return {
      authStore,
      newUsername,
      newPassword,
      updateProfile,
      deleteAccount
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
  
  .profile-form, .supp-user {
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
    background-color: #fa0000;
    color: #fff;
    border: none;
    border-radius: 4px;
    padding: 7px 14px;
    cursor: pointer;
    transition: background-color 0.2s;
  }
  .btn-danger:hover {
    background-color: #c03502;
  }

  </style>
  