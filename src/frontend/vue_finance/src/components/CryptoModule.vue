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
              <p>Aktueller Preis: {{ formatPrice(assetPrices[asset.symbol].price) }} {{ asset.type === 'crypto' ? 'USD' : 'USD' }}</p>
              <p v-if="assetPrices[asset.symbol].change !== undefined && assetPrices[asset.symbol].change !== null">24h Änderung:
                <span :class="getChangeClass(assetPrices[asset.symbol].change)">
                  {{ formatChange(assetPrices[asset.symbol].change) }}
                </span>
              </p>
              <p v-if="assetPrices[asset.symbol].change_percent !== undefined && assetPrices[asset.symbol].change_percent !== null">24h Änderung %:
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
            <p v-if="isLoading(asset.symbol)">Lade Preisdaten...</p>
            <div v-else class="error-info">
              <p>Noch keine Preisdaten geladen</p>
              <button @click="fetchAssetPrice(asset)">Preise laden</button>
            </div>
          </div>

          <div class="notes">
            <p><strong>Notizen:</strong></p>
            <p>{{ asset.notes || 'Keine Notizen' }}</p>
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
import { ref, reactive, onMounted } from 'vue';
import CryptoService from '../services/CryptoService';

export default {
  name: 'CryptoModule',
  setup() {
    // Reactive data
    const watchlist = ref([]);
    const newAsset = reactive({
      symbol: '',
      name: '',
      type: 'crypto',
      notes: ''
    });
    const assetPrices = ref({});
    const selectedAsset = ref(null);
    const priceHistory = ref([]);
    const loadingAssets = ref({});

    // Methods
    const fetchWatchlist = () => {
      CryptoService.getWatchlist()
          .then(response => {
            watchlist.value = response.data;
            // Load price data for all assets
            watchlist.value.forEach(asset => {
              fetchAssetPrice(asset);
            });
          })
          .catch(error => {
            console.error('Fehler beim Laden der Watchlist:', error);
            alert('Fehler beim Laden der Watchlist: ' +
                ((error.response && error.response.data && error.response.data.error) || error.message));
          });
    };

    const addToWatchlist = () => {
      if (!newAsset.symbol || !newAsset.name) {
        alert('Bitte geben Sie Symbol und Name ein.');
        return;
      }

      // Validate and clean up inputs
      const assetToAdd = {
        symbol: newAsset.symbol.trim().toUpperCase(),
        name: newAsset.name.trim(),
        type: newAsset.type,
        notes: newAsset.notes.trim()
      };

      CryptoService.addToWatchlist(assetToAdd)
          .then(() => {
            fetchWatchlist();
            // Reset form
            newAsset.symbol = '';
            newAsset.name = '';
            newAsset.type = 'crypto';
            newAsset.notes = '';
          })
          .catch(error => {
            console.error('Fehler beim Hinzufügen:', error);
            alert('Fehler beim Hinzufügen zur Watchlist: ' +
                ((error.response && error.response.data && error.response.data.error) || error.message));
          });
    };

    const removeFromWatchlist = (id) => {
      CryptoService.removeFromWatchlist(id)
          .then(() => {
            fetchWatchlist();
            // Clean up price data for removed asset
            const removedAsset = watchlist.value.find(asset => asset.id === id);
            if (removedAsset) {
              delete assetPrices.value[removedAsset.symbol];
              delete loadingAssets.value[removedAsset.symbol];
            }
          })
          .catch(error => {
            console.error('Fehler beim Entfernen:', error);
            alert('Fehler beim Entfernen aus der Watchlist: ' +
                ((error.response && error.response.data && error.response.data.error) || error.message));
          });
    };

    const fetchAssetPrice = (asset) => {
      if (!asset || !asset.symbol) {
        console.error('Invalid asset provided to fetchAssetPrice');
        return;
      }

      // Set loading state
      loadingAssets.value[asset.symbol] = true;

      console.log(`Fetching price for ${asset.symbol} as ${asset.type}`);

      CryptoService.getAssetPriceAuto(asset.symbol, asset.type)
          .then(response => {
            console.log(`API Response for ${asset.symbol}:`, response.data);

            if (!response.data || !response.data.success) {
              throw new Error((response.data && response.data.error) || 'API returned unsuccessful response');
            }

            const priceData = response.data.priceData;
            if (!priceData || typeof priceData.price === 'undefined') {
              throw new Error('No valid price data in response');
            }

            // Set the asset price data
            assetPrices.value[asset.symbol] = {
              success: true,
              price: parseFloat(priceData.price),
              change: priceData.change ? parseFloat(priceData.change) : null,
              change_percent: priceData.change_percent ? parseFloat(priceData.change_percent) : null,
              lastUpdated: new Date().toLocaleTimeString()
            };

            console.log(`Successfully loaded price for ${asset.symbol}:`, assetPrices.value[asset.symbol]);
          })
          .catch(error => {
            console.error(`Price fetch error for ${asset.symbol}:`, error);

            assetPrices.value[asset.symbol] = {
              success: false,
              error: simplifyError(error),
              lastRetry: new Date().toLocaleTimeString()
            };
          })
          .finally(() => {
            loadingAssets.value[asset.symbol] = false;
          });
    };

    const simplifyError = error => {
      if (error.response && error.response.data && error.response.data.error) {
        return error.response.data.error;
      } else if (error.response && error.response.status) {
        return `HTTP ${error.response.status}`;
      } else {
        return error.message || 'Unbekannter Fehler';
      }
    };


    const showHistory = (asset) => {
      if (selectedAsset.value && selectedAsset.value.symbol === asset.symbol) {
        selectedAsset.value = null;
        priceHistory.value = [];
        return;
      }

      selectedAsset.value = asset;
      priceHistory.value = [];

      console.log(`Loading history for ${asset.symbol} (${asset.type})`);

      CryptoService.getAssetHistory(asset.symbol, asset.type)
          .then(response => {
            console.log(`History response for ${asset.symbol}:`, response.data);

            if (!response.data.success || !response.data.historyData) {
              throw new Error('No history data available');
            }

            const historyData = response.data.historyData;

            if (asset.type === 'crypto') {
              // Handle CoinGecko format
              const prices = historyData.prices;
              if (prices && Array.isArray(prices)) {
                priceHistory.value = prices.slice(0, 7).map((item) => ({
                  date: new Date(item[0]).toLocaleDateString('de-DE'),
                  price: item[1],
                  close: item[1]
                }));
              }
            } else if (asset.type === 'stock') {
              // Handle TwelveData format
              const values = historyData.values;
              if (values && Array.isArray(values)) {
                priceHistory.value = values.slice(0, 7).map(item => ({
                  date: item.datetime,
                  close: parseFloat(item.close),
                  price: parseFloat(item.close)
                }));
              }
            }

            console.log(`Loaded ${priceHistory.value.length} history points for ${asset.symbol}`);
          })
          .catch(error => {
            console.error(`History fetch error for ${asset.symbol}:`, error);
            alert('Fehler beim Laden der Kursverlaufsdaten: ' + simplifyError(error));
            priceHistory.value = [];
          });
    };

    const isLoading = (symbol) => {
      return loadingAssets.value[symbol] || false;
    };

    const formatPrice = (price) => {
      if (!price || isNaN(price)) return '0.00';
      return parseFloat(price).toFixed(2);
    };

    const formatChange = (change) => {
      if (!change || isNaN(change)) return '0.00';
      const cleanChange = change.toString().replace('%', '');
      return parseFloat(cleanChange).toFixed(2);
    };

    const formatChangePercent = (changePercent) => {
      if (!changePercent || isNaN(changePercent)) return '0.00%';
      if (changePercent.toString().includes('%')) {
        return changePercent.toString();
      }
      return parseFloat(changePercent).toFixed(2) + '%';
    };

    const getChangeClass = (change) => {
      if (!change || isNaN(change)) return '';
      const numericChange = parseFloat(change.toString().replace('%', ''));
      return numericChange >= 0 ? 'positive-change' : 'negative-change';
    };

    const calculateBarHeight = (price) => {
      if (!price || isNaN(price)) return 10;
      // Scale the price to a reasonable height for display
      const scaledHeight = Math.max(parseFloat(price) * 0.1, 10);
      return Math.min(scaledHeight, 200); // Cap at 200px for very high prices
    };

    // Lifecycle
    onMounted(() => {
      fetchWatchlist();
    });

    return {
      watchlist,
      newAsset,
      assetPrices,
      selectedAsset,
      priceHistory,
      loadingAssets,
      fetchWatchlist,
      addToWatchlist,
      removeFromWatchlist,
      fetchAssetPrice,
      showHistory,
      isLoading,
      formatPrice,
      formatChange,
      formatChangePercent,
      getChangeClass,
      calculateBarHeight
    };
  }
};
</script>

<style scoped>
.add-asset-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: 400px;
}

.form input, .form select, .form textarea {
  padding: 10px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form button {
  padding: 12px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
}

.form button:hover {
  background: #0056b3;
}

.asset-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 20px;
}

.asset-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 20px;
  background: white;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 15px;
}

.card-header h4 {
  margin: 0;
  color: #333;
}

.card-header button {
  background: #dc3545;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.card-header button:hover {
  background: #c82333;
}

.price-info {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 4px;
  margin-bottom: 15px;
}

.price-info p {
  margin: 5px 0;
  font-size: 14px;
}

.error-info {
  background: #f8d7da;
  color: #721c24;
  padding: 10px;
  border-radius: 4px;
}

.error-info button {
  background: #007bff;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 5px;
  font-size: 12px;
}

.positive-change {
  color: #28a745;
  font-weight: bold;
}

.negative-change {
  color: #dc3545;
  font-weight: bold;
}

.notes {
  margin-bottom: 15px;
}

.notes p {
  margin: 5px 0;
  font-size: 14px;
}

.price-history {
  margin-top: 20px;
  padding: 15px;
  background: #f8f9fa;
  border-radius: 4px;
}

.chart {
  display: flex;
  align-items: end;
  height: 150px;
  gap: 5px;
  margin-top: 10px;
}

.chart-bar {
  background: linear-gradient(to top, #007bff, #0056b3);
  width: 30px;
  min-height: 10px;
  border-radius: 2px 2px 0 0;
  cursor: pointer;
  transition: opacity 0.2s;
}

.chart-bar:hover {
  opacity: 0.8;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #666;
  font-size: 18px;
}
</style>