<script setup lang="ts">
import { onMounted, ref } from 'vue';
import CountryRail from './CountryRail.vue';
import AppHeader from './AppHeader.vue';
import { useCountryStore } from '@/stores/country';

const country = useCountryStore();
const railOpen = ref(false);

onMounted(() => {
  if (!country.loaded) void country.load();
});
</script>

<template>
  <div class="shell">
    <CountryRail :class="{ open: railOpen }" @navigate="railOpen = false" />
    <div class="shell-main">
      <AppHeader @toggle-rail="railOpen = !railOpen" />
      <main class="shell-content">
        <div v-if="country.unavailable.length" class="banner" role="status">
          Pays indisponibles (backend local injoignable) :
          <strong>{{ country.unavailable.join(', ') }}</strong>
        </div>
        <slot />
      </main>
    </div>
  </div>
</template>

<style scoped>
.shell {
  display: flex;
  min-height: 100vh;
}
.shell-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}
.shell-content {
  padding: 1.5rem clamp(1rem, 3vw, 2.5rem);
  flex: 1;
}
.banner {
  background: color-mix(in srgb, var(--cherry) 18%, var(--surface-raised));
  border: 1px solid var(--cherry);
  color: var(--text-crema);
  padding: 0.6rem 0.9rem;
  border-radius: var(--radius-sm);
  margin-bottom: 1.25rem;
  font-size: 0.85rem;
}
</style>
