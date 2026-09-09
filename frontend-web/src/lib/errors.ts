import axios from 'axios';

/**
 * Message lisible à partir d'une erreur d'appel API.
 * Les deux backends répondent en RFC 7807 (`application/problem+json`), donc le
 * champ `detail` porte le message métier ; on retombe sur le statut HTTP sinon.
 */
export function messageErreur(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const data = error.response?.data as { detail?: string; title?: string } | undefined;
    if (data?.detail) return data.detail;
    if (data?.title) return data.title;
    if (error.response) return `Erreur ${error.response.status} du serveur.`;
    return 'Le serveur est injoignable.';
  }
  return error instanceof Error ? error.message : 'Erreur inattendue.';
}

/** Vrai si l'erreur est un 404 : la ressource est absente, pas le service. */
export function estIntrouvable(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 404;
}

/** Vrai si le pays est momentanément indisponible (circuit ouvert côté central). */
export function estIndisponible(error: unknown): boolean {
  return axios.isAxiosError(error) && error.response?.status === 503;
}
