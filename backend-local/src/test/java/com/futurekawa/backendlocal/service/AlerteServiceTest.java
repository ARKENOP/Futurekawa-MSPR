package com.futurekawa.backendlocal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.AlerteMapper;
import com.futurekawa.backendlocal.model.Alerte;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.model.MesureStockage;
import com.futurekawa.backendlocal.odoo.OdooQualityAlertService;
import com.futurekawa.backendlocal.repository.AlerteRepository;
import com.futurekawa.lib.dto.request.UpdateAlerteRequest;
import com.futurekawa.lib.enums.NiveauAlerte;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.TypeAlerte;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AlerteServiceTest {

    @Mock private AlerteRepository alerteRepository;
    @Mock private AlerteMapper alerteMapper;
    @Mock private OdooQualityAlertService odooQualityAlertService;
    @Mock private PaysProperties paysProperties;

    private AlerteService service;

    private Entrepot entrepot;

    @BeforeEach
    void setUp() {
        service = new AlerteService(alerteRepository, alerteMapper, odooQualityAlertService, paysProperties);
        when(paysProperties.code()).thenReturn("BR");
        entrepot = new Entrepot();
        entrepot.setId(1L);
        entrepot.setNomEntrepot("Entrepôt BR");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        when(alerteRepository.findById(5L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getById(5L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void closeAlertePersistsAndPropagatesToOdoo() {
        Alerte alerte = new Alerte();
        alerte.setId(9L);
        alerte.setStatutAlerte(StatutAlerte.OUVERTE);
        when(alerteRepository.findById(9L)).thenReturn(Optional.of(alerte));
        when(alerteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.closeAlerte(9L, new UpdateAlerteRequest(StatutAlerte.CLOTUREE));

        verify(alerteRepository).save(alerte);
        verify(odooQualityAlertService).updateAlerteStatut(9L, StatutAlerte.CLOTUREE);
    }

    @Test
    void closeAlerteThrowsWhenMissing() {
        when(alerteRepository.findById(9L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.closeAlerte(9L, new UpdateAlerteRequest(StatutAlerte.CLOTUREE)))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(alerteRepository, never()).save(any());
    }

    @Test
    void createConditionAlerteSkippedWhenActiveDuplicateExists() {
        when(alerteRepository.findFirstByEntrepotIdAndTypeAlerteAndStatutAlerte(
                1L, TypeAlerte.CONDITION_NON_IDEALE, StatutAlerte.OUVERTE))
                .thenReturn(Optional.of(new Alerte()));

        service.createConditionAlerte(entrepot, new MesureStockage(), NiveauAlerte.WARNING, "drift");

        verify(alerteRepository, never()).save(any());
        verify(odooQualityAlertService, never()).pushAlerte(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createConditionAlertePersistsAndPushesWhenNoDuplicate() {
        when(alerteRepository.findFirstByEntrepotIdAndTypeAlerteAndStatutAlerte(
                1L, TypeAlerte.CONDITION_NON_IDEALE, StatutAlerte.OUVERTE))
                .thenReturn(Optional.empty());
        when(alerteRepository.save(any())).thenAnswer(inv -> {
            Alerte a = inv.getArgument(0);
            a.setId(42L);
            return a;
        });
        Lot lot = new Lot();
        lot.setReferenceLot("LOT-1");
        MesureStockage mesure = new MesureStockage();
        mesure.setLot(lot);

        service.createConditionAlerte(entrepot, mesure, NiveauAlerte.CRITIQUE, "critical drift");

        verify(alerteRepository).save(any());
        verify(odooQualityAlertService).pushAlerte(eq(42L), eq("Entrepôt BR"), eq("BR"), any(),
                eq(TypeAlerte.CONDITION_NON_IDEALE), eq(NiveauAlerte.CRITIQUE),
                eq("LOT-1"), eq("critical drift"), any(LocalDateTime.class));
    }

    @Test
    void createConditionAlertePushesNullLotReferenceWhenNoLot() {
        when(alerteRepository.findFirstByEntrepotIdAndTypeAlerteAndStatutAlerte(
                1L, TypeAlerte.CONDITION_NON_IDEALE, StatutAlerte.OUVERTE))
                .thenReturn(Optional.empty());
        when(alerteRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.createConditionAlerte(entrepot, new MesureStockage(), NiveauAlerte.WARNING, "drift");

        verify(odooQualityAlertService).pushAlerte(any(), eq("Entrepôt BR"), eq("BR"), any(),
                eq(TypeAlerte.CONDITION_NON_IDEALE), eq(NiveauAlerte.WARNING),
                isNull(), anyString(), any(LocalDateTime.class));
    }

    @Test
    void createPeremptionAlertePersistsAndPushesCritical() {
        when(alerteRepository.save(any())).thenAnswer(inv -> {
            Alerte a = inv.getArgument(0);
            a.setId(7L);
            return a;
        });
        Lot lot = new Lot();
        lot.setReferenceLot("LOT-OLD");

        service.createPeremptionAlerte(entrepot, lot, "expired");

        verify(alerteRepository).save(any());
        verify(odooQualityAlertService).pushAlerte(eq(7L), eq("Entrepôt BR"), eq("BR"), any(),
                eq(TypeAlerte.LOT_TROP_ANCIEN), eq(NiveauAlerte.CRITIQUE),
                eq("LOT-OLD"), eq("expired"), any(LocalDateTime.class));
    }
    @Test
    void closeAlerteStampsClosureDate() {
        Alerte alerte = new Alerte();
        alerte.setId(7L);
        alerte.setStatutAlerte(StatutAlerte.OUVERTE);
        when(alerteRepository.findById(7L)).thenReturn(Optional.of(alerte));
        when(alerteRepository.save(any(Alerte.class))).thenAnswer(inv -> inv.getArgument(0));
        when(alerteMapper.toResponse(any(Alerte.class))).thenReturn(null);

        service.closeAlerte(7L, new UpdateAlerteRequest(StatutAlerte.CLOTUREE));

        assertThat(alerte.getStatutAlerte()).isEqualTo(StatutAlerte.CLOTUREE);
        assertThat(alerte.getDateCloture()).isNotNull();
        verify(odooQualityAlertService).updateAlerteStatut(7L, StatutAlerte.CLOTUREE);
    }
}
