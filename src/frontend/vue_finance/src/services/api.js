import axios from 'axios';

// API-Basis-URL konfigurieren
export const API_URL = 'http://localhost:8080/api';

// Axios-Instanz mit Basis-URL erstellen
const ApiService = axios.create({
    baseURL: API_URL,
    headers: {
        'Content-Type': 'application/json',
    },
});

// Response Interceptor für fehlerhafte Anfragen
ApiService.interceptors.response.use(
    (response) => response,
    (error) => {
        console.error('API Fehler:', error);
        return Promise.reject(error);
    }
);

export default ApiService;