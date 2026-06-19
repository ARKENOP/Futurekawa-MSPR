package com.futurekawa.backendlocal.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.dto.MqttMesurePayload;
import com.futurekawa.backendlocal.dto.response.MesureStockageResponse;
import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.MesureStockageMapper;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.MesureStockage;
import com.futurekawa.backendlocal.model.enums.NiveauAlerte;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.MesureStockageRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MesureService {

    private final MesureStockageRepository mesureRepository;
    private final EntrepotRepository entrepotRepository;
    private final MesureStockageMapper mesureMapper;
    private final AlerteService alerteService;
    private final PaysProperties paysProperties;

    public Page<MesureStockageResponse> getHistoryByEntrepot(Long entrepotId, Pageable pageable) {
        return mesureRepository.findByEntrepotIdOrderByDateHeureMesureDesc(entrepotId, pageable)
                .map(mesureMapper::toResponse);
    }

    public MesureStockageResponse getLatestByEntrepot(Long entrepotId) {
        return mesureRepository.findTopByEntrepotIdOrderByDateHeureMesureDesc(entrepotId)
                .map(mesureMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("No measures found for entrepôt ID: " + entrepotId));
    }

    @Transactional
    public void saveMesure(Long entrepotId, MqttMesurePayload payload) {
        Entrepot entrepot = entrepotRepository.findById(entrepotId)
                .orElseThrow(() -> new ResourceNotFoundException("Entrepot not found: " + entrepotId));

        MesureStockage mesure = mesureMapper.toEntity(payload);
        mesure.setEntrepot(entrepot);
        // Convert timestamp to LocalDateTime
        mesure.setDateHeureMesure(LocalDateTime.ofInstant(Instant.ofEpochMilli(payload.timestamp()), ZoneId.systemDefault()));

        MesureStockage saved = mesureRepository.save(mesure);
        log.debug("Saved new mesure for entrepôt {}", entrepotId);

        checkThresholds(entrepot, saved);
    }

    private void checkThresholds(Entrepot entrepot, MesureStockage mesure) {
        BigDecimal temp = mesure.getTemperatureC();
        BigDecimal hum = mesure.getHumiditePourcent();

        BigDecimal idealTemp = paysProperties.temperatureIdealeC();
        BigDecimal idealHum = paysProperties.humiditeIdealePourcent();

        BigDecimal tempTol = paysProperties.toleranceTemperatureC();
        BigDecimal humTol = paysProperties.toleranceHumiditePourcent();

        boolean tempWarning = temp.subtract(idealTemp).abs().compareTo(tempTol) > 0;
        boolean tempCritical = temp.subtract(idealTemp).abs().compareTo(tempTol.multiply(BigDecimal.valueOf(2))) > 0;

        boolean humWarning = hum.subtract(idealHum).abs().compareTo(humTol) > 0;
        boolean humCritical = hum.subtract(idealHum).abs().compareTo(humTol.multiply(BigDecimal.valueOf(2))) > 0;

        if (tempCritical || humCritical) {
            alerteService.createConditionAlerte(entrepot, mesure, NiveauAlerte.CRITIQUE,
                    String.format("Critical conditions in %s: Temp=%.1f (Ideal=%.1f), Hum=%.1f (Ideal=%.1f)",
                            entrepot.getNomEntrepot(), temp, idealTemp, hum, idealHum));
        } else if (tempWarning || humWarning) {
            alerteService.createConditionAlerte(entrepot, mesure, NiveauAlerte.WARNING,
                    String.format("Warning conditions in %s: Temp=%.1f, Hum=%.1f",
                            entrepot.getNomEntrepot(), temp, hum));
        }
    }
}
