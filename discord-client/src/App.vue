<!-- src/App.vue -->
<template>
  <div id="app">
    <!-- Sidebar visible si on n'est PAS sur la page de login -->
    <Sidebar v-if="currentRouteName !== 'Login'" />

    <!-- Le contenu principal -->
    <div class="main-content">
      <router-view :key="$route.fullPath" />
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Sidebar from './views/Sidebar.vue';

const route = useRoute()
const currentRouteName = computed(() => route.name)
</script>
