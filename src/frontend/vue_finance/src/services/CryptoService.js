import axios from 'axios';

const API_URL = 'http://localhost:8081/api/assets';

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
        return axios.get(`${API_URL}/price/${symbol}?type=crypto&market=USD`);
    },
    getCryptoHistory(symbol) {
        return axios.get(`${API_URL}/history/${symbol}?type=crypto&market=USD`);
    },
    // Neue Methoden basierend auf dem AssetController
    getMultipleCryptoPrices(symbols) {
        return axios.post(`${API_URL}/prices`, {
            symbols: symbols,
            type: 'crypto',
            market: 'USD'
        });
    },
    getCryptoIntradayData(symbol, interval = '5min') {
        return axios.get(`${API_URL}/intraday/${symbol}?type=crypto&market=USD&interval=${interval}`);
    },
    getCryptoChartData(symbol, period = 'daily') {
        return axios.get(`${API_URL}/chart/${symbol}?type=crypto&market=USD&period=${period}`);
    }
};