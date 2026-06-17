import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

const STORAGE_KEY = 'fk_auth';

interface StoredAuth {
  user: string;
  roles: string[];
  token: string;
}

/**
 * Auth STUB (Keycloak non disponible en dev).
 * TODO: remplacer par keycloak-js + flux OIDC réel.
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
