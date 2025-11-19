package org.igdevx.spring_boot_microservice_boilerplate.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for retrieving Stripe account information from Account Service
 * Must match the StripeConnectedAccountResponse from account service exactly
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccountStripeInfoResponse {
    private String stripeAccountId;
    private String accountStatus; 
    private Boolean onboardingComplete; 
    private String onboardingUrl;
    private String userKeycloakId;
    private String email;
}