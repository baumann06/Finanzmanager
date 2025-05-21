<template>
  <div>
    <h2>Finanzmanager</h2>

    <div class="overview-section">
      <div class="balance">
        <h3>Kontostand</h3>
        <p>{{ balance }} €</p>
      </div>
    </div>

    <div class="input-sections">
      <div class="expense-section">
        <h3>Neue Ausgabe hinzufügen</h3>
        <div class="form">
          <input v-model="newExpense.description" placeholder="Beschreibung" />
          <input v-model="newExpense.amount" type="number" placeholder="Betrag" />
          <input v-model="newExpense.category" placeholder="Kategorie" />
          <input v-model="newExpense.date" type="date" />
          <button @click="addExpense">Ausgabe hinzufügen</button>
        </div>
      </div>

      <div class="income-section">
        <h3>Neue Einnahme hinzufügen</h3>
        <div class="form">
          <input v-model="newIncome.source" placeholder="Quelle" />
          <input v-model="newIncome.amount" type="number" placeholder="Betrag" />
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
          <td>{{ expense.amount }} €</td>
          <td>{{ expense.category }}</td>
          <td>{{ expense.date }}</td>
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
          <td>{{ income.amount }} €</td>
          <td>{{ income.date }}</td>
        </tr>
        </tbody>
      </table>
    </div>

    <div class="currency-section">
      <h3>Währungsrechner</h3>
      <div class="form">
        <input v-model="currencyAmount" type="number" placeholder="Betrag" />
        <select v-model="fromCurrency">
          <option value="EUR">EUR</option>
          <option value="USD">USD</option>
          <option value="GBP">GBP</option>
          <option value="JPY">JPY</option>
        </select>
        <span>zu</span>
        <select v-model="toCurrency">
          <option value="USD">USD</option>
          <option value="EUR">EUR</option>
          <option value="GBP">GBP</option>
          <option value="JPY">JPY</option>
        </select>
        <button @click="convertCurrency">Umrechnen</button>
        <p v-if="convertedAmount">
          {{ currencyAmount }} {{ fromCurrency }} = {{ convertedAmount }} {{ toCurrency }}
        </p>
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
      exchangeRate: null
    };
  },
  created() {
    this.fetchData();
  },
  methods: {
    fetchData() {
      FinanceService.getAllExpenses()
          .then(response => {
            this.expenses = response.data;
          })
          .catch(error => {
            console.error('Error fetching expenses:', error);
          });

      FinanceService.getAllIncomes()
          .then(response => {
            this.incomes = response.data;
          })
          .catch(error => {
            console.error('Error fetching incomes:', error);
          });

      FinanceService.getBalance()
          .then(response => {
            this.balance = response.data;
          })
          .catch(error => {
            console.error('Error fetching balance:', error);
          });
    },
    addExpense() {
      if (!this.newExpense.description || !this.newExpense.amount || !this.newExpense.category) {
        alert('Bitte alle Felder ausfüllen');
        return;
      }

      FinanceService.addExpense(this.newExpense)
          .then(() => {
            this.fetchData();
            this.newExpense = {
              description: '',
              amount: null,
              category: '',
              date: new Date().toISOString().split('T')[0]
            };
          })
          .catch(error => {
            console.error('Error adding expense:', error);
          });
    },
    addIncome() {
      if (!this.newIncome.source || !this.newIncome.amount) {
        alert('Bitte alle Felder ausfüllen');
        return;
      }

      FinanceService.addIncome(this.newIncome)
          .then(() => {
            this.fetchData();
            this.newIncome = {
              source: '',
              amount: null,
              date: new Date().toISOString().split('T')[0]
            };
          })
          .catch(error => {
            console.error('Error adding income:', error);
          });
    },
    convertCurrency() {
      FinanceService.getExchangeRate(this.fromCurrency, this.toCurrency)
          .then(response => {
            this.exchangeRate = response.data.rate;
            this.convertedAmount = (this.currencyAmount * this.exchangeRate).toFixed(2);
          })
          .catch(error => {
            console.error('Error converting currency:', error);
          });
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
  padding: 10px;
  border-radius: 5px;
}
.input-sections {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}
.expense-section, .income-section {
  flex: 1;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.list-section {
  margin-bottom: 20px;
}
table {
  width: 100%;
  border-collapse: collapse;
}
th, td {
  border: 1px solid #ddd;
  padding: 8px;
  text-align: left;
}
th {
  background-color: #f2f2f2;
}
</style>
