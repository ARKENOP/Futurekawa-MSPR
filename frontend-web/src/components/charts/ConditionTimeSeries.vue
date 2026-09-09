<script setup lang="ts">
import { computed } from 'vue';
import VChart from 'vue-echarts';
import type { EChartsOption } from 'echarts';
import type { MesureStockage, Pays } from '@/types/api';

const props = defineProps<{ mesures: MesureStockage[]; pays: Pays }>();

function band(ideal: number, tol: number): [{ yAxis: number }, { yAxis: number }] {
  return [{ yAxis: ideal - tol }, { yAxis: ideal + tol }];
}

const option = computed<EChartsOption>(() => {
  const times = props.mesures.map((m) => m.dateHeureMesure.slice(5, 16).replace('T', ' '));
  const temps = props.mesures.map((m) => m.temperatureC);
  const hums = props.mesures.map((m) => m.humiditePourcent);
  return {
    tooltip: { trigger: 'axis' },
    legend: { data: ['Température °C', 'Humidité %'], top: 0 },
    grid: { left: 44, right: 44, top: 36, bottom: 30 },
    xAxis: { type: 'category', data: times },
    yAxis: [
      { type: 'value', name: '°C', scale: true },
      { type: 'value', name: '%', scale: true },
    ],
    series: [
      {
        name: 'Température °C',
        type: 'line',
        data: temps,
        markArea: {
          itemStyle: { color: 'rgba(232,200,154,0.10)' },
          data: [band(props.pays.temperatureIdealeC, props.pays.toleranceTemperatureC)],
        },
        markLine: {
          symbol: 'none',
          lineStyle: { color: 'var(--cherry)', type: 'dashed' },
          data: [
            { yAxis: props.pays.temperatureIdealeC + 2 * props.pays.toleranceTemperatureC },
            { yAxis: props.pays.temperatureIdealeC - 2 * props.pays.toleranceTemperatureC },
          ],
        },
      },
      { name: 'Humidité %', type: 'line', yAxisIndex: 1, data: hums },
    ],
  };
});
</script>

<template>
  <VChart class="ts" :option="option" theme="coffee" autoresize />
</template>

<style scoped>
.ts {
  height: 300px;
  width: 100%;
}
</style>
