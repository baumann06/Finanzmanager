<!-- src/views/FinanceView.vue - Finanzübersicht Komponente -->
<template>
  <div class="finance-container">
    <h2>Finanzübersicht</h2>

    <!-- Filter-Bereich -->
    <div class="filter-section">
      <div class="filter-item">
        <label for="month-filter">Monat:</label>
        <select id="month-filter" v-model="selectedMonth">
          <option v-for="(month, index) in months" :key="index" :value="index + 1">
            {{ month }}
          </option>
        </select>
      </div>

      <div class="filter-item">
        <label for="year-filter">Jahr:</label>
        <select id="year-filter" v-model="selectedYear">
          <option v-for="year in years" :key="year" :value="year">
            {{ year }}
          </option>
        </select>
      </div>

      <div class="filter-item">
        <label for="currency-filter">Währung:</label>
        <select id="currency-filter" v-model="selectedCurrency">
          <option v-for="currency in currencies" :key="currency" :value="currency">
            {{ currency }}
          </option>
        </select>
      </div>

      <button @click="applyFilters" class="btn primary">Filter anwenden</button>
    </div>

    <!-- Zusammenfassung Bereich -->
    <div class="summary-section">
      <div class="summary-card income">
        <h3>Einnahmen</h3>
        <p class="amount">{{ formatCurrency(totalIncome, selectedCurrency) }}</p>
      </div>

      <div class="summary-card expenses">
        <h3>Ausgaben</h3>
        <p class="amount">{{ formatCurrency(totalExpenses, selectedCurrency) }}</p>
      </div>

      <div class="summary-card balance">
        <h3>Bilanz</h3>
        <p class="amount" :class="{ 'positive': totalBalance >= 0, 'negative': totalBalance < 0 }">
          {{ formatCurrency(totalBalance, selectedCurrency) }}
        </p>
      </div>
    </div>

    <!-- Diagramm-Bereich -->
    <div class="chart-section">
      <div class="chart-container">
        <h3>Ausgaben nach Kategorie</h3>
        <canvas ref="pieChart"></canvas>
      </div>

      <div class="chart-container">
        <h3>Monatliche Übersicht</h3>
        <canvas ref="lineChart"></canvas>
      </div>
    </div>

    <!-- Transaktions-Tabelle -->
    <div class="transactions-section">
      <div class="section-header">
        <h3>Transaktionen</h3>
        <button @click="showAddTransactionModal = true" class="btn primary">+ Neue Transaktion</button>
      </div>

      <table class="transactions-table">
        <thead>
        <tr>
          <th>Datum</th>
          <th>Beschreibung</th>
          <th>Kategorie</th>
          <th>Betrag</th>
          <th>Währung</th>
          <th>Aktionen</th>
        </tr>
        </thead>
        <tbody>
        <tr v-for="transaction in transactions" :key="transaction.id"
            :class="{ 'income-row': transaction.betrag > 0, 'expense-row': transaction.betrag < 0 }">
          <td>{{ formatDate(transaction.datum) }}</td>
          <td>{{ transaction.beschreibung }}</td>
          <td>{{ getCategoryName(transaction.kategorie) }}</td>
          <td>{{ formatCurrency(transaction.betrag, transaction.waehrung) }}</td>
          <td>{{ transaction.waehrung }}</td>
          <td class="actions">
            <button @click="editTransaction(transaction)" class="btn small">Bearbeiten</button>
            <button @click="deleteTransaction(transaction.id)" class="btn small danger">Löschen</button>
          </td>
        </tr>
        <tr v-if="transactions.length === 0">
          <td colspan="6" class="no-data">Keine Transaktionen gefunden.</td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Modal für neue/bearbeitete Transaktion -->
    <div v-if="showAddTransactionModal" class="modal">
      <div class="modal-content">
        <div class="modal-header">
          <h3>{{ editMode ? 'Transaktion bearbeiten' : 'Neue Transaktion' }}</h3>
          <button @click="closeTransactionModal" class="close-btn">&times;</button>
        </div>

        <div class="modal-body">
          <form @submit.prevent="saveTransaction">
            <div class="form-group">
              <label for="description">Beschreibung:</label>
              <input type="text" id="description" v-model="currentTransaction.beschreibung" required>
            </div>

            <div class="form-group">
              <label for="amount">Betrag:</label>
              <input type="number" id="amount" v-model="currentTransaction.betrag" step="0.01" required>
            </div>

            <div class="form-group">
              <label for="date">Datum:</label>
              <input type="date" id="date" v-model="currentTransaction.datum" required>
            </div>

            <div class="form-group">
              <label for="category">Kategorie:</label>
              <select id="category" v-model="currentTransaction.kategorie.id" required>
                <option v-for="category in categories" :key="category.id" :value="category.id">
                  {{ category.name }}
                </option>
              </select>
            </div>

            <div class="form-group">
              <label for="currency">Währung:</label>
              <select id="currency" v-model="currentTransaction.waehrung">
                <option v-for="currency in currencies" :key="currency" :value="currency">
                  {{ currency }}
                </option>
              </select>
            </div>

            <div class="form-actions">
              <button type="button" @click="closeTransactionModal" class="btn">Abbrechen</button>
              <button type="submit" class="btn primary">Speichern</button>
            </div>
          </form>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios';
import Chart from 'chart.js/auto';

export default {
  name: 'FinanceView',
  data() {
    return {
      transactions: [],
      categories: [],
      exchangeRates: {},
      selectedMonth: new Date().getMonth() + 1,
      selectedYear: new Date().getFullYear(),
      selectedCurrency: 'EUR',
      currencies: ['EUR', 'USD', 'GBP', 'CHF', 'BTC'],
      months: [
        'Januar', 'Februar', 'März', 'April', 'Mai', 'Juni',
        'Juli', 'August', 'September', 'Oktober', 'November', 'Dezember'
      ],
      years: [2023, 2024, 2025],
      totalIncome: 0,
      totalExpenses: 0,
      totalBalance: 0,
      showAddTransactionModal: false,
      editMode: false,
      currentTransaction: this.getEmptyTransaction(),
      pieChart: null,
      lineChart: null
    };
  },
  mounted() {
    this.fetchData();
  },
  methods: {
    async fetchData() {
      try {
        // Fetch categories
        const categoryResponse = await axios.get('http://localhost:8080/api/finance/categories');
        this.categories = categoryResponse.data;

        // Fetch exchange rates
        const ratesResponse = await axios.get('http://localhost:8080/api/finance/exchange-rates');
        this.exchangeRates = ratesResponse.data;

        // Apply initial filters to get transactions
        this.applyFilters();
      } catch (error) {
        console.error('Error fetching initial data:', error);
      }
    },

    async applyFilters() {
      try {
        // Fetch transactions for selected month and year
        const response = await axios.get('http://localhost:8080/api/finance/transactions/month', {
          params: {
            year: this.selectedYear,
            month: this.selectedMonth
          }
        });

        this.transactions = response.data;

        // Calculate summary data
        this.calculateSummary();

        // Update charts
        this.updateCharts();
      } catch (error) {
        console.error('Error applying filters:', error);
      }
    },

    calculateSummary() {
      // Calculate income, expenses and balance
      this.totalIncome = this.transactions
          .filter(t => t.betrag > 0)
          .reduce((sum, t) => sum + this.convertToSelectedCurrency(t.betrag, t.waehrung), 0);

      this.totalExpenses = this.transactions
          .filter(t => t.betrag < 0)
          .reduce((sum, t) => sum + this.convertToSelectedCurrency(t.betrag, t.waehrung), 0);

      this.totalBalance = this.totalIncome + this.totalExpenses;
    },

    updateCharts() {
      // Destroy previous charts if they exist
      if (this.pieChart) this.pieChart.destroy();
      if (this.lineChart) this.lineChart.destroy();

      // Create category pie chart
      this.createPieChart();

      // Create monthly line chart
      this.createLineChart();
    },

    createPieChart() {
      // Group transactions by category
      const categorySummary = {};
      const categoryColors = {};

      this.transactions.forEach(transaction => {
        if (transaction.betrag < 0) { // Only expenses for pie chart
          const categoryName = this.getCategoryName(transaction.kategorie);
          const amount = Math.abs(this.convertToSelectedCurrency(transaction.betrag, transaction.waehrung));

          if (categorySummary[categoryName]) {
            categorySummary[categoryName] += amount;
          } else {
            categorySummary[categoryName] = amount;
            categoryColors[categoryName] = this.getRandomColor();
          }
        }
      });

      const pieCtx = this.$refs.pieChart.getContext('2d');

      this.pieChart = new Chart(pieCtx, {
        type: 'pie',
        data: {
          labels: Object.keys(categorySummary),
          datasets: [{
            data: Object.values(categorySummary),
            backgroundColor: Object.values(categoryColors)
          }]
        },
        options: {
          responsive: true,
          plugins: {
            legend: {
              position: 'right'
            },
            tooltip: {
              callbacks: {
                label: function(context) {
                  const label = context.label || '';
                  const value = context.raw;
                  return `${label}: ${value.toFixed(2)} ${this.selectedCurrency}`;
                }.bind(this)
              }
            }
          }
        }
      });
    },

    createLineChart() {
      // Fetch monthly summary for the year
      axios.get('http://localhost:8080/api/finance/summary/monthly', {
        params: {
          year: this.selectedYear,
          currency: this.selectedCurrency
        }
      }).then(response => {
        const monthlySummary = response.data;
        const months = this.months.map((_, index) => index + 1);
        const values = months.map(month =>
            monthlySummary[month.toString().padStart(2, '0')] || 0
        );

        const lineCtx = this.$refs.lineChart.getContext('2d');

        this.lineChart = new Chart(lineCtx, {
          type: 'line',
          data: {
            labels: this.months,
            datasets: [{
              label: `Monatliche Bilanz (${this.selectedYear})`,
              data: values,
              borderColor: '#42b983',
              backgroundColor: 'rgba(66, 185, 131, 0.1)',
              fill: true,
              tension: 0.4
            }]
          },
          options: {
            responsive: true,
            scales: {
              y: {
                beginAtZero: true,
                ticks: {
                  callback: function(value) {
                    return `${value} ${this.selectedCurrency}`;
                  }.bind(this)
                }
              }
            }
          }
        });
      }).catch(error => {
        console.error('Error fetching monthly summary:', error);
      });
    },

    getEmptyTransaction() {
      return {
        id: null,
        beschreibung: '',
        betrag: 0,
        datum: this.formatDateForInput(new Date()),
        kategorie: { id: '' },
        waehrung: this.selectedCurrency
      };
    },

    editTransaction(transaction) {
      this.editMode = true;
      this.currentTransaction = { ...transaction };
      // Format date for input field
      this.currentTransaction.datum = this.formatDateForInput(new Date(transaction.datum));
      this.showAddTransactionModal = true;
    },

    closeTransactionModal() {
      this.showAddTransactionModal = false;
      this.editMode = false;
      this.currentTransaction = this.getEmptyTransaction();
    },

    async saveTransaction() {
      try {
        if (this.editMode) {
          // Update existing transaction
          await axios.put(
              `http://localhost:8080/api/finance/transactions/${this.currentTransaction.id}`,
              this.currentTransaction
          );
        } else {
          // Create new transaction
          await axios.post('http://localhost:8080/api/finance/transactions', this.currentTransaction);
        }

        // Close modal and refresh data
        this.closeTransactionModal();
        this.applyFilters();
      } catch (error) {
        console.error('Error saving transaction:', error);
      }
    },

    async deleteTransaction(id) {
      if (confirm('Sind Sie sicher, dass Sie diese Transaktion löschen möchten?')) {
        try {
          await axios.delete(`http://localhost:8080/api/finance/transactions/${id}`);
          this.applyFilters();
        } catch (error) {
          console.error('Error deleting transaction:', error);
        }
      }
    },

    getCategoryName(category) {
      if (!category) return 'Unbekannt';
      const foundCategory = this.categories.find(c => c.id === category.id);
      return foundCategory ? foundCategory.name : 'Unbekannt';
    },

    formatDate(dateString) {
      const date = new Date(dateString);
      return date.toLocaleDateString('de-DE');
    },

    formatDateForInput(date) {
      return date.toISOString().split('T')[0];
    },

    formatCurrency(amount, currency) {
      return new Intl.NumberFormat('de-DE', {
        style: 'currency',
        currency: currency || this.selectedCurrency
      }).format(amount);
    },

    convertToSelectedCurrency(amount, fromCurrency) {
      if (fromCurrency === this.selectedCurrency) {
        return amount;
      }

      // Simple conversion using exchange rates
      if (this.exchangeRates[fromCurrency] && this.exchangeRates[this.selectedCurrency]) {
        // Convert to base currency (usually EUR), then to target currency
        const inBaseCurrency = amount / this.exchangeRates[fromCurrency];
        return inBaseCurrency * this.exchangeRates[this.selectedCurrency];
      }

      return amount; // Fallback if no conversion available
    },

    getRandomColor() {
      const letters = '0123456789ABCDEF';
      let color = '#';
      for (let i = 0; i < 6; i++) {
        color += letters[Math.floor(Math.random() * 16)];
      }
      return color;
    }
  }
};
</script>

<style scoped>
.finance-container {
  max-width: 1200px;
  margin: 0 auto;
}

.filter-section {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
  padding: 1rem;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.filter-item {
  display: flex;
  flex-direction: column;
  flex: 1;
}

.filter-item label {
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.filter-item select {
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.summary-section {
  display: flex;
  gap: 1rem;
  margin-bottom: 2rem;
}

.summary-card {
  flex: 1;
  padding: 1.5rem;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
  text-align: center;
}

.summary-card h3 {
  margin-top: 0;
  font-size: 1.2rem;
}

.summary-card .amount {
  font-size: 1.8rem;
  font-weight: bold;
  margin: 0.5rem 0 0;
}

.income {
  background-color: #e6f7ff;
  border-left: 4px solid #1890ff;
}

.expenses {
  background-color: #fff1f0;
  border-left: 4px solid #f5222d;
}

.balance {
  background-color: #f6ffed;
  border-left: 4px solid #52c41a;
}

.amount.positive {
  color: #52c41a;
}

.amount.negative {
  color: #f5222d;
}

.chart-section {
  display: flex;
  gap: 2rem;
  margin-bottom: 2rem;
}

.chart-container {
  flex: 1;
  padding: 1.5rem;
  background-color: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.chart-container h3 {
  margin-top: 0;
  margin-bottom: 1rem;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 1rem;
}

.transactions-section {
  background-color: white;
  border-radius: 8px;
  padding: 1.5rem;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.transactions-table {
  width: 100%;
  border-collapse: collapse;
}

.transactions-table th {
  text-align: left;
  padding: 1rem 0.5rem;
  border-bottom: 2px solid #f0f0f0;
}

.transactions-table td {
  padding: 0.75rem 0.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.transactions-table .income-row {
  background-color: rgba(82, 196, 26, 0.05);
}

.transactions-table .expense-row {
  background-color: rgba(245, 34, 45, 0.05);
}

.actions {
  display: flex;
  gap: 0.5rem;
}

.no-data {
  text-align: center;
  padding: 2rem;
  color: #999;
}

/* Modal Styles */
.modal {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(0,0,0,0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
}

.modal-content {
  background-color: white;
  border-radius: 8px;
  width: 500px;
  max-width: 90%;
  box-shadow: 0 4px 12px rgba(0,0,0,0.15);
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 1rem 1.5rem;
  border-bottom: 1px solid #f0f0f0;
}

.modal-header h3 {
  margin: 0;
}

.close-btn {
  background: none;
  border: none;
  font-size: 1.5rem;
  cursor: pointer;
  color: #999;
}

.modal-body {
  padding: 1.5rem;
}

.form-group {
  margin-bottom: 1rem;
}

.form-group label {
  display: block;
  margin-bottom: 0.5rem;
  font-weight: 500;
}

.form-group input,
.form-group select {
  width: 100%;
  padding: 0.5rem;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 1rem;
  margin-top: 1.5rem;
}

/* Button Styles */
.btn {
  padding: 0.5rem 1rem;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-weight: 500;
  background-color: #f0f0f0;
}

.btn.primary {
  background-color: #1890ff;
  color: white;
}

.btn.danger {
  background-color: #f5222d;
  color: white;
}

.btn.small {
  padding: 0.3rem 0.5rem;
  font-size: 0.85rem;
}
</style>