<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink } from 'vue-router';
import { useCountryStore } from '@/stores/country';

defineEmits<{ navigate: [] }>();

const country = useCountryStore();

const options = computed(() => [
  { code: 'ALL', label: 'Tous les pays' },
  ...country.pays.map((p) => ({ code: p.codePays, label: p.nomPays })),
]);

const nav = [
  { to: '/', label: 'Tableau de bord', icon: '◉' },
  { to: '/lots', label: 'Lots', icon: '▤' },
  { to: '/alertes', label: 'Alertes', icon: '◆' },
];
</script>

<template>
  <aside class="rail">
    <div class="brand">
      <span class="brand-mark">☕</span>
      <div>
        <div class="brand-name">FutureKawa</div>
        <div class="eyebrow">Supervision stockage</div>
      </div>
    </div>

    <nav class="nav">
      <RouterLink
        v-for="item in nav"
        :key="item.to"
        :to="item.to"
        class="nav-link"
        active-class="active"
        @click="$emit('navigate')"
      >
        <span class="nav-icon" aria-hidden="true">{{ item.icon }}</span>
        {{ item.label }}
      </RouterLink>
    </nav>

    <div class="picker">
      <div class="eyebrow picker-title">Périmètre pays</div>
      <button
        v-for="opt in options"
        :key="opt.code"
        class="picker-opt"
        :class="{ selected: country.selected === opt.code }"
        @click="country.select(opt.code)"
      >
        <span class="dot" />
        {{ opt.label }}
      </button>
    </div>
  </aside>
</template>

<style scoped>
.rail {
  width: 232px;
  flex-shrink: 0;
  background: var(--surface-bean);
  border-right: 1px solid var(--border-husk);
  padding: 1.25rem 1rem;
  display: flex;
  flex-direction: column;
  gap: 1.75rem;
}
.brand {
  display: flex;
  align-items: center;
  gap: 0.65rem;
}
.brand-mark {
  font-size: 1.6rem;
  filter: saturate(0.85);
}
.brand-name {
  font-family: var(--font-display);
  font-weight: 700;
  font-size: 1.05rem;
}
.nav {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
}
.nav-link {
  display: flex;
  align-items: center;
  gap: 0.7rem;
  padding: 0.55rem 0.7rem;
  border-radius: var(--radius-sm);
  color: var(--text-muted);
  font-size: 0.9rem;
  font-weight: 500;
  border: 1px solid transparent;
}
.nav-link:hover {
  color: var(--text-crema);
  background: var(--surface-raised);
}
.nav-link.active {
  color: var(--text-crema);
  background: var(--surface-raised);
  border-color: var(--border-husk);
}
.nav-icon {
  color: var(--crema-gold);
  width: 1.1rem;
  text-align: center;
}
.picker-title {
  margin-bottom: 0.6rem;
}
.picker-opt {
  display: flex;
  align-items: center;
  gap: 0.55rem;
  width: 100%;
  background: transparent;
  border: none;
  color: var(--text-muted);
  padding: 0.4rem 0.5rem;
  border-radius: var(--radius-sm);
  font-size: 0.85rem;
  text-align: left;
}
.picker-opt:hover {
  color: var(--text-crema);
}
.picker-opt.selected {
  color: var(--text-crema);
  background: var(--surface-raised);
}
.dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--border-husk);
}
.picker-opt.selected .dot {
  background: var(--crema-gold);
}

@media (max-width: 768px) {
  .rail {
    position: fixed;
    inset: 0 auto 0 0;
    z-index: 40;
    transform: translateX(-100%);
    transition: transform 0.2s ease;
  }
  .rail.open {
    transform: translateX(0);
  }
}
</style>
