<template>
  <div>
    <h2>Krypto Watchlist</h2>

    <div class="add-crypto-section">
      <h3>Kryptowährung zur Watchlist hinzufügen</h3>
      <div class="form">
        <input v-model="newCrypto.symbol" placeholder="Symbol (z.B. btc)" />
        <input v-model="newCrypto.name" placeholder="Name (z.B. Bitcoin)" />
        <textarea v-model="newCrypto.notes" placeholder="Notizen"></textarea>
        <button @click="addToWatchlist">Hinzufügen</button>
      </div>
    </div>

    <div v-if="watchlist.length > 0" class="watchlist-section">
      <h3>Meine Watchlist</h3>
      <div class="crypto-cards">
        <div v-for="crypto in watchlist" :key="crypto.id" class="crypto-card">
          <div class="card-header">
            <h4>{{ crypto.name }} ({{ crypto.symbol }})</h4>
            <button @click="removeFromWatchlist(crypto.id)">Entfernen</button>
          </div>

          <div v-if="cryptoPrices[crypto.symbol]" class="price-info">
            <div v-if="cryptoPrices[crypto.symbol].success">
              <p>Aktueller Preis: {{ formatPrice(cryptoPrices[crypto.symbol].priceData.price || cryptoPrices[crypto.symbol].priceData.close) }} USD</p>
              <p v-if="cryptoPrices[crypto.symbol].priceData.change">24h Änderung:
                <span :class="getChangeClass(cryptoPrices[crypto.symbol].priceData.change)">
                  {{ formatChange(cryptoPrices[crypto.symbol].priceData.change) }}
                </span>
              </p>
              <p v-if="cryptoPrices[crypto.symbol].priceData.change_percent">24h Änderung %:
                <span :class="getChangeClass(cryptoPrices[crypto.symbol].priceData.change_percent)">
                  {{ formatChangePercent(cryptoPrices[crypto.symbol].priceData.change_percent) }}
                </span>
              </p>
            </div>
            <div v-else class="error-info">
              <p>Fehler beim Laden der Preisdaten: {{ cryptoPrices[crypto.symbol].error }}</p>
              <button @click="fetchCryptoPrice(crypto.symbol)">Erneut versuchen</button>
            </div>
          </div>

          <div v-else class="price-info">
            <p>Lade Preisdaten...</p>
            <button @click="fetchCryptoPrice(crypto.symbol)">Aktualisieren</button>
          </div>

          <div class="notes">
            <p><strong>Notizen:</strong></p>
            <p>{{ crypto.notes || 'Keine Notizen' }}</p>
          </div>

          <button @click="showHistory(crypto.symbol)">Preisverlauf anzeigen</button>

          <div v-if="selectedCrypto === crypto.symbol && priceHistory.length > 0" class="price-history">
            <h4>Preisverlauf</h4>
            <div class="chart">
              <div
                  v-for="(price, index) in priceHistory"
                  :key="index"
                  class="chart-bar"
                  :style="{ height: calculateBarHeight(price.close || price.price) + 'px' }"
                  :title="price.date + ': ' + formatPrice(price.close || price.price) + ' USD'"
              ></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div v-else class="empty-state">
      <p>Deine Watchlist ist leer. Füge eine Kryptowährung hinzu!</p>
    </div>
  </div>
</template>

<script>
import CryptoService from '../services/CryptoService';

export default {
  name: 'CryptoModule',
  data() {
    return {
      watchlist: [],
      newCrypto: {
        symbol: '',
        name: '',
        notes: ''
      },
      cryptoPrices: {},
      selectedCrypto: null,
      priceHistory: []
    };
  },
  created() {
    this.fetchWatchlist();
  },
  methods: {
    fetchWatchlist() {
      CryptoService.getWatchlist()
          .then(response => {
            this.watchlist = response.data;
            this.watchlist.forEach(crypto => {
              this.fetchCryptoPrice(crypto.symbol);
            });
          })
          .catch(error => {
            console.error('Error fetching watchlist:', error);
            alert('Fehler beim Laden der Watchlist: ' + error.message);
          });
    },
    addToWatchlist() {
      if (!this.newCrypto.symbol || !this.newCrypto.name) {
        alert('Bitte geben Sie mindestens Symbol und Name ein');
        return;
      }

      CryptoService.addToWatchlist(this.newCrypto)
          .then(() => {
            this.fetchWatchlist();
            this.newCrypto = { symbol: '', name: '', notes: '' };
          })
          .catch(error => {
            console.error('Error adding to watchlist:', error);
            alert('Fehler beim Hinzufügen zur Watchlist: ' + error.message);
          });
    },
    removeFromWatchlist(id) {
      CryptoService.removeFromWatchlist(id)
          .then(() => this.fetchWatchlist())
          .catch(error => {
            console.error('Error removing from watchlist:', error);
            alert('Fehler beim Entfernen aus der Watchlist: ' + error.message);
          });
    },
    fetchCryptoPrice(symbol) {
      CryptoService.getCryptoPrice(symbol)
          .then(response => {
            this.cryptoPrices = {
              ...this.cryptoPrices,
              [symbol]: response.data
            };
          })

          .catch(error => {
            console.error(`Error fetching price for ${symbol}:`, error);
            const errorMessage =
                error.response && error.response.data && error.response.data.error
                    ? error.response.data.error
                    : error.message;

            this.$set(this.cryptoPrices, symbol, {
              success: false,
              error: errorMessage
            });
          });
    },
    showHistory(symbol) {
      if (this.selectedCrypto === symbol) {
        this.selectedCrypto = null;
        this.priceHistory = [];
        return;
      }

      this.selectedCrypto = symbol;

      CryptoService.getCryptoHistory(symbol)
          .then(response => {
            if (response.data.success && response.data.historyData) {
              const timeSeries = response.data.historyData['Time Series (Digital Currency Daily)'];
              if (timeSeries) {
                this.priceHistory = Object.keys(timeSeries)
                    .slice(0, 7)
                    .map(date => ({
                      date,
                      close: timeSeries[date]['4a. close (USD)'],
                      price: timeSeries[date]['4a. close (USD)']
                    }));
              } else {
                console.warn('Keine Time Series Daten gefunden');
                this.priceHistory = [];
              }
            } else {
              console.error('Fehler in API Response:', response.data);
              this.priceHistory = [];
            }
          })
          .catch(error => {
            console.error(`Error fetching history for ${symbol}:`, error);
            alert('Fehler beim Laden der Kursverlaufsdaten: ' + error.message);
            this.priceHistory = [];
          });
    },
    formatPrice(price) {
      if (!price) return '0.00';
      return parseFloat(price).toFixed(2);
    },
    formatChange(change) {
      if (!change) return '0.00';
      const cleanChange = change.toString().replace('%', '');
      return parseFloat(cleanChange).toFixed(2);
    },
    formatChangePercent(changePercent) {
      if (!changePercent) return '0.00%';
      return changePercent.toString().includes('%')
          ? changePercent
          : parseFloat(changePercent).toFixed(2) + '%';
    },
    getChangeClass(change) {
      if (!change) return '';
      const numericChange = parseFloat(change.toString().replace('%', ''));
      return numericChange >= 0 ? 'positive-change' : 'negative-change';
    },
    calculateBarHeight(price) {
      if (!price) return 10;
      return Math.max(parseFloat(price) / 1000, 10);
    }
  }
};
</script>

<style scoped>
.add-crypto-section {
  margin-bottom: 20px;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 400px;
}
.watchlist-section {
  margin-top: 20px;
}
.crypto-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: 20px;
}
.crypto-card {
  border: 1px solid #ddd;
  border-radius: 5px;
  padding: 15px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.positive-change {
  color: green;
}
.negative-change {
  color: red;
}
.price-history {
  margin-top: 15px;
}
.chart {
  display: flex;
  align-items: flex-end;
  height: 150px;
  gap: 5px;
  border-bottom: 1px solid #ccc;
  padding-top: 10px;
}
.chart-bar {
  flex: 1;
  background-color: #4caf50;
  min-width: 10px;
}
.notes {
  margin: 10px 0;
  background-color: #f9f9f9;
  padding: 10px;
  border-radius: 3px;
}
.error-info {
  color: red;
}
.error-info button {
  background-color: #ff6b6b;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 3px;
  cursor: pointer;
  margin-top: 5px;
}
.error-info button:hover {
  background-color: #ff5252;
}
</style>
