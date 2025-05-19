<template>
  <div>
    <h3 class="text-lg font-bold mb-4">Monthly Chart</h3>
    <chart-component :data="chartData" />
  </div>
</template>

<script>
import ChartComponent from '@/components/shared/ChartComponent.vue';

export default {
  name: 'MonthlyChart',
  components: { ChartComponent },
  props: {
    chartData: {
      type: Array,
      default: () => []
    }
  }
};
</script>