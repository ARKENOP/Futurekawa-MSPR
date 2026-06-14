package com.futurekawa.backendlocal.odoo;

import java.util.List;
import java.util.Map;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.futurekawa.backendlocal.config.OdooProperties;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Service to send emails via Odoo's mail.message API.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OdooEmailService {

    private final OdooRpcClient odooRpcClient;
    private final OdooProperties odooProperties;

    /**
     * Sends an alert email via Odoo asynchronously.
     * Uses the mail.message model to create a notification.
     */
    @Async
    public void sendAlertEmail(String subject, String body) {
        try {
            log.info("Sending alert email via Odoo. Subject: {}", subject);

            Map<String, Object> messageVals = Map.of(
                    "subject", subject,
                    "body", body,
                    "email_to", odooProperties.alerteDestinataireEmail(),
                    "message_type", "notification"
            );

            Object result = odooRpcClient.executeKw(
                    "mail.message",
                    "create",
                    List.of(List.of(messageVals)),
                    Map.of()
            );

            log.info("Successfully created Odoo mail.message with ID: {}", result);

        } catch (Exception e) {
            log.error("Failed to send alert email via Odoo", e);
        }
    }
}
