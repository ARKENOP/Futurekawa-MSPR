package com.futurekawa.lib.dto.response;

import java.time.LocalDateTime;

import com.futurekawa.lib.enums.NiveauAlerte;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.TypeAlerte;

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
) {}
