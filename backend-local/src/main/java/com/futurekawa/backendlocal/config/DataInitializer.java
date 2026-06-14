package com.futurekawa.backendlocal.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Exploitation;
import com.futurekawa.backendlocal.model.Pays;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.ExploitationRepository;
import com.futurekawa.backendlocal.repository.PaysRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Seeds the {@code pays} table with the current country's identity and
 * storage thresholds on application startup.
 *
 * <p>Values come from {@link PaysProperties}, which are bound from
 * the {@code .env} file. If a row with the same {@code codePays} already
 * exists, the seed is skipped (idempotent).</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final PaysRepository paysRepository;
    private final ExploitationRepository exploitationRepository;
    private final EntrepotRepository entrepotRepository;
    private final PaysProperties paysProperties;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Pays pays = seedPays();
        seedDevEntrepot(pays);
    }

    private Pays seedPays() {
        String code = paysProperties.code();

        return paysRepository.findByCodePays(code).orElseGet(() -> {
            Pays pays = new Pays();
            pays.setCodePays(code);
            pays.setNomPays(paysProperties.nom());
            pays.setTemperatureIdealeC(paysProperties.temperatureIdealeC());
            pays.setHumiditeIdealePourcent(paysProperties.humiditeIdealePourcent());
            pays.setToleranceTemperatureC(paysProperties.toleranceTemperatureC());
            pays.setToleranceHumiditePourcent(paysProperties.toleranceHumiditePourcent());
            pays.setEstActif(true);
            Pays saved = paysRepository.save(pays);
            log.info("Seeded pays: {} ({})", saved.getNomPays(), saved.getCodePays());
            return saved;
        });
    }

    /**
     * BETA seed: guarantees at least one exploitation + entrepôt exist so that
     * incoming MQTT measures (topic .../entrepot/{id}/mesures) can be persisted.
     * The first entrepôt gets id 1 on a fresh database — match it in the IoT
     * topic / serial bridge. Idempotent: skipped if any entrepôt already exists.
     */
    private void seedDevEntrepot(Pays pays) {
        if (entrepotRepository.count() > 0) {
            log.info("Entrepôt(s) already present — skipping dev seed.");
            return;
        }

        Exploitation exploitation = new Exploitation();
        exploitation.setNomExploitation("Exploitation " + pays.getNomPays());
        exploitation.setLocalisation(pays.getNomPays());
        exploitation.setResponsableEmail("responsable." + pays.getCodePays().toLowerCase() + "@futurekawa.local");
        exploitation.setEstActive(true);
        exploitation.setPays(pays);
        exploitation = exploitationRepository.save(exploitation);

        Entrepot entrepot = new Entrepot();
        entrepot.setNomEntrepot("Entrepôt principal " + pays.getCodePays());
        entrepot.setLocalisation(pays.getNomPays());
        entrepot.setStatutEntrepot("actif");
        entrepot.setExploitation(exploitation);
        entrepot.setPays(pays);
        entrepot = entrepotRepository.save(entrepot);

        log.info("Seeded dev entrepôt id={} ('{}') under exploitation id={}",
                entrepot.getId(), entrepot.getNomEntrepot(), exploitation.getId());
    }
}
