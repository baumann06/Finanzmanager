<template>
  <div>
    <h3 class="text-lg font-bold mb-4">Category Chart</h3>
    <chart-component :data="chartData" />
  </div>
</template>

<script>
import ChartComponent from '@/components/shared/ChartComponent.vue';

export default {
  name: 'CategoryChart',
  components: { ChartComponent },
  props: {
    chartData: {
      type: Array,
      default: () => []
    }
  }
};
</script>