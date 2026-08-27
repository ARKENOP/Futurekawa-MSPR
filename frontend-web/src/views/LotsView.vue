<script setup lang="ts">
import { ref, computed, watch, onMounted, reactive } from 'vue';
import { storeToRefs } from 'pinia';
import { useCountryStore } from '@/stores/country';
import { listLots, createLot, updateLotStatut } from '@/api/lots';
import { flattenPageGroups, type WithCountry } from '@/lib/groups';
import type { Lot, StatutLot } from '@/types/api';
import StatusBadge from '@/components/common/StatusBadge.vue';
import PaginationBar from '@/components/common/PaginationBar.vue';
import EmptyState from '@/components/common/EmptyState.vue';
import LoadingState from '@/components/common/LoadingState.vue';
import AppModal from '@/components/common/AppModal.vue';
import ErrorBanner from '@/components/common/ErrorBanner.vue';
import { listExploitations } from '@/api/exploitations';
import { listEntrepots } from '@/api/entrepots';
import { flattenGroups } from '@/lib/groups';
import { messageErreur } from '@/lib/errors';
import type { Exploitation, Entrepot } from '@/types/api';

const country = useCountryStore();
const { selected } = storeToRefs(country);

const loading = ref(true);
const all = ref<WithCountry<Lot>[]>([]);
const statutFilter = ref<StatutLot | ''>('');
const page = ref(0);
const erreur = ref('');
const size = 8;

const statuts: StatutLot[] = ['CONFORME', 'EN_ALERTE', 'PERIME'];

async function load(): Promise<void> {
  loading.value = true;
  erreur.value = '';
  try {
    if (!country.loaded) await country.load();
    const groups = await listLots({ size: 100 });
    all.value = flattenPageGroups(groups);
  } catch (e) {
    erreur.value = messageErreur(e);
  } finally {
    loading.value = false;
  }
}

const filtered = computed(() =>
  all.value
    .filter((l) => selected.value === 'ALL' || l.codePays === selected.value)
    .filter((l) => !statutFilter.value || l.statutLot === statutFilter.value),
);
const totalPages = computed(() => Math.max(1, Math.ceil(filtered.value.length / size)));
const pageRows = computed(() => filtered.value.slice(page.value * size, page.value * size + size));

watch([selected, statutFilter], () => (page.value = 0));
watch(selected, load);
onMounted(load);

async function cycleStatut(lot: WithCountry<Lot>): Promise<void> {
  const next: StatutLot =
    lot.statutLot === 'CONFORME'
      ? 'EN_ALERTE'
      : lot.statutLot === 'EN_ALERTE'
        ? 'PERIME'
        : 'CONFORME';
  erreur.value = '';
  try {
    const updated = await updateLotStatut(lot.codePays, lot.id, { statutLot: next });
    lot.statutLot = updated.statutLot;
  } catch (e) {
    erreur.value = `Changement de statut impossible : ${messageErreur(e)}`;
  }
}

// ── Création ──
const showCreate = ref(false);
const exploitations = ref<WithCountry<Exploitation>[]>([]);
const entrepots = ref<WithCountry<Entrepot>[]>([]);
const creating = ref(false);
const form = reactive({
  codePays: '',
  referenceLot: '',
  dateEntreeStockage: new Date().toISOString().slice(0, 10),
  dateRecolte: '',
  qualiteLot: '',
  exploitationId: null as number | null,
  entrepotId: null as number | null,
});

/** Exploitations et entrepôts du pays choisi : le backend refuse des ids d'un autre pays. */
const exploitationsDuPays = computed(() =>
  exploitations.value.filter((e) => e.codePays === form.codePays),
);
const entrepotsDeLExploitation = computed(() =>
  entrepots.value.filter(
    (e) => e.codePays === form.codePays && e.exploitationId === form.exploitationId,
  ),
);

async function openCreate(): Promise<void> {
  erreur.value = '';
  showCreate.value = true;
  form.codePays = selected.value !== 'ALL' ? selected.value : (country.pays[0]?.codePays ?? '');
  try {
    if (!exploitations.value.length) {
      const [expG, entG] = await Promise.all([listExploitations(), listEntrepots()]);
      exploitations.value = flattenGroups(expG);
      entrepots.value = flattenGroups(entG);
    }
    syncSelection();
  } catch (e) {
    erreur.value = `Chargement du formulaire impossible : ${messageErreur(e)}`;
  }
}

/** Garde le couple exploitation/entrepôt cohérent avec le pays sélectionné. */
function syncSelection(): void {
  if (!exploitationsDuPays.value.some((e) => e.id === form.exploitationId)) {
    form.exploitationId = exploitationsDuPays.value[0]?.id ?? null;
  }
  if (!entrepotsDeLExploitation.value.some((e) => e.id === form.entrepotId)) {
    form.entrepotId = entrepotsDeLExploitation.value[0]?.id ?? null;
  }
}

watch(() => form.codePays, syncSelection);
watch(() => form.exploitationId, syncSelection);

const createValide = computed(
  () =>
    !!form.referenceLot &&
    !!form.codePays &&
    form.exploitationId !== null &&
    form.entrepotId !== null,
);

async function submitCreate(): Promise<void> {
  if (!createValide.value || form.exploitationId === null || form.entrepotId === null) return;
  creating.value = true;
  erreur.value = '';
  try {
    await createLot({
      codePays: form.codePays,
      referenceLot: form.referenceLot,
      dateEntreeStockage: form.dateEntreeStockage,
      dateRecolte: form.dateRecolte || null,
      qualiteLot: form.qualiteLot || null,
      exploitationId: form.exploitationId,
      entrepotId: form.entrepotId,
    });
    showCreate.value = false;
    form.referenceLot = '';
    await load();
  } catch (e) {
    erreur.value = `Création impossible : ${messageErreur(e)}`;
  } finally {
    creating.value = false;
  }
}
</script>

<template>
  <ErrorBanner v-if="erreur" :message="erreur" @retry="load" />
  <div class="toolbar">
    <div class="filters">
      <label class="eyebrow">Statut</label>
      <select v-model="statutFilter" class="select">
        <option value="">Tous</option>
        <option v-for="s in statuts" :key="s" :value="s">{{ s }}</option>
      </select>
    </div>
    <button class="btn btn-primary" @click="openCreate">+ Nouveau lot</button>
  </div>

  <LoadingState v-if="loading" />
  <template v-else>
    <EmptyState v-if="!filtered.length" message="Aucun lot dans ce périmètre." />
    <div v-else class="card table-wrap">
      <table>
        <thead>
          <tr>
            <th>Référence</th>
            <th>Pays</th>
            <th>Entrée stockage</th>
            <th>Récolte</th>
            <th>Qualité</th>
            <th>Ancienneté</th>
            <th>Statut</th>
            <th></th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="l in pageRows" :key="`${l.codePays}-${l.id}`">
            <td class="mono">{{ l.referenceLot }}</td>
            <td class="mono">{{ l.codePays }}</td>
            <td class="mono">{{ l.dateEntreeStockage }}</td>
            <td class="mono">{{ l.dateRecolte ?? '—' }}</td>
            <td>{{ l.qualiteLot ?? '—' }}</td>
            <td class="mono">{{ l.ancienneteJours != null ? `${l.ancienneteJours} j` : '—' }}</td>
            <td><StatusBadge :value="l.statutLot" /></td>
            <td>
              <button class="btn small" @click="cycleStatut(l)">Changer statut</button>
            </td>
          </tr>
        </tbody>
      </table>
      <PaginationBar
        v-model:page="page"
        :total-pages="totalPages"
        :total-elements="filtered.length"
      />
    </div>
  </template>

  <AppModal v-if="showCreate" title="Nouveau lot" @close="showCreate = false">
    <form class="form" @submit.prevent="submitCreate">
      <label
        >Pays
        <select v-model="form.codePays">
          <option v-for="p in country.pays" :key="p.codePays" :value="p.codePays">
            {{ p.nomPays }}
          </option>
        </select>
      </label>
      <label>Référence<input v-model="form.referenceLot" required /></label>
      <label
        >Date entrée stockage<input v-model="form.dateEntreeStockage" type="date" required
      /></label>
      <label>Date récolte<input v-model="form.dateRecolte" type="date" /></label>
      <label>Qualité<input v-model="form.qualiteLot" /></label>
      <label
        >Exploitation
        <select v-model.number="form.exploitationId">
          <option v-for="e in exploitationsDuPays" :key="e.id" :value="e.id">
            {{ e.nomExploitation }}
          </option>
        </select>
      </label>
      <label
        >Entrepôt
        <select v-model.number="form.entrepotId">
          <option v-for="e in entrepotsDeLExploitation" :key="e.id" :value="e.id">
            {{ e.nomEntrepot }}
          </option>
        </select>
      </label>
      <p v-if="form.codePays && !exploitationsDuPays.length" class="hint">
        Aucune exploitation connue pour ce pays : son backend est peut-être indisponible.
      </p>
    </form>
    <template #footer>
      <button class="btn" @click="showCreate = false">Annuler</button>
      <button class="btn btn-primary" :disabled="!createValide || creating" @click="submitCreate">
        {{ creating ? 'Création…' : 'Créer' }}
      </button>
    </template>
  </AppModal>
</template>

<style scoped>
.hint {
  margin: 0;
  font-size: 0.8rem;
  color: var(--niveau-critique);
}
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 1rem;
  margin-bottom: 1.25rem;
}
.filters {
  display: flex;
  align-items: center;
  gap: 0.6rem;
}
.select,
.form select,
.form input {
  background: var(--surface-bean);
  border: 1px solid var(--border-husk);
  border-radius: var(--radius-sm);
  color: var(--text-crema);
  padding: 0.45rem 0.6rem;
  font-family: var(--font-mono);
  font-size: 0.85rem;
}
.table-wrap {
  padding: 0.5rem 1rem 1rem;
  overflow-x: auto;
}
table {
  width: 100%;
  border-collapse: collapse;
  font-size: 0.85rem;
}
th {
  text-align: left;
  font-family: var(--font-mono);
  font-size: 0.7rem;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  color: var(--text-muted);
  padding: 0.7rem 0.6rem;
  border-bottom: 1px solid var(--border-husk);
}
td {
  padding: 0.65rem 0.6rem;
  border-bottom: 1px solid color-mix(in srgb, var(--border-husk) 50%, transparent);
}
.btn.small {
  padding: 0.3rem 0.6rem;
  font-size: 0.76rem;
}
.form {
  display: flex;
  flex-direction: column;
  gap: 0.7rem;
}
.form label {
  display: flex;
  flex-direction: column;
  gap: 0.3rem;
  font-size: 0.78rem;
  color: var(--text-muted);
}
</style>
