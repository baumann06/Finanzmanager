import ApiService from './api';

const FINANCE_URL = '/finance';

const FinanceService = {
    // Transaktionen abrufen
    getAllTransactions() {
        return ApiService.get(`${FINANCE_URL}/transactions`);
    },

    // Transaktion nach ID abrufen
    getTransactionById(id) {
        return ApiService.get(`${FINANCE_URL}/transactions/${id}`);
    },

    // Neue Transaktion speichern
    saveTransaction(transaction) {
        return ApiService.post(`${FINANCE_URL}/transactions`, transaction);
    },

    // Transaktion aktualisieren
    updateTransaction(id, transaction) {
        return ApiService.put(`${FINANCE_URL}/transactions/${id}`, transaction);
    },

    // Transaktion löschen
    deleteTransaction(id) {
        return ApiService.delete(`${FINANCE_URL}/transactions/${id}`);
    },

    // Transaktionen nach Kategorie abrufen
    getTransactionsByCategory(categoryId) {
        return ApiService.get(`${FINANCE_URL}/transactions/category/${categoryId}`);
    },

    // Transaktionen nach Monat abrufen
    getTransactionsByMonth(year, month) {
        return ApiService.get(`${FINANCE_URL}/transactions/month?year=${year}&month=${month}`);
    },

    // Transaktionen nach Währung abrufen
    getTransactionsByCurrency(currency) {
        return ApiService.get(`${FINANCE_URL}/transactions/currency/${currency}`);
    },

    // Summe für einen Zeitraum abrufen
    getSumForPeriod(start, end, currency = 'EUR') {
        return ApiService.get(
            `${FINANCE_URL}/summary/period?start=${start}&end=${end}&currency=${currency}`
        );
    },

    // Monatliche Zusammenfassung abrufen
    getMonthlySummary(year, currency = 'EUR') {
        return ApiService.get(`${FINANCE_URL}/summary/monthly?year=${year}&currency=${currency}`);
    },

    // Zusammenfassung nach Kategorien abrufen
    getCategorySummary(start, end, currency = 'EUR') {
        return ApiService.get(
            `${FINANCE_URL}/summary/category?start=${start}&end=${end}&currency=${currency}`
        );
    },

    // Alle Kategorien abrufen
    getAllCategories() {
        return ApiService.get(`${FINANCE_URL}/categories`);
    },

    // Wechselkurse abrufen
    getExchangeRates() {
        return ApiService.get(`${FINANCE_URL}/exchange-rates`);
    },

    // Währung umrechnen
    convertCurrency(amount, from, to) {
        return ApiService.get(
            `${FINANCE_URL}/convert?amount=${amount}&from=${from}&to=${to}`
        );
    }
};

export default FinanceService;