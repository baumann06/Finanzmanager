import axios from 'axios';

const API_URL = 'http://localhost:8081/api/finance';

export default {
    getAllExpenses() {
        return axios.get(`${API_URL}/expenses`);
    },
    addExpense(expense) {
        return axios.post(`${API_URL}/expenses`, expense);
    },
    getAllIncomes() {
        return axios.get(`${API_URL}/incomes`);
    },
    addIncome(income) {
        return axios.post(`${API_URL}/incomes`, income);
    },
    getBalance() {
        return axios.get(`${API_URL}/balance`);
    },
    getExpensesByCategory() {
        return axios.get(`${API_URL}/expenses/by-category`);
    },
    getExchangeRate(fromCurrency, toCurrency) {
        return axios.get(`${API_URL}/exchange-rate?fromCurrency=${fromCurrency}&toCurrency=${toCurrency}`);
    }
};