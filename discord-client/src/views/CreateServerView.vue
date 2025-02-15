<template>
    <div>
        <h1>Creer Server</h1>
        <label for="name">Name</label>
        <input type="text" v-model="name">
        
        <label for="img">Image</label>
        <input type="text" v-model="img">
        
        <button @click="handleServerCreation">Create</button>
        
    </div>
    <ListAllServers></ListAllServers>
    <SearchServer></SearchServer>
</template>

<script setup>
import ListAllServers from '../components/ListAllServers.vue'
import SearchServer from '../components/SearchServer.vue'
import { ref } from 'vue'
import { createServer } from '../services/serverService' 
const name = ref('')
const img = ref('')

const handleServerCreation = async () => {
    
    try {
        const serverData = await createServer(name.value, img.value)
        console.log('Server created');
        
    } catch (error) {   
        console.error('Error server creation !', error);
        
    }
}
</script>