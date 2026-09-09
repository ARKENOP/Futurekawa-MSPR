import type { CountryGroup, CountryPageGroup } from '@/types/api';

/** Élément aplati portant le pays d'origine (clé d'unicité = {codePays, id}). */
export type WithCountry<T> = T & { codePays: string; nomPays: string };

/** Aplatit des groupes simples en une liste enrichie du pays. */
export function flattenGroups<T>(groups: CountryGroup<T>[]): WithCountry<T>[] {
  return groups.flatMap((g) =>
    g.data.map((item) => ({ ...item, codePays: g.codePays, nomPays: g.nomPays })),
  );
}

/** Aplatit des groupes paginés (concatène le content de chaque page). */
export function flattenPageGroups<T>(groups: CountryPageGroup<T>[]): WithCountry<T>[] {
  return groups.flatMap((g) =>
    g.page.content.map((item) => ({ ...item, codePays: g.codePays, nomPays: g.nomPays })),
  );
}

/** Total d'éléments tous pays confondus (somme des totalElements). */
export function totalElements<T>(groups: CountryPageGroup<T>[]): number {
  return groups.reduce((sum, g) => sum + g.page.totalElements, 0);
}

/** Restreint des groupes au pays sélectionné ('ALL' => tous). */
export function filterByCountry<G extends { codePays: string }>(
  groups: G[],
  codePays: string,
): G[] {
  return codePays === 'ALL' ? groups : groups.filter((g) => g.codePays === codePays);
}
