package com.futurekawa.backendcentral.dto.response;

import java.time.LocalDateTime;

import com.futurekawa.backendcentral.dto.enums.NiveauAlerte;
import com.futurekawa.backendcentral.dto.enums.StatutAlerte;
import com.futurekawa.backendcentral.dto.enums.TypeAlerte;

/** Miroir de AlerteResponse (backend-local). dateHeureCloture est null tant que l'alerte est ouverte. */
public record Alerte(
        Long id,
        TypeAlerte typeAlerte,
        NiveauAlerte niveau,
        StatutAlerte statutAlerte,
        String messageDescription,
        LocalDateTime dateHeureCreation,
        LocalDateTime dateHeureCloture,
        Long entrepotId,
        Long lotId,
        Long paysId
) {
}
