package com.futurekawa.backendlocal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.LotMapper;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Exploitation;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.model.Pays;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.ExploitationRepository;
import com.futurekawa.backendlocal.repository.LotRepository;
import com.futurekawa.lib.dto.request.CreateLotRequest;
import com.futurekawa.lib.dto.request.UpdateLotRequest;
import com.futurekawa.lib.enums.StatutLot;

@ExtendWith(MockitoExtension.class)
class LotServiceTest {
    @Mock private LotRepository lotRepository;
    @Mock private ExploitationRepository exploitationRepository;
    @Mock private EntrepotRepository entrepotRepository;
    @Mock private LotMapper lotMapper;

    private LotService service;

    @BeforeEach
    void setUp() {
        service = new LotService(lotRepository, exploitationRepository, entrepotRepository, lotMapper);
    }

    private CreateLotRequest request() {
        return new CreateLotRequest("LOT-1", LocalDate.now(), LocalDate.now().minusDays(10), "AA", 3L, 2L);
    }

    @Test
    void createLotWiresRelationsAndPersists() {
        Pays pays = new Pays();
        pays.setCodePays("BR");
        Exploitation exploitation = new Exploitation();
        exploitation.setId(3L);
        Entrepot entrepot = new Entrepot();
        entrepot.setId(2L);
        entrepot.setPays(pays);

        when(exploitationRepository.findById(3L)).thenReturn(Optional.of(exploitation));
        when(entrepotRepository.findById(2L)).thenReturn(Optional.of(entrepot));
        when(lotMapper.toEntity(any())).thenReturn(new Lot());
        when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createLot(request());

        ArgumentCaptor<Lot> captor = ArgumentCaptor.forClass(Lot.class);
        verify(lotRepository).save(captor.capture());
        Lot persisted = captor.getValue();
        assertThat(persisted.getPays()).isSameAs(pays);
        assertThat(persisted.getExploitation()).isSameAs(exploitation);
        assertThat(persisted.getEntrepot()).isSameAs(entrepot);
        assertThat(persisted.getDateEntreeStockage()).isNotNull();
    }

    @Test
    void createLotThrowsWhenExploitationMissing() {
        when(exploitationRepository.findById(3L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createLot(request())).isInstanceOf(ResourceNotFoundException.class);
        verify(lotRepository, never()).save(any());
    }

    @Test
    void createLotThrowsWhenEntrepotMissing() {
        when(exploitationRepository.findById(3L)).thenReturn(Optional.of(new Exploitation()));
        when(entrepotRepository.findById(2L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.createLot(request())).isInstanceOf(ResourceNotFoundException.class);
        verify(lotRepository, never()).save(any());
    }

    @Test
    void updateStatutChangesStatusAndPersists() {
        Lot lot = new Lot();
        lot.setStatutLot(StatutLot.CONFORME);
        when(lotRepository.findById(1L)).thenReturn(Optional.of(lot));
        when(lotRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.updateStatut(1L, new UpdateLotRequest(StatutLot.PERIME));

        assertThat(lot.getStatutLot()).isEqualTo(StatutLot.PERIME);
        verify(lotRepository).save(lot);
    }

    @Test
    void updateStatutThrowsWhenMissing() {
        when(lotRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateStatut(1L, new UpdateLotRequest(StatutLot.PERIME)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(lotRepository.findById(1L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(1L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
