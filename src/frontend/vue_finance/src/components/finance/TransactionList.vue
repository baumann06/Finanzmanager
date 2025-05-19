<template>
  <div>
    <h3 class="text-lg font-bold mb-4">Transactions</h3>
    <ul>
      <li v-for="transaction in transactions" :key="transaction.id" class="mb-2">
        <p>{{ transaction.description }} - {{ formatCurrency(transaction.amount) }}</p>
        <button @click="$emit('delete-transaction', transaction.id)" class="text-red-500">
          Delete
        </button>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'TransactionList',
  props: {
    transactions: {
      type: Array,
      default: () => []
    }
  },
  methods: {
    formatCurrency(value) {
      return new Intl.NumberFormat('de-DE', { style: 'currency', currency: 'EUR' }).format(value);
    }
  }
};
</script>