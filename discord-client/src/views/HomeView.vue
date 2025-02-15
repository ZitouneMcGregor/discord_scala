<template>
  <div class="home">
    <h2>Bienvenue sur Discord</h2>
    <router-link to="/login">Connexion</router-link>
    <router-link to="/register">Inscription</router-link>
    <button @click="deconnecter">Déconnexion</button>
  </div>
</template>
<script setup>
const deconnecter = () => {
  localStorage.setItem('isUserConnected', 'false');
};
</script>
<style scoped>
.home {
  display: flex;
  flex-direction: column;
  align-items: center;
  margin-top: 50px;
}
</style>
