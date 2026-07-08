package com.futurekawa.backendcentral.dto.response;

import java.time.LocalDate;

import com.futurekawa.backendcentral.dto.enums.StatutLot;

/** Miroir de LotResponse (backend-local). ancienneteJours est calculé côté local. */
public record Lot(
        Long id,
        String referenceLot,
        LocalDate dateEntreeStockage,
        LocalDate dateRecolte,
        StatutLot statutLot,
        String qualiteLot,
        Long exploitationId,
        Long entrepotId,
        Long paysId,
        Integer ancienneteJours
) {
}
