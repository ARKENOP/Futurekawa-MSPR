package com.futurekawa.lib.dto.response;

import java.time.LocalDate;

import com.futurekawa.lib.enums.StatutLot;

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
) {}
