<script setup lang="ts">
import StatusBadge from '@/components/common/StatusBadge.vue';
import type { Alerte } from '@/types/api';
import type { WithCountry } from '@/lib/groups';

defineProps<{ alerte: WithCountry<Alerte>; closable?: boolean }>();
defineEmits<{ close: [alerte: WithCountry<Alerte>] }>();

function fmt(dt: string): string {
  return dt.slice(0, 16).replace('T', ' ');
}
</script>

<template>
  <article class="item" :style="{ '--lvl': `var(--niveau-${alerte.niveau.toLowerCase()})` }">
    <div class="top">
      <StatusBadge :value="alerte.niveau" />
      <span class="pays mono">{{ alerte.codePays }}</span>
      <span class="when mono">{{ fmt(alerte.dateHeureCreation) }}</span>
    </div>
    <p class="msg">{{ alerte.messageDescription }}</p>
    <div class="bottom">
      <StatusBadge :value="alerte.typeAlerte" />
      <StatusBadge :value="alerte.statutAlerte" />
      <button
        v-if="closable && alerte.statutAlerte !== 'CLOTUREE'"
        class="btn close-btn"
        @click="$emit('close', alerte)"
      >
        Clôturer
      </button>
    </div>
  </article>
</template>

<style scoped>
.item {
  border: 1px solid var(--border-husk);
  border-left: 3px solid var(--lvl);
  border-radius: var(--radius-sm);
  background: var(--surface-bean);
  padding: 0.75rem 0.85rem;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.top,
.bottom {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}
.bottom {
  flex-wrap: wrap;
}
.pays {
  font-size: 0.72rem;
  color: var(--crema-gold);
  font-weight: 600;
}
.when {
  font-size: 0.72rem;
  color: var(--text-muted);
  margin-left: auto;
}
.msg {
  margin: 0;
  font-size: 0.88rem;
  line-height: 1.4;
}
.close-btn {
  margin-left: auto;
  padding: 0.3rem 0.7rem;
  font-size: 0.78rem;
}
</style>
