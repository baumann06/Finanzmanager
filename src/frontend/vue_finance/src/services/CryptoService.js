import axios from 'axios';

const API_URL = 'http://localhost:8081/api/assets';

export default {
    // ========== WATCHLIST MANAGEMENT ==========

    getWatchlist() {
        return axios.get(`${API_URL}/watchlist`);
    },

    addToWatchlist(asset) {
        return axios.post(`${API_URL}/watchlist`, asset);
    },

    removeFromWatchlist(id) {
        return axios.delete(`${API_URL}/watchlist/${id}`);
    },

    // ========== CURRENT PRICE DATA ==========

    /**
     * Holt aktuelle Preisdaten mit Change-Informationen für Krypto
     */
    getCryptoPrice(symbol, market = 'USD') {
        return axios.get(`${API_URL}/price/${symbol}?type=crypto&market=${market}`);
    },

    /**
     * Holt aktuelle Preisdaten mit Change-Informationen für Aktien
     */
    getStockPrice(symbol) {
        return axios.get(`${API_URL}/price/${symbol}?type=stock`);
    },

    /**
     * Generische Methode für beide Asset-Typen
     */
    getAssetPrice(symbol, type, market = 'USD') {
        const params = new URLSearchParams({
            type: type,
            ...(type === 'crypto' && { market: market })
        });
        return axios.get(`${API_URL}/price/${symbol}?${params}`);
    },

    // ========== HISTORICAL DATA ==========

    /**
     * Holt historische Krypto-Daten (täglich)
     */
    getCryptoHistory(symbol, market = 'USD') {
        return axios.get(`${API_URL}/history/${symbol}?type=crypto&market=${market}&period=daily`);
    },

    /**
     * Holt historische Aktien-Daten (täglich)
     */
    getStockHistory(symbol) {
        return axios.get(`${API_URL}/history/${symbol}?type=stock&period=daily`);
    },

    /**
     * Generische Methode für historische Daten
     */
    getAssetHistory(symbol, type, market = 'USD', period = 'daily') {
        const params = new URLSearchParams({
            type: type,
            period: period,
            ...(type === 'crypto' && { market: market })
        });
        return axios.get(`${API_URL}/history/${symbol}?${params}`);
    },

    // ========== INTRADAY DATA ==========

    /**
     * Holt Krypto-Intraday-Daten (stündlich)
     */
    getCryptoIntradayData(symbol, market = 'USD') {
        return axios.get(`${API_URL}/intraday/${symbol}?type=crypto&market=${market}`);
    },

    /**
     * Holt Aktien-Intraday-Daten mit konfigurierbarem Intervall
     */
    getStockIntradayData(symbol, interval = '5min') {
        return axios.get(`${API_URL}/intraday/${symbol}?type=stock&interval=${interval}`);
    },

    /**
     * Generische Methode für Intraday-Daten
     */
    getAssetIntradayData(symbol, type, market = 'USD', interval = '5min') {
        const params = new URLSearchParams({
            type: type,
            ...(type === 'crypto' && { market: market }),
            ...(type === 'stock' && { interval: interval })
        });
        return axios.get(`${API_URL}/intraday/${symbol}?${params}`);
    },

    // ========== CHART DATA ==========

    /**
     * Holt formatierte Chart-Daten für Krypto
     */
    getCryptoChartData(symbol, market = 'USD', period = 'daily') {
        return axios.get(`${API_URL}/chart/${symbol}?type=crypto&market=${market}&period=${period}`);
    },

    /**
     * Holt formatierte Chart-Daten für Aktien
     */
    getStockChartData(symbol, period = 'daily', interval = null) {
        const params = new URLSearchParams({
            type: 'stock',
            period: period,
            ...(interval && { interval: interval })
        });
        return axios.get(`${API_URL}/chart/${symbol}?${params}`);
    },

    /**
     * Generische Chart-Daten Methode
     */
    getAssetChartData(symbol, type, market = 'USD', period = 'daily', interval = null) {
        const params = new URLSearchParams({
            type: type,
            period: period,
            ...(type === 'crypto' && { market: market }),
            ...(type === 'stock' && interval && { interval: interval })
        });
        return axios.get(`${API_URL}/chart/${symbol}?${params}`);
    },

    // ========== MULTIPLE ASSETS ==========

    /**
     * Holt mehrere Krypto-Preise gleichzeitig
     */
    getMultipleCryptoPrices(symbols, market = 'USD') {
        return axios.post(`${API_URL}/prices`, {
            symbols: Array.isArray(symbols) ? symbols : [symbols],
            type: 'crypto',
            market: market
        });
    },

    /**
     * Holt mehrere Aktien-Preise gleichzeitig
     */
    getMultipleStockPrices(symbols) {
        return axios.post(`${API_URL}/prices`, {
            symbols: Array.isArray(symbols) ? symbols : [symbols],
            type: 'stock'
        });
    },

    /**
     * Generische Methode für mehrere Assets
     */
    getMultipleAssetPrices(symbols, type, market = 'USD') {
        return axios.post(`${API_URL}/prices`, {
            symbols: Array.isArray(symbols) ? symbols : [symbols],
            type: type,
            ...(type === 'crypto' && { market: market })
        });
    },

    // ========== UTILITY METHODS ==========

    /**
     * Validiert Asset-Symbol vor API-Aufruf
     */
    isValidSymbol(symbol, type) {
        if (!symbol || typeof symbol !== 'string' || symbol.trim().length === 0) {
            return false;
        }

        const trimmedSymbol = symbol.trim();

        if (type === 'crypto') {
            return trimmedSymbol.length >= 2 && trimmedSymbol.length <= 20;
        } else if (type === 'stock') {
            return trimmedSymbol.length >= 1 && trimmedSymbol.length <= 10;
        }

        return false;
    },

    /**
     * Formatiert Symbol für API-Aufruf
     */
    formatSymbol(symbol, type) {
        if (!symbol) return '';

        const formatted = symbol.trim();
        return type === 'stock' ? formatted.toUpperCase() : formatted;
    },

    /**
     * Behandelt API-Fehler einheitlich
     */
    handleApiError(error) {
        if (error.response) {
            // Server hat mit Fehlercode geantwortet
            const status = error.response.status;
            const data = error.response.data;

            switch (status) {
                case 400:
                    throw new Error(data?.error || 'Ungültige Anfrage');
                case 404:
                    throw new Error(data?.error || 'Asset nicht gefunden');
                case 429:
                    throw new Error('API-Limit erreicht. Bitte versuchen Sie es später erneut.');
                case 500:
                    throw new Error(data?.error || 'Serverfehler');
                default:
                    throw new Error(data?.error || `HTTP ${status}: ${error.message}`);
            }
        } else if (error.request) {
            // Anfrage wurde gesendet, aber keine Antwort erhalten
            throw new Error('Keine Verbindung zum Server möglich');
        } else {
            // Fehler beim Erstellen der Anfrage
            throw new Error(error.message || 'Unbekannter Fehler');
        }
    },

    // ========== WRAPPER METHODS WITH ERROR HANDLING ==========

    /**
     * Sichere Wrapper-Methoden mit Fehlerbehandlung
     */
    async safeGetCryptoPrice(symbol, market = 'USD') {
        try {
            if (!this.isValidSymbol(symbol, 'crypto')) {
                throw new Error('Ungültiges Krypto-Symbol');
            }
            const formattedSymbol = this.formatSymbol(symbol, 'crypto');
            return await this.getCryptoPrice(formattedSymbol, market);
        } catch (error) {
            this.handleApiError(error);
        }
    },

    async safeGetStockPrice(symbol) {
        try {
            if (!this.isValidSymbol(symbol, 'stock')) {
                throw new Error('Ungültiges Aktien-Symbol');
            }
            const formattedSymbol = this.formatSymbol(symbol, 'stock');
            return await this.getStockPrice(formattedSymbol);
        } catch (error) {
            this.handleApiError(error);
        }
    },

    async safeGetAssetHistory(symbol, type, market = 'USD', period = 'daily') {
        try {
            if (!this.isValidSymbol(symbol, type)) {
                throw new Error(`Ungültiges ${type}-Symbol`);
            }
            const formattedSymbol = this.formatSymbol(symbol, type);
            return await this.getAssetHistory(formattedSymbol, type, market, period);
        } catch (error) {
            this.handleApiError(error);
        }
    },

    // ========== DEPRECATED METHODS (für Rückwärtskompatibilität) ==========

    /**
     * @deprecated Verwenden Sie getCryptoPrice() stattdessen
     */
    getCurrentCryptoPrice(symbol, market = 'USD') {
        console.warn('getCurrentCryptoPrice() ist veraltet. Verwenden Sie getCryptoPrice()');
        return this.getCryptoPrice(symbol, market);
    },

    /**
     * @deprecated Verwenden Sie getStockPrice() stattdessen
     */
    getCurrentStockPrice(symbol) {
        console.warn('getCurrentStockPrice() ist veraltet. Verwenden Sie getStockPrice()');
        return this.getStockPrice(symbol);
    }
};