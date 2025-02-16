<template>
  <div class="login-container">
    <div class="login-box">
      <h2>{{ isRegistering ? "Créer un compte" : "Connexion" }}</h2>
      <form @submit.prevent="handleSubmit">
        <input v-model="username" type="text" placeholder="Nom d'utilisateur" required />
        <input v-model="password" type="password" placeholder="Mot de passe" required />
        <button type="submit">{{ isRegistering ? "S'inscrire" : "Se connecter" }}</button>
      </form>
      <p @click="toggleMode">
        {{ isRegistering ? "Déjà un compte ? Se connecter" : "Pas encore de compte ? S'inscrire" }}
      </p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue';
import { useAuthStore } from '../store/auth';
import { useRouter } from 'vue-router';

const authStore = useAuthStore();
const router = useRouter();
const username = ref('');
const password = ref('');
const isRegistering = ref(false);

const toggleMode = () => {
  isRegistering.value = !isRegistering.value;
};

const handleSubmit = async () => {
  if (isRegistering.value) {
    const success = await authStore.register(username.value, password.value);
    if (success) {
      alert('Inscription réussie, connectez-vous.');
      isRegistering.value = false;
    } else {
      alert('Erreur lors de l\'inscription.');
    }
  } else {
    const success = await authStore.login(username.value, password.value);
    if (success) {
      router.push('/home');
    } else {
      alert('Identifiants incorrects.');
    }
  }
};
</script>

<style scoped>
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  height: 100vh;
  background: #23272a;
}

.login-box {
  background: #2c2f33;
  padding: 20px;
  border-radius: 8px;
  width: 300px;
  text-align: center;
  color: white;
}

.login-box h2 {
  margin-bottom: 15px;
}

input {
  width: 90%;
  padding: 10px;
  margin: 8px 0;
  border: none;
  border-radius: 5px;
  background: #40444b;
  color: white;
}

button {
  width: 100%;
  padding: 10px;
  margin-top: 10px;
  background: #5865f2;
  border: none;
  border-radius: 5px;
  color: white;
  font-weight: bold;
  cursor: pointer;
}

button:hover {
  background: #4752c4;
}

p {
  margin-top: 10px;
  color: #5865f2;
  cursor: pointer;
}

p:hover {
  text-decoration: underline;
}
</style>
