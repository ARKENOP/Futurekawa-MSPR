package com.futurekawa.backendcentral.support;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

import com.futurekawa.backendcentral.client.LocalBackendClient;
import com.futurekawa.backendcentral.dto.envelope.PageDto;
import com.futurekawa.lib.dto.request.CreateLotRequest;
import com.futurekawa.lib.dto.request.UpdateAlerteRequest;
import com.futurekawa.lib.dto.request.UpdateLotRequest;
import com.futurekawa.lib.dto.response.AlerteResponse;
import com.futurekawa.lib.dto.response.EntrepotResponse;
import com.futurekawa.lib.dto.response.ExploitationResponse;
import com.futurekawa.lib.dto.response.LotResponse;
import com.futurekawa.lib.dto.response.MesureStockageResponse;
import com.futurekawa.lib.dto.response.PaysResponse;
import com.futurekawa.lib.enums.NiveauAlerte;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.StatutLot;
import com.futurekawa.lib.enums.TypeAlerte;

/**
 * One country backend, in memory.
 *
 * <p>The central's whole job is talking to country backends over HTTP, so the tests
 * substitute the client rather than the network: the fan-out, the circuit breakers,
 * the consolidated envelopes and the error translation are all exercised for real,
 * and only the wire is replaced. That also lets a test say precisely what a country
 * does — answer normally, reject one resource with a 404, or be unreachable — which
 * is the distinction the central is built around.
 *
 * <p>Data is deterministic and derived from the country code, so assertions can name
 * exact values.
 */
public class StubLocalBackendClient implements LocalBackendClient {
    private final String codePays;
    private final String nomPays;

    /** When set, every call throws it — the country is unreachable. */
    private RuntimeException panne;

    /** Ids this country answers 404 for: the resource is absent, the backend is fine. */
    private final Set<Long> lotsAbsents = new HashSet<>();
    private final Set<Long> entrepotsSansMesure = new HashSet<>();

    private final List<CreateLotRequest> lotsCrees = new ArrayList<>();
    private final AtomicInteger appels = new AtomicInteger();

    public StubLocalBackendClient(String codePays, String nomPays) {
        this.codePays = codePays;
        this.nomPays = nomPays;
    }

    /** Back to a healthy country with no absent resources. */
    public void reset() {
        panne = null;
        lotsAbsents.clear();
        entrepotsSansMesure.clear();
        lotsCrees.clear();
        appels.set(0);
    }

    /** The backend is down: connection refused, as RestClient would report it. */
    public void tombeEnPanne() {
        panne = new ResourceAccessException("I/O error: connection refused",
                new java.net.ConnectException("Connection refused"));
    }

    public void redevientDisponible() {
        panne = null;
    }

    /** This lot does not exist here — the country answers 404, correctly. */
    public void declareLotAbsent(Long id) {
        lotsAbsents.add(id);
    }

    /** This entrepôt has received no measure yet — 404 on /mesures/latest. */
    public void declareEntrepotSansMesure(Long id) {
        entrepotsSansMesure.add(id);
    }

    public List<CreateLotRequest> lotsCrees() {
        return List.copyOf(lotsCrees);
    }

    public int nombreDAppels() {
        return appels.get();
    }

    private void garde() {
        appels.incrementAndGet();
        if (panne != null) {
            throw panne;
        }
    }

    private static HttpClientErrorException introuvable(String quoi) {
        return HttpClientErrorException.create(
                HttpStatus.NOT_FOUND, quoi + " not found", HttpHeaders.EMPTY, null, null);
    }

    @Override
    public PaysResponse getPays() {
        garde();
        return new PaysResponse(1L, codePays, nomPays,
                new BigDecimal("29.0"), new BigDecimal("55.0"),
                new BigDecimal("3.0"), new BigDecimal("2.0"), true);
    }

    @Override
    public List<ExploitationResponse> getExploitations() {
        garde();
        return List.of(new ExploitationResponse(1L, "Exploitation " + codePays, nomPays,
                "responsable." + codePays.toLowerCase() + "@futurekawa.local", true, 1L, codePays));
    }

    @Override
    public List<EntrepotResponse> getEntrepots(Long exploitationId) {
        garde();
        List<EntrepotResponse> tous = List.of(
                new EntrepotResponse(1L, "Entrepôt 1 " + codePays, nomPays, 5000, "actif", 1L, 1L),
                new EntrepotResponse(2L, "Entrepôt 2 " + codePays, nomPays, 3000, "actif", 2L, 1L));
        if (exploitationId == null) {
            return tous;
        }
        return tous.stream().filter(e -> e.exploitationId().equals(exploitationId)).toList();
    }

    @Override
    public EntrepotResponse getEntrepot(Long id) {
        garde();
        return getEntrepots(null).stream()
                .filter(e -> e.id().equals(id))
                .findFirst()
                .orElseThrow(() -> introuvable("Entrepot"));
    }

    @Override
    public PageDto<LotResponse> getLots(StatutLot statutLot, int page, int size) {
        garde();
        List<LotResponse> lots = List.of(
                lot(1L, codePays + "-2025-0001", LocalDate.of(2025, 1, 10), StatutLot.PERIME, 500),
                lot(2L, codePays + "-2026-0002", LocalDate.of(2026, 3, 1), StatutLot.CONFORME, 120));
        List<LotResponse> filtres = statutLot == null
                ? lots
                : lots.stream().filter(l -> l.statutLot() == statutLot).toList();
        return page(filtres, page, size);
    }

    private LotResponse lot(Long id, String reference, LocalDate entree, StatutLot statut, int anciennete) {
        return new LotResponse(id, reference, entree, entree.minusMonths(1),
                statut, "Arabica", 1L, 1L, 1L, anciennete);
    }

    @Override
    public LotResponse getLot(Long id) {
        garde();
        if (lotsAbsents.contains(id)) {
            throw introuvable("Lot");
        }
        return lot(id, codePays + "-LOT-" + id, LocalDate.of(2026, 3, 1), StatutLot.CONFORME, 120);
    }

    @Override
    public LotResponse createLot(CreateLotRequest request) {
        garde();
        lotsCrees.add(request);
        return new LotResponse(99L, request.referenceLot(), request.dateEntreeStockage(),
                request.dateRecolte(), StatutLot.CONFORME, request.qualiteLot(),
                request.exploitationId(), request.entrepotId(), 1L, 0);
    }

    @Override
    public LotResponse updateLot(Long id, UpdateLotRequest request) {
        garde();
        if (lotsAbsents.contains(id)) {
            throw introuvable("Lot");
        }
        return lot(id, codePays + "-LOT-" + id, LocalDate.of(2026, 3, 1), request.statutLot(), 120);
    }

    @Override
    public PageDto<AlerteResponse> getAlertes(StatutAlerte statutAlerte, TypeAlerte typeAlerte,
                                              int page, int size) {
        garde();
        List<AlerteResponse> alertes = List.of(
                alerte(1L, TypeAlerte.CONDITION_NON_IDEALE, NiveauAlerte.CRITIQUE, StatutAlerte.OUVERTE),
                alerte(2L, TypeAlerte.LOT_TROP_ANCIEN, NiveauAlerte.CRITIQUE, StatutAlerte.CLOTUREE));
        List<AlerteResponse> filtres = alertes.stream()
                .filter(a -> statutAlerte == null || a.statutAlerte() == statutAlerte)
                .filter(a -> typeAlerte == null || a.typeAlerte() == typeAlerte)
                .toList();
        return page(filtres, page, size);
    }

    private AlerteResponse alerte(Long id, TypeAlerte type, NiveauAlerte niveau, StatutAlerte statut) {
        return new AlerteResponse(id, type, niveau, statut,
                "Anomalie " + codePays, LocalDateTime.of(2026, 6, 17, 8, 32),
                statut == StatutAlerte.CLOTUREE ? LocalDateTime.of(2026, 6, 17, 18, 0) : null,
                1L, 1L, 1L);
    }

    @Override
    public AlerteResponse getAlerte(Long id) {
        garde();
        return alerte(id, TypeAlerte.CONDITION_NON_IDEALE, NiveauAlerte.CRITIQUE, StatutAlerte.OUVERTE);
    }

    @Override
    public AlerteResponse updateAlerte(Long id, UpdateAlerteRequest request) {
        garde();
        return alerte(id, TypeAlerte.CONDITION_NON_IDEALE, NiveauAlerte.CRITIQUE, request.statutAlerte());
    }

    @Override
    public PageDto<MesureStockageResponse> getMesures(Long entrepotId, LocalDateTime from,
                                                      LocalDateTime to, int page, int size) {
        garde();
        List<MesureStockageResponse> mesures = List.of(
                mesure(1L, entrepotId, LocalDateTime.of(2026, 6, 17, 8, 0), "29.0"),
                mesure(2L, entrepotId, LocalDateTime.of(2026, 6, 17, 8, 30), "31.0"),
                mesure(3L, entrepotId, LocalDateTime.of(2026, 6, 17, 9, 0), "36.0"));
        List<MesureStockageResponse> filtres = mesures.stream()
                .filter(m -> from == null || !m.dateHeureMesure().isBefore(from))
                .filter(m -> to == null || !m.dateHeureMesure().isAfter(to))
                .toList();
        return page(filtres, page, size);
    }

    private MesureStockageResponse mesure(Long id, Long entrepotId, LocalDateTime quand, String temperature) {
        return new MesureStockageResponse(id, "capteur-" + codePays.toLowerCase(), quand,
                new BigDecimal(temperature), new BigDecimal("55.0"), entrepotId, null);
    }

    @Override
    public MesureStockageResponse getLatestMesure(Long entrepotId) {
        garde();
        if (entrepotsSansMesure.contains(entrepotId)) {
            throw introuvable("Mesure");
        }
        return mesure(3L, entrepotId, LocalDateTime.of(2026, 6, 17, 9, 0), "36.0");
    }

    private static <T> PageDto<T> page(List<T> items, int page, int size) {
        int totalPages = Math.max(1, (int) Math.ceil((double) items.size() / size));
        int debut = Math.min(page * size, items.size());
        int fin = Math.min(debut + size, items.size());
        List<T> contenu = items.subList(debut, fin);
        return new PageDto<>(contenu, items.size(), totalPages, page, size,
                contenu.size(), page == 0, page >= totalPages - 1, contenu.isEmpty());
    }
}
