import axios from 'axios';

const API_URL = 'http://localhost:8080';

export const login = async (username, password) => {
  try {
    const response = await axios.get(`${API_URL}/user/${username}`);

    const user = response.data;

    if (user && user.password === password) {
      return user;
    } else {
      throw new Error('ERROR login / mdp');
    }
  } catch (error) {
    console.error('Login failed:', error);
    throw error;
  }
};

export const register = async (username, password) => {
  try {
    const user = { username, password };
    const response = await axios.post(`${API_URL}/users`, user);
    console.log("User registered:", response.data);
    return response.data;
  } catch (error) {
    console.error('Registration failed:', error);
    throw error;
  }
};
