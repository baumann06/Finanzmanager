<template>
  <div>
    <h2>Finanzmanager</h2>

    <div class="overview-section">
      <div class="balance">
        <h3>Kontostand</h3>
        <p>{{ formatBalance(balance) }} €</p>
      </div>
    </div>

    <div class="input-sections">
      <div class="expense-section">
        <h3>Neue Ausgabe hinzufügen</h3>
        <div class="form">
          <input v-model="newExpense.description" placeholder="Beschreibung" />
          <input v-model="newExpense.amount" type="number" step="0.01" placeholder="Betrag" />
          <input v-model="newExpense.category" placeholder="Kategorie" />
          <input v-model="newExpense.date" type="date" />
          <button @click="addExpense">Ausgabe hinzufügen</button>
        </div>
      </div>

      <div class="income-section">
        <h3>Neue Einnahme hinzufügen</h3>
        <div class="form">
          <input v-model="newIncome.source" placeholder="Quelle" />
          <input v-model="newIncome.amount" type="number" step="0.01" placeholder="Betrag" />
          <input v-model="newIncome.date" type="date" />
          <button @click="addIncome">Einnahme hinzufügen</button>
        </div>
      </div>
    </div>

    <div v-if="expenses.length > 0" class="list-section">
      <h3>Ausgaben</h3>
      <table>
        <thead>
        <tr>
          <th>Beschreibung</th>
          <th>Betrag</th>
          <th>Kategorie</th>
          <th>Datum</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="expense in expenses" :key="expense.id">
          <td>{{ expense.description }}</td>
          <td>{{ formatAmount(expense.amount) }} €</td>
          <td>{{ expense.category }}</td>
          <td>{{ formatDate(expense.date) }}</td>
        </tr>
        </tbody>
      </table>
    </div>

    <div v-if="incomes.length > 0" class="list-section">
      <h3>Einnahmen</h3>
      <table>
        <thead>
        <tr>
          <th>Quelle</th>
          <th>Betrag</th>
          <th>Datum</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="income in incomes" :key="income.id">
          <td>{{ income.source }}</td>
          <td>{{ formatAmount(income.amount) }} €</td>
          <td>{{ formatDate(income.date) }}</td>
        </tr>
        </tbody>
      </table>
    </div>

    <div v-if="Object.keys(expensesByCategory).length > 0" class="category-section">
      <h3>Ausgaben nach Kategorien</h3>
      <div class="category-list">
        <div v-for="(amount, category) in expensesByCategory" :key="category" class="category-item">
          <span class="category-name">{{ category }}:</span>
          <span class="category-amount">{{ formatAmount(amount) }} €</span>
        </div>
      </div>
    </div>

    <div class="currency-section">
      <h3>Währungsrechner</h3>
      <div class="form">
        <input v-model="currencyAmount" type="number" step="0.01" placeholder="Betrag" />
        <select v-model="fromCurrency">
          <option value="EUR">EUR</option>
          <option value="USD">USD</option>
          <option value="GBP">GBP</option>
          <option value="JPY">JPY</option>
          <option value="CHF">CHF</option>
          <option value="CAD">CAD</option>
          <option value="AUD">AUD</option>
        </select>
        <span>zu</span>
        <select v-model="toCurrency">
          <option value="USD">USD</option>
          <option value="EUR">EUR</option>
          <option value="GBP">GBP</option>
          <option value="JPY">JPY</option>
          <option value="CHF">CHF</option>
          <option value="CAD">CAD</option>
          <option value="AUD">AUD</option>
        </select>
        <button @click="convertCurrency" :disabled="convertingCurrency">
          {{ convertingCurrency ? 'Umrechnung...' : 'Umrechnen' }}
        </button>
        <div v-if="convertedAmount !== null" class="conversion-result">
          <p>
            {{ formatAmount(currencyAmount) }} {{ fromCurrency }} =
            {{ formatAmount(convertedAmount) }} {{ toCurrency }}
          </p>
          <p class="exchange-rate">
            Wechselkurs: 1 {{ fromCurrency }} = {{ formatAmount(exchangeRate) }} {{ toCurrency }}
          </p>
        </div>
        <div v-if="conversionError" class="error-message">
          <p>Fehler bei der Währungsumrechnung: {{ conversionError }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import FinanceService from '../services/FinanceService';

export default {
  name: 'FinanceModule',
  data() {
    return {
      expenses: [],
      incomes: [],
      balance: 0,
      expensesByCategory: {},
      newExpense: {
        description: '',
        amount: null,
        category: '',
        date: new Date().toISOString().split('T')[0]
      },
      newIncome: {
        source: '',
        amount: null,
        date: new Date().toISOString().split('T')[0]
      },
      currencyAmount: 100,
      fromCurrency: 'EUR',
      toCurrency: 'USD',
      convertedAmount: null,
      exchangeRate: null,
      convertingCurrency: false,
      conversionError: null
    };
  },
  created() {
    this.fetchData();
  },
  methods: {
    fetchData() {
      this.fetchExpenses();
      this.fetchIncomes();
      this.fetchBalance();
      this.fetchExpensesByCategory();
    },
    fetchExpenses() {
      FinanceService.getAllExpenses()
          .then(response => {
            this.expenses = response.data;
          })
          .catch(error => {
            console.error('Error fetching expenses:', error);
            alert('Fehler beim Laden der Ausgaben: ' + error.message);
          });
    },
    fetchIncomes() {
      FinanceService.getAllIncomes()
          .then(response => {
            this.incomes = response.data;
          })
          .catch(error => {
            console.error('Error fetching incomes:', error);
            alert('Fehler beim Laden der Einnahmen: ' + error.message);
          });
    },
    fetchBalance() {
      FinanceService.getBalance()
          .then(response => {
            this.balance = response.data;
          })
          .catch(error => {
            console.error('Error fetching balance:', error);
            alert('Fehler beim Laden des Kontostands: ' + error.message);
          });
    },
    fetchExpensesByCategory() {
      FinanceService.getExpensesByCategory()
          .then(response => {
            this.expensesByCategory = response.data;
          })
          .catch(error => {
            console.error('Error fetching expenses by category:', error);
          });
    },
    addExpense() {
      if (!this.newExpense.description || !this.newExpense.amount || !this.newExpense.category) {
        alert('Bitte alle Felder ausfüllen');
        return;
      }

      if (this.newExpense.amount <= 0) {
        alert('Der Betrag muss größer als 0 sein');
        return;
      }

      FinanceService.addExpense(this.newExpense)
          .then(() => {
            this.fetchData();
            this.resetExpenseForm();
          })
          .catch(error => {
            console.error('Error adding expense:', error);
            alert('Fehler beim Hinzufügen der Ausgabe: ' + error.message);
          });
    },
    addIncome() {
      if (!this.newIncome.source || !this.newIncome.amount) {
        alert('Bitte alle Felder ausfüllen');
        return;
      }

      if (this.newIncome.amount <= 0) {
        alert('Der Betrag muss größer als 0 sein');
        return;
      }

      FinanceService.addIncome(this.newIncome)
          .then(() => {
            this.fetchData();
            this.resetIncomeForm();
          })
          .catch(error => {
            console.error('Error adding income:', error);
            alert('Fehler beim Hinzufügen der Einnahme: ' + error.message);
          });
    },
    convertCurrency() {
      if (!this.currencyAmount || this.currencyAmount <= 0) {
        alert('Bitte geben Sie einen gültigen Betrag ein');
        return;
      }

      if (this.fromCurrency === this.toCurrency) {
        this.convertedAmount = this.currencyAmount;
        this.exchangeRate = 1;
        return;
      }

      this.convertingCurrency = true;
      this.conversionError = null;
      this.convertedAmount = null;

      FinanceService.getExchangeRate(this.fromCurrency, this.toCurrency)
          .then(response => {
            this.exchangeRate = response.data.rate;
            this.convertedAmount = this.currencyAmount * this.exchangeRate;
            this.convertingCurrency = false;
          })
          .catch(error => {
            console.error('Error converting currency:', error);
            this.conversionError =
                (error.response && error.response.data && error.response.data.message) ||
                error.message;
            this.convertingCurrency = false;
          });
    },
    resetExpenseForm() {
      this.newExpense = {
        description: '',
        amount: null,
        category: '',
        date: new Date().toISOString().split('T')[0]
      };
    },
    resetIncomeForm() {
      this.newIncome = {
        source: '',
        amount: null,
        date: new Date().toISOString().split('T')[0]
      };
    },
    formatAmount(amount) {
      if (amount === null || amount === undefined) return '0.00';
      return parseFloat(amount).toFixed(2);
    },
    formatBalance(balance) {
      if (balance === null || balance === undefined) return '0.00';
      const num = parseFloat(balance);
      return num.toFixed(2);
    },
    formatDate(dateString) {
      if (!dateString) return '';
      return new Date(dateString).toLocaleDateString('de-DE');
    }
  }
};
</script>


<style scoped>
.overview-section {
  margin-bottom: 20px;
}
.balance {
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
  background-color: #f8f9fa;
}
.balance h3 {
  margin: 0 0 10px 0;
  color: #333;
}
.balance p {
  font-size: 1.5em;
  font-weight: bold;
  margin: 0;
  color: #007bff;
}
.input-sections {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}
.expense-section, .income-section {
  flex: 1;
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.form input, .form select, .form button {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 3px;
}
.form button {
  background-color: #007bff;
  color: white;
  cursor: pointer;
}
.form button:hover:not(:disabled) {
  background-color: #0056b3;
}
.form button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}
.list-section {
  margin-bottom: 20px;
}
.category-section {
  margin-bottom: 20px;
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
}
.category-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}
.category-item {
  display: flex;
  justify-content: space-between;
  padding: 8px;
  background-color: #f8f9fa;
  border-radius: 3px;
}
.category-name {
  font-weight: bold;
}
.category-amount {
  color: #dc3545;
}
.currency-section {
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
}
.currency-section .form {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto;
  gap: 10px;
  align-items: center;
  max-width: 500px;
}
.conversion-result {
  grid-column: 1 / -1;
  margin-top: 10px;
  padding: 10px;
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
  border-radius: 3px;
}
.exchange-rate {
  font-size: 0.9em;
  color: #666;
  margin: 5px 0 0 0;
}
.error-message {
  grid-column: 1 / -1;
  margin-top: 10px;
  padding: 10px;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 3px;
  color: #721c24;
}
table {
  width: 100%;
  border-collapse: collapse;
  margin-top: 10px;
}
th, td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}
th {
  background-color: #f2f2f2;
  font-weight: bold;
}
tbody tr:nth-child(even) {
  background-color: #f9f9f9;
}
</style>