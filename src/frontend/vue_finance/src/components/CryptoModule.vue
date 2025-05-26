<template>
  <div>
    <h2>Watchlist</h2>

    <div class="add-asset-section">
      <h3>Asset zur Watchlist hinzufügen</h3>
      <div class="form">
        <input v-model="newAsset.symbol" placeholder="Symbol (z.B. BTC oder AAPL)" />
        <input v-model="newAsset.name" placeholder="Name (z.B. Bitcoin oder Apple)" />

        <!-- Dropdown nur anzeigen, wenn symbol und name gesetzt sind -->
        <div v-if="newAsset.symbol && newAsset.name" style="margin: 10px 0;">
          <label for="assetType">Typ wählen:</label>
          <select id="assetType" v-model="newAsset.type">
            <option value="crypto">Kryptowährung</option>
            <option value="stock">Aktie</option>
          </select>
        </div>

        <textarea v-model="newAsset.notes" placeholder="Notizen"></textarea>
        <button @click="addToWatchlist">Hinzufügen</button>
      </div>
    </div>

    <div v-if="watchlist.length > 0" class="watchlist-section">
      <h3>Meine Watchlist</h3>
      <div class="asset-cards">
        <div v-for="asset in watchlist" :key="asset.id" class="asset-card">
          <div class="card-header">
            <h4>{{ asset.name }} ({{ asset.symbol }}) - {{ asset.type === 'crypto' ? 'Krypto' : 'Aktie' }}</h4>
            <button @click="removeFromWatchlist(asset.id)">Entfernen</button>
          </div>

          <div v-if="assetPrices[asset.symbol]" class="price-info">
            <div v-if="assetPrices[asset.symbol].success">
              <p>Aktueller Preis: {{ formatPrice(assetPrices[asset.symbol].price) }} USD</p>
              <p v-if="assetPrices[asset.symbol].change !== undefined">24h Änderung:
                <span :class="getChangeClass(assetPrices[asset.symbol].change)">
                  {{ formatChange(assetPrices[asset.symbol].change) }}
                </span>
              </p>
              <p v-if="assetPrices[asset.symbol].change_percent !== undefined">24h Änderung %:
                <span :class="getChangeClass(assetPrices[asset.symbol].change_percent)">
                  {{ formatChangePercent(assetPrices[asset.symbol].change_percent) }}
                </span>
              </p>
            </div>
            <div v-else class="error-info">
              <p>Fehler beim Laden der Preisdaten: {{ assetPrices[asset.symbol].error }}</p>
              <button @click="fetchAssetPrice(asset)">Erneut versuchen</button>
            </div>
          </div>

          <div v-else class="price-info">
            <p>Lade Preisdaten...</p>
            <button @click="fetchAssetPrice(asset)">Aktualisieren</button>
          </div>

          <div class="notes">
            <p><strong>Notizen:</strong></p>
            <p>{{ asset.notes || 'Keine Notizen' }}</p>
          </div>

          <button @click="showHistory(asset)">Preisverlauf anzeigen</button>

          <div v-if="selectedAsset && selectedAsset.symbol === asset.symbol && priceHistory.length > 0" class="price-history">
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
      <p>Deine Watchlist ist leer. Füge ein Asset hinzu!</p>
    </div>
  </div>
</template>

<script>
import CryptoService from '../services/CryptoService'; // Dein Service (erweitern um Aktien-Methoden)

export default {
  name: 'WatchlistModule',
  data() {
    return {
      watchlist: [],
      newAsset: {
        symbol: '',
        name: '',
        type: 'crypto',  // Default auf Krypto
        notes: ''
      },
      assetPrices: {},  // { symbol: { success: true/false, price: x, change: y, change_percent: z } }
      selectedAsset: null,
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
            this.watchlist.forEach(asset => {
              this.fetchAssetPrice(asset);
            });
          })
          .catch(error => {
            console.error('Fehler beim Laden der Watchlist:', error);
            alert('Fehler beim Laden der Watchlist: ' + error.message);
          });
    },
    addToWatchlist() {
      if (!this.newAsset.symbol || !this.newAsset.name) {
        alert('Bitte geben Sie Symbol und Name ein.');
        return;
      }

      CryptoService.addToWatchlist(this.newAsset)
          .then(() => {
            this.fetchWatchlist();
            this.newAsset = { symbol: '', name: '', type: 'crypto', notes: '' };
          })
          .catch(error => {
            console.error('Fehler beim Hinzufügen:', error);
            alert('Fehler beim Hinzufügen zur Watchlist: ' + error.message);
          });
    },
    removeFromWatchlist(id) {
      CryptoService.removeFromWatchlist(id)
          .then(() => this.fetchWatchlist())
          .catch(error => {
            console.error('Fehler beim Entfernen:', error);
            alert('Fehler beim Entfernen aus der Watchlist: ' + error.message);
          });
    },
    fetchAssetPrice(asset) {
      if (asset.type === 'crypto') {
        CryptoService.getCryptoPrice(asset.symbol)
            .then(response => {
              // response.data = { success: true, priceData: {...} }
              const priceData = response.data.priceData || {};
              this.assetPrices = {
                ...this.assetPrices,
                [asset.symbol]: {
                  success: response.data.success,
                  price: priceData.price || priceData.close,
                  change: priceData.change,
                  change_percent: priceData.change_percent
                }
              };
            })
            .catch(error => {
              console.error(`Fehler bei Krypto-Preis für ${asset.symbol}:`, error);
              this.$set(this.assetPrices, asset.symbol, { success: false, error: error.message });
            });
      } else if (asset.type === 'stock') {
        CryptoService.getStockPrice(asset.symbol)
            .then(response => {
              // response.data = { success: true, priceData: {...} }
              const priceData = response.data.priceData || {};
              this.assetPrices = {
                ...this.assetPrices,
                [asset.symbol]: {
                  success: response.data.success,
                  price: priceData.price || priceData.close,
                  change: priceData.change,
                  change_percent: priceData.change_percent
                }
              };
            })
            .catch(error => {
              console.error(`Fehler bei Aktien-Preis für ${asset.symbol}:`, error);
              this.$set(this.assetPrices, asset.symbol, { success: false, error: error.message });
            });
      }
    },
    showHistory(asset) {
      if (this.selectedAsset && this.selectedAsset.symbol === asset.symbol) {
        this.selectedAsset = null;
        this.priceHistory = [];
        return;
      }

      this.selectedAsset = asset;

      if (asset.type === 'crypto') {
        CryptoService.getCryptoHistory(asset.symbol)
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
                  this.priceHistory = [];
                }
              } else {
                this.priceHistory = [];
              }
            })
            .catch(error => {
              console.error(`Fehler beim Laden History Krypto für ${asset.symbol}:`, error);
              alert('Fehler beim Laden der Kursverlaufsdaten: ' + error.message);
              this.priceHistory = [];
            });
      } else if (asset.type === 'stock') {
        CryptoService.getStockHistory(asset.symbol)
            .then(response => {
              if (response.data.success && response.data.historyData) {
                const timeSeries = response.data.historyData['Time Series (Daily)'];
                if (timeSeries) {
                  this.priceHistory = Object.keys(timeSeries)
                      .slice(0, 7)
                      .map(date => ({
                        date,
                        close: timeSeries[date]['4. close'],
                        price: timeSeries[date]['4. close']
                      }));
                } else {
                  this.priceHistory = [];
                }
              } else {
                this.priceHistory = [];
              }
            })
            .catch(error => {
              console.error(`Fehler beim Laden History Aktie für ${asset.symbol}:`, error);
              alert('Fehler beim Laden der Kursverlaufsdaten: ' + error.message);
              this.priceHistory = [];
            });
      }
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
