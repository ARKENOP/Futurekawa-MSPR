package com.futurekawa.backendlocal.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.dto.MqttMesurePayload;
import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.MesureStockageMapper;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.MesureStockage;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.MesureStockageRepository;
import com.futurekawa.lib.enums.NiveauAlerte;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MesureServiceTest {

    @Mock private MesureStockageRepository mesureRepository;
    @Mock private EntrepotRepository entrepotRepository;
    @Mock private MesureStockageMapper mesureMapper;
    @Mock private AlerteService alerteService;
    @Mock private PaysProperties paysProperties;

    private MesureService service;

    private Entrepot entrepot;

    @BeforeEach
    void setUp() {
        service = new MesureService(mesureRepository, entrepotRepository, mesureMapper,
                alerteService, paysProperties);

        entrepot = new Entrepot();
        entrepot.setId(1L);
        entrepot.setNomEntrepot("Entrepôt BR");

        when(paysProperties.temperatureIdealeC()).thenReturn(new BigDecimal("29"));
        when(paysProperties.humiditeIdealePourcent()).thenReturn(new BigDecimal("55"));
        when(paysProperties.toleranceTemperatureC()).thenReturn(new BigDecimal("3"));
        when(paysProperties.toleranceHumiditePourcent()).thenReturn(new BigDecimal("2"));
    }

    private MqttMesurePayload payload(String temp, String hum) {
        return new MqttMesurePayload("capteur-1", new BigDecimal(temp), new BigDecimal(hum), 1_718_373_120_000L);
    }

    private void stubPersist(String temp, String hum) {
        MesureStockage mesure = new MesureStockage();
        mesure.setTemperatureC(new BigDecimal(temp));
        mesure.setHumiditePourcent(new BigDecimal(hum));
        when(mesureMapper.toEntity(any())).thenReturn(mesure);
        when(entrepotRepository.findById(1L)).thenReturn(Optional.of(entrepot));
        when(mesureRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void savesMeasureAndRaisesNoAlertWithinTolerance() {
        stubPersist("30.0", "56.0");

        service.saveMesure(1L, payload("30.0", "56.0"));

        verify(mesureRepository).save(any());
        verifyNoInteractions(alerteService);
    }

    @Test
    void raisesNoAlertExactlyOnToleranceBoundary() {
        stubPersist("32.0", "57.0");

        service.saveMesure(1L, payload("32.0", "57.0"));

        verifyNoInteractions(alerteService);
    }

    @Test
    void raisesWarningWhenTemperatureDriftsBeyondTolerance() {
        stubPersist("33.0", "55.0");

        service.saveMesure(1L, payload("33.0", "55.0"));

        verify(alerteService).createConditionAlerte(eq(entrepot), any(), eq(NiveauAlerte.WARNING), anyString());
    }

    @Test
    void raisesCriticalWhenTemperatureDriftsBeyondDoubleTolerance() {
        stubPersist("36.0", "55.0");

        service.saveMesure(1L, payload("36.0", "55.0"));

        verify(alerteService).createConditionAlerte(eq(entrepot), any(), eq(NiveauAlerte.CRITIQUE), anyString());
    }

    @Test
    void raisesWarningWhenHumidityDriftsBeyondTolerance() {
        stubPersist("29.0", "58.0");

        service.saveMesure(1L, payload("29.0", "58.0"));

        verify(alerteService).createConditionAlerte(eq(entrepot), any(), eq(NiveauAlerte.WARNING), anyString());
    }

    @Test
    void raisesCriticalWhenHumidityDriftsBeyondDoubleTolerance() {
        stubPersist("29.0", "60.0");

        service.saveMesure(1L, payload("29.0", "60.0"));

        verify(alerteService).createConditionAlerte(eq(entrepot), any(), eq(NiveauAlerte.CRITIQUE), anyString());
    }

    @Test
    void throwsWhenEntrepotDoesNotExist() {
        when(entrepotRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveMesure(99L, payload("29.0", "55.0")))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");

        verify(mesureRepository, never()).save(any());
        verifyNoInteractions(alerteService);
    }

    @Test
    void getLatestThrowsWhenNoMeasure() {
        when(mesureRepository.findTopByEntrepotIdOrderByDateHeureMesureDesc(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLatestByEntrepot(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getLatestMapsWhenPresent() {
        MesureStockage mesure = new MesureStockage();
        var response = new com.futurekawa.lib.dto.response.MesureStockageResponse(
                1L, "c1", null, null, null, 1L, null);
        when(mesureRepository.findTopByEntrepotIdOrderByDateHeureMesureDesc(1L)).thenReturn(Optional.of(mesure));
        when(mesureMapper.toResponse(mesure)).thenReturn(response);

        assertThat(service.getLatestByEntrepot(1L)).isSameAs(response);
    }
}
