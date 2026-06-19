package com.futurekawa.backendlocal.mqtt;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.futurekawa.backendlocal.dto.MqttMesurePayload;
import com.futurekawa.backendlocal.service.MesureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles incoming MQTT messages from sensors.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttMessageHandler {

    private final ObjectMapper objectMapper;
    private final MesureService mesureService;

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handleMessage(Message<String> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = message.getPayload();

        log.debug("Received MQTT message on topic: {}", topic);

        try {
            // Expected topic format: futurekawa/{COUNTRY_CODE}/entrepot/{id}/mesures
            // Example: futurekawa/BR/entrepot/1/mesures
            String[] parts = topic.split("/");
            if (parts.length < 5 || !"entrepot".equals(parts[2])) {
                log.warn("Ignoring message from unexpected topic structure: {}", topic);
                return;
            }

            Long entrepotId = Long.parseLong(parts[3]);
            MqttMesurePayload mesurePayload = objectMapper.readValue(payload, MqttMesurePayload.class);

            mesureService.saveMesure(entrepotId, mesurePayload);

        } catch (NumberFormatException e) {
            log.error("Failed to parse entrepôt ID from topic: {}", topic, e);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize MQTT payload: {}", payload, e);
        } catch (Exception e) {
            log.error("Error processing MQTT message", e);
        }
    }
}
