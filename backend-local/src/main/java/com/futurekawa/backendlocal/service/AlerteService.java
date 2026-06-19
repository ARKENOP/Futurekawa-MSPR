package com.futurekawa.backendlocal.service;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.futurekawa.backendlocal.dto.request.UpdateAlerteRequest;
import com.futurekawa.backendlocal.dto.response.AlerteResponse;
import com.futurekawa.backendlocal.exception.ResourceNotFoundException;
import com.futurekawa.backendlocal.mapper.AlerteMapper;
import com.futurekawa.backendlocal.model.Alerte;
import com.futurekawa.backendlocal.model.Entrepot;
import com.futurekawa.backendlocal.model.Lot;
import com.futurekawa.backendlocal.model.MesureStockage;
import com.futurekawa.backendlocal.model.enums.NiveauAlerte;
import com.futurekawa.backendlocal.model.enums.StatutAlerte;
import com.futurekawa.backendlocal.model.enums.TypeAlerte;
import com.futurekawa.backendlocal.config.PaysProperties;
import com.futurekawa.backendlocal.odoo.OdooQualityAlertService;
import com.futurekawa.backendlocal.repository.AlerteRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AlerteService {

    private final AlerteRepository alerteRepository;
    private final AlerteMapper alerteMapper;
    private final OdooQualityAlertService odooQualityAlertService;
    private final PaysProperties paysProperties;

    public Page<AlerteResponse> listAll(Pageable pageable) {
        return alerteRepository.findAll(pageable).map(alerteMapper::toResponse);
    }

    public AlerteResponse getById(Long id) {
        return alerteRepository.findById(id)
                .map(alerteMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte not found with ID: " + id));
    }

    @Transactional
    public AlerteResponse closeAlerte(Long id, UpdateAlerteRequest request) {
        Alerte alerte = alerteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Alerte not found with ID: " + id));

        alerte.setStatutAlerte(request.statutAlerte());
        Alerte saved = alerteRepository.save(alerte);

        // Local backend is the source of truth: propagate the new status to Odoo.
        odooQualityAlertService.updateAlerteStatut(saved.getId(), saved.getStatutAlerte());

        return alerteMapper.toResponse(saved);
    }

    @Transactional
    public void createConditionAlerte(Entrepot entrepot, MesureStockage mesure, NiveauAlerte niveau, String description) {
        // Dedup logic: check if an active alert already exists for this entrepot and type
        alerteRepository.findFirstByEntrepotIdAndTypeAlerteAndStatutAlerte(
                entrepot.getId(), TypeAlerte.CONDITION_NON_IDEALE, StatutAlerte.OUVERTE
        ).ifPresentOrElse(
                existing -> log.debug("Active condition alert already exists for entrepôt {}. Skipping.", entrepot.getId()),
                () -> {
                    Alerte alerte = new Alerte();
                    alerte.setEntrepot(entrepot);
                    alerte.setMesureDeclencheuse(mesure);
                    alerte.setLotConcerne(mesure.getLot());
                    alerte.setTypeAlerte(TypeAlerte.CONDITION_NON_IDEALE);
                    alerte.setNiveau(niveau);
                    alerte.setStatutAlerte(StatutAlerte.OUVERTE);
                    alerte.setDateHeureCreation(LocalDateTime.now());
                    alerte.setMessageDescription(description);

                    Alerte saved = alerteRepository.save(alerte);

                    String lotReference = mesure.getLot() != null ? mesure.getLot().getReferenceLot() : null;
                    odooQualityAlertService.pushAlerte(
                            saved.getId(),
                            entrepot.getNomEntrepot(),
                            paysProperties.code(),
                            TypeAlerte.CONDITION_NON_IDEALE,
                            niveau,
                            lotReference,
                            description,
                            saved.getDateHeureCreation()
                    );
                }
        );
    }

    @Transactional
    public void createPeremptionAlerte(Entrepot entrepot, Lot lot, String description) {
        Alerte alerte = new Alerte();
        alerte.setEntrepot(entrepot);
        alerte.setLotConcerne(lot);
        alerte.setTypeAlerte(TypeAlerte.LOT_TROP_ANCIEN);
        alerte.setNiveau(NiveauAlerte.CRITIQUE); // Expiry is always critical
        alerte.setStatutAlerte(StatutAlerte.OUVERTE);
        alerte.setDateHeureCreation(LocalDateTime.now());
        alerte.setMessageDescription(description);

        Alerte saved = alerteRepository.save(alerte);

        odooQualityAlertService.pushAlerte(
                saved.getId(),
                entrepot.getNomEntrepot(),
                paysProperties.code(),
                TypeAlerte.LOT_TROP_ANCIEN,
                NiveauAlerte.CRITIQUE,
                lot.getReferenceLot(),
                description,
                saved.getDateHeureCreation()
        );
    }
}
