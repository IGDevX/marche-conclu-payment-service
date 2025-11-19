package org.igdevx.spring_boot_microservice_boilerplate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentRequest {
    private Long amount; // Amount in cents
    private String currency;
    private String producerKeycloakId; // Producer's Keycloak ID to get their Stripe account
    private String orderId; // Optional: for tracking purposes
    private Long applicationFeeAmount; // Optional: platform fee in cents
}
