import axios from 'axios'

const API_URL = 'http://localhost:8080'

export const createServer = async (name, img) => {
    try {
        const response = await axios.post(`${API_URL}/server`, { name, img})
        return response.data
    } catch (error) {
        console.error('Server creation failed:', error);
        throw error
        
    }
}

export const getAllServers = async () => {
    try {
        const response = await axios.get(`${API_URL}/server`)
        return response.data
    } catch (error) {
        console.error('Fetch servers failed:', error);
        throw error;
        
    }
}

export const getServerById   = async (id) => {
    try {
        const response = await axios.get(`${API_URL}/server/${id}`)
        return response.data    
    } catch (error) {
        console.error('Fetch server failed:', error);
        throw error;
        
    }
}

export const searchServer = async (name) => {
    try {
        const response = await axios.get(`${API_URL}/server/search/${name}`)
        return response.data
    } catch (error) {
        console.error(`Error searching ${name} server `, error);
        return null;        
    }
}

export const editServer = async (id, name, img) => {
    try {
        console.log("Sending PUT request to:", `${API_URL}/server/${id}`);
console.log("Payload:", { name, img });
        const response = await axios.put(`${API_URL}/server/${id}`, {name, img})
        return response.data
    } catch (error) {
        console.error(`Error updating server ${id} ${name}`, error);
        throw error
        
    }
}

export const deleteServer = async (id) => {
    try {
        console.log("Sending DELETE request to:", `${API_URL}/server/${id}`);
        const response = await axios.delete(`${API_URL}/server/${id}`)
        return response.data
    } catch (error) {
        console.error(`Error deleting server ${id} ${name}`, error);
        throw error
        
    }
}