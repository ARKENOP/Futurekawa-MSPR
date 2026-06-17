<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { useRoute, RouterLink } from 'vue-router';
import { useCountryStore } from '@/stores/country';
import { getEntrepot } from '@/api/entrepots';
import { listMesures, latestMesure } from '@/api/mesures';
import { listLots } from '@/api/lots';
import { listAlertes } from '@/api/alertes';
import { flattenPageGroups, type WithCountry } from '@/lib/groups';
import type { Entrepot, MesureStockage, Lot, Alerte, Pays } from '@/types/api';
import RoastGauge from '@/components/charts/RoastGauge.vue';
import ConditionTimeSeries from '@/components/charts/ConditionTimeSeries.vue';
import AlertFeed from '@/components/alerts/AlertFeed.vue';
import StatusBadge from '@/components/common/StatusBadge.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import EmptyState from '@/components/common/EmptyState.vue';

const route = useRoute();
const country = useCountryStore();

const codePays = route.params.codePays as string;
const id = Number(route.params.id);

const loading = ref(true);
const entrepot = ref<Entrepot | null>(null);
const latest = ref<MesureStockage | null>(null);
const series = ref<MesureStockage[]>([]);
const lots = ref<WithCountry<Lot>[]>([]);
const alertes = ref<WithCountry<Alerte>[]>([]);

const pays = computed<Pays | null>(() => country.pays.find((p) => p.codePays === codePays) ?? null);

async function load(): Promise<void> {
  loading.value = true;
  if (!country.loaded) await country.load();
  const [ent, last, page, lotG, alG] = await Promise.all([
    getEntrepot(codePays, id),
    latestMesure(codePays, id),
    listMesures(codePays, id, { size: 48 }),
    listLots({ size: 100 }),
    listAlertes({ size: 100 }),
  ]);
  entrepot.value = ent;
  latest.value = last;
  series.value = [...page.content].reverse();
  lots.value = flattenPageGroups(lotG).filter(
    (l) => l.codePays === codePays && l.entrepotId === id,
  );
  alertes.value = flattenPageGroups(alG).filter(
    (a) => a.codePays === codePays && a.entrepotId === id,
  );
  loading.value = false;
}

onMounted(load);
</script>

<template>
  <RouterLink to="/" class="back">‹ Retour au tableau de bord</RouterLink>
  <LoadingState v-if="loading" />
  <template v-else-if="entrepot && pays">
    <header class="head">
      <div>
        <h2>{{ entrepot.nomEntrepot }}</h2>
        <span class="eyebrow">{{ entrepot.localisation }} · {{ codePays }}</span>
      </div>
      <span class="cap mono">Capacité {{ entrepot.capaciteMax ?? '—' }}</span>
    </header>

    <section class="gauges card">
      <RoastGauge
        v-if="latest"
        label="Température"
        :valeur="latest.temperatureC"
        :ideale="pays.temperatureIdealeC"
        :tolerance="pays.toleranceTemperatureC"
        unite="°C"
      />
      <RoastGauge
        v-if="latest"
        label="Humidité"
        :valeur="latest.humiditePourcent"
        :ideale="pays.humiditeIdealePourcent"
        :tolerance="pays.toleranceHumiditePourcent"
        unite="%"
      />
    </section>

    <section class="card block">
      <h3 class="block-title">Historique des conditions</h3>
      <ConditionTimeSeries :mesures="series" :pays="pays" />
    </section>

    <div class="cols">
      <section class="card block">
        <h3 class="block-title">Lots stockés</h3>
        <EmptyState v-if="!lots.length" message="Aucun lot." />
        <ul v-else class="lots">
          <li v-for="l in lots" :key="l.id">
            <span class="mono">{{ l.referenceLot }}</span>
            <StatusBadge :value="l.statutLot" />
          </li>
        </ul>
      </section>
      <section class="card block">
        <h3 class="block-title">Alertes liées</h3>
        <AlertFeed :alertes="alertes" />
      </section>
    </div>
  </template>
  <EmptyState v-else message="Entrepôt introuvable." />
</template>

<style scoped>
.back {
  display: inline-block;
  color: var(--text-muted);
  font-size: 0.82rem;
  margin-bottom: 1rem;
}
.back:hover {
  color: var(--crema-gold);
}
.head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 1.25rem;
}
.head h2 {
  font-size: 1.3rem;
}
.cap {
  color: var(--text-muted);
  font-size: 0.8rem;
}
.gauges {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(220px, 1fr));
  gap: 1rem;
  padding: 1rem 1.25rem;
  margin-bottom: 1.25rem;
}
.block {
  padding: 1.25rem;
  margin-bottom: 1.25rem;
}
.block-title {
  font-size: 0.95rem;
  margin-bottom: 0.9rem;
}
.cols {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 1.25rem;
}
.lots {
  list-style: none;
  margin: 0;
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 0.5rem;
}
.lots li {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.45rem 0;
  border-bottom: 1px solid color-mix(in srgb, var(--border-husk) 50%, transparent);
}
@media (max-width: 900px) {
  .cols {
    grid-template-columns: 1fr;
  }
}
</style>
