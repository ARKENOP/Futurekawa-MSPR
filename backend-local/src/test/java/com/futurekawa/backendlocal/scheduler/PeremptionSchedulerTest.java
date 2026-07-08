package com.futurekawa.backendlocal.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.model.enums.StatutLot;
import com.futurekawa.backendlocal.repository.LotRepository;
import com.futurekawa.backendlocal.service.AlerteService;

@ExtendWith(MockitoExtension.class)
class PeremptionSchedulerTest {

    @Mock private LotRepository lotRepository;
    @Mock private AlerteService alerteService;
    @Mock private PaysProperties paysProperties;

    private PeremptionScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new PeremptionScheduler(lotRepository, alerteService, paysProperties);
    }

    private Lot lot(String ref) {
        Lot lot = new Lot();
        lot.setReferenceLot(ref);
        lot.setStatutLot(StatutLot.CONFORME);
        lot.setEntrepot(new Entrepot());
        return lot;
    }

    @Test
    void marksExpiredLotsAndRaisesOneAlertEach() {
        when(paysProperties.dureeMaxStockageJours()).thenReturn(365);
        Lot a = lot("LOT-A");
        Lot b = lot("LOT-B");
        when(lotRepository.findLotsOlderThanAndStatutNot(any(), eq(StatutLot.PERIME)))
                .thenReturn(List.of(a, b));

        scheduler.checkLotExpirations();

        assertThat(a.getStatutLot()).isEqualTo(StatutLot.PERIME);
        assertThat(b.getStatutLot()).isEqualTo(StatutLot.PERIME);
        verify(lotRepository).save(a);
        verify(lotRepository).save(b);
        verify(alerteService).createPeremptionAlerte(eq(a.getEntrepot()), eq(a), any());
        verify(alerteService).createPeremptionAlerte(eq(b.getEntrepot()), eq(b), any());
    }

    @Test
    void doesNothingWhenNoExpiredLots() {
        when(paysProperties.dureeMaxStockageJours()).thenReturn(365);
        when(lotRepository.findLotsOlderThanAndStatutNot(any(), eq(StatutLot.PERIME)))
                .thenReturn(List.of());

        scheduler.checkLotExpirations();

        verify(lotRepository, never()).save(any());
        verifyNoInteractions(alerteService);
    }
}
