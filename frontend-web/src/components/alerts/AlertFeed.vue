<script setup lang="ts">
import AlertItem from './AlertItem.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import type { Alerte } from '@/types/api';
import type { WithCountry } from '@/lib/groups';

defineProps<{ alertes: WithCountry<Alerte>[]; closable?: boolean }>();
defineEmits<{ close: [alerte: WithCountry<Alerte>] }>();
</script>

<template>
  <div class="feed">
    <EmptyState v-if="!alertes.length" message="Aucune alerte." />
    <AlertItem
      v-for="a in alertes"
      :key="`${a.codePays}-${a.id}`"
      :alerte="a"
      :closable="closable"
      @close="$emit('close', $event)"
    />
  </div>
</template>

<style scoped>
.feed {
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
}
</style>
