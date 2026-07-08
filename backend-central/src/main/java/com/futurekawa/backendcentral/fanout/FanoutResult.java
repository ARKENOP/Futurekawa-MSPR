package com.futurekawa.backendcentral.fanout;

import java.util.List;

/** Résultat d'un fan-out vers tous les backends locaux : succès groupés par pays + pays indisponibles (§5 du contrat). */
public record FanoutResult<T>(List<CountrySuccess<T>> successes, List<String> unavailable) {

    public record CountrySuccess<T>(String codePays, String nomPays, T data) {
    }
}
