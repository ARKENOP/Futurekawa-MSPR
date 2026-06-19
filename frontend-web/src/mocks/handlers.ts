import { http, HttpResponse } from 'msw';
import type {
  Page,
  CountryGroup,
  CountryPageGroup,
  Lot,
  Alerte,
  CreateLotRequest,
  UpdateLotRequest,
  UpdateAlerteRequest,
} from '@/types/api';
import {
  countries,
  pays,
  exploitations,
  entrepots,
  lots,
  alertes,
  generateMesures,
} from './fixtures/db';

const API = '/api/v1';

function buildPage<T>(items: T[], page: number, size: number): Page<T> {
  const totalElements = items.length;
  const totalPages = Math.max(1, Math.ceil(totalElements / size));
  const start = page * size;
  const content = items.slice(start, start + size);
  return {
    content,
    totalElements,
    totalPages,
    number: page,
    size,
    numberOfElements: content.length,
    first: page === 0,
    last: page >= totalPages - 1,
    empty: content.length === 0,
  };
}

function paginationParams(url: URL): { page: number; size: number } {
  return {
    page: Number(url.searchParams.get('page') ?? 0),
    size: Number(url.searchParams.get('size') ?? 20),
  };
}

export const handlers = [
  // ── Pays ────────────────────────────────────────────────
  http.get(`${API}/pays`, () => HttpResponse.json(countries.map((c) => pays[c.codePays]))),

  // ── Exploitations (groupé) ──────────────────────────────
  http.get(`${API}/exploitations`, () => {
    const groups: CountryGroup<(typeof exploitations)[string][number]>[] = countries.map((c) => ({
      codePays: c.codePays,
      nomPays: c.nomPays,
      data: exploitations[c.codePays],
    }));
    return HttpResponse.json(groups);
  }),

  // ── Entrepôts (groupé) ──────────────────────────────────
  http.get(`${API}/entrepots`, ({ request }) => {
    const url = new URL(request.url);
    const exploitationId = url.searchParams.get('exploitationId');
    const groups: CountryGroup<(typeof entrepots)[string][number]>[] = countries.map((c) => ({
      codePays: c.codePays,
      nomPays: c.nomPays,
      data: exploitationId
        ? entrepots[c.codePays].filter((e) => e.exploitationId === Number(exploitationId))
        : entrepots[c.codePays],
    }));
    return HttpResponse.json(groups);
  }),

  // ── Entrepôt : détail ───────────────────────────────────
  http.get(`${API}/entrepots/:codePays/:id`, ({ params }) => {
    const list = entrepots[params.codePays as string] ?? [];
    const found = list.find((e) => e.id === Number(params.id));
    return found ? HttpResponse.json(found) : new HttpResponse(null, { status: 404 });
  }),

  // ── Mesures : dernière ──────────────────────────────────
  http.get(`${API}/entrepots/:codePays/:id/mesures/latest`, ({ params }) => {
    const series = generateMesures(params.codePays as string, Number(params.id));
    return HttpResponse.json(series[series.length - 1]);
  }),

  // ── Mesures : série paginée ─────────────────────────────
  http.get(`${API}/entrepots/:codePays/:id/mesures`, ({ request, params }) => {
    const url = new URL(request.url);
    const { page, size } = paginationParams(url);
    const series = generateMesures(params.codePays as string, Number(params.id)).reverse();
    return HttpResponse.json(buildPage(series, page, size));
  }),

  // ── Lots (groupé + paginé par pays) ─────────────────────
  http.get(`${API}/lots`, ({ request }) => {
    const url = new URL(request.url);
    const { page, size } = paginationParams(url);
    const statut = url.searchParams.get('statutLot');
    const groups: CountryPageGroup<Lot>[] = countries.map((c) => {
      let items = lots[c.codePays];
      if (statut) items = items.filter((l) => l.statutLot === statut);
      return { codePays: c.codePays, nomPays: c.nomPays, page: buildPage(items, page, size) };
    });
    return HttpResponse.json(groups);
  }),

  // ── Lot : détail ────────────────────────────────────────
  http.get(`${API}/lots/:codePays/:id`, ({ params }) => {
    const found = (lots[params.codePays as string] ?? []).find((l) => l.id === Number(params.id));
    return found ? HttpResponse.json(found) : new HttpResponse(null, { status: 404 });
  }),

  // ── Lot : création ──────────────────────────────────────
  http.post(`${API}/lots`, async ({ request }) => {
    const body = (await request.json()) as CreateLotRequest & { codePays: string };
    const list = lots[body.codePays] ?? [];
    const created: Lot = {
      id: Math.max(0, ...list.map((l) => l.id)) + 1,
      referenceLot: body.referenceLot,
      dateEntreeStockage: body.dateEntreeStockage,
      dateRecolte: body.dateRecolte ?? null,
      statutLot: 'CONFORME',
      qualiteLot: body.qualiteLot ?? null,
      exploitationId: body.exploitationId,
      entrepotId: body.entrepotId,
      paysId: pays[body.codePays].id,
      ancienneteJours: 0,
    };
    list.unshift(created);
    return HttpResponse.json(created, { status: 201 });
  }),

  // ── Lot : changement de statut ──────────────────────────
  http.patch(`${API}/lots/:codePays/:id`, async ({ request, params }) => {
    const body = (await request.json()) as UpdateLotRequest;
    const found = (lots[params.codePays as string] ?? []).find((l) => l.id === Number(params.id));
    if (!found) return new HttpResponse(null, { status: 404 });
    found.statutLot = body.statutLot;
    return HttpResponse.json(found);
  }),

  // ── Alertes (groupé + paginé par pays) ──────────────────
  http.get(`${API}/alertes`, ({ request }) => {
    const url = new URL(request.url);
    const { page, size } = paginationParams(url);
    const statut = url.searchParams.get('statutAlerte');
    const type = url.searchParams.get('typeAlerte');
    const groups: CountryPageGroup<Alerte>[] = countries.map((c) => {
      let items = alertes[c.codePays];
      if (statut) items = items.filter((a) => a.statutAlerte === statut);
      if (type) items = items.filter((a) => a.typeAlerte === type);
      items = [...items].sort((a, b) => b.dateHeureCreation.localeCompare(a.dateHeureCreation));
      return { codePays: c.codePays, nomPays: c.nomPays, page: buildPage(items, page, size) };
    });
    return HttpResponse.json(groups);
  }),

  // ── Alerte : détail ─────────────────────────────────────
  http.get(`${API}/alertes/:codePays/:id`, ({ params }) => {
    const found = (alertes[params.codePays as string] ?? []).find(
      (a) => a.id === Number(params.id),
    );
    return found ? HttpResponse.json(found) : new HttpResponse(null, { status: 404 });
  }),

  // ── Alerte : changement de statut (clôture) ─────────────
  http.patch(`${API}/alertes/:codePays/:id`, async ({ request, params }) => {
    const body = (await request.json()) as UpdateAlerteRequest;
    const found = (alertes[params.codePays as string] ?? []).find(
      (a) => a.id === Number(params.id),
    );
    if (!found) return new HttpResponse(null, { status: 404 });
    found.statutAlerte = body.statutAlerte;
    if (body.statutAlerte === 'CLOTUREE' && !found.dateHeureCloture) {
      found.dateHeureCloture = new Date().toISOString().slice(0, 19);
    }
    return HttpResponse.json(found);
  }),
];
