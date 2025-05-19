<template>
  <div>
    <div class="mb-4">
      <div class="relative">
        <input
            type="text"
            v-model="symbol"
            placeholder="BTC, ETH, ADA, ..."
            class="w-full px-4 py-2 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 dark:bg-gray-700 dark:border-gray-600 dark:text-white"
        />
        <button
            @click="addCrypto"
            class="absolute right-0 top-0 h-full px-4 bg-blue-500 text-white rounded-r-lg hover:bg-blue-600 flex items-center"
            :disabled="isLoading"
        >
          <span v-if="isLoading" class="inline-block w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin mr-2"></span>
          Hinzufügen
        </button>
      </div>
      <p v-if="error" class="text-red-500 text-sm mt-1">{{ error }}</p>
      <p class="text-gray-500 dark:text-gray-400 text-sm mt-1">
        Gib das Symbol einer Kryptowährung ein, um sie zu deiner Watchlist hinzuzufügen.
      </p>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CryptoForm',
  data() {
    return {
      symbol: '',
      error: '',
      isLoading: false
    };
  },
  methods: {
    async addCrypto() {
      if (!this.symbol.trim()) {
        this.error = 'Bitte gib ein Symbol ein';
        return;
      }

      this.error = '';
      this.isLoading = true;

      try {
        await this.$emit('add-crypto', this.symbol.trim());
        this.symbol = ''; // Zurücksetzen nach erfolgreichem Hinzufügen
      } catch (error) {
        this.error = 'Fehler beim Hinzufügen: Ungültiges Symbol oder API-Fehler';
        console.error('Fehler beim Hinzufügen der Kryptowährung:', error);
      } finally {
        this.isLoading = false;
      }
    }
  }
};
</script>