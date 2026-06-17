<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import { useCountryStore } from '@/stores/country';
import { listEntrepots } from '@/api/entrepots';
import { listLots } from '@/api/lots';
import { listAlertes } from '@/api/alertes';
import { latestMesure } from '@/api/mesures';
import { flattenGroups, flattenPageGroups, type WithCountry } from '@/lib/groups';
import type { Entrepot, Lot, Alerte, MesureStockage, Pays } from '@/types/api';
import KpiCard from '@/components/common/KpiCard.vue';
import RoastGauge from '@/components/charts/RoastGauge.vue';
import ConditionTimeSeries from '@/components/charts/ConditionTimeSeries.vue';
import AlertFeed from '@/components/alerts/AlertFeed.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import { listMesures } from '@/api/mesures';

const country = useCountryStore();
const { selected, pays } = storeToRefs(country);
const router = useRouter();

const loading = ref(true);
const entrepots = ref<WithCountry<Entrepot>[]>([]);
const lots = ref<WithCountry<Lot>[]>([]);
const alertes = ref<WithCountry<Alerte>[]>([]);
const latest = ref<Record<string, MesureStockage>>({});
const focusSeries = ref<MesureStockage[]>([]);
const focusPays = ref<Pays | null>(null);
const focusLabel = ref('');

function paysOf(code: string): Pays | undefined {
  return pays.value.find((p) => p.codePays === code);
}

function inScope(code: string): boolean {
  return selected.value === 'ALL' || selected.value === code;
}

async function load(): Promise<void> {
  loading.value = true;
  if (!country.loaded) await country.load();

  const [entG, lotG, alG] = await Promise.all([
    listEntrepots(),
    listLots({ size: 100 }),
    listAlertes({ size: 100 }),
  ]);
  entrepots.value = flattenGroups(entG).filter((e) => inScope(e.codePays));
  lots.value = flattenPageGroups(lotG).filter((l) => inScope(l.codePays));
  alertes.value = flattenPageGroups(alG).filter((a) => inScope(a.codePays));

  const entries = await Promise.all(
    entrepots.value.map(async (e) => {
      const m = await latestMesure(e.codePays, e.id);
      return [`${e.codePays}-${e.id}`, m] as const;
    }),
  );
  latest.value = Object.fromEntries(entries);

  // Entrepôt focalisé pour la courbe : le premier du périmètre.
  const focus = entrepots.value[0];
  if (focus) {
    focusLabel.value = `${focus.nomEntrepot} (${focus.codePays})`;
    focusPays.value = paysOf(focus.codePays) ?? null;
    const page = await listMesures(focus.codePays, focus.id, { size: 48 });
    focusSeries.value = [...page.content].reverse();
  } else {
    focusSeries.value = [];
    focusPays.value = null;
  }

  loading.value = false;
}

const openAlertes = computed(() =>
  alertes.value
    .filter((a) => a.statutAlerte !== 'CLOTUREE')
    .sort((a, b) => b.dateHeureCreation.localeCompare(a.dateHeureCreation)),
);
const kpis = computed(() => ({
  alertesOuvertes: openAlertes.value.length,
  conformes: lots.value.filter((l) => l.statutLot === 'CONFORME').length,
  enAlerte: lots.value.filter((l) => l.statutLot === 'EN_ALERTE').length,
  perimes: lots.value.filter((l) => l.statutLot === 'PERIME').length,
  entrepots: entrepots.value.length,
}));

function openEntrepot(e: WithCountry<Entrepot>): void {
  void router.push({ name: 'entrepot-detail', params: { codePays: e.codePays, id: e.id } });
}

onMounted(load);
watch(selected, load);
</script>

<template>
  <LoadingState v-if="loading" />
  <template v-else>
    <section class="kpis">
      <KpiCard
        label="Alertes ouvertes"
        :value="kpis.alertesOuvertes"
        accent="var(--cherry)"
        hint="à traiter"
      />
      <KpiCard label="Lots conformes" :value="kpis.conformes" accent="var(--crema-gold)" />
      <KpiCard label="Lots en alerte" :value="kpis.enAlerte" accent="var(--roast-cinnamon)" />
      <KpiCard label="Lots périmés" :value="kpis.perimes" accent="var(--roast-charred)" />
      <KpiCard label="Entrepôts" :value="kpis.entrepots" accent="var(--roast-green)" />
    </section>

    <div class="grid">
      <section class="panel card">
        <h2 class="panel-title">Conditions par entrepôt</h2>
        <div class="gauges">
          <div
            v-for="e in entrepots"
            :key="`${e.codePays}-${e.id}`"
            class="gauge-card"
            role="button"
            tabindex="0"
            @click="openEntrepot(e)"
            @keydown.enter="openEntrepot(e)"
          >
            <div class="gauge-name">
              {{ e.nomEntrepot }} · <span class="mono">{{ e.codePays }}</span>
            </div>
            <RoastGauge
              v-if="latest[`${e.codePays}-${e.id}`] && paysOf(e.codePays)"
              label="Température"
              :valeur="latest[`${e.codePays}-${e.id}`].temperatureC"
              :ideale="paysOf(e.codePays)!.temperatureIdealeC"
              :tolerance="paysOf(e.codePays)!.toleranceTemperatureC"
              unite="°C"
            />
          </div>
        </div>

        <div v-if="focusPays && focusSeries.length" class="ts-block">
          <h3 class="ts-title">Historique — {{ focusLabel }}</h3>
          <ConditionTimeSeries :mesures="focusSeries" :pays="focusPays" />
        </div>
      </section>

      <section class="panel card">
        <h2 class="panel-title">Alertes ouvertes</h2>
        <AlertFeed :alertes="openAlertes" />
      </section>
    </div>
  </template>
</template>

<style scoped>
.kpis {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 0.9rem;
  margin-bottom: 1.5rem;
}
.grid {
  display: grid;
  grid-template-columns: minmax(0, 2fr) minmax(280px, 1fr);
  gap: 1.25rem;
  align-items: start;
}
.panel {
  padding: 1.25rem;
}
.panel-title {
  font-size: 1rem;
  margin-bottom: 1rem;
}
.gauges {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 0.75rem;
}
.gauge-card {
  border: 1px solid var(--border-husk);
  border-radius: var(--radius-sm);
  padding: 0.6rem 0.7rem 0.2rem;
  background: var(--surface-bean);
  cursor: pointer;
  transition: border-color 0.15s ease;
}
.gauge-card:hover {
  border-color: var(--crema-gold);
}
.gauge-name {
  font-size: 0.85rem;
  font-weight: 600;
}
.ts-block {
  margin-top: 1.5rem;
}
.ts-title {
  font-size: 0.9rem;
  color: var(--text-muted);
  margin-bottom: 0.5rem;
}
@media (max-width: 980px) {
  .grid {
    grid-template-columns: 1fr;
  }
}
</style>
