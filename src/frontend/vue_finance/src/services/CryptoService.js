import axios from 'axios';

// Basis-URL für API-Endpunkte
const API_URL = 'http://localhost:8081/api/assets';

export default {
    // === SYMBOL MANAGEMENT ===

    /**
     * Bekannte Symbol-Korrekturen für häufige Tippfehler
     */
    symbolCorrections: {
        'APPL': 'AAPL',
        'GOOG': 'GOOGL'
    },

    /**
     * Liste der unterstützten Kryptowährungen
     */
    cryptoSymbols: [
        // Gängige Krypto-Symbole
        'BTC', 'ETH', 'ADA', 'DOT', 'SOL', 'MATIC', 'LINK', 'UNI',
        'AVAX', 'ATOM', 'XRP', 'LTC', 'USDT', 'USDC', 'BNB', 'DOGE',
        'SHIB', 'ALGO', 'VET', 'THETA', 'FIL', 'TRX', 'EOS', 'XLM',
        'NEAR', 'FLOW', 'ICP', 'AAVE', 'CRO', 'SAND', 'MANA', 'AXS'
    ],

    /**
     * Korrigiert bekannte Symbol-Tippfehler
     */
    correctSymbol(symbol) {
        // Normalisiert Symbol und wendet ggf. Korrekturregel an
        const normalized = (symbol && symbol.toString().trim().toUpperCase()) || '';
        return this.symbolCorrections[normalized] || normalized;
    },

    /**
     * Bestimmt Asset-Typ basierend auf Symbol
     */
    getAssetType(symbol) {
        // Prüft ob Symbol in der Krypto-Liste ist
        return this.cryptoSymbols.includes(this.correctSymbol(symbol)) ? 'crypto' : 'stock';
    },

    /**
     * Validiert und korrigiert Symbol mit Typ-Erkennung
     */
    validateAndCorrectSymbol(symbol, expectedType = null) {
        // Prüft auf leere Eingabe
        if (!symbol || !symbol.trim()) {
            throw new Error('Symbol ist erforderlich');
        }

        const corrected = this.correctSymbol(symbol);
        const detectedType = this.getAssetType(corrected);

        // Gibt Warnung aus, falls erwarteter Typ nicht erkanntem Typ entspricht
        if (expectedType && expectedType !== detectedType) {
            console.warn(`⚠️ Typ-Diskrepanz: Erwartet ${expectedType}, aber ${corrected} scheint ${detectedType} zu sein`);
        }

        // Gibt Korrekturinformationen zurück
        return {
            original: symbol,
            corrected,
            detectedType,
            wasCorrected: corrected !== symbol.toUpperCase()
        };
    },

    // === PRICE FETCHING ===

    /**
     * Automatische Preis-Abfrage mit Typ-Erkennung
     * @param {string} symbol - Asset Symbol
     * @param {string} forceType - Erzwinge spezifischen Typ (optional)
     */
    async getAssetPriceAuto(symbol, forceType = null) {
        // Symbol validieren und ggf. Typ ermitteln
        const { corrected, detectedType, wasCorrected, original } = this.validateAndCorrectSymbol(symbol, forceType);
        const type = forceType || detectedType;

        try {
            // Preis vom API abrufen
            const response = await axios.get(`${API_URL}/price/${corrected}`, {
                params: {
                    type: type,
                    market: 'USD'
                }
            });

            // Zusatzinfo bei korrigiertem Symbol
            if (wasCorrected) {
                response.data.corrected = true;
                response.data.originalSymbol = original;
            }

            return response;
        } catch (error) {
            // Fehler behandeln
            this.handleApiError(error, `Auto-Preis für ${symbol}`);
        }
    },

    /**
     * Spezifische Krypto-Preis-Abfrage
     */
    async getCryptoPrice(symbol, market = 'USD') {
        // Symbol validieren mit erwarteter Typ "crypto"
        const { corrected, wasCorrected, original } = this.validateAndCorrectSymbol(symbol, 'crypto');

        try {
            // Preis für Kryptowährung abrufen
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
            this.handleApiError(error, `Krypto-Preis für ${symbol}`);
        }
    },

    /**
     * Spezifische Aktien-Preis-Abfrage
     */
    async getStockPrice(symbol) {
        // Symbol validieren mit erwarteter Typ "stock"
        const { corrected, wasCorrected, original } = this.validateAndCorrectSymbol(symbol, 'stock');

        try {
            // Preis für Aktie abrufen
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
            this.handleApiError(error, `Aktien-Preis für ${symbol}`);
        }
    },

    // === WATCHLIST MANAGEMENT ===

    /**
     * Lädt komplette Watchlist
     */
    async getWatchlist() {
        try {
            // Abrufen aller Watchlist-Einträge
            return await axios.get(`${API_URL}/watchlist`);
        } catch (error) {
            this.handleApiError(error, 'Watchlist laden');
        }
    },

    /**
     * Fügt Asset zur Watchlist hinzu
     */
    async addToWatchlist(asset) {
        try {
            // Symbol validieren und Typ ermitteln
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

    /**
     * Entfernt Asset von Watchlist
     */
    async removeFromWatchlist(id) {
        try {
            // Asset mit ID aus Watchlist entfernen
            return await axios.delete(`${API_URL}/watchlist/${id}`);
        } catch (error) {
            this.handleApiError(error, 'Von Watchlist entfernen');
        }
    },

    // === INVESTMENT MANAGEMENT ===

    /**
     * Fügt Investition hinzu (erstellt Asset oder aktualisiert bestehendes)
     */
    async addInvestment(asset, investmentAmount) {
        try {
            // Symbol validieren und Typ ermitteln
            const { corrected, detectedType } = this.validateAndCorrectSymbol(asset.symbol);
            const response = await axios.post(`${API_URL}/investment`, {
                symbol: corrected,
                name: asset.name,
                type: asset.type || detectedType,
                investmentAmount: investmentAmount
            });
            return response;
        } catch (error) {
            this.handleApiError(error, 'Investition hinzufügen');
        }
    },

    /**
     * Lädt Portfolio-Zusammenfassung für Asset
     */
    async getPortfolioSummary(watchlistId) {
        try {
            // Zusammenfassung zu Asset-Investments abrufen
            const response = await axios.get(`${API_URL}/watchlist/${watchlistId}/portfolio`);
            return response;
        } catch (error) {
            this.handleApiError(error, 'Portfolio-Zusammenfassung laden');
        }
    },

    // === HISTORICAL DATA ===

    /**
     * Lädt historische Kursdaten
     */
    async getAssetHistory(symbol, type = null, market = 'USD') {
        // Symbol validieren und Typ ermitteln
        const { corrected, detectedType } = this.validateAndCorrectSymbol(symbol, type);
        const actualType = type || detectedType;

        try {
            // Kursverlauf vom Server abrufen
            const response = await axios.get(`${API_URL}/history/${corrected}`, {
                params: {
                    type: actualType,
                    ...(actualType === 'crypto' && { market })
                }
            });
            return response;
        } catch (error) {
            this.handleApiError(error, `Kursverlauf für ${symbol}`);
        }
    },

    // === BATCH OPERATIONS ===

    /**
     * Lädt mehrere Asset-Preise gleichzeitig
     * Vereinfacht für bessere Performance bei vielen Assets
     */
    async getMultipleAssetPrices(symbols, type = null, market = 'USD') {
        // Prüft ob Eingabe ein Array ist
        if (!Array.isArray(symbols) || !symbols.length) {
            throw new Error('Symbols-Array ist erforderlich');
        }

        // Alle Symbole validieren und korrigieren
        const validatedSymbols = symbols.map(s => this.validateAndCorrectSymbol(s, type).corrected);

        // Typ automatisch ermitteln, wenn nicht vorgegeben
        const actualType = type || (
            validatedSymbols.filter(s => this.getAssetType(s) === 'crypto').length > symbols.length / 2
                ? 'crypto'
                : 'stock'
        );

        const payload = {
            symbols: validatedSymbols,
            type: actualType,
            ...(actualType === 'crypto' && { market })
        };

        try {
            // Batch-Request an Server senden
            return await axios.post(`${API_URL}/prices`, payload);
        } catch (error) {
            this.handleApiError(error, 'Batch-Preis-Abfrage');
        }
    },

    // === ERROR HANDLING ===

    /**
     * Einheitliche API-Fehlerbehandlung mit benutzerfreundlichen Nachrichten
     */
    handleApiError(error, context = 'API') {
        console.error(`${context} Fehler:`, error);

        if (error.response) {
            // Fehler vom Server vorhanden
            const { status, data } = error.response;
            let errorMessage;

            // Fehler basierend auf Statuscode bestimmen
            switch (status) {
                case 400:
                    errorMessage = (data && data.error) || 'Ungültige Anfrage';
                    break;
                case 404:
                    errorMessage = (data && data.error) || 'Asset nicht gefunden';
                    break;
                case 429:
                    errorMessage = 'Rate Limit erreicht - bitte warten Sie einen Moment';
                    break;
                case 500:
                    errorMessage = (data && data.error) || 'Serverfehler - bitte versuchen Sie es später erneut';
                    break;
                default:
                    errorMessage = (data && data.error) || `HTTP ${status}: ${error.message}`;
            }

            throw new Error(errorMessage);
        } else if (error.request) {
            // Anfrage konnte nicht gesendet werden
            throw new Error('Server nicht erreichbar - bitte Netzwerkverbindung prüfen');
        } else {
            // Anderer Fehlerfall
            throw new Error(error.message || 'Unbekannter Fehler');
        }
    },

    // === UTILITY METHODS ===

    /**
     * Prüft ob Symbol korrigiert werden muss
     */
    needsCorrection(symbol) {
        // Prüft ob Symbol in Korrektur-Tabelle vorhanden ist
        return symbol && Object.prototype.hasOwnProperty.call(this.symbolCorrections, symbol.toUpperCase().trim());
    },

    /**
     * Gibt Korrektur-Vorschlag zurück
     */
    getSuggestedCorrection(symbol) {
        // Gibt Korrekturwert zurück, falls vorhanden
        return this.symbolCorrections[symbol.toUpperCase().trim()] || null;
    }
};
