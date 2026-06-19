import { defineStore } from 'pinia';
import { ref } from 'vue';
import type { Pays } from '@/types/api';
import { listPays } from '@/api/pays';
import { unavailableCountries } from '@/lib/http';

/** 'ALL' = tous les pays, sinon un codePays (BR/EC/CO). */
export const useCountryStore = defineStore('country', () => {
  const pays = ref<Pays[]>([]);
  const selected = ref<string>('ALL');
  const loaded = ref(false);
  const unavailable = ref<string[]>([]);

  async function load(): Promise<void> {
    pays.value = await listPays();
    unavailable.value = [...unavailableCountries.value];
    loaded.value = true;
  }

  function select(codePays: string): void {
    selected.value = codePays;
  }

  return { pays, selected, loaded, unavailable, load, select };
});
