import axios from 'axios';

export const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
});

/** Liste des pays indisponibles (circuit breaker) renvoyée par le central. */
export const unavailableCountries = { value: [] as string[] };

http.interceptors.response.use(
  (response) => {
    const header = response.headers['x-unavailable-countries'];
    unavailableCountries.value =
      typeof header === 'string' && header.length > 0 ? header.split(',') : [];
    return response;
  },
  (error) => {
    // Journalisation centralisée ; les vues gèrent l'affichage local.
    console.error('[http]', error?.response?.status, error?.config?.url, error?.message);
    return Promise.reject(error);
  },
);
