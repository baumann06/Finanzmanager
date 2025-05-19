<!-- App.vue - Die Hauptkomponente der Anwendung -->
<template>
  <div class="app-container">
    <header class="app-header">
      <div class="logo-container">
        <img src="./assets/logo.svg" alt="FinCrypto Logo" class="app-logo" />
        <h1>FinCrypto Dashboard</h1>
      </div>
      <nav class="main-nav">
        <button
            @click="activeTab = 'finance'"
            :class="{ active: activeTab === 'finance' }"
        >
          Finanzen
        </button>
        <button
            @click="activeTab = 'crypto'"
            :class="{ active: activeTab === 'crypto' }"
        >
          Krypto
        </button>
      </nav>
    </header>

    <main class="app-content">
      <FinanceView v-if="activeTab === 'finance'" />
      <CryptoView v-else-if="activeTab === 'crypto'" />
    </main>

    <footer class="app-footer">
      <p>© {{ currentYear }} FinCrypto Dashboard | Alle Rechte vorbehalten</p>
    </footer>
  </div>
</template>

<script>
import FinanceView from './views/FinanceView.vue'
import CryptoView from './views/CryptoView.vue'

export default {
  name: 'App',
  components: {
    FinanceView,
    CryptoView
  },
  data() {
    return {
      activeTab: 'finance',
      currentYear: new Date().getFullYear()
    }
  }
}
</script>

<style>
.app-container {
  display: flex;
  flex-direction: column;
  min-height: 100vh;
  font-family: 'Roboto', Arial, sans-serif;
}

.app-header {
  background-color: #2c3e50;
  color: white;
  padding: 1rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.logo-container {
  display: flex;
  align-items: center;
  gap: 1rem;
}

.app-logo {
  height: 40px;
}

.main-nav {
  display: flex;
  gap: 1rem;
}

.main-nav button {
  background: none;
  border: none;
  color: #ddd;
  font-size: 1rem;
  padding: 0.5rem 1rem;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.3s ease;
}

.main-nav button.active {
  background-color: #42b983;
  color: white;
}

.main-nav button:hover:not(.active) {
  background-color: rgba(255, 255, 255, 0.1);
}

.app-content {
  flex: 1;
  padding: 2rem;
  background-color: #f5f5f5;
}

.app-footer {
  background-color: #2c3e50;
  color: white;
  text-align: center;
  padding: 1rem;
  margin-top: auto;
}
</style>