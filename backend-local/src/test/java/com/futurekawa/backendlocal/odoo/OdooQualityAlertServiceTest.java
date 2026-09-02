package com.futurekawa.backendlocal.odoo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.futurekawa.lib.enums.NiveauAlerte;
import com.futurekawa.lib.enums.StatutAlerte;
import com.futurekawa.lib.enums.TypeAlerte;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OdooQualityAlertServiceTest {
    @Mock private OdooRpcClient odooRpcClient;

    private OdooQualityAlertService service;

    @BeforeEach
    void setUp() {
        service = new OdooQualityAlertService(odooRpcClient);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> capturedCreateVals() {
        ArgumentCaptor<List<Object>> args = ArgumentCaptor.forClass(List.class);
        verify(odooRpcClient).executeKw(eq("futurekawa.quality.alert"), eq("create"), args.capture(), anyMap());

        List<Object> outer = (List<Object>) args.getValue().get(0);
        return (Map<String, Object>) outer.get(0);
    }

    @Test
    void pushAlerteBuildsLowercasedValsWithLotAndUtcDate() {
        service.pushAlerte(42L, "Entrepôt BR", "BR", "Brésil", TypeAlerte.CONDITION_NON_IDEALE,
                NiveauAlerte.CRITIQUE, "LOT-1", "critical drift",
                LocalDateTime.of(2026, 1, 2, 3, 4, 5));

        Map<String, Object> vals = capturedCreateVals();
        assertThat(vals).containsEntry("backend_alerte_id", 42L)
                .containsEntry("entrepot_nom", "Entrepôt BR")
                .containsEntry("type_anomaly", "condition_non_ideale")
                .containsEntry("niveau", "critique")
                .containsEntry("pays_code", "BR")
                .containsEntry("pays_nom", "Brésil")
                .containsEntry("lot_reference", "LOT-1")
                .containsEntry("message_description", "critical drift");
        assertThat((String) vals.get("date_creation")).matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    @Test
    void pushAlerteOmitsLotReferenceWhenNull() {
        service.pushAlerte(1L, "E", "BR", "Brésil", TypeAlerte.LOT_TROP_ANCIEN,
                NiveauAlerte.WARNING, null, "msg", LocalDateTime.now());

        assertThat(capturedCreateVals()).doesNotContainKey("lot_reference");
    }

    @Test
    void pushAlerteSwallowsRpcFailure() {
        when(odooRpcClient.executeKw(any(), any(), anyList(), anyMap()))
                .thenThrow(new RuntimeException("Odoo down"));

        service.pushAlerte(1L, "E", "BR", "Brésil", TypeAlerte.LOT_TROP_ANCIEN,
                NiveauAlerte.INFO, null, "msg", LocalDateTime.now());
    }

    @Test
    void pushAlerteSendsNoRecipientAddress() {
        service.pushAlerte(42L, "Entrepôt BR", "BR", "Brésil", TypeAlerte.CONDITION_NON_IDEALE,
                NiveauAlerte.CRITIQUE, "LOT-1", "critical drift", LocalDateTime.now());

        assertThat(capturedCreateVals()).doesNotContainKey("responsable_email");
    }

    @Test
    void updateAlerteStatutWritesMappedStateWhenTicketFound() {
        when(odooRpcClient.executeKw(eq("futurekawa.quality.alert"), eq("search"), anyList(), anyMap()))
                .thenReturn(List.of(7));

        service.updateAlerteStatut(99L, StatutAlerte.CLOTUREE);

        ArgumentCaptor<List<Object>> args = ArgumentCaptor.forClass(List.class);
        verify(odooRpcClient).executeKw(eq("futurekawa.quality.alert"), eq("write"), args.capture(), anyMap());
        @SuppressWarnings("unchecked")
        Map<String, Object> writeVals = (Map<String, Object>) args.getValue().get(1);
        assertThat(writeVals).containsEntry("state", "resolved");
    }

    @Test
    void updateAlerteStatutSkipsWriteWhenNoTicketFound() {
        when(odooRpcClient.executeKw(eq("futurekawa.quality.alert"), eq("search"), anyList(), anyMap()))
                .thenReturn(List.of());

        service.updateAlerteStatut(99L, StatutAlerte.OUVERTE);

        verify(odooRpcClient, never()).executeKw(any(), eq("write"), anyList(), anyMap());
    }
}
