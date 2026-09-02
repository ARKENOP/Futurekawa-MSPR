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

import com.futurekawa.lib.enums.NiveauAlerte;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.TypeAlerte;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class OdooQualityAlertService {
    private static final String MODEL = "futurekawa.quality.alert";
    private static final DateTimeFormatter ODOO_DT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final OdooRpcClient odooRpcClient;

    @Async
    public void pushAlerte(Long backendAlerteId, String entrepotNom, String paysCode, String paysNom,
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
                vals.put("pays_nom", paysNom);
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

    private String toOdooState(StatutAlerte statut) {
        return switch (statut) {
            case OUVERTE -> "draft";
            case NOTIFIEE -> "investigation";
            case CLOTUREE -> "resolved";
        };
    }

    private String toOdooDatetime(LocalDateTime localDateTime) {
        return localDateTime.atZone(ZoneId.systemDefault())
                .withZoneSameInstant(ZoneOffset.UTC)
                .toLocalDateTime()
                .format(ODOO_DT);
    }
}
