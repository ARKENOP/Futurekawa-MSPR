import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

const STORAGE_KEY = 'fk_auth';

interface StoredAuth {
  user: string;
  roles: string[];
  token: string;
}

/**
 * Identification de poste, volontairement non authentifiante : elle nomme
 * l'opérateur pour l'affichage et rien de plus. L'authentification utilisateur
 * est hors périmètre MSPR (décision documentée dans docs/ARCHITECTURE.md) — la
 * solution est déployée sur le réseau privé du siège, sans exposition publique.
 */
export const useAuthStore = defineStore('auth', () => {
  const stored = localStorage.getItem(STORAGE_KEY);
  const initial: StoredAuth | null = stored ? JSON.parse(stored) : null;

  const user = ref<string | null>(initial?.user ?? null);
  const roles = ref<string[]>(initial?.roles ?? []);

  const isAuthenticated = computed(() => user.value !== null);

  function login(username: string): void {
    const auth: StoredAuth = {
      user: username || 'demo',
      roles: ['SIEGE'],
      token: 'stub-token',
    };
    user.value = auth.user;
    roles.value = auth.roles;
    localStorage.setItem(STORAGE_KEY, JSON.stringify(auth));
  }

  function logout(): void {
    user.value = null;
    roles.value = [];
    localStorage.removeItem(STORAGE_KEY);
  }

  return { user, roles, isAuthenticated, login, logout };
});
