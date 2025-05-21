<template>
  <div class="navbar">
    <div
        class="nav-item"
        :class="{ active: activeTab === 'finance' }"
        @click="$emit('change-tab', 'finance')"
    >
      Finanzen
    </div>
    <div
        class="nav-item"
        :class="{ active: activeTab === 'crypto' }"
        @click="$emit('change-tab', 'crypto')"
    >
      Krypto
    </div>
  </div>
</template>

<script>
export default {
  name: 'NavBar',
  props: {
    activeTab: String
  }
};
</script>

<style scoped>
.navbar {
  display: flex;
  border-bottom: 1px solid #ddd;
}
.nav-item {
  padding: 10px 20px;
  cursor: pointer;
}
.nav-item.active {
  font-weight: bold;
  border-bottom: 2px solid #333;
}
</style>
