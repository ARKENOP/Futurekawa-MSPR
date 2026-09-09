package com.futurekawa.backendcentral.fanout;

import java.util.List;

public record FanoutResult<T>(List<CountrySuccess<T>> successes, List<String> unavailable) {
    public record CountrySuccess<T>(String codePays, String nomPays, T data) {
    }
}
