<template>
  <div>
    <h3 class="text-lg font-bold mb-4">Add Expense</h3>
    <form @submit.prevent="addExpense">
      <input
          type="text"
          v-model="name"
          placeholder="Expense Name"
          class="w-full mb-2 border rounded-lg p-2"
      />
      <input
          type="number"
          v-model="amount"
          placeholder="Amount"
          class="w-full mb-2 border rounded-lg p-2"
      />
      <button type="submit" class="bg-blue-500 text-white px-4 py-2 rounded-lg">Add</button>
    </form>
  </div>
</template>

<script>
export default {
  name: 'AddExpenseForm',
  data() {
    return {
      name: '',
      amount: null
    };
  },
  methods: {
    addExpense() {
      if (this.name.trim() && this.amount > 0) {
        this.$emit('add-expense', { name: this.name.trim(), amount: this.amount });
        this.name = '';
        this.amount = null;
      }
    }
  }
};
</script>