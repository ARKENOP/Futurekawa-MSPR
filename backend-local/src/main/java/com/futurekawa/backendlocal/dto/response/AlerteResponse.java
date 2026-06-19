package com.futurekawa.backendlocal.dto.response;

import java.time.LocalDateTime;

import com.futurekawa.backendlocal.model.enums.NiveauAlerte;
import com.futurekawa.backendlocal.model.enums.StatutAlerte;
import com.futurekawa.backendlocal.model.enums.TypeAlerte;

public record AlerteResponse(
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
