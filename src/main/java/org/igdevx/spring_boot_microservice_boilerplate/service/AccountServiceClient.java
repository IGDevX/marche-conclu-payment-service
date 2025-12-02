package org.igdevx.spring_boot_microservice_boilerplate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.igdevx.spring_boot_microservice_boilerplate.dto.AccountStripeInfoResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.RestClientException;

/**
 * Service to communicate with Account Service for Stripe account information
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceClient {

    private final RestTemplate restTemplate;

    private static final String ACCOUNT_SERVICE_URL = "lb://account-service";

    /**
     * Get Stripe account information for a producer
     */
    public AccountStripeInfoResponse getProducerStripeInfo(String producerKeycloakId) {
        try {
            String url = ACCOUNT_SERVICE_URL + "/account/stripe/connected-account";
            log.info("Fetching Stripe account info for producer: {} from: {}", producerKeycloakId, url);
            
            // Create headers with Keycloak ID
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.set("X-Keycloak-Id", producerKeycloakId);
            
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            
            org.springframework.http.ResponseEntity<AccountStripeInfoResponse> response = restTemplate.exchange(
                url,
                org.springframework.http.HttpMethod.GET,
                entity,
                AccountStripeInfoResponse.class
            );
            
            AccountStripeInfoResponse stripeInfo = response.getBody();
            if (stripeInfo == null || stripeInfo.getStripeAccountId() == null) {
                throw new RuntimeException("Producer does not have a connected Stripe account");
            }
            
            if (!Boolean.TRUE.equals(stripeInfo.getOnboardingComplete())) {  // Changed from getStripeOnboardingComplete()
                throw new RuntimeException("Producer's Stripe account onboarding is not complete");
            }
            
            log.info("Successfully retrieved Stripe account info: {}", stripeInfo.getStripeAccountId());
            return stripeInfo;
            
        } catch (RestClientException e) {
            log.error("Error communicating with Account Service: {}", e.getMessage());
            throw new RuntimeException("Failed to retrieve producer's Stripe account information", e);
        }
    }
}
