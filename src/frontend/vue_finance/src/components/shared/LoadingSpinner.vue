<template>
  <div class="flex justify-center items-center">
    <div class="spinner-border animate-spin inline-block w-8 h-8 border-4 rounded-full" role="status"></div>
  </div>
</template>

<script>
export default {
  name: 'LoadingSpinner'
};
</script>

<style scoped>
.spinner-border {
  border-top-color: transparent;
  border-right-color: #3498db;
  border-bottom-color: #3498db;
  border-left-color: #3498db;
}
</style>