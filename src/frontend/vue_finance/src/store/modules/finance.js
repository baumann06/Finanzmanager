import FinanceService from '@/services/financeService';

export default {
    namespaced: true,

    state: {
        transactions: [],
        categories: [],
        selectedTransaction: null,
        monthlySummary: {},
        categorySummary: {},
        exchangeRates: {},
        selectedCurrency: 'EUR',
        dateRange: {
            start: new Date(new Date().getFullYear(), new Date().getMonth(), 1).toISOString().split('T')[0],
            end: new Date().toISOString().split('T')[0]
        }
    },

    mutations: {
        SET_TRANSACTIONS(state, transactions) {
            state.transactions = transactions;
        },
        SET_CATEGORIES(state, categories) {
            state.categories = categories;
        },
        SET_SELECTED_TRANSACTION(state, transaction) {
            state.selectedTransaction = transaction;
        },
        ADD_TRANSACTION(state, transaction) {
            state.transactions.push(transaction);
        },
        UPDATE_TRANSACTION(state, updatedTransaction) {
            const index = state.transactions.findIndex(t => t.id === updatedTransaction.id);
            if (index !== -1) {
                state.transactions.splice(index, 1, updatedTransaction);
            }
        },
        REMOVE_TRANSACTION(state, id) {
            state.transactions = state.transactions.filter(t => t.id !== id);
        },
        SET_MONTHLY_SUMMARY(state, summary) {
            state.monthlySummary = summary;
        },
        SET_CATEGORY_SUMMARY(state, summary) {
            state.categorySummary = summary;
        },
        SET_EXCHANGE_RATES(state, rates) {
            state.exchangeRates = rates;
        },
        SET_SELECTED_CURRENCY(state, currency) {
            state.selectedCurrency = currency;
        },
        SET_DATE_RANGE(state, { start, end }) {
            state.dateRange = { start, end };
        }
    },

    actions: {
        // Alle Transaktionen laden
        async fetchTransactions({ commit, dispatch }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await FinanceService.getAllTransactions();
                commit('SET_TRANSACTIONS', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Laden der Transaktionen', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Alle Kategorien laden
        async fetchCategories({ commit, dispatch }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await FinanceService.getAllCategories();
                commit('SET_CATEGORIES', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Laden der Kategorien', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Transaktion nach ID laden
        async fetchTransactionById({ commit, dispatch }, id) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await FinanceService.getTransactionById(id);
                commit('SET_SELECTED_TRANSACTION', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', `Fehler beim Laden der Transaktion mit ID ${id}`, { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Neue Transaktion erstellen
        async createTransaction({ commit, dispatch }, transaction) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await FinanceService.saveTransaction(transaction);
                commit('ADD_TRANSACTION', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Erstellen der Transaktion', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Transaktion aktualisieren
        async updateTransaction({ commit, dispatch }, { id, transaction }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await FinanceService.updateTransaction(id, transaction);
                commit('UPDATE_TRANSACTION', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Aktualisieren der Transaktion', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Transaktion löschen
        async deleteTransaction({ commit, dispatch }, id) {
            try {
                dispatch('setLoading', true, { root: true });
                await FinanceService.deleteTransaction(id);
                commit('REMOVE_TRANSACTION', id);
            } catch (error) {
                dispatch('setError', 'Fehler beim Löschen der Transaktion', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Monatliche Zusammenfassung laden
        async fetchMonthlySummary({ commit, dispatch }, { year, currency }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await FinanceService.getMonthlySummary(year, currency);
                commit('SET_MONTHLY_SUMMARY', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Laden der monatlichen Übersicht', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Zusammenfassung nach Kategorien laden
        async fetchCategorySummary({ commit, dispatch, state }) {
            try {
                dispatch('setLoading', true, { root: true });
                const { start, end } = state.dateRange;
                const response = await FinanceService.getCategorySummary(
                    start,
                    end,
                    state.selectedCurrency
                );
                commit('SET_CATEGORY_SUMMARY', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Laden der Kategorienübersicht', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Wechselkurse laden
        async fetchExchangeRates({ commit, dispatch }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await FinanceService.getExchangeRates();
                commit('SET_EXCHANGE_RATES', response.data);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler beim Laden der Wechselkurse', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Währung umrechnen
        async convertCurrency({ dispatch }, { amount, from, to }) {
            try {
                dispatch('setLoading', true, { root: true });
                const response = await FinanceService.convertCurrency(amount, from, to);
                return response.data;
            } catch (error) {
                dispatch('setError', 'Fehler bei der Währungsumrechnung', { root: true });
                throw error;
            } finally {
                dispatch('setLoading', false, { root: true });
            }
        },

        // Filter und Parameter setzen
        setSelectedCurrency({ commit }, currency) {
            commit('SET_SELECTED_CURRENCY', currency);
        },

        setDateRange({ commit }, { start, end }) {
            commit('SET_DATE_RANGE', { start, end });
        }
    },

    getters: {
        getTransactionById: (state) => (id) => {
            return state.transactions.find(transaction => transaction.id === id);
        },
        getCategoryById: (state) => (id) => {
            return state.categories.find(category => category.id === id);
        },
        getCategoryName: (state) => (id) => {
            const category = state.categories.find(c => c.id === id);
            return category ? category.name : 'Unbekannt';
        },
        getAvailableCurrencies: (state) => {
            return Object.keys(state.exchangeRates).concat(['EUR']);
        },
        getTotalBalance: (state) => {
            return state.transactions.reduce((total, transaction) => {
                return total + transaction.betrag;
            }, 0);
        },
        getIncomeTransactions: (state) => {
            return state.transactions.filter(transaction => transaction.betrag > 0);
        },
        getExpenseTransactions: (state) => {
            return state.transactions.filter(transaction => transaction.betrag < 0);
        }
    }
};