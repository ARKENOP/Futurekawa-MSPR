<script setup lang="ts">
defineProps<{ label: string; value: number | string; accent?: string; hint?: string }>();
</script>

<template>
  <div class="kpi card" :style="{ '--accent': accent ?? 'var(--crema-gold)' }">
    <div class="eyebrow">{{ label }}</div>
    <div class="kpi-value mono">{{ value }}</div>
    <div v-if="hint" class="kpi-hint">{{ hint }}</div>
  </div>
</template>

<style scoped>
.kpi {
  padding: 1rem 1.1rem;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  border-left: 3px solid var(--accent);
}
.kpi-value {
  font-size: 1.9rem;
  font-weight: 600;
  line-height: 1;
  color: var(--text-crema);
}
.kpi-hint {
  font-size: 0.75rem;
  color: var(--text-muted);
}
</style>
