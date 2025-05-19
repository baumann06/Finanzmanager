<template>
  <div>
    <h3 class="text-lg font-bold mb-4">Currency Converter</h3>
    <div class="mb-4">
      <input
          type="number"
          v-model="amount"
          placeholder="Amount"
          class="w-full px-4 py-2 border rounded-lg"
      />
    </div>
    <div class="mb-4">
      <select v-model="fromCurrency" class="w-full px-4 py-2 border rounded-lg">
        <option v-for="currency in currencies" :key="currency" :value="currency">
          {{ currency }}
        </option>
      </select>
    </div>
    <div class="mb-4">
      <select v-model="toCurrency" class="w-full px-4 py-2 border rounded-lg">
        <option v-for="currency in currencies" :key="currency" :value="currency">
          {{ currency }}
        </option>
      </select>
    </div>
    <button @click="convert" class="bg-blue-500 text-white px-4 py-2 rounded-lg">
      Convert
    </button>
    <p v-if="result" class="mt-4">Result: {{ result }}</p>
  </div>
</template>

<script>
export default {
  name: 'CurrencyConverter',
  data() {
    return {
      amount: null,
      fromCurrency: 'USD',
      toCurrency: 'EUR',
      result: null,
      currencies: ['USD', 'EUR', 'GBP', 'JPY']
    };
  },
  methods: {
    async convert() {
      if (!this.amount || !this.fromCurrency || !this.toCurrency) return;
      // Simulate conversion logic
      this.result = (this.amount * 0.85).toFixed(2); // Example conversion rate
    }
  }
};
</script>