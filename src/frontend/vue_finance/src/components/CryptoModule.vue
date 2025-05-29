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

        <!-- Investment Amount Field -->
        <div v-if="newAsset.symbol && newAsset.name" class="investment-section">
          <label for="investmentAmount">Investitionsbetrag (USD):</label>
          <input
              id="investmentAmount"
              type="number"
              step="0.01"
              min="0"
              v-model="newAsset.investmentAmount"
              placeholder="0.00"
          />
          <small>Optional: Leer lassen für Watchlist ohne Investition</small>
        </div>

        <textarea v-model="newAsset.notes" placeholder="Notizen"></textarea>
        <button @click="addToWatchlist" :disabled="isAddingAsset">
          {{ isAddingAsset ? 'Wird hinzugefügt...' : 'Hinzufügen' }}
        </button>
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

          <!-- Investment Information -->
          <div v-if="asset.invested_amount && parseFloat(asset.invested_amount) > 0" class="investment-info">
            <div class="investment-summary">
              <h5>💰 Investment Details</h5>
              <p><strong>Investiert:</strong> ${{ formatPrice(asset.invested_amount) }}</p>

              <!-- Portfolio Summary -->
              <div v-if="portfolioSummaries[asset.id]" class="portfolio-details">
                <div v-if="portfolioSummaries[asset.id].success">
                  <p><strong>Aktueller Wert:</strong> ${{ formatPrice(portfolioSummaries[asset.id].data.currentValue) }}</p>
                  <p><strong>Holdings:</strong> {{ formatHoldings(portfolioSummaries[asset.id].data.holdings) }} {{ asset.symbol }}</p>
                  <p><strong>Gewinn/Verlust:</strong>
                    <span :class="getChangeClass(portfolioSummaries[asset.id].data.gainLoss)">
                      ${{ formatChange(portfolioSummaries[asset.id].data.gainLoss) }}
                      ({{ formatChangePercent(portfolioSummaries[asset.id].data.gainLossPercent) }})
                    </span>
                  </p>
                </div>
                <div v-else class="error-info">
                  <p>Portfolio-Fehler: {{ portfolioSummaries[asset.id].error }}</p>
                  <button @click="fetchPortfolioSummary(asset)">Erneut laden</button>
                </div>
              </div>
              <div v-else-if="isLoadingPortfolio(asset.id)" class="loading-info">
                <p>Lade Portfolio-Daten...</p>
              </div>
              <div v-else>
                <button @click="fetchPortfolioSummary(asset)">Portfolio laden</button>
              </div>
            </div>
          </div>

          <!-- Price Information -->
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

          <!-- Add Investment Button for existing assets -->
          <div v-if="!asset.invested_amount || parseFloat(asset.invested_amount) === 0" class="add-investment">
            <div class="investment-form">
              <label>Investition hinzufügen:</label>
              <input
                  type="number"
                  step="0.01"
                  min="0"
                  v-model="investmentAmounts[asset.id]"
                  placeholder="Betrag in USD"
                  style="margin: 5px 0; padding: 8px; width: 150px;"
              />
              <button
                  @click="addInvestmentToAsset(asset)"
                  :disabled="!investmentAmounts[asset.id] || parseFloat(investmentAmounts[asset.id]) <= 0 || isAddingInvestment[asset.id]"
                  style="margin-left: 10px; padding: 8px 15px; background-color: #4CAF50; color: white; border: none; border-radius: 4px; cursor: pointer;"
              >
                {{ isAddingInvestment[asset.id] ? 'Wird hinzugefügt...' : 'Investieren' }}
              </button>
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
    const watchlist = ref([]);
    const newAsset = reactive({
      symbol: '',
      name: '',
      type: 'crypto',
      notes: '',
      investmentAmount: null
    });
    const assetPrices = ref({});
    const selectedAsset = ref(null);
    const priceHistory = ref([]);
    const loadingAssets = ref({});
    const portfolioSummaries = ref({});
    const loadingPortfolio = ref({});
    const investmentAmounts = ref({});
    const isAddingAsset = ref(false);
    const isAddingInvestment = ref({});

    const fetchWatchlist = async () => {
      try {
        console.log('Fetching watchlist...');
        const response = await CryptoService.getWatchlist();
        const assets = Array.isArray(response.data) ? response.data : [];
        console.log('Received assets:', assets);
        watchlist.value = assets;
        assets.forEach(asset => {
          if (asset.symbol) {
            fetchAssetPrice(asset);
            if (asset.invested_amount && parseFloat(asset.invested_amount) > 0) {
              fetchPortfolioSummary(asset);
            }
          }
        });
      } catch (error) {
        console.error('Error loading watchlist:', error);
        const errorMessage = (error.response && error.response.data && error.response.data.message) ||
            (error.response && error.response.data && error.response.data.error) ||
            error.message ||
            'Unknown error';
        alert('Fehler beim Laden der Watchlist: ' + errorMessage);
        watchlist.value = [];
      }
    };

    const addToWatchlist = async () => {
      if (!newAsset.symbol || !newAsset.name) {
        alert('Bitte geben Sie Symbol und Name ein.');
        return;
      }

      isAddingAsset.value = true;

      try {
        const assetToAdd = {
          symbol: newAsset.symbol.trim().toUpperCase(),
          name: newAsset.name.trim(),
          type: newAsset.type,
          notes: newAsset.notes.trim()
        };

        const investmentAmount = newAsset.investmentAmount && parseFloat(newAsset.investmentAmount) > 0
            ? parseFloat(newAsset.investmentAmount)
            : null;

        console.log('Adding asset:', assetToAdd, 'with investment:', investmentAmount);

        if (investmentAmount) {
          await CryptoService.addInvestment(assetToAdd, investmentAmount);
        } else {
          await CryptoService.addToWatchlist(assetToAdd);
        }

        console.log('Asset added successfully');
        await fetchWatchlist();
        resetForm();

      } catch (error) {
        console.error('Error adding asset:', error);
        const errorMessage = (error.response && error.response.data && error.response.data.message) ||
            (error.response && error.response.data && error.response.data.error) ||
            error.message ||
            'Unknown error';
        alert('Fehler beim Hinzufügen zur Watchlist: ' + errorMessage);
      } finally {
        isAddingAsset.value = false;
      }
    };

    const addInvestmentToAsset = async (asset) => {
      const amount = investmentAmounts.value[asset.id];
      if (!amount || parseFloat(amount) <= 0) {
        alert('Bitte geben Sie einen gültigen Investitionsbetrag ein.');
        return;
      }

      isAddingInvestment.value[asset.id] = true;

      try {
        console.log('Adding investment to asset:', asset, 'amount:', amount);
        await CryptoService.addInvestment(asset, parseFloat(amount));
        console.log('Investment added successfully');

        await fetchWatchlist();
        investmentAmounts.value[asset.id] = null;

      } catch (error) {
        console.error('Error adding investment:', error);
        const errorMessage = (error.response && error.response.data && error.response.data.message) ||
            (error.response && error.response.data && error.response.data.error) ||
            error.message ||
            'Unknown error';
        alert('Fehler beim Hinzufügen der Investition: ' + errorMessage);
      } finally {
        isAddingInvestment.value[asset.id] = false;
      }
    };

    const fetchPortfolioSummary = async (asset) => {
      loadingPortfolio.value[asset.id] = true;

      try {
        const response = await CryptoService.getPortfolioSummary(asset.id);
        portfolioSummaries.value[asset.id] = {
          success: true,
          data: response.data
        };
      } catch (error) {
        console.error('Error loading portfolio summary:', error);
        const errorMessage = (error.response && error.response.data && error.response.data.message) ||
            (error.response && error.response.data && error.response.data.error) ||
            error.message ||
            'Unknown error';
        portfolioSummaries.value[asset.id] = {
          success: false,
          error: errorMessage
        };
      } finally {
        loadingPortfolio.value[asset.id] = false;
      }
    };

    const resetForm = () => {
      newAsset.symbol = '';
      newAsset.name = '';
      newAsset.type = 'crypto';
      newAsset.notes = '';
      newAsset.investmentAmount = null;
    };

    const removeFromWatchlist = async (id) => {
      try {
        console.log('Removing asset with id:', id);
        await CryptoService.removeFromWatchlist(id);
        console.log('Asset removed successfully');

        const removedAsset = watchlist.value.find(asset => asset.id === id);
        if (removedAsset) {
          delete assetPrices.value[removedAsset.symbol];
          delete loadingAssets.value[removedAsset.symbol];
          delete portfolioSummaries.value[id];
          delete loadingPortfolio.value[id];
          delete investmentAmounts.value[id];
          delete isAddingInvestment.value[id];
        }

        await fetchWatchlist();

      } catch (error) {
        console.error('Error removing asset:', error);
        const errorMessage = (error.response && error.response.data && error.response.data.message) ||
            (error.response && error.response.data && error.response.data.error) ||
            error.message ||
            'Unknown error';
        alert('Fehler beim Entfernen aus der Watchlist: ' + errorMessage);
      }
    };

    const fetchAssetPrice = async (asset) => {
      if (!(asset && asset.symbol)) {
        console.error('Invalid asset provided to fetchAssetPrice');
        return;
      }

      loadingAssets.value[asset.symbol] = true;

      try {
        console.log(`Fetching price for ${asset.symbol} as ${asset.type}`);
        const response = await CryptoService.getAssetPriceAuto(asset.symbol, asset.type);

        if (!(response.data && response.data.success) || !response.data.priceData) {
          throw new Error((response.data && response.data.error) || 'API returned unsuccessful response');
        }

        const priceData = response.data.priceData;
        if (typeof priceData.price === 'undefined') {
          throw new Error('No valid price data in response');
        }

        assetPrices.value[asset.symbol] = {
          success: true,
          price: parseFloat(priceData.price),
          change: priceData.change ? parseFloat(priceData.change) : null,
          change_percent: priceData.change_percent ? parseFloat(priceData.change_percent) : null,
          lastUpdated: new Date().toLocaleTimeString()
        };

        console.log(`Successfully loaded price for ${asset.symbol}`);

      } catch (error) {
        console.error(`Price fetch error for ${asset.symbol}:`, error);
        assetPrices.value[asset.symbol] = {
          success: false,
          error: getErrorMessage(error),
          lastRetry: new Date().toLocaleTimeString()
        };
      } finally {
        loadingAssets.value[asset.symbol] = false;
      }
    };

    const showHistory = async (asset) => {
      if (selectedAsset.value && selectedAsset.value.symbol === asset.symbol) {
        selectedAsset.value = null;
        priceHistory.value = [];
        return;
      }

      selectedAsset.value = asset;
      priceHistory.value = [];

      try {
        console.log(`Loading history for ${asset.symbol} (${asset.type})`);
        const response = await CryptoService.getAssetHistory(asset.symbol, asset.type);

        if (!(response.data && response.data.success) || !response.data.historyData) {
          throw new Error('No history data available');
        }

        const historyData = response.data.historyData;

        if (asset.type === 'crypto') {
          const prices = historyData.prices;
          if (prices && Array.isArray(prices)) {
            priceHistory.value = prices.slice(0, 7).map((item) => ({
              date: new Date(item[0]).toLocaleDateString('de-DE'),
              price: item[1],
              close: item[1]
            }));
          }
        } else if (asset.type === 'stock') {
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

      } catch (error) {
        console.error(`History fetch error for ${asset.symbol}:`, error);
        alert('Fehler beim Laden der Kursverlaufsdaten: ' + getErrorMessage(error));
        priceHistory.value = [];
      }
    };

    const getErrorMessage = (error) => {
      return (error.response && error.response.data && error.response.data.error) ||
          (error.response && error.response.data && error.response.data.message) ||
          error.message ||
          'Unknown error';
    };

    const isLoading = (symbol) => loadingAssets.value[symbol] || false;
    const isLoadingPortfolio = (assetId) => loadingPortfolio.value[assetId] || false;

    const formatPrice = (price) => {
      if (!price || isNaN(price)) return '0.00';
      return parseFloat(price).toFixed(2);
    };

    const formatHoldings = (holdings) => {
      if (!holdings || isNaN(holdings)) return '0.00000000';
      return parseFloat(holdings).toFixed(8);
    };

    const formatChange = (change) => {
      if (!change || isNaN(change)) return '0.00';
      const cleanChange = change.toString().replace('%', '');
      const numericChange = parseFloat(cleanChange);
      return (numericChange >= 0 ? '+' : '') + numericChange.toFixed(2);
    };

    const formatChangePercent = (changePercent) => {
      if (!changePercent || isNaN(changePercent)) return '0.00%';
      const numericChange = parseFloat(changePercent.toString().replace('%', ''));
      return (numericChange >= 0 ? '+' : '') + numericChange.toFixed(2) + '%';
    };

    const getChangeClass = (change) => {
      if (!change || isNaN(change)) return '';
      const numericChange = parseFloat(change.toString().replace('%', ''));
      return numericChange >= 0 ? 'positive-change' : 'negative-change';
    };

    const calculateBarHeight = (price) => {
      if (!price || isNaN(price)) return 10;
      const scaledHeight = Math.max(parseFloat(price) * 0.1, 10);
      return Math.min(scaledHeight, 200);
    };

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
      portfolioSummaries,
      loadingPortfolio,
      investmentAmounts,
      isAddingAsset,
      isAddingInvestment,
      fetchWatchlist,
      addToWatchlist,
      addInvestmentToAsset,
      fetchPortfolioSummary,
      removeFromWatchlist,
      fetchAssetPrice,
      showHistory,
      isLoading,
      isLoadingPortfolio,
      formatPrice,
      formatHoldings,
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
  background: #f5f5f5;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 30px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 15px;
  max-width: 500px;
}

.form input,
.form select,
.form textarea {
  padding: 12px;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-size: 14px;
}

.form button {
  padding: 12px 20px;
  background-color: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
}

.form button:hover:not(:disabled) {
  background-color: #0056b3;
}

.form button:disabled {
  background-color: #6c757d;
  cursor: not-allowed;
}

.investment-section {
  border: 1px solid #e0e0e0;
  padding: 15px;
  border-radius: 4px;
  background: white;
}

.investment-section label {
  font-weight: 500;
  margin-bottom: 5px;
  display: block;
}

.investment-section small {
  color: #666;
  font-style: italic;
}

.asset-cards {
  display: grid;
  gap: 20px;
  grid-template-columns: repeat(auto-fit, minmax(400px, 1fr));
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
  padding: 6px 12px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 12px;
}

.investment-info {
  background: #f8f9fa;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 15px;
}

.investment-summary h5 {
  margin: 0 0 10px 0;
  color: #495057;
}

.portfolio-details {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #e9ecef;
}

.price-info {
  background: #e9ecef;
  padding: 12px;
  border-radius: 6px;
  margin-bottom: 15px;
}

.error-info {
  color: #dc3545;
  font-size: 14px;
}

.error-info button {
  background: #ffc107;
  color: #212529;
  border: none;
  padding: 4px 8px;
  border-radius: 3px;
  cursor: pointer;
  font-size: 12px;
  margin-top: 5px;
}

.loading-info {
  color: #6c757d;
  font-style: italic;
}

.add-investment {
  background: #e8f5e8;
  padding: 15px;
  border-radius: 6px;
  margin-bottom: 15px;
}

.investment-form {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.investment-form label {
  font-weight: 500;
  color: #495057;
}

.notes {
  padding-top: 10px;
  border-top: 1px solid #e9ecef;
}

.notes p {
  margin: 5px 0;
}

.empty-state {
  text-align: center;
  padding: 40px;
  color: #6c757d;
  font-style: italic;
}
</style>