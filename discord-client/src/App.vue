<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup>
</script>

<style>
/* Styles globaux */
body {
  margin: 0;
  font-family: 'Arial', sans-serif;
  background-color: #2c2f33;
  color: white;
}

#app {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 100vh;
}
</style>
