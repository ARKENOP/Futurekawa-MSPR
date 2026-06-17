<script setup lang="ts">
import { computed } from 'vue';

const props = defineProps<{ value: string }>();

const colorMap: Record<string, string> = {
  // StatutLot
  CONFORME: 'var(--statut-conforme)',
  EN_ALERTE: 'var(--statut-en-alerte)',
  PERIME: 'var(--statut-perime)',
  // NiveauAlerte
  INFO: 'var(--niveau-info)',
  WARNING: 'var(--niveau-warning)',
  CRITIQUE: 'var(--niveau-critique)',
  // StatutAlerte
  OUVERTE: 'var(--cherry)',
  NOTIFIEE: 'var(--roast-cinnamon)',
  CLOTUREE: 'var(--text-muted)',
};

const labels: Record<string, string> = {
  CONFORME: 'Conforme',
  EN_ALERTE: 'En alerte',
  PERIME: 'Périmé',
  INFO: 'Info',
  WARNING: 'Warning',
  CRITIQUE: 'Critique',
  OUVERTE: 'Ouverte',
  NOTIFIEE: 'Notifiée',
  CLOTUREE: 'Clôturée',
  CONDITION_NON_IDEALE: 'Condition non idéale',
  LOT_TROP_ANCIEN: 'Lot trop ancien',
};

const color = computed(() => colorMap[props.value] ?? 'var(--text-muted)');
const label = computed(() => labels[props.value] ?? props.value);
</script>

<template>
  <span class="badge" :style="{ '--c': color }">
    <span class="badge-dot" />
    {{ label }}
  </span>
</template>

<style scoped>
.badge {
  display: inline-flex;
  align-items: center;
  gap: 0.4rem;
  font-size: 0.74rem;
  font-weight: 600;
  letter-spacing: 0.02em;
  padding: 0.2rem 0.55rem;
  border-radius: 999px;
  color: var(--c);
  background: color-mix(in srgb, var(--c) 14%, transparent);
  border: 1px solid color-mix(in srgb, var(--c) 35%, transparent);
}
.badge-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: var(--c);
}
</style>
