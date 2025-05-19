<template>
  <div>
    <h3 class="text-lg font-bold mb-4">Add Transaction</h3>
    <form @submit.prevent="addTransaction">
      <input
          type="text"
          v-model="description"
          placeholder="Description"
          class="w-full mb-2 border rounded-lg p-2"
      />
      <input
          type="number"
          v-model="amount"
          placeholder="Amount"
          class="w-full mb-2 border rounded-lg p-2"
      />
      <button type="submit" class="bg-blue-500 text-white px-4 py-2 rounded-lg">
        Add
      </button>
    </form>
  </div>
</template>

<script>
export default {
  name: 'TransactionForm',
  data() {
    return {
      description: '',
      amount: null
    };
  },
  methods: {
    addTransaction() {
      if (this.description.trim() && this.amount > 0) {
        this.$emit('add-transaction', {
          description: this.description.trim(),
          amount: this.amount
        });
        this.description = '';
        this.amount = null;
      }
    }
  }
};
</script>