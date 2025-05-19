<template>
  <div class="divide-y divide-gray-200 dark:divide-gray-700">
    <div v-if="cryptos.length === 0" class="py-4 text-center">
      <p class="text-gray-500 dark:text-gray-400">
        Keine Kryptowährungen in der Watchlist.
      </p>
    </div>

    <div
        v-for="crypto in cryptos"
        :key="crypto.id"
        @click="$emit('select-crypto', crypto.id)"
        :class="[
        'p-4 cursor-pointer hover:bg-gray-50 dark:hover:bg-gray-700 flex justify-between items-center',
        selectedCryptoId === crypto.id ? 'bg-blue-50 dark:bg-gray-700' : ''
      ]"
    >
      <div class="flex items-center">
        <img
            :src="`https://coinicons-api.vercel.app/api/icon/${crypto.symbol.toLowerCase()}`"
            :alt="crypto.symbol"
            class="w-8 h-8 mr-3"
            onerror="this.src='https://via.placeholder.com/32?text=' + this.alt"
        />
        <div>
          <p class="font-medium">{{ crypto.name }}</p>
          <p class="text-sm text-gray-600 dark:text-gray-400">{{ crypto.symbol }}</p>
        </div>
      </div>

      <div class="flex items-center">
        <div class="text-right mr-4">
          <p class="font-medium">{{ formatCurrency(crypto.aktuellerPreis) }} €</p>
          <p :class="[
            'text-sm',
            crypto.priceChangePercentage24h >= 0 ? 'text-green-600' : 'text-red-600'
          ]">
            {{ crypto.priceChangePercentage24h >= 0 ? '+' : '' }}{{ crypto.priceChangePercentage24h?.toFixed(2) || 0 }}%
          </p>
        </div>

        <button
            @click.stop="$emit('remove-crypto', crypto.id)"
            class="p-1 text-gray-500 hover:text-red-500 dark:text-gray-400 dark:hover:text-red-400"
            title="Von Watchlist entfernen"
        >
          <svg xmlns="http://www.w3.org/2000/svg" class="h-5 w-5" viewBox="0 0 20 20" fill="currentColor">
            <path fill-rule="evenodd" d="M9 2a1 1 0 00-.894.553L7.382 4H4a1 1 0 000 2v10a2 2 0 002 2h8a2 2 0 002-2V6a1 1 0 100-2h-3.382l-.724-1.447A1 1 0 0011 2H9zM7 8a1 1 0 012 0v6a1 1 0 11-2 0V8zm5-1a1 1 0 00-1 1v6a1 1 0 102 0V8a1 1 0 00-1-1z" clip-rule="evenodd" />
          </svg>
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CryptoList',
  props: {
    cryptos: {
      type: Array,
      required: true,
      default: () => []
    },
    selectedCryptoId: {
      type: [Number, String],
      default: null
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