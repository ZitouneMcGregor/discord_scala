<template>
  <div class="profile-container">
    <!-- Carte profil au centre -->
    <div class="profile-card">
      <h1>Mon profil</h1>
      <p class="user-info">Utilisateur actuel : {{ authStore.user?.username }}</p>

      <div class="profile-form">
        <label>Nouveau pseudo</label>
        <input v-model="newUsername" placeholder="Nouveau username" />

        <label>Nouveau mot de passe</label>
        <input v-model="newPassword" type="password" placeholder="Nouveau password" />

        <button class="btn-primary" @click="updateProfile">
          Enregistrer
        </button>
      </div>

      <hr class="separator" />

      <div class="supp-user">
        <button class="btn-danger" @click="deleteAccount">
          Supprimer mon compte
        </button>
      </div>
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
      const trimmedUsername = newUsername.value.trim()
      const trimmedPassword = newPassword.value.trim()

      const success = await authStore.updateUser(
        trimmedUsername,
        trimmedPassword
      )

      if (success) {
        alert('Profil mis à jour avec succès !');
        newUsername.value = '';
        newPassword.value = '';
      } else {
        alert("Erreur : nom d'utilisateur déjà pris ou problème réseau");
      }
    }

    async function deleteAccount() {
      if (!confirm("Voulez-vous vraiment supprimer votre compte ?")) {
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
/* Conteneur principal : centre verticalement et horizontalement */
.profile-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background-color: #36393f; /* Fond “Discord” */
  color: #dcddde;           /* Texte clair */
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
}

/* Carte centrale */
.profile-card {
  background-color: #2f3136; /* Bloc un peu plus clair */
  border-radius: 6px;
  padding: 30px;
  width: 400px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Titre */
.profile-card h1 {
  margin: 0 0 10px 0;
  text-align: center;
}

/* Petite info utilisateur */
.user-info {
  margin: 0;
  margin-bottom: 20px;
  text-align: center;
  color: #b9bbbe;
}

/* Formulaire principal */
.profile-form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Séparateur horizontal */
.separator {
  margin: 20px 0;
  border: 0;
  border-top: 1px solid #555;
}

/* Section “supp-user” */
.supp-user {
  display: flex;
  justify-content: center;
  margin-top: 10px;
}

/* Inputs */
label {
  font-weight: bold;
  margin-bottom: 4px;
  color: #fff;
}

input {
  padding: 8px;
  border: none;
  border-radius: 4px;
  background-color: #202225;
  color: #fff;
  outline: none;
}

/* Boutons principaux */
.btn-primary,
.btn-danger {
  border: none;
  border-radius: 4px;
  padding: 7px 14px;
  cursor: pointer;
  transition: background-color 0.2s;
  font-size: 14px;
}

/* Bouton principal (bleu) */
.btn-primary {
  background-color: #5865F2;
  color: #fff;
}
.btn-primary:hover {
  background-color: #4752c4;
}

/* Bouton danger (rouge) */
.btn-danger {
  background-color: #fa0000;
  color: #fff;
}
.btn-danger:hover {
  background-color: #c03502;
}
</style>
