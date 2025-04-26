import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080',
  auth: {
    username: 'foo',   // vos creds BasicAuth
    password: 'bar'
  },
  headers: {
    'Content-Type': 'application/json'
  }
});

export default api;
