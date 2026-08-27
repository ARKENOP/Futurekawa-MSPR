package com.futurekawa.backendlocal.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.futurekawa.backendlocal.AbstractIntegrationTest;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Exploitation;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.model.Pays;
import com.futurekawa.lib.enums.StatutLot;

class LotRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired private LotRepository lotRepository;
    @Autowired private PaysRepository paysRepository;
    @Autowired private ExploitationRepository exploitationRepository;
    @Autowired private EntrepotRepository entrepotRepository;

    private Lot persistLot(String ref, LocalDateTime entryDate, StatutLot statut) {
        Pays pays = paysRepository.findByCodePays("BR").orElseThrow();
        Exploitation exploitation = exploitationRepository.findById(1L).orElseThrow();
        Entrepot entrepot = entrepotRepository.findById(1L).orElseThrow();

        Lot lot = new Lot();
        lot.setReferenceLot(ref);
        lot.setDateEntreeStockage(entryDate);
        lot.setStatutLot(statut);
        lot.setPays(pays);
        lot.setExploitation(exploitation);
        lot.setEntrepot(entrepot);
        return lotRepository.save(lot);
    }

    @Test
    void findsOldNonExpiredLotsOnly() {
        String oldRef = "OLD-" + UUID.randomUUID();
        String freshRef = "FRESH-" + UUID.randomUUID();
        String alreadyExpiredRef = "EXP-" + UUID.randomUUID();

        persistLot(oldRef, LocalDateTime.now().minusDays(400), StatutLot.CONFORME);
        persistLot(freshRef, LocalDateTime.now().minusDays(1), StatutLot.CONFORME);
        persistLot(alreadyExpiredRef, LocalDateTime.now().minusDays(400), StatutLot.PERIME);

        LocalDateTime threshold = LocalDateTime.now().minusDays(365);
        List<Lot> result = lotRepository.findLotsOlderThanAndStatutNot(threshold, StatutLot.PERIME);

        List<String> refs = result.stream().map(Lot::getReferenceLot).toList();
        assertThat(refs).contains(oldRef)
                .doesNotContain(freshRef)
                .doesNotContain(alreadyExpiredRef);
    }
}
