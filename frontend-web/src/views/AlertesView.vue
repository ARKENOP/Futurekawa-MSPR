<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useCountryStore } from '@/stores/country';
import { listAlertes, closeAlerte } from '@/api/alertes';
import { flattenPageGroups, type WithCountry } from '@/lib/groups';
import type { Alerte, StatutAlerte, TypeAlerte } from '@/types/api';
import AlertFeed from '@/components/alerts/AlertFeed.vue';
import PaginationBar from '@/components/common/PaginationBar.vue';
import LoadingState from '@/components/common/LoadingState.vue';

const country = useCountryStore();
const { selected } = storeToRefs(country);

const loading = ref(true);
const all = ref<WithCountry<Alerte>[]>([]);
const statutFilter = ref<StatutAlerte | ''>('');
const typeFilter = ref<TypeAlerte | ''>('');
const page = ref(0);
const size = 8;

const statuts: StatutAlerte[] = ['OUVERTE', 'NOTIFIEE', 'CLOTUREE'];
const types: TypeAlerte[] = ['CONDITION_NON_IDEALE', 'LOT_TROP_ANCIEN'];

async function load(): Promise<void> {
  loading.value = true;
  if (!country.loaded) await country.load();
  const groups = await listAlertes({ size: 100 });
  all.value = flattenPageGroups(groups).sort((a, b) =>
    b.dateHeureCreation.localeCompare(a.dateHeureCreation),
  );
  loading.value = false;
}

const filtered = computed(() =>
  all.value
    .filter((a) => selected.value === 'ALL' || a.codePays === selected.value)
    .filter((a) => !statutFilter.value || a.statutAlerte === statutFilter.value)
    .filter((a) => !typeFilter.value || a.typeAlerte === typeFilter.value),
);
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / size)));
const pageRows = computed(() => filtered.value.slice(page.value * size, page.value * size + size));

watch([selected, statutFilter, typeFilter], () => (page.value = 0));
watch(selected, load);
onMounted(load);

async function onClose(a: WithCountry<Alerte>): Promise<void> {
  const updated = await closeAlerte(a.codePays, a.id);
  a.statutAlerte = updated.statutAlerte;
  a.dateHeureCloture = updated.dateHeureCloture;
}
</script>

<template>
  <div class="toolbar">
    <div class="filters">
      <label class="eyebrow">Statut</label>
      <select v-model="statutFilter" class="select">
        <option value="">Tous</option>
        <option v-for="s in statuts" :key="s" :value="s">{{ s }}</option>
      </select>
      <label class="eyebrow">Type</label>
      <select v-model="typeFilter" class="select">
        <option value="">Tous</option>
        <option v-for="t in types" :key="t" :value="t">{{ t }}</option>
      </select>
    </div>
  </div>

  <LoadingState v-if="loading" />
  <template v-else>
    <AlertFeed :alertes="pageRows" closable @close="onClose" />
    <PaginationBar
      v-model:page="page"
      :total-pages="totalPages"
      :total-elements="filtered.length"
    />
  </template>
</template>

<style scoped>
.toolbar {
  margin-bottom: 1.25rem;
}
.filters {
  display: flex;
  align-items: center;
  gap: 0.6rem;
  flex-wrap: wrap;
}
.select {
  background: var(--surface-bean);
  border: 1px solid var(--border-husk);
  border-radius: var(--radius-sm);
  color: var(--text-crema);
  padding: 0.45rem 0.6rem;
  font-family: var(--font-mono);
  font-size: 0.85rem;
}
</style>
