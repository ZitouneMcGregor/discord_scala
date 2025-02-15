<template>
    <div>
        <h1>Search Server</h1>
        
        <!-- 🔎 Input pour la recherche -->
        <input v-model="searchQuery" placeholder="Enter server name" @keyup.enter="handleSearch" />

        <button @click="handleSearch">Search</button>

        <!-- 📌 Résultat de la recherche -->
        <div v-if="searchedServer">
            <h2>Server Found</h2>
            <p><strong>Id:</strong> {{ searchedServer.id }}</p>

            <p><strong>Name:</strong> {{ searchedServer.name }}</p>
            <p><strong>Img:</strong> {{ searchedServer.img }}</p>

        </div>

        <p v-if="searchError" class="error">{{ searchError }}</p>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import { searchServer } from '../services/serverService'

const searchQuery = ref('')
const searchedServer = ref(null)
const searchError = ref(null)

const handleSearch = async () => {
    searchError.value = null // Reset l'erreur avant chaque recherche
    searchedServer.value = null

    if (!searchQuery.value.trim()) {
        searchError.value = "Please enter a server name."
        return
    }

    try {
        const result = await searchServer(searchQuery.value)
        if (result) {
            searchedServer.value = result
        } else {
            searchError.value = `No server found with name "${searchQuery.value}".`
        }
    } catch (error) {
        searchError.value = "An error occurred while searching for the server."
    }
}
</script>

<style scoped>
.error {
    color: red;
    font-weight: bold;
}
</style>
