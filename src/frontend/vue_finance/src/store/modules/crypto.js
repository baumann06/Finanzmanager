import CryptoService from '@/services/cryptoService';

export default {
    namespaced: true,

    state: {
        cryptoEntries: [],
        selectedCrypto: null,
        notes: {},
        topGainers: []
    },

    mutations: {
        SET_CRYPTO_ENTRIES(state, entries) {
            state.cryptoEntries = entries;
        },
        SET_SELECTED_CRYPTO(state, crypto) {
            state.selectedCrypto = crypto;
        },
        ADD_CRYPTO_ENTRY(state, entry) {
            state.cryptoEntries.push(entry);
        },
        REMOVE_CRYPTO_ENTRY(state, id) {
            state.cryptoEntries = state.cryptoEntries.filter(entry => entry.id !== id);
        },
        SET_NOTES(state, { cryptoId, notes }) {
            state.notes = {
                ...state.notes,
                [cryptoId]: notes
            };
        },
        ADD_NOTE(state, { cryptoId, note }) {
            if (!state.notes[cryptoId]) {
                state.notes[cryptoId] = [];
            }
            state.notes[cryptoId].push(note);
        },
        UPDATE_NOTE(state, { cryptoId, noteId, text }) {
            if (state.notes[cryptoId]) {
                const noteIndex = state.notes[cryptoId].findIndex(note => note.id === noteId);
                if (noteIndex !== -1) {
                    state.notes[cryptoId][noteIndex].text = text;
                }
            }
        },
        REMOVE_NOTE(state, { cryptoId, noteId }) {
            if (state.notes[cryptoId]) {
                state.notes[cryptoId] = state.notes[cryptoId].filter(note => note.id !== noteId);
            }
        },
        SET_TOP_GAINERS(state, gainers) {
            state.topGainers = gainers;
        }
    },

    actions: {
        // Alle Kryptowährungen in der Watchlist abrufen
        async fetchCryptoEntries({ commit, dispatch }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await CryptoService.getAllCryptos();
                commit('SET_CRYPTO_ENTRIES', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Laden der Kryptowährungen', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Kryptowährung nach ID abrufen
        async fetchCryptoById({ commit, dispatch }, id) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await CryptoService.getCryptoById(id);
                commit('SET_SELECTED_CRYPTO', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', `Fehler beim Laden der Kryptowährung mit ID ${id}`, { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Kryptowährung nach Symbol abrufen
        async fetchCryptoBySymbol({ commit, dispatch }, symbol) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await CryptoService.getCryptoBySymbol(symbol);
                return response.data;
            } catch (error) {
                dispatch('setError', `Fehler beim Laden der Kryptowährung mit Symbol ${symbol}`, { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Kryptowährung zur Watchlist hinzufügen
        async addCryptoToWatchlist({ commit, dispatch }, symbol) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await CryptoService.addCryptoToWatchlist(symbol);
                commit('ADD_CRYPTO_ENTRY', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Hinzufügen zur Watchlist', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Kryptowährung von der Watchlist entfernen
        async removeCryptoFromWatchlist({ commit, dispatch }, id) {
            try {
                dispatch('setLoading', true, { root: true });
                await CryptoService.removeCryptoFromWatchlist(id);
                commit('REMOVE_CRYPTO_ENTRY', id);
            } catch (error) {
                dispatch('setError', 'Fehler beim Entfernen von der Watchlist', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Notizen zu einer Kryptowährung abrufen
        async fetchNotesForCrypto({ commit, dispatch }, cryptoId) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await CryptoService.getNotesForEntry(cryptoId);
                commit('SET_NOTES', { cryptoId, notes: response.data });
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Laden der Notizen', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Notiz hinzufügen
        async addNote({ commit, dispatch }, { cryptoId, text }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await CryptoService.addNoteToEntry(cryptoId, text);
                commit('ADD_NOTE', { cryptoId, note: response.data });
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Hinzufügen der Notiz', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Notiz aktualisieren
        async updateNote({ commit, dispatch }, { cryptoId, noteId, text }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await CryptoService.updateNote(noteId, text);
                commit('UPDATE_NOTE', { cryptoId, noteId, text });
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Aktualisieren der Notiz', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Notiz löschen
        async deleteNote({ commit, dispatch }, { cryptoId, noteId }) {
            try {
                dispatch('setLoading', true, { root: true });
                await CryptoService.deleteNote(noteId);
                commit('REMOVE_NOTE', { cryptoId, noteId });
            } catch (error) {
                dispatch('setError', 'Fehler beim Löschen der Notiz', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Top Gainer abrufen (falls vom Backend unterstützt)
        setTopGainers({ commit }, gainers) {
            commit('SET_TOP_GAINERS', gainers);
        }
    },

    getters: {
        getCryptoById: (state) => (id) => {
            return state.cryptoEntries.find(entry => entry.id === id);
        },
        getCryptoBySymbol: (state) => (symbol) => {
            return state.cryptoEntries.find(entry => entry.symbol.toLowerCase() === symbol.toLowerCase());
        },
        getNotesForCrypto: (state) => (cryptoId) => {
            return state.notes[cryptoId] || [];
        }
    }
};