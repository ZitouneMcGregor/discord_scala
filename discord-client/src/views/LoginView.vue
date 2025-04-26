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
import { ref, computed } from 'vue';
import { useRouter } from 'vue-router';
import { useAuthStore } from '../store/auth';

const auth = useAuthStore();
const router = useRouter();

const username = ref('');
const password = ref('');
const isRegistering = ref(false);

const toggleMode = () => {
  isRegistering.value = !isRegistering.value;
  username.value = '';
  password.value = '';
};

const handleSubmit = async () => {
  if (!username.value || !password.value) return;

  if (isRegistering.value) {
    const res = await auth.register(username.value, password.value);
    if (res.success) {
      alert(res.message);
      toggleMode();
    } else {
      alert(`Erreur : ${res.message}`);
    }
  } else {
    const res = await auth.login(username.value, password.value);
    if (res.success) {
      // redirige vers /home si OK
      router.push('/home');
    } else {
      alert(res.message);
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
