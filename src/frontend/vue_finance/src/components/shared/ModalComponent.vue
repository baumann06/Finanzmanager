<template>
  <div v-if="visible" class="fixed inset-0 bg-black bg-opacity-50 flex justify-center items-center">
    <div class="bg-white rounded-lg shadow-lg p-6 w-1/3">
      <div class="flex justify-between items-center mb-4">
        <h3 class="text-lg font-bold">{{ title }}</h3>
        <button @click="close" class="text-gray-500 hover:text-gray-700">&times;</button>
      </div>
      <div>
        <slot></slot>
      </div>
      <div class="flex justify-end mt-4">
        <button @click="close" class="bg-gray-300 px-4 py-2 rounded-lg">Close</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'ModalComponent',
  props: {
    visible: {
      type: Boolean,
      required: true
    },
    title: {
      type: String,
      default: 'Modal Title'
    }
  },
  methods: {
    close() {
      this.$emit('close');
    }
  }
};
</script>

<style scoped>
/* Add any additional styling here */
</style>