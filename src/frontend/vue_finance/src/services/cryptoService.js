import ApiService from './api';

const CRYPTO_URL = '/krypto';

const CryptoService = {
    // Alle Kryptowährungen in der Watchlist abrufen
    getAllCryptos() {
        return ApiService.get(CRYPTO_URL);
    },

    // Kryptowährung nach ID abrufen
    getCryptoById(id) {
        return ApiService.get(`${CRYPTO_URL}/${id}`);
    },

    // Kryptowährung nach Symbol abrufen
    getCryptoBySymbol(symbol) {
        return ApiService.get(`${CRYPTO_URL}/symbol/${symbol}`);
    },

    // Kryptowährung zur Watchlist hinzufügen
    addCryptoToWatchlist(symbol) {
        return ApiService.post(CRYPTO_URL, { symbol });
    },

    // Kryptowährung von der Watchlist entfernen
    removeCryptoFromWatchlist(id) {
        return ApiService.delete(`${CRYPTO_URL}/${id}`);
    },

    // Notiz zu einer Kryptowährung hinzufügen
    addNoteToEntry(cryptoId, text) {
        return ApiService.post(`${CRYPTO_URL}/${cryptoId}/notizen`, { text });
    },

    // Notizen zu einer Kryptowährung abrufen
    getNotesForEntry(cryptoId) {
        return ApiService.get(`${CRYPTO_URL}/${cryptoId}/notizen`);
    },

    // Notiz aktualisieren
    updateNote(noteId, text) {
        return ApiService.put(`${CRYPTO_URL}/notizen/${noteId}`, { text });
    },

    // Notiz löschen
    deleteNote(noteId) {
        return ApiService.delete(`${CRYPTO_URL}/notizen/${noteId}`);
    }
};

export default CryptoService;