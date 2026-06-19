<script setup lang="ts">
import { computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useAuthStore } from '@/stores/auth';
import { useCountryStore } from '@/stores/country';

defineEmits<{ 'toggle-rail': [] }>();

const route = useRoute();
const router = useRouter();
const auth = useAuthStore();
const country = useCountryStore();

const titles: Record<string, string> = {
  dashboard: 'Tableau de bord',
  lots: 'Lots de café',
  alertes: 'Alertes',
  'entrepot-detail': 'Détail entrepôt',
};
const title = computed(() => titles[route.name as string] ?? 'FutureKawa');
const scope = computed(() =>
  country.selected === 'ALL'
    ? 'Tous les pays'
    : (country.pays.find((p) => p.codePays === country.selected)?.nomPays ?? country.selected),
);

function logout(): void {
  auth.logout();
  void router.push({ name: 'login' });
}
</script>

<template>
  <header class="header">
    <button class="burger" aria-label="Menu" @click="$emit('toggle-rail')">☰</button>
    <div class="titles">
      <h1>{{ title }}</h1>
      <span class="eyebrow">{{ scope }}</span>
    </div>
    <div class="spacer" />
    <div class="user">
      <span class="mono">{{ auth.user }}</span>
      <span class="role">{{ auth.roles.join(', ') }}</span>
      <button class="btn" @click="logout">Déconnexion</button>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  align-items: center;
  gap: 1rem;
  padding: 1rem clamp(1rem, 3vw, 2.5rem);
  border-bottom: 1px solid var(--border-husk);
  background: var(--bg-espresso);
  position: sticky;
  top: 0;
  z-index: 10;
}
.titles h1 {
  font-size: 1.25rem;
  line-height: 1.2;
}
.spacer {
  flex: 1;
}
.user {
  display: flex;
  align-items: center;
  gap: 0.85rem;
  font-size: 0.85rem;
}
.role {
  color: var(--text-muted);
  font-size: 0.72rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
}
.burger {
  display: none;
  background: transparent;
  border: none;
  color: var(--text-crema);
  font-size: 1.2rem;
}
@media (max-width: 768px) {
  .burger {
    display: inline-flex;
  }
}
</style>
