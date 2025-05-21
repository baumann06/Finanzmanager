<template>
  <div class="app">
    <NavBar :activeTab="activeTab" @change-tab="changeTab" />

    <div class="content">
      <FinanceModule v-if="activeTab === 'finance'" />
      <CryptoModule v-if="activeTab === 'crypto'" />
    </div>
  </div>
</template>

<script>
import NavBar from './components/NavBar.vue';
import FinanceModule from './components/FinanceModule.vue';
import CryptoModule from './components/CryptoModule.vue';

export default {
  name: 'App',
  components: {
    NavBar,
    FinanceModule,
    CryptoModule
  },
  data() {
    return {
      activeTab: 'finance'
    };
  },
  methods: {
    changeTab(tab) {
      this.activeTab = tab;
    }
  }
};
</script>

<style>
body {
  margin: 0;
  font-family: Arial, sans-serif;
}
.app {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}
.content {
  margin-top: 20px;
}
</style>