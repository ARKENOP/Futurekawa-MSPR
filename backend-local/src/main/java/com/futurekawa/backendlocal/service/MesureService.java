package com.futurekawa.backendlocal.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.dto.MqttMesurePayload;
import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.MesureStockageMapper;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.MesureStockage;
import com.futurekawa.backendlocal.repository.EntrepotRepository;
import com.futurekawa.backendlocal.repository.MesureStockageRepository;
import com.futurekawa.lib.dto.response.MesureStockageResponse;
import com.futurekawa.lib.enums.NiveauAlerte;

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

    public Page<MesureStockageResponse> getHistoryByEntrepot(Long entrepotId, LocalDateTime from,
                                                             LocalDateTime to, Pageable pageable) {
        if (from != null || to != null) {
            LocalDateTime start = from != null ? from : LocalDateTime.of(1970, 1, 1, 0, 0);
            LocalDateTime end = to != null ? to : LocalDateTime.now().plusYears(100);
            return mesureRepository
                    .findByEntrepotIdAndDateHeureMesureBetweenOrderByDateHeureMesureDesc(
                            entrepotId, start, end, pageable)
                    .map(mesureMapper::toResponse);
        }
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

        mesure.setDateHeureMesure(LocalDateTime.ofInstant(Instant.ofEpochMilli(payload.timestamp()), ZoneId.systemDefault()));

        MesureStockage saved = mesureRepository.save(mesure);
        log.debug("Saved new mesure for entrepôt {}", entrepotId);

        checkThresholds(entrepot, saved);
    }

    /**
     * Wording of an alert, in French.
     *
     * <p>This string is not a log message: it is displayed in the supervision
     * interface, copied onto the Odoo non-conformity ticket, and mailed to the
     * quality team. It is business content read by French-speaking staff, so it
     * follows the frontend's language rather than the codebase's.
     *
     * <p>Both severities quote the ideal values, because the reading alone does not
     * tell the recipient how far out of range the entrepôt actually is.
     *
     * <p>The locale is explicit. {@code String.format} otherwise uses the JVM
     * default, so the decimal separator would follow whatever locale the container
     * happens to start with — the same reading would render "38.0" here and "38,0"
     * elsewhere, in text that is stored and mailed.
     */
    private String descriptionConditions(String prefixe, String nomEntrepot,
                                         BigDecimal temp, BigDecimal idealTemp,
                                         BigDecimal hum, BigDecimal idealHum) {
        return String.format(Locale.FRENCH,
                "%s dans %s : température %.1f °C (idéale %.1f °C), humidité %.1f %% (idéale %.1f %%).",
                prefixe, nomEntrepot, temp, idealTemp, hum, idealHum);
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
                    descriptionConditions("Conditions critiques", entrepot.getNomEntrepot(),
                            temp, idealTemp, hum, idealHum));
        } else if (tempWarning || humWarning) {
            alerteService.createConditionAlerte(entrepot, mesure, NiveauAlerte.WARNING,
                    descriptionConditions("Conditions hors tolérance", entrepot.getNomEntrepot(),
                            temp, idealTemp, hum, idealHum));
        }
    }
}
