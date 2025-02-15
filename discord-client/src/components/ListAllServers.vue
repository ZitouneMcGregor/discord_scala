<template>
    <div>
        <h1>All Servers</h1>
        <ul>
            <li v-for="server in servers" :key="server.id">
                <router-link :to="`/edit-server/${server.id}`">
                    {{ server.name }} - <img :src="server.img" alt="Server Image" width="50" />
                </router-link>
            </li>
        </ul>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getAllServers } from '../services/serverService' 

const servers = ref([])

const fetchServers = async () => {
    try {
        const serverData = await getAllServers()
        servers.value = serverData // Stocke les serveurs dans la variable réactive
    } catch (error) {   
        console.error('Error fetching servers', error)
    }
}

// Exécuter la récupération des serveurs au montage du composant
onMounted(() => {
    fetchServers()
})
</script>
