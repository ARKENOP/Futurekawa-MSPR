package com.futurekawa.backendlocal.dto.response;

import java.time.LocalDate;

import com.futurekawa.backendlocal.model.enums.StatutLot;

public record LotResponse(
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
