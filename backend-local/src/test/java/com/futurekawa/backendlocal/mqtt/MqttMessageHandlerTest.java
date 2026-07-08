package com.futurekawa.backendlocal.mqtt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.futurekawa.backendlocal.dto.MqttMesurePayload;
import com.futurekawa.backendlocal.service.MesureService;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class MqttMessageHandlerTest {

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private MesureService mesureService;

    @InjectMocks
    private MqttMessageHandler handler;

    private static Message<String> message(String topic, String payload) {
        return MessageBuilder.withPayload(payload)
                .setHeader(MqttHeaders.RECEIVED_TOPIC, topic)
                .build();
    }

    @Test
    void parsesValidTopicAndDelegatesToService() {
        String payload = "{\"id_capteur\":\"c1\",\"temperature_c\":27.7,"
                + "\"humidite_pourcent\":46.4,\"timestamp\":1718373120000}";

        handler.handleMessage(message("futurekawa/BR/entrepot/7/mesures", payload));

        ArgumentCaptor<MqttMesurePayload> captor = ArgumentCaptor.forClass(MqttMesurePayload.class);
        verify(mesureService).saveMesure(eq(7L), captor.capture());
        assertThat(captor.getValue().idCapteur()).isEqualTo("c1");
        assertThat(captor.getValue().temperatureC()).isEqualByComparingTo(new BigDecimal("27.7"));
        assertThat(captor.getValue().timestamp()).isEqualTo(1718373120000L);
    }

    @Test
    void ignoresTopicWithWrongStructure() {
        handler.handleMessage(message("futurekawa/BR/sensor/7/mesures", "{}"));
        verify(mesureService, never()).saveMesure(any(), any());
    }

    @Test
    void ignoresTopicTooShort() {
        handler.handleMessage(message("futurekawa/BR/entrepot", "{}"));
        verify(mesureService, never()).saveMesure(any(), any());
    }

    @Test
    void swallowsNonNumericEntrepotId() {
        String payload = "{\"id_capteur\":\"c1\",\"temperature_c\":27.7,"
                + "\"humidite_pourcent\":46.4,\"timestamp\":1}";

        handler.handleMessage(message("futurekawa/BR/entrepot/abc/mesures", payload));

        verify(mesureService, never()).saveMesure(any(), any());
    }

    @Test
    void swallowsMalformedJson() {
        handler.handleMessage(message("futurekawa/BR/entrepot/1/mesures", "not-json"));
        verify(mesureService, never()).saveMesure(any(), any());
    }
}
