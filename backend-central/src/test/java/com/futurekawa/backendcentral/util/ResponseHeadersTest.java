package com.futurekawa.backendcentral.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * {@code X-Unavailable-Countries} is the only signal the frontend has that a country
 * is missing from an otherwise successful response, so its exact shape matters.
 */
class ResponseHeadersTest {
    @Test
    void omitsTheHeaderWhenEveryCountryAnswered() {
        ResponseEntity<String> response = ResponseHeaders.withUnavailable(List.of()).body("ok");

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getHeaders().getFirst("X-Unavailable-Countries")).isNull();
    }

    @Test
    void listsOneUnavailableCountry() {
        ResponseEntity<String> response = ResponseHeaders.withUnavailable(List.of("EC")).body("ok");

        assertThat(response.getHeaders().getFirst("X-Unavailable-Countries")).isEqualTo("EC");
    }

    @Test
    void joinsSeveralUnavailableCountriesWithCommas() {
        ResponseEntity<String> response =
                ResponseHeaders.withUnavailable(List.of("BR", "EC", "CO")).body("ok");

        assertThat(response.getHeaders().getFirst("X-Unavailable-Countries")).isEqualTo("BR,EC,CO");
    }

    @Test
    void staysA200SoPartialDataIsStillDelivered() {
        assertThat(ResponseHeaders.withUnavailable(List.of("BR", "EC")).body("partiel")
                .getStatusCode().is2xxSuccessful()).isTrue();
    }
}
