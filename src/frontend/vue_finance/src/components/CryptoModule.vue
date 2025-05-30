<template>
  <div>
    <h2>Watchlist</h2>

    <!-- Asset hinzufügen Sektion -->
    <div class="add-asset-section">
      <h3>Asset zur Watchlist hinzufügen</h3>
      <div class="form">
        <input v-model="newAsset.symbol" placeholder="Symbol (z.B. BTC oder AAPL)" />
        <input v-model="newAsset.name" placeholder="Name (z.B. Bitcoin oder Apple)" />

        <!-- Typ-Dropdown nur bei vollständigen Eingaben anzeigen -->
        <div v-if="newAsset.symbol && newAsset.name" style="margin: 10px 0;">
          <label for="assetType">Typ wählen:</label>
          <select id="assetType" v-model="newAsset.type">
            <option value="crypto">Kryptowährung</option>
            <option value="stock">Aktie</option>
          </select>
        </div>

        <!-- Investitionsbetrag (optional) -->
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

    <!-- Watchlist anzeigen -->
    <div v-if="watchlist.length > 0" class="watchlist-section">
      <h3>Meine Watchlist</h3>
      <div class="asset-cards">
        <div v-for="asset in watchlist" :key="asset.id" class="asset-card">
          <div class="card-header">
            <h4>{{ asset.name }} ({{ asset.symbol }}) - {{ asset.type === 'crypto' ? 'Krypto' : 'Aktie' }}</h4>
            <button @click="removeFromWatchlist(asset)">Entfernen</button>
          </div>

          <!-- Investment-Details (nur wenn investiert) -->
          <div v-if="hasInvestment(asset)" class="investment-info">
            <div class="investment-summary">
              <h5>Investment Details</h5>
              <p><strong>Investiert:</strong> ${{ formatPrice(getInvestedAmount(asset)) }}</p>
              <p><strong>Holdings:</strong> {{ formatHoldings(getTotalHoldings(asset)) }} {{ asset.symbol }}</p>
            </div>
          </div>

          <!-- Preis-Informationen -->
          <div class="price-info">
            <div v-if="assetPrices[asset.symbol] && assetPrices[asset.symbol].success">
              <p><strong>Aktueller Preis:</strong> ${{ formatPrice(assetPrices[asset.symbol].price) }}</p>
              <p v-if="assetPrices[asset.symbol].change_percent"><strong>24h Änderung:</strong>
                <span :class="getChangeClass(assetPrices[asset.symbol].change_percent)">
                  {{ formatChangePercent(assetPrices[asset.symbol].change_percent) }}
                </span>
              </p>
              <small v-if="assetPrices[asset.symbol].lastUpdated">
                Zuletzt aktualisiert: {{ assetPrices[asset.symbol].lastUpdated }}
              </small>
            </div>
            <div v-else-if="assetPrices[asset.symbol] && assetPrices[asset.symbol].error" class="error-info">
              <p>Fehler beim Laden der Preisdaten: {{ assetPrices[asset.symbol].error }}</p>
              <button @click="fetchAssetPrice(asset)" class="retry-btn">Erneut versuchen</button>
            </div>
            <div v-else-if="isLoading(asset.symbol)" class="loading-info">
              <p>Lade Preisdaten...</p>
            </div>
            <div v-else>
              <button @click="fetchAssetPrice(asset)" class="load-btn">Preise laden</button>
            </div>
          </div>

          <!-- Investment hinzufügen (nur wenn noch nicht investiert) -->
          <div v-if="!hasInvestment(asset)" class="add-investment">
            <h5>Investition hinzufügen</h5>
            <div class="investment-form">
              <input
                  type="number"
                  step="0.01"
                  min="0"
                  v-model="investmentAmounts[asset.id]"
                  placeholder="Betrag in USD"
                  class="investment-input"
              />
              <button
                  @click="addInvestmentToAsset(asset)"
                  :disabled="!canAddInvestment(asset)"
                  class="invest-btn"
                  :class="{ 'disabled': !canAddInvestment(asset) }"
              >
                {{ isAddingInvestment[asset.id] ? 'Wird hinzugefügt...' : 'Investieren' }}
              </button>
            </div>
          </div>

          <!-- Notizen -->
          <div class="notes">
            <p><strong>Notizen:</strong> {{ asset.notes || 'Keine Notizen' }}</p>
          </div>
        </div>
      </div>
    </div>

    <!-- Leere Watchlist -->
    <div v-else class="empty-state">
      <p>Deine Watchlist ist leer. Füge ein Asset hinzu!</p>
    </div>
  </div>
</template>

<script>
import { ref, reactive, onMounted } from 'vue';
import CryptoService from '@/services/CryptoService';

export default {
  name: 'CryptoModule',
  setup() {
    // === REACTIVE STATE ===
    const watchlist = ref([]);
    const newAsset = reactive({
      symbol: '',
      name: '',
      type: 'crypto',
      notes: '',
      investmentAmount: null
    });

    // Zustandsverwaltung für Preise und UI
    const assetPrices = ref({});
    const loadingAssets = ref({});
    const investmentAmounts = ref({});
    const isAddingAsset = ref(false);
    const isAddingInvestment = ref({});

    // === HELPER FUNCTIONS === (Updated to match backend data structure)

    /**
     * Prüft ob ein Asset eine Investition hat
     */
    const hasInvestment = (asset) => {
      // Prüfe explizites hasInvestment Flag
      if (asset.hasInvestment !== undefined) {
        return asset.hasInvestment;
      }

      // Fallback: prüfe investedAmount (aus Console-Output sichtbar)
      const invested = getInvestedAmount(asset);
      return invested > 0;
    };

    /**
     * Holt investierten Betrag - basierend auf Console-Output
     */
    const getInvestedAmount = (asset) => {
      // Hauptfeld aus Backend (Console zeigt: investedAmount)
      if (asset.investedAmount !== undefined) {
        return parseFloat(asset.investedAmount) || 0;
      }

      // Fallback-Optionen
      if (asset.invested_amount !== undefined) {
        return parseFloat(asset.invested_amount) || 0;
      }

      return 0;
    };

    /**
     * Holt Holdings - basierend auf Console-Output
     */
    const getTotalHoldings = (asset) => {
      // Hauptfeld aus Backend (Console zeigt: totalHoldings)
      if (asset.totalHoldings !== undefined) {
        return parseFloat(asset.totalHoldings) || 0;
      }

      // Fallback für andere mögliche Feldnamen
      if (asset.totalAmount !== undefined) {
        return parseFloat(asset.totalAmount) || 0;
      }

      if (asset.total_holdings !== undefined) {
        return parseFloat(asset.total_holdings) || 0;
      }

      return 0;
    };

    /**
     * Holt durchschnittlichen Kaufpreis - basierend auf Console-Output
     */
    const getAverageBuyPrice = (asset) => {
      // Hauptfeld aus Backend (Console zeigt: averageBuyPrice)
      if (asset.averageBuyPrice !== undefined) {
        return parseFloat(asset.averageBuyPrice) || 0;
      }

      // Fallback-Optionen
      if (asset.averagePrice !== undefined) {
        return parseFloat(asset.averagePrice) || 0;
      }

      if (asset.average_buy_price !== undefined) {
        return parseFloat(asset.average_buy_price) || 0;
      }

      return 0;
    };

    /**
     * Holt Transaktionsanzahl - basierend auf Console-Output
     */
    const getTransactionCount = (asset) => {
      // Hauptfeld aus Backend (Console zeigt: transactionCount)
      if (asset.transactionCount !== undefined) {
        return parseInt(asset.transactionCount) || 0;
      }

      // Fallback-Optionen
      if (asset.transaction_count !== undefined) {
        return parseInt(asset.transaction_count) || 0;
      }

      return 0;
    };

    /**
     * Berechnet aktuellen Wert der Holdings
     */
    const getCurrentValue = (asset) => {
      const holdings = getTotalHoldings(asset);
      const price = assetPrices.value[asset.symbol];

      if (!holdings || !price || !price.success || !price.price) {
        return 0;
      }

      return holdings * price.price;
    };

    /**
     * Berechnet Gewinn/Verlust in absoluten Zahlen
     */
    const getProfitLoss = (asset) => {
      const currentValue = getCurrentValue(asset);
      const investedAmount = getInvestedAmount(asset);

      return currentValue - investedAmount;
    };

    /**
     * Berechnet Gewinn/Verlust in Prozent
     */
    const getProfitLossPercent = (asset) => {
      const investedAmount = getInvestedAmount(asset);
      const profitLoss = getProfitLoss(asset);

      if (investedAmount === 0) return 0;

      return (profitLoss / investedAmount) * 100;
    };

    /**
     * Prüft ob eine Investition hinzugefügt werden kann
     */
    const canAddInvestment = (asset) => {
      const amount = investmentAmounts.value[asset.id];
      return amount && parseFloat(amount) > 0 && !isAddingInvestment.value[asset.id];
    };

    /**
     * Behandelt API-Fehler einheitlich
     */
    const handleError = (error, context) => {
      console.error(`${context} error:`, error);
      let errorMessage = 'Unbekannter Fehler';

      if (error.response && error.response.data) {
        if (error.response.data.message) {
          errorMessage = error.response.data.message;
        } else if (error.response.data.error) {
          errorMessage = error.response.data.error;
        }
      } else if (error.message) {
        errorMessage = error.message;
      }

      alert(`Fehler beim ${context}: ${errorMessage}`);
    };

    // === MAIN FUNCTIONS ===

    /**
     * Lädt die komplette Watchlist vom Server
     */
    const fetchWatchlist = async () => {
      try {
        const response = await CryptoService.getWatchlist();
        const assets = Array.isArray(response.data) ? response.data : [];
        watchlist.value = assets;

        console.log('Loaded watchlist with calculated fields:', assets);

        // Debug-Ausgabe für jedes Asset
        assets.forEach((asset, index) => {
          console.log(`Asset ${index}:`, {
            symbol: asset.symbol,
            name: asset.name,
            investedAmount: asset.investedAmount,
            totalHoldings: asset.totalHoldings,
            averageBuyPrice: asset.averageBuyPrice,
            transactionCount: asset.transactionCount,
            hasInvestment: hasInvestment(asset),
            notes: asset.notes || 'Keine Notizen'
          });
        });

        // Preise für alle Assets laden
        assets.forEach(asset => {
          if (asset.symbol) {
            fetchAssetPrice(asset);
          }
        });
      } catch (error) {
        handleError(error, 'Laden der Watchlist');
        watchlist.value = [];
      }
    };

    /**
     * Fügt neues Asset zur Watchlist hinzu
     */
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

        // Asset mit oder ohne Investition hinzufügen
        if (investmentAmount) {
          await CryptoService.addInvestment(assetToAdd, investmentAmount);
        } else {
          await CryptoService.addToWatchlist(assetToAdd);
        }

        await fetchWatchlist(); // Lädt neue berechnete Werte
        resetForm();

      } catch (error) {
        handleError(error, 'Hinzufügen zur Watchlist');
      } finally {
        isAddingAsset.value = false;
      }
    };

    /**
     * Fügt Investition zu bestehendem Asset hinzu
     */
    const addInvestmentToAsset = async (asset) => {
      const amount = investmentAmounts.value[asset.id];
      if (!amount || parseFloat(amount) <= 0) {
        alert('Bitte geben Sie einen gültigen Investitionsbetrag ein.');
        return;
      }

      isAddingInvestment.value[asset.id] = true;

      try {
        await CryptoService.addInvestment(asset, parseFloat(amount));
        await fetchWatchlist(); // Lädt aktualisierte berechnete Werte
        investmentAmounts.value[asset.id] = null;
      } catch (error) {
        handleError(error, 'Hinzufügen der Investition');
      } finally {
        isAddingInvestment.value[asset.id] = false;
      }
    };

    /**
     * Entfernt Asset aus der Watchlist
     */
    const removeFromWatchlist = async (asset) => {
      if (!confirm(`Möchten Sie "${asset.name}" (${asset.symbol}) wirklich aus der Watchlist entfernen?`)) {
        return;
      }

      try {
        await CryptoService.removeFromWatchlist(asset.id);
        await fetchWatchlist();
      } catch (error) {
        handleError(error, 'Entfernen aus der Watchlist');
      }
    };

    /**
     * Lädt aktuellen Preis für ein Asset
     */
    const fetchAssetPrice = async (asset) => {
      if (loadingAssets.value[asset.symbol]) return;

      loadingAssets.value[asset.symbol] = true;

      try {
        console.log(`Fetching price for ${asset.symbol}...`);
        const response = await CryptoService.getAssetPriceAuto(asset.symbol);
        console.log(`Price response for ${asset.symbol}:`, response.data);

        // Speichere nur den Preis-Wert
        assetPrices.value[asset.symbol] = {
          price: response.data.priceData.price,
          success: true
        };
      } catch (error) {
        console.error(`Error fetching price for ${asset.symbol}:`, error);
        assetPrices.value[asset.symbol] = {
          success: false,
          error: error.message || 'Preis nicht verfügbar'
        };
      } finally {
        loadingAssets.value[asset.symbol] = false;
      }
    };

    /**
     * Setzt das Formular zurück
     */
    const resetForm = () => {
      Object.assign(newAsset, {
        symbol: '',
        name: '',
        type: 'crypto',
        notes: '',
        investmentAmount: null
      });
    };

    // === UTILITY FUNCTIONS ===

    const isLoading = (symbol) => loadingAssets.value[symbol] || false;

    /**
     * Formatiert Preise für die Anzeige
     */
    const formatPrice = (price) => {
      if (!price || isNaN(price)) return '0.00';
      return parseFloat(price).toLocaleString('de-DE', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
      });
    };

    /**
     * Formatiert Holdings für die Anzeige
     */
    const formatHoldings = (holdings) => {
      if (!holdings || isNaN(holdings)) return '0.00000000';
      return parseFloat(holdings).toFixed(8);
    };

    /**
     * Formatiert Änderungen für die Anzeige
     */
    const formatChange = (change) => {
      if (!change || isNaN(change)) return '0.00';
      const numericChange = parseFloat(change);
      return (numericChange >= 0 ? '+' : '') + numericChange.toFixed(2);
    };

    /**
     * Formatiert prozentuale Änderungen
     */
    const formatChangePercent = (changePercent) => {
      if (!changePercent || isNaN(changePercent)) return '0.00%';
      const numericChange = parseFloat(changePercent);
      return (numericChange >= 0 ? '+' : '') + numericChange.toFixed(2) + '%';
    };

    /**
     * Bestimmt CSS-Klasse basierend auf Änderung (positiv/negativ)
     */
    const getChangeClass = (change) => {
      if (!change || isNaN(change)) return '';
      const numericChange = parseFloat(change);
      if (numericChange > 0) return 'positive-change';
      if (numericChange < 0) return 'negative-change';
      return '';
    };

    /**
     * Bestimmt CSS-Klasse für Gewinn/Verlust
     */
    const getProfitLossClass = (asset) => {
      const profitLoss = getProfitLoss(asset);
      if (profitLoss > 0) return 'positive-change';
      if (profitLoss < 0) return 'negative-change';
      return '';
    };

    // === LIFECYCLE ===
    onMounted(() => {
      fetchWatchlist();
    });

    // === RETURN ===
    return {
      // State
      watchlist,
      newAsset,
      assetPrices,
      loadingAssets,
      investmentAmounts,
      isAddingAsset,
      isAddingInvestment,

      // Helper functions
      hasInvestment,
      getInvestedAmount,
      getTotalHoldings,
      getAverageBuyPrice,
      getTransactionCount,
      getCurrentValue,
      getProfitLoss,
      getProfitLossPercent,
      canAddInvestment,

      // Main functions
      fetchWatchlist,
      addToWatchlist,
      addInvestmentToAsset,
      removeFromWatchlist,
      fetchAssetPrice,
      resetForm,

      // Utility functions
      isLoading,
      formatPrice,
      formatHoldings,
      formatChange,
      formatChangePercent,
      getChangeClass,
      getProfitLossClass
    };
  }
};
</script>

<style scoped>
.add-asset-section {
  background: #f8f9fa;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
}

.form {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.form input, .form textarea, .form select {
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.form button {
  padding: 10px;
  background: #007bff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
}

.form button:hover {
  background: #0056b3;
}

.form button:disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.asset-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(400px, 1fr));
  gap: 20px;
}

.asset-card {
  border: 1px solid #ddd;
  border-radius: 8px;
  padding: 15px;
  background: white;
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
}

.investment-info {
  background: #e8f5e8;
  padding: 10px;
  border-radius: 6px;
  margin-bottom: 15px;
}

.investment-summary h5 {
  margin: 0 0 10px 0;
  color: #155724;
}

.performance-info {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #c3e6c3;
}

.price-info {
  background: #f8f9fa;
  padding: 10px;
  border-radius: 6px;
  margin: 10px 0;
}

.error-info {
  background: #f8d7da;
  color: #721c24;
}

.loading-info {
  background: #fff3cd;
  color: #856404;
}

.retry-btn, .load-btn {
  background: #6c757d;
  color: white;
  border: none;
  padding: 5px 10px;
  border-radius: 4px;
  cursor: pointer;
  margin-top: 5px;
}

.add-investment {
  background: #e3f2fd;
  padding: 10px;
  border-radius: 6px;
  margin: 10px 0;
}

.investment-form {
  display: flex;
  gap: 10px;
  align-items: center;
}

.investment-input {
  flex: 1;
  padding: 5px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.invest-btn {
  background: #28a745;
  color: white;
  border: none;
  padding: 5px 15px;
  border-radius: 4px;
  cursor: pointer;
}

.invest-btn.disabled {
  background: #6c757d;
  cursor: not-allowed;
}

.notes {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #eee;
}

.positive-change {
  color: #28a745;
  font-weight: bold;
}

.negative-change {
  color: #dc3545;
  font-weight: bold;
}

.empty-state {
  text-align: center;
  color: #6c757d;
  font-style: italic;
  padding: 40px;
}
</style>