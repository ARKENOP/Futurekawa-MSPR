package com.futurekawa.backendlocal.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.repository.LotRepository;
import com.futurekawa.backendlocal.service.AlerteService;
import com.futurekawa.lib.enums.StatutLot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Flags lots past their maximum storage duration and raises their alert.
 *
 * <p>The schedule is configurable ({@code futurekawa.peremption.cron}, hourly by
 * default) so a deployment can be driven to run the check on demand — the rule is
 * otherwise only observable once an hour, which makes it impractical to test or
 * demonstrate.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PeremptionScheduler {
    private static final DateTimeFormatter DATE_FR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final LotRepository lotRepository;
    private final AlerteService alerteService;
    private final PaysProperties paysProperties;

    @Scheduled(cron = "${futurekawa.peremption.cron:0 0 * * * *}")
    @Transactional
    public void checkLotExpirations() {
        log.info("Running scheduled check for expired lots...");

        int maxDays = paysProperties.dureeMaxStockageJours();
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(maxDays);

        List<Lot> expiredLots = lotRepository.findLotsOlderThanAndStatutNot(thresholdDate, StatutLot.PERIME);

        if (expiredLots.isEmpty()) {
            log.info("No newly expired lots found.");
            return;
        }

        log.warn("Found {} newly expired lots. Updating status and generating alerts.", expiredLots.size());

        for (Lot lot : expiredLots) {
            lot.setStatutLot(StatutLot.PERIME);
            lotRepository.save(lot);

            long joursEcoules = ChronoUnit.DAYS.between(lot.getDateEntreeStockage(), LocalDateTime.now());
            String description = String.format(Locale.FRENCH,
                    "Le lot %s dépasse la durée maximale de stockage : %d jours écoulés depuis "
                            + "son entrée en entrepôt le %s (limite %d jours).",
                    lot.getReferenceLot(), joursEcoules,
                    lot.getDateEntreeStockage().format(DATE_FR), maxDays);

            alerteService.createPeremptionAlerte(lot.getEntrepot(), lot, description);
        }

        log.info("Finished processing expired lots.");
    }
}
