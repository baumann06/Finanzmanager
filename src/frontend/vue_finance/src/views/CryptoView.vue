<template>
  <div>
    <navbar />

    <div class="container mx-auto px-4 py-6">
      <h1 class="text-3xl font-bold mb-6">Kryptowährungen</h1>

      <div class="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <!-- Linke Spalte: Krypto-Liste und Formular -->
        <div class="lg:col-span-1">
          <div class="bg-white dark:bg-gray-800 rounded-lg shadow mb-6">
            <div class="p-4 border-b dark:border-gray-700">
              <h2 class="text-xl font-semibold">Watchlist</h2>
            </div>

            <div class="p-4">
              <crypto-form @add-crypto="addCrypto" />
            </div>

            <div v-if="loading" class="flex justify-center py-6">
              <loading-spinner />
            </div>

            <div v-else>
              <crypto-list
                  :cryptos="cryptoEntries"
                  :selected-crypto-id="selectedCrypto?.id"
                  @select-crypto="selectCrypto"
                  @remove-crypto="removeCrypto"
              />
            </div>
          </div>
        </div>

        <!-- Rechte Spalte: Krypto-Details -->
        <div class="lg:col-span-2">
          <div v-if="loading" class="flex justify-center py-10">
            <loading-spinner />
          </div>

          <div v-else-if="!selectedCrypto" class="bg-white dark:bg-gray-800 rounded-lg shadow p-6 text-center">
            <p class="text-gray-600 dark:text-gray-400 text-lg">
              Bitte wähle eine Kryptowährung aus der Watchlist aus
              oder füge eine neue hinzu.
            </p>
          </div>

          <div v-else>
            <crypto-detail
                :crypto="selectedCrypto"
                :notes="getNotesForCrypto(selectedCrypto.id)"
                @add-note="addNote"
                @update-note="updateNote"
                @delete-note="deleteNote"
            />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapGetters, mapActions } from 'vuex';
import Navbar from '@/components/shared/Navbar.vue';
import LoadingSpinner from '@/components/shared/LoadingSpinner.vue';
import CryptoList from '@/components/crypto/CryptoList.vue';
import CryptoForm from '@/components/crypto/CryptoForm.vue';
import CryptoDetail from '@/components/crypto/CryptoDetail.vue';

export default {
  name: 'CryptoView',
  components: {
    Navbar,
    LoadingSpinner,
    CryptoList,
    CryptoForm,
    CryptoDetail
  },
  data() {
    return {
      selectedCryptoId: null
    };
  },
  computed: {
    ...mapState({
      loading: state => state.loading,
      cryptoEntries: state => state.crypto.cryptoEntries,
      storedSelectedCrypto: state => state.crypto.selectedCrypto
    }),
    ...mapGetters({
      getCryptoById: 'crypto/getCryptoById',
      getNotesForCrypto: 'crypto/getNotesForCrypto'
    }),
    selectedCrypto() {
      return this.selectedCryptoId ? this.getCryptoById(this.selectedCryptoId) : this.storedSelectedCrypto;
    }
  },
  methods: {
    ...mapActions({
      fetchCryptoEntries: 'crypto/fetchCryptoEntries',
      fetchCryptoById: 'crypto/fetchCryptoById',
      addCryptoToWatchlist: 'crypto/addCryptoToWatchlist',
      removeCryptoFromWatchlist: 'crypto/removeCryptoFromWatchlist',
      fetchNotesForCrypto: 'crypto/fetchNotesForCrypto',
      addCryptoNote: 'crypto/addNote',
      updateCryptoNote: 'crypto/updateNote',
      deleteCryptoNote: 'crypto/deleteNote'
    }),
    async selectCrypto(cryptoId) {
      this.selectedCryptoId = cryptoId;
      try {
        await this.fetchCryptoById(cryptoId);
        await this.fetchNotesForCrypto(cryptoId);
      } catch (error) {
        console.error('Fehler beim Laden der Kryptowährung:', error);
      }
    },
    async addCrypto(symbol) {
      try {
        const crypto = await this.addCryptoToWatchlist(symbol);
        this.selectCrypto(crypto.id);
      } catch (error) {
        console.error('Fehler beim Hinzufügen der Kryptowährung:', error);
      }
    },
    async removeCrypto(id) {
      try {
        await this.removeCryptoFromWatchlist(id);
        if (this.selectedCryptoId === id) {
          this.selectedCryptoId = null;
        }
      } catch (error) {
        console.error('Fehler beim Entfernen der Kryptowährung:', error);
      }
    },
    async addNote(text) {
      if (!this.selectedCryptoId) return;

      try {
        await this.addCryptoNote({
          cryptoId: this.selectedCryptoId,
          text
        });
      } catch (error) {
        console.error('Fehler beim Hinzufügen der Notiz:', error);
      }
    },
    async updateNote({ noteId, text }) {
      if (!this.selectedCryptoId) return;

      try {
        await this.updateCryptoNote({
          cryptoId: this.selectedCryptoId,
          noteId,
          text
        });
      } catch (error) {
        console.error('Fehler beim Aktualisieren der Notiz:', error);
      }
    },
    async deleteNote(noteId) {
      if (!this.selectedCryptoId) return;

      try {
        await this.deleteCryptoNote({
          cryptoId: this.selectedCryptoId,
          noteId
        });
      } catch (error) {
        console.error('Fehler beim Löschen der Notiz:', error);
      }
    }
  },
  async created() {
    try {
      await this.fetchCryptoEntries();

      // Wenn Kryptowährungen vorhanden sind, wähle die erste aus
      if (this.cryptoEntries.length > 0) {
        this.selectCrypto(this.cryptoEntries[0].id);
      }
    } catch (error) {
      console.error('Fehler beim Laden der Kryptowährungen:', error);
    }
  }
};
</script>