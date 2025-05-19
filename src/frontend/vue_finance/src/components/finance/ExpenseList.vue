<template>
  <div>
    <h3 class="text-lg font-bold mb-4">Expenses</h3>
    <ul>
      <li v-for="expense in expenses" :key="expense.id" class="mb-2">
        <p>{{ expense.name }} - {{ formatCurrency(expense.amount) }}</p>
        <button @click="$emit('delete-expense', expense.id)" class="text-red-500">Delete</button>
      </li>
    </ul>
  </div>
</template>

<script>
export default {
  name: 'ExpenseList',
  props: {
    expenses: {
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