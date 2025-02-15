<template>
    <div>
        <h1>Edit Server</h1>
        <form @submit.prevent="updateServer">
            <label>Name:</label>
            <input v-model="name" type="text" required />

            <label>Image URL:</label>
            <input v-model="img" type="text" required />

            <button type="submit">Update</button>
        </form>

        <form @submit.prevent="deleteServersubmit">
            <button type="submit">Delete Server</button>
        </form>
    </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getServerById, editServer, deleteServer } from '../services/serverService'

const route = useRoute()
const router = useRouter()

const name = ref('')
const img = ref('')

// Store the server data
const server = ref(null)

const getServer = async () => {
    try {
        const serverData = await getServerById(route.params.id)  // Fetch server by ID
        server.value = serverData  // Assign fetched server data to the reactive variable
        name.value = serverData.name // Pre-fill the form with current data
        img.value = serverData.img
    } catch (error) {
        console.error('Error fetching server', error)
    }
}

const updateServer = async () => {
    try {
        await editServer(route.params.id, name.value, img.value)
        router.push('/createServer') // Redirect to the server list after update
    } catch (error) {
        console.error('Error updating server', error)
    }
}

const deleteServersubmit = async () => {
    try {
        await deleteServer(route.params.id)
        router.push('/createServer') // Redirect to the server list after update
    } catch (error) {
        console.error('Error deleting server', error)
    }
}

onMounted(() => {
    getServer()  // Fetch the server data on component mount
})
</script>
