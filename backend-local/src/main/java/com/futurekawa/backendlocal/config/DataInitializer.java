package com.futurekawa.backendlocal.config;

import java.util.List;

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
 * Seeds the reference data a country deployment cannot function without: its own
 * {@link Pays} record, its exploitations, and their entrepôts.
 *
 * <p>These are not demo fixtures bolted on for a screenshot. The cahier des charges
 * (§III.1) requires the solution to handle several exploitations and their entrepôts,
 * but only ever asks the web interface to <em>select</em> them — never to create them.
 * They are therefore reference data, owned by the deployment and seeded by the
 * application, rather than rows typed into a database by hand.
 *
 * <p>Every item is seeded <b>idempotently, by name</b>: an existing record is left
 * untouched and only what is missing is added. So this runs safely on a fresh
 * database, on an already-populated one, and on every restart — in particular it never
 * disturbs the entrepôt the IoT module already publishes to.
 *
 * <p>No country code appears here. Names and addresses are derived from the country
 * the deployment declares in its {@code .env}, so the same code seeds any country.
 * Set {@code futurekawa.seed.enabled=false} to skip it entirely.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {
    private final PaysRepository paysRepository;
    private final ExploitationRepository exploitationRepository;
    private final EntrepotRepository entrepotRepository;
    private final PaysProperties paysProperties;
    private final SeedProperties seedProperties;

    /**
     * One exploitation to seed, with the entrepôts it operates.
     *
     * @param suffixe distinguishes the exploitation within its country; it also builds
     *                the manager's address, so it must stay stable across restarts —
     *                it is the idempotency key.
     */
    private record ExploitationSeed(String suffixe, String region, List<EntrepotSeed> entrepots) {}

    private record EntrepotSeed(String nom, String localisation, int capaciteMax) {}

    private static final List<ExploitationSeed> MODELE = List.of(
            new ExploitationSeed("Nord", "Région Nord", List.of(
                    new EntrepotSeed("Entrepôt Nord A", "Hangar A — Région Nord", 5000),
                    new EntrepotSeed("Entrepôt Nord B", "Hangar B — Région Nord", 3000))),
            new ExploitationSeed("Sud", "Région Sud", List.of(
                    new EntrepotSeed("Entrepôt Sud A", "Hangar A — Région Sud", 4000))),
            new ExploitationSeed("Centre", "Plateau central", List.of(
                    new EntrepotSeed("Entrepôt Centre A", "Silo 1 — Plateau central", 6000))));

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Pays pays = seedPays();
        if (!seedProperties.enabled()) {
            log.info("Reference-data seeding disabled (futurekawa.seed.enabled=false).");
            return;
        }
        seedExploitationPrincipale(pays);
        MODELE.forEach(seed -> seedExploitation(pays, seed));
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
     * The historical single exploitation and its entrepôt, kept so that a deployment
     * seeded by an earlier version keeps the entrepôt ids its IoT module publishes to.
     */
    private void seedExploitationPrincipale(Pays pays) {
        String nomExploitation = "Exploitation " + pays.getNomPays();
        Exploitation exploitation = exploitationRepository.findByNomExploitation(nomExploitation)
                .orElseGet(() -> creerExploitation(pays, nomExploitation, pays.getNomPays(),
                        "responsable." + pays.getCodePays().toLowerCase() + "@futurekawa.local"));

        creerEntrepotSiAbsent(pays, exploitation,
                new EntrepotSeed("Entrepôt principal " + pays.getCodePays(), pays.getNomPays(), 5000));
    }

    private void seedExploitation(Pays pays, ExploitationSeed seed) {
        String nomExploitation = "Exploitation " + seed.suffixe() + " " + pays.getNomPays();
        String email = "responsable." + seed.suffixe().toLowerCase() + "."
                + pays.getCodePays().toLowerCase() + "@futurekawa.local";

        Exploitation exploitation = exploitationRepository.findByNomExploitation(nomExploitation)
                .orElseGet(() -> creerExploitation(pays, nomExploitation, seed.region(), email));

        seed.entrepots().forEach(entrepot -> creerEntrepotSiAbsent(pays, exploitation, entrepot));
    }

    private Exploitation creerExploitation(Pays pays, String nom, String localisation, String email) {
        Exploitation exploitation = new Exploitation();
        exploitation.setNomExploitation(nom);
        exploitation.setLocalisation(localisation);
        exploitation.setResponsableEmail(email);
        exploitation.setEstActive(true);
        exploitation.setPays(pays);
        Exploitation saved = exploitationRepository.save(exploitation);
        log.info("Seeded exploitation id={} '{}'", saved.getId(), saved.getNomExploitation());
        return saved;
    }

    private void creerEntrepotSiAbsent(Pays pays, Exploitation exploitation, EntrepotSeed seed) {
        if (entrepotRepository.findByNomEntrepot(seed.nom()).isPresent()) {
            return;
        }
        Entrepot entrepot = new Entrepot();
        entrepot.setNomEntrepot(seed.nom());
        entrepot.setLocalisation(seed.localisation());
        entrepot.setCapaciteMax(seed.capaciteMax());
        entrepot.setStatutEntrepot("actif");
        entrepot.setExploitation(exploitation);
        entrepot.setPays(pays);
        Entrepot saved = entrepotRepository.save(entrepot);
        log.info("Seeded entrepôt id={} '{}' under exploitation id={}",
                saved.getId(), saved.getNomEntrepot(), exploitation.getId());
    }
}
