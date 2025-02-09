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