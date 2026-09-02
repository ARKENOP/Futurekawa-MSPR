package com.futurekawa.backendcentral.registry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.futurekawa.backendcentral.registry.LocalBackendProperties.Entry;

/**
 * The country registry is the one place where a deployment mistake can silently
 * misroute a whole country's data, so the parsing is expected to fail fast rather
 * than start with a half-usable registry.
 */
class LocalBackendPropertiesTest {
    private static List<Entry> parse(String... locals) {
        return new LocalBackendProperties(Arrays.asList(locals)).entries();
    }

    @Test
    void parsesCodeAndUrlPairsInOrder() {
        List<Entry> entries = parse(
                "BR=http://backend-local-br:8081",
                "EC=http://backend-local-ec:8081");

        assertThat(entries).containsExactly(
                new Entry("BR", "http://backend-local-br:8081"),
                new Entry("EC", "http://backend-local-ec:8081"));
    }

    @Test
    void acceptsAnyNumberOfCountries() {
        List<Entry> entries = parse(
                "BR=http://br:8081", "EC=http://ec:8081", "CO=http://co:8081",
                "PE=http://pe:8081", "VN=http://vn:8081");

        assertThat(entries).hasSize(5);
        assertThat(entries).extracting(Entry::codePays)
                .containsExactly("BR", "EC", "CO", "PE", "VN");
    }

    @Test
    void trimsSurroundingWhitespace() {
        assertThat(parse("  BR = http://br:8081  "))
                .containsExactly(new Entry("BR", "http://br:8081"));
    }

    @Test
    void keepsTheUrlIntactWhenItContainsAPath() {
        assertThat(parse("BR=http://gateway/br?token=abc"))
                .containsExactly(new Entry("BR", "http://gateway/br?token=abc"));
    }

    @Test
    void skipsBlankEntries() {
        assertThat(parse("BR=http://br:8081", "", "   ")).hasSize(1);
    }

    @Test
    void treatsNoCountriesAsAnEmptyRegistry() {
        assertThat(new LocalBackendProperties(null).entries()).isEmpty();
        assertThat(parse()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://br:8081",
            "=http://br:8081",
            "BR=",
            "BR",
    })
    void refusesAMalformedEntry(String malformed) {
        assertThatThrownBy(() -> parse(malformed))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Expected CODE=url");
    }

    @Test
    void refusesADuplicateCountryCode() {
        assertThatThrownBy(() -> parse("BR=http://br-1:8081", "BR=http://br-2:8081"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate country code 'BR'");
    }

    @Test
    void returnsAnImmutableList() {
        assertThatThrownBy(() -> parse("BR=http://br:8081").add(new Entry("EC", "http://ec:8081")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
