<template>
  <div v-if="crypto">
    <h2 class="text-xl font-bold">{{ crypto.name }} ({{ crypto.symbol }})</h2>
    <p>Aktueller Preis: {{ formatCurrency(crypto.aktuellerPreis) }} €</p>
    <p>24h Änderung:
      <span :class="crypto.priceChangePercentage24h >= 0 ? 'text-green-600' : 'text-red-600'">
        {{ crypto.priceChangePercentage24h >= 0 ? '+' : '' }}{{ crypto.priceChangePercentage24h?.toFixed(2) || 0 }}%
      </span>
    </p>
    <chart-component :data="chartData" />
  </div>
  <div v-else>
    <p>Keine Kryptowährung ausgewählt.</p>
  </div>
</template>

<script>
import ChartComponent from '@/components/shared/ChartComponent.vue';

export default {
  name: 'CryptoDetail',
  components: { ChartComponent },
  props: {
    crypto: {
      type: Object,
      default: null
    },
    chartData: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    formatCurrency(value) {
      if (value === undefined || value === null) return '0.00';
      return parseFloat(value).toFixed(2);
    }
  }
};
</script>