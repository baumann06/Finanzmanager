<template>
  <div>
    <navbar />

    <div class="container mx-auto px-4 py-6">
      <h1 class="text-3xl font-bold mb-6">Dashboard</h1>

      <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
        <!-- Finanzen-Überblick -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-xl font-semibold">Finanzübersicht</h2>
            <router-link to="/finance" class="text-blue-500 hover:text-blue-700">
              Mehr anzeigen →
            </router-link>
          </div>

          <div v-if="loading" class="flex justify-center py-10">
            <loading-spinner />
          </div>

          <div v-else>
            <div class="mb-4">
              <p class="text-gray-600 dark:text-gray-400">Gesamtbilanz</p>
              <p :class="[
                'text-2xl font-bold',
                totalBalance >= 0 ? 'text-green-600' : 'text-red-600'
              ]">
                {{ totalBalance.toFixed(2) }} €
              </p>
            </div>

            <monthly-chart
                v-if="Object.keys(monthlySummary).length > 0"
                :chart-data="monthlySummary"
                class="h-64"
            />
          </div>
        </div>

        <!-- Krypto-Überblick -->
        <div class="bg-white dark:bg-gray-800 rounded-lg shadow p-6">
          <div class="flex justify-between items-center mb-4">
            <h2 class="text-xl font-semibold">Krypto-Watchlist</h2>
            <router-link to="/crypto" class="text-blue-500 hover:text-blue-700">
              Mehr anzeigen →
            </router-link>
          </div>

          <div v-if="loading" class="flex justify-center py-10">
            <loading-spinner />
          </div>

          <div v-else-if="cryptoEntries.length === 0" class="py-10 text-center">
            <p class="text-gray-600 dark:text-gray-400">
              Keine Kryptowährungen in der Watchlist.
            </p>
            <router-link
                to="/crypto"
                class="mt-2 inline-block px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
            >
              Kryptowährungen hinzufügen
            </router-link>
          </div>

          <div v-else>
            <div class="space-y-4">
              <div
                  v-for="crypto in cryptoEntries.slice(0, 5)"
                  :key="crypto.id"
                  class="flex justify-between items-center border-b border-gray-200 dark:border-gray-700 pb-2"
              >
                <div class="flex items-center">
                  <img
                      :src="`https://coinicons-api.vercel.app/api/icon/${crypto.symbol.toLowerCase()}`"
                      :alt="crypto.symbol"
                      class="w-8 h-8 mr-2"
                      onerror="this.src='https://via.placeholder.com/32?text=' + this.alt"
                  />
                  <div>
                    <p class="font-medium">{{ crypto.name }}</p>
                    <p class="text-sm text-gray-600 dark:text-gray-400">{{ crypto.symbol }}</p>
                  </div>
                </div>
                <div class="text-right">
                  <p class="font-medium">{{ formatCurrency(crypto.aktuellerPreis) }} €</p>
                  <p :class="[
                    'text-sm',
                    crypto.priceChangePercentage24h >= 0 ? 'text-green-600' : 'text-red-600'
                  ]">
                    {{ crypto.priceChangePercentage24h >= 0 ? '+' : '' }}{{ crypto.priceChangePercentage24h?.toFixed(2) || 0 }}%
                  </p>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Neueste Transaktionen -->
      <div class="mt-8 bg-white dark:bg-gray-800 rounded-lg shadow p-6">
        <div class="flex justify-between items-center mb-4">
          <h2 class="text-xl font-semibold">Neueste Transaktionen</h2>
          <router-link to="/finance" class="text-blue-500 hover:text-blue-700">
            Alle anzeigen →
          </router-link>
        </div>

        <div v-if="loading" class="flex justify-center py-10">
          <loading-spinner />
        </div>

        <div v-else-if="transactions.length === 0" class="py-10 text-center">
          <p class="text-gray-600 dark:text-gray-400">
            Keine Transaktionen vorhanden.
          </p>
          <router-link
              to="/finance"
              class="mt-2 inline-block px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600"
          >
            Transaktion hinzufügen
          </router-link>
        </div>

        <div v-else class="overflow-x-auto">
          <table class="min-w-full divide-y divide-gray-200 dark:divide-gray-700">
            <thead>
            <tr>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Datum
              </th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Beschreibung
              </th>
              <th class="px-4 py-3 text-left text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Kategorie
              </th>
              <th class="px-4 py-3 text-right text-xs font-medium text-gray-500 dark:text-gray-400 uppercase tracking-wider">
                Betrag
              </th>
            </tr>
            </thead>
            <tbody class="divide-y divide-gray-200 dark:divide-gray-700">
            <tr v-for="transaction in latestTransactions" :key="transaction.id">
              <td class="px-4 py-3 whitespace-nowrap">
                {{ formatDate(transaction.datum) }}
              </td>
              <td class="px-4 py-3">
                {{ transaction.beschreibung }}
              </td>
              <td class="px-4 py-3">
                {{ getCategoryName(transaction.kategorie?.id) }}
              </td>
              <td :class="[
                  'px-4 py-3 text-right whitespace-nowrap font-medium',
                  transaction.betrag >= 0 ? 'text-green-600' : 'text-red-600'
                ]">
                {{ formatCurrency(transaction.betrag) }} {{ transaction.waehrung }}
              </td>
            </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex';
import Navbar from '@/components/shared/Navbar.vue';
import LoadingSpinner from '@/components/shared/LoadingSpinner.vue';
import MonthlyChart from '@/components/finance/MonthlyChart.vue';

export default {
  name: 'HomeView',
  components: {
    Navbar,
    LoadingSpinner,
    MonthlyChart
  },
  computed: {
    ...mapState({
      loading: state => state.loading,
      transactions: state => state.finance.transactions,
      monthlySummary: state => state.finance.monthlySummary,
      cryptoEntries: state => state.crypto.cryptoEntries
    }),
    ...mapGetters({
      totalBalance: 'finance/getTotalBalance',
      getCategoryName: 'finance/getCategoryName'
    }),
    latestTransactions() {
      return [...this.transactions]
          .sort((a, b) => new Date(b.datum) - new Date(a.datum))
          .slice(0, 5);
    }
  },
  methods: {
    ...mapActions({
      fetchTransactions: 'finance/fetchTransactions',
      fetchCategories: 'finance/fetchCategories',
      fetchMonthlySummary: 'finance/fetchMonthlySummary',
      fetchCryptoEntries: 'crypto/fetchCryptoEntries'
    }),
    formatDate(dateStr) {
      if (!dateStr) return '';
      const date = new Date(dateStr);
      return date.toLocaleDateString('de-DE');
    },
    formatCurrency(value) {
      if (value === undefined || value === null) return '0.00';
      return parseFloat(value).toFixed(2);
    }
  },
  async created() {
    try {
      await this.fetchCategories();
      await this.fetchTransactions();
      await this.fetchMonthlySummary({
        year: new Date().getFullYear(),
        currency: 'EUR'
      });
      await this.fetchCryptoEntries();
    } catch (error) {
      console.error('Fehler beim Laden der Daten:', error);
    }
  }
};
</script>