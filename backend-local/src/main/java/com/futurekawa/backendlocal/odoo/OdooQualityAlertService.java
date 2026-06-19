package com.futurekawa.backendlocal.odoo;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.futurekawa.backendlocal.model.enums.NiveauAlerte;
import com.futurekawa.backendlocal.model.enums.StatutAlerte;
import com.futurekawa.backendlocal.model.enums.TypeAlerte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Pushes alerts into Odoo so they appear in the
 * <em>FutureKawa Quality &rarr; Alertes</em> screen.
 *
 * <p>Creates a record in the custom {@code futurekawa.quality.alert} model
 * (provided by the {@code futurekawa_quality} Odoo addon) via the external
 * JSON-RPC API. Runs asynchronously so a slow or unreachable Odoo never blocks
 * the MQTT ingestion / alert-creation transaction.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdooQualityAlertService {

    private static final String MODEL = "futurekawa.quality.alert";
    private static final DateTimeFormatter ODOO_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OdooRpcClient odooRpcClient;

    @Async
    public void pushAlerte(Long backendAlerteId, String entrepotNom, String paysCode,
                           TypeAlerte typeAlerte, NiveauAlerte niveau,
                           String lotReference, String description, LocalDateTime dateCreation) {
        try {
            Map<String, Object> vals = new HashMap<>();
            vals.put("backend_alerte_id", backendAlerteId);
            vals.put("entrepot_nom", entrepotNom);
            vals.put("type_anomaly", typeAlerte.name().toLowerCase());
            vals.put("niveau", niveau.name().toLowerCase());
            vals.put("message_description", description);
            vals.put("date_creation", toOdooDatetime(dateCreation));
            if (paysCode != null) {
                vals.put("pays_code", paysCode);
            }
            if (lotReference != null) {
                vals.put("lot_reference", lotReference);
            }

            Object result = odooRpcClient.executeKw(
                    MODEL, "create", List.of(List.of(vals)), Map.of());

            log.info("Pushed alert (backendId={}) to Odoo {} -> record {}",
                    backendAlerteId, MODEL, result);

        } catch (Exception e) {
            log.error("Failed to push alert (backendId={}) to Odoo", backendAlerteId, e);
        }
    }

    /**
     * Propagates a status change to the matching Odoo ticket (one-way:
     * local backend is the source of truth). The ticket is located by its
     * {@code backend_alerte_id}. Runs asynchronously; failures are logged only.
     */
    @Async
    public void updateAlerteStatut(Long backendAlerteId, StatutAlerte statut) {
        try {
            Object searchResult = odooRpcClient.executeKw(
                    MODEL, "search",
                    List.of(List.of(List.of("backend_alerte_id", "=", backendAlerteId))),
                    Map.of("limit", 1));

            if (!(searchResult instanceof List<?> ids) || ids.isEmpty()) {
                log.warn("No Odoo ticket found for backendAlerteId={}; status not propagated",
                        backendAlerteId);
                return;
            }

            odooRpcClient.executeKw(
                    MODEL, "write",
                    List.of(ids, Map.of("state", toOdooState(statut))),
                    Map.of());

            log.info("Updated Odoo ticket {} for backendAlerteId={} -> state '{}'",
                    ids, backendAlerteId, toOdooState(statut));

        } catch (Exception e) {
            log.error("Failed to update Odoo ticket status for backendAlerteId={}", backendAlerteId, e);
        }
    }

    /** Maps the local {@link StatutAlerte} to the Odoo ticket {@code state}. */
    private String toOdooState(StatutAlerte statut) {
        return switch (statut) {
            case OUVERTE -> "draft";
            case NOTIFIEE -> "investigation";
            case CLOTUREE -> "resolved";
        };
    }

    /** Odoo stores datetimes as naive UTC strings ("yyyy-MM-dd HH:mm:ss"). */
    private String toOdooDatetime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
                .format(ODOO_DT);
    }
}
