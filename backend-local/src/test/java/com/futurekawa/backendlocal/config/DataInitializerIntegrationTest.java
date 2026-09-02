package com.futurekawa.backendlocal.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.futurekawa.backendlocal.AbstractIntegrationTest;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Exploitation;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.ExploitationRepository;
import com.futurekawa.backendlocal.repository.PaysRepository;

/**
 * The reference data a country deployment is seeded with, and the property everything
 * else depends on: seeding again must change nothing. The initializer runs on every
 * boot, against databases that are already populated, and the entrepôt ids it would
 * disturb are the ones the IoT module publishes to.
 */
class DataInitializerIntegrationTest extends AbstractIntegrationTest {
    @Autowired private DataInitializer dataInitializer;
    @Autowired private PaysRepository paysRepository;
    @Autowired private ExploitationRepository exploitationRepository;
    @Autowired private EntrepotRepository entrepotRepository;

    @Test
    void seedsTheCountryDeclaredInTheConfiguration() {
        assertThat(paysRepository.findByCodePays("BR")).isPresent()
                .get().extracting(p -> p.getNomPays()).isEqualTo("Brésil");
    }

    @Test
    void seedsSeveralExploitationsAsTheCahierDesChargesRequires() {
        List<Exploitation> exploitations =
                exploitationRepository.findByPaysId(paysId());

        assertThat(exploitations).hasSizeGreaterThan(1);
        assertThat(exploitations).extracting(Exploitation::getNomExploitation)
                .allSatisfy(nom -> assertThat(nom).contains("Brésil"));
    }

    @Test
    void seedsSeveralEntrepotsSpreadOverThoseExploitations() {
        List<Entrepot> entrepots = entrepotRepository.findByPaysId(paysId());

        assertThat(entrepots).hasSizeGreaterThan(1);
        assertThat(entrepots).extracting(e -> e.getExploitation().getId())
                .doesNotContainNull();
        assertThat(entrepots).extracting(e -> e.getExploitation().getId())
                .hasSizeGreaterThan(1);
    }

    @Test
    void keepsTheHistoricalPrincipalEntrepotSoIotTopicsStayValid() {
        assertThat(entrepotRepository.findByNomEntrepot("Entrepôt principal BR")).isPresent();
    }

    @Test
    void givesEveryExploitationAManagerAddressDerivedFromTheCountry() {
        assertThat(exploitationRepository.findByPaysId(paysId()))
                .extracting(Exploitation::getResponsableEmail)
                .allSatisfy(email -> assertThat(email).endsWith("@futurekawa.local"));
    }

    @Test
    void everyEntrepotBelongsToTheDeployedCountry() {
        assertThat(entrepotRepository.findByPaysId(paysId()))
                .extracting(e -> e.getPays().getCodePays())
                .containsOnly("BR");
    }

    @Test
    void seedingAgainAddsNothing() {
        long exploitationsAvant = exploitationRepository.count();
        long entrepotsAvant = entrepotRepository.count();
        long paysAvant = paysRepository.count();

        dataInitializer.run(null);
        dataInitializer.run(null);

        assertThat(exploitationRepository.count()).isEqualTo(exploitationsAvant);
        assertThat(entrepotRepository.count()).isEqualTo(entrepotsAvant);
        assertThat(paysRepository.count()).isEqualTo(paysAvant);
    }

    @Test
    void seedingAgainKeepsTheExistingEntrepotIds() {
        List<Long> avant = entrepotRepository.findByPaysId(paysId()).stream()
                .map(Entrepot::getId).sorted().toList();

        dataInitializer.run(null);

        assertThat(entrepotRepository.findByPaysId(paysId()).stream()
                .map(Entrepot::getId).sorted().toList()).isEqualTo(avant);
    }

    private Long paysId() {
        return paysRepository.findByCodePays("BR").orElseThrow().getId();
    }
}
