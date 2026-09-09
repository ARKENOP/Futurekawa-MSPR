<script setup lang="ts">
import { computed } from 'vue';
import VChart from 'vue-echarts';
import { gaugeOption, roastNiveau } from '@/composables/useRoast';

const props = defineProps<{
  label: string;
  valeur: number;
  ideale: number;
  tolerance: number;
  unite: string;
}>();

const reduceMotion =
  typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches;

const option = computed(() =>
  gaugeOption(
    { valeur: props.valeur, ideale: props.ideale, tolerance: props.tolerance },
    props.unite,
    !reduceMotion,
  ),
);

const niveau = computed(() =>
  roastNiveau({ valeur: props.valeur, ideale: props.ideale, tolerance: props.tolerance }),
);
</script>

<template>
  <div class="gauge">
    <div class="gauge-head">
      <span class="eyebrow">{{ label }}</span>
      <span class="ideal mono">idéal {{ ideale }}{{ unite }} ± {{ tolerance }}</span>
    </div>
    <VChart class="chart" :option="option" theme="coffee" autoresize :class="`n-${niveau}`" />
  </div>
</template>

<style scoped>
.gauge {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.gauge-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
}
.ideal {
  font-size: 0.7rem;
  color: var(--text-muted);
}
.chart {
  height: 190px;
  width: 100%;
}
</style>
