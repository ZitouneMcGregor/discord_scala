import axios from 'axios'

const API_URL = 'http://localhost:8080'

export const login = async (username, password) => {
  try {
    const response = await axios.post(`${API_URL}/users`, { username, password })
    return response.data
  } catch (error) {
    console.error('Login failed:', error)
    throw error
  }
}
