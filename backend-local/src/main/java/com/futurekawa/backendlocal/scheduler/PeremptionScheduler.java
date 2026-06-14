package com.futurekawa.backendlocal.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.model.enums.StatutLot;
import com.futurekawa.backendlocal.repository.LotRepository;
import com.futurekawa.backendlocal.service.AlerteService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Scheduled job to check for expired coffee lots.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PeremptionScheduler {

    private final LotRepository lotRepository;
    private final AlerteService alerteService;
    private final PaysProperties paysProperties;

    /**
     * Runs every hour at the top of the hour.
     * Finds lots that have exceeded their maximum storage duration.
     */
    @Scheduled(cron = "0 0 * * * *")
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

            String description = String.format("Lot %s has exceeded the maximum storage duration of %d days.",
                    lot.getReferenceLot(), maxDays);

            alerteService.createPeremptionAlerte(lot.getEntrepot(), lot, description);
        }

        log.info("Finished processing expired lots.");
    }
}
