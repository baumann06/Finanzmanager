<template>
  <div class="app-container">
    <nav class="bg-gray-800 text-white p-4">
      <div class="container mx-auto flex justify-between items-center">
        <div class="text-xl font-bold">FinCrypto</div>
        <ul class="flex space-x-6">
          <li><router-link to="/" class="hover:text-gray-300">Dashboard</router-link></li>
          <li><router-link to="/finance" class="hover:text-gray-300">Finanzen</router-link></li>
          <li><router-link to="/crypto" class="hover:text-gray-300">Krypto</router-link></li>
        </ul>
      </div>
    </nav>

    <div class="container mx-auto p-4">
      <router-view />
    </div>
  </div>
</template>

<script>
export default {
  name: 'App'
}
</script>