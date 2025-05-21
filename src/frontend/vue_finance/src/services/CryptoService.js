import axios from 'axios';

const API_URL = 'http://localhost:8081/api/crypto';

export default {
    getWatchlist() {
        return axios.get(`${API_URL}/watchlist`);
    },
    addToWatchlist(crypto) {
        return axios.post(`${API_URL}/watchlist`, crypto);
    },
    removeFromWatchlist(id) {
        return axios.delete(`${API_URL}/watchlist/${id}`);
    },
    getCryptoPrice(symbol) {
        return axios.get(`${API_URL}/price/${symbol}`);
    },
    getCryptoHistory(symbol) {
        return axios.get(`${API_URL}/history/${symbol}`);
    }
};