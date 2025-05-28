import axios from 'axios';

const API_URL = 'http://localhost:8081/api/assets';

export default {
    symbolCorrections: {
        'APPL': 'AAPL',
        'GOOG': 'GOOGL'
    },

    cryptoSymbols: [
        'BTC', 'ETH', 'ADA', 'DOT', 'SOL', 'MATIC', 'LINK', 'UNI',
        'AVAX', 'ATOM', 'XRP', 'LTC', 'USDT', 'USDC', 'BNB', 'DOGE',
        'SHIB', 'ALGO', 'VET', 'THETA', 'FIL', 'TRX', 'EOS', 'XLM',
        'NEAR', 'FLOW', 'ICP', 'AAVE', 'CRO', 'SAND', 'MANA', 'AXS'
    ],

    correctSymbol(symbol) {
        const normalized = (symbol && symbol.toString().trim().toUpperCase()) || '';
        return this.symbolCorrections[normalized] || normalized;
    },

    getAssetType(symbol) {
        return this.cryptoSymbols.includes(this.correctSymbol(symbol)) ? 'crypto' : 'stock';
    },

    validateAndCorrectSymbol(symbol, expectedType = null) {
        if (!symbol || !symbol.trim()) throw new Error('Symbol ist erforderlich');

        const corrected = this.correctSymbol(symbol);
        const detectedType = this.getAssetType(corrected);

        if (expectedType && expectedType !== detectedType) {
            console.warn(`⚠️ Type mismatch: Expected ${expectedType} but ${corrected} appears to be ${detectedType}`);
        }

        return {
            original: symbol,
            corrected,
            detectedType,
            wasCorrected: corrected !== symbol.toUpperCase()
        };
    },

    async getAssetPriceAuto(symbol, forceType = null) {
        const { corrected, detectedType, wasCorrected, original } = this.validateAndCorrectSymbol(symbol, forceType);
        const type = forceType || detectedType;

        try {
            const response = await axios.get(`${API_URL}/price/${corrected}`, {
                params: {
                    type: type,
                    market: 'USD'
                }
            });

            if (wasCorrected) {
                response.data.corrected = true;
                response.data.originalSymbol = original;
            }

            return response;
        } catch (error) {
            this.handleApiError(error, `Auto price for ${symbol}`);
        }
    },

    async getCryptoPrice(symbol, market = 'USD') {
        const { corrected, wasCorrected, original } = this.validateAndCorrectSymbol(symbol, 'crypto');
        try {
            const response = await axios.get(`${API_URL}/price/${corrected}`, {
                params: {
                    type: 'crypto',
                    market: market
                }
            });

            if (wasCorrected) {
                response.data.corrected = true;
                response.data.originalSymbol = original;
            }
            return response;
        } catch (error) {
            this.handleApiError(error, `Crypto price for ${symbol}`);
        }
    },

    async getStockPrice(symbol) {
        const { corrected, wasCorrected, original } = this.validateAndCorrectSymbol(symbol, 'stock');
        try {
            const response = await axios.get(`${API_URL}/price/${corrected}`, {
                params: {
                    type: 'stock'
                }
            });

            if (wasCorrected) {
                response.data.corrected = true;
                response.data.originalSymbol = original;
            }
            return response;
        } catch (error) {
            this.handleApiError(error, `Stock price for ${symbol}`);
        }
    },

    async getMultipleAssetPrices(symbols, type = null, market = 'USD') {
        if (!Array.isArray(symbols) || !symbols.length) throw new Error('Symbols array ist erforderlich');

        const validatedSymbols = symbols.map(s => this.validateAndCorrectSymbol(s, type).corrected);
        const actualType = type || (validatedSymbols.filter(s => this.getAssetType(s) === 'crypto').length > symbols.length / 2 ? 'crypto' : 'stock');

        const payload = {
            symbols: validatedSymbols,
            type: actualType,
            ...(actualType === 'crypto' && { market })
        };

        try {
            return await axios.post(`${API_URL}/prices`, payload);
        } catch (error) {
            this.handleApiError(error, 'Batch price fetch');
        }
    },

    async getWatchlist() {
        try {
            return await axios.get(`${API_URL}/watchlist`);
        } catch (error) {
            this.handleApiError(error, 'Watchlist laden');
        }
    },

    async addToWatchlist(asset) {
        try {
            const { corrected, detectedType } = this.validateAndCorrectSymbol(asset.symbol);
            return await axios.post(`${API_URL}/watchlist`, {
                ...asset,
                symbol: corrected,
                type: asset.type || detectedType
            });
        } catch (error) {
            this.handleApiError(error, 'Zur Watchlist hinzufügen');
        }
    },

    async removeFromWatchlist(id) {
        try {
            return await axios.delete(`${API_URL}/watchlist/${id}`);
        } catch (error) {
            this.handleApiError(error, 'Von Watchlist entfernen');
        }
    },

    async getAssetHistory(symbol, type = null, market = 'USD') {
        const { corrected, detectedType } = this.validateAndCorrectSymbol(symbol, type);
        const actualType = type || detectedType;

        try {
            const response = await axios.get(`${API_URL}/history/${corrected}`, {
                params: {
                    type: actualType,
                    ...(actualType === 'crypto' && { market })
                }
            });
            return response;
        } catch (error) {
            this.handleApiError(error, `History für ${symbol}`);
        }
    },

    // Legacy methods for backward compatibility
    async getCryptoHistory(symbol, market = 'USD') {
        return this.getAssetHistory(symbol, 'crypto', market);
    },

    async getStockHistory(symbol) {
        return this.getAssetHistory(symbol, 'stock');
    },

    handleApiError(error, context = 'API') {
        console.error(`${context} error:`, error);

        if (error.response) {
            const { status, data } = error.response;
            let errorMessage;

            switch (status) {
                case 400:
                    errorMessage = (data && data.error) || 'Ungültige Anfrage';
                    break;
                case 404:
                    errorMessage = (data && data.error) || 'Nicht gefunden';
                    break;
                case 429:
                    errorMessage = 'Rate Limit erreicht - zu viele Anfragen';
                    break;
                case 500:
                    errorMessage = (data && data.error) || 'Serverfehler';
                    break;
                default:
                    errorMessage = (data && data.error) || `HTTP ${status}: ${error.message}`;
            }

            throw new Error(errorMessage);
        } else if (error.request) {
            throw new Error('Server nicht erreichbar - Netzwerkverbindung prüfen');
        } else {
            throw new Error(error.message || 'Unbekannter Fehler');
        }
    },

    needsCorrection(symbol) {
        return symbol && Object.prototype.hasOwnProperty.call(this.symbolCorrections, symbol.toUpperCase().trim());
    },

    getSuggestedCorrection(symbol) {
        return this.symbolCorrections[symbol.toUpperCase().trim()] || null;
    }
};