package com.futurekawa.backendcentral.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

/**
 * The central's create-lot request differs from the shared one by exactly one field:
 * {@code codePays}, which routes the call and must not be forwarded — a backend-local
 * serves a single country and rejects an unknown field.
 */
class CreateLotRequestTest {
    private static final Validator VALIDATOR;

    static {
        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            VALIDATOR = factory.getValidator();
        }
    }

    private static CreateLotRequest valide() {
        return new CreateLotRequest("BR", "BR-2026-0001",
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 2, 1),
                "Arabica AA", 1L, 2L);
    }

    @Test
    void stripsTheRoutingCountryAndKeepsEveryOtherField() {
        com.futurekawa.lib.dto.request.CreateLotRequest local = valide().toLocalRequest();

        assertThat(local.referenceLot()).isEqualTo("BR-2026-0001");
        assertThat(local.dateEntreeStockage()).isEqualTo(LocalDate.of(2026, 3, 1));
        assertThat(local.dateRecolte()).isEqualTo(LocalDate.of(2026, 2, 1));
        assertThat(local.qualiteLot()).isEqualTo("Arabica AA");
        assertThat(local.exploitationId()).isEqualTo(1L);
        assertThat(local.entrepotId()).isEqualTo(2L);
    }

    @Test
    void carriesTheStorageDateThroughUnchanged() {
        LocalDate ancien = LocalDate.of(2024, 1, 15);
        CreateLotRequest request = new CreateLotRequest("BR", "BR-OLD", ancien, null, null, 1L, 1L);

        assertThat(request.toLocalRequest().dateEntreeStockage()).isEqualTo(ancien);
    }

    @Test
    void acceptsAnOptionalHarvestDateAndQuality() {
        CreateLotRequest request = new CreateLotRequest("BR", "BR-2026-0002",
                LocalDate.of(2026, 3, 1), null, null, 1L, 1L);

        assertThat(VALIDATOR.validate(request)).isEmpty();
        assertThat(request.toLocalRequest().dateRecolte()).isNull();
        assertThat(request.toLocalRequest().qualiteLot()).isNull();
    }

    @Test
    void passesValidationWhenComplete() {
        assertThat(VALIDATOR.validate(valide())).isEmpty();
    }

    @Test
    void refusesARequestWithoutARoutingCountry() {
        CreateLotRequest request = new CreateLotRequest("", "BR-2026-0001",
                LocalDate.of(2026, 3, 1), null, null, 1L, 1L);

        assertThat(VALIDATOR.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("codePays");
    }

    @Test
    void refusesARequestMissingTheMandatoryLotFields() {
        CreateLotRequest request = new CreateLotRequest("BR", null, null, null, null, null, null);

        Set<ConstraintViolation<CreateLotRequest>> violations = VALIDATOR.validate(request);

        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("referenceLot", "dateEntreeStockage", "exploitationId", "entrepotId");
    }

    @Test
    void refusesAnOverlongLotReference() {
        CreateLotRequest request = new CreateLotRequest("BR", "R".repeat(51),
                LocalDate.of(2026, 3, 1), null, null, 1L, 1L);

        assertThat(VALIDATOR.validate(request)).extracting(v -> v.getPropertyPath().toString())
                .contains("referenceLot");
    }
}
