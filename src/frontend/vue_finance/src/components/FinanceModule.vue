<template>
  <div>
    <h2>Finanzmanager</h2>

    <!-- Übersichtsbereich mit aktuellem Kontostand -->
    <div class="overview-section">
      <div class="balance">
        <h3>Kontostand</h3>
        <!-- Formatierte Anzeige des aktuellen Kontostands -->
        <p>{{ formatBalance(balance) }} €</p>
      </div>
    </div>

    <!-- Eingabebereiche für neue Ausgaben und Einnahmen -->
    <div class="input-sections">
      <!-- Formular für neue Ausgaben -->
      <div class="expense-section">
        <h3>Neue Ausgabe hinzufügen</h3>
        <div class="form">
          <!-- v-model bindet Eingaben an die newExpense Datenstruktur -->
          <input v-model="newExpense.description" placeholder="Beschreibung" />
          <input v-model="newExpense.amount" type="number" step="0.01" placeholder="Betrag" />
          <input v-model="newExpense.category" placeholder="Kategorie" />
          <input v-model="newExpense.date" type="date" />
          <!-- Button mit Event-Handler für das Hinzufügen von Ausgaben -->
          <button @click="addExpense">Ausgabe hinzufügen</button>
        </div>
      </div>

      <!-- Formular für neue Einnahmen -->
      <div class="income-section">
        <h3>Neue Einnahme hinzufügen</h3>
        <div class="form">
          <!-- Vereinfachtes Formular für Einnahmen (nur Quelle, Betrag, Datum) -->
          <input v-model="newIncome.source" placeholder="Quelle" />
          <input v-model="newIncome.amount" type="number" step="0.01" placeholder="Betrag" />
          <input v-model="newIncome.date" type="date" />
          <button @click="addIncome">Einnahme hinzufügen</button>
        </div>
      </div>
    </div>

    <!-- Ausgabenliste - wird nur angezeigt wenn Ausgaben vorhanden sind -->
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
        <!-- v-for iteriert über alle Ausgaben und erstellt Tabellenzeilen -->
        <tr v-for="expense in expenses" :key="expense.id">
          <td>{{ expense.description }}</td>
          <td>{{ formatAmount(expense.amount) }} €</td>
          <td>{{ expense.category }}</td>
          <td>{{ formatDate(expense.date) }}</td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Einnahmenliste - wird nur angezeigt wenn Einnahmen vorhanden sind -->
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
        <!-- Iteration über alle Einnahmen -->
        <tr v-for="income in incomes" :key="income.id">
          <td>{{ income.source }}</td>
          <td>{{ formatAmount(income.amount) }} €</td>
          <td>{{ formatDate(income.date) }}</td>
        </tr>
        </tbody>
      </table>
    </div>

    <!-- Kategorieübersicht - zeigt Ausgaben gruppiert nach Kategorien -->
    <div v-if="Object.keys(expensesByCategory).length > 0" class="category-section">
      <h3>Ausgaben nach Kategorien</h3>
      <div class="category-list">
        <!-- Iteration über das expensesByCategory Objekt -->
        <div v-for="(amount, category) in expensesByCategory" :key="category" class="category-item">
          <span class="category-name">{{ category }}:</span>
          <span class="category-amount">{{ formatAmount(amount) }} €</span>
        </div>
      </div>
    </div>

    <!-- Währungsrechner-Sektion -->
    <div class="currency-section">
      <h3>Währungsrechner</h3>
      <div class="form">
        <!-- Eingabefelder für Währungsumrechnung -->
        <input v-model="currencyAmount" type="number" step="0.01" placeholder="Betrag" />
        <!-- Dropdown für Ausgangswährung -->
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
        <!-- Dropdown für Zielwährung -->
        <select v-model="toCurrency">
          <option value="USD">USD</option>
          <option value="EUR">EUR</option>
          <option value="GBP">GBP</option>
          <option value="JPY">JPY</option>
          <option value="CHF">CHF</option>
          <option value="CAD">CAD</option>
          <option value="AUD">AUD</option>
        </select>
        <!-- Button wird deaktiviert während der Umrechnung -->
        <button @click="convertCurrency" :disabled="convertingCurrency">
          {{ convertingCurrency ? 'Umrechnung...' : 'Umrechnen' }}
        </button>
        <!-- Ergebnis der Währungsumrechnung -->
        <div v-if="convertedAmount !== null" class="conversion-result">
          <p>
            {{ formatAmount(currencyAmount) }} {{ fromCurrency }} =
            {{ formatAmount(convertedAmount) }} {{ toCurrency }}
          </p>
          <p class="exchange-rate">
            Wechselkurs: 1 {{ fromCurrency }} = {{ formatAmount(exchangeRate) }} {{ toCurrency }}
          </p>
        </div>
        <!-- Fehlerbehandlung für Währungsumrechnung -->
        <div v-if="conversionError" class="error-message">
          <p>Fehler bei der Währungsumrechnung: {{ conversionError }}</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
// Import des FinanceService für Backend-Kommunikation
import FinanceService from '../services/FinanceService';

export default {
  name: 'FinanceModule',
  data() {
    return {
      // Arrays für die Speicherung von Ausgaben und Einnahmen
      expenses: [],
      incomes: [],
      balance: 0,
      // Objekt für kategorisierte Ausgaben (Key: Kategorie, Value: Gesamtbetrag)
      expensesByCategory: {},

      // Formular-Datenstrukturen für neue Einträge
      newExpense: {
        description: '',
        amount: null,
        category: '',
        date: new Date().toISOString().split('T')[0] // Heutiges Datum als Standard
      },
      newIncome: {
        source: '',
        amount: null,
        date: new Date().toISOString().split('T')[0]
      },

      // Währungsrechner-Variablen
      currencyAmount: 100, // Standardbetrag für Umrechnung
      fromCurrency: 'EUR', // Standard Ausgangswährung
      toCurrency: 'USD',   // Standard Zielwährung
      convertedAmount: null,
      exchangeRate: null,
      convertingCurrency: false, // Loading-State für Währungsumrechnung
      conversionError: null
    };
  },

  // Lifecycle Hook - wird beim Erstellen der Komponente aufgerufen
  created() {
    this.fetchData();
  },

  methods: {
    /**
     * Zentrale Methode zum Laden aller Daten
     * Wird beim Start und nach Änderungen aufgerufen
     */
    fetchData() {
      this.fetchExpenses();
      this.fetchIncomes();
      this.fetchBalance();
      this.fetchExpensesByCategory();
    },

    /**
     * Lädt alle Ausgaben vom Backend
     */
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

    /**
     * Lädt alle Einnahmen vom Backend
     */
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

    /**
     * Lädt den aktuellen Kontostand
     */
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

    /**
     * Lädt die Ausgaben gruppiert nach Kategorien
     */
    fetchExpensesByCategory() {
      FinanceService.getExpensesByCategory()
          .then(response => {
            this.expensesByCategory = response.data;
          })
          .catch(error => {
            console.error('Error fetching expenses by category:', error);
          });
    },

    /**
     * Fügt eine neue Ausgabe hinzu
     * Validiert die Eingaben vor dem Senden
     */
    addExpense() {
      // Validierung der Eingaben
      if (!this.newExpense.description || !this.newExpense.amount || !this.newExpense.category) {
        alert('Bitte alle Felder ausfüllen');
        return;
      }

      if (this.newExpense.amount <= 0) {
        alert('Der Betrag muss größer als 0 sein');
        return;
      }

      // Senden der neuen Ausgabe an das Backend
      FinanceService.addExpense(this.newExpense)
          .then(() => {
            // Aktualisierung der Daten und Zurücksetzen des Formulars
            this.fetchData();
            this.resetExpenseForm();
          })
          .catch(error => {
            console.error('Error adding expense:', error);
            alert('Fehler beim Hinzufügen der Ausgabe: ' + error.message);
          });
    },

    /**
     * Fügt eine neue Einnahme hinzu
     * Ähnliche Validierung wie bei Ausgaben
     */
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

    /**
     * Konvertiert Währungen über die Backend-API
     * Behandelt Sonderfälle und Fehler
     */
    convertCurrency() {
      // Validierung des Eingabebetrags
      if (!this.currencyAmount || this.currencyAmount <= 0) {
        alert('Bitte geben Sie einen gültigen Betrag ein');
        return;
      }

      // Sonderfall: gleiche Währungen
      if (this.fromCurrency === this.toCurrency) {
        this.convertedAmount = this.currencyAmount;
        this.exchangeRate = 1;
        return;
      }

      // Loading-State setzen und vorherige Ergebnisse zurücksetzen
      this.convertingCurrency = true;
      this.conversionError = null;
      this.convertedAmount = null;

      // API-Aufruf für Wechselkurs
      FinanceService.getExchangeRate(this.fromCurrency, this.toCurrency)
          .then(response => {
            this.exchangeRate = response.data.rate;
            this.convertedAmount = this.currencyAmount * this.exchangeRate;
            this.convertingCurrency = false;
          })
          .catch(error => {
            console.error('Error converting currency:', error);
            // Detaillierte Fehlerbehandlung mit Fallback-Nachricht
            this.conversionError =
                (error.response && error.response.data && error.response.data.message) ||
                error.message;
            this.convertingCurrency = false;
          });
    },

    /**
     * Setzt das Ausgaben-Formular auf Standardwerte zurück
     */
    resetExpenseForm() {
      this.newExpense = {
        description: '',
        amount: null,
        category: '',
        date: new Date().toISOString().split('T')[0]
      };
    },

    /**
     * Setzt das Einnahmen-Formular auf Standardwerte zurück
     */
    resetIncomeForm() {
      this.newIncome = {
        source: '',
        amount: null,
        date: new Date().toISOString().split('T')[0]
      };
    },

    /**
     * Formatiert Beträge für die Anzeige (2 Dezimalstellen)
     * @param {number} amount - Der zu formatierende Betrag
     * @returns {string} Formatierter Betrag als String
     */
    formatAmount(amount) {
      if (amount === null || amount === undefined) return '0.00';
      return parseFloat(amount).toFixed(2);
    },

    /**
     * Spezielle Formatierung für den Kontostand
     * @param {number} balance - Der Kontostand
     * @returns {string} Formatierter Kontostand
     */
    formatBalance(balance) {
      if (balance === null || balance === undefined) return '0.00';
      const num = parseFloat(balance);
      return num.toFixed(2);
    },

    /**
     * Formatiert Datumsstrings für die deutsche Lokalisierung
     * @param {string} dateString - ISO-Datumsstring
     * @returns {string} Formatiertes Datum (DD.MM.YYYY)
     */
    formatDate(dateString) {
      if (!dateString) return '';
      return new Date(dateString).toLocaleDateString('de-DE');
    }
  }
};
</script>

<style scoped>
/* Übersichtsbereich Styling */
.overview-section {
  margin-bottom: 20px;
}

/* Kontostand-Box mit auffälliger Darstellung */
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

/* Hervorhebung des Kontostands */
.balance p {
  font-size: 1.5em;
  font-weight: bold;
  margin: 0;
  color: #007bff;
}

/* Flexbox-Layout für nebeneinander liegende Eingabebereiche */
.input-sections {
  display: flex;
  gap: 20px;
  margin-bottom: 20px;
}

/* Gleiche Breite für beide Eingabesektionen */
.expense-section, .income-section {
  flex: 1;
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
}

/* Vertikales Flexbox-Layout für Formularelemente */
.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

/* Einheitliches Styling für alle Eingabeelemente */
.form input, .form select, .form button {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 3px;
}

/* Button-Styling mit Hover-Effekten */
.form button {
  background-color: #007bff;
  color: white;
  cursor: pointer;
}

.form button:hover:not(:disabled) {
  background-color: #0056b3;
}

/* Deaktivierte Buttons */
.form button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

/* Abstand zwischen Listen */
.list-section {
  margin-bottom: 20px;
}

/* Kategorieübersicht mit Grid-Layout */
.category-section {
  margin-bottom: 20px;
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
}

/* Responsive Grid für Kategorien */
.category-list {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 10px;
}

/* Einzelne Kategorie-Items */
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

/* Rote Farbe für Ausgabenbeträge */
.category-amount {
  color: #dc3545;
}

/* Währungsrechner-Styling */
.currency-section {
  border: 1px solid #ddd;
  padding: 15px;
  border-radius: 5px;
}

/* Grid-Layout für Währungsrechner-Formular */
.currency-section .form {
  display: grid;
  grid-template-columns: 1fr auto 1fr auto;
  gap: 10px;
  align-items: center;
  max-width: 500px;
}

/* Erfolgs-Styling für Umrechnungsergebnis */
.conversion-result {
  grid-column: 1 / -1;
  margin-top: 10px;
  padding: 10px;
  background-color: #d4edda;
  border: 1px solid #c3e6cb;
  border-radius: 3px;
}

/* Kleinere Schrift für Wechselkurs-Info */
.exchange-rate {
  font-size: 0.9em;
  color: #666;
  margin: 5px 0 0 0;
}

/* Fehler-Styling */
.error-message {
  grid-column: 1 / -1;
  margin-top: 10px;
  padding: 10px;
  background-color: #f8d7da;
  border: 1px solid #f5c6cb;
  border-radius: 3px;
  color: #721c24;
}

/* Tabellen-Styling */
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

/* Header-Styling */
th {
  background-color: #f2f2f2;
  font-weight: bold;
}

/* Abwechselnde Zeilenfarben für bessere Lesbarkeit */
tbody tr:nth-child(even) {
  background-color: #f9f9f9;
}
</style>